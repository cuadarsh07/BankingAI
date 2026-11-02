import React, { useState } from 'react';
import { Outlet, NavLink } from 'react-router-dom';
import { Navbar } from './Navbar';
import { SecondaryBar } from './SecondaryBar';
import { MobileNav } from './MobileNav';
import { Footer } from './Footer';
import { ShieldAlert, Users, LayoutDashboard, ArrowLeft } from 'lucide-react';

export const AdminLayout: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground transition-colors">
      <Navbar onMobileMenuToggle={() => setMobileMenuOpen(true)} />
      <SecondaryBar />
      <MobileNav isOpen={mobileMenuOpen} onClose={() => setMobileMenuOpen(false)} />

      {/* Admin Command Sub-Header */}
      <div className="bg-status-error-bg/20 border-b border-status-error-border py-2 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs font-mono">
          <div className="flex items-center gap-2 text-status-error font-semibold">
            <ShieldAlert className="w-4 h-4 shrink-0" />
            <span>ELEVATED SECURITY CLEARANCE / ROLE_ADMIN ACTIVE</span>
          </div>
          <div className="flex items-center gap-4 text-foreground-secondary">
            <NavLink
              to="/admin/dashboard"
              className={({ isActive }) =>
                `flex items-center gap-1.5 hover:text-foreground transition-colors ${
                  isActive ? 'text-accent font-semibold underline' : ''
                }`
              }
            >
              <LayoutDashboard className="w-3.5 h-3.5" />
              <span>[01] SYSTEM METRICS</span>
            </NavLink>
            <NavLink
              to="/admin/accounts"
              className={({ isActive }) =>
                `flex items-center gap-1.5 hover:text-foreground transition-colors ${
                  isActive ? 'text-accent font-semibold underline' : ''
                }`
              }
            >
              <Users className="w-3.5 h-3.5" />
              <span>[02] ALL BANK ACCOUNTS</span>
            </NavLink>
            <NavLink
              to="/admin/fraud-monitoring"
              className={({ isActive }) =>
                `flex items-center gap-1.5 hover:text-foreground transition-colors ${
                  isActive ? 'text-accent font-semibold underline' : ''
                }`
              }
            >
              <ShieldAlert className="w-3.5 h-3.5" />
              <span>[03] FRAUD SHIELD</span>
            </NavLink>
            <NavLink
              to="/dashboard"
              className="flex items-center gap-1 hover:text-accent transition-colors text-foreground-muted"
            >
              <ArrowLeft className="w-3 h-3" />
              <span>CUSTOMER VIEW</span>
            </NavLink>
          </div>
        </div>
      </div>

      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-8 py-8 sm:py-10">
        <Outlet />
      </main>

      <Footer />
    </div>
  );
};

