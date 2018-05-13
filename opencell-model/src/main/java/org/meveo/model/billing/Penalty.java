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
import org.meveo.model.Auditable;
import org.meveo.model.BusinessEntity;

@Entity
@Table(name = "billing_penalty")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "billing_penalty_seq"), })
public class Penalty extends BusinessEntity {

	private static final long serialVersionUID = 1L;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "calculation_date")
    private Date calculation_date;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "termination_date")
    private Date terminationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_termin_reason_id")
    private SubscriptionTerminationReason subscriptionTerminationReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type")
    private PenaltyCalculationTypeEnum calculation_type;
    
    @Column(name = "amount_with_tax", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal penaltyAmountWitTax;

    @Column(name = "to_be_charged_amount_with_tax", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal totalToBeChargedAmountWithTax;

    @Column(name = "applied_discount_amount_with_tax", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal totalAppliedDiscountAmountWithTax;	

    @Column(name = "installment_amount_with_tax", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal totalInstallmentAmountWithTax;	
    
    @Enumerated(EnumType.STRING)
    @Column(name = "applied_wallet_op_type")
    private WalletOperationStatusEnum appliedWalletOpType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_penalty_id")
    private Penalty InfoPenalty;
    
    @OneToMany(mappedBy = "penalty", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private List<PenaltyWalletOperation> penaltyWalletOperations = new ArrayList<>();

    public void addPenaltyWalletOperation(PenaltyWalletOperation wo){
    	penaltyWalletOperations.add(wo);
    	wo.setAuditable(this.getAuditable());
    	wo.setPenalty(this);
    }
    
    public void removePenaltyWalletOperation(PenaltyWalletOperation wo){
    	penaltyWalletOperations.remove(wo);
    	wo.setPenalty(null);
    }
    
	public Subscription getSubscription() {
		return subscription;
	}

	public void setSubscription(Subscription subscription) {
		this.subscription = subscription;
	}

	public Date getCalculation_date() {
		return calculation_date;
	}

	public void setCalculation_date(Date calculation_date) {
		this.calculation_date = calculation_date;
	}

	public Date getTerminationDate() {
		return terminationDate;
	}

	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}

	public SubscriptionTerminationReason getSubscriptionTerminationReason() {
		return subscriptionTerminationReason;
	}

	public void setSubscriptionTerminationReason(SubscriptionTerminationReason subscriptionTerminationReason) {
		this.subscriptionTerminationReason = subscriptionTerminationReason;
	}

	public PenaltyCalculationTypeEnum getCalculation_type() {
		return calculation_type;
	}

	public void setCalculation_type(PenaltyCalculationTypeEnum calculation_type) {
		this.calculation_type = calculation_type;
	}

	public BigDecimal getPenaltyAmountWitTax() {
		return penaltyAmountWitTax;
	}

	public void setPenaltyAmountWitTax(BigDecimal penaltyAmountWitTax) {
		this.penaltyAmountWitTax = penaltyAmountWitTax;
	}

	public BigDecimal getTotalToBeChargedAmountWithTax() {
		return totalToBeChargedAmountWithTax;
	}

	public void setTotalToBeChargedAmountWithTax(BigDecimal totalToBeChargedAmountWithTax) {
		this.totalToBeChargedAmountWithTax = totalToBeChargedAmountWithTax;
	}

	public BigDecimal getTotalAppliedDiscountAmountWithTax() {
		return totalAppliedDiscountAmountWithTax;
	}

	public void setTotalAppliedDiscountAmountWithTax(BigDecimal totalAppliedDiscountAmountWithTax) {
		this.totalAppliedDiscountAmountWithTax = totalAppliedDiscountAmountWithTax;
	}

	public BigDecimal getTotalInstallmentAmountWithTax() {
		return totalInstallmentAmountWithTax;
	}

	public void setTotalInstallmentAmountWithTax(BigDecimal totalInstallmentAmountWithTax) {
		this.totalInstallmentAmountWithTax = totalInstallmentAmountWithTax;
	}

	public List<PenaltyWalletOperation> getPenaltyWalletOperations() {
		return penaltyWalletOperations;
	}

	public void setPenaltyWalletOperations(List<PenaltyWalletOperation> penaltyWalletOperations) {
		this.penaltyWalletOperations = penaltyWalletOperations;
	}

	public WalletOperationStatusEnum getAppliedWalletOpType() {
		return appliedWalletOpType;
	}

	public void setAppliedWalletOpType(WalletOperationStatusEnum appliedWalletOpType) {
		this.appliedWalletOpType = appliedWalletOpType;
	}

	public Penalty getInfoPenalty() {
		return InfoPenalty;
	}

	public void setInfoPenalty(Penalty infoPenalty) {
		InfoPenalty = infoPenalty;
	}
    
}
