-- ══════════════════════════════════════════════
-- AI Usage & Token Tracking Schema
-- ══════════════════════════════════════════════

CREATE TABLE ai_usage_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id          UUID         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    model_name          VARCHAR(100) NOT NULL,
    prompt_tokens       INTEGER      NOT NULL DEFAULT 0,
    completion_tokens   INTEGER      NOT NULL DEFAULT 0,
    total_tokens        INTEGER      NOT NULL DEFAULT 0,
    estimated_cost_usd  NUMERIC(10, 6) NOT NULL DEFAULT 0.0,
    execution_time_ms   BIGINT       NOT NULL DEFAULT 0,
    successful          BOOLEAN      NOT NULL DEFAULT true,
    error_details       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_tenant_id  ON ai_usage_logs(tenant_id);
CREATE INDEX idx_ai_usage_invoice_id ON ai_usage_logs(invoice_id);
CREATE INDEX idx_ai_usage_created_at ON ai_usage_logs(tenant_id, created_at DESC);
