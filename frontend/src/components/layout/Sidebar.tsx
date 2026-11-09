import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  FileText, 
  UploadCloud, 
  MessageSquare, 
  Settings, 
  ReceiptText 
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { tenant } = useAuth();

  const navigation = [
    { name: 'Dashboard', href: '/', icon: LayoutDashboard },
    { name: 'Invoices', href: '/invoices', icon: FileText },
    { name: 'Upload', href: '/upload', icon: UploadCloud },
    { name: 'Ask Your Books', href: '/reports', icon: MessageSquare },
    { name: 'Settings', href: '/settings', icon: Settings },
  ];

  return (
    <aside className="w-64 border-r border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 flex flex-col shrink-0 min-h-screen">
      {/* Brand Header */}
      <div className="h-16 flex items-center px-6 border-b border-slate-200 dark:border-slate-800 gap-3">
        <div className="h-9 w-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white shadow-sm">
          <ReceiptText className="h-5 w-5" />
        </div>
        <div>
          <span className="font-bold text-base text-slate-900 dark:text-slate-100 tracking-tight">InvoiceIQ</span>
          <span className="text-[10px] block uppercase font-medium tracking-wider text-indigo-600 dark:text-indigo-400">GST Smart Engine</span>
        </div>
      </div>

      {/* Tenant Indicator */}
      {tenant && (
        <div className="px-6 py-3 border-b border-slate-100 dark:border-slate-800/60 bg-slate-50/50 dark:bg-slate-900/50">
          <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">Tenant Workspace</p>
          <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 truncate">{tenant.name}</p>
        </div>
      )}

      {/* Navigation links */}
      <nav className="flex-1 px-4 py-5 space-y-1">
        {navigation.map((item) => (
          <NavLink
            key={item.name}
            to={item.href}
            end={item.href === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/70 dark:text-indigo-300'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-100'
              }`
            }
          >
            <item.icon className="h-5 w-5 shrink-0" />
            <span>{item.name}</span>
          </NavLink>
        ))}
      </nav>

      {/* Bottom Footer Info */}
      <div className="p-4 border-t border-slate-200 dark:border-slate-800 text-xs text-slate-400 text-center">
        v0.1.0 • Enterprise Edition
      </div>
    </aside>
  );
};
