import { apiClient } from './client';
import { InvoiceDetail, InvoiceStatus, InvoiceSummary, PageResponse } from '../types/invoice';
import { AuditLogEntry, ValidationResult } from '../types/validation';

export interface ListInvoicesParams {
  status?: InvoiceStatus;
  page?: number;
  size?: number;
}

export const invoiceApi = {
  uploadInvoice: async (file: File): Promise<InvoiceDetail> => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient<InvoiceDetail>('/invoices/upload', {
      method: 'POST',
      body: formData,
    });
  },

  listInvoices: async (params: ListInvoicesParams = {}): Promise<PageResponse<InvoiceSummary>> => {
    const query = new URLSearchParams();
    if (params.status) query.append('status', params.status);
    if (params.page !== undefined) query.append('page', params.page.toString());
    if (params.size !== undefined) query.append('size', params.size.toString());

    return apiClient<PageResponse<InvoiceSummary>>(`/invoices?${query.toString()}`);
  },

  getInvoice: async (id: string): Promise<InvoiceDetail> => {
    return apiClient<InvoiceDetail>(`/invoices/${id}`);
  },

  updateInvoice: async (id: string, payload: Partial<InvoiceDetail>): Promise<InvoiceDetail> => {
    return apiClient<InvoiceDetail>(`/invoices/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },

  approveInvoice: async (id: string, notes?: string): Promise<InvoiceDetail> => {
    return apiClient<InvoiceDetail>(`/invoices/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ notes }),
    });
  },

  rejectInvoice: async (id: string, reason: string): Promise<InvoiceDetail> => {
    return apiClient<InvoiceDetail>(`/invoices/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  getValidations: async (id: string): Promise<ValidationResult[]> => {
    return apiClient<ValidationResult[]>(`/invoices/${id}/validations`);
  },

  revalidate: async (id: string): Promise<InvoiceDetail> => {
    return apiClient<InvoiceDetail>(`/invoices/${id}/validate`, {
      method: 'POST',
    });
  },

  getAuditLogs: async (id: string): Promise<AuditLogEntry[]> => {
    return apiClient<AuditLogEntry[]>(`/invoices/${id}/audit-logs`);
  },
};
