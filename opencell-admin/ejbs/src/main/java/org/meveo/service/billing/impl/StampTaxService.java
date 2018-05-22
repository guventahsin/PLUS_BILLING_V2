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
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.NumberUtil;
import org.meveo.cache.RatingCacheContainerProvider;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingAccountStampTax;
import org.meveo.model.billing.RecurringChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.StampTax;
import org.meveo.model.billing.StampTaxCalculationTypeEnum;
import org.meveo.model.billing.StampTaxChargeInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.service.base.BusinessService;

@Stateless
public class StampTaxService  extends BusinessService<StampTax> {

	@Inject 
	private BillingAccountService billingAccountService;
	
    @Inject
    private RatingCacheContainerProvider ratingCacheContainerProvider;
    
    public StampTax calculateStampTax(Subscription subscription) throws BusinessException
    {
    	
    	//TODO: daha once hesaplanmissa tekrar hesaplama
    	StampTax stampTax = new StampTax();
    	stampTax.setCode("STAMP_TAX");
		stampTax.setSubscription(subscription);
		stampTax.setCalculationDate(new Date());
		stampTax.setCalculationType(StampTaxCalculationTypeEnum.INFO);
		stampTax.setTotalTaxAmount(new BigDecimal(0));
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
					StampTaxChargeInstance stampTaxChargeInstance = calculateStampTaxOfRecCharge(subscription,attachedStampTax, recChargeIns, subscription.getSubscriptionRenewal().getInitialyActiveFor());
					attachedStampTax.addStampTaxChargeInstance(stampTaxChargeInstance);
					totalTaxAmount = totalTaxAmount.add(stampTaxChargeInstance.getStampTaxAmount());
				}
	    	}
	    	
	    	attachedStampTax.setTotalTaxAmount(totalTaxAmount);
	    	this.update(attachedStampTax);
    	}
    	return attachedStampTax;
    }

	private StampTaxChargeInstance calculateStampTaxOfRecCharge(Subscription subscription, StampTax stampTax, RecurringChargeInstance recChargeIns, Integer commitmentMonth) {
		
		Integer periodStart = recChargeIns.getRecurringChargeTemplate().getDurationTermInMonthStart();
		Integer periodEnd = recChargeIns.getRecurringChargeTemplate().getDurationTermInMonth();
		BigDecimal stampTaxAmount = new BigDecimal(0);
		int stampTaxMonth = 0;
		
		if (periodStart == null && periodEnd == null)
		{
			stampTaxMonth = commitmentMonth;
		}
		else if (periodStart != null)
		{
			if (periodEnd == null){
				periodEnd = commitmentMonth;
			}
			stampTaxMonth = periodEnd - periodStart + 1;
			if (stampTaxMonth > commitmentMonth)
			{
				stampTaxMonth = commitmentMonth;
			}
		}
		
		if (stampTaxMonth > 0){
			
			List<PricePlanMatrix> chargePricePlans = ratingCacheContainerProvider.getPricePlansByChargeCode(recChargeIns.getChargeTemplate().getCode());
			BigDecimal recChargeAmountWithoutTax = new BigDecimal(0);
			for (PricePlanMatrix pricePlanMatrix : chargePricePlans){
       		 if (pricePlanMatrix.getOfferTemplate() != null && pricePlanMatrix.getOfferTemplate().getCode() != null 
       				 && pricePlanMatrix.getOfferTemplate().getCode().equals(subscription.getOffer().getCode())){
       			 recChargeAmountWithoutTax = pricePlanMatrix.getAmountWithoutTax();
       		 }
			}
			stampTaxAmount = recChargeAmountWithoutTax.multiply(new BigDecimal(stampTaxMonth)).multiply(new BigDecimal(StampTax.stampTaxRate));
			stampTaxAmount = NumberUtil.getInChargeUnit(stampTaxAmount, recChargeIns.getRecurringChargeTemplate().getUnitMultiplicator(), 
						recChargeIns.getRecurringChargeTemplate().getUnitNbDecimal()
						, recChargeIns.getRecurringChargeTemplate().getRoundingMode());
		}

		StampTaxChargeInstance stampTaxChargeInstance = new StampTaxChargeInstance();
		stampTaxChargeInstance.setCode("STAMP_TAX_CHARGE");
		stampTaxChargeInstance.setStampTax(stampTax);
		stampTaxChargeInstance.setStampTaxAmount(stampTaxAmount);
		stampTaxChargeInstance.setChargeInstance(recChargeIns);
		
		
		return stampTaxChargeInstance;
	}


}