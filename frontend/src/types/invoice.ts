export type InvoiceStatus =
  | 'UPLOADED'
  | 'PROCESSING'
  | 'EXTRACTED'
  | 'NEEDS_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'EXPORTED'
  | 'FAILED';

export interface LineItem {
  id: string;
  lineNumber: number;
  description: string;
  hsnSac?: string;
  quantity?: number;
  unitPrice?: number;
  taxRate?: number;
  cgstAmount?: number;
  sgstAmount?: number;
  igstAmount?: number;
  totalAmount: number;
  confidence?: number;
}

export interface InvoiceSummary {
  id: string;
  status: InvoiceStatus;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  invoiceNumber?: string;
  invoiceDate?: string;
  dueDate?: string;
  vendorName?: string;
  vendorGstin?: string;
  totalAmount?: number;
  currency: string;
  overallConfidence?: number;
  hasValidationErrors: boolean;
  hasValidationWarnings: boolean;
  createdAt: string;
}

export interface InvoiceDetail extends InvoiceSummary {
  tenantId: string;
  fileHash: string;
  fileUrl?: string;
  buyerGstin?: string;
  subtotal?: number;
  cgstAmount?: number;
  sgstAmount?: number;
  igstAmount?: number;
  errorMessage?: string;
  metadata: {
    fieldConfidences?: Record<string, number>;
    [key: string]: unknown;
  };
  lineItems: LineItem[];
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
