CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    document_type VARCHAR(30) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_customers_tenant_document UNIQUE (tenant_id, document_number),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_customers_tenant_id ON customers (tenant_id);
CREATE INDEX idx_customers_tenant_document ON customers (tenant_id, document_number);

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    account_number VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_accounts_tenant_number UNIQUE (tenant_id, account_number),
    CONSTRAINT ck_accounts_balance CHECK (balance >= 0),
    CONSTRAINT ck_accounts_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_accounts_tenant_id ON accounts (tenant_id);
CREATE INDEX idx_accounts_tenant_customer ON accounts (tenant_id, customer_id);
CREATE INDEX idx_accounts_tenant_status ON accounts (tenant_id, status);
