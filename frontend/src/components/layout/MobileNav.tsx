import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { X, LogOut, ArrowRightLeft } from 'lucide-react';
import { maskAccountNumber } from '@/lib/utils';

interface MobileNavProps {
  isOpen: boolean;
  onClose: () => void;
}

export const MobileNav: React.FC<MobileNavProps> = ({ isOpen, onClose }) => {
  const { user, isAdmin, logout, setDemoUser } = useAuth();

  if (!isOpen) return null;

  const customerNavLinks = [
    { to: '/dashboard', label: 'DASHBOARD', num: '01' },
    { to: '/accounts', label: 'ACCOUNTS', num: '02' },
    { to: '/transfers', label: 'TRANSFERS', num: '03' },
    { to: '/transactions', label: 'TRANSACTIONS', num: '04' },
    { to: '/insights', label: 'INSIGHTS', num: '05' },
    { to: '/assistant', label: 'AI COPILOT', num: '06' },
    { to: '/security-center', label: 'SECURITY', num: '07' },
    { to: '/settings', label: 'SETTINGS', num: '08' },
  ];

  const adminNavLinks = [
    { to: '/admin/dashboard', label: 'ADMIN METRICS', num: '01' },
    { to: '/admin/accounts', label: 'ALL ACCOUNTS DIRECTORY', num: '02' },
    { to: '/admin/fraud-monitoring', label: 'FRAUD SHIELD', num: '03' },
    { to: '/dashboard', label: 'CUSTOMER WORKSPACE', num: '04' },
  ];

  const navLinks = isAdmin ? adminNavLinks : customerNavLinks;

  return (
    <div className="fixed inset-0 z-50 lg:hidden flex">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/80 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Drawer */}
      <div className="relative w-4/5 max-w-sm bg-background border-r border-border h-full flex flex-col justify-between p-6 z-10 animate-in slide-in-from-left duration-200">
        <div>
          {/* Header */}
          <div className="flex items-center justify-between pb-6 border-b border-border">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 bg-accent rounded-sm" />
              <span className="font-heading font-bold text-foreground tracking-tight uppercase">
                AETHEL BANK
              </span>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 text-foreground-muted hover:text-foreground rounded-sm border border-border"
              aria-label="Close menu"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* User badge */}
          {user && (
            <div className="my-5 p-3.5 bg-background-secondary border border-border rounded-sm">
              <p className="text-xs font-heading font-semibold text-foreground">
                {user.fullName}
              </p>
              <p className="text-[10px] font-mono text-accent mt-0.5">
                {user.role} {user.accountNumber ? `• ${maskAccountNumber(user.accountNumber)}` : ''}
              </p>
            </div>
          )}

          {/* Links */}
          <nav className="space-y-1 mt-4">
            {navLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                onClick={onClose}
                className={({ isActive }) =>
                  `flex items-center justify-between py-3 px-3 rounded-sm text-xs font-mono tracking-tech transition-colors ${
                    isActive
                      ? 'bg-background-elevated text-foreground font-bold border-l-2 border-accent'
                      : 'text-foreground-secondary hover:text-foreground hover:bg-background-secondary'
                  }`
                }
              >
                <span>{link.label}</span>
                <span className="text-accent text-[11px] font-mono">{link.num}</span>
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Footer actions */}
        <div className="pt-6 border-t border-border space-y-3">
          <button
            onClick={() => {
              setDemoUser(isAdmin ? 'ROLE_CUSTOMER' : 'ROLE_ADMIN');
              onClose();
            }}
            className="w-full flex items-center justify-center gap-2 py-2 text-xs font-mono border border-border text-foreground-secondary hover:text-foreground bg-background-secondary rounded-sm"
          >
            <ArrowRightLeft className="w-3.5 h-3.5 text-accent" />
            <span>SWITCH TO {isAdmin ? 'CUSTOMER' : 'ADMIN'}</span>
          </button>

          <button
            onClick={() => {
              logout();
              onClose();
            }}
            className="w-full flex items-center justify-center gap-2 py-2 text-xs font-mono text-status-error border border-status-error-border bg-status-error-bg rounded-sm"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>SIGN OUT</span>
          </button>
        </div>
      </div>
    </div>
  );
};

