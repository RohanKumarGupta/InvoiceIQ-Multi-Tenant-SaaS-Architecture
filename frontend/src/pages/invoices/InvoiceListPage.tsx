import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { 
  Search, 
  UploadCloud, 
  AlertTriangle, 
  ExternalLink, 
  ChevronLeft, 
  ChevronRight,
  FileText
} from 'lucide-react';
import { invoiceApi } from '../../api/invoiceApi';
import { InvoiceStatus } from '../../types/invoice';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Card } from '../../components/ui/Card';
import { Skeleton } from '../../components/ui/Skeleton';

const STATUS_FILTERS: { label: string; value: InvoiceStatus | 'ALL' }[] = [
  { label: 'All Invoices', value: 'ALL' },
  { label: 'Needs Review', value: 'NEEDS_REVIEW' },
  { label: 'Extracted', value: 'EXTRACTED' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Rejected', value: 'REJECTED' },
];

export const InvoiceListPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const currentStatus = (searchParams.get('status') as InvoiceStatus) || undefined;
  const currentPage = parseInt(searchParams.get('page') || '0', 10);
  const [searchTerm, setSearchTerm] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['invoices-list', currentStatus, currentPage],
    queryFn: () =>
      invoiceApi.listInvoices({
        status: currentStatus,
        page: currentPage,
        size: 15,
      }),
  });

  const invoices = data?.content || [];
  const totalPages = data?.totalPages || 1;

  const handleStatusFilter = (statusVal: InvoiceStatus | 'ALL') => {
    const params = new URLSearchParams(searchParams);
    if (statusVal === 'ALL') {
      params.delete('status');
    } else {
      params.set('status', statusVal);
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  const handlePageChange = (newPage: number) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', newPage.toString());
    setSearchParams(params);
  };

  // Filter client-side by search query
  const filteredInvoices = invoices.filter((inv) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    return (
      (inv.invoiceNumber && inv.invoiceNumber.toLowerCase().includes(term)) ||
      (inv.vendorName && inv.vendorName.toLowerCase().includes(term)) ||
      (inv.vendorGstin && inv.vendorGstin.toLowerCase().includes(term)) ||
      inv.originalFileName.toLowerCase().includes(term)
    );
  });

  return (
    <div className="space-y-6">
      {/* Title & Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
            Invoices & Expense Documents
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Search, filter, inspect GST breakdowns, and review AI extractions.
          </p>
        </div>
        <Link to="/upload">
          <Button className="shadow-sm">
            <UploadCloud className="h-4 w-4" />
            Upload New Invoice
          </Button>
        </Link>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4 space-y-4">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          {/* Status Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-2 md:pb-0">
            {STATUS_FILTERS.map((f) => {
              const active = (!currentStatus && f.value === 'ALL') || currentStatus === f.value;
              return (
                <button
                  key={f.value}
                  onClick={() => handleStatusFilter(f.value)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                    active
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
                  }`}
                >
                  {f.label}
                </button>
              );
            })}
          </div>

          {/* Search Box */}
          <div className="relative w-full md:w-72">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search invoice #, vendor, GSTIN..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3.5 py-1.5 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors"
            />
          </div>
        </div>
      </Card>

      {/* Invoice Table */}
      <Card className="p-0 overflow-hidden">
        {isLoading ? (
          <div className="p-8 space-y-4">
            <Skeleton className="h-6 w-full" />
            <Skeleton className="h-6 w-full" />
            <Skeleton className="h-6 w-full" />
          </div>
        ) : filteredInvoices.length === 0 ? (
          <div className="text-center py-16 text-slate-500">
            <div className="h-12 w-12 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center mx-auto text-slate-400 mb-3">
              <FileText className="h-6 w-6" />
            </div>
            <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">No invoices found</p>
            <p className="text-xs text-slate-400 mt-1">Try resetting the status filter or uploading an invoice.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-700 dark:text-slate-300">
              <thead className="text-xs uppercase bg-slate-50 dark:bg-slate-800/60 text-slate-500 border-b border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="px-5 py-3.5">Invoice #</th>
                  <th className="px-5 py-3.5">Vendor</th>
                  <th className="px-5 py-3.5">Vendor GSTIN</th>
                  <th className="px-5 py-3.5">Invoice Date</th>
                  <th className="px-5 py-3.5">Total Amount</th>
                  <th className="px-5 py-3.5">Status</th>
                  <th className="px-5 py-3.5">Confidence</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredInvoices.map((inv) => (
                  <tr
                    key={inv.id}
                    className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors"
                  >
                    <td className="px-5 py-4 font-semibold text-slate-900 dark:text-slate-100">
                      <div className="flex items-center gap-2">
                        <span>{inv.invoiceNumber || inv.originalFileName}</span>
                        {inv.hasValidationErrors && (
                          <span title="Validation Errors Detected" className="text-red-500">
                            <AlertTriangle className="h-4 w-4" />
                          </span>
                        )}
                        {!inv.hasValidationErrors && inv.hasValidationWarnings && (
                          <span title="Validation Warnings" className="text-amber-500">
                            <AlertTriangle className="h-4 w-4" />
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-4 font-medium text-slate-800 dark:text-slate-200">
                      {inv.vendorName || <span className="text-slate-400 italic">Unknown</span>}
                    </td>
                    <td className="px-5 py-4 font-mono text-xs">
                      {inv.vendorGstin || <span className="text-slate-400 italic">—</span>}
                    </td>
                    <td className="px-5 py-4 text-xs text-slate-500">
                      {inv.invoiceDate || '—'}
                    </td>
                    <td className="px-5 py-4 font-bold text-slate-900 dark:text-slate-100">
                      ₹{(inv.totalAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="px-5 py-4">
                      <Badge status={inv.status} />
                    </td>
                    <td className="px-5 py-4">
                      {inv.overallConfidence !== undefined ? (
                        <div className="flex items-center gap-1.5">
                          <div className="w-12 bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                inv.overallConfidence > 0.8
                                  ? 'bg-emerald-500'
                                  : inv.overallConfidence > 0.6
                                  ? 'bg-amber-500'
                                  : 'bg-red-500'
                              }`}
                              style={{ width: `${Math.round(inv.overallConfidence * 100)}%` }}
                            />
                          </div>
                          <span className="text-[11px] font-mono text-slate-500">
                            {Math.round(inv.overallConfidence * 100)}%
                          </span>
                        </div>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <Link to={`/invoices/${inv.id}/review`}>
                        <Button variant="outline" size="sm" className="font-medium text-xs">
                          Inspect & Review
                          <ExternalLink className="h-3 w-3 ml-1 text-slate-400" />
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="px-5 py-3 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
            <span>
              Page {currentPage + 1} of {totalPages}
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage === 0}
                onClick={() => handlePageChange(currentPage - 1)}
              >
                <ChevronLeft className="h-3.5 w-3.5" /> Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage + 1 >= totalPages}
                onClick={() => handlePageChange(currentPage + 1)}
              >
                Next <ChevronRight className="h-3.5 w-3.5" />
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};
