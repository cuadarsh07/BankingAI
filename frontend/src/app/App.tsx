import React, { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { AppProviders } from './providers';
import { router } from './router';
import { ENV } from '@/config/env';

export const App: React.FC = () => {
  useEffect(() => {
    // Non-blocking early wake-up ping for Render cold-starts
    try {
      const baseUrl = ENV.API_BASE_URL;
      if (baseUrl) {
        fetch(`${baseUrl}/api/auth/health`, { method: 'GET', mode: 'cors' }).catch(() => {
          fetch(`${baseUrl}/`, { method: 'GET', mode: 'no-cors' }).catch(() => {});
        });
      }
    } catch {}
  }, []);

  return (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  );
};

