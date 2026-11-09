import { mockBackend } from './mockBackend';

const API_BASE = '/api/v1';

let accessToken: string | null = localStorage.getItem('access_token');

export const setAccessToken = (token: string | null) => {
  accessToken = token;
  if (token) {
    localStorage.setItem('access_token', token);
  } else {
    localStorage.removeItem('access_token');
  }
};

export const getAccessToken = () => accessToken;

export async function apiClient<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers || {});

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const config: RequestInit = {
    ...options,
    headers,
    credentials: 'include',
  };

  try {
    const response = await fetch(`${API_BASE}${endpoint}`, config);

    // If 401 and not already attempting a refresh, try silent token rotation
    if (response.status === 401 && endpoint !== '/auth/login' && endpoint !== '/auth/refresh') {
      try {
        const refreshRes = await fetch(`${API_BASE}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
        });
        if (refreshRes.ok) {
          const data = await refreshRes.json();
          setAccessToken(data.accessToken);
          headers.set('Authorization', `Bearer ${data.accessToken}`);
          const retryRes = await fetch(`${API_BASE}${endpoint}`, { ...config, headers });
          if (!retryRes.ok) throw new Error((await retryRes.json()).message || 'Request failed');
          if (retryRes.status === 204) return null as T;
          return retryRes.json();
        } else {
          setAccessToken(null);
          window.location.href = '/login';
        }
      } catch {
        setAccessToken(null);
        window.location.href = '/login';
      }
    }

    // If proxy returned 502/504 Bad Gateway / ECONNREFUSED because Spring Boot is down, fallback to mock demo
    if (response.status === 502 || response.status === 504) {
      console.warn(`[InvoiceIQ Demo Mode] Backend at localhost:8080 is unreachable. Using offline demo handler for ${endpoint}.`);
      return handleMockFallback<T>(endpoint, options);
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'An unexpected error occurred' }));
      throw new Error(errorData.message || errorData.error || `HTTP ${response.status}`);
    }

    if (response.status === 204) {
      return null as T;
    }

    return response.json();
  } catch (err: unknown) {
    // If network error / fetch failed entirely
    console.warn(`[InvoiceIQ Demo Mode] Network fetch failed: ${(err as Error).message}. Handling via demo mode for ${endpoint}.`);
    return handleMockFallback<T>(endpoint, options);
  }
}

function handleMockFallback<T>(endpoint: string, options: RequestInit): T {
  // Auth endpoints
  if (endpoint === '/auth/signup' && options.body) {
    const parsed = typeof options.body === 'string' ? JSON.parse(options.body) : {};
    return mockBackend.handleSignup(parsed) as unknown as T;
  }
  if (endpoint === '/auth/login') {
    const parsed = typeof options.body === 'string' ? JSON.parse(options.body) : {};
    return mockBackend.handleLogin(parsed.email) as unknown as T;
  }
  if (endpoint === '/auth/logout') {
    setAccessToken(null);
    return null as unknown as T;
  }
  if (endpoint === '/auth/me') {
    return mockBackend.getCurrentUser() as unknown as T;
  }
  if (endpoint === '/tenants/current') {
    return mockBackend.getCurrentTenant() as unknown as T;
  }

  // Invoice endpoints
  if (endpoint.startsWith('/invoices/upload') && options.body instanceof FormData) {
    const file = options.body.get('file') as File;
    return mockBackend.uploadInvoice(file) as unknown as T;
  }
  if (endpoint.startsWith('/invoices?') || endpoint === '/invoices') {
    const url = new URL(`http://localhost${endpoint}`);
    const status = url.searchParams.get('status') || undefined;
    return mockBackend.getInvoices(status) as unknown as T;
  }
  if (endpoint.match(/^\/invoices\/[a-zA-Z0-9-]+$/)) {
    const id = endpoint.split('/')[2];
    return mockBackend.getInvoice(id) as unknown as T;
  }

  return {} as T;
}
