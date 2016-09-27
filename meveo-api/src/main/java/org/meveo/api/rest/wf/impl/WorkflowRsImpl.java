package org.meveo.api.rest.wf.impl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.payment.WorkflowDto;
import org.meveo.api.dto.wf.WorkflowResponseDto;
import org.meveo.api.dto.wf.WorkflowsResponseDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.api.rest.wf.WorkflowRs;
import org.meveo.api.wf.WorkflowApi;

@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class WorkflowRsImpl extends BaseRs implements WorkflowRs {

	 @Inject
	 private WorkflowApi workflowApi;
	 
	    @Override
	    public ActionStatus create(WorkflowDto workflowDto) {
	        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
	        try {
	        	workflowApi.create(workflowDto, getCurrentUser());
	        } catch (Exception e) {
	        	super.processException(e, result);
	        }
	        return result;
	    }

	    @Override
	    public ActionStatus update(WorkflowDto workflowDto) {
	        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
	        try {
	        	workflowApi.update(workflowDto, getCurrentUser());
	        } catch (Exception e) {
	        	super.processException(e, result);
	        }

	        return result;
	    }

	    @Override
	    public ActionStatus remove(String workflowCode) {
	        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
	        try {
	        	workflowApi.remove(workflowCode, getCurrentUser());
	        } catch (Exception e) {
	        	super.processException(e, result);
	        }
	        return result;
	    }

		@Override
		public ActionStatus createOrUpdate(WorkflowDto workflowDto) {
			ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
			try {
				workflowApi.createOrUpdate(workflowDto, getCurrentUser());
			} catch (Exception e) {
	        	super.processException(e, result);
	        }			
			return result;
		}

		@Override
		public WorkflowResponseDto find(String workflowCode) {
			WorkflowResponseDto workflowResponseDto = new WorkflowResponseDto();
			try {
				workflowResponseDto.setWorkflowDto(workflowApi.find(workflowCode, getCurrentUser()));
			} catch (Exception e) {
	        	super.processException(e, workflowResponseDto.getActionStatus());
	        }				
			return workflowResponseDto;
		}
		
		@Override
		public WorkflowsResponseDto list() {
			WorkflowsResponseDto workflowsResponseDto = new WorkflowsResponseDto();
			try {
				workflowsResponseDto.setListWorkflowDto(workflowApi.list(getCurrentUser()));
			} catch (Exception e) {
	        	super.processException(e, workflowsResponseDto.getActionStatus());
	        }
			
			return workflowsResponseDto;
		}

		@Override
		public ActionStatus execute(String baseEntityName, Long baseEntityInstanceId, String workflowCode) {
			ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
			try {
				workflowApi.execute( baseEntityName,  baseEntityInstanceId,  workflowCode,getCurrentUser() );
			} catch (Exception e) {
	        	super.processException(e, result);
	        }			
			return result;
		}

		@Override
		public WorkflowsResponseDto findByEntity(String baseEntityName) {
			WorkflowsResponseDto workflowsResponseDto = new WorkflowsResponseDto();
			try {
				workflowsResponseDto.setListWorkflowDto(workflowApi.findByEntity(baseEntityName,getCurrentUser()));
			} catch (Exception e) {
	        	super.processException(e, workflowsResponseDto.getActionStatus());
	        }			
			return workflowsResponseDto;
		}		
}
