import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { UserSession, LoginRequest, RegisterRequest } from '@/types';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY, isMockSession } from '@/services/api/apiClient';
import { authService } from '@/services/auth/authService';
import { MOCK_USER_CUSTOMER, MOCK_USER_ADMIN } from '@/services/mock/mockData';

interface AuthContextType {
  user: UserSession | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isCustomer: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  setDemoUser: (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => Promise<void> | void;
  enableInstantOfflineDemo: (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSession | null>(() => {
    try {
      const stored = localStorage.getItem(USER_STORAGE_KEY);
      if (stored) {
        return JSON.parse(stored) as UserSession;
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
      // Don't auto-clear mock demo sessions on unauthorized events
      if (!isMockSession()) {
        clearSession();
      }
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
        isMock: false,
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
        isMock: false,
      };
      persistSession(session);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    clearSession();
  };

  const enableInstantOfflineDemo = (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => {
    if (role === 'ROLE_CUSTOMER') {
      persistSession({
        ...MOCK_USER_CUSTOMER,
        isMock: true,
      });
    } else {
      persistSession({
        ...MOCK_USER_ADMIN,
        isMock: true,
      });
    }
  };

  const setDemoUser = async (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => {
    setIsLoading(true);
    try {
      if (role === 'ROLE_CUSTOMER') {
        await login({ username: 'alice@example.com', password: 'Password123' });
      } else {
        await login({ username: 'admin@example.com', password: 'Password123' });
      }
    } catch (err) {
      console.warn('Backend currently asleep or unreachable; falling back to instant interactive demo:', err);
      enableInstantOfflineDemo(role);
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
        enableInstantOfflineDemo,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
