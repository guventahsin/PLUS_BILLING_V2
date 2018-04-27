alter table cat_recurring_charge_templ add duration_term_in_month_start int;

alter table billing_service_instance add prev_service_instance_id int8;

alter table billing_service_instance add sub_activation_reason_id int8;

alter table billing_subscrip_termin_reason add apply_agreement_extension int4;

alter table billing_subscription add agreement_extension_days int;

alter table cat_charge_template add charge_sub_type varchar(20);
