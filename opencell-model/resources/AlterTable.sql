alter table cat_recurring_charge_templ add duration_term_in_month_start int;

alter table billing_service_instance add prev_service_instance_id int8;

alter table billing_service_instance add sub_activation_reason_id int8;

alter table billing_subscrip_termin_reason add apply_agreement_extension int4;

alter table billing_subscription add agreement_extension_days int;

alter table cat_charge_template add charge_sub_type varchar(20);

CREATE SEQUENCE billing_penalty_seq;

drop table billing_penalty

CREATE TABLE billing_penalty
(
	id bigint not null,
	calculation_type varchar (20) not null,
	calculation_date timestamp not null,
	termination_date timestamp not null,
	sub_termin_reason_id int8,
	amount_with_tax numeric (23,12),
	to_be_charged_amount_with_tax numeric (23,12),
	applied_discount_amount_with_tax numeric (23,12),
	installment_amount_with_tax numeric (23,12),
	subscription_id bigint not null,
	description varchar(255),
	version INT, 
	disabled INT DEFAULT 0 NOT NULL, 
	created TIMESTAMP WITHOUT TIME ZONE NOT NULL, 
	creator VARCHAR(100),
	updated TIMESTAMP WITHOUT TIME ZONE,
	updater varchar(100),
	code VARCHAR(255) NOT null,
	CONSTRAINT billing_penalty_pkey PRIMARY KEY (id)
)


ALTER TABLE billing_penalty ADD CONSTRAINT fk_billing_penalty_subscription FOREIGN KEY (subscription_id) REFERENCES billing_subscription (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE SEQUENCE billing_penalty_wo_seq;

drop table billing_penalty_wallet_operation

create table billing_penalty_wallet_operation
(
	id bigint not null,
	penalty_id bigint not null,
	wallet_operation_id bigint not null,
	type varchar(20) not null,
	description varchar(255),
	version INT, 
	disabled INT DEFAULT 0 NOT NULL, 
	created TIMESTAMP WITHOUT TIME ZONE NOT NULL, 
	creator VARCHAR(100),
	updated TIMESTAMP WITHOUT TIME ZONE,
	updater varchar(100),
	code VARCHAR(255) NOT null,
	CONSTRAINT billing_penalty_wo_pkey PRIMARY KEY (id)
)

ALTER TABLE billing_penalty_wallet_operation ADD CONSTRAINT fk_billing_penalty_wo_penalty FOREIGN KEY (penalty_id) REFERENCES billing_penalty (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE billing_penalty_wallet_operation ADD CONSTRAINT fk_billing_penalty_wo_wallet FOREIGN KEY (wallet_operation_id) REFERENCES billing_wallet_operation (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

