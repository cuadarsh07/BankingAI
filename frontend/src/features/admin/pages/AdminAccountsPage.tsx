import React, { useState, useEffect, useCallback } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { DataTable, Column } from '@/components/ui/DataTable';
import { Tabs, TabItem } from '@/components/ui/Tabs';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { LoadingIndicator } from '@/components/ui/LoadingIndicator';
import { formatCurrency, formatDate } from '@/lib/utils';
import { accountService } from '@/services/accounts/accountService';
import { AccountResponse, AccountStatus } from '@/types';
import { Search, Eye, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';

export const AdminAccountsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedAccount, setSelectedAccount] = useState<AccountResponse | null>(null);

  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState<boolean>(false);

  const fetchAccounts = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await accountService.getAllAccounts();
      setAccounts(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch accounts';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: 'ALL ACCOUNTS', count: accounts.length },
    { id: 'ACTIVE', label: 'ACTIVE', count: accounts.filter((a) => a.status === 'ACTIVE').length },
    { id: 'SUSPENDED', label: 'SUSPENDED', count: accounts.filter((a) => a.status === 'SUSPENDED').length },
    { id: 'CLOSED', label: 'CLOSED', count: accounts.filter((a) => a.status === 'CLOSED').length },
  ];

  const filteredAccounts = accounts.filter((acc) => {
    const matchesTab = activeTab === 'ALL' || acc.status === activeTab;
    const matchesSearch =
      searchQuery === '' ||
      acc.accountHolderName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      acc.accountNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      acc.email.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  const handleToggleStatus = async (accId: number, newStatus: AccountStatus) => {
    setIsUpdatingStatus(true);
    try {
      const updated = await accountService.patchAccount(accId, { status: newStatus });
      setAccounts((prev) => prev.map((a) => (a.id === accId ? updated : a)));
      if (selectedAccount && selectedAccount.id === accId) {
        setSelectedAccount(updated);
      }
      toast.success(`Account ID #${accId} status changed to ${newStatus}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update account status';
      toast.error(msg);
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const columns: Column<AccountResponse>[] = [
    {
      key: 'id',
      header: 'ID',
      isMono: true,
      className: 'w-16 font-bold text-accent',
      render: (r) => `#${r.id}`,
    },
    {
      key: 'accountNumber',
      header: 'ACCOUNT NUMBER',
      isMono: true,
      className: 'font-semibold text-foreground',
    },
    {
      key: 'accountHolderName',
      header: 'CUSTOMER NAME',
      render: (r) => (
        <div>
          <p className="text-xs font-heading font-semibold text-foreground">
            {r.accountHolderName}
          </p>
          <p className="text-[10px] font-mono text-foreground-muted">{r.email}</p>
        </div>
      ),
    },
    {
      key: 'balance',
      header: 'CURRENT BALANCE',
      align: 'right',
      render: (r) => (
        <span className="font-mono font-bold text-xs text-foreground">
          {formatCurrency(r.balance)}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'STATUS',
      align: 'center',
      render: (r) => (
        <Badge
          variant={
            r.status === 'ACTIVE'
              ? 'success'
              : r.status === 'SUSPENDED'
              ? 'warning'
              : 'neutral'
          }
          size="sm"
        >
          {r.status}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: 'ACTION',
      align: 'right',
      render: (r) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setSelectedAccount(r)}
          leftIcon={<Eye className="w-3.5 h-3.5" />}
          className="text-xs font-mono"
        >
          VIEW
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-8">
      <PageHeader
        index="[02]"
        tag="ADMINISTRATIVE REGISTRY"
        title="ALL ACCOUNTS"
        subtitle="MASTER DATABASE OF ALL REGISTERED CUSTOMER BANK ACCOUNTS"
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={fetchAccounts}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            REFRESH
          </Button>
        }
      />

      <Card withCrosshairs className="p-0 overflow-hidden">
        {/* Tabs */}
        <div className="px-4 pt-2 bg-background-secondary/70 border-b border-border">
          <Tabs tabs={filterTabs} activeTab={activeTab} onChange={setActiveTab} />
        </div>

        {/* Search */}
        <div className="p-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-b border-border bg-background-secondary/30">
          <div className="w-full sm:w-80">
            <Input
              placeholder="Search by name, email, or account #..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
              isMonospace
            />
          </div>

          <div className="flex items-center gap-2 text-xs font-mono text-foreground-muted">
            <span>REGISTERED: {filteredAccounts.length} ACCOUNTS</span>
          </div>
        </div>

        {/* Data Table */}
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center space-y-3">
            <LoadingIndicator size="md" />
            <p className="text-xs font-mono text-foreground-secondary uppercase tracking-tech">
              QUERYING ACCOUNTS FROM SERVER...
            </p>
          </div>
        ) : (
          <DataTable
            columns={columns}
            data={filteredAccounts}
            keyExtractor={(r) => r.id}
            emptyMessage="No accounts matching current criteria."
          />
        )}
      </Card>

      {/* Inspect Modal */}
      {selectedAccount && (
        <Modal
          isOpen={!!selectedAccount}
          onClose={() => setSelectedAccount(null)}
          title={`ACCOUNT #${selectedAccount.id}`}
          description={`// ${selectedAccount.accountNumber} • ${selectedAccount.accountHolderName}`}
          maxWidth="lg"
        >
          <div className="space-y-5">
            <div className="grid grid-cols-2 gap-4 text-xs font-mono">
              <div className="p-3 bg-background-elevated border border-border rounded-sm">
                <span className="text-foreground-muted uppercase">CURRENT BALANCE</span>
                <p className="text-xl font-heading font-extrabold text-foreground mt-1">
                  {formatCurrency(selectedAccount.balance)}
                </p>
              </div>
              <div className="p-3 bg-background-elevated border border-border rounded-sm">
                <span className="text-foreground-muted uppercase">STATUS</span>
                <p className="mt-1">
                  <Badge
                    variant={selectedAccount.status === 'ACTIVE' ? 'success' : 'warning'}
                    size="sm"
                  >
                    {selectedAccount.status}
                  </Badge>
                </p>
              </div>
            </div>

            <div className="space-y-2 text-xs font-mono border-t border-border pt-4">
              <div className="flex justify-between py-1 border-b border-border/60">
                <span className="text-foreground-muted">ACCOUNT HOLDER:</span>
                <span className="text-foreground font-semibold">
                  {selectedAccount.accountHolderName}
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-border/60">
                <span className="text-foreground-muted">REGISTERED EMAIL:</span>
                <span className="text-foreground">{selectedAccount.email}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-border/60">
                <span className="text-foreground-muted">CREATED DATE:</span>
                <span className="text-foreground">{formatDate(selectedAccount.createdAt)}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-foreground-muted">LAST UPDATED:</span>
                <span className="text-foreground">{formatDate(selectedAccount.updatedAt)}</span>
              </div>
            </div>

            {/* Admin Override Controls */}
            <div className="pt-4 border-t border-border space-y-2">
              <span className="text-[11px] font-mono uppercase tracking-tech text-foreground-secondary">
                // ACCOUNT STATUS ACTIONS
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleToggleStatus(selectedAccount.id, 'ACTIVE')}
                  disabled={selectedAccount.status === 'ACTIVE' || isUpdatingStatus}
                >
                  ACTIVATE
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => handleToggleStatus(selectedAccount.id, 'SUSPENDED')}
                  disabled={selectedAccount.status === 'SUSPENDED' || isUpdatingStatus}
                >
                  SUSPEND
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => handleToggleStatus(selectedAccount.id, 'CLOSED')}
                  disabled={selectedAccount.status === 'CLOSED' || isUpdatingStatus}
                >
                  CLOSE ACCOUNT
                </Button>
              </div>
            </div>

            <div className="pt-4 border-t border-border flex justify-end">
              <Button variant="outline" size="sm" onClick={() => setSelectedAccount(null)}>
                CLOSE
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
