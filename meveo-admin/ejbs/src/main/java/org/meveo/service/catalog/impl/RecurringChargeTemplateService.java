/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.service.catalog.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.crm.Provider;

/**
 * Charge Template service implementation.
 * 
 */
@Stateless
public class RecurringChargeTemplateService extends
		ChargeTemplateService<RecurringChargeTemplate> {

	public void removeByCode(EntityManager em, String code, Provider provider) {
		Query query = em
				.createQuery("DELETE RecurringChargeTemplate t WHERE t.code=:code AND t.provider=:provider");
		query.setParameter("code", code);
		query.setParameter("provider", provider);
		query.executeUpdate();
	}
	
	public int getNbrRecurringChrgWithNotPricePlan(Provider provider) { 
		return ((Long)getEntityManager().
				createQuery("select count (*) from RecurringChargeTemplate r where r.code not in (select p.eventCode from  PricePlanMatrix p where p.eventCode is not null) and r.provider=:provider")
				.setParameter("provider", provider).getSingleResult()).intValue();
		}
	
	public  List<RecurringChargeTemplate> getRecurringChrgWithNotPricePlan(Provider provider) { 
		return (List<RecurringChargeTemplate>)getEntityManager().createQuery("from RecurringChargeTemplate r where r.code not in (select p.eventCode from  PricePlanMatrix p where p.eventCode is not null)"
				+ " and r.provider=:provider").setParameter("provider", provider).getResultList();
		}
	
	public  int getNbrRecurringChrgNotAssociated(Provider provider) { 
		return ((Long)getEntityManager().createQuery(
				"select count(*) from RecurringChargeTemplate r where r.id not in (select serv.chargeTemplate from ServiceChargeTemplateRecurring serv) "
				+ " OR r.code not in (select p.eventCode from  PricePlanMatrix p where p.eventCode is not null) "
				+ " and r.provider=:provider").setParameter("provider", provider).getSingleResult()).intValue();
		}
	
	public  List<RecurringChargeTemplate> getRecurringChrgNotAssociated(Provider provider) { 
		return (List<RecurringChargeTemplate>)getEntityManager().createQuery(
				"from RecurringChargeTemplate r where r.id not in (select serv.chargeTemplate from ServiceChargeTemplateRecurring serv)"
				+ " OR r.code not in (select p.eventCode from  PricePlanMatrix p where p.eventCode is not null)"
				+ " and r.provider=:provider").setParameter("provider", provider).getResultList();
		}

}
