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
package org.meveo.service.billing.impl;

import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.AccountEntity;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingCycle;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.CategoryInvoiceAgregate;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.billing.XMLInvoiceHeaderCategoryDTO;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.crm.CustomFieldInstance;
import org.meveo.model.crm.Customer;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.CustomerAccountStatusEnum;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.model.rating.EDR;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.catalog.impl.CatMessagesService;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

@Stateless
public class XMLInvoiceCreator extends PersistenceService<Invoice> {

	private ParamBean paramBean = ParamBean.getInstance();

	private String dueDateFormat = "yyyy-MM-dd";

	@Inject
	private InvoiceService invoiceService;

	@Inject
	private RatedTransactionService ratedTransactionService;

	@Inject
	private CatMessagesService catMessagesService;

	@Inject
	private InvoiceAgregateService invoiceAgregateService;

	@Inject
	private BillingAccountService billingAccountService;

	TransformerFactory transfac = TransformerFactory.newInstance();

	public void createXMLInvoiceAdjustment(Long invoiceId, File billingRundir) throws BusinessException {
		createXMLInvoice(invoiceId, billingRundir, true);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void createXMLInvoice(Long invoiceId, File billingRundir) throws BusinessException {
		createXMLInvoice(invoiceId, billingRundir, false);
	}

	public void createXMLInvoice(Long invoiceId, File billingRundir, boolean isInvoiceAdjustment)
			throws BusinessException {
		// log.debug("creating xml invoice...");

		try {
			Invoice invoice = findById(invoiceId);
			String billingAccountLanguage = invoice.getBillingAccount().getTradingLanguage().getLanguage()
					.getLanguageCode();

			boolean entreprise = invoice.getProvider().isEntreprise();
			int rounding = invoice.getProvider().getRounding() == null ? 2 : invoice.getProvider().getRounding();

			if (!isInvoiceAdjustment && invoice.getBillingRun() != null
					&& BillingRunStatusEnum.VALIDATED.equals(invoice.getBillingRun().getStatus())
					&& invoice.getInvoiceNumber() == null) {
				invoiceService.setInvoiceNumber(invoice);
			}
			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			Element invoiceTag = doc.createElement("invoice");
			Element header = doc.createElement("header");
			invoiceTag.setAttribute("number", invoice.getInvoiceNumber());
			invoiceTag.setAttribute("type", invoice.getInvoiceTypeEnum().name());
			invoiceTag.setAttribute("invoiceCounter", invoice.getAlias());
			invoiceTag.setAttribute("id", invoice.getId().toString());
			invoiceTag.setAttribute("customerId", invoice.getBillingAccount().getCustomerAccount().getCustomer()
					.getCode()
					+ "");
			invoiceTag.setAttribute("customerAccountCode",
					invoice.getBillingAccount().getCustomerAccount().getCode() != null ? invoice.getBillingAccount()
							.getCustomerAccount().getCode() : "");
			if (invoice.getAdjustedInvoice() != null) {
				invoiceTag.setAttribute("adjustedInvoiceNumber", invoice.getAdjustedInvoice().getInvoiceNumber());
			}

			BillingCycle billingCycle = null;
			if (isInvoiceAdjustment && invoice.getAdjustedInvoice().getBillingRun() != null) {
				billingCycle = invoice.getAdjustedInvoice().getBillingRun().getBillingCycle();
			} else {
				if (invoice.getBillingRun() != null && invoice.getBillingRun().getBillingCycle() != null) {
					billingCycle = invoice.getBillingRun().getBillingCycle();
				}
			}

			invoiceTag.setAttribute("templateName", billingCycle != null
					&& billingCycle.getBillingTemplateName() != null ? billingCycle.getBillingTemplateName()
					: "default");
			doc.appendChild(invoiceTag);
			invoiceTag.appendChild(header);
			// log.debug("creating provider");
			Provider provider = invoice.getProvider();
			if (provider.getInvoiceConfiguration() != null
					&& provider.getInvoiceConfiguration().getDisplayProvider() != null
					&& provider.getInvoiceConfiguration().getDisplayProvider()) {
				Element providerTag = doc.createElement("provider");
				providerTag.setAttribute("code", provider.getCode() + "");
				Element bankCoordinates = doc.createElement("bankCoordinates");
				Element ics = doc.createElement("ics");
				Element iban = doc.createElement("iban");
				Element bic = doc.createElement("bic");
				bankCoordinates.appendChild(ics);
				bankCoordinates.appendChild(iban);
				bankCoordinates.appendChild(bic);
				providerTag.appendChild(bankCoordinates);
				header.appendChild(providerTag);

				if (provider.getBankCoordinates() != null) {
					Text icsTxt = doc.createTextNode(provider.getBankCoordinates().getIcs() != null ? provider
							.getBankCoordinates().getIcs() : "");
					ics.appendChild(icsTxt);
					Text ibanTxt = doc.createTextNode(provider.getBankCoordinates().getIban() != null ? provider
							.getBankCoordinates().getIban() : "");
					iban.appendChild(ibanTxt);
					Text bicTxt = doc.createTextNode(provider.getBankCoordinates().getBic() != null ? provider
							.getBankCoordinates().getBic() : "");
					bic.appendChild(bicTxt);
				}
			}

			// log.debug("creating customer");
			Customer customer = invoice.getBillingAccount().getCustomerAccount().getCustomer();
			Element customerTag = doc.createElement("customer");
			customerTag.setAttribute("id", customer.getId() + "");
			customerTag.setAttribute("code", customer.getCode() + "");
			customerTag.setAttribute("externalRef1", customer.getExternalRef1() != null ? customer.getExternalRef1()
					: "");
			customerTag.setAttribute("externalRef2", customer.getExternalRef2() != null ? customer.getExternalRef2()
					: "");
			customerTag.setAttribute("sellerCode", customer.getSeller().getCode() != null ? customer.getSeller()
					.getCode() : "");
			customerTag.setAttribute("brand", customer.getCustomerBrand() != null ? customer.getCustomerBrand()
					.getCode() : "");
			customerTag.setAttribute("category", customer.getCustomerCategory() != null ? customer
					.getCustomerCategory().getCode() : "");
			if (PaymentMethodEnum.DIRECTDEBIT.equals(invoice.getBillingAccount().getPaymentMethod())) {
				customerTag.setAttribute("mandateIdentification",
						customer.getMandateIdentification() != null ? customer.getMandateIdentification() : "");
			}
			String json = customer.getCustomFieldsAsJson();
			if (json.length() > 0) {
				customerTag.setAttribute("customFields", customer.getCustomFieldsAsJson());
			}
			header.appendChild(customerTag);
			addNameAndAdress(customer, doc, customerTag, billingAccountLanguage);

			// log.debug("creating ca");
			CustomerAccount customerAccount = invoice.getBillingAccount().getCustomerAccount();
			Element customerAccountTag = doc.createElement("customerAccount");
			customerAccountTag.setAttribute("id", customerAccount.getId() + "");
			customerAccountTag.setAttribute("code", customerAccount.getCode() + "");
			customerAccountTag.setAttribute("description", customerAccount.getDescription() + "");
			customerAccountTag.setAttribute("externalRef1",
					customerAccount.getExternalRef1() != null ? customerAccount.getExternalRef1() : "");
			customerAccountTag.setAttribute("externalRef2",
					customerAccount.getExternalRef2() != null ? customerAccount.getExternalRef2() : "");
			customerAccountTag.setAttribute("currency",
					customerAccount.getTradingCurrency().getPrDescription() != null ? customerAccount
							.getTradingCurrency().getPrDescription() : "");
			customerAccountTag.setAttribute("language",
					customerAccount.getTradingLanguage().getPrDescription() != null ? customerAccount
							.getTradingLanguage().getPrDescription() : "");
			if (PaymentMethodEnum.DIRECTDEBIT.equals(invoice.getBillingAccount().getPaymentMethod())) {
				customerAccountTag.setAttribute("mandateIdentification",
						customerAccount.getMandateIdentification() != null ? customerAccount.getMandateIdentification()
								: "");
			}
			json = customerAccount.getCustomFieldsAsJson();
			if (json.length() > 0) {
				customerAccountTag.setAttribute("customFields", customerAccount.getCustomFieldsAsJson());
			}
			header.appendChild(customerAccountTag);

			/*
			 * EntityManager em = getEntityManager(); Query billingQuery = em
			 * .createQuery(
			 * "select si from ServiceInstance si join si.subscription s join s.userAccount ua join ua.billingAccount ba join ba.customerAccount ca where ca.id = :customerAccountId"
			 * ); billingQuery.setParameter("customerAccountId",
			 * customerAccount.getId()); List<ServiceInstance> services =
			 * (List<ServiceInstance>) billingQuery .getResultList();
			 * 
			 * 
			 * 
			 * boolean terminated = services.size() > 0 ?
			 * isAllServiceInstancesTerminated(services) : false;
			 */

			customerAccountTag.setAttribute("accountTerminated",
					customerAccount.getStatus().equals(CustomerAccountStatusEnum.CLOSE) + "");

			header.appendChild(customerAccountTag);
			addNameAndAdress(customerAccount, doc, customerAccountTag, billingAccountLanguage);
			addproviderContact(customerAccount, doc, customerAccountTag);

			// log.debug("creating ba");
			BillingAccount billingAccount = invoice.getBillingAccount();
			Element billingAccountTag = doc.createElement("billingAccount");
			if (billingCycle == null) {
				billingCycle = billingAccount.getBillingCycle();
			}
			String billingCycleCode = billingCycle != null ? billingCycle.getCode() + "" : "";
			billingAccountTag.setAttribute("billingCycleCode", billingCycleCode);
			String billingAccountId = billingAccount.getId() + "";
			String billingAccountCode = billingAccount.getCode() + "";
			billingAccountTag.setAttribute("id", billingAccountId);
			billingAccountTag.setAttribute("code", billingAccountCode);
			billingAccountTag.setAttribute("description", billingAccount.getDescription() + "");
			billingAccountTag.setAttribute("externalRef1",
					billingAccount.getExternalRef1() != null ? billingAccount.getExternalRef1() : "");
			billingAccountTag.setAttribute("externalRef2",
					billingAccount.getExternalRef2() != null ? billingAccount.getExternalRef2() : "");
			json = billingAccount.getCustomFieldsAsJson();
			if (json.length() > 0) {
				billingAccountTag.setAttribute("customFields", billingAccount.getCustomFieldsAsJson());
			}
			header.appendChild(billingAccountTag);

			/*
			 * if (billingAccount.getName() != null &&
			 * billingAccount.getName().getTitle() != null) { // Element company
			 * = doc.createElement("company"); Text companyTxt =
			 * doc.createTextNode
			 * (billingAccount.getName().getTitle().getIsCompany() + "");
			 * billingAccountTag.appendChild(companyTxt); }
			 */

			Element email = doc.createElement("email");
			Text emailTxt = doc.createTextNode(billingAccount.getEmail() != null ? billingAccount.getEmail() : "");
			email.appendChild(emailTxt);
			billingAccountTag.appendChild(email);

			addNameAndAdress(billingAccount, doc, billingAccountTag, billingAccountLanguage);

			addPaymentInfo(billingAccount, doc, billingAccountTag);

			if (invoice.getInvoiceDate() != null) {
				Element invoiceDate = doc.createElement("invoiceDate");
				Text invoiceDateTxt = doc.createTextNode(DateUtils.formatDateWithPattern(invoice.getInvoiceDate(),
						"dd/MM/yyyy"));
				invoiceDate.appendChild(invoiceDateTxt);
				header.appendChild(invoiceDate);
			}

			if (invoice.getDueDate() != null) {
				Element dueDate = doc.createElement("dueDate");
				Text dueDateTxt = doc.createTextNode(DateUtils.formatDateWithPattern(invoice.getDueDate(),
						dueDateFormat));
				dueDate.appendChild(dueDateTxt);
				header.appendChild(dueDate);
			}

			Element comment = doc.createElement("comment");
			Comment commentText = doc.createComment(invoice.getComment() != null ? invoice.getComment() : "");
			comment.appendChild(commentText);
			header.appendChild(comment);

			addHeaderCategories(invoice, doc, header);
			addDiscounts(invoice, doc, header);

			Element amount = doc.createElement("amount");
			invoiceTag.appendChild(amount);

			Element currency = doc.createElement("currency");
			Text currencyTxt = doc.createTextNode(invoice.getBillingAccount().getCustomerAccount().getTradingCurrency()
					.getCurrencyCode());
			currency.appendChild(currencyTxt);
			amount.appendChild(currency);

			Element amountWithoutTax = doc.createElement("amountWithoutTax");
			Text amountWithoutTaxTxt = doc.createTextNode(round(invoice.getAmountWithoutTax(), rounding));
			amountWithoutTax.appendChild(amountWithoutTaxTxt);
			amount.appendChild(amountWithoutTax);

			Element amountWithTax = doc.createElement("amountWithTax");
			Text amountWithTaxTxt = doc.createTextNode(round(invoice.getAmountWithTax(), rounding));
			amountWithTax.appendChild(amountWithTaxTxt);
			amount.appendChild(amountWithTax);

			BigDecimal netToPay = BigDecimal.ZERO;
			if (entreprise) {
				netToPay = invoice.getAmountWithTax();
			} else {
				netToPay = invoice.getNetToPay();
			}

			/*
			 * Element balanceElement = doc.createElement("balance"); Text
			 * balanceTxt = doc.createTextNode(round(balance));
			 * balanceElement.appendChild(balanceTxt);
			 * amount.appendChild(balanceElement);
			 */

			Element netToPayElement = doc.createElement("netToPay");
			Text netToPayTxt = doc.createTextNode(round(netToPay, rounding));
			netToPayElement.appendChild(netToPayTxt);
			amount.appendChild(netToPayElement);

			addTaxes(invoice, doc, amount);

			Element detail = null;
			boolean displayDetail = false;
			if (provider.getInvoiceConfiguration() != null
					&& provider.getInvoiceConfiguration().getDisplayDetail() != null
					&& provider.getInvoiceConfiguration().getDisplayDetail() && invoice.isDetailedInvoice()) {
				displayDetail = true;

				detail = doc.createElement("detail");
				invoiceTag.appendChild(detail);
			}

			addUserAccounts(invoice, doc, detail, entreprise, invoiceTag, displayDetail);

			Transformer trans = transfac.newTransformer();
			// trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			// create string from xml tree
			DOMSource source = new DOMSource(doc);

			if (isInvoiceAdjustment) {
				StreamResult result = new StreamResult(billingRundir + File.separator
						+ paramBean.getProperty("invoicing.invoiceAdjustment.prefix", "_IA_")
						+ invoice.getInvoiceNumber() + ".xml");
				billingRundir.mkdirs();

				StringWriter writer = new StringWriter();
				trans.transform(new DOMSource(doc), new StreamResult(writer));
				log.debug(writer.getBuffer().toString().replaceAll("\n|\r", ""));

				trans.transform(source, result);
			} else {
				StreamResult result = new StreamResult(billingRundir
						+ File.separator
						+ (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber()
								: invoice.getTemporaryInvoiceNumber()) + ".xml");
				billingRundir.mkdirs();
				trans.transform(source, result);
			}

		} catch (TransformerException e) {
			log.error("Error occured when creating xml for invoiceID={}. {}", invoiceId, e);
		} catch (ParserConfigurationException e) {
			log.error("Error occured when creating xml for invoiceID={}. {}", invoiceId, e);
		}

	}

	public void addUserAccounts(Invoice invoice, Document doc, Element parent, boolean enterprise, Element invoiceTag,
			boolean displayDetail) {
		// log.debug("add user account");

		Element userAccountsTag = null;
		if (displayDetail) {
			userAccountsTag = doc.createElement("userAccounts");
			parent.appendChild(userAccountsTag);
		}

		BillingAccount billingAccount = invoice.getBillingAccount();
		String billingAccountLanguage = billingAccount.getTradingLanguage().getLanguage().getLanguageCode();

		for (UserAccount userAccount : billingAccount.getUsersAccounts()) {
			Element userAccountTag = doc.createElement("userAccount");
			userAccountTag.setAttribute("id", userAccount.getId() + "");
			userAccountTag.setAttribute("code", userAccount.getCode() != null ? userAccount.getCode() : "");
			userAccountTag.setAttribute("description",
					userAccount.getDescription() != null ? userAccount.getDescription() : "");
			String json = userAccount.getCustomFieldsAsJson();
			if (json.length() > 0) {
				userAccountTag.setAttribute("customFields", userAccount.getCustomFieldsAsJson());
			}

			if (displayDetail) {
				userAccountsTag.appendChild(userAccountTag);
				addNameAndAdress(userAccount, doc, userAccountTag, billingAccountLanguage);
				addCategories(userAccount, invoice, doc, userAccountTag, invoice.getProvider()
						.getInvoiceConfiguration().getDisplayDetail(), enterprise);
			}

			addSubscriptions(userAccount, invoice, doc, invoiceTag);
		}

	}

	private void addSubscriptions(UserAccount userAccount, Invoice invoice, Document doc, Element invoiceTag) {
		if (userAccount.getSubscriptions() != null && userAccount.getSubscriptions().size() > 0) {

			Element subscriptionsTag = null;
			if (invoice.getProvider().getInvoiceConfiguration() != null
					&& invoice.getProvider().getInvoiceConfiguration().getDisplaySubscriptions() != null
					&& invoice.getProvider().getInvoiceConfiguration().getDisplaySubscriptions()) {
				subscriptionsTag = doc.createElement("subscriptions");
				invoiceTag.appendChild(subscriptionsTag);
			}

			for (Subscription subscription : userAccount.getSubscriptions()) {
				if (invoice.getProvider().getInvoiceConfiguration() != null
						&& invoice.getProvider().getInvoiceConfiguration().getDisplaySubscriptions() != null
						&& invoice.getProvider().getInvoiceConfiguration().getDisplaySubscriptions()) {
					Element subscriptionTag = doc.createElement("subscription");
					subscriptionTag.setAttribute("id", subscription.getId() + "");
					subscriptionTag.setAttribute("code", subscription.getCode() != null ? subscription.getCode() : "");
					subscriptionTag.setAttribute("description",
							subscription.getDescription() != null ? subscription.getDescription() : "");

					Element subscriptionDateTag = doc.createElement("subscriptionDate");
					Text subscriptionDateText = null;
					if (subscription.getSubscriptionDate() != null) {
						subscriptionDateText = doc.createTextNode(subscription.getSubscriptionDate().toString());
					} else {
						subscriptionDateText = doc.createTextNode("");
					}
					subscriptionDateTag.appendChild(subscriptionDateText);
					subscriptionTag.appendChild(subscriptionDateTag);

					Element endAgreementTag = doc.createElement("endAgreementDate");
					Text endAgreementText = null;
					if (subscription.getEndAgrementDate() != null) {
						endAgreementText = doc.createTextNode(subscription.getEndAgrementDate().toString());
					} else {
						endAgreementText = doc.createTextNode("");
					}
					endAgreementTag.appendChild(endAgreementText);
					subscriptionTag.appendChild(endAgreementTag);

					if (subscription.getCustomFields() != null && subscription.getCustomFields().size() > 0) {
						addCustomFields(subscription, invoice, doc, subscriptionTag);
					}

					subscriptionsTag.appendChild(subscriptionTag);
				}

				if (subscription.getOffer() != null) {
					OfferTemplate offerTemplate = subscription.getOffer();
					if (invoice.getProvider().getInvoiceConfiguration() != null
							&& invoice.getProvider().getInvoiceConfiguration().getDisplayOffers() != null
							&& invoice.getProvider().getInvoiceConfiguration().getDisplayOffers()) {
						addOffers(offerTemplate, invoice, doc, invoiceTag);
					}

					if (invoice.getProvider().getInvoiceConfiguration() != null
							&& invoice.getProvider().getInvoiceConfiguration().getDisplayServices() != null
							&& invoice.getProvider().getInvoiceConfiguration().getDisplayServices()) {
						addServices(offerTemplate, invoice, doc, invoiceTag);
					}
				}
			}
		}
	}

	private void addOffers(OfferTemplate offerTemplate, Invoice invoice, Document doc, Element invoiceTag) {
		NodeList offerList = doc.getElementsByTagName("offers");

		Element offersTag = null;
		if (offerList != null && offerList.getLength() > 0) {
			offersTag = (Element) offerList.item(0);
		} else {
			offersTag = doc.createElement("offers");
			invoiceTag.appendChild(offersTag);
		}

		Element offerTag = doc.createElement("offer");
		offerTag.setAttribute("id", offerTemplate.getId() + "");
		offerTag.setAttribute("code", offerTemplate.getCode() != null ? offerTemplate.getCode() : "");
		offerTag.setAttribute("description", offerTemplate.getDescription() != null ? offerTemplate.getDescription()
				: "");
		offersTag.appendChild(offerTag);
	}

	private void addServices(OfferTemplate offerTemplate, Invoice invoice, Document doc, Element invoiceTag) {
		if (offerTemplate.getServiceTemplates() != null && offerTemplate.getServiceTemplates().size() > 0) {
			NodeList serviceList = doc.getElementsByTagName("services");

			Element servicesTag = null;
			if (serviceList != null && serviceList.getLength() > 0) {
				servicesTag = (Element) serviceList.item(0);
			} else {
				servicesTag = doc.createElement("services");
				invoiceTag.appendChild(servicesTag);
			}

			for (ServiceTemplate serviceTemplate : offerTemplate.getServiceTemplates()) {
				Element serviceTag = doc.createElement("service");
				serviceTag.setAttribute("id", serviceTemplate.getId() + "");
				serviceTag.setAttribute("code", serviceTemplate.getCode() != null ? serviceTemplate.getCode() : "");
				serviceTag.setAttribute("description",
						serviceTemplate.getDescription() != null ? serviceTemplate.getDescription() : "");

				Element calendarTag = doc.createElement("calendar");
				Text calendarText = null;
				if (serviceTemplate.getInvoicingCalendar() != null) {
					calendarText = doc.createTextNode(serviceTemplate.getInvoicingCalendar().getCode());
				} else {
					calendarText = doc.createTextNode("");
				}
				calendarTag.appendChild(calendarText);

				servicesTag.appendChild(serviceTag);
			}
		}
	}

	private void addCustomFields(Subscription subscription, Invoice invoice, Document doc, Element parent) {
		Iterator<String> keys = subscription.getCustomFields().keySet().iterator();

		Element customFieldsTag = doc.createElement("customFields");
		parent.appendChild(customFieldsTag);

		while (keys.hasNext()) {
			String key = keys.next();
			CustomFieldInstance cfi = subscription.getCustomFields().get(key);

			if (!StringUtils.isBlank(cfi.getValueAsString())) {
				Element customFieldTag = doc.createElement("customField");
				customFieldTag.setAttribute("id", cfi.getId() + "");
				customFieldTag.setAttribute("code", cfi.getCode() != null ? cfi.getCode() : "");

				Text customFieldText = doc.createTextNode(cfi.getValueAsString());
				customFieldTag.appendChild(customFieldText);

				customFieldsTag.appendChild(customFieldTag);
			}
		}
	}

	public void addNameAndAdress(AccountEntity account, Document doc, Element parent, String languageCode) {
		log.debug("add name and address");

		if (!(account instanceof Customer)) {
			Element nameTag = doc.createElement("name");
			parent.appendChild(nameTag);

			Element quality = doc.createElement("quality");
			if (account.getName().getTitle() != null) {
				Text qualityTxt = doc.createTextNode(catMessagesService.getMessageDescription(account.getName()
						.getTitle(), languageCode));
				quality.appendChild(qualityTxt);
			}
			nameTag.appendChild(quality);
			if (account.getName().getFirstName() != null) {
				Element firstName = doc.createElement("firstName");
				Text firstNameTxt = doc.createTextNode(account.getName().getFirstName());
				firstName.appendChild(firstNameTxt);
				nameTag.appendChild(firstName);
			}

			Element name = doc.createElement("name");
			if (account.getName().getLastName() != null) {
				Text nameTxt = doc.createTextNode(account.getName().getLastName());
				name.appendChild(nameTxt);
			}
			nameTag.appendChild(name);
		}
		Element addressTag = doc.createElement("address");
		Element address1 = doc.createElement("address1");
		if (account.getAddress().getAddress1() != null) {
			Text adress1Txt = doc.createTextNode(account.getAddress().getAddress1());
			address1.appendChild(adress1Txt);
		}
		addressTag.appendChild(address1);

		Element address2 = doc.createElement("address2");
		if (account.getAddress().getAddress2() != null) {
			Text adress2Txt = doc.createTextNode(account.getAddress().getAddress2());
			address2.appendChild(adress2Txt);
		}
		addressTag.appendChild(address2);

		Element address3 = doc.createElement("address3");
		if (account.getAddress().getAddress3() != null) {
			Text adress3Txt = doc.createTextNode(account.getAddress().getAddress3() != null ? account.getAddress()
					.getAddress3() : "");
			address3.appendChild(adress3Txt);
		}
		addressTag.appendChild(address3);

		Element city = doc.createElement("city");
		Text cityTxt = doc.createTextNode(account.getAddress().getCity() != null ? account.getAddress().getCity() : "");
		city.appendChild(cityTxt);
		addressTag.appendChild(city);

		Element postalCode = doc.createElement("postalCode");
		Text postalCodeTxt = doc.createTextNode(account.getAddress().getZipCode() != null ? account.getAddress()
				.getZipCode() : "");
		postalCode.appendChild(postalCodeTxt);
		addressTag.appendChild(postalCode);

		Element state = doc.createElement("state");
		addressTag.appendChild(state);

		Element country = doc.createElement("country");
		Text countryTxt = doc.createTextNode(account.getAddress().getCountry() != null ? account.getAddress()
				.getCountry() : "");
		country.appendChild(countryTxt);
		addressTag.appendChild(country);

		parent.appendChild(addressTag);
	}

	public void addproviderContact(AccountEntity account, Document doc, Element parent) {

		// log.debug("add provider");

		if (account.getPrimaryContact() != null) {
			Element providerContactTag = doc.createElement("providerContact");
			parent.appendChild(providerContactTag);
			if (account.getPrimaryContact().getFirstName() != null) {
				Element firstName = doc.createElement("firstName");
				Text firstNameTxt = doc.createTextNode(account.getPrimaryContact().getFirstName());
				firstName.appendChild(firstNameTxt);
				providerContactTag.appendChild(firstName);
			}

			if (account.getPrimaryContact().getLastName() != null) {
				Element name = doc.createElement("lastname");
				Text nameTxt = doc.createTextNode(account.getPrimaryContact().getLastName());
				name.appendChild(nameTxt);
				providerContactTag.appendChild(name);
			}

			if (account.getPrimaryContact().getEmail() != null) {
				Element email = doc.createElement("email");
				Text emailTxt = doc.createTextNode(account.getPrimaryContact().getEmail());
				email.appendChild(emailTxt);
				providerContactTag.appendChild(email);
			}
			if (account.getPrimaryContact().getFax() != null) {
				Element fax = doc.createElement("fax");
				Text faxTxt = doc.createTextNode(account.getPrimaryContact().getFax());
				fax.appendChild(faxTxt);
				providerContactTag.appendChild(fax);

			}
			if (account.getPrimaryContact().getMobile() != null) {

				Element mobile = doc.createElement("mobile");
				Text mobileTxt = doc.createTextNode(account.getPrimaryContact().getMobile());
				mobile.appendChild(mobileTxt);
				providerContactTag.appendChild(mobile);
			}
			if (account.getPrimaryContact().getPhone() != null) {
				Element phone = doc.createElement("phone");
				Text phoneTxt = doc.createTextNode(account.getPrimaryContact().getPhone());
				phone.appendChild(phoneTxt);
				providerContactTag.appendChild(phone);
			}
		}

	}

	public void addPaymentInfo(BillingAccount billingAccount, Document doc, Element parent) {

		// log.debug("add payment info");

		Element paymentMethod = doc.createElement("paymentMethod");
		parent.appendChild(paymentMethod);
		if (billingAccount.getPaymentMethod() != null) {
			paymentMethod.setAttribute("type", billingAccount.getPaymentMethod().name());
		}

		Element bankCoordinates = doc.createElement("bankCoordinates");
		Element bankCode = doc.createElement("bankCode");
		Element branchCode = doc.createElement("branchCode");
		Element accountNumber = doc.createElement("accountNumber");
		Element accountOwner = doc.createElement("accountOwner");
		Element key = doc.createElement("key");
		Element iban = doc.createElement("IBAN");
		bankCoordinates.appendChild(bankCode);
		bankCoordinates.appendChild(branchCode);
		bankCoordinates.appendChild(accountNumber);
		bankCoordinates.appendChild(accountOwner);
		bankCoordinates.appendChild(key);
		bankCoordinates.appendChild(iban);
		paymentMethod.appendChild(bankCoordinates);

		Element paymentTerm = doc.createElement("paymentTerm");
		parent.appendChild(paymentTerm);
		paymentTerm.setAttribute("type", billingAccount.getPaymentTerm() != null ? billingAccount.getPaymentTerm()
				.toString() : "");

		if (billingAccount.getBankCoordinates() != null && billingAccount.getBankCoordinates().getBankCode() != null) {
			Text bankCodeTxt = doc
					.createTextNode(billingAccount.getBankCoordinates().getBankCode() != null ? billingAccount
							.getBankCoordinates().getBankCode() : "");
			bankCode.appendChild(bankCodeTxt);

			Text branchCodeTxt = doc
					.createTextNode(billingAccount.getBankCoordinates().getBranchCode() != null ? billingAccount
							.getBankCoordinates().getBranchCode() : "");
			branchCode.appendChild(branchCodeTxt);

			Text accountNumberTxt = doc
					.createTextNode(billingAccount.getBankCoordinates().getAccountNumber() != null ? billingAccount
							.getBankCoordinates().getAccountNumber() : "");
			accountNumber.appendChild(accountNumberTxt);

			Text accountOwnerTxt = doc
					.createTextNode(billingAccount.getBankCoordinates().getAccountOwner() != null ? billingAccount
							.getBankCoordinates().getAccountOwner() : "");
			accountOwner.appendChild(accountOwnerTxt);

			Text keyTxt = doc.createTextNode(billingAccount.getBankCoordinates().getKey() != null ? billingAccount
					.getBankCoordinates().getKey() : "");
			key.appendChild(keyTxt);
			if (billingAccount.getBankCoordinates().getIban() != null) {
				Text ibanTxt = doc
						.createTextNode(billingAccount.getBankCoordinates().getIban() != null ? billingAccount
								.getBankCoordinates().getIban() : "");
				iban.appendChild(ibanTxt);
			}

		}
	}

	public void addCategories(UserAccount userAccount, Invoice invoice, Document doc, Element parent,
			boolean generateSubCat, boolean enterprise) {

		log.debug("add categories");

		long startDate = System.currentTimeMillis();
		String languageCode = invoice.getBillingAccount().getTradingLanguage().getLanguage().getLanguageCode();

		Element categories = doc.createElement("categories");
		parent.appendChild(categories);
		boolean entreprise = invoice.getProvider().isEntreprise();
		int rounding = invoice.getProvider().getRounding() == null ? 2 : invoice.getProvider().getRounding();

		List<CategoryInvoiceAgregate> categoryInvoiceAgregates = new ArrayList<CategoryInvoiceAgregate>();

		for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
			if (invoiceAgregate.getUserAccount().getId() == userAccount.getId()) {
				if (invoiceAgregate instanceof CategoryInvoiceAgregate) {
					CategoryInvoiceAgregate categoryInvoiceAgregate = (CategoryInvoiceAgregate) invoiceAgregate;
					categoryInvoiceAgregates.add(categoryInvoiceAgregate);
				}
			}
		}

		Collections.sort(categoryInvoiceAgregates, new Comparator<CategoryInvoiceAgregate>() {
			public int compare(CategoryInvoiceAgregate c0, CategoryInvoiceAgregate c1) {
				if (c0.getInvoiceCategory() != null && c1.getInvoiceCategory() != null
						&& c0.getInvoiceCategory().getSortIndex() != null
						&& c1.getInvoiceCategory().getSortIndex() != null) {
					return c0.getInvoiceCategory().getSortIndex().compareTo(c1.getInvoiceCategory().getSortIndex());
				}
				return 0;
			}
		});

		for (CategoryInvoiceAgregate categoryInvoiceAgregate : categoryInvoiceAgregates) {

			InvoiceCategory invoiceCategory = categoryInvoiceAgregate.getInvoiceCategory();

			String invoiceCategoryLabel = invoiceCategory != null ? catMessagesService.getMessageDescription(
					invoiceCategory, languageCode) : "";
			Element category = doc.createElement("category");
			category.setAttribute("label", (invoiceCategoryLabel != null) ? invoiceCategoryLabel : "");
			category.setAttribute("code",
					invoiceCategory != null && invoiceCategory.getCode() != null ? invoiceCategory.getCode() : "");
			categories.appendChild(category);
			Element amountWithoutTax = doc.createElement("amountWithoutTax");
			Text amountWithoutTaxTxt = doc
					.createTextNode(round(categoryInvoiceAgregate.getAmountWithoutTax(), rounding));
			amountWithoutTax.appendChild(amountWithoutTaxTxt);
			category.appendChild(amountWithoutTax);

			// if (!entreprise) {
			// Element amountWithTax = doc.createElement("amountWithTax");
			// Text amountWithTaxTxt =
			// doc.createTextNode(round(categoryInvoiceAgregate.getAmountWithTax(),
			// rounding));
			// amountWithTax.appendChild(amountWithTaxTxt);
			// category.appendChild(amountWithTax);
			// }
			if (generateSubCat) {
				Element subCategories = doc.createElement("subCategories");
				category.appendChild(subCategories);
				Set<SubCategoryInvoiceAgregate> subCategoryInvoiceAgregates = categoryInvoiceAgregate
						.getSubCategoryInvoiceAgregates();

				for (SubCategoryInvoiceAgregate subCatInvoiceAgregate : subCategoryInvoiceAgregates) {
					InvoiceSubCategory invoiceSubCat = subCatInvoiceAgregate.getInvoiceSubCategory();
					List<RatedTransaction> transactions = ratedTransactionService.getRatedTransactions(
							subCatInvoiceAgregate.getWallet(), subCatInvoiceAgregate.getInvoice(),
							subCatInvoiceAgregate.getInvoiceSubCategory());

					String invoiceSubCategoryLabel = invoiceSubCat != null ? catMessagesService.getMessageDescription(
							invoiceSubCat, languageCode) : "";
					Element subCategory = doc.createElement("subCategory");
					subCategories.appendChild(subCategory);
					subCategory.setAttribute("label", (invoiceSubCategoryLabel != null) ? invoiceSubCategoryLabel : "");
					subCategory.setAttribute("code", invoiceSubCat.getCode());
					subCategory.setAttribute("amountWithoutTax",
							round(subCatInvoiceAgregate.getAmountWithoutTax(), rounding));

					if (!entreprise) {
						subCategory.setAttribute("amountWithTax",
								round(subCatInvoiceAgregate.getAmountWithTax(), rounding));
					}

					String taxesCode = "";
					String taxesPercent = "";
					String sep = "";
					for (Tax tax : subCatInvoiceAgregate.getSubCategoryTaxes()) {
						taxesCode = taxesCode + sep + tax.getCode();
						taxesPercent = taxesPercent + sep + round(tax.getPercent(), rounding);
						sep = ";";
					}
					subCategory.setAttribute("taxCode", taxesCode);
					subCategory.setAttribute("taxPercent", taxesPercent);

					for (RatedTransaction ratedTransaction : transactions) {
						BigDecimal transactionAmount = entreprise ? ratedTransaction.getAmountWithTax()
								: ratedTransaction.getAmountWithoutTax();
						if (transactionAmount == null) {
							transactionAmount = BigDecimal.ZERO;
						}

						Element line = doc.createElement("line");
						String code = "", description = "";
						WalletOperation walletOperation = null;
						if (ratedTransaction.getWalletOperationId() != null) {
							walletOperation = getEntityManager().find(WalletOperation.class,
									ratedTransaction.getWalletOperationId());
							code = walletOperation.getCode();
							description = walletOperation.getDescription();
						} else {
							code = ratedTransaction.getCode();
							description = ratedTransaction.getDescription();
						}

						line.setAttribute("code", code != null ? code : "");

						if (ratedTransaction.getParameter1() != null) {
							line.setAttribute("param1", ratedTransaction.getParameter1());
						}
						if (ratedTransaction.getParameter2() != null) {
							line.setAttribute("param2", ratedTransaction.getParameter2());
						}
						if (ratedTransaction.getParameter3() != null) {
							line.setAttribute("param3", ratedTransaction.getParameter3());
						}

						if (ratedTransaction.getPriceplan() != null) {
							Element pricePlan = doc.createElement("pricePlan");
							pricePlan.setAttribute("code", ratedTransaction.getPriceplan().getCode());
							pricePlan.setAttribute("description", catMessagesService.getMessageDescription(
									ratedTransaction.getPriceplan(), languageCode));
							line.appendChild(pricePlan);
						}

						Element lebel = doc.createElement("label");
						Text lebelTxt = doc.createTextNode(description != null ? description : "");

						lebel.appendChild(lebelTxt);
						line.appendChild(lebel);

						if (!StringUtils.isBlank(ratedTransaction.getUnityDescription())) {
							Element lineUnit = doc.createElement("unit");
							Text lineUnitTxt = doc.createTextNode(ratedTransaction.getUnityDescription());
							lineUnit.appendChild(lineUnitTxt);
							line.appendChild(lineUnit);
						}
						Element lineUnitAmountWithoutTax = doc.createElement("unitAmountWithoutTax");
						Text lineUnitAmountWithoutTaxTxt = doc.createTextNode(ratedTransaction
								.getUnitAmountWithoutTax().toPlainString());
						lineUnitAmountWithoutTax.appendChild(lineUnitAmountWithoutTaxTxt);
						line.appendChild(lineUnitAmountWithoutTax);

						Element lineAmountWithoutTax = doc.createElement("amountWithoutTax");
						Text lineAmountWithoutTaxTxt = doc.createTextNode(round(ratedTransaction.getAmountWithoutTax(),
								rounding));
						lineAmountWithoutTax.appendChild(lineAmountWithoutTaxTxt);
						line.appendChild(lineAmountWithoutTax);

						if (!enterprise) {
							Element lineAmountWithTax = doc.createElement("amountWithTax");
							Text lineAmountWithTaxTxt = doc.createTextNode(round(ratedTransaction.getAmountWithTax(),
									rounding));
							lineAmountWithTax.appendChild(lineAmountWithTaxTxt);
							line.appendChild(lineAmountWithTax);
						}

						Element quantity = doc.createElement("quantity");
						Text quantityTxt = doc.createTextNode(ratedTransaction.getQuantity() != null ? ratedTransaction
								.getQuantity().toPlainString() : "");
						quantity.appendChild(quantityTxt);
						line.appendChild(quantity);

						Element usageDate = doc.createElement("usageDate");
						Text usageDateTxt = doc.createTextNode(ratedTransaction.getUsageDate() != null ? DateUtils
								.formatDateWithPattern(ratedTransaction.getUsageDate(), "dd/MM/yyyy") + "" : "");
						usageDate.appendChild(usageDateTxt);
						line.appendChild(usageDate);
						EDR edr = ratedTransaction.getEdr();
						if (ratedTransaction.getProvider().getInvoiceConfiguration() != null
								&& ratedTransaction.getProvider().getInvoiceConfiguration().getDisplayEdrs() != null
								&& ratedTransaction.getProvider().getInvoiceConfiguration().getDisplayEdrs()
								&& edr != null) {
							Element edrInfo = doc.createElement("edr");
							edrInfo.setAttribute("originRecord", edr.getOriginRecord() != null ? edr.getOriginRecord()
									: "");
							edrInfo.setAttribute("originBatch", edr.getOriginBatch() != null ? edr.getOriginBatch()
									: "");
							edrInfo.setAttribute("quantity", edr.getQuantity() != null ? edr.getQuantity()
									.toPlainString() : "");
							edrInfo.setAttribute("status",
									String.valueOf(edr.getStatus()) != null ? String.valueOf(edr.getStatus()) : "");
							edrInfo.setAttribute("rejectReason", edr.getRejectReason() != null ? edr.getRejectReason()
									: "");
							edrInfo.setAttribute("subscription", edr.getSubscription() != null ? edr.getSubscription()
									.getDescription() : "");
							edrInfo.setAttribute(
									"eventDate",
									edr.getEventDate() != null ? DateUtils.formatDateWithPattern(edr.getEventDate(),
											"dd/MM/yyyy") + "" : "");
							edrInfo.setAttribute("accessCode", edr.getAccessCode() != null ? edr.getAccessCode() : "");
							edrInfo.setAttribute("parameter1", edr.getParameter1() != null ? edr.getParameter1() : "");
							edrInfo.setAttribute("parameter2", edr.getParameter2() != null ? edr.getParameter2() : "");
							edrInfo.setAttribute("parameter3", edr.getParameter3() != null ? edr.getParameter3() : "");
							edrInfo.setAttribute("parameter4", edr.getParameter4() != null ? edr.getParameter4() : "");
							edrInfo.setAttribute("parameter5", edr.getParameter5() != null ? edr.getParameter5() : "");
							edrInfo.setAttribute("parameter6", edr.getParameter6() != null ? edr.getParameter6() : "");
							edrInfo.setAttribute("parameter7", edr.getParameter7() != null ? edr.getParameter7() : "");
							edrInfo.setAttribute("parameter8", edr.getParameter8() != null ? edr.getParameter8() : "");
							edrInfo.setAttribute("parameter9", edr.getParameter9() != null ? edr.getParameter9() : "");
							edrInfo.setAttribute(
									"dateParam1",
									edr.getDateParam1() != null ? DateUtils.formatDateWithPattern(edr.getDateParam1(),
											"dd/MM/yyyy") + "" : "");
							edrInfo.setAttribute(
									"dateParam2",
									edr.getDateParam2() != null ? DateUtils.formatDateWithPattern(edr.getDateParam2(),
											"dd/MM/yyyy") + "" : "");
							edrInfo.setAttribute(
									"dateParam3",
									edr.getDateParam3() != null ? DateUtils.formatDateWithPattern(edr.getDateParam3(),
											"dd/MM/yyyy") + "" : "");
							edrInfo.setAttribute(
									"dateParam4",
									edr.getDateParam4() != null ? DateUtils.formatDateWithPattern(edr.getDateParam4(),
											"dd/MM/yyyy") + "" : "");
							edrInfo.setAttribute(
									"dateParam5",
									edr.getDateParam5() != null ? DateUtils.formatDateWithPattern(edr.getDateParam5(),
											"dd/MM/yyyy") + "" : "");
							edrInfo.setAttribute("decimalParam1", edr.getDecimalParam1() != null ? edr
									.getDecimalParam1().toPlainString() : "");
							edrInfo.setAttribute("decimalParam2", edr.getDecimalParam2() != null ? edr
									.getDecimalParam2().toPlainString() : "");
							edrInfo.setAttribute("decimalParam3", edr.getDecimalParam3() != null ? edr
									.getDecimalParam3().toPlainString() : "");
							edrInfo.setAttribute("decimalParam4", edr.getDecimalParam4() != null ? edr
									.getDecimalParam4().toPlainString() : "");
							edrInfo.setAttribute("decimalParam5", edr.getDecimalParam5() != null ? edr
									.getDecimalParam5().toPlainString() : "");
							line.appendChild(edrInfo);
						}

						subCategory.appendChild(line);

					}
				}
			}
		}

		log.info("addCategorries time: " + (System.currentTimeMillis() - startDate));
	}

