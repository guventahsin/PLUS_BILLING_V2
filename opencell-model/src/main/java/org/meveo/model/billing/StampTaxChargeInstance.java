package org.meveo.model.billing;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.BusinessEntity;

@Entity
@Table(name = "billing_stamp_tax_charge_ins")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "billing_stamptax_chargeins_seq"), })
public class StampTaxChargeInstance extends BusinessEntity {

	private static final long serialVersionUID = 1L;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stamp_tax_id")
    private StampTax stampTax;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_instance_id")
    private ChargeInstance chargeInstance;
    
    @Column(name = "stamp_tax_amount", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal stampTaxAmount;

	public StampTax getStampTax() {
		return stampTax;
	}

	public void setStampTax(StampTax stampTax) {
		this.stampTax = stampTax;
	}

	public ChargeInstance getChargeInstance() {
		return chargeInstance;
	}

	public void setChargeInstance(ChargeInstance chargeInstance) {
		this.chargeInstance = chargeInstance;
	}

	public BigDecimal getStampTaxAmount() {
		return stampTaxAmount;
	}

	public void setStampTaxAmount(BigDecimal stampTaxAmount) {
		this.stampTaxAmount = stampTaxAmount;
	}
    
}
