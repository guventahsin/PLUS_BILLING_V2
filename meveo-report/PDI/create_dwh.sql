create table dwh_rec_wallet_op (
    hash_id character varying(255) NOT NULL, --sha256(provider_code+"_"+sub_code+"_"+mrr_date+"_"+code)
    provider_code  character varying(60) NOT NULL,
    sub_code  character varying(60) NOT NULL,
    mrr_date timestamp without time zone NOT NULL,
    code character varying(60) NOT NULL,
    mrr_value  numeric(23,12),
    created timestamp without time zone NOT NULL,
    updated timestamp without time zone,
    subscription_date timestamp without time zone NOT NULL,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    operation_type character varying(31) NOT NULL,
    description character varying(100),
    amount_tax numeric(23,12),
    amount_with_tax numeric(23,12),
    amount_without_tax numeric(23,12),
    offer_code character varying(35),
    operation_date timestamp without time zone,
    parameter_1 character varying(255),
    parameter_2 character varying(255),
    parameter_3 character varying(255),
    quantity numeric(23,12),
    status character varying(255),
    tax_percent numeric(23,12),
    credit_debit_flag character varying(255),
    counter_code  character varying(60) NOT NULL,
    priceplan_code  character varying(60) NOT NULL,
    reratedwalletoperation_id bigint,
    wallet_code  character varying(60) NOT NULL,
    ua_code  character varying(60) NOT NULL,
    ba_code  character varying(60) NOT NULL,
    ca_code  character varying(60) NOT NULL,
    cust_code  character varying(60) NOT NULL,
    seller_code  character varying(60) NOT NULL,
    invoicing_date timestamp without time zone
);