	private void addTaxes(Invoice invoice, Document doc, Element parent) throws BusinessException {
		// log.debug("adding taxes...");
		Element taxes = doc.createElement("taxes");		
		boolean exoneratedFromTaxes = billingAccountService.isExonerated(invoice.getBillingAccount());
		
		if(exoneratedFromTaxes){
			Element exoneratedElement = doc.createElement("exonerated");			
			exoneratedElement.setAttribute("reason",invoice.getBillingAccount().getCustomerAccount().getCustomer().getCustomerCategory().getExonerationReason() );
			taxes.appendChild(exoneratedElement);
		}else{			
			int rounding = invoice.getProvider().getRounding() == null ? 2 : invoice.getProvider().getRounding();
			taxes.setAttribute("total", round(invoice.getAmountTax(), rounding));			
			Map<Long, TaxInvoiceAgregate> taxInvoiceAgregateMap = new HashMap<Long, TaxInvoiceAgregate>();
			for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
				if (invoiceAgregate instanceof TaxInvoiceAgregate) {
					TaxInvoiceAgregate taxInvoiceAgregate = (TaxInvoiceAgregate) invoiceAgregate;
					TaxInvoiceAgregate taxAgregate = null;
	
					if (taxInvoiceAgregateMap.containsKey(taxInvoiceAgregate.getTax().getId())) {
						taxAgregate = taxInvoiceAgregateMap.get(taxInvoiceAgregate.getTax().getId());
						taxAgregate.setAmountTax(taxAgregate.getAmountTax().add(taxInvoiceAgregate.getAmountTax()));
						taxAgregate.setAmountWithoutTax(taxAgregate.getAmountWithoutTax().add(
								taxInvoiceAgregate.getAmountWithoutTax()));
					} else {
						taxAgregate = new TaxInvoiceAgregate();
						taxAgregate.setTaxPercent(taxInvoiceAgregate.getTaxPercent());
						taxAgregate.setTax(taxInvoiceAgregate.getTax());
						taxAgregate.setAmountTax(taxInvoiceAgregate.getAmountTax());
						taxAgregate.setAmountWithoutTax(taxInvoiceAgregate.getAmountWithoutTax());
						taxInvoiceAgregateMap.put(taxInvoiceAgregate.getTax().getId(), taxAgregate);
					}
				}
	
			}
	
			int taxId = 0;
			for (TaxInvoiceAgregate taxInvoiceAgregate : taxInvoiceAgregateMap.values()) {
	
				Element tax = doc.createElement("tax");
	
				tax.setAttribute("id", ++taxId + "");
				tax.setAttribute("code", taxInvoiceAgregate.getTax().getCode() + "");
	
				String languageCode = "";
				try {
					// log.debug("ba={}, tradingLanguage={}",
					// invoice.getBillingAccount(),
					// invoice.getBillingAccount().getTradingLanguage());
					languageCode = invoice.getBillingAccount().getTradingLanguage().getLanguage().getLanguageCode();
				} catch (NullPointerException e) {
					log.error("Billing account must have a trading language.");
					throw new BusinessException("Billing account must have a trading language.");
				}
	
				String taxDescription = catMessagesService.getMessageDescription(taxInvoiceAgregate.getTax(), languageCode);
				Element taxName = doc.createElement("name");
				Text taxNameTxt = doc.createTextNode(taxDescription != null ? taxDescription : "");
				taxName.appendChild(taxNameTxt);
				tax.appendChild(taxName);
	
				Element percent = doc.createElement("percent");
				Text percentTxt = doc.createTextNode(round(taxInvoiceAgregate.getTaxPercent(), rounding));
				percent.appendChild(percentTxt);
				tax.appendChild(percent);
	
				Element taxAmount = doc.createElement("amount");
				Text amountTxt = doc.createTextNode(round(taxInvoiceAgregate.getAmountTax(), rounding));
				taxAmount.appendChild(amountTxt);
				tax.appendChild(taxAmount);
	
				Element amountHT = doc.createElement("amountHT");
				Text amountHTTxt = doc.createTextNode(round(taxInvoiceAgregate.getAmountWithoutTax(), rounding));
				amountHT.appendChild(amountHTTxt);
				tax.appendChild(amountHT);
	
				taxes.appendChild(tax);
			}
		}
		parent.appendChild(taxes);
	}

	private void addHeaderCategories(Invoice invoice, Document doc, Element parent) {
		// log.debug("add header categories");

		boolean entreprise = invoice.getProvider().isEntreprise();
		LinkedHashMap<String, XMLInvoiceHeaderCategoryDTO> headerCategories = new LinkedHashMap<String, XMLInvoiceHeaderCategoryDTO>();
		List<CategoryInvoiceAgregate> categoryInvoiceAgregates = new ArrayList<CategoryInvoiceAgregate>();
		for (InvoiceAgregate invoiceAgregate : invoice.getInvoiceAgregates()) {
			if (invoiceAgregate instanceof CategoryInvoiceAgregate) {
				CategoryInvoiceAgregate categoryInvoiceAgregate = (CategoryInvoiceAgregate) invoiceAgregate;
				categoryInvoiceAgregates.add(categoryInvoiceAgregate);
			}
		}
		Collections.sort(categoryInvoiceAgregates, new Comparator<CategoryInvoiceAgregate>() {
			public int compare(CategoryInvoiceAgregate c0, CategoryInvoiceAgregate c1) {
				if (c0.getInvoiceCategory() != null && c1.getInvoiceCategory() != null
						&& c0.getInvoiceCategory().getSortIndex() != null
						&& c1.getInvoiceCategory().getSortIndex() != null) {
					return c0.getInvoiceCategory().getSortIndex().compareTo(c1.getInvoiceCategory().getSortIndex());
				}
				return 0;
			}
		});

		for (CategoryInvoiceAgregate categoryInvoiceAgregate : categoryInvoiceAgregates) {
			InvoiceCategory invoiceCategory = categoryInvoiceAgregate.getInvoiceCategory();
			XMLInvoiceHeaderCategoryDTO headerCat = null;
			if (headerCategories.containsKey(invoiceCategory.getCode())) {
				headerCat = headerCategories.get(invoiceCategory.getCode());
				headerCat.addAmountWithoutTax(categoryInvoiceAgregate.getAmountWithoutTax());
				headerCat.addAmountWithTax(categoryInvoiceAgregate.getAmountWithTax());
			} else {
				headerCat = new XMLInvoiceHeaderCategoryDTO();
				headerCat.setDescription(invoiceCategory.getDescription());
				headerCat.setCode(invoiceCategory.getCode());
				headerCat.setAmountWithoutTax(categoryInvoiceAgregate.getAmountWithoutTax());
				headerCat.setAmountWithTax(categoryInvoiceAgregate.getAmountWithTax());

				Set<SubCategoryInvoiceAgregate> subCategoryInvoiceAgregates = categoryInvoiceAgregate
						.getSubCategoryInvoiceAgregates();
				for (SubCategoryInvoiceAgregate subCatInvoiceAgregate : subCategoryInvoiceAgregates) {
					headerCat.getSubCategoryInvoiceAgregates().add(subCatInvoiceAgregate);
				}

				headerCategories.put(invoiceCategory.getCode(), headerCat);
			}

		}
		addHeaderCategories(invoice, headerCategories, doc, parent, entreprise, invoice.getProvider());

		// log.info("addHeaderCategories time: " + (System.currentTimeMillis() -
		// startDate));
	}

	private void addHeaderCategories(Invoice invoice,
			LinkedHashMap<String, XMLInvoiceHeaderCategoryDTO> headerCategories, Document doc, Element parent,
			boolean entreprise, Provider provider) {

		int rounding = provider.getRounding() == null ? 2 : provider.getRounding();
		String languageCode = invoice.getBillingAccount().getTradingLanguage().getLanguage().getLanguageCode();

		// log.debug("add header categories");

		Element categories = doc.createElement("categories");
		parent.appendChild(categories);
		for (XMLInvoiceHeaderCategoryDTO xmlInvoiceHeaderCategoryDTO : headerCategories.values()) {

			Element category = doc.createElement("category");
			category.setAttribute("label", xmlInvoiceHeaderCategoryDTO.getDescription());
			category.setAttribute("code", xmlInvoiceHeaderCategoryDTO != null
					&& xmlInvoiceHeaderCategoryDTO.getCode() != null ? xmlInvoiceHeaderCategoryDTO.getCode() : "");
			categories.appendChild(category);

			Element amountWithoutTax = doc.createElement("amountWithoutTax");
			Text amountWithoutTaxTxt = doc.createTextNode(round(xmlInvoiceHeaderCategoryDTO.getAmountWithoutTax(),
					rounding));
			amountWithoutTax.appendChild(amountWithoutTaxTxt);
			category.appendChild(amountWithoutTax);

			if (xmlInvoiceHeaderCategoryDTO.getSubCategoryInvoiceAgregates() != null) {
				Element subCategories = doc.createElement("subCategories");
				category.appendChild(subCategories);

				for (SubCategoryInvoiceAgregate subCatInvoiceAgregate : xmlInvoiceHeaderCategoryDTO
						.getSubCategoryInvoiceAgregates()) {
					Element subCategory = doc.createElement("subCategory");

					InvoiceSubCategory invoiceSubCat = subCatInvoiceAgregate.getInvoiceSubCategory();

					String invoiceSubCategoryLabel = invoiceSubCat != null ? catMessagesService.getMessageDescription(
							invoiceSubCat, languageCode) : "";

					subCategories.appendChild(subCategory);
					subCategory.setAttribute("label", (invoiceSubCategoryLabel != null) ? invoiceSubCategoryLabel : "");
					subCategory.setAttribute("code", invoiceSubCat.getCode());
					String taxesCode = "";
					String taxesPercent = "";
					String sep = "";
					for (Tax tax : subCatInvoiceAgregate.getSubCategoryTaxes()) {
						taxesCode = taxesCode + sep + tax.getCode();
						taxesPercent = taxesPercent + sep + round(tax.getPercent(), rounding);
						sep = ";";
					}
					subCategory.setAttribute("taxCode", taxesCode);
					subCategory.setAttribute("taxPercent", taxesPercent);

					if (!entreprise) {
						subCategory.setAttribute("amountWithTax",
								round(subCatInvoiceAgregate.getAmountWithTax(), rounding));
					}

					subCategory.setAttribute("amountWithoutTax",
							round(subCatInvoiceAgregate.getAmountWithoutTax(), rounding));
					// subCategory.setAttribute("taxAmount",
					// round(subCatInvoiceAgregate.getAmountTax(), rounding));
				}
			}
		}
	}

	private void addDiscounts(Invoice invoice, Document doc, Element parent) {
		int rounding = invoice.getProvider().getRounding() == null ? 2 : invoice.getProvider().getRounding();
		Element discounts = doc.createElement("discounts");

		parent.appendChild(discounts);
		for (SubCategoryInvoiceAgregate subCategoryInvoiceAgregate : invoiceAgregateService
				.findDiscountAggregates(invoice)) {

			Element discount = doc.createElement("discount");
			discount.setAttribute("discountPlanCode", subCategoryInvoiceAgregate.getDiscountPlanCode());
			discount.setAttribute("discountPlanItemCode", subCategoryInvoiceAgregate.getDiscountPlanItemCode());
			discount.setAttribute("invoiceSubCategoryCode", subCategoryInvoiceAgregate.getInvoiceSubCategory()
					.getCode());
			discount.setAttribute("discountAmountWithoutTax",
					round(subCategoryInvoiceAgregate.getAmountWithoutTax(), rounding) + "");
			discount.setAttribute("discountPercent", round(subCategoryInvoiceAgregate.getDiscountPercent(), rounding)
					+ "");

			discounts.appendChild(discount);

		}

	}

	private String round(BigDecimal amount, Integer scale) {
		if (amount == null) {
			amount = BigDecimal.ZERO;
		}
		if (scale == null) {
			scale = 2;
		}
		amount = amount.setScale(scale, RoundingMode.HALF_UP);
		return amount.toPlainString();
	}

	@SuppressWarnings("unused")
	private boolean isAllServiceInstancesTerminated(List<ServiceInstance> serviceInstances) {
		for (ServiceInstance service : serviceInstances) {
			boolean serviceActive = service.getStatus() == InstanceStatusEnum.ACTIVE;
			if (serviceActive) {
				return false;
			}
		}
		return true;
	}

}