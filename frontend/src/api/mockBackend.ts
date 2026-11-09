import { AuthResponse, TenantDetails, UserProfile } from '../types/auth';
import { InvoiceDetail, InvoiceSummary, PageResponse } from '../types/invoice';

const STORAGE_KEYS = {
  USER: 'invoiceiq_mock_user',
  TENANT: 'invoiceiq_mock_tenant',
  INVOICES: 'invoiceiq_mock_invoices',
};

const INITIAL_INVOICES: InvoiceDetail[] = [
  {
    id: 'inv-101',
    tenantId: 'tenant-001',
    status: 'NEEDS_REVIEW',
    originalFileName: 'Infotech_Tax_Invoice_Oct.pdf',
    contentType: 'application/pdf',
    fileSize: 245120,
    fileHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    invoiceNumber: 'INV-2024-0089',
    invoiceDate: '2024-10-15',
    dueDate: '2024-11-14',
    currency: 'INR',
    vendorName: 'Infotech Solutions Private Limited',
    vendorGstin: '29AABCU9603R1ZM',
    buyerGstin: '29XYZAB5678C1Z2',
    subtotal: 50000.0,
    cgstAmount: 4500.0,
    sgstAmount: 4500.0,
    igstAmount: 0.0,
    totalAmount: 59000.0,
    overallConfidence: 0.98,
    hasValidationErrors: false,
    hasValidationWarnings: false,
    createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 2).toISOString(),
    metadata: {
      fieldConfidences: {
        invoiceNumber: 0.98,
        invoiceDate: 0.99,
        vendorName: 0.99,
        vendorGstin: 0.99,
        totalAmount: 0.99,
      },
    },
    lineItems: [
      {
        id: 'li-1',
        lineNumber: 1,
        description: 'Cloud Infrastructure Consulting & Setup',
        hsnSac: '998313',
        quantity: 1,
        unitPrice: 35000.0,
        taxRate: 18.0,
        cgstAmount: 3150.0,
        sgstAmount: 3150.0,
        igstAmount: 0.0,
        totalAmount: 41300.0,
        confidence: 0.98,
      },
      {
        id: 'li-2',
        lineNumber: 2,
        description: 'DevOps Maintenance Support (Quarterly)',
        hsnSac: '998314',
        quantity: 1,
        unitPrice: 15000.0,
        taxRate: 18.0,
        cgstAmount: 1350.0,
        sgstAmount: 1350.0,
        igstAmount: 0.0,
        totalAmount: 17700.0,
        confidence: 0.97,
      },
    ],
  },
  {
    id: 'inv-102',
    tenantId: 'tenant-001',
    status: 'APPROVED',
    originalFileName: 'AWS_India_Monthly_Bill.pdf',
    contentType: 'application/pdf',
    fileSize: 189400,
    fileHash: 'a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e',
    invoiceNumber: 'AWS-IN-98721',
    invoiceDate: '2024-10-01',
    dueDate: '2024-10-31',
    currency: 'INR',
    vendorName: 'Amazon Web Services India Pvt Ltd',
    vendorGstin: '27AABCA2233M1Z8',
    buyerGstin: '29XYZAB5678C1Z2',
    subtotal: 35720.0,
    cgstAmount: 0.0,
    sgstAmount: 0.0,
    igstAmount: 6430.0,
    totalAmount: 42150.0,
    overallConfidence: 0.96,
    hasValidationErrors: false,
    hasValidationWarnings: false,
    createdAt: new Date(Date.now() - 86400000 * 5).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 3).toISOString(),
    metadata: {},
    lineItems: [
      {
        id: 'li-3',
        lineNumber: 1,
        description: 'Amazon Elastic Compute Cloud (EC2) Usage',
        hsnSac: '998315',
        quantity: 1,
        unitPrice: 35720.0,
        taxRate: 18.0,
        cgstAmount: 0.0,
        sgstAmount: 0.0,
        igstAmount: 6430.0,
        totalAmount: 42150.0,
        confidence: 0.96,
      },
    ],
  },
  {
    id: 'inv-103',
    tenantId: 'tenant-001',
    status: 'NEEDS_REVIEW',
    originalFileName: 'Airtel_Broadband_Invoice.pdf',
    contentType: 'application/pdf',
    fileSize: 95400,
    fileHash: '3b7b190d62c65bf0bcda32b57b277d9ad9f146ea591a6d40bf420404a011733c',
    invoiceNumber: 'AIR-2024-771',
    invoiceDate: '2024-10-20',
    dueDate: '2024-11-05',
    currency: 'INR',
    vendorName: 'Bharti Airtel Limited',
    vendorGstin: '29AAACB2894G1ZG',
    buyerGstin: '29XYZAB5678C1Z2',
    subtotal: 10508.48,
    cgstAmount: 945.76,
    sgstAmount: 945.76,
    igstAmount: 0.0,
    totalAmount: 12400.0,
    overallConfidence: 0.94,
    hasValidationErrors: false,
    hasValidationWarnings: true, // Example warning
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
    metadata: {},
    lineItems: [],
  },
];

