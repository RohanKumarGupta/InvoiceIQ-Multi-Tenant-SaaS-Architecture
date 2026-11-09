import React, { createContext, useContext, useEffect, useState } from 'react';
import { TenantDetails, UserProfile } from '../types/auth';
import { authApi, LoginPayload, SignupPayload } from '../api/authApi';
import { getAccessToken, setAccessToken } from '../api/client';

interface AuthContextType {
  user: UserProfile | null;
  tenant: TenantDetails | null;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  signup: (payload: SignupPayload) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [tenant, setTenant] = useState<TenantDetails | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchUserAndTenant = async () => {
    try {
      const userData = await authApi.getMe();
      setUser(userData);
      const tenantData = await authApi.getCurrentTenant();
      setTenant(tenantData);
    } catch {
      setUser(null);
      setTenant(null);
      setAccessToken(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (getAccessToken()) {
      fetchUserAndTenant();
    } else {
      setIsLoading(false);
    }
  }, []);

  const login = async (payload: LoginPayload) => {
    setIsLoading(true);
    try {
      const auth = await authApi.login(payload);
      setUser(auth.user);
      const tenantData = await authApi.getCurrentTenant();
      setTenant(tenantData);
    } finally {
      setIsLoading(false);
    }
  };

  const signup = async (payload: SignupPayload) => {
    setIsLoading(true);
    try {
      const auth = await authApi.signup(payload);
      setUser(auth.user);
      const tenantData = await authApi.getCurrentTenant();
      setTenant(tenantData);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
      setTenant(null);
    }
  };

  const refreshProfile = async () => {
    await fetchUserAndTenant();
  };

  return (
    <AuthContext.Provider value={{ user, tenant, isLoading, login, signup, logout, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
