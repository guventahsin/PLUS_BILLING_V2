/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.service.billing.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.NoResultException;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.type.descriptor.java.BigDecimalTypeDescriptor;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.IncorrectServiceInstanceException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.billing.ChargeInstance;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.OneShotChargeInstance;
import org.meveo.model.billing.Penalty;
import org.meveo.model.billing.PenaltyCalculationTypeEnum;
import org.meveo.model.billing.PenaltyWalletOperation;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RecurringChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.billing.SubscriptionTerminationReason;
import org.meveo.model.billing.UsageChargeInstance;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.billing.WalletOperationStatusEnum;
import org.meveo.model.catalog.CalendarJoin;
import org.meveo.model.catalog.ChargeSubTypeEnum;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.ServiceChargeTemplate;
import org.meveo.model.catalog.ServiceChargeTemplateUsage;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.CalendarJoin.CalendarJoinTypeEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.BusinessService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.meveo.service.script.service.ServiceModelScriptService;

import jxl.write.DateTime;

@Stateless
public class PenaltyService  extends BusinessService<Penalty> {

    @Inject
    private RatedTransactionService ratedTransactionService;

    
    public BigDecimal calculatePenalty(Subscription subscription, Date terminationDate) throws BusinessException
    {
    	BigDecimal penaltyAmountWithTax = null;
    	if (subscription != null && subscription.getEndAgreementDate() != null)
    	{
	    	Penalty penalty  = new Penalty();
	    	penalty.setCode("CODE");
	    	penalty.setSubscription(subscription);
	    	penalty.setCalculation_date(new Date());
	    	penalty.setCalculation_type(PenaltyCalculationTypeEnum.INFO);
	    	penalty.setTerminationDate(terminationDate);
			penalty.setSubscriptionTerminationReason(subscription.getSubscriptionTerminationReason());
			
			Penalty attachedPenalty = update(penalty);
			
    		if (subscription.getEndAgreementDate().after(terminationDate)){
    			    	    	
    			penalty = getAppliedTotalDiscountAmount(subscription, terminationDate, attachedPenalty);
    			penalty.setTotalAppliedDiscountAmountWithTax(penalty.getTotalAppliedDiscountAmountWithTax());
    			BigDecimal totalToBeChargedAmountWithTax = getTotalToBeChargedAmountIfNotSubsTerminated(subscription, terminationDate);
    			penalty.setTotalToBeChargedAmountWithTax(totalToBeChargedAmountWithTax);
    			BigDecimal totalInstallmentAmountWithTax = getTotalRemainingInstallmentAmount(subscription, terminationDate);
    			penalty.setTotalInstallmentAmountWithTax(totalInstallmentAmountWithTax);

    			if (penalty.getTotalAppliedDiscountAmountWithTax().compareTo(totalToBeChargedAmountWithTax) <= 0 ){
    				penaltyAmountWithTax = penalty.getTotalAppliedDiscountAmountWithTax().add(totalInstallmentAmountWithTax);
    			}
    			else{
    				penaltyAmountWithTax = totalToBeChargedAmountWithTax.add(totalInstallmentAmountWithTax);
    			}
    		}
    		else{
    			penaltyAmountWithTax = new BigDecimal(0);
    		}
    		
    		penalty.setPenaltyAmountWitTax(penaltyAmountWithTax);
    		update(penalty);
    	}
    	    	
    	return penaltyAmountWithTax;
    }


	private Penalty getAppliedTotalDiscountAmount(Subscription subscription, Date terminationDate, Penalty penalty) {
		
		List<WalletOperation> walletOperations = new ArrayList<WalletOperation>(); 

//		subscription.getServiceInstances()
//				.forEach( serviceInstance -> serviceInstance.getRecurringChargeInstances()
//			.stream().filter(recCharge -> recCharge.getRecurringChargeTemplate().getChargeSubType().equals(ChargeSubTypeEnum.DISCOUNT))
//			.forEach(recCharge -> walletOperations.addAll( recCharge.getWalletOperations().stream()
//			.filter(wo -> wo.getStatus().equals(WalletOperationStatusEnum.TREATED) && wo.getEndDate().compareTo(terminationDate) <= 0)
//			.collect(Collectors.toList()))));

		for (ServiceInstance serviceInstance : subscription.getServiceInstances()){
			for (RecurringChargeInstance recurringChargeInstance : serviceInstance.getRecurringChargeInstances()){
				if (recurringChargeInstance.getRecurringChargeTemplate().getChargeSubType().equals(ChargeSubTypeEnum.DISCOUNT)){
					for (WalletOperation walletOperation : recurringChargeInstance.getWalletOperations()){
						if (walletOperation.getStatus().equals(WalletOperationStatusEnum.TREATED)){
							walletOperations.add(walletOperation);
						}
						else{
							continue;
						}
					}
				}
				else{
					continue;
				}
			}
		}
		
		
		BigDecimal totalDiscountAmountWithTax = new BigDecimal(0);
		if (walletOperations.size() > 0){
			List<RatedTransaction> billedRatedTransactions = ratedTransactionService.getBilledRatedTransactions(walletOperations.stream().map(wo -> wo.getId()).collect(Collectors.toList()));
			for(RatedTransaction ratedTransaction: billedRatedTransactions){
				totalDiscountAmountWithTax = totalDiscountAmountWithTax.add(ratedTransaction.getAmountWithTax());
			}
			//billedRatedTransactions.forEach(rt -> totalDiscountAmountWithTax.add(rt.getAmountWithTax()));
			
			for (WalletOperation wo : walletOperations){
				PenaltyWalletOperation penaltyWalletOperation = new PenaltyWalletOperation();
				penaltyWalletOperation.setCode("PWO");
				penaltyWalletOperation.setPenalty(penalty);
				penaltyWalletOperation.setChargeSubType(ChargeSubTypeEnum.DISCOUNT);
				penaltyWalletOperation.setWalletOperation(wo);
				penalty.getPenaltyWalletOperations().add(penaltyWalletOperation);
			}
			
//			List<PenaltyWalletOperation> penaltyWalletOperation = walletOperations
//					.stream().map(wo -> new PenaltyWalletOperation(penalty, wo, ChargeSubTypeEnum.DISCOUNT) ).collect(Collectors.toList());
//			penalty.getPenaltyWalletOperations().addAll(penaltyWalletOperation);
		}
		
		penalty.setTotalAppliedDiscountAmountWithTax(totalDiscountAmountWithTax);
		return penalty;
	}

	private BigDecimal getTotalToBeChargedAmountIfNotSubsTerminated(Subscription subscription, Date terminationDate) {
		
		return new BigDecimal(100000);
	}
	
	private BigDecimal getTotalRemainingInstallmentAmount(Subscription subscription, Date terminationDate) {
		// TODO Auto-generated method stub
		return new BigDecimal(10);
	}

	
	public boolean applyPenalty(Long penaltyId) throws BusinessException
    {
		Penalty calculatedPenalty = findById(penaltyId);
		calculatedPenalty.setCalculation_type(PenaltyCalculationTypeEnum.EXECUTE);	
		return true;
   }
   
}