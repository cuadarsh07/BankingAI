import React from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { PublicOnlyRoute } from '@/components/layout/PublicOnlyRoute';

// Auth Pages
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';

// Customer Feature Pages
import { CustomerDashboardPage } from '@/features/dashboard/pages/CustomerDashboardPage';
import { AccountsPage } from '@/features/accounts/pages/AccountsPage';
import { TransfersPage } from '@/features/transfers/pages/TransfersPage';
import { TransactionsPage } from '@/features/transactions/pages/TransactionsPage';

// Admin Feature Pages
import { AdminDashboardPage } from '@/features/admin/pages/AdminDashboardPage';
import { AdminAccountsPage } from '@/features/admin/pages/AdminAccountsPage';

// Fully Implemented Pages
import { InsightsPage } from '@/features/analytics/pages/InsightsPage';
import { AssistantPage } from '@/features/assistant/pages/AssistantPage';
import { SettingsPage } from '@/features/settings/pages/SettingsPage';
import { SecurityCenterPage } from '@/features/security/pages/SecurityCenterPage';
import { AdminFraudMonitoringPage } from '@/features/admin/pages/AdminFraudMonitoringPage';

// 404 Component
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-[70vh] flex items-center justify-center p-4">
      <Card withCrosshairs className="max-w-md p-8 text-center space-y-4">
        <span className="text-4xl font-heading font-extrabold text-accent">404</span>
        <h2 className="text-xl font-heading font-bold uppercase text-foreground">
          DESTINATION NOT FOUND
        </h2>
        <p className="text-xs font-mono text-foreground-secondary">
          The requested page route does not exist in the banking navigation tree.
        </p>
        <div className="pt-2">
          <Button variant="primary" size="sm" onClick={() => (window.location.href = '/')}>
            RETURN TO DASHBOARD
          </Button>
        </div>
      </Card>
    </div>
  );
};

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/dashboard" replace />,
  },
  // Public Routes (Auth)
  {
    path: '/login',
    element: (
      <PublicOnlyRoute>
        <LoginPage />
      </PublicOnlyRoute>
    ),
  },
  {
    path: '/register',
    element: (
      <PublicOnlyRoute>
        <RegisterPage />
      </PublicOnlyRoute>
    ),
  },

  // Customer Routes Shell
  {
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        path: '/dashboard',
        element: <CustomerDashboardPage />,
      },
      {
        path: '/accounts',
        element: <AccountsPage />,
      },
      {
        path: '/transfers',
        element: <TransfersPage />,
      },
      {
        path: '/transactions',
        element: <TransactionsPage />,
      },
      {
        path: '/insights',
        element: <InsightsPage />,
      },
      {
        path: '/assistant',
        element: <AssistantPage />,
      },
      {
        path: '/security-center',
        element: <SecurityCenterPage />,
      },
      {
        path: '/settings',
        element: <SettingsPage />,
      },
    ],
  },

  // Admin Routes Shell
  {
    path: '/admin',
    element: (
      <ProtectedRoute requiredRole="ROLE_ADMIN">
        <AdminLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <Navigate to="/admin/dashboard" replace />,
      },
      {
        path: 'dashboard',
        element: <AdminDashboardPage />,
      },
      {
        path: 'accounts',
        element: <AdminAccountsPage />,
      },
      {
        path: 'fraud-monitoring',
        element: <AdminFraudMonitoringPage />,
      },
    ],
  },

  // Fallback Route
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
