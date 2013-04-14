/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.admin.action.catalog;

import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.primefaces.component.datatable.DataTable;

/**
 * Standard backing bean for {@link ServiceTemplate} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their
 * create, edit, view, delete operations). It works with Manaty custom JSF components.
 * 
 * @author Ignas Lelys
 * @created Dec 7, 2010
 * 
 */
@Named
@ConversationScoped
public class ServiceTemplateBean extends BaseBean<ServiceTemplate> {

    private static final long serialVersionUID = 1L;

    /**
     * Injected
     * 
     * @{link ServiceTemplate} service. Extends {@link PersistenceService}.
     */
    @Inject
    private ServiceTemplateService serviceTemplateService;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public ServiceTemplateBean() {
        super(ServiceTemplate.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public ServiceTemplate initEntity() {
        return super.initEntity();
    }

    @Override
    public DataTable search() {
        getFilters();
        if (!filters.containsKey("disabled")) {
            filters.put("disabled", false);
        }
        return super.search();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.meveo.admin.action.BaseBean#saveOrUpdate(boolean)
     */
    @Override
    public String saveOrUpdate(boolean killConversation) {

        List<RecurringChargeTemplate> recurringCharges = entity.getRecurringCharges();
        for (RecurringChargeTemplate recurringCharge : recurringCharges) {
            if (!recurringCharge.getApplyInAdvance()) {
                break;
            }
        }

        return super.saveOrUpdate(killConversation);
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<ServiceTemplate> getPersistenceService() {
        return serviceTemplateService;
    }

    // /**
    // * @see org.meveo.admin.action.BaseBean#getListFieldsToFetch()
    // */
    // protected List<String> getListFieldsToFetch() {
    // return Arrays.asList("recurringCharges", "subscriptionCharges",
    // "terminationCharges", "durationTermCalendar");
    // }
    //
    // /**
    // * @see org.meveo.admin.action.BaseBean#getFormFieldsToFetch()
    // */
    // protected List<String> getFormFieldsToFetch() {
    // return Arrays.asList("recurringCharges", "subscriptionCharges",
    // "terminationCharges", "durationTermCalendar");
    // }

}
