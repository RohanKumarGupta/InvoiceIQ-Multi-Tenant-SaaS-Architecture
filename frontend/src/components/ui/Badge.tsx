import React from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { InvoiceStatus } from '../../types/invoice';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  status?: InvoiceStatus;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info';
}

export const Badge: React.FC<BadgeProps> = ({
  status,
  variant,
  children,
  className,
  ...props
}) => {
  let style = 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300';

  if (status) {
    switch (status) {
      case 'UPLOADED':
        style = 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300 border border-slate-300 dark:border-slate-700';
        break;
      case 'PROCESSING':
        style = 'bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300 border border-blue-200 dark:border-blue-900 animate-pulse';
        break;
      case 'EXTRACTED':
        style = 'bg-purple-50 text-purple-700 dark:bg-purple-950 dark:text-purple-300 border border-purple-200 dark:border-purple-900';
        break;
      case 'NEEDS_REVIEW':
        style = 'bg-amber-50 text-amber-700 dark:bg-amber-950 dark:text-amber-300 border border-amber-200 dark:border-amber-900 font-semibold';
        break;
      case 'APPROVED':
        style = 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-900 font-medium';
        break;
      case 'REJECTED':
      case 'FAILED':
        style = 'bg-red-50 text-red-700 dark:bg-red-950 dark:text-red-300 border border-red-200 dark:border-red-900';
        break;
      case 'EXPORTED':
        style = 'bg-teal-50 text-teal-700 dark:bg-teal-950 dark:text-teal-300 border border-teal-200 dark:border-teal-900';
        break;
    }
  } else if (variant) {
    switch (variant) {
      case 'success':
        style = 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-200';
        break;
      case 'warning':
        style = 'bg-amber-50 text-amber-700 dark:bg-amber-950 dark:text-amber-300 border border-amber-200';
        break;
      case 'danger':
        style = 'bg-red-50 text-red-700 dark:bg-red-950 dark:text-red-300 border border-red-200';
        break;
      case 'info':
        style = 'bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300 border border-blue-200';
        break;
    }
  }

  return (
    <span
      className={twMerge(
        clsx(
          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs tracking-wide',
          style,
          className
        )
      )}
      {...props}
    >
      {children || (status ? status.replace('_', ' ') : null)}
    </span>
  );
};
