package org.meveo.model.billing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessEntity;

@Entity
@Table(name = "billing_stamp_tax")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "billing_stamp_tax_seq"), })
public class StampTax extends BusinessEntity {

	private static final long serialVersionUID = 1L;
	
	public static final double stampTaxRate = 0.00948;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "calculation_date")
    private Date calculationDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type")
    private StampTaxCalculationTypeEnum calculationType;
    
    @Column(name = "total_tax_amount", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal totalTaxAmount;
    
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name = "billing_account_stamp_tax_id")
    private BillingAccountStampTax billingAccountStampTax;
    
    @OneToMany(mappedBy = "stampTax", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private List<StampTaxChargeInstance> stampTaxChargeInstances = new ArrayList<>();

    public void addStampTaxChargeInstance(StampTaxChargeInstance stci){
    	stampTaxChargeInstances.add(stci);
    	stci.setAuditable(this.getAuditable());
    	stci.setStampTax(this);
    }
    
    public void removeStampTaxChargeInstance(StampTaxChargeInstance stci){
    	stampTaxChargeInstances.remove(stci);
    	stci.setStampTax(null);
    }

	public Subscription getSubscription() {
		return subscription;
	}

	public void setSubscription(Subscription subscription) {
		this.subscription = subscription;
	}

	public Date getCalculationDate() {
		return calculationDate;
	}

	public void setCalculationDate(Date calculation_date) {
		this.calculationDate = calculation_date;
	}

	public StampTaxCalculationTypeEnum getCalculationType() {
		return calculationType;
	}

	public void setCalculationType(StampTaxCalculationTypeEnum calculation_type) {
		this.calculationType = calculation_type;
	}

	public BigDecimal getTotalTaxAmount() {
		return totalTaxAmount;
	}

	public void setTotalTaxAmount(BigDecimal totalTaxAmount) {
		this.totalTaxAmount = totalTaxAmount;
	}

	public List<StampTaxChargeInstance> getStampTaxChargeInstances() {
		return stampTaxChargeInstances;
	}

	public void setStampTaxChargeInstances(List<StampTaxChargeInstance> stampTaxChargeInstances) {
		this.stampTaxChargeInstances = stampTaxChargeInstances;
	}

	public BillingAccountStampTax getBillingAccountStampTax() {
		return billingAccountStampTax;
	}

	public void setBillingAccountStampTax(BillingAccountStampTax billingAccountStampTax) {
		this.billingAccountStampTax = billingAccountStampTax;
	}

}
