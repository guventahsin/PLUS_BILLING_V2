package org.meveo.service.quote;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.CategoryInvoiceAgregate;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.OneShotChargeInstance;
import org.meveo.model.billing.ProductChargeInstance;
import org.meveo.model.billing.ProductInstance;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RecurringChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.quote.Quote;
import org.meveo.model.rating.EDR;
import org.meveo.model.rating.EDRStatusEnum;
import org.meveo.service.base.BusinessService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.billing.impl.InvoiceTypeService;
import org.meveo.service.billing.impl.OneShotChargeInstanceService;
import org.meveo.service.billing.impl.ProductChargeInstanceService;
import org.meveo.service.billing.impl.RatedTransactionService;
import org.meveo.service.billing.impl.RecurringChargeInstanceService;
import org.meveo.service.billing.impl.UsageRatingService;
import org.meveo.service.billing.impl.XMLInvoiceCreator;
import org.meveo.service.medina.impl.CDRParsingException;
import org.meveo.service.medina.impl.CDRParsingService;

@Stateless
public class QuoteService extends BusinessService<Quote> {

    @Inject
    private CDRParsingService cdrParsingService;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    private UsageRatingService usageRatingService;

    @Inject
    private RatedTransactionService ratedTransactionService;

    @Inject
    private XMLInvoiceCreator xmlInvoiceCreator;

    @Inject
    private OneShotChargeInstanceService oneShotChargeInstanceService;

    @Inject
    private ProductChargeInstanceService productChargeInstanceService;

    @Inject
    private RecurringChargeInstanceService recurringChargeInstanceService;

    @Inject
    private InvoiceTypeService invoiceTypeService;

