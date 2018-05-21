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

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingAccountStampTax;
import org.meveo.model.billing.StampTax;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.service.base.BusinessService;

@Stateless
public class BillingAccountStampTaxService  extends BusinessService<BillingAccountStampTax> {

	@Inject 
	private BillingAccountService billingAccountService;
	
    @Inject
    private StampTaxService stampTaxService;
    

	public BillingAccountStampTax getTotalStampTaxOfBillingAccount(BillingAccount billingAccount) throws BusinessException{
		BigDecimal totalStampTax = new BigDecimal(0);
		boolean exoneratedFromTaxes = billingAccountService.isExonerated(billingAccount);
		BillingAccountStampTax billingAccountStampTax = new BillingAccountStampTax();		
		if (!exoneratedFromTaxes){
			
			billingAccountStampTax.setCode("BILL_ACC_STAMP");
			billingAccountStampTax.setBillingAccount(billingAccount);
			billingAccountStampTax = update(billingAccountStampTax);
			
			for (UserAccount userAccount : billingAccount.getUsersAccounts()){
				for (Subscription subscription : userAccount.getSubscriptions()){
					
					StampTax stampTax = stampTaxService.calculateStampTax(subscription);
					stampTax.setBillingAccountStampTax(billingAccountStampTax);
					stampTaxService.update(stampTax);
					
					totalStampTax = totalStampTax.add(stampTax.getTotalTaxAmount());
				}
			}
		}
		billingAccountStampTax.setStampTaxAmount(totalStampTax);
		billingAccountStampTax = update(billingAccountStampTax);
		return billingAccountStampTax;
	}
   
}