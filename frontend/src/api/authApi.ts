import { apiClient, setAccessToken } from './client';
import { AuthResponse, TenantDetails, UserProfile } from '../types/auth';

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  tenantName: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export const authApi = {
  login: async (payload: LoginPayload): Promise<AuthResponse> => {
    const data = await apiClient<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    setAccessToken(data.accessToken);
    return data;
  },

  signup: async (payload: SignupPayload): Promise<AuthResponse> => {
    const data = await apiClient<AuthResponse>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    setAccessToken(data.accessToken);
    return data;
  },

  logout: async (): Promise<void> => {
    try {
      await apiClient<void>('/auth/logout', { method: 'POST' });
    } finally {
      setAccessToken(null);
    }
  },

  getMe: async (): Promise<UserProfile> => {
    return apiClient<UserProfile>('/auth/me');
  },

  getCurrentTenant: async (): Promise<TenantDetails> => {
    return apiClient<TenantDetails>('/tenants/current');
  },
};
