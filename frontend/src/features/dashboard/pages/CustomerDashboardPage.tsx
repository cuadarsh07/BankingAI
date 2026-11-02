import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { accountService } from '@/services/accounts/accountService';
import { transactionService } from '@/services/transactions/transactionService';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { SectionLabel } from '@/components/ui/SectionLabel';
import { FinancialCard } from '../components/FinancialCard';
import { BalanceChart } from '../components/BalanceChart';
import { DepositWithdrawModal } from '../components/DepositWithdrawModal';
import { formatCurrency, formatDate } from '@/lib/utils';
import { TransactionRecord } from '@/types';
import {
  ArrowDownLeft,
  ArrowUpRight,
  Send,
  Eye,
  EyeOff,
  History,
  ShieldCheck,
  RefreshCw,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

export const CustomerDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [isBalanceHidden, setIsBalanceHidden] = useState(false);
  const [modalType, setModalType] = useState<'DEPOSIT' | 'WITHDRAW' | null>(null);

  // Live data
  const [balance, setBalance] = useState<number>(0);
  const [accountStatus, setAccountStatus] = useState<string>('ACTIVE');
  const [recentTransactions, setRecentTransactions] = useState<TransactionRecord[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const loadDashboardData = useCallback(async () => {
    if (!user?.accountId) {
      setIsLoading(false);
      return;
    }
    try {
      setIsLoading(true);
      const [acc, txRes] = await Promise.all([
        accountService.getAccountById(user.accountId),
        transactionService.getAccountTransactions(user.accountId, {
          page: 0,
          size: 6,
          sortBy: 'createdAt',
          direction: 'DESC',
        }),
      ]);

      if (acc) {
        setBalance(Number(acc.balance) || 0);
        setAccountStatus(acc.status || 'ACTIVE');
      }
      if (txRes?.content) {
        setRecentTransactions(txRes.content);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error connecting to banking server';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  }, [user?.accountId]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  // Compute calculated sums from recent transactions for overview cards
  const totalInflows = recentTransactions
    .filter((tx) => tx.type === 'DEPOSIT' || tx.type === 'TRANSFER_IN')
    .reduce((sum, tx) => sum + (Number(tx.amount) || 0), 0);

  const totalOutflows = recentTransactions
    .filter((tx) => tx.type === 'WITHDRAWAL' || tx.type === 'TRANSFER_OUT')
    .reduce((sum, tx) => sum + (Number(tx.amount) || 0), 0);

  return (
    <div className="space-y-8">
      {/* Editorial Page Header */}
      <PageHeader
        index="[01]"
        tag="CUSTOMER DASHBOARD"
        title="ACCOUNT OVERVIEW"
        subtitle={`WELCOME BACK, ${(user?.fullName || 'CUSTOMER').toUpperCase()} • ACTIVE ACCOUNT`}
        statusBadge={
          <Badge
            variant={accountStatus === 'ACTIVE' ? 'success' : 'warning'}
            size="sm"
            className="flex items-center gap-1"
          >
            <ShieldCheck className="w-3 h-3" />
            <span>ACCOUNT {accountStatus}</span>
          </Badge>
        }
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={loadDashboardData}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              REFRESH
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/transfers')}
              leftIcon={<Send className="w-3.5 h-3.5 text-accent" />}
            >
              SEND MONEY
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => setModalType('DEPOSIT')}
              leftIcon={<ArrowDownLeft className="w-3.5 h-3.5" />}
            >
              ADD FUNDS
            </Button>
          </div>
        }
      />

      {/* Top Level Financial Summary */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Main Balance Card */}
        <Card
          withCrosshairs
          variant="elevated"
          className="p-6 md:col-span-2 flex flex-col justify-between"
        >
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-border">
              <span className="text-[11px] font-mono text-foreground-secondary uppercase tracking-tech">
                TOTAL AVAILABLE LIQUIDITY
              </span>
              <button
                type="button"
                onClick={() => setIsBalanceHidden(!isBalanceHidden)}
                className="text-foreground-muted hover:text-foreground transition-colors p-1"
                title={isBalanceHidden ? 'Reveal Balance' : 'Hide Balance'}
              >
                {isBalanceHidden ? (
                  <EyeOff className="w-4 h-4" />
                ) : (
                  <Eye className="w-4 h-4 text-accent" />
                )}
              </button>
            </div>

            <div className="my-6">
              <span className="text-3xl sm:text-4xl lg:text-5xl font-heading font-extrabold tracking-tight text-foreground">
                {isBalanceHidden ? '••••••••' : formatCurrency(balance)}
              </span>
              <p className="text-xs font-mono text-foreground-secondary mt-2">
                ACCOUNT: {user?.accountNumber || 'ACC-••••'} • CLEARING: INSTANT (INR)
              </p>
            </div>
          </div>

          <div className="pt-4 border-t border-border flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Button
                variant="primary"
                size="sm"
                onClick={() => setModalType('DEPOSIT')}
                leftIcon={<ArrowDownLeft className="w-3.5 h-3.5" />}
              >
                DEPOSIT
              </Button>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setModalType('WITHDRAW')}
                leftIcon={<ArrowUpRight className="w-3.5 h-3.5" />}
              >
                WITHDRAW
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => navigate('/transfers')}
                leftIcon={<Send className="w-3.5 h-3.5" />}
              >
                TRANSFER
              </Button>
            </div>

            <span className="text-[10px] font-mono text-foreground-muted uppercase tracking-tech">
              256-BIT ENCRYPTED SESSION
            </span>
          </div>
        </Card>

        {/* ATM / Debit Card Visual Component */}
        <FinancialCard
          cardHolder={user?.fullName || 'ACTIVE HOLDER'}
          accountNumber={user?.accountNumber || 'ACC-0000-0000'}
        />
      </div>

      {/* Middle Row: Trend Visual & Cash Flow Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8">
          <BalanceChart />
        </div>

        <div className="lg:col-span-4 flex flex-col gap-4">
          <Card className="p-5 flex-1 flex flex-col justify-between">
            <div>
              <span className="text-xs font-mono text-foreground-secondary tracking-tech uppercase">
                // RECENT CREDITS (INFLOWS)
              </span>
              <p className="text-2xl font-heading font-extrabold text-status-success mt-2">
                +{formatCurrency(totalInflows)}
              </p>
            </div>
            <p className="text-[11px] font-mono text-foreground-muted mt-2">
              DEPOSITS & TRANSFERS RECEIVED
            </p>
          </Card>

          <Card className="p-5 flex-1 flex flex-col justify-between">
            <div>
              <span className="text-xs font-mono text-foreground-secondary tracking-tech uppercase">
                // RECENT DEBITS (OUTFLOWS)
              </span>
              <p className="text-2xl font-heading font-extrabold text-foreground mt-2">
                -{formatCurrency(totalOutflows)}
              </p>
            </div>
            <p className="text-[11px] font-mono text-foreground-muted mt-2">
              WITHDRAWALS & TRANSFERS SENT
            </p>
          </Card>

          <Card className="p-5 flex-1 flex flex-col justify-between bg-accent/5 border-accent/30">
            <div>
              <span className="text-xs font-mono text-accent tracking-tech uppercase">
                // NET CASH FLOW
              </span>
              <p className="text-2xl font-heading font-extrabold text-foreground mt-2">
                {formatCurrency(totalInflows - totalOutflows)}
              </p>
            </div>
            <p className="text-[11px] font-mono text-foreground-muted mt-2">
              SURPLUS / DEFICIT
            </p>
          </Card>
        </div>
      </div>

      {/* Bottom Section: Recent Activity Table */}
      <Card withCrosshairs className="p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
          <div className="flex items-center gap-2">
            <History className="w-4 h-4 text-accent" />
            <SectionLabel
              label="RECENT TRANSACTIONS"
              index="[02]"
              className="border-none py-0 mb-0"
            />
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate('/transactions')}
            className="text-xs font-mono"
          >
            VIEW FULL STATEMENT &gt;
          </Button>
        </div>

        {/* Transaction Table */}
        <div className="overflow-x-auto">
          {recentTransactions.length === 0 && !isLoading ? (
            <div className="py-8 text-center text-xs font-mono text-foreground-secondary">
              No transactions recorded yet. Use "Deposit" to seed initial funds.
            </div>
          ) : (
            <table className="w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-border bg-background/50 text-xs font-mono text-foreground-secondary uppercase tracking-tech">
                  <th className="py-3 px-4">REF CODE</th>
                  <th className="py-3 px-4">DATE / TIME</th>
                  <th className="py-3 px-4">TYPE</th>
                  <th className="py-3 px-4">DESCRIPTION</th>
                  <th className="py-3 px-4 text-right">AMOUNT</th>
                  <th className="py-3 px-4 text-right">BALANCE AFTER</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {recentTransactions.map((tx) => {
                  const isPositive = tx.type === 'DEPOSIT' || tx.type === 'TRANSFER_IN';
                  return (
                    <tr key={tx.id} className="hover:bg-background-elevated/70 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-xs text-foreground font-semibold">
                        {tx.referenceNumber}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-xs text-foreground-muted">
                        {formatDate(tx.createdAt)}
                      </td>
                      <td className="py-3.5 px-4">
                        <Badge
                          variant={
                            tx.type === 'DEPOSIT'
                              ? 'success'
                              : tx.type === 'WITHDRAWAL'
                              ? 'warning'
                              : 'accent'
                          }
                          size="sm"
                        >
                          {tx.type}
                        </Badge>
                      </td>
                      <td className="py-3.5 px-4 text-xs font-medium text-foreground">
                        {tx.description}
                      </td>
                      <td
                        className={`py-3.5 px-4 font-mono text-xs text-right font-bold ${
                          isPositive ? 'text-status-success' : 'text-foreground'
                        }`}
                      >
                        {isPositive ? `+${formatCurrency(tx.amount)}` : `-${formatCurrency(tx.amount)}`}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-xs text-right text-foreground-muted">
                        {formatCurrency(tx.balanceAfter)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </Card>

      {/* Modal for Deposit/Withdraw */}
      {modalType && (
        <DepositWithdrawModal
          isOpen={!!modalType}
          onClose={() => setModalType(null)}
          type={modalType}
          accountId={user?.accountId || 1}
          currentBalance={balance}
          onSuccess={() => {
            loadDashboardData();
          }}
        />
      )}
    </div>
  );
};
