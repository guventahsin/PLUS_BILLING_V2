package org.meveo.admin.job;

import java.math.BigDecimal;
import java.util.Arrays;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.api.dto.payment.DoPaymentResponseDto;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.service.payments.impl.PaymentService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.slf4j.Logger;

/**
 * 
 * @author anasseh
 */

@Stateless
public class UnitPaymentCardJobBean {

    @Inject
    private Logger log;

    @Inject
    private RecordedInvoiceService recordedInvoiceService;
    
    @Inject
    private PaymentService paymentService;

   
    // @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl result, Long aoId, boolean createAO, boolean matchingAO) {
        log.debug("Running with RecordedInvoice ID={}", aoId);

        RecordedInvoice recordedInvoice = null;
        try {
        	recordedInvoice = recordedInvoiceService.findById(aoId);
            if (recordedInvoice == null) {
                return;
            }
           DoPaymentResponseDto doPaymentResponseDto =  paymentService.doPaymentCardToken(recordedInvoice.getCustomerAccount(), recordedInvoice.getUnMatchingAmount().multiply(new BigDecimal("100")).longValue(), Arrays.asList(aoId), createAO, matchingAO);
           if(!StringUtils.isBlank(doPaymentResponseDto.getPaymentID())){
        	   result.registerSucces();
            }
            
        } catch (Exception e) {
        	log.error("Failed to pay recorded invoice id:"+aoId, e);
            result.registerError(aoId, e.getMessage());
            result.addReport("RecordedInvoice id : " + aoId + " RejectReason : " + e.getMessage());
            
        }
    }

   
}