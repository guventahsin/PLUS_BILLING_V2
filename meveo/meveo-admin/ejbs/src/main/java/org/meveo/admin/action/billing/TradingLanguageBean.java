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
package org.meveo.admin.action.billing;

import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage.Severity;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.util.pagination.PaginationDataModel;
import org.meveo.model.billing.CatMessages;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.Language;
import org.meveo.model.billing.TradingLanguage;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.local.TradingLanguageServiceLocal;
import org.meveo.service.crm.local.ProviderServiceLocal;

/**
 * Standard backing bean for {@link TradingLanguage} (extends {@link BaseBean} that
 * provides almost all common methods to handle entities filtering/sorting in
 * datatable, their create, edit, view, delete operations). It works with Manaty
 * custom JSF components.
 * 
 * @author Marouane ALAMI
 * @created 25-03-2013
 * 
 */
@Name("tradingLanguageBean")
@Scope(ScopeType.CONVERSATION)
public class TradingLanguageBean extends BaseBean<TradingLanguage> {

    private static final long serialVersionUID = 1L;

    /**
     * Injected @{link TradingLanguage} service. Extends {@link PersistenceService}
     * .
     */
    @In
    private TradingLanguageServiceLocal tradingLanguageService;
    
    @In
    private ProviderServiceLocal providerService;
    
    

    /**
     * Constructor. Invokes super constructor and provides class type of this
     * bean for {@link BaseBean}.
     */
    public TradingLanguageBean() {
        super(TradingLanguage.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity
     * from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Begin(nested = true)
    @Factory("tradingLanguage")
    public TradingLanguage init() {
        return initEntity();
    }

    /**
     * Data model of entities for data table in GUI.
     * 
     * @return filtered entities.
     */
    @Out(value = "tradingLanguages", required = false)
    protected PaginationDataModel<TradingLanguage> getDataModel() {
        return entities;
    }

    /**
     * Factory method, that is invoked if data model is empty. Invokes
     * BaseBean.list() method that handles all data model loading. Overriding is
     * needed only to put factory name on it.
     * 
     * @see org.meveo.admin.action.BaseBean#list()
     */
    @Begin(join = true)
    @Factory("tradingLanguages")
    public void list() {
    	getFilters();
    	if(filters.containsKey("languageCode")){
    		filters.put("language.languageCode", filters.get("languageCode"));
    		filters.remove("languageCode");
    	}else if (filters.containsKey("language.languageCode")){
    		filters.remove("language.languageCode");
    	}
    	super.list();
    }

    /**
     * Conversation is ended and user is redirected from edit to his previous
     * window.
     * 
     * @see org.meveo.admin.action.BaseBean#saveOrUpdate(org.meveo.model.IEntity)
     */
    @End(beforeRedirect = true, root=false)
    public String saveOrUpdate() {
    	String back=null; 
    	try {
    		currentProvider=providerService.findById(currentProvider.getId());
    		for(TradingLanguage tr : currentProvider.getTradingLanguages()){
        		if(tr.getLanguage().getLanguageCode().equalsIgnoreCase(entity.getLanguage().getLanguageCode())
        				&& !tr.getId().equals(entity.getId())){
        			throw new Exception();
        		}
    		}
    		currentProvider.addTradingLanguage(entity);
		    back=saveOrUpdate(entity); 
			
		} catch (Exception e) {
			statusMessages.addFromResourceBundle(Severity.ERROR,"tradingLanguage.uniqueField");
		}

		
		
    	   
    
        return back;
    }

	public void populateLanguages(Language language){
	      log.info("populatLanguages language", language!=null?language.getLanguageCode():null);
		  if(language!=null){
		      entity.setLanguage(language);
		      entity.setPrDescription(language.getDescriptionEn());
	     }
	}
    
    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<TradingLanguage> getPersistenceService() {
        return tradingLanguageService;
    }
}
