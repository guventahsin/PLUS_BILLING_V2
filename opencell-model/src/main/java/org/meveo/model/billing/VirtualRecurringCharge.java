package org.meveo.model.billing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VirtualRecurringCharge
{
	List<WalletOperation> walletOperations = new ArrayList<WalletOperation>();
	Date chargeDate;
	Date nextChargeDate;
	InstanceStatusEnum status;
	
	public List<WalletOperation> getWalletOperations() {
		return walletOperations;
	}
	public void setWalletOperations(List<WalletOperation> walletOperations) {
		this.walletOperations = walletOperations;
	}
	public Date getChargeDate() {
		return chargeDate;
	}
	public void setChargeDate(Date chargeDate) {
		this.chargeDate = chargeDate;
	}
	public Date getNextChargeDate() {
		return nextChargeDate;
	}
	public void setNextChargeDate(Date nextChargeDate) {
		this.nextChargeDate = nextChargeDate;
	}
	public InstanceStatusEnum getStatus() {
		return status;
	}
	public void setStatus(InstanceStatusEnum status) {
		this.status = status;
	}
	
	
}