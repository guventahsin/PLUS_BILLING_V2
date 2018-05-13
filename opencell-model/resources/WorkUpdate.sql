update billing_wallet_operation set status = 'TREATED' where status = 'OPEN' -- and charge_instance_id < 334

update billing_wallet_operation set status = 'TREATED' where status = 'PENALTY' -- and charge_instance_id < 334

update billing_subscription set agreement_extension_days = null where id = 4

update billing_rated_transaction set status = 'CANCELED' where status = 'OPEN'

update cat_recurring_charge_templ set duration_term_in_month_start = 0 where Id = 6

update cat_recurring_charge_templ set duration_term_in_month_start = 2 where Id = 7

update billing_service_instance set termination_date = '2018-04-08' where Id = 71

update billing_service_instance set end_agrement_date = null

update billing_billing_account set next_invoice_date = '2018-06-01 00:00:00'