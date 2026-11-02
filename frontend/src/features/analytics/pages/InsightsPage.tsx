import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { transactionService } from '@/services/transactions/transactionService';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { SectionLabel } from '@/components/ui/SectionLabel';
import { LoadingIndicator } from '@/components/ui/LoadingIndicator';
import { formatCurrency } from '@/lib/utils';
import { TransactionRecord } from '@/types';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import {
  TrendingUp,
  ArrowDownLeft,
  ArrowUpRight,
  PieChart as PieIcon,
  BarChart3,
  RefreshCw,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';

export const InsightsPage: React.FC = () => {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState<TransactionRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadData = useCallback(async () => {
    if (!user?.accountId) {
      setIsLoading(false);
      return;
    }
    try {
      setIsLoading(true);
      const res = await transactionService.getAccountTransactions(user.accountId, {
        page: 0,
        size: 100, // Fetch up to 100 for analytics aggregation
        sortBy: 'createdAt',
        direction: 'DESC',
      });
      setTransactions(res.content || []);
    } catch {
      setTransactions([]);
    } finally {
      setIsLoading(false);
    }
  }, [user?.accountId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Aggregate stats
  const totalInflow = transactions
    .filter((t) => t.type === 'DEPOSIT' || t.type === 'TRANSFER_IN')
    .reduce((sum, t) => sum + (Number(t.amount) || 0), 0);

  const totalOutflow = transactions
    .filter((t) => t.type === 'WITHDRAWAL' || t.type === 'TRANSFER_OUT')
    .reduce((sum, t) => sum + (Number(t.amount) || 0), 0);

  // Transaction type distribution for Pie Chart
  const typeCounts: Record<string, number> = {};
  transactions.forEach((t) => {
    typeCounts[t.type] = (typeCounts[t.type] || 0) + (Number(t.amount) || 0);
  });

  const pieData = [
    { name: 'Deposits', value: typeCounts['DEPOSIT'] || 0, color: '#10B981' },
    { name: 'Received Transfers', value: typeCounts['TRANSFER_IN'] || 0, color: '#06B6D4' },
    { name: 'Withdrawals', value: typeCounts['WITHDRAWAL'] || 0, color: '#F59E0B' },
    { name: 'Sent Transfers', value: typeCounts['TRANSFER_OUT'] || 0, color: '#FF5533' },
  ].filter((item) => item.value > 0);

  // Group by last 5 transactions for a bar chart comparison
  const recentBarData = transactions.slice(0, 8).map((t) => ({
    name: t.referenceNumber.slice(-6),
    amount: Number(t.amount),
    type: t.type,
  })).reverse();

  return (
    <div className="space-y-8">
      <PageHeader
        index="[05]"
        tag="SPENDING ANALYTICS"
        title="FINANCIAL INSIGHTS"
        subtitle="COMPREHENSIVE CASH FLOW ANALYSIS CALCULATED DIRECTLY FROM YOUR LEDGER"
        statusBadge={
          <Badge variant="accent" size="sm">
            LIVE ANALYTICS
          </Badge>
        }
        actions={
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            REFRESH
          </Button>
        }
      />

      {isLoading ? (
        <div className="py-20 flex flex-col items-center justify-center space-y-3">
          <LoadingIndicator size="md" />
          <p className="text-xs font-mono text-foreground-secondary uppercase tracking-tech">
            ANALYZING TRANSACTION FLOWS...
          </p>
        </div>
      ) : (
        <>
          {/* Top KPI row */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Card withCrosshairs className="p-5">
              <span className="text-[11px] font-mono text-foreground-secondary uppercase tracking-tech flex items-center justify-between">
                <span>TOTAL CREDITS RECEIVED</span>
                <ArrowDownLeft className="w-4 h-4 text-status-success" />
              </span>
              <p className="text-2xl font-heading font-extrabold text-status-success mt-2">
                +{formatCurrency(totalInflow)}
              </p>
              <p className="text-[10px] font-mono text-foreground-muted mt-1">
                ACROSS ALL RECORDED TRANSACTIONS
              </p>
            </Card>

            <Card withCrosshairs className="p-5">
              <span className="text-[11px] font-mono text-foreground-secondary uppercase tracking-tech flex items-center justify-between">
                <span>TOTAL DEBITS SPENT</span>
                <ArrowUpRight className="w-4 h-4 text-status-error" />
              </span>
              <p className="text-2xl font-heading font-extrabold text-foreground mt-2">
                -{formatCurrency(totalOutflow)}
              </p>
              <p className="text-[10px] font-mono text-foreground-muted mt-1">
                WITHDRAWALS AND OUTBOUND TRANSFERS
              </p>
            </Card>

            <Card withCrosshairs className="p-5 bg-accent/5 border-accent/30">
              <span className="text-[11px] font-mono text-accent uppercase tracking-tech flex items-center justify-between">
                <span>NET RETENTION</span>
                <TrendingUp className="w-4 h-4 text-accent" />
              </span>
              <p className="text-2xl font-heading font-extrabold text-foreground mt-2">
                {formatCurrency(totalInflow - totalOutflow)}
              </p>
              <p className="text-[10px] font-mono text-foreground-muted mt-1">
                SAVINGS ACCUMULATION RATIO
              </p>
            </Card>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Recent Volume Bar Chart */}
            <Card withCrosshairs className="lg:col-span-7 p-6">
              <div className="flex items-center gap-2 mb-4">
                <BarChart3 className="w-4 h-4 text-accent" />
                <SectionLabel
                  label="RECENT ACTIVITY AMOUNTS"
                  index="[01]"
                  className="border-none py-0 mb-0"
                />
              </div>
              {recentBarData.length === 0 ? (
                <div className="h-64 flex items-center justify-center text-xs font-mono text-foreground-secondary">
                  No activity available yet.
                </div>
              ) : (
                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={recentBarData}>
                      <XAxis dataKey="name" stroke="#666" fontSize={11} />
                      <YAxis stroke="#666" fontSize={11} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#111',
                          borderColor: '#333',
                          fontSize: '12px',
                        }}
                        formatter={(val: number) => [formatCurrency(val), 'Amount']}
                      />
                      <Bar dataKey="amount" fill="#FF5533" radius={[2, 2, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>

            {/* Inflow vs Outflow Distribution */}
            <Card withCrosshairs className="lg:col-span-5 p-6">
              <div className="flex items-center gap-2 mb-4">
                <PieIcon className="w-4 h-4 text-accent" />
                <SectionLabel
                  label="VOLUME BREAKDOWN BY TYPE"
                  index="[02]"
                  className="border-none py-0 mb-0"
                />
              </div>
              {pieData.length === 0 ? (
                <div className="h-64 flex items-center justify-center text-xs font-mono text-foreground-secondary">
                  No breakdown data available.
                </div>
              ) : (
                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={pieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        outerRadius={75}
                        innerRadius={45}
                        paddingAngle={4}
                      >
                        {pieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#111',
                          borderColor: '#333',
                          fontSize: '12px',
                        }}
                        formatter={(val: number) => [formatCurrency(val), 'Volume']}
                      />
                      <Legend
                        wrapperStyle={{ fontSize: '11px', fontFamily: 'monospace' }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>
          </div>
        </>
      )}
    </div>
  );
};

