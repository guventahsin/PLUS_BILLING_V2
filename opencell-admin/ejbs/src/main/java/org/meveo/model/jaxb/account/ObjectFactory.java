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
 * along with this program.  If not, see <http://www.gnu.org/licenses/&gt;.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.02.01 at 06:47:58 PM WET 
//


package org.meveo.model.jaxb.account;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.meveo.model.jaxb.account package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _BIC_QNAME = new QName("", "BIC");
    private final static QName _State_QNAME = new QName("", "state");
    private final static QName _Address1_QNAME = new QName("", "address1");
    private final static QName _BankName_QNAME = new QName("", "bankName");
    private final static QName _Address2_QNAME = new QName("", "address2");
    private final static QName _Firstname_QNAME = new QName("", "firstname");
    private final static QName _Address3_QNAME = new QName("", "address3");
    private final static QName _BranchCode_QNAME = new QName("", "branchCode");
    private final static QName _IBAN_QNAME = new QName("", "IBAN");
    private final static QName _BankCode_QNAME = new QName("", "bankCode");
    private final static QName _Country_QNAME = new QName("", "country");
    private final static QName _City_QNAME = new QName("", "city");
    private final static QName _ExternalRef2_QNAME = new QName("", "externalRef2");
    private final static QName _AccountNumber_QNAME = new QName("", "accountNumber");
    private final static QName _AccountName_QNAME = new QName("", "accountName");
    private final static QName _Title_QNAME = new QName("", "title");
    private final static QName _ElectronicBilling_QNAME = new QName("", "electronicBilling");
    private final static QName _ExternalRef1_QNAME = new QName("", "externalRef1");
    private final static QName _Email_QNAME = new QName("", "email");
    private final static QName _Description_QNAME = new QName("", "description");
    private final static QName _Company_QNAME = new QName("", "company");
    private final static QName _ZipCode_QNAME = new QName("", "zipCode");
    private final static QName _SubscriptionDate_QNAME = new QName("", "subscriptionDate");
    private final static QName _Key_QNAME = new QName("", "key");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.meveo.model.jaxb.account.
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Address }
     * @return address..
     * 
     */
    public Address createAddress() {
        return new Address();
    }

    /**
     * Create an instance of {@link ErrorUserAccount }.
     * @return error user account.
     * 
     */
    public ErrorUserAccount createErrorUserAccount() {
        return new ErrorUserAccount();
    }

    /**
     * Create an instance of {@link Name }.
     * @return name.
     * 
     */
    public Name createName() {
        return new Name();
    }

    /**
     * Create an instance of {@link UserAccount }.
     * @return user account.
     * 
     */
    public UserAccount createUserAccount() {
        return new UserAccount();
    }

    /**
     * Create an instance of {@link Errors }.
     * @return errors.
     * 
     */
    public Errors createErrors() {
        return new Errors();
    }

    /**
     * Create an instance of {@link BankCoordinates }.
     * @return bank coordinates.
     * 
     */
    public BankCoordinates createBankCoordinates() {
        return new BankCoordinates();
    }

    /**
     * Create an instance of {@link UserAccounts }.
     * @return user accounts.
     * 
     */
    public UserAccounts createUserAccounts() {
        return new UserAccounts();
    }

    /**
     * Create an instance of {@link WarningBillingAccount }.
     * @return warning billing account.
     * 
     */
    public WarningBillingAccount createWarningBillingAccount() {
        return new WarningBillingAccount();
    }

    /**
     * Create an instance of {@link ErrorBillingAccount }.
     * @return error billing account.
     * 
     */
    public ErrorBillingAccount createErrorBillingAccount() {
        return new ErrorBillingAccount();
    }

    /**
     * Create an instance of {@link BillingAccounts }.
     * @return billing accounts.
     * 
     */
    public BillingAccounts createBillingAccounts() {
        return new BillingAccounts();
    }

    /**
     * Create an instance of {@link WarningUserAccount }.
     * @return warning user account.
     * 
     */
    public WarningUserAccount createWarningUserAccount() {
        return new WarningUserAccount();
    }

    /**
     * Create an instance of {@link Warnings }.
     * @return warnings
     * 
     */
    public Warnings createWarnings() {
        return new Warnings();
    }

    /**
     * Create an instance of {@link BillingAccount }.
     * @return billing account.
     * 
     */
    public BillingAccount createBillingAccount() {
        return new BillingAccount();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value bic 
     * @return jaxb bic value.
     * 
     */
    @XmlElementDecl(namespace = "", name = "BIC")
    public JAXBElement<String> createBIC(String value) {
        return new JAXBElement<String>(_BIC_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value state
     * @return jaxb state
     * 
     */
    @XmlElementDecl(namespace = "", name = "state")
    public JAXBElement<String> createState(String value) {
        return new JAXBElement<String>(_State_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value address 1
     * @return jaxb address 1.
     * 
     */
    @XmlElementDecl(namespace = "", name = "address1")
    public JAXBElement<String> createAddress1(String value) {
        return new JAXBElement<String>(_Address1_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value bank name
     * @return jaxb bank name.
     * 
     */
    @XmlElementDecl(namespace = "", name = "bankName")
    public JAXBElement<String> createBankName(String value) {
        return new JAXBElement<String>(_BankName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value address 2
     * @return jaxb address 2.
     * 
     */
    @XmlElementDecl(namespace = "", name = "address2")
    public JAXBElement<String> createAddress2(String value) {
        return new JAXBElement<String>(_Address2_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value first name.
     * @return jaxb first name.
     * 
     */
    @XmlElementDecl(namespace = "", name = "firstname")
    public JAXBElement<String> createFirstname(String value) {
        return new JAXBElement<String>(_Firstname_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value addresse 3
     * @return jabx address 3.
     * 
     */
    @XmlElementDecl(namespace = "", name = "address3")
    public JAXBElement<String> createAddress3(String value) {
        return new JAXBElement<String>(_Address3_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value branch code
     * @return jabx branch code.
     * 
     */
    @XmlElementDecl(namespace = "", name = "branchCode")
    public JAXBElement<String> createBranchCode(String value) {
        return new JAXBElement<String>(_BranchCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value iban number
     * @return jaxb iban
     * 
     */
    @XmlElementDecl(namespace = "", name = "IBAN")
    public JAXBElement<String> createIBAN(String value) {
        return new JAXBElement<String>(_IBAN_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value bank code
     * @return jabx bank code.
     * 
     */
    @XmlElementDecl(namespace = "", name = "bankCode")
    public JAXBElement<String> createBankCode(String value) {
        return new JAXBElement<String>(_BankCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value country
     * @return jaxb country.
     * 
     */
    @XmlElementDecl(namespace = "", name = "country")
    public JAXBElement<String> createCountry(String value) {
        return new JAXBElement<String>(_Country_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value city name
     * @return jaxb city name
     * 
     */
    @XmlElementDecl(namespace = "", name = "city")
    public JAXBElement<String> createCity(String value) {
        return new JAXBElement<String>(_City_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value external reference 2
     * @return jaxb external reference 2
     * 
     */
    @XmlElementDecl(namespace = "", name = "externalRef2")
    public JAXBElement<String> createExternalRef2(String value) {
        return new JAXBElement<String>(_ExternalRef2_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value account number
     * @return jaxb account nmber.
     * 
     */
    @XmlElementDecl(namespace = "", name = "accountNumber")
    public JAXBElement<String> createAccountNumber(String value) {
        return new JAXBElement<String>(_AccountNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value account name.
     * @return jaxb account name.
     * 
     */
    @XmlElementDecl(namespace = "", name = "accountName")
    public JAXBElement<String> createAccountName(String value) {
        return new JAXBElement<String>(_AccountName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @value title
     * @return jaxb title
     * 
     */
    @XmlElementDecl(namespace = "", name = "title")
    public JAXBElement<String> createTitle(String value) {
        return new JAXBElement<String>(_Title_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value electronic billing
     * @return jaxb electronic billing
     * 
     */
    @XmlElementDecl(namespace = "", name = "electronicBilling")
    public JAXBElement<String> createElectronicBilling(String value) {
        return new JAXBElement<String>(_ElectronicBilling_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value external ref1
     * @return jaxb external ref1.
     * 
     */
    @XmlElementDecl(namespace = "", name = "externalRef1")
    public JAXBElement<String> createExternalRef1(String value) {
        return new JAXBElement<String>(_ExternalRef1_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value code
     * @return jaxb email
     * 
     */
    @XmlElementDecl(namespace = "", name = "email")
    public JAXBElement<String> createEmail(String value) {
        return new JAXBElement<String>(_Email_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value description
     * @return jaxb description.
     * 
     */
    @XmlElementDecl(namespace = "", name = "description")
    public JAXBElement<String> createDescription(String value) {
        return new JAXBElement<String>(_Description_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value value of company
     * @return jaxb company.
     * 
     */
    @XmlElementDecl(namespace = "", name = "company")
    public JAXBElement<String> createCompany(String value) {
        return new JAXBElement<String>(_Company_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value zip code
     * @return jaxb zip code.
     * 
     */
    @XmlElementDecl(namespace = "", name = "zipCode")
    public JAXBElement<String> createZipCode(String value) {
        return new JAXBElement<String>(_ZipCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value subscription date
     * @return jaxb subscription date.
     * 
     */
    @XmlElementDecl(namespace = "", name = "subscriptionDate")
    public JAXBElement<String> createSubscriptionDate(String value) {
        return new JAXBElement<String>(_SubscriptionDate_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}.
     * @param value key
     * @return jaxb key
     * 
     */
    @XmlElementDecl(namespace = "", name = "key")
    public JAXBElement<String> createKey(String value) {
        return new JAXBElement<String>(_Key_QNAME, String.class, null, value);
    }

}
