-- ══════════════════════════════════════════════
-- Invoices Schema
-- ══════════════════════════════════════════════

CREATE TABLE invoices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    status              VARCHAR(50)  NOT NULL DEFAULT 'UPLOADED',
    file_key            VARCHAR(500) NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    file_size           BIGINT       NOT NULL,
    file_hash           VARCHAR(64)  NOT NULL,
    invoice_number      VARCHAR(100),
    invoice_date        DATE,
    due_date            DATE,
    currency            VARCHAR(10)  NOT NULL DEFAULT 'INR',
    vendor_name         VARCHAR(255),
    vendor_gstin        VARCHAR(15),
    buyer_gstin         VARCHAR(15),
    subtotal            NUMERIC(15, 2),
    cgst_amount         NUMERIC(15, 2),
    sgst_amount         NUMERIC(15, 2),
    igst_amount         NUMERIC(15, 2),
    total_amount        NUMERIC(15, 2),
    overall_confidence  NUMERIC(5, 4),
    error_message       TEXT,
    metadata            JSONB        NOT NULL DEFAULT '{}',
    created_by          UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoices_tenant_id        ON invoices(tenant_id);
CREATE INDEX idx_invoices_status           ON invoices(status);
CREATE INDEX idx_invoices_tenant_hash      ON invoices(tenant_id, file_hash);
CREATE INDEX idx_invoices_vendor_gstin     ON invoices(tenant_id, vendor_gstin);
CREATE INDEX idx_invoices_invoice_date     ON invoices(tenant_id, invoice_date DESC);
CREATE INDEX idx_invoices_created_at       ON invoices(tenant_id, created_at DESC);

-- ══════════════════════════════════════════════
-- Invoice Line Items Schema
-- ══════════════════════════════════════════════

CREATE TABLE invoice_line_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id          UUID         NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    tenant_id           UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    line_number         INT          NOT NULL,
    description         TEXT         NOT NULL,
    hsn_sac             VARCHAR(20),
    quantity            NUMERIC(12, 4),
    unit_price          NUMERIC(15, 2),
    tax_rate            NUMERIC(5, 2),
    cgst_amount         NUMERIC(15, 2),
    sgst_amount         NUMERIC(15, 2),
    igst_amount         NUMERIC(15, 2),
    total_amount        NUMERIC(15, 2) NOT NULL,
    confidence          NUMERIC(5, 4),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_line_items_invoice_id ON invoice_line_items(invoice_id);
CREATE INDEX idx_line_items_tenant_id  ON invoice_line_items(tenant_id);
