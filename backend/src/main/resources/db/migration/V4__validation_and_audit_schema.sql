-- ══════════════════════════════════════════════
-- Invoice Validations & Audit Trail Schema
-- ══════════════════════════════════════════════

-- 1. Validation Results Table
CREATE TABLE invoice_validations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id  UUID         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    rule_code   VARCHAR(50)  NOT NULL,
    rule_name   VARCHAR(100) NOT NULL,
    status      VARCHAR(10)  NOT NULL,
    field       VARCHAR(50),
    message     TEXT         NOT NULL,
    details     JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_validations_tenant_id  ON invoice_validations(tenant_id);
CREATE INDEX idx_invoice_validations_invoice_id ON invoice_validations(invoice_id);

-- 2. Audit Logs Table
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   UUID         NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    actor_id    UUID,
    actor_email VARCHAR(255),
    actor_role  VARCHAR(50),
    changes     JSONB        NOT NULL DEFAULT '{}',
    reason      TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_tenant_id  ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_entity     ON audit_logs(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(tenant_id, created_at DESC);

-- 3. Extend Invoices Table with Validation Summary Flags
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS has_validation_errors BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS has_validation_warnings BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_invoices_validation_flags ON invoices(tenant_id, has_validation_errors, has_validation_warnings);
