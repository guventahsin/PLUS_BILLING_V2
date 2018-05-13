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
import org.meveo.model.catalog.ChargeSubTypeEnum;

@Entity
@Table(name = "billing_penalty_wallet_operation")
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
        @Parameter(name = "sequence_name", value = "billing_penalty_wo_seq"), })
public class PenaltyWalletOperation extends BusinessEntity {

	private static final long serialVersionUID = 1L;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id")
    private Penalty penalty;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_operation_id")
    private WalletOperation walletOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private WalletOperationStatusEnum type;

  
	public Penalty getPenalty() {
		return penalty;
	}

	public void setPenalty(Penalty penalty) {
		this.penalty = penalty;
	}

	public WalletOperation getWalletOperation() {
		return walletOperation;
	}

	public void setWalletOperation(WalletOperation walletOperation) {
		this.walletOperation = walletOperation;
	}

	public WalletOperationStatusEnum getType() {
		return type;
	}

	public void setType(WalletOperationStatusEnum type) {
		this.type = type;
	}
    
    
}
