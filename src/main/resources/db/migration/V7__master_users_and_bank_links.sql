CREATE TABLE master_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_master_users_email UNIQUE (email),
    CONSTRAINT ck_master_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_master_users_email ON master_users (email);

CREATE TABLE user_bank_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES master_users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    account_nickname VARCHAR(100),
    account_number_mask VARCHAR(30),
    account_type VARCHAR(50) NOT NULL DEFAULT 'SAVINGS',
    auth_status VARCHAR(30) NOT NULL DEFAULT 'CONNECTED',
    auth_mechanism VARCHAR(50) NOT NULL DEFAULT 'OAUTH2',
    auth_token_encrypted TEXT,
    is_default BOOLEAN NOT NULL DEFAULT false,
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_tenant_link UNIQUE (user_id, tenant_id),
    CONSTRAINT ck_user_bank_links_auth_status CHECK (auth_status IN ('CONNECTED', 'PENDING_REAUTH', 'REVOKED')),
    CONSTRAINT ck_user_bank_links_mechanism CHECK (auth_mechanism IN ('OAUTH2', 'DIRECT_CONSENT', 'API_KEY'))
);

CREATE INDEX idx_user_bank_links_user ON user_bank_links (user_id);
CREATE INDEX idx_user_bank_links_tenant ON user_bank_links (tenant_id);
CREATE INDEX idx_user_bank_links_user_status ON user_bank_links (user_id, auth_status);
