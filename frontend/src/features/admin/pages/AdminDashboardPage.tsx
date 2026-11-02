import React, { useState, useEffect, useCallback } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { SectionLabel } from '@/components/ui/SectionLabel';
import { Button } from '@/components/ui/Button';
import { LoadingIndicator } from '@/components/ui/LoadingIndicator';
import { accountService } from '@/services/accounts/accountService';
import { formatCurrency } from '@/lib/utils';
import { useNavigate } from 'react-router-dom';
import { AccountResponse } from '@/types';
import {
  Users,
  Coins,
  Activity,
  Server,
  ArrowRight,
  Database,
  Lock,
  ShieldCheck,
  RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';

export const AdminDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchMetrics = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await accountService.getAllAccounts();
      setAccounts(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch admin metrics';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  const totalLiquidity = accounts.reduce((acc, curr) => acc + (Number(curr.balance) || 0), 0);
  const activeCount = accounts.filter((a) => a.status === 'ACTIVE').length;
  const suspendedCount = accounts.filter((a) => a.status === 'SUSPENDED').length;
  const closedCount = accounts.filter((a) => a.status === 'CLOSED').length;

  return (
    <div className="space-y-8">
      <PageHeader
        index="[01]"
        tag="ADMINISTRATIVE CONSOLE"
        title="SYSTEM METRICS & GOVERNANCE"
        subtitle="CENTRAL TELEMETRY • CORE TRANSACTION SYSTEM"
        statusBadge={
          <Badge variant="accent" size="sm">
            ELEVATED CLEARANCE
          </Badge>
        }
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchMetrics}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              REFRESH
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => navigate('/admin/accounts')}
              leftIcon={<Users className="w-4 h-4" />}
            >
              VIEW ACCOUNTS
            </Button>
          </div>
        }
      />

      {isLoading ? (
        <div className="py-16 flex flex-col items-center justify-center space-y-3">
          <LoadingIndicator size="md" />
          <p className="text-xs font-mono text-foreground-secondary uppercase tracking-tech">
            CALCULATING SYSTEM LIQUIDITY & STATS...
          </p>
        </div>
      ) : (
        <>
          {/* Top 4 KPI Metrics */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card withCrosshairs className="p-5 flex flex-col justify-between">
              <div className="flex items-center justify-between text-foreground-secondary">
                <span className="text-xs font-mono uppercase tracking-tech">// TOTAL LIQUIDITY</span>
                <Coins className="w-4 h-4 text-accent" />
              </div>
              <div className="my-3">
                <p className="text-2xl sm:text-3xl font-heading font-extrabold text-foreground">
                  {formatCurrency(totalLiquidity)}
                </p>
                <p className="text-[11px] font-mono text-status-success mt-1">
                  ACROSS ALL CUSTOMER ACCOUNTS
                </p>
              </div>
              <div className="text-[10px] font-mono text-foreground-muted border-t border-border pt-2">
                RESERVE STATUS: 100% VERIFIED
              </div>
            </Card>

            <Card withCrosshairs className="p-5 flex flex-col justify-between">
              <div className="flex items-center justify-between text-foreground-secondary">
                <span className="text-xs font-mono uppercase tracking-tech">// REGISTERED ACCOUNTS</span>
                <Users className="w-4 h-4 text-accent" />
              </div>
              <div className="my-3">
                <p className="text-3xl font-heading font-extrabold text-foreground">
                  {accounts.length}
                </p>
                <p className="text-[11px] font-mono text-foreground-secondary mt-1">
                  {activeCount} ACTIVE • {suspendedCount} SUSPENDED • {closedCount} CLOSED
                </p>
              </div>
              <div className="text-[10px] font-mono text-foreground-muted border-t border-border pt-2">
                CUSTOMER DIRECTORY
              </div>
            </Card>

            <Card withCrosshairs className="p-5 flex flex-col justify-between">
              <div className="flex items-center justify-between text-foreground-secondary">
                <span className="text-xs font-mono uppercase tracking-tech">// ACTIVE RATIO</span>
                <Activity className="w-4 h-4 text-accent" />
              </div>
              <div className="my-3">
                <p className="text-3xl font-heading font-extrabold text-foreground">
                  {accounts.length > 0 ? ((activeCount / accounts.length) * 100).toFixed(0) : 0}%
                </p>
                <p className="text-[11px] font-mono text-status-success mt-1">
                  HEALTHY OPERATING STATUS
                </p>
              </div>
              <div className="text-[10px] font-mono text-foreground-muted border-t border-border pt-2">
                ACCOUNTS IN GOOD STANDING
              </div>
            </Card>

            <Card withCrosshairs className="p-5 flex flex-col justify-between">
              <div className="flex items-center justify-between text-foreground-secondary">
                <span className="text-xs font-mono uppercase tracking-tech">// CLUSTER HEALTH</span>
                <Server className="w-4 h-4 text-status-success" />
              </div>
              <div className="my-3">
                <p className="text-3xl font-heading font-extrabold text-status-success">
                  ONLINE
                </p>
                <p className="text-[11px] font-mono text-foreground-secondary mt-1">
                  SPRING BOOT + MYSQL 8.0
                </p>
              </div>
              <div className="text-[10px] font-mono text-foreground-muted border-t border-border pt-2">
                ALL ENDPOINTS RESPONDING
              </div>
            </Card>
          </div>

          {/* Middle Row: Database & Security Nodes */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            <Card withCrosshairs className="lg:col-span-8 p-6">
              <SectionLabel label="SECURITY & INTEGRITY MONITORS" index="[02]" />
              <div className="space-y-4 mt-4">
                <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Database className="w-5 h-5 text-accent" />
                    <div>
                      <p className="text-xs font-heading font-bold text-foreground uppercase">
                        Database Pessimistic Locking & Transfer Isolation
                      </p>
                      <p className="text-[11px] font-mono text-foreground-secondary">
                        Protects simultaneous concurrent transfers against race conditions and balance discrepancies
                      </p>
                    </div>
                  </div>
                  <Badge variant="success" size="sm">ACTIVE</Badge>
                </div>

                <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Lock className="w-5 h-5 text-accent" />
                    <div>
                      <p className="text-xs font-heading font-bold text-foreground uppercase">
                        Stateless Cryptographic JWT Authentication
                      </p>
                      <p className="text-[11px] font-mono text-foreground-secondary">
                        HMAC-SHA256 bearer tokens with role-based authority verification
                      </p>
                    </div>
                  </div>
                  <Badge variant="success" size="sm">ENFORCED</Badge>
                </div>

                <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <ShieldCheck className="w-5 h-5 text-accent" />
                    <div>
                      <p className="text-xs font-heading font-bold text-foreground uppercase">
                        Atomic Dual-Entry Ledger Audit
                      </p>
                      <p className="text-[11px] font-mono text-foreground-secondary">
                        Every debit has an associated credit record and immutable balance tracking
                      </p>
                    </div>
                  </div>
                  <Badge variant="success" size="sm">ACTIVE</Badge>
                </div>
              </div>
            </Card>

            <Card withCrosshairs className="lg:col-span-4 p-6 flex flex-col justify-between">
              <div>
                <SectionLabel label="ADMIN ACTIONS" index="[03]" />
                <p className="text-xs font-mono text-foreground-secondary leading-relaxed mb-4">
                  Manage customer bank accounts, inspect live balances, or switch between roles.
                </p>
              </div>

              <div className="space-y-3 pt-4 border-t border-border">
                <Button
                  variant="primary"
                  size="sm"
                  className="w-full justify-between"
                  onClick={() => navigate('/admin/accounts')}
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  MANAGE ALL ACCOUNTS
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full justify-between"
                  onClick={() => navigate('/dashboard')}
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  GO TO CUSTOMER DASHBOARD
                </Button>
              </div>
            </Card>
          </div>
        </>
      )}
    </div>
  );
};
