import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './pages/auth/LoginPage';
import { SignupPage } from './pages/auth/SignupPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { UploadPage } from './pages/upload/UploadPage';
import { InvoiceListPage } from './pages/invoices/InvoiceListPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 1000 * 30, // 30 seconds
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              {/* Public Routes */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />

              {/* Protected App Routes */}
              <Route element={<AppLayout />}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/invoices" element={<InvoiceListPage />} />
                <Route path="/upload" element={<UploadPage />} />
                {/* Review screen placeholder (Phase 6 target) */}
                <Route
                  path="/invoices/:id/review"
                  element={
                    <div className="p-8 text-center text-slate-500">
                      <h2 className="text-xl font-bold text-slate-800 dark:text-slate-200">
                        Review Split View Screen
                      </h2>
                      <p className="mt-2 text-sm">
                        This view will be fully implemented in <strong>Phase 6</strong> with the document viewer on the left and live editable tax form on the right.
                      </p>
                    </div>
                  }
                />
                <Route
                  path="/reports"
                  element={
                    <div className="p-8 text-center text-slate-500">
                      <h2 className="text-xl font-bold text-slate-800 dark:text-slate-200">
                        "Ask Your Books" Reporting
                      </h2>
                      <p className="mt-2 text-sm">
                        Natural-language reporting with safe query plans will be activated in <strong>Phase 8</strong>.
                      </p>
                    </div>
                  }
                />
                <Route
                  path="/settings"
                  element={
                    <div className="p-8 text-center text-slate-500">
                      <h2 className="text-xl font-bold text-slate-800 dark:text-slate-200">
                        Workspace Settings
                      </h2>
                      <p className="mt-2 text-sm">
                        Team management, export profiles, and categories will be activated in <strong>Phase 9</strong>.
                      </p>
                    </div>
                  }
                />
              </Route>

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;
