import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { transactionService } from '@/services/transactions/transactionService';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { DataTable, Column } from '@/components/ui/DataTable';
import { Tabs, TabItem } from '@/components/ui/Tabs';
import { Pagination } from '@/components/ui/Pagination';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { LoadingIndicator } from '@/components/ui/LoadingIndicator';
import { formatCurrency, formatDate } from '@/lib/utils';
import { TransactionRecord, TransactionType } from '@/types';
import { Download, Search, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';

export const TransactionsPage: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  const [transactions, setTransactions] = useState<TransactionRecord[]>([]);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const filterTabs: TabItem[] = [
    { id: 'ALL', label: 'ALL ACTIVITY', number: '01' },
    { id: 'DEPOSIT', label: 'DEPOSITS', number: '02' },
    { id: 'WITHDRAWAL', label: 'WITHDRAWALS', number: '03' },
    { id: 'TRANSFER_IN', label: 'RECEIVED MONEY', number: '04' },
    { id: 'TRANSFER_OUT', label: 'SENT MONEY', number: '05' },
  ];

  const fetchTransactions = useCallback(async () => {
    if (!user?.accountId) {
      setIsLoading(false);
      return;
    }
    try {
      setIsLoading(true);
      const queryType = activeTab === 'ALL' ? undefined : (activeTab as TransactionType);
      const response = await transactionService.getAccountTransactions(user.accountId, {
        page: currentPage,
        size: pageSize,
        type: queryType,
        sortBy: 'createdAt',
        direction: 'DESC',
      });

      setTransactions(response.content || []);
      setTotalPages(response.totalPages || 1);
      setTotalElements(response.totalElements || 0);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load transactions';
      toast.error(msg);
      setTransactions([]);
    } finally {
      setIsLoading(false);
    }
  }, [user?.accountId, currentPage, activeTab]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  // Client-side text filter on the current page's records (for instant search)
  const filteredData = transactions.filter((t) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      t.referenceNumber.toLowerCase().includes(q) ||
      t.description.toLowerCase().includes(q) ||
      (t.counterpartyAccountNumber && t.counterpartyAccountNumber.toLowerCase().includes(q))
    );
  });

  const columns: Column<TransactionRecord>[] = [
    {
      key: 'referenceNumber',
      header: 'REFERENCE ID',
      isMono: true,
      className: 'font-semibold text-foreground w-40',
    },
    {
      key: 'createdAt',
      header: 'DATE / TIME',
      isMono: true,
      className: 'text-foreground-secondary w-44',
      render: (row) => formatDate(row.createdAt),
    },
    {
      key: 'type',
      header: 'TYPE',
      className: 'w-36',
      render: (row) => {
        const variantMap: Record<TransactionType, 'success' | 'warning' | 'accent' | 'neutral'> = {
          DEPOSIT: 'success',
          WITHDRAWAL: 'warning',
          TRANSFER_IN: 'accent',
          TRANSFER_OUT: 'neutral',
        };
        const labelMap: Record<TransactionType, string> = {
          DEPOSIT: 'DEPOSIT',
          WITHDRAWAL: 'WITHDRAWAL',
          TRANSFER_IN: 'RECEIVED',
          TRANSFER_OUT: 'SENT',
        };
        return <Badge variant={variantMap[row.type]} size="sm">{labelMap[row.type] || row.type}</Badge>;
      },
    },
    {
      key: 'description',
      header: 'DESCRIPTION / COUNTERPARTY',
      render: (row) => (
        <div>
          <p className="text-xs font-medium text-foreground">{row.description}</p>
          {row.counterpartyAccountNumber && (
            <p className="text-[10px] font-mono text-foreground-muted">
              COUNTERPARTY: {row.counterpartyAccountNumber}
            </p>
          )}
        </div>
      ),
    },
    {
      key: 'amount',
      header: 'AMOUNT',
      align: 'right',
      render: (row) => {
        const isCredit = row.type === 'DEPOSIT' || row.type === 'TRANSFER_IN';
        return (
          <span
            className={`font-mono font-bold text-xs ${
              isCredit ? 'text-status-success' : 'text-foreground'
            }`}
          >
            {isCredit ? `+${formatCurrency(row.amount)}` : `-${formatCurrency(row.amount)}`}
          </span>
        );
      },
    },
    {
      key: 'balanceAfter',
      header: 'BALANCE AFTER',
      align: 'right',
      render: (row) => (
        <span className="font-mono text-xs text-foreground-muted">
          {formatCurrency(row.balanceAfter)}
        </span>
      ),
    },
  ];

  const handleExportCSV = () => {
    if (filteredData.length === 0) {
      toast.info('No transactions available to export');
      return;
    }
    const csvContent =
      'data:text/csv;charset=utf-8,' +
      ['Reference,Date,Type,Description,Amount,BalanceAfter']
        .concat(
          filteredData.map(
            (t) =>
              `${t.referenceNumber},"${t.createdAt}",${t.type},"${t.description.replace(/"/g, '""')}",${t.amount},${t.balanceAfter}`
          )
        )
        .join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `statement_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    toast.success('Transactions statement exported');
  };

  return (
    <div className="space-y-8">
      <PageHeader
        index="[04]"
        tag="PERMANENT RECORD"
        title="TRANSACTION HISTORY"
        subtitle="VERIFIED DOUBLE-ENTRY PASSBOOK OF ALL DEBITS, CREDITS, AND TRANSFERS"
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchTransactions}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              REFRESH
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleExportCSV}
              leftIcon={<Download className="w-3.5 h-3.5" />}
            >
              EXPORT CSV
            </Button>
          </div>
        }
      />

      <Card withCrosshairs className="p-0 overflow-hidden">
        {/* Filter Tabs */}
        <div className="px-4 pt-2 bg-background-secondary/70 border-b border-border">
          <Tabs
            tabs={filterTabs}
            activeTab={activeTab}
            onChange={(t) => {
              setActiveTab(t);
              setCurrentPage(0);
            }}
          />
        </div>

        {/* Search & Actions Bar */}
        <div className="p-4 flex flex-col sm:flex-row items-center justify-between gap-3 border-b border-border bg-background-secondary/30">
          <div className="w-full sm:w-80">
            <Input
              placeholder="Search narrative or ref #..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
              isMonospace
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end text-xs font-mono text-foreground-muted">
            <span>TOTAL: {totalElements} TRANSACTIONS</span>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setSearchQuery('');
                setActiveTab('ALL');
                setCurrentPage(0);
              }}
            >
              RESET
            </Button>
          </div>
        </div>

        {/* Table Content */}
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center space-y-3">
            <LoadingIndicator size="md" />
            <p className="text-xs font-mono text-foreground-secondary uppercase tracking-tech">
              FETCHING TRANSACTIONS FROM BANK LEDGER...
            </p>
          </div>
        ) : (
          <DataTable
            columns={columns}
            data={filteredData}
            keyExtractor={(r) => r.id}
            emptyMessage="No transactions found matching your criteria."
          />
        )}

        {/* Pagination */}
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={pageSize}
          onPageChange={setCurrentPage}
        />
      </Card>
    </div>
  );
};
