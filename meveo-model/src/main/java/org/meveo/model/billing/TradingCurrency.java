/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.model.billing;

import java.math.BigDecimal;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.meveo.model.AuditableEntity;
import org.meveo.model.admin.Currency;

@Entity
@Cacheable
@Table(name = "BILLING_TRADING_CURRENCY")
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "BILLING_TRADING_CURRENCY_SEQ")
public class TradingCurrency extends AuditableEntity {
	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CURRENCY_ID")
	private Currency currency;

	@Column(name = "PR_DESCRIPTION", length = 100)
	private String prDescription;

	@Column(name = "PR_CURRENCY_TO_THIS", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal prCurrencyToThis;

	@Transient
	String currencyCode;

	public BigDecimal getPrCurrencyToThis() {
		return prCurrencyToThis;
	}

	public void setPrCurrencyToThis(BigDecimal prCurrencyToThis) {
		this.prCurrencyToThis = prCurrencyToThis;
	}

	public String getPrDescription() {
		return prDescription;
	}

	public void setPrDescription(String prDescription) {
		this.prDescription = prDescription;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public String getCurrencyCode() {
		return currency.getCurrencyCode();
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

}