    /**
     * Create a simulated invoice for quote
     * 
     * @param quoteCode Quote code
     * @param cdrs Cdrs for simulation
     * @param subscription Simulated subscription
     * @param productInstances Sumulated product instances
     * @param fromDate Date range for recuring charges - from
     * @param toDate Date range for recuring charges - to
     * @param currentUser Current user
     * @return
     * @throws BusinessException
     */
    @SuppressWarnings("unused")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Invoice provideQuote(List<QuoteInvoiceInfo> quoteInvoiceInfos, User currentUser) throws BusinessException {

        log.info("Creating simulated invoice for {}", quoteInvoiceInfos);
        List<RatedTransaction> ratedTransactions = new ArrayList<>();
        BillingAccount billingAccount = null;

        for (QuoteInvoiceInfo quoteInvoiceInfo : quoteInvoiceInfos) {

            List<WalletOperation> walletOperations = new ArrayList<>();

            // Add Product charges
            if (quoteInvoiceInfo.getProductInstances() != null) {
                for (ProductInstance productInstance : quoteInvoiceInfo.getProductInstances()) {
                    if (productInstance.getUserAccount() != null) {
                        billingAccount = productInstance.getUserAccount().getBillingAccount();
                    }
                    for (ProductChargeInstance productChargeInstance : productInstance.getProductChargeInstances()) {
                        walletOperations.addAll(productChargeInstanceService.applyProductChargeInstance(productChargeInstance, currentUser, true));
                    }
                }
            }

            if (quoteInvoiceInfo.getSubscription() != null) {
                billingAccount = quoteInvoiceInfo.getSubscription().getUserAccount().getBillingAccount();

                // Add subscription charges
                for (ServiceInstance serviceInstance : quoteInvoiceInfo.getSubscription().getServiceInstances()) {
                    for (OneShotChargeInstance subscriptionCharge : serviceInstance.getSubscriptionChargeInstances()) {
                        walletOperations.add(oneShotChargeInstanceService.oneShotChargeApplicationVirtual(quoteInvoiceInfo.getSubscription(), subscriptionCharge,
                            serviceInstance.getSubscriptionDate(), serviceInstance.getQuantity(), currentUser));
                    }

                    // Add recurring charges
                    for (RecurringChargeInstance recurringCharge : serviceInstance.getRecurringChargeInstances()) {
                        List<WalletOperation> walletOps = recurringChargeInstanceService.applyRecurringChargeVirtual(recurringCharge, quoteInvoiceInfo.getFromDate(),
                            quoteInvoiceInfo.getToDate(), currentUser);
                        if (walletOperations != null) {
                            walletOperations.addAll(walletOps);
                        }
                    }
                }

                // Process CDRS
                if (quoteInvoiceInfo.getCdrs() != null && !quoteInvoiceInfo.getCdrs().isEmpty() && quoteInvoiceInfo.getSubscription() != null) {

                    cdrParsingService.initByApi(currentUser.getUserName(), "quote");

                    List<EDR> edrs = new ArrayList<>();

                    // Parse CDRs to Edrs
                    try {
                        for (String cdr : quoteInvoiceInfo.getCdrs()) {
                            edrs.add(cdrParsingService.getEDRForVirtual(cdr, CDRParsingService.CDR_ORIGIN_API, quoteInvoiceInfo.getSubscription(), currentUser));
                        }

                    } catch (CDRParsingException e) {
                        log.error("Error parsing cdr={}", e.getRejectionCause());
                        throw new BusinessException(e.getRejectionCause().toString());
                    }

                    // Rate EDRs
                    for (EDR edr : edrs) {
                        log.debug("edr={}", edr);
                        List<WalletOperation> walletOperationsFromEdr = usageRatingService.rateUsageDontChangeTransaction(edr, true, currentUser);
                        if (edr.getStatus() == EDRStatusEnum.REJECTED) {
                            log.error("edr rejected={}", edr.getRejectReason());
                            throw new BusinessException(edr.getRejectReason());
                        }
                        walletOperations.addAll(walletOperationsFromEdr);
                    }
                }
            }

            // Create rated transactions from wallet operations
            for (WalletOperation walletOperation : walletOperations) {
                ratedTransactions.add(ratedTransactionService.createRatedTransaction(walletOperation, true, currentUser));
            }
        }
        Invoice invoice = invoiceService.createAgregatesAndInvoiceVirtual(ratedTransactions, billingAccount, invoiceTypeService.getDefaultQuote(currentUser), currentUser);

        File xmlInvoiceFile = xmlInvoiceCreator.createXMLInvoice(invoice, true);
        invoiceService.producePdf(invoice, true, currentUser);

        // Clean up data (left only the methods that remove FK data that would fail to persist in case of virtual operations)
        // invoice.setBillingAccount(null);
        invoice.setRatedTransactions(null);
        for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
            log.error("Invoice aggregate class {}", invoiceAgregate.getClass().getName());
            // invoiceAgregate.setBillingAccount(null);
            // invoiceAgregate.setTradingCurrency(null);
            // invoiceAgregate.setTradingLanguage(null);
            // invoiceAgregate.setBillingRun(null);
            // invoiceAgregate.setUserAccount(null);
            invoiceAgregate.setAuditable(null);
            invoiceAgregate.updateAudit(currentUser);
            if (invoiceAgregate instanceof CategoryInvoiceAgregate) {
                // ((CategoryInvoiceAgregate)invoiceAgregate).setInvoiceCategory(null);
                // ((CategoryInvoiceAgregate)invoiceAgregate).setSubCategoryInvoiceAgregates(null);
            } else if (invoiceAgregate instanceof TaxInvoiceAgregate) {
                // ((TaxInvoiceAgregate)invoiceAgregate).setTax(null);
            } else if (invoiceAgregate instanceof SubCategoryInvoiceAgregate) {
                // ((SubCategoryInvoiceAgregate)invoiceAgregate).setInvoiceSubCategory(null);
                // ((SubCategoryInvoiceAgregate)invoiceAgregate).setSubCategoryTaxes(null);
                // ((SubCategoryInvoiceAgregate)invoiceAgregate).setCategoryInvoiceAgregate(null);
                ((SubCategoryInvoiceAgregate) invoiceAgregate).setWallet(null);
                ((SubCategoryInvoiceAgregate) invoiceAgregate).setRatedtransactions(null);
            }
        }

        invoiceService.create(invoice, currentUser);
        return invoice;
    }
}