import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { UserSession } from '@/types';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/services/api/apiClient';
import { authService } from '@/services/auth/authService';
import { LoginRequest, RegisterRequest } from '@/types';

interface AuthContextType {
  user: UserSession | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isCustomer: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  // Demo switch helper for effortless design review without needing active backend database seeding
  setDemoUser: (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => Promise<void> | void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSession | null>(() => {
    try {
      const stored = localStorage.getItem(USER_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as UserSession;
        // Purge legacy mock tokens so user is not stuck in 403 forbidden state
        if (parsed?.token && parsed.token.startsWith('demo-jwt-')) {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          localStorage.removeItem(USER_STORAGE_KEY);
          return null;
        }
        return parsed;
      }
    } catch {}
    return null;
  });

  const [isLoading, setIsLoading] = useState<boolean>(false);

  const persistSession = (session: UserSession) => {
    setUser(session);
    try {
      localStorage.setItem(TOKEN_STORAGE_KEY, session.token);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(session));
    } catch {}
  };

  const clearSession = useCallback(() => {
    setUser(null);
    try {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
    } catch {}
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      clearSession();
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [clearSession]);

  const login = async (credentials: LoginRequest) => {
    setIsLoading(true);
    try {
      const res = await authService.login(credentials);
      const session: UserSession = {
        token: res.token,
        tokenType: res.tokenType,
        username: res.username,
        fullName: res.fullName,
        role: res.role,
        accountId: res.accountId,
        accountNumber: res.accountNumber,
      };
      persistSession(session);
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegisterRequest) => {
    setIsLoading(true);
    try {
      const res = await authService.register(data);
      const session: UserSession = {
        token: res.token,
        tokenType: res.tokenType,
        username: res.username,
        fullName: res.fullName,
        role: res.role,
        accountId: res.accountId,
        accountNumber: res.accountNumber,
      };
      persistSession(session);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    clearSession();
  };

  const setDemoUser = async (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => {
    setIsLoading(true);
    try {
      if (role === 'ROLE_CUSTOMER') {
        await login({ username: 'alice@example.com', password: 'Password123' });
      } else {
        await login({ username: 'admin@example.com', password: 'Password123' });
      }
    } catch {
      // Offline fallback if backend isn't reachable
      if (role === 'ROLE_CUSTOMER') {
        persistSession({
          token: 'demo-jwt-customer-token',
          tokenType: 'Bearer',
          username: 'alice@example.com',
          fullName: 'Alice Spender',
          role: 'ROLE_CUSTOMER',
          accountId: 1,
          accountNumber: '1000000001',
        });
      } else {
        persistSession({
          token: 'demo-jwt-admin-token',
          tokenType: 'Bearer',
          username: 'admin@example.com',
          fullName: 'Marcus Vance',
          role: 'ROLE_ADMIN',
          accountId: null,
          accountNumber: null,
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  const isAuthenticated = !!user?.token;
  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isCustomer = user?.role === 'ROLE_CUSTOMER';

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isAdmin,
        isCustomer,
        isLoading,
        login,
        register,
        logout,
        setDemoUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

