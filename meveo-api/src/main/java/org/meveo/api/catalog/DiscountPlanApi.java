package org.meveo.api.catalog;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.api.BaseApi;
import org.meveo.api.dto.catalog.DiscountPlanDto;
import org.meveo.api.dto.catalog.DiscountPlansDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.catalog.DiscountPlan;
import org.meveo.model.crm.Provider;
import org.meveo.service.catalog.impl.DiscountPlanService;

/**
 * 
 * 
 *
 */
@Stateless
public class DiscountPlanApi extends BaseApi{

	@Inject
	private DiscountPlanService discountPlanService;
	
	/**
	 * creates a discount plan
	 * @param postData
	 * @param currentUser
	 * @throws MeveoApiException
	 */
	public void create(DiscountPlanDto postData, User currentUser) throws MeveoApiException {
		
		if (!StringUtils.isBlank(postData.getCode()) && !StringUtils.isBlank(postData.getDescription())) {
			
			if (discountPlanService.findByCode(postData.getCode(), currentUser.getProvider()) != null) {
				throw new EntityAlreadyExistsException(DiscountPlan.class, postData.getCode());
			}
			
			DiscountPlan discountPlan = new DiscountPlan();
			discountPlan.setCode(postData.getCode());
			discountPlan.setDescription(postData.getDescription());
			
			discountPlanService.create(discountPlan, currentUser, currentUser.getProvider());
		} else {
			if (StringUtils.isBlank(postData.getCode())) {
				missingParameters.add("code");
			}
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		
	}
	
	/**
	 * updates the description of an existing discount plan
	 * @param postData
	 * @param currentUser
	 * @throws MeveoApiException
	 */
	public void update(DiscountPlanDto postData, User currentUser) throws MeveoApiException {
		
		if (!StringUtils.isBlank(postData.getCode()) && !StringUtils.isBlank(postData.getDescription())) {
			
			DiscountPlan discountPlan = discountPlanService.findByCode(postData.getCode(), currentUser.getProvider());
			
			if (discountPlan == null) {
				throw new EntityDoesNotExistsException(DiscountPlan.class, postData.getCode());
			}
			discountPlan.setDescription(postData.getDescription());
			
			discountPlanService.update(discountPlan, currentUser);
		} else {
			if (StringUtils.isBlank(postData.getCode())) {
				missingParameters.add("code");
			}
			
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		
	}
	
	/**
	 * retrieves a discount plan based on code
	 * @param discountPlanCode
	 * @param provider
	 * @return
	 * @throws MeveoApiException
	 */
	public DiscountPlanDto find(String discountPlanCode, Provider provider) throws MeveoApiException {
		DiscountPlanDto discountPlanDto = new DiscountPlanDto();
		
		if (!StringUtils.isBlank(discountPlanCode)) {
			DiscountPlan discountPlan = discountPlanService.findByCode(discountPlanCode, provider);
			if (discountPlan == null) {
				throw new EntityDoesNotExistsException(DiscountPlan.class, discountPlanCode);
			}
			
			discountPlanDto.setCode(discountPlan.getCode());
			discountPlanDto.setDescription(discountPlan.getDescription());
			
		} else {
			if (StringUtils.isBlank(discountPlanCode)) {
				missingParameters.add("code");
			}
			
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		
		return discountPlanDto;
	}
	
	/**
	 * deletes a discount plan based on code
	 * @param discountPlanCode
	 * @param provider
	 * @throws MeveoApiException
	 */
	public void remove(String discountPlanCode, Provider provider) throws MeveoApiException {
		
		
		if (!StringUtils.isBlank(discountPlanCode)) {
			DiscountPlan discountPlan = discountPlanService.findByCode(discountPlanCode, provider);
			if (discountPlan == null) {
				throw new EntityDoesNotExistsException(DiscountPlan.class, discountPlanCode);
			}
			
			discountPlanService.remove(discountPlan);
		} else {
			if (StringUtils.isBlank(discountPlanCode)) {
				missingParameters.add("code");
			}
			
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		
	}
	
	/**
	 * creates if the the discount plan code is not existing, updates if exists
	 * @param postData
	 * @param currentUser
	 * @throws MeveoApiException
	 */
	public void createOrUpdate(DiscountPlanDto postData, User currentUser) throws MeveoApiException {
		
		String discountPlanCode = postData.getCode();
		
		if (!StringUtils.isBlank(discountPlanCode)) {
			
			if (discountPlanService.findByCode(discountPlanCode, currentUser.getProvider()) == null) {
				create(postData, currentUser);
			} else {
				update(postData, currentUser);
			}
			
		} else {
			if (StringUtils.isBlank(discountPlanCode)) {
				missingParameters.add("code");
			}
			
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		
		
	}
	
	/**
	 * retrieves all discount plan of the user
	 * @param provider
	 * @return
	 * @throws MeveoApiException
	 */
	public DiscountPlansDto list(Provider provider) throws MeveoApiException {
		
		DiscountPlansDto discountPlansDto = null;
		List<DiscountPlan> discountPlans = discountPlanService.list(provider);
		
		if (discountPlans != null && !discountPlans.isEmpty()) {
			discountPlansDto = new DiscountPlansDto();
			List<DiscountPlanDto> discountPlanDtos = new ArrayList<DiscountPlanDto>();
			for (DiscountPlan dp: discountPlans) {
				DiscountPlanDto dpd = new DiscountPlanDto();
				dpd.setCode(dp.getCode());
				dpd.setDescription(dp.getDescription());
				discountPlanDtos.add(dpd);
			}
			discountPlansDto.setDiscountPlan(discountPlanDtos);
		}
		
		return discountPlansDto;
	}
}