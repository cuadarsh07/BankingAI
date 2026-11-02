import React, { useState, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { maskAccountNumber } from '@/lib/utils';
import { ShieldCheck, ShieldAlert, Activity } from 'lucide-react';
import { Link } from 'react-router-dom';

export const SecondaryBar: React.FC = () => {
  const { user, isAdmin } = useAuth();
  const [timeString, setTimeString] = useState<string>('');

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeString(
        now.toLocaleTimeString('en-US', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: false,
        }) + ' UTC'
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="w-full bg-background-secondary/70 border-b border-border text-[11px] font-mono py-1.5 px-4 sm:px-8 flex items-center justify-between text-foreground-secondary tracking-tech select-none">
      <div className="flex items-center gap-4">
        <span className="text-accent flex items-center gap-1.5">
          <Activity className="w-3 h-3 animate-pulse text-accent" />
          <span>+ [01] LEDGER NODE: ACTIVE</span>
        </span>
        <span className="hidden md:inline-block text-border-strong">/</span>
        <span className="hidden md:inline-block">
          ENCRYPTION: AES-256-GCM
        </span>
      </div>

      <div className="flex items-center gap-4">
        {user?.accountNumber && (
          <span className="hidden sm:inline-block text-foreground">
            ACCOUNT: <span className="font-semibold">{maskAccountNumber(user.accountNumber)}</span>
          </span>
        )}
        <span className="text-foreground-muted">{timeString}</span>
        {isAdmin ? (
          <span className="flex items-center gap-1 text-accent font-semibold">
            <ShieldAlert className="w-3 h-3" />
            <Link to="/admin/dashboard" className="hover:underline">ADMIN CONSOLE &gt;</Link>
          </span>
        ) : (
          <span className="flex items-center gap-1 text-status-success font-medium">
            <ShieldCheck className="w-3 h-3" />
            <span>SECURE SESSION</span>
          </span>
        )}
      </div>
    </div>
  );
};

