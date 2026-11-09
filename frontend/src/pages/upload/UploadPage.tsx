import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  UploadCloud, 
  FileText, 
  AlertCircle, 
  Loader2, 
  ArrowRight, 
  X 
} from 'lucide-react';
import { invoiceApi } from '../../api/invoiceApi';
import { InvoiceDetail } from '../../types/invoice';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';

interface UploadTask {
  id: string;
  file: File;
  status: 'pending' | 'uploading' | 'success' | 'error';
  errorMessage?: string;
  result?: InvoiceDetail;
}

export const UploadPage: React.FC = () => {
  const [tasks, setTasks] = useState<UploadTask[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  const handleFiles = (files: FileList | File[]) => {
    const newTasks: UploadTask[] = Array.from(files).map((file) => ({
      id: `${file.name}-${Date.now()}-${Math.random()}`,
      file,
      status: 'pending',
    }));

    setTasks((prev) => [...newTasks, ...prev]);
    processQueue(newTasks);
  };

  const processQueue = async (queue: UploadTask[]) => {
    for (const task of queue) {
      // Client-side pre-validation
      if (task.file.size > 10 * 1024 * 1024) {
        updateTaskStatus(task.id, 'error', 'File exceeds maximum 10MB limit');
        continue;
      }

      const validExts = ['.pdf', '.png', '.jpg', '.jpeg'];
      const hasValidExt = validExts.some((ext) => task.file.name.toLowerCase().endsWith(ext));
      if (!hasValidExt) {
        updateTaskStatus(task.id, 'error', 'Unsupported format. Allowed: PDF, PNG, JPG');
        continue;
      }

      updateTaskStatus(task.id, 'uploading');

      try {
        const response = await invoiceApi.uploadInvoice(task.file);
        setTasks((prev) =>
          prev.map((t) => (t.id === task.id ? { ...t, status: 'success', result: response } : t))
        );
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Upload failed';
        updateTaskStatus(task.id, 'error', msg);
      }
    }
  };

  const updateTaskStatus = (id: string, status: UploadTask['status'], error?: string) => {
    setTasks((prev) =>
      prev.map((t) => (t.id === id ? { ...t, status, errorMessage: error } : t))
    );
  };

  const removeTask = (id: string) => {
    setTasks((prev) => prev.filter((t) => t.id !== id));
  };

  const onDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const onDragLeave = () => {
    setIsDragging(false);
  };

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          Upload Invoice Documents
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          Upload PDF bills, scans, or vendor receipt images for instant GST & line-item AI extraction.
        </p>
      </div>

      {/* Drag & Drop Zone */}
      <div
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`border-2 border-dashed rounded-2xl p-12 text-center cursor-pointer transition-all duration-200 ${
          isDragging
            ? 'border-indigo-600 bg-indigo-50/70 dark:bg-indigo-950/40 scale-[1.01]'
            : 'border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 hover:border-indigo-400 dark:hover:border-indigo-600'
        }`}
      >
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept=".pdf,.png,.jpg,.jpeg"
          className="hidden"
          onChange={(e) => e.target.files && handleFiles(e.target.files)}
        />
        <div className="h-16 w-16 mx-auto rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mb-4 shadow-sm">
          <UploadCloud className="h-8 w-8" />
        </div>
        <p className="text-base font-semibold text-slate-900 dark:text-slate-100">
          Click to upload or drag and drop invoice files here
        </p>
        <p className="text-xs text-slate-500 dark:text-slate-400 mt-1.5">
          Supports multi-page PDF, PNG, JPG (up to 10MB per file) • SHA-256 deduplicated
        </p>
      </div>

      {/* Upload Tasks List */}
      {tasks.length > 0 && (
        <Card className="space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
              Processing Queue ({tasks.length} {tasks.length === 1 ? 'file' : 'files'})
            </h3>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setTasks([])}
              className="text-xs text-slate-500 hover:text-slate-800"
            >
              Clear Queue
            </Button>
          </div>

          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {tasks.map((task) => (
              <div key={task.id} className="py-3 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="p-2 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 shrink-0">
                    <FileText className="h-5 w-5" />
                  </div>
                  <div className="truncate">
                    <p className="text-sm font-medium text-slate-800 dark:text-slate-200 truncate">
                      {task.file.name}
                    </p>
                    <p className="text-xs text-slate-400">
                      {(task.file.size / 1024).toFixed(1)} KB
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  {task.status === 'uploading' && (
                    <span className="inline-flex items-center gap-1.5 text-xs text-indigo-600 font-medium">
                      <Loader2 className="h-4 w-4 animate-spin" /> Uploading & Queuing...
                    </span>
                  )}
                  {task.status === 'success' && (
                    <div className="flex items-center gap-2">
                      <Badge status={task.result?.status || 'UPLOADED'} />
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => navigate(`/invoices/${task.result?.id}/review`)}
                      >
                        Review <ArrowRight className="h-3 w-3 ml-1" />
                      </Button>
                    </div>
                  )}
                  {task.status === 'error' && (
                    <div className="flex items-center gap-1.5 text-xs text-red-600 dark:text-red-400 font-medium">
                      <AlertCircle className="h-4 w-4" />
                      <span>{task.errorMessage}</span>
                    </div>
                  )}

                  <button
                    onClick={() => removeTask(task.id)}
                    className="p-1 rounded text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
};
