-- Migración V9: Tarjetas Bancarias y Ajustes de Branding para Tenants

-- 1. Ampliar longitud de logo_url en tenants para soportar rutas largas o URLs completas
ALTER TABLE tenants ALTER COLUMN logo_url TYPE VARCHAR(1000);

-- 2. Vincular usuario con cliente de manera opcional para trazabilidad directa
ALTER TABLE customers ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers (user_id);

-- 3. Crear tabla de tarjetas bancarias (débito y crédito)
CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    card_number VARCHAR(19) NOT NULL,
    cardholder_name VARCHAR(150) NOT NULL,
    expiration_date VARCHAR(5) NOT NULL,
    cvv_masked VARCHAR(4) NOT NULL,
    card_type VARCHAR(20) NOT NULL DEFAULT 'DEBIT',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_cards_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    CONSTRAINT ck_cards_card_type CHECK (card_type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_cards_tenant_id ON cards (tenant_id);
CREATE INDEX idx_cards_account_id ON cards (account_id);
CREATE INDEX idx_cards_customer_id ON cards (customer_id);
CREATE INDEX idx_cards_tenant_customer ON cards (tenant_id, customer_id);
