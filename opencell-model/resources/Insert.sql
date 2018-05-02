--select * from billing_subscrip_termin_reason

INSERT INTO billing_subscrip_termin_reason (id, version, apply_agreement, apply_reimbursment, apply_termination_charges, code, description, created, apply_agreement_extension) 
VALUES (1, 0, 0, 0, 0, 'NEW_ACTIVATION', 'Yeni Aktivasyon', current_date, 0);

INSERT INTO billing_subscrip_termin_reason (id, version, apply_agreement, apply_reimbursment, apply_termination_charges, code, description, created, apply_agreement_extension) 
VALUES (2, 0, 0, 0, 0, 'FREEZE', 'Hat Dondurma', current_date, 1);

INSERT INTO billing_subscrip_termin_reason (id, version, apply_agreement, apply_reimbursment, apply_termination_charges, code, description, created, apply_agreement_extension) 
VALUES (3, 0, 0, 0, 0, 'TRANSFER', 'Nakil', current_date, 0);

INSERT INTO billing_subscrip_termin_reason (id, version, apply_agreement, apply_reimbursment, apply_termination_charges, code, description, created, apply_agreement_extension) 
VALUES (4, 0, 1, 0, 0, 'MUSTERI_TALEBI', 'Müşteri Talebi', current_date, 0);

update billing_subscrip_termin_reason set apply_agreement_extension = 0 where apply_agreement_extension is null
