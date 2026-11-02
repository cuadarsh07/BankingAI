import React from 'react';
import { useAuth } from '@/hooks/useAuth';
import { useTheme } from '@/hooks/useTheme';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { SectionLabel } from '@/components/ui/SectionLabel';
import {
  Sun,
  Moon,
  LogOut,
  User,
  Key,
  Globe,
} from 'lucide-react';
import { toast } from 'sonner';

export const SettingsPage: React.FC = () => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      <PageHeader
        index="[07]"
        tag="CONFIGURATION"
        title="SYSTEM SETTINGS"
        subtitle="APPLICATION PREFERENCES, PROFILE METRICS, AND SESSION CONTROLS"
      />

      <div className="space-y-6">
        {/* Profile & Identity Card */}
        <Card withCrosshairs className="p-6">
          <SectionLabel label="ACCOUNT IDENTITY" index="[01]" />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-4 text-xs font-mono">
            <div className="p-3.5 bg-background-elevated border border-border rounded-sm">
              <span className="text-foreground-muted uppercase tracking-tech">USER NAME</span>
              <p className="text-sm font-bold text-foreground mt-1 flex items-center gap-2">
                <User className="w-4 h-4 text-accent" />
                <span>{user?.fullName || 'Active User'}</span>
              </p>
            </div>

            <div className="p-3.5 bg-background-elevated border border-border rounded-sm">
              <span className="text-foreground-muted uppercase tracking-tech">AUTHENTICATION EMAIL</span>
              <p className="text-sm font-bold text-foreground mt-1">
                {user?.username || '—'}
              </p>
            </div>

            <div className="p-3.5 bg-background-elevated border border-border rounded-sm">
              <span className="text-foreground-muted uppercase tracking-tech">ACCOUNT NUMBER</span>
              <p className="text-sm font-bold text-foreground mt-1">
                {user?.accountNumber || '—'}
              </p>
            </div>

            <div className="p-3.5 bg-background-elevated border border-border rounded-sm">
              <span className="text-foreground-muted uppercase tracking-tech">CLEARANCE ROLE</span>
              <div className="mt-1">
                <Badge variant={user?.role === 'ROLE_ADMIN' ? 'accent' : 'success'} size="sm">
                  {user?.role || 'ROLE_CUSTOMER'}
                </Badge>
              </div>
            </div>
          </div>
        </Card>

        {/* Display & Interface Preferences */}
        <Card withCrosshairs className="p-6">
          <SectionLabel label="INTERFACE PREFERENCES" index="[02]" />
          <div className="space-y-4 mt-4">
            <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
              <div className="flex items-center gap-3">
                {theme === 'dark' ? (
                  <Moon className="w-5 h-5 text-accent" />
                ) : (
                  <Sun className="w-5 h-5 text-accent" />
                )}
                <div>
                  <p className="text-xs font-heading font-bold text-foreground uppercase">
                    COLOR THEME (CURRENT: {theme.toUpperCase()})
                  </p>
                  <p className="text-[11px] font-mono text-foreground-secondary">
                    Switch between dark and light high-contrast financial palettes
                  </p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  toggleTheme();
                  toast.success(`Theme switched to ${theme === 'dark' ? 'Light' : 'Dark'}`);
                }}
              >
                TOGGLE THEME
              </Button>
            </div>

            <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Globe className="w-5 h-5 text-accent" />
                <div>
                  <p className="text-xs font-heading font-bold text-foreground uppercase">
                    DEFAULT CURRENCY
                  </p>
                  <p className="text-[11px] font-mono text-foreground-secondary">
                    Indian Rupee (₹ INR) with standard Lakhs and Crores formatting
                  </p>
                </div>
              </div>
              <Badge variant="neutral" size="sm">
                INR (₹) ENFORCED
              </Badge>
            </div>
          </div>
        </Card>

        {/* Security & Session */}
        <Card withCrosshairs className="p-6">
          <SectionLabel label="SESSION & SECURITY" index="[03]" />
          <div className="space-y-4 mt-4">
            <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Key className="w-5 h-5 text-status-success" />
                <div>
                  <p className="text-xs font-heading font-bold text-foreground uppercase">
                    STATELess JWT SESSION
                  </p>
                  <p className="text-[11px] font-mono text-foreground-secondary">
                    Cryptographically validated via HMAC-SHA256 signature
                  </p>
                </div>
              </div>
              <Badge variant="success" size="sm">ACTIVE</Badge>
            </div>

            <div className="pt-2 flex justify-end">
              <Button
                variant="danger"
                size="md"
                onClick={() => {
                  logout();
                  toast.info('Logged out successfully');
                }}
                leftIcon={<LogOut className="w-4 h-4" />}
              >
                TERMINATE CURRENT SESSION (LOGOUT)
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};
