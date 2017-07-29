package org.meveo.model.payments;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.meveo.model.shared.DateUtils;

@Entity
@DiscriminatorValue(value = "CARD")
public class CardPaymentMethod extends PaymentMethod {

    private static final long serialVersionUID = 8726571628074346184L;

    @Column(name = "token_id")
    @NotNull
    private String tokenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type")
    @NotNull
    private CreditCardTypeEnum cardType;

    @Column(name = "owner")
    @NotNull
    private String owner;

    @Column(name = "month_expiration")
    @NotNull
    @Min(1)
    @Max(12)
    private Integer monthExpiration;

    @Column(name = "year_expiration")
    @NotNull
    @Min(0)
    @Max(99)
    private Integer yearExpiration;

    @Column(name = "card_number")
    @NotNull
    private String hiddenCardNumber;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "INFO_1")
    private String info1;

    @Column(name = "INFO_2")
    private String info2;

    @Column(name = "INFO_3")
    private String info3;

    @Column(name = "INFO_4")
    private String info4;

    @Column(name = "INFO_5")
    private String info5;

    @Transient
    private String cardNumber;

    @Transient
    private String issueNumber;

    public CardPaymentMethod() {
        this.paymentType = PaymentMethodEnum.CARD;
    }

    public CardPaymentMethod(String alias, boolean preferred) {
        super();
        this.alias = alias;
        this.preferred = preferred;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public CreditCardTypeEnum getCardType() {
        return cardType;
    }

    public void setCardType(CreditCardTypeEnum cardType) {
        this.cardType = cardType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Integer getMonthExpiration() {
        return monthExpiration;
    }

    public void setMonthExpiration(Integer monthExpiration) {
        this.monthExpiration = monthExpiration;
    }

    public Integer getYearExpiration() {
        return yearExpiration;
    }

    public void setYearExpiration(Integer yearExpiration) {
        this.yearExpiration = yearExpiration;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(String issueNumber) {
        this.issueNumber = issueNumber;
    }

    public String getHiddenCardNumber() {
        return hiddenCardNumber;
    }

    public void setHiddenCardNumber(String hiddenCardNumber) {
        this.hiddenCardNumber = hiddenCardNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getInfo1() {
        return info1;
    }

    public void setInfo1(String info1) {
        this.info1 = info1;
    }

    public String getInfo2() {
        return info2;
    }

    public void setInfo2(String info2) {
        this.info2 = info2;
    }

    public String getInfo3() {
        return info3;
    }

    public void setInfo3(String info3) {
        this.info3 = info3;
    }

    public String getInfo4() {
        return info4;
    }

    public void setInfo4(String info4) {
        this.info4 = info4;
    }

    public String getInfo5() {
        return info5;
    }

    public void setInfo5(String info5) {
        this.info5 = info5;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof CardPaymentMethod)) {
            return false;
        }

        CardPaymentMethod other = (CardPaymentMethod) obj;

        if (getId() != null && other.getId() != null && getId().equals(other.getId())) {
            return true;
        }

        return StringUtils.compare(hiddenCardNumber, other.getHiddenCardNumber()) == 0 && monthExpiration.equals(other.getMonthExpiration())
                && yearExpiration.equals(other.getYearExpiration());
    }

    public String getExpirationMonthAndYear() {
        return (monthExpiration != null && monthExpiration < 10 ? "0" : "") + monthExpiration + "/" + yearExpiration;
    }

    @Override
    public void updateWith(PaymentMethod paymentMethod) {

        CardPaymentMethod otherPaymentMethod = (CardPaymentMethod) paymentMethod;

        setAlias(otherPaymentMethod.getAlias());
        setPreferred(otherPaymentMethod.isPreferred());

        // The rest of information is not updatable if token was generated already
        if (tokenId != null) {
            return;
        }
        setCardNumber(otherPaymentMethod.getCardNumber());
        setIssueNumber(otherPaymentMethod.getIssueNumber());

        setHiddenCardNumber(otherPaymentMethod.getCardNumber().substring(cardNumber.length() - 4));
        setOwner(otherPaymentMethod.getOwner());
        setCardType(otherPaymentMethod.getCardType());
        setPreferred(otherPaymentMethod.isPreferred());
        setYearExpiration(otherPaymentMethod.getYearExpiration());
        setMonthExpiration(otherPaymentMethod.getMonthExpiration());
        setUserId(otherPaymentMethod.getUserId());
        setInfo1(otherPaymentMethod.getInfo1());
        setInfo2(otherPaymentMethod.getInfo2());
        setInfo3(otherPaymentMethod.getInfo3());
        setInfo4(otherPaymentMethod.getInfo4());
        setInfo5(otherPaymentMethod.getInfo5());
    }

    /**
     * Is card valid for a given date
     * 
     * @param date Date to check
     * @return True is expiration date is beyond a given date
     */
    public boolean isValidForDate(Date date) {

        int year = new Integer(DateUtils.getYearFromDate(date).toString().substring(2, 4));
        int month = DateUtils.getMonthFromDate(new Date());
        return yearExpiration.intValue() > year || (yearExpiration.intValue() == year && monthExpiration >= month);
    }

    @Override
    public String toString() {
        return "CardPaymentMethod [tokenId=" + tokenId + ", cardType=" + cardType + ", owner=" + owner + ", monthExpiration=" + monthExpiration + ", yearExpiration="
                + yearExpiration + ", hiddenCardNumber=" + hiddenCardNumber + ", userId=" + userId + ", info1=" + info1 + ", info2=" + info2 + ", info3=" + info3 + ", info4="
                + info4 + ", info5=" + info5 + ", cardNumber=" + cardNumber + ", issueNumber=" + issueNumber + "]";
    }
}