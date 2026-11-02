export const ENV = {
  // Empty string base URL → all service paths like /api/auth/login are relative to the
  // current origin, so Vite's dev proxy forwards them to localhost:8080 without CORS issues.
  // In production build set VITE_API_BASE_URL='' or the CDN/reverse-proxy handles routing.
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL ?? '',
  APP_NAME: import.meta.env.VITE_APP_NAME || 'AETHEL FINTECH',
  IS_DEV: import.meta.env.DEV,
} as const;

