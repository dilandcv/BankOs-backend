CREATE TABLE webhook_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    url VARCHAR(500) NOT NULL,
    events VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_webhook_configs_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_webhook_configs_tenant ON webhook_configs (tenant_id);
CREATE INDEX idx_webhook_configs_tenant_status ON webhook_configs (tenant_id, status);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    webhook_config_id UUID REFERENCES webhook_configs(id) ON DELETE RESTRICT,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'DELIVERED', 'DEAD'))
);

CREATE INDEX idx_outbox_pending ON outbox_events (status, next_retry_at);
CREATE INDEX idx_outbox_tenant_created ON outbox_events (tenant_id, created_at DESC);
