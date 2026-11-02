/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        background: {
          DEFAULT: 'var(--color-bg-main)',
          secondary: 'var(--color-bg-secondary)',
          elevated: 'var(--color-bg-elevated)',
          subtle: 'var(--color-bg-subtle)',
        },
        foreground: {
          DEFAULT: 'var(--color-text-primary)',
          secondary: 'var(--color-text-secondary)',
          muted: 'var(--color-text-muted)',
        },
        accent: {
          DEFAULT: '#FF5533',
          hover: '#FF6E50',
          active: '#E64424',
          subtle: 'rgba(255, 85, 51, 0.12)',
          border: 'rgba(255, 85, 51, 0.3)',
        },
        border: {
          DEFAULT: 'var(--color-border-subtle)',
          strong: 'var(--color-border-strong)',
          accent: 'var(--color-border-accent)',
        },
        status: {
          success: '#4EBA6F',
          'success-bg': 'rgba(78, 186, 111, 0.12)',
          'success-border': 'rgba(78, 186, 111, 0.25)',
          error: '#E55353',
          'error-bg': 'rgba(229, 83, 83, 0.12)',
          'error-border': 'rgba(229, 83, 83, 0.25)',
          warning: '#E8A338',
          'warning-bg': 'rgba(232, 163, 56, 0.12)',
          'warning-border': 'rgba(232, 163, 56, 0.25)',
          info: '#3D8BFD',
          'info-bg': 'rgba(61, 139, 253, 0.12)',
        },
      },
      fontFamily: {
        heading: ['Space Grotesk', 'Inter', '-apple-system', 'sans-serif'],
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'SF Mono', 'monospace'],
      },
      fontSize: {
        'display-2xl': ['4.5rem', { lineHeight: '1', letterSpacing: '-0.04em' }],
        'display-xl': ['3.75rem', { lineHeight: '1.05', letterSpacing: '-0.035em' }],
        'display-lg': ['3rem', { lineHeight: '1.1', letterSpacing: '-0.03em' }],
        'display-md': ['2.25rem', { lineHeight: '1.15', letterSpacing: '-0.025em' }],
        'display-sm': ['1.875rem', { lineHeight: '1.2', letterSpacing: '-0.02em' }],
      },
      borderRadius: {
        sm: '2px',
        DEFAULT: '4px',
        md: '6px',
        lg: '8px',
        xl: '12px',
      },
      boxShadow: {
        subtle: '0 1px 2px 0 rgba(0, 0, 0, 0.2)',
        card: '0 4px 20px -2px rgba(0, 0, 0, 0.35)',
        glow: '0 0 24px -4px rgba(255, 85, 51, 0.25)',
      },
      letterSpacing: {
        tighter: '-0.04em',
        tight: '-0.02em',
        editorial: '0.08em',
        tech: '0.12em',
      },
    },
  },
  plugins: [],
};

