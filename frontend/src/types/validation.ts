export type ValidationStatus = 'PASS' | 'WARN' | 'FAIL';

export interface ValidationResult {
  id: string;
  invoiceId: string;
  ruleCode: string;
  ruleName: string;
  status: ValidationStatus;
  field?: string;
  message: string;
  details: Record<string, unknown>;
  createdAt: string;
}

export interface AuditLogEntry {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  actorId?: string;
  actorEmail?: string;
  actorRole?: string;
  changes: Record<string, { old: unknown; new: unknown }>;
  reason?: string;
  ipAddress?: string;
  createdAt: string;
}