export const mockBackend = {
  handleSignup: (data: { tenantName: string; email: string; firstName: string; lastName: string }): AuthResponse => {
    const tenant: TenantDetails = {
      id: 'tenant-' + Math.random().toString(36).substring(2, 9),
      name: data.tenantName,
      slug: data.tenantName.toLowerCase().replace(/[^a-z0-9]/g, '-'),
      plan: 'FREE',
      settings: {},
      createdAt: new Date().toISOString(),
    };
    const user: UserProfile = {
      id: 'user-' + Math.random().toString(36).substring(2, 9),
      tenantId: tenant.id,
      email: data.email,
      firstName: data.firstName,
      lastName: data.lastName,
      role: 'OWNER',
      active: true,
      createdAt: new Date().toISOString(),
    };

    localStorage.setItem(STORAGE_KEYS.TENANT, JSON.stringify(tenant));
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    if (!localStorage.getItem(STORAGE_KEYS.INVOICES)) {
      localStorage.setItem(STORAGE_KEYS.INVOICES, JSON.stringify(INITIAL_INVOICES));
    }

    return {
      accessToken: 'demo-token-' + Date.now(),
      tokenType: 'Bearer',
      expiresIn: 900000,
      user,
    };
  },

  handleLogin: (email: string): AuthResponse => {
    let user: UserProfile;
    const storedUser = localStorage.getItem(STORAGE_KEYS.USER);
    if (storedUser) {
      user = JSON.parse(storedUser);
    } else {
      user = {
        id: 'user-demo-1',
        tenantId: 'tenant-demo-1',
        email: email || 'owner@acme.com',
        firstName: 'Alex',
        lastName: 'Sharma',
        role: 'OWNER',
        active: true,
        createdAt: new Date().toISOString(),
      };
      const tenant: TenantDetails = {
        id: user.tenantId,
        name: 'Acme Technologies',
        slug: 'acme-tech',
        plan: 'FREE',
        settings: {},
        createdAt: new Date().toISOString(),
      };
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
      localStorage.setItem(STORAGE_KEYS.TENANT, JSON.stringify(tenant));
    }

    if (!localStorage.getItem(STORAGE_KEYS.INVOICES)) {
      localStorage.setItem(STORAGE_KEYS.INVOICES, JSON.stringify(INITIAL_INVOICES));
    }

    return {
      accessToken: 'demo-token-' + Date.now(),
      tokenType: 'Bearer',
      expiresIn: 900000,
      user,
    };
  },

  getCurrentUser: (): UserProfile => {
    const stored = localStorage.getItem(STORAGE_KEYS.USER);
    if (stored) return JSON.parse(stored);
    return {
      id: 'user-demo-1',
      tenantId: 'tenant-demo-1',
      email: 'owner@acme.com',
      firstName: 'Alex',
      lastName: 'Sharma',
      role: 'OWNER',
      active: true,
      createdAt: new Date().toISOString(),
    };
  },

  getCurrentTenant: (): TenantDetails => {
    const stored = localStorage.getItem(STORAGE_KEYS.TENANT);
    if (stored) return JSON.parse(stored);
    return {
      id: 'tenant-demo-1',
      name: 'Acme Technologies',
      slug: 'acme-tech',
      plan: 'FREE',
      settings: {},
      createdAt: new Date().toISOString(),
    };
  },

  getInvoices: (status?: string): PageResponse<InvoiceSummary> => {
    const stored = localStorage.getItem(STORAGE_KEYS.INVOICES);
    let list: InvoiceDetail[] = stored ? JSON.parse(stored) : INITIAL_INVOICES;

    if (status && status !== 'ALL') {
      list = list.filter((i) => i.status === status);
    }

    const summaries: InvoiceSummary[] = list.map((i) => ({
      id: i.id,
      status: i.status,
      originalFileName: i.originalFileName,
      contentType: i.contentType,
      fileSize: i.fileSize,
      invoiceNumber: i.invoiceNumber,
      invoiceDate: i.invoiceDate,
      dueDate: i.dueDate,
      vendorName: i.vendorName,
      vendorGstin: i.vendorGstin,
      totalAmount: i.totalAmount,
      currency: i.currency,
      overallConfidence: i.overallConfidence,
      hasValidationErrors: i.hasValidationErrors,
      hasValidationWarnings: i.hasValidationWarnings,
      createdAt: i.createdAt,
    }));

    return {
      content: summaries,
      totalElements: summaries.length,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: summaries.length === 0,
    };
  },

  uploadInvoice: (file: File): InvoiceDetail => {
    const stored = localStorage.getItem(STORAGE_KEYS.INVOICES);
    const list: InvoiceDetail[] = stored ? JSON.parse(stored) : INITIAL_INVOICES;

    const newInvoice: InvoiceDetail = {
      id: 'inv-' + Math.random().toString(36).substring(2, 9),
      tenantId: 'tenant-demo-1',
      status: 'NEEDS_REVIEW',
      originalFileName: file.name,
      contentType: file.type || 'application/pdf',
      fileSize: file.size,
      fileHash: 'hash-' + Date.now(),
      invoiceNumber: 'INV-' + Math.floor(1000 + Math.random() * 9000),
      invoiceDate: new Date().toISOString().split('T')[0],
      dueDate: new Date(Date.now() + 86400000 * 30).toISOString().split('T')[0],
      currency: 'INR',
      vendorName: 'Uploaded Vendor Services',
      vendorGstin: '29ABCDE1234F1Z5',
      buyerGstin: '29XYZAB5678C1Z2',
      subtotal: 25000.0,
      cgstAmount: 2250.0,
      sgstAmount: 2250.0,
      igstAmount: 0.0,
      totalAmount: 29500.0,
      overallConfidence: 0.95,
      hasValidationErrors: false,
      hasValidationWarnings: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      metadata: {},
      lineItems: [
        {
          id: 'li-new-1',
          lineNumber: 1,
          description: 'Consulting & Setup Services',
          hsnSac: '998313',
          quantity: 1,
          unitPrice: 25000.0,
          taxRate: 18.0,
          cgstAmount: 2250.0,
          sgstAmount: 2250.0,
          igstAmount: 0.0,
          totalAmount: 29500.0,
          confidence: 0.95,
        },
      ],
    };

    list.unshift(newInvoice);
    localStorage.setItem(STORAGE_KEYS.INVOICES, JSON.stringify(list));
    return newInvoice;
  },

  getInvoice: (id: string): InvoiceDetail => {
    const stored = localStorage.getItem(STORAGE_KEYS.INVOICES);
    const list: InvoiceDetail[] = stored ? JSON.parse(stored) : INITIAL_INVOICES;
    const found = list.find((i) => i.id === id);
    if (!found) throw new Error('Invoice not found');
    return found;
  },
};
