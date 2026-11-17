CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    fee NUMERIC(19,4) NOT NULL DEFAULT 0,
    exchange_rate NUMERIC(19,8),
    source_account_id UUID REFERENCES accounts(id) ON DELETE RESTRICT,
    destination_account_id UUID REFERENCES accounts(id) ON DELETE RESTRICT,
    description VARCHAR(255),
    correlation_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_transactions_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT ck_transactions_status CHECK (status IN ('COMPLETED', 'REVERSED')),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_transactions_fee CHECK (fee >= 0),
    CONSTRAINT ck_transactions_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX idx_transactions_tenant_created ON transactions (tenant_id, created_at DESC);
CREATE INDEX idx_transactions_tenant_source ON transactions (tenant_id, source_account_id);
CREATE INDEX idx_transactions_tenant_destination ON transactions (tenant_id, destination_account_id);
CREATE INDEX idx_transactions_correlation ON transactions (correlation_id);

CREATE TABLE transaction_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_transaction_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_transaction_entries_amount CHECK (amount > 0),
    CONSTRAINT ck_transaction_entries_currency CHECK (char_length(currency) = 3)
);

CREATE INDEX idx_entries_transaction ON transaction_entries (transaction_id);
CREATE INDEX idx_entries_tenant_account ON transaction_entries (tenant_id, account_id);
