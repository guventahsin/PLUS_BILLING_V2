alter table cat_recurring_charge_templ add duration_term_in_month_start int;

alter table billing_service_instance add prev_service_instance_id int8;

alter table billing_service_instance add sub_activation_reason_id int8;

alter table billing_subscrip_termin_reason add apply_agreement_extension int4;

alter table billing_subscription add agreement_extension_days int;

alter table cat_charge_template add charge_sub_type varchar(20);

drop SEQUENCE billing_penalty_wo_seq;

CREATE SEQUENCE billing_penalty_wo_seq;

drop sequence billing_penalty_seq;

CREATE SEQUENCE billing_penalty_seq;

drop table billing_penalty_wallet_operation

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
	applied_wallet_op_type varchar(20),
	subscription_id bigint not null,
	info_penalty_id bigint,
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

ALTER TABLE billing_penalty ADD CONSTRAINT fk_billing_penalty_info_penalty FOREIGN KEY (info_penalty_id) REFERENCES billing_penalty (id) ON UPDATE NO ACTION ON DELETE NO ACTION;


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


create sequence billing_stamp_tax_seq;

create sequence billing_account_stamp_tax_seq;

create sequence billing_stamptax_chargeins_seq;



drop table billing_stamp_tax_charge_ins;

drop table billing_stamp_tax;

drop table billing_account_stamp_tax;

create table billing_account_stamp_tax
(
	id bigint not null,
	billing_account_id bigint not null,
	stamp_tax_amount numeric(23,12),
	description varchar(255),
	version INT, 
	disabled INT DEFAULT 0 NOT NULL, 
	created TIMESTAMP WITHOUT TIME ZONE NOT NULL, 
	creator VARCHAR(100),
	updated TIMESTAMP WITHOUT TIME ZONE,
	updater varchar(100),
	code VARCHAR(255) NOT null,
	CONSTRAINT billing_stamp_tax_billacc_pkey PRIMARY KEY (id)
)

ALTER TABLE billing_account_stamp_tax ADD CONSTRAINT fk_billing_billacc_stamp_bill_acc FOREIGN KEY (billing_account_id) REFERENCES billing_billing_account (id) ON UPDATE NO ACTION ON DELETE NO ACTION;


create table billing_stamp_tax
(
	id bigint not null,
	calculation_type varchar (20) not null,
	calculation_date timestamp not null,
	total_tax_amount numeric (23,12) not null,
	subscription_id bigint not null,
	billing_account_stamp_tax_id bigint,
	description varchar(255),
	version INT, 
	disabled INT DEFAULT 0 NOT NULL, 
	created TIMESTAMP WITHOUT TIME ZONE NOT NULL, 
	creator VARCHAR(100),
	updated TIMESTAMP WITHOUT TIME ZONE,
	updater varchar(100),
	code VARCHAR(255) NOT null,
	CONSTRAINT billing_stamp_tax_pkey PRIMARY KEY (id)
)

ALTER TABLE billing_stamp_tax ADD CONSTRAINT fk_billing_stamp_subs_id FOREIGN KEY (subscription_id) REFERENCES billing_subscription (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE billing_stamp_tax ADD CONSTRAINT fk_billing_stamp_billaccstamptax FOREIGN KEY (billing_account_stamp_tax_id) REFERENCES billing_account_stamp_tax (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

create table billing_stamp_tax_charge_ins
(
	id bigint not null,
	stamp_tax_id bigint not null,
	charge_instance_id bigint not null,
	stamp_tax_amount numeric(23,12) not null,
	description varchar(255),
	version INT, 
	disabled INT DEFAULT 0 NOT NULL, 
	created TIMESTAMP WITHOUT TIME ZONE NOT NULL, 
	creator VARCHAR(100),
	updated TIMESTAMP WITHOUT TIME ZONE,
	updater varchar(100),
	code VARCHAR(255) NOT null,
	CONSTRAINT billing_stamp_tax_charge_ins_pkey PRIMARY KEY (id)
)

ALTER TABLE billing_stamp_tax_charge_ins ADD CONSTRAINT fk_billing_stamp_charge_ins_stamp FOREIGN KEY (stamp_tax_id) REFERENCES billing_stamp_tax (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE billing_stamp_tax_charge_ins ADD CONSTRAINT fk_billing_stamp_charge_ins FOREIGN KEY (charge_instance_id) REFERENCES billing_charge_instance (id) ON UPDATE NO ACTION ON DELETE NO ACTION;


alter table billing_invoice add stamp_tax_amount numeric(23,12);

alter table billing_invoice add billing_account_stamp_tax_id bigint;

