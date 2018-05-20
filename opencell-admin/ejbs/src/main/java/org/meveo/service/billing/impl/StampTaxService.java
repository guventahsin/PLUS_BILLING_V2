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
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.RecurringChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.StampTax;
import org.meveo.model.billing.StampTaxCalculationTypeEnum;
import org.meveo.model.billing.StampTaxChargeInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.service.base.BusinessService;

@Stateless
public class StampTaxService  extends BusinessService<StampTax> {

	@Inject BillingAccountService billingAccountService;
    
    public StampTax calculateStampTax(Subscription subscription) throws BusinessException
    {
    	
    	//TODO: daha once hesaplanmissa tekrar hesaplama
    	StampTax stampTax = new StampTax();
    	stampTax.setCode("STAMP_TAX");
		stampTax.setSubscription(subscription);
		stampTax.setCalculation_date(new Date());
		stampTax.setCalculation_type(StampTaxCalculationTypeEnum.INFO);
		stampTax.setTotal_tax_amount(new BigDecimal(0));
		StampTax attachedStampTax = this.update(stampTax);
		
    	if (subscription != null && subscription.getEndAgreementDate() != null 
    			&&  !billingAccountService.isExonerated(subscription.getUserAccount().getBillingAccount())
    			&& subscription.getSubscriptionRenewal().getInitialyActiveFor() != null
    			&& subscription.getSubscriptionRenewal().getInitialyActiveFor() > 0 
    	   )
    	{	
    		BigDecimal totalTaxAmount = new BigDecimal(0);
	    	for (ServiceInstance serviceInstance : subscription.getServiceInstances()){
				for (RecurringChargeInstance recChargeIns : serviceInstance.getRecurringChargeInstances()){
					StampTaxChargeInstance stampTaxChargeInstance = calculateStampTaxOfRecCharge(attachedStampTax, recChargeIns, subscription.getSubscriptionRenewal().getInitialyActiveFor());
					attachedStampTax.addStampTaxChargeInstance(stampTaxChargeInstance);
					totalTaxAmount = totalTaxAmount.add(stampTaxChargeInstance.getStamp_tax_amount());
				}
	    	}
	    	
	    	stampTax.setTotal_tax_amount(totalTaxAmount);
	    	this.update(stampTax);
    	}
    	return stampTax;
    }

	private StampTaxChargeInstance calculateStampTaxOfRecCharge(StampTax stampTax, RecurringChargeInstance recChargeIns, Integer commitmentMonth) {
		
		Integer periodStart = recChargeIns.getRecurringChargeTemplate().getDurationTermInMonthStart();
		Integer periodEnd = recChargeIns.getRecurringChargeTemplate().getDurationTermInMonth();
		BigDecimal stampTaxAmount = new BigDecimal(0);
		int stampTaxMonth = 0;
		
		if (periodStart != null)
		{
			if (periodEnd == null){
				periodEnd = commitmentMonth;
			}
			stampTaxMonth = periodEnd - periodStart + 1;
			if (stampTaxMonth > commitmentMonth)
			{
				stampTaxMonth = commitmentMonth;
			}
			stampTaxAmount = recChargeIns.getAmountWithoutTax().multiply(new BigDecimal(stampTaxMonth)).multiply(new BigDecimal(StampTax.stampTaxRate));
		}

		StampTaxChargeInstance stampTaxChargeInstance = new StampTaxChargeInstance();
		stampTaxChargeInstance.setCode("STAMP_TAX_CHARGE");
		stampTaxChargeInstance.setStampTax(stampTax);
		stampTaxChargeInstance.setStamp_tax_amount(stampTaxAmount);
		stampTaxChargeInstance.setChargeInstance(recChargeIns);
		
		
		return stampTaxChargeInstance;
	}

 
	public BigDecimal getTotalStampTaxOfBillingAccount(BillingAccount billingAccount) throws BusinessException{
		BigDecimal totalStampTax = new BigDecimal(0);
		boolean exoneratedFromTaxes = billingAccountService.isExonerated(billingAccount);
		if (!exoneratedFromTaxes){
			for (UserAccount userAccount : billingAccount.getUsersAccounts()){
				for (Subscription subscription : userAccount.getSubscriptions()){
					totalStampTax = totalStampTax.add(calculateStampTax(subscription).getTotal_tax_amount());
				}
			}
		}
		return totalStampTax;
	}
   
}