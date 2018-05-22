select a.Id, a.code, a.charge_date, b.next_charge_date, status, status_date, created from billing_charge_instance a, 
billing_recurring_charge_inst b where a.Id = B.Id and a.status in( 'ACTIVE', 'CLOSED')
and user_account_id = 8

select a.code, a.charge_date, b.next_charge_date, status from billing_charge_instance a, billing_recurring_charge_inst b where a.Id = B.Id and a.status = 'TERMINATED'  
and user_account_id = 8 order by a.Id desc

select a.code, a.charge_date, b.next_charge_date, status from billing_charge_instance a, billing_recurring_charge_inst b where a.Id = B.Id and a.status = 'SUSPENDED'
and user_account_id = 8

select a.created, a.code, a.input_quantity, a.amount_without_tax, a.amount_tax, a.amount_with_tax, a.start_date, a.end_date, a.charge_instance_id  from billing_wallet_operation a
, billing_charge_instance b 
where a.status = 'OPEN' 
and a.charge_instance_id = b.id
and b.user_account_id = 8
order by created desc


select a.created, a.code, a.input_quantity, a.amount_without_tax, a.amount_tax, a.amount_with_tax, a.start_date, a.end_date, a.charge_instance_id  from billing_wallet_operation a
, billing_charge_instance b
where a.status = 'TREATED' 
and a.charge_instance_id = b.id
and b.user_account_id = 8
order by created desc


select a.created, a.code, a.input_quantity, a.amount_without_tax, a.amount_tax, a.amount_with_tax, a.start_date, a.end_date, a.charge_instance_id  from billing_wallet_operation a
, billing_charge_instance b
where a.status = 'BILLED' 
and a.charge_instance_id = b.id
and b.user_account_id = 8
order by created desc

select a.created, a.code, a.input_quantity, a.amount_without_tax, a.amount_tax, a.amount_with_tax, a.start_date, a.end_date, a.charge_instance_id  from billing_wallet_operation a
, billing_charge_instance b
where a.status = 'PENALTY' 
and a.charge_instance_id = b.id
and b.user_account_id = 8
order by created desc


select d.*, e.charge_date, c.next_charge_date from billing_subscription a, billing_service_instance b, billing_recurring_charge_inst c, billing_wallet_operation d, billing_charge_instance e,
cat_charge_template f
where a.Id = b.subscription_id and b.Id = c.service_instance_id and c.Id = d.charge_instance_id 
and c.Id = e.Id
and e.charge_template_id = f.Id
and a.Id = 20


select e.*, d.* from billing_subscription a, billing_service_instance b, billing_recurring_charge_inst c, billing_wallet_operation d, billing_rated_transaction e
where a.Id = b.subscription_id and b.Id = c.service_instance_id and c.Id = d.charge_instance_id 
and d.Id = e.wallet_operation_id
and a.Id = 15

update billing_rated_transaction set status = 'BILLED' where id in (102,103)

select * from billing_subscription

select * from billing_penalty

select * from billing_penalty_wallet_operation where penalty_id = 4

select 
--b.code, b.amount_with_tax, b.start_date, b.end_date, b.quantity, b.status, a.type 
b.* from billing_penalty_wallet_operation a, billing_wallet_operation b  where 
b.Id = a.wallet_operation_id
and a.penalty_id = 3
order by code, start_date

select * from billing_rated_transaction where wallet_operation_id in (526,527, 528)

select * from billing_billing_account


select * from billing_wallet_operation --where charge_instance_id in (334,335)

select * from billing_rated_transaction where wallet_operation_id in (509, 510)

select id, status, status_date, subscription_date, termination_date, sub_termin_reason_id, prev_service_instance_id, sub_activation_reason_id from billing_service_instance 
where id in (116,117)

select agreement_extension_days from billing_subscription where id = 3

select * from billing_tax

select * from billing_invoice_agregate_taxes

select * from billing_invoice_agregate  order by id desc

select * from billing_invoice where invoice_number = '000000014'

select * from billing_recurring_charge_inst order by id desc

select * from billing_charge_instance order by id desc

select * from billing_subscription order by id desc 

select * from billing_wallet_operation

select * from billing_rated_transaction where status = 'OPEN'

select * from cat_charge_template

select * from cat_price_plan_matrix

select * from billing_wallet_operation

select * from meveo_job_instance where code = 'RR_Job'

select * from cat_recurring_charge_templ

select * from cat_calendar 

select * from cat_calendar_days where calendar_id = -1

select * from cat_calendar_days a, cat_day_in_year b where a.day_id = b.Id and calendar_id = -1

select * from cat_day_in_year

select Id, end_agrement_date, termination_date, status, a.* from billing_service_instance a order by Id desc

select * from billing_recurring_charge_inst order by Id desc

select * from billing_charge_instance where status = 'ACTIVE' and subscription_id = 2

select * from billing_charge_instance where status = 'SUSPENDED' and subscription_id = 2

select * from billing_wallet_operation order by Id desc

--open-> treated

--kontrol

select * from cat_recurring_charge_templ


select * from cat_charge_template

select * from billing_recurring_charge_inst

select sub_termin_reason_id, sub_activation_reason_id, status, Id, prev_service_instance_id, status_date, termination_date from billing_service_instance order by Id desc

update billing_service_instance set termination_date = '2018-04-04' where Id = 74

select a.Id, a.code, a.charge_date, b.next_charge_date, status from billing_charge_instance a, billing_recurring_charge_inst b where a.Id = B.Id order by a.Id desc



SELECT sum(r.amount_Without_Tax),sum(r.amount_With_Tax),sum(r.amount_Tax) FROM billing_rated_transaction r
                WHERE r.status='OPEN' AND r.do_Not_Trigger_Invoicing=0     
                AND r.amount_Without_Tax<>0
                AND r.invoice_id is null
                AND r.usageDate<lastTransactionDate "
                AND r.billingAccount.billingCycle=:billingCycle" + " AND (r.billingAccount.nextInvoiceDate >= :startDate)"
                AND (r.billingAccount.nextInvoiceDate < :endDate) "),
            

select * from billing_rated_transaction
                
 select * from billing_billing_account    
 

 
 select * from billing_billing_run where Id = 10
 
 select * from billing_billing_run_list
 
 select * from billing_subscrip_termin_reason
 
 select * from billing_subscription
 
select * from billing_inv_sub_cat_country where invoice_sub_category_id = -2 and  trading_country_id = -1

select * from billing_invoice_sub_cat where id = -2

select * from billing_tax

select * from billing_invoice order by id desc

select * from billing_stamp_tax

select * from billing_stamp_tax_charge_ins

select * from billing_account_stamp_tax

select * from cat_price_plan_matrix

select * from billing_wallet_operation order by created desc