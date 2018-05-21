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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.Auditable;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.Penalty;
import org.meveo.model.billing.PenaltyCalculationTypeEnum;
import org.meveo.model.billing.PenaltyWalletOperation;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RecurringChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.VirtualRecurringCharge;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.billing.WalletOperationStatusEnum;
import org.meveo.model.catalog.ChargeSubTypeEnum;
import org.meveo.service.base.BusinessService;

@Stateless
public class PenaltyService  extends BusinessService<Penalty> {

    @Inject
    private RatedTransactionService ratedTransactionService;

    @Inject
    private WalletOperationService walletOperationService;
    
    @Inject
    private RecurringChargeInstanceService recurringChargeInstanceService;
    
    public Penalty calculatePenalty(Subscription subscription, Date terminationDate) throws BusinessException
    {
    	
    	Penalty returnedPenalty = null;
    	if (subscription != null && subscription.getEndAgreementDate() != null)
    	{
	    	Penalty penalty  = new Penalty();
	    	penalty.setCode("INFO_CODE");
	    	penalty.setSubscription(subscription);
	    	penalty.setCalculationDate(new Date());
	    	penalty.setCalculationType(PenaltyCalculationTypeEnum.INFO);
	    	penalty.setTerminationDate(terminationDate);
			penalty.setSubscriptionTerminationReason(subscription.getSubscriptionTerminationReason());
			
			Penalty attachedPenalty = update(penalty);
			
			BigDecimal penaltyAmountWithTax = null;
    		if (subscription.getEndAgreementDate().after(terminationDate)){
    			    	    	
    			returnedPenalty = getAppliedTotalDiscountAmount(subscription, terminationDate, attachedPenalty);
    			returnedPenalty = getTotalToBeChargedAndRemainingInstallmentAmount(subscription, terminationDate, returnedPenalty);

    			if (returnedPenalty.getTotalAppliedDiscountAmountWithTax().compareTo(returnedPenalty.getTotalToBeChargedAmountWithTax()) <= 0 ){
    				penaltyAmountWithTax = returnedPenalty.getTotalAppliedDiscountAmountWithTax().add(returnedPenalty.getTotalInstallmentAmountWithTax());
    				returnedPenalty.setAppliedWalletOpType(WalletOperationStatusEnum.APPLIED_DISCOUNT);
    			}
    			else{
    				penaltyAmountWithTax = returnedPenalty.getTotalToBeChargedAmountWithTax().add(returnedPenalty.getTotalInstallmentAmountWithTax());
    				returnedPenalty.setAppliedWalletOpType(WalletOperationStatusEnum.TOBE_CHARGED);
    			}
    		}
    		else{
    			penaltyAmountWithTax = new BigDecimal(0);
    		}
    		
    		returnedPenalty.setPenaltyAmountWitTax(penaltyAmountWithTax);
    		update(returnedPenalty);
    	}
    	    	
    	return returnedPenalty;
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
	
	private WalletOperation getNewWalletOperation(WalletOperation wo, WalletOperationStatusEnum status, Date terminationDate)
	{
		WalletOperation newWo = new WalletOperation();
		newWo.setVersion(wo.getVersion());
		newWo.setDisabled(false);
		newWo.setAuditable(new Auditable(this.currentUser));
		newWo.setCode(wo.getCode());
		newWo.setDescription("Taahhüt İptal Bedeli");
		newWo.setAmountTax(wo.getAmountTax().compareTo(BigDecimal.ZERO) < 0 ? wo.getAmountTax().negate() : wo.getAmountTax());
		newWo.setAmountWithTax(wo.getAmountWithTax().compareTo(BigDecimal.ZERO) < 0 ? wo.getAmountWithTax().negate() : wo.getAmountWithTax());
		newWo.setAmountWithoutTax(wo.getAmountWithoutTax().compareTo(BigDecimal.ZERO) < 0 ? wo.getAmountWithoutTax().negate() : wo.getAmountWithoutTax());
		newWo.setEndDate(terminationDate);
		newWo.setOfferCode(wo.getOfferCode());
		newWo.setOperationDate(wo.getOperationDate());
		newWo.setQuantity(wo.getQuantity());
		newWo.setStartDate(wo.getSubscriptionDate());
		newWo.setStatus(status);
		newWo.setSubscriptionDate(wo.getSubscriptionDate());
		newWo.setTaxPercent(wo.getTaxPercent());
		newWo.setUnitAmountTax(wo.getUnitAmountTax());
		newWo.setUnitAmountWithTax(wo.getUnitAmountWithTax());
		newWo.setUnitAmountWithoutTax(wo.getUnitAmountWithoutTax());
		newWo.setChargeInstance(wo.getChargeInstance());
		newWo.setCurrency(wo.getCurrency());
		newWo.setPriceplan(wo.getPriceplan());
		newWo.setSeller(wo.getSeller());
		newWo.setWallet(wo.getWallet());
		newWo.setInputQuantity(wo.getInputQuantity());
		return newWo;
	}
	
	public Penalty applyPenalty(Long penaltyId) throws BusinessException
    {
		Penalty infoPenalty = findById(penaltyId);
		if (infoPenalty == null){
			return null;
		}
		
    	Penalty penalty  = new Penalty();
    	penalty.setCode("EXECUTE_CODE");
    	penalty.setSubscription(infoPenalty.getSubscription());
    	penalty.setCalculationDate(new Date());
    	penalty.setCalculationType(PenaltyCalculationTypeEnum.EXECUTE);
    	penalty.setTerminationDate(infoPenalty.getTerminationDate());
		penalty.setSubscriptionTerminationReason(infoPenalty.getSubscriptionTerminationReason());
		penalty.setAppliedWalletOpType(infoPenalty.getAppliedWalletOpType());
		penalty.setPenaltyAmountWitTax(infoPenalty.getPenaltyAmountWitTax());
		penalty.setTotalAppliedDiscountAmountWithTax(infoPenalty.getTotalAppliedDiscountAmountWithTax());
		penalty.setTotalInstallmentAmountWithTax(infoPenalty.getTotalInstallmentAmountWithTax());
		penalty.setTotalToBeChargedAmountWithTax(infoPenalty.getTotalToBeChargedAmountWithTax());
		penalty.setInfoPenalty(infoPenalty);
		
		Penalty attachedPenalty = update(penalty);
		
		for (PenaltyWalletOperation penaltyWalletOperation : infoPenalty.getPenaltyWalletOperations()
				.stream().filter(p-> p.getType().equals(infoPenalty.getAppliedWalletOpType())).collect(Collectors.toList())){
			
			WalletOperation wo = getNewWalletOperation(penaltyWalletOperation.getWalletOperation(), WalletOperationStatusEnum.OPEN, infoPenalty.getTerminationDate());
			WalletOperation attachedWalletOperation = walletOperationService.update(wo);
			PenaltyWalletOperation penaltyWo = new PenaltyWalletOperation();
			penaltyWo.setCode("PWO");
			penaltyWo.setPenalty(attachedPenalty);
			penaltyWo.setType(infoPenalty.getAppliedWalletOpType());
			penaltyWo.setWalletOperation(attachedWalletOperation);
			attachedPenalty.addPenaltyWalletOperation(penaltyWo);
		}
		
		for (PenaltyWalletOperation penaltyWalletOperation : infoPenalty.getPenaltyWalletOperations()
				.stream().filter(p-> p.getType().equals(WalletOperationStatusEnum.REMAIN_INSTALLMENT)).collect(Collectors.toList())){
			
			WalletOperation wo = getNewWalletOperation(penaltyWalletOperation.getWalletOperation(), WalletOperationStatusEnum.OPEN, infoPenalty.getTerminationDate());
			WalletOperation attachedWalletOperation = walletOperationService.update(wo);
			PenaltyWalletOperation penaltyWo = new PenaltyWalletOperation();
			penaltyWo.setCode("PWO");
			penaltyWo.setPenalty(attachedPenalty);
			penaltyWo.setType(WalletOperationStatusEnum.REMAIN_INSTALLMENT);
			penaltyWo.setWalletOperation(attachedWalletOperation);
			attachedPenalty.addPenaltyWalletOperation(penaltyWo);
		}
		
		Penalty returnedPenalty = update(attachedPenalty);
		
		return returnedPenalty;
   }
   
}