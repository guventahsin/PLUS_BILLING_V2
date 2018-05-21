package org.meveo.model.billing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessEntity;

@Entity
@Table(name = "billing_account_stamp_tax")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "billing_account_stamp_tax_seq"), })
public class BillingAccountStampTax extends BusinessEntity {

	private static final long serialVersionUID = 1L;
	    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_account_id")
    private BillingAccount billingAccount;
    
    @Column(name = "stamp_tax_amount", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal stampTaxAmount;
    
    @OneToMany(mappedBy = "billingAccountStampTax", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private List<StampTax> stampTaxes = new ArrayList<>();

	public BigDecimal getStampTaxAmount() {
		return stampTaxAmount;
	}

	public void setStampTaxAmount(BigDecimal stampTaxAmount) {
		this.stampTaxAmount = stampTaxAmount;
	}

	public BillingAccount getBillingAccount() {
		return billingAccount;
	}

	public void setBillingAccount(BillingAccount billingAccount) {
		this.billingAccount = billingAccount;
	}

	public List<StampTax> getStampTaxes() {
		return stampTaxes;
	}

	public void setStampTaxes(List<StampTax> stampTaxes) {
		this.stampTaxes = stampTaxes;
	}
    
}
