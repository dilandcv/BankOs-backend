-- Datos de demostración para usuarios maestros de plataforma (Master Users).
-- Contraseña de todos los usuarios demo: Admin123!
-- Hash BCrypt (strength 12): $2y$12$fF/qsiCm1pZ8ShwFMHC6ve.LFEHS8x4tp6f7.DKMLOC/M3rCcxBne

INSERT INTO master_users (id, email, password_hash, full_name, phone, status) VALUES
    ('40000000-0000-0000-0000-000000000001', 'usuario@bankos.com', '$2y$12$fF/qsiCm1pZ8ShwFMHC6ve.LFEHS8x4tp6f7.DKMLOC/M3rCcxBne', 'María González', '+57 300 1234567', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000002', 'juan.perez@bankos.com', '$2y$12$fF/qsiCm1pZ8ShwFMHC6ve.LFEHS8x4tp6f7.DKMLOC/M3rCcxBne', 'Juan Pérez', '+57 311 9876543', 'ACTIVE');

INSERT INTO user_bank_links (id, user_id, tenant_id, customer_id, account_nickname, account_number_mask, account_type, auth_status, auth_mechanism, is_default, last_sync_at) VALUES
    -- María González: Nequi, Daviplata, Banco Horizonte
    ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Nequi Personal', '•••• 4567', 'SAVINGS', 'CONNECTED', 'OAUTH2', true, now()),
    ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'Daviplata Ahorros', '•••• 9876', 'SAVINGS', 'CONNECTED', 'OAUTH2', false, now()),
    ('50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000001', 'Horizonte Nómina', '•••• 4821', 'CHECKING', 'CONNECTED', 'OAUTH2', false, now()),
    -- Juan Pérez: Solo Nequi
    ('50000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Nequi Juan', '•••• 3456', 'SAVINGS', 'CONNECTED', 'OAUTH2', true, now());
