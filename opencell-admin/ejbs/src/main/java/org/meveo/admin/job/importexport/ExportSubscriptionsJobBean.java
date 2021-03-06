package org.meveo.admin.job.importexport;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.xml.bind.JAXBException;

import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.commons.utils.JAXBUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.crm.Provider;
import org.meveo.model.jaxb.customer.CustomFields;
import org.meveo.model.jaxb.subscription.Accesses;
import org.meveo.model.jaxb.subscription.Services;
import org.meveo.model.jaxb.subscription.Status;
import org.meveo.model.jaxb.subscription.Subscription;
import org.meveo.model.jaxb.subscription.Subscriptions;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.job.JobExecutionService;
import org.meveo.util.ApplicationProvider;
import org.slf4j.Logger;

@Stateless
public class ExportSubscriptionsJobBean {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmss");

    @Inject
    private Logger log;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    @ApplicationProvider
    protected Provider appProvider;

    @Inject
    private JobExecutionService jobExecutionService;

    private Subscriptions subscriptions;
    private ParamBean param = ParamBean.getInstance();

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl result, String parameter) {

        String exportDir = param.getProperty("providers.rootDir", "./opencelldata/") + File.separator + appProvider.getCode() + File.separator + "exports" + File.separator
                + "subscriptions" + File.separator;
        log.info("exportDir=" + exportDir);
        File dir = new File(exportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = sdf.format(new Date());
        List<org.meveo.model.billing.Subscription> subs = subscriptionService.list();
        subscriptions = subscriptionsToDto(subs, param.getProperty("connectorCRM.dateFormat", "yyyy-MM-dd"), result.getJobInstance().getId());
        try {
            JAXBUtils.marshaller(subscriptions, new File(dir + File.separator + "SUB_" + timestamp + ".xml"));
        } catch (JAXBException e) {
            log.error("Failed to export subscriptions job", e);
        }

    }

    private Subscriptions subscriptionsToDto(List<org.meveo.model.billing.Subscription> subs, String dateFormat, Long jobInstanceId) {
        Subscriptions dto = new Subscriptions();
        if (subs != null) {
            for (org.meveo.model.billing.Subscription sub : subs) {
                if (!jobExecutionService.isJobRunningOnThis(jobInstanceId)) {
                    break;
                }
                dto.getSubscription().add(subscriptionToDto(sub, dateFormat));
            }
        }
        return dto;
    }

    private Subscription subscriptionToDto(org.meveo.model.billing.Subscription sub, String dateFormat) {
        Subscription dto = new Subscription();
        if (sub != null) {
            dto.setSubscriptionDate(sub.getSubscriptionDate() == null ? null : DateUtils.formatDateWithPattern(sub.getSubscriptionDate(), dateFormat));
            dto.setEndAgreementDate(sub.getEndAgreementDate() == null ? null : DateUtils.formatDateWithPattern(sub.getEndAgreementDate(), dateFormat));
            dto.setDescription(sub.getDescription());
            if (sub.getCfValues() != null) {
                dto.setCustomFields(CustomFields.toDTO(sub.getCfValues().getValuesByCode()));
            }
            dto.setCode(sub.getCode());
            dto.setUserAccountId(sub.getUserAccount() == null ? null : sub.getUserAccount().getCode());
            dto.setOfferCode(sub.getOffer() == null ? null : sub.getOffer().getCode());
            dto.setStatus(new Status(sub, dateFormat));
            dto.setServices(new Services(sub, dateFormat));
            dto.setAccesses(new Accesses(sub, dateFormat));
        }
        return dto;
    }
}
