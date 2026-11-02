import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { useTheme } from '@/hooks/useTheme';
import { Sun, Moon, Bell, LogOut, Menu, User, Shield, ArrowRightLeft } from 'lucide-react';
import { Dropdown } from '../ui/Dropdown';
import { Button } from '../ui/Button';

interface NavbarProps {
  onMobileMenuToggle: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onMobileMenuToggle }) => {
  const { user, isAuthenticated, isAdmin, logout, setDemoUser } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const customerNavLinks = [
    { to: '/dashboard', label: 'DASHBOARD', num: '01' },
    { to: '/accounts', label: 'ACCOUNTS', num: '02' },
    { to: '/transfers', label: 'TRANSFERS', num: '03' },
    { to: '/transactions', label: 'TRANSACTIONS', num: '04' },
    { to: '/insights', label: 'INSIGHTS', num: '05' },
    { to: '/assistant', label: 'COPILOT', num: '06' },
    { to: '/security-center', label: 'SECURITY', num: '07' },
    { to: '/settings', label: 'SETTINGS', num: '08' },
  ];

  const adminNavLinks = [
    { to: '/admin/dashboard', label: 'ADMIN METRICS', num: '01' },
    { to: '/admin/accounts', label: 'ACCOUNT DIRECTORY', num: '02' },
    { to: '/admin/fraud-monitoring', label: 'FRAUD SHIELD', num: '03' },
    { to: '/dashboard', label: 'SWITCH TO CUSTOMER VIEW', num: '04' },
  ];

  const navLinks = isAdmin ? adminNavLinks : customerNavLinks;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="sticky top-0 z-40 w-full bg-background/95 backdrop-blur-md border-b border-border">
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-8 h-16 flex items-center justify-between">
        {/* Brand / Logo */}
        <div className="flex items-center gap-6">
          <NavLink to="/" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-md bg-accent/15 border border-accent/30 flex items-center justify-center text-accent group-hover:scale-105 transition-transform">
              <Shield className="w-4 h-4 fill-accent/20" />
            </div>
            <div className="flex flex-col">
              <span className="font-heading font-extrabold text-base tracking-tight text-foreground flex items-center gap-1.5">
                AETHEL BANK
                <span className="inline-flex items-center px-1.5 py-0.5 text-[9px] font-mono rounded bg-accent/15 text-accent border border-accent/25">
                  AI SHIELD
                </span>
              </span>
              <span className="text-[10px] font-mono text-foreground-muted hidden sm:inline">
                FRAUD DEFENSE SYSTEM
              </span>
            </div>
          </NavLink>
        </div>

        {/* Minimal Horizontal Navigation (inspired by dhruvgupta.co) */}
        {isAuthenticated && (
          <nav className="hidden lg:flex items-center space-x-1">
            {navLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `relative py-2 px-3 text-xs font-mono tracking-tech transition-colors flex items-center gap-1.5 ${
                    isActive
                      ? 'text-foreground font-semibold'
                      : 'text-foreground-secondary hover:text-foreground hover:bg-background-secondary/60'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <span className={isActive ? 'text-accent font-bold' : 'text-foreground-muted'}>
                      {link.num}
                    </span>
                    <span>{link.label}</span>
                    {isActive && (
                      <span className="absolute bottom-[-17px] left-0 right-0 h-[2px] bg-accent" />
                    )}
                  </>
                )}
              </NavLink>
            ))}
          </nav>
        )}

        {/* Right Area: Controls & Profile */}
        <div className="flex items-center space-x-2 sm:space-x-3">
          {/* Quick Demo Switcher (Instant role toggle for testing without backend setup) */}
          <div className="hidden sm:flex items-center">
            <button
              onClick={() => setDemoUser(isAdmin ? 'ROLE_CUSTOMER' : 'ROLE_ADMIN')}
              className="text-[10px] font-mono px-2 py-1 rounded-sm border border-border bg-background-elevated hover:border-accent text-foreground-secondary hover:text-foreground flex items-center gap-1 transition-colors"
              title="Quick Toggle Role (Demo Helper)"
            >
              <ArrowRightLeft className="w-3 h-3 text-accent" />
              <span>{isAdmin ? 'MOCK: ADMIN' : 'MOCK: CUSTOMER'}</span>
            </button>
          </div>

          {/* Theme Toggle */}
          <button
            onClick={toggleTheme}
            className="p-2 text-foreground-secondary hover:text-foreground rounded-sm border border-border bg-background-secondary hover:bg-background-elevated transition-colors"
            aria-label="Toggle visual theme"
          >
            {theme === 'dark' ? (
              <Sun className="w-4 h-4 text-accent" />
            ) : (
              <Moon className="w-4 h-4 text-accent" />
            )}
          </button>

          {/* Notification Placeholder */}
          {isAuthenticated && (
            <button
              className="p-2 text-foreground-secondary hover:text-foreground rounded-sm border border-border bg-background-secondary hover:bg-background-elevated transition-colors relative"
              aria-label="Notifications"
            >
              <Bell className="w-4 h-4" />
              <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-accent rounded-full" />
            </button>
          )}

          {/* Profile Dropdown or Login Button */}
          {isAuthenticated && user ? (
            <Dropdown
              trigger={
                <div className="flex items-center gap-2 pl-2 cursor-pointer group">
                  <div className="w-8 h-8 rounded-sm bg-background-elevated border border-border group-hover:border-accent flex items-center justify-center font-mono text-xs font-semibold text-accent transition-colors">
                    {user.fullName ? user.fullName.charAt(0) : 'U'}
                  </div>
                  <div className="hidden xl:block text-left">
                    <p className="text-xs font-heading font-medium text-foreground leading-tight">
                      {user.fullName}
                    </p>
                    <p className="text-[10px] font-mono text-foreground-muted">
                      {isAdmin ? 'ADMINISTRATOR' : 'CUSTOMER'}
                    </p>
                  </div>
                </div>
              }
              items={[
                {
                  id: 'profile',
                  label: user.username,
                  icon: <User className="w-3.5 h-3.5" />,
                  onClick: () => {},
                },
                {
                  id: 'role',
                  label: `Role: ${user.role}`,
                  icon: <Shield className="w-3.5 h-3.5" />,
                  onClick: () => {},
                },
                {
                  id: 'logout',
                  label: 'Sign Out Session',
                  icon: <LogOut className="w-3.5 h-3.5" />,
                  danger: true,
                  onClick: handleLogout,
                },
              ]}
            />
          ) : (
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={() => navigate('/login')}>
                LOG IN
              </Button>
              <Button variant="primary" size="sm" onClick={() => navigate('/register')}>
                OPEN ACCOUNT
              </Button>
            </div>
          )}

          {/* Mobile Menu Hamburger */}
          {isAuthenticated && (
            <button
              onClick={onMobileMenuToggle}
              className="lg:hidden p-2 text-foreground-secondary hover:text-foreground rounded-sm border border-border bg-background-secondary"
              aria-label="Open mobile navigation"
            >
              <Menu className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>
    </header>
  );
};

