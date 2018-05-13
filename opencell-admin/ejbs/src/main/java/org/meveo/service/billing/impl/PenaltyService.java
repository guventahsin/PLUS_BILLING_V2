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
import org.meveo.model.billing.VirtualRecurringCharge;
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

    @Inject
    private WalletOperationService walletOperationService;
    
    @Inject
    private RecurringChargeInstanceService recurringChargeInstanceService;
    
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
			
			Penalty returnedPenalty = null;
    		if (subscription.getEndAgreementDate().after(terminationDate)){
    			    	    	
    			returnedPenalty = getAppliedTotalDiscountAmount(subscription, terminationDate, attachedPenalty);
    			returnedPenalty = getTotalToBeChargedAndRemainingInstallmentAmount(subscription, terminationDate, returnedPenalty);

    			if (returnedPenalty.getTotalAppliedDiscountAmountWithTax().compareTo(returnedPenalty.getTotalToBeChargedAmountWithTax()) <= 0 ){
    				penaltyAmountWithTax = returnedPenalty.getTotalAppliedDiscountAmountWithTax().add(returnedPenalty.getTotalInstallmentAmountWithTax());
    			}
    			else{
    				penaltyAmountWithTax = returnedPenalty.getTotalToBeChargedAmountWithTax().add(returnedPenalty.getTotalInstallmentAmountWithTax());
    			}
    		}
    		else{
    			penaltyAmountWithTax = new BigDecimal(0);
    		}
    		
    		returnedPenalty.setPenaltyAmountWitTax(penaltyAmountWithTax);
    		update(returnedPenalty);
    	}
    	    	
    	return penaltyAmountWithTax;
    }

    private List<WalletOperation> chargeUpToTermination(Subscription subscription, Date terminationDate) throws BusinessException
    {
    	List<WalletOperation> walletOperations = new ArrayList<WalletOperation>();
		for (ServiceInstance serviceInstance : subscription.getServiceInstances()){
			for (RecurringChargeInstance recChargeIns : serviceInstance.getRecurringChargeInstances()){
	            if ( recChargeIns.getChargeDate() != null && subscription.getEndAgreementDate() != null
	            		&& !recChargeIns.getStatus().equals(InstanceStatusEnum.CLOSED)
	            		&& !recChargeIns.getStatus().equals(InstanceStatusEnum.TERMINATED)
	            		&& recChargeIns.getChargeDate().getTime() <= terminationDate.getTime()) {
	            	
	            	Date chargeDate = recChargeIns.getChargeDate();
	                Date nextChargeDate = recChargeIns.getNextChargeDate();
	                
	                if (nextChargeDate.after(terminationDate)){
	                	nextChargeDate = terminationDate;
	                }
	                
	                if (chargeDate.after(nextChargeDate)){
	                	continue;
	                }
	                
	                while (chargeDate!= null && nextChargeDate != null && chargeDate.before(terminationDate)) {
	                    
	                    List<WalletOperation> wos = new ArrayList<WalletOperation>();
	                    VirtualRecurringCharge virtualRecurringCharge = new VirtualRecurringCharge();
	                    if (!recChargeIns.getRecurringChargeTemplate().getApplyInAdvance()) {
	                    	virtualRecurringCharge = walletOperationService.applyVirtualNotAppliedinAdvanceReccuringCharge(recChargeIns, false, recChargeIns.getRecurringChargeTemplate(), subscription.getEndAgreementDate(), WalletOperationStatusEnum.PENALTY, chargeDate, nextChargeDate);
	                    	walletOperations.addAll(virtualRecurringCharge.getWalletOperations());
		                    chargeDate = virtualRecurringCharge.getChargeDate();
		                    nextChargeDate = virtualRecurringCharge.getNextChargeDate();
	                    } else {
	                    	wos = walletOperationService.applyReccuringCharge(recChargeIns, false, recChargeIns.getRecurringChargeTemplate(), false, WalletOperationStatusEnum.PENALTY);
	                    	walletOperations.addAll(wos);
	                    	//TODO: chargeDate nextChargeDate i advance olanlar icin de handle et
	                    }
	                }
	            }
	            
	            //subscription terminate edildiginde chargelar terminate oluyor ve termination da open wallet lar olusmus oluyor
	            if (recChargeIns.getStatus().equals(InstanceStatusEnum.TERMINATED)){
	            	for (WalletOperation wo : recChargeIns.getWalletOperations()){
	            		if (wo.getStatus().equals(WalletOperationStatusEnum.OPEN)){
	            			walletOperations.add(wo);
	            		}
	            	}
	            }
	           
			}
		}
    	return walletOperations;
    }
    

	private Penalty getAppliedTotalDiscountAmount(Subscription subscription, Date terminationDate, Penalty penalty) throws BusinessException {
		
		List<WalletOperation> walletOperations = new ArrayList<WalletOperation>(); 

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

			for (WalletOperation wo : walletOperations){
				PenaltyWalletOperation penaltyWalletOperation = new PenaltyWalletOperation();
				penaltyWalletOperation.setCode("PWO");
				penaltyWalletOperation.setPenalty(penalty);
				penaltyWalletOperation.setType(WalletOperationStatusEnum.APPLIED_DISCOUNT);
				penaltyWalletOperation.setWalletOperation(wo);
				penalty.addPenaltyWalletOperation(penaltyWalletOperation);
			}
		}
		
		
		for (WalletOperation wo: chargeUpToTermination(subscription, terminationDate)){
			if (wo.getChargeInstance().getChargeTemplate().getChargeSubType().equals(ChargeSubTypeEnum.DISCOUNT)){
				PenaltyWalletOperation penaltyWalletOperation = new PenaltyWalletOperation();
				penaltyWalletOperation.setCode("PWO");
				penaltyWalletOperation.setPenalty(penalty);
				penaltyWalletOperation.setType(WalletOperationStatusEnum.APPLIED_DISCOUNT);
				penaltyWalletOperation.setWalletOperation(wo);
				penalty.addPenaltyWalletOperation(penaltyWalletOperation);
				totalDiscountAmountWithTax = totalDiscountAmountWithTax.add(wo.getAmountWithTax());
			}
		}
		
		
		totalDiscountAmountWithTax = totalDiscountAmountWithTax.multiply(new BigDecimal(-1));
		penalty.setTotalAppliedDiscountAmountWithTax(totalDiscountAmountWithTax);
		return penalty;
	}


	private List<WalletOperation> getToBeChargedWallets(Subscription subscription, Date terminationDate) throws BusinessException{
		List<WalletOperation> walletOperations = new ArrayList<WalletOperation>();
		for (ServiceInstance serviceInstance : subscription.getServiceInstances()){
			for (RecurringChargeInstance recChargeIns : serviceInstance.getRecurringChargeInstances()){
	            if ( recChargeIns.getChargeDate() != null && subscription.getEndAgreementDate() != null
	            		&& !recChargeIns.getStatus().equals(InstanceStatusEnum.CLOSED)
	            		&& recChargeIns.getChargeDate().getTime() <= subscription.getEndAgreementDate().getTime()) {
	            	
	            	Date chargeDate = recChargeIns.getChargeDate();
	            	if (chargeDate.before(terminationDate)){
	            		chargeDate = terminationDate;
	            	}
	                Date nextChargeDate = recChargeIns.getNextChargeDate();
	                
	                if (chargeDate != null && nextChargeDate == null && recChargeIns.getStatus().equals(InstanceStatusEnum.TERMINATED)){
	                	nextChargeDate = recChargeIns.getRecurringChargeTemplate().getCalendar().nextCalendarDate(chargeDate);
	                }
	                
	                if (chargeDate.after(nextChargeDate)){
	                	continue;
	                }
	                
	                while (chargeDate!= null && nextChargeDate != null && chargeDate.before(subscription.getEndAgreementDate())) {
	                    
	                    List<WalletOperation> wos = new ArrayList<WalletOperation>();
	                    VirtualRecurringCharge virtualRecurringCharge = new VirtualRecurringCharge();
	                    if (!recChargeIns.getRecurringChargeTemplate().getApplyInAdvance()) {
	                    	virtualRecurringCharge = walletOperationService.applyVirtualNotAppliedinAdvanceReccuringCharge(recChargeIns, false, recChargeIns.getRecurringChargeTemplate(), subscription.getEndAgreementDate(), WalletOperationStatusEnum.PENALTY, chargeDate, nextChargeDate);
	                    	walletOperations.addAll(virtualRecurringCharge.getWalletOperations());
		                    chargeDate = virtualRecurringCharge.getChargeDate();
		                    nextChargeDate = virtualRecurringCharge.getNextChargeDate();
	                    } else {
	                    	wos = walletOperationService.applyReccuringCharge(recChargeIns, false, recChargeIns.getRecurringChargeTemplate(), false, WalletOperationStatusEnum.PENALTY);
	                    	walletOperations.addAll(wos);
	                    	//TODO: chargeDate nextChargeDate i advance olanlar icin de handle et
	                    }
	                }
	            }
	           
			}
		}
		
		return walletOperations;
	}	
	
	
	private Penalty getTotalToBeChargedAndRemainingInstallmentAmount(Subscription subscription, Date terminationDate, Penalty penalty) throws BusinessException {
		
		List<WalletOperation> wos = getToBeChargedWallets(subscription, terminationDate);
        BigDecimal totalToBeChargedAmountWithTax = new BigDecimal(0);
        BigDecimal totalInstallmentAmountWithTax = new BigDecimal(0);
        for (WalletOperation wo : wos){
        	if (wo.getStatus().equals(WalletOperationStatusEnum.PENALTY)){
				if (wo.getChargeInstance().getChargeTemplate().getChargeSubType().equals(ChargeSubTypeEnum.CHARGE)
						|| wo.getChargeInstance().getChargeTemplate().getChargeSubType().equals(ChargeSubTypeEnum.DISCOUNT))
				{
					totalToBeChargedAmountWithTax = totalToBeChargedAmountWithTax.add(wo.getAmountWithTax());
					PenaltyWalletOperation penaltyWalletOperation = new PenaltyWalletOperation();
					penaltyWalletOperation.setCode("PWO");
					penaltyWalletOperation.setPenalty(penalty);
					penaltyWalletOperation.setType(WalletOperationStatusEnum.TOBE_CHARGED);
					penaltyWalletOperation.setWalletOperation(wo);
					penalty.addPenaltyWalletOperation(penaltyWalletOperation);
				}
				else if (wo.getChargeInstance().getChargeTemplate().getChargeSubType().equals(ChargeSubTypeEnum.INSTALLMENT))
				{
					totalInstallmentAmountWithTax = totalInstallmentAmountWithTax.add(wo.getAmountWithTax());
					PenaltyWalletOperation penaltyWalletOperation = new PenaltyWalletOperation();
					penaltyWalletOperation.setCode("PWO");
					penaltyWalletOperation.setPenalty(penalty);
					penaltyWalletOperation.setType(WalletOperationStatusEnum.REMAIN_INSTALLMENT);
					penaltyWalletOperation.setWalletOperation(wo);
					penalty.addPenaltyWalletOperation(penaltyWalletOperation);
				}
        	}
		}
		
        penalty.setTotalToBeChargedAmountWithTax(totalToBeChargedAmountWithTax);
        penalty.setTotalInstallmentAmountWithTax(totalInstallmentAmountWithTax);
        
        return penalty;
	}
	
	private BigDecimal getTotalToBeChargedAmountIfNotSubsTerminated(Subscription subscription, Date terminationDate, Penalty penalty) throws BusinessException {
		
		List<WalletOperation> wos = getToBeChargedWallets(subscription, terminationDate);
        BigDecimal totalToBeChargedAmountWithTax = new BigDecimal(0);
        for (WalletOperation wo : wos){
			if (wo.getStatus().equals(WalletOperationStatusEnum.TOBE_CHARGED))
			{
				totalToBeChargedAmountWithTax = totalToBeChargedAmountWithTax.add(wo.getAmountWithTax());
				PenaltyWalletOperation penaltyWalletOperation = new PenaltyWalletOperation();
				penaltyWalletOperation.setCode("PWO");
				penaltyWalletOperation.setPenalty(penalty);
				penaltyWalletOperation.setType(WalletOperationStatusEnum.TOBE_CHARGED);
				penaltyWalletOperation.setWalletOperation(wo);
				penalty.addPenaltyWalletOperation(penaltyWalletOperation);
			}
		}
		
        penalty.setTotalToBeChargedAmountWithTax(totalToBeChargedAmountWithTax);
        
		return totalToBeChargedAmountWithTax;
	}
	
	private BigDecimal getTotalRemainingInstallmentAmount(Subscription subscription, Date terminationDate, Penalty penalty) throws BusinessException {

		List<WalletOperation> wos = getToBeChargedWallets(subscription, terminationDate);
        BigDecimal totalInstallmentAmountWithTax = new BigDecimal(0);
        for (WalletOperation wo : wos){
			if (wo.getStatus().equals(WalletOperationStatusEnum.REMAIN_INSTALLMENT))
			{
				totalInstallmentAmountWithTax = totalInstallmentAmountWithTax.add(wo.getAmountWithTax());
				PenaltyWalletOperation penaltyWalletOperation = new PenaltyWalletOperation();
				penaltyWalletOperation.setCode("PWO");
				penaltyWalletOperation.setPenalty(penalty);
				penaltyWalletOperation.setType(WalletOperationStatusEnum.REMAIN_INSTALLMENT);
				penaltyWalletOperation.setWalletOperation(wo);
				penalty.addPenaltyWalletOperation(penaltyWalletOperation);
			}
			
		}
		
        penalty.setTotalInstallmentAmountWithTax(totalInstallmentAmountWithTax);
        
		return totalInstallmentAmountWithTax;
		
	}

	
	public boolean applyPenalty(Long penaltyId) throws BusinessException
    {
		Penalty calculatedPenalty = findById(penaltyId);
		calculatedPenalty.setCalculation_type(PenaltyCalculationTypeEnum.EXECUTE);	
		return true;
   }
   
}