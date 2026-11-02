import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { accountService } from '@/services/accounts/accountService';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { LoadingIndicator } from '@/components/ui/LoadingIndicator';
import { formatCurrency, formatDate } from '@/lib/utils';
import { DepositWithdrawModal } from '@/features/dashboard/components/DepositWithdrawModal';
import { AccountResponse } from '@/types';
import {
  ShieldCheck,
  ArrowDownLeft,
  ArrowUpRight,
  Settings,
  Copy,
  Check,
  RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';

export const AccountsPage: React.FC = () => {
  const { user } = useAuth();
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [modalType, setModalType] = useState<'DEPOSIT' | 'WITHDRAW' | null>(null);
  const [editProfileOpen, setEditProfileOpen] = useState(false);

  const [account, setAccount] = useState<AccountResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isUpdating, setIsUpdating] = useState<boolean>(false);

  // Form edit fields
  const [editName, setEditName] = useState('');
  const [editEmail, setEditEmail] = useState('');

  const fetchAccount = useCallback(async () => {
    if (!user?.accountId) {
      setIsLoading(false);
      return;
    }
    try {
      setIsLoading(true);
      const data = await accountService.getAccountById(user.accountId);
      setAccount(data);
      setEditName(data.accountHolderName);
      setEditEmail(data.email);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch account info';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  }, [user?.accountId]);

  useEffect(() => {
    fetchAccount();
  }, [fetchAccount]);

  const copyToClipboard = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    toast.success(`${field} copied to clipboard`);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const handleUpdateAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!account?.id) return;

    setIsUpdating(true);
    try {
      const updated = await accountService.patchAccount(account.id, {
        accountHolderName: editName.trim(),
        email: editEmail.trim(),
      });
      setAccount(updated);
      toast.success('Account profile updated successfully');
      setEditProfileOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update account';
      toast.error(msg);
    } finally {
      setIsUpdating(false);
    }
  };

  if (isLoading) {
    return (
      <div className="py-24 flex flex-col items-center justify-center space-y-4">
        <LoadingIndicator size="lg" />
        <p className="text-xs font-mono text-foreground-secondary uppercase tracking-tech">
          LOADING ACCOUNT LEDGER...
        </p>
      </div>
    );
  }

  const currentAcc = account || {
    id: user?.accountId || 0,
    accountNumber: user?.accountNumber || '—',
    accountHolderName: user?.fullName || 'User',
    email: user?.username || '—',
    balance: 0,
    status: 'ACTIVE' as const,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  return (
    <div className="space-y-8">
      <PageHeader
        index="[02]"
        tag="ACCOUNT DETAILS"
        title="BANK ACCOUNTS"
        subtitle="MANAGE ACTIVE SETTLEMENT NODES AND PERSONAL CLEARING PREFERENCES"
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchAccount}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              REFRESH
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setEditProfileOpen(true)}
              leftIcon={<Settings className="w-3.5 h-3.5" />}
            >
              EDIT PROFILE
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

      {/* Account Profile Card */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <Card withCrosshairs variant="elevated" className="lg:col-span-8 p-6 sm:p-8">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-border">
            <div>
              <div className="flex items-center gap-3">
                <h2 className="text-xl sm:text-2xl font-heading font-extrabold text-foreground uppercase">
                  {currentAcc.accountHolderName}
                </h2>
                <Badge
                  variant={currentAcc.status === 'ACTIVE' ? 'success' : 'warning'}
                  size="sm"
                >
                  {currentAcc.status}
                </Badge>
              </div>
              <p className="text-xs font-mono text-foreground-secondary mt-1">
                REGISTERED EMAIL: {currentAcc.email}
              </p>
            </div>
            <div className="text-left sm:text-right">
              <span className="text-[10px] font-mono text-foreground-muted uppercase tracking-tech">
                CURRENT BALANCE
              </span>
              <p className="text-2xl sm:text-3xl font-heading font-extrabold text-foreground tracking-tight">
                {formatCurrency(currentAcc.balance)}
              </p>
            </div>
          </div>

          {/* Account Identifiers Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 py-6 border-b border-border">
            <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
              <div>
                <p className="text-[10px] font-mono uppercase text-foreground-muted tracking-tech">
                  ACCOUNT NUMBER
                </p>
                <p className="text-sm font-mono font-bold text-foreground mt-0.5">
                  {currentAcc.accountNumber}
                </p>
              </div>
              <button
                type="button"
                onClick={() => copyToClipboard(currentAcc.accountNumber, 'Account Number')}
                className="p-1.5 hover:text-accent transition-colors text-foreground-secondary"
                title="Copy Account Number"
              >
                {copiedField === 'Account Number' ? (
                  <Check className="w-4 h-4 text-status-success" />
                ) : (
                  <Copy className="w-4 h-4" />
                )}
              </button>
            </div>

            <div className="p-4 bg-background-elevated border border-border rounded-sm flex items-center justify-between">
              <div>
                <p className="text-[10px] font-mono uppercase text-foreground-muted tracking-tech">
                  ACCOUNT CREATED
                </p>
                <p className="text-sm font-mono font-bold text-foreground mt-0.5">
                  {formatDate(currentAcc.createdAt)}
                </p>
              </div>
              <ShieldCheck className="w-4 h-4 text-status-success" />
            </div>
          </div>

          {/* Quick Transaction Actions */}
          <div className="pt-6 flex flex-wrap gap-3">
            <Button
              variant="primary"
              size="sm"
              onClick={() => setModalType('DEPOSIT')}
              leftIcon={<ArrowDownLeft className="w-4 h-4" />}
            >
              DEPOSIT MONEY
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setModalType('WITHDRAW')}
              leftIcon={<ArrowUpRight className="w-4 h-4" />}
            >
              WITHDRAW MONEY
            </Button>
          </div>
        </Card>

        {/* Security & Regulatory Notes Card */}
        <div className="lg:col-span-4 space-y-6">
          <Card withCrosshairs className="p-6">
            <h3 className="font-heading font-bold text-sm uppercase text-foreground mb-2 flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-accent" />
              <span>SECURITY PROTOCOLS</span>
            </h3>
            <p className="text-xs font-mono text-foreground-secondary leading-relaxed mb-4">
              All transactions are cryptographically signed with JWT bearer tokens. Assets held
              are protected by database transaction isolation (ACID compliant).
            </p>
            <div className="space-y-2 text-[11px] font-mono">
              <div className="flex justify-between py-1 border-b border-border">
                <span className="text-foreground-muted">STATUS</span>
                <span className="text-status-success font-semibold">{currentAcc.status}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-border">
                <span className="text-foreground-muted">CURRENCY</span>
                <span className="text-foreground font-semibold">INR (₹)</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-foreground-muted">SETTLEMENT SPEED</span>
                <span className="text-accent font-semibold">INSTANT RTGS</span>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Edit Profile Modal */}
      <Modal
        isOpen={editProfileOpen}
        onClose={() => setEditProfileOpen(false)}
        title="UPDATE ACCOUNT PROFILE"
        description="// Modify Account Holder Metadata"
        maxWidth="md"
      >
        <form onSubmit={handleUpdateAccount} className="space-y-4">
          <Input
            label="Account Holder Name"
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            required
          />
          <Input
            label="Contact Email"
            value={editEmail}
            onChange={(e) => setEditEmail(e.target.value)}
            type="email"
            required
            isMonospace
          />

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-border">
            <Button
              variant="outline"
              size="sm"
              type="button"
              onClick={() => setEditProfileOpen(false)}
              disabled={isUpdating}
            >
              CANCEL
            </Button>
            <Button variant="primary" size="sm" type="submit" isLoading={isUpdating}>
              SAVE CHANGES
            </Button>
          </div>
        </form>
      </Modal>

      {/* Deposit/Withdraw Modal */}
      {modalType && (
        <DepositWithdrawModal
          isOpen={!!modalType}
          onClose={() => setModalType(null)}
          type={modalType}
          accountId={currentAcc.id}
          currentBalance={currentAcc.balance}
          onSuccess={() => {
            fetchAccount();
          }}
        />
      )}
    </div>
  );
};
