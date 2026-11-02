import React, { useState, useEffect, useCallback } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { fraudService } from '@/services/fraud/fraudService';
import {
  AdminFraudDashboardData,
  HeldTransaction,
  RiskLevel,
  HeldTransactionState,
} from '@/types';
import { formatCurrency, formatDate } from '@/lib/utils';
import { toast } from 'sonner';
import {
  ShieldAlert,
  AlertTriangle,
  FileCheck,
  FileX,
  RefreshCw,
  Search,
  History,
  CheckCircle,
} from 'lucide-react';

export const AdminFraudMonitoringPage: React.FC = () => {
  const [data, setData] = useState<AdminFraudDashboardData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  // Review modal state
  const [selectedTxn, setSelectedTxn] = useState<HeldTransaction | null>(null);
  const [reviewAction, setReviewAction] = useState<'APPROVE' | 'REJECT'>('APPROVE');
  const [reviewNotes, setReviewNotes] = useState('');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);

  const loadDashboard = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await fraudService.getAdminMonitoring();
      setData(res);
    } catch {
      toast.error('Failed to load fraud monitoring telemetry');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const handleOpenReview = (txn: HeldTransaction, action: 'APPROVE' | 'REJECT') => {
    setSelectedTxn(txn);
    setReviewAction(action);
    setReviewNotes('');
  };

  const handleSubmitReview = async () => {
    if (!selectedTxn) return;
    try {
      setIsSubmittingReview(true);
      await fraudService.reviewTransaction(selectedTxn.id, {
        action: reviewAction,
        notes: reviewNotes.trim() || `Admin manual ${reviewAction.toLowerCase()} action.`,
      });
      toast.success(
        `Transaction ${selectedTxn.referenceNumber} ${
          reviewAction === 'APPROVE' ? 'APPROVED & EXECUTED' : 'REJECTED'
        }`
      );
      setSelectedTxn(null);
      loadDashboard();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Review action failed';
      toast.error(msg);
    } finally {
      setIsSubmittingReview(false);
    }
  };

  const getRiskBadge = (level: RiskLevel) => {
    switch (level) {
      case 'CRITICAL':
        return <Badge variant="error">CRITICAL</Badge>;
      case 'HIGH':
        return <Badge variant="warning">HIGH</Badge>;
      case 'MEDIUM':
        return <Badge variant="accent">MEDIUM</Badge>;
      default:
        return <Badge variant="neutral">LOW</Badge>;
    }
  };

  const getStateBadge = (state: HeldTransactionState) => {
    switch (state) {
      case 'SUCCESS':
        return <Badge variant="success">SUCCESS</Badge>;
      case 'REJECTED':
        return <Badge variant="error">REJECTED</Badge>;
      case 'CANCELLED':
        return <Badge variant="neutral">CANCELLED</Badge>;
      case 'PENDING_BANK_REVIEW':
        return <Badge variant="error">PENDING REVIEW</Badge>;
      case 'PENDING_CUSTOMER_VERIFICATION':
        return <Badge variant="warning">AWAITING CUSTOMER</Badge>;
      default:
        return <Badge variant="accent">{state}</Badge>;
    }
  };

  const filteredHeld = data?.heldTransactions.filter((h) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      h.referenceNumber.toLowerCase().includes(q) ||
      h.sourceAccountNumber.toLowerCase().includes(q) ||
      h.destinationAccountNumber.toLowerCase().includes(q) ||
      (h.scamCategory && h.scamCategory.toLowerCase().includes(q))
    );
  }) || [];

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <PageHeader
          index="[ADM-03]"
          tag="SECURITY OPERATIONS"
          title="AI FRAUD MONITORING"
          subtitle="CENTRAL INCIDENT COMMAND FOR DIGITAL ARREST SCAMS, RISK AUDITS & TRANSACTION CLEARING"
        />
        <Button
          variant="outline"
          size="sm"
          onClick={loadDashboard}
          isLoading={isLoading}
          leftIcon={<RefreshCw className="w-4 h-4" />}
        >
          REFRESH TELEMETRY
        </Button>
      </div>

      {/* Metrics Bar */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <Card withCrosshairs className="p-4 bg-background-elevated">
          <span className="text-[10px] font-mono text-foreground-muted block uppercase">
            FLAGGED TXNS
          </span>
          <p className="text-2xl font-mono font-bold text-foreground mt-1">
            {data?.totalFlaggedTransactions ?? 0}
          </p>
        </Card>

        <Card withCrosshairs className="p-4 bg-background-elevated border-status-danger/40">
          <span className="text-[10px] font-mono text-status-danger block uppercase font-bold">
            PENDING REVIEW
          </span>
          <p className="text-2xl font-mono font-bold text-status-danger mt-1">
            {data?.pendingReviewCount ?? 0}
          </p>
        </Card>

        <Card withCrosshairs className="p-4 bg-background-elevated">
          <span className="text-[10px] font-mono text-status-danger block uppercase">
            CRITICAL RISK
          </span>
          <p className="text-2xl font-mono font-bold text-status-danger mt-1">
            {data?.criticalRiskCount ?? 0}
          </p>
        </Card>

        <Card withCrosshairs className="p-4 bg-background-elevated">
          <span className="text-[10px] font-mono text-status-warning block uppercase">
            HIGH RISK
          </span>
          <p className="text-2xl font-mono font-bold text-status-warning mt-1">
            {data?.highRiskCount ?? 0}
          </p>
        </Card>

        <Card withCrosshairs className="p-4 bg-background-elevated">
          <span className="text-[10px] font-mono text-accent block uppercase">
            MEDIUM RISK
          </span>
          <p className="text-2xl font-mono font-bold text-accent mt-1">
            {data?.mediumRiskCount ?? 0}
          </p>
        </Card>

        <Card withCrosshairs className="p-4 bg-background-elevated border-status-danger/30">
          <span className="text-[10px] font-mono text-foreground-muted block uppercase">
            SCAMS DETECTED
          </span>
          <p className="text-2xl font-mono font-bold text-foreground mt-1">
            {data?.scamDetectedCount ?? 0}
          </p>
        </Card>
      </div>

      {/* Held Transactions Review Queue */}
      <Card withCrosshairs variant="elevated">
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <CardTitle>
              <ShieldAlert className="w-4 h-4 text-accent" />
              <span>TRANSACTION HOLD & REVIEW QUEUE</span>
            </CardTitle>
            <CardDescription>// Transactions requiring administrative oversight or security clearing</CardDescription>
          </div>

          <div className="w-full sm:w-64">
            <Input
              placeholder="Search reference or account..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
              isMonospace
            />
          </div>
        </CardHeader>
        <CardContent>
          {filteredHeld.length === 0 ? (
            <div className="p-8 text-center text-xs font-mono text-foreground-muted border border-dashed border-border rounded-sm">
              <CheckCircle className="w-8 h-8 text-status-success mx-auto mb-2 opacity-80" />
              No matching held transactions in the queue.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs border-collapse">
                <thead>
                  <tr className="border-b border-border text-[10px] text-foreground-muted uppercase tracking-tech">
                    <th className="py-2.5 px-3">REFERENCE</th>
                    <th className="py-2.5 px-3">SOURCE</th>
                    <th className="py-2.5 px-3">DESTINATION</th>
                    <th className="py-2.5 px-3">AMOUNT</th>
                    <th className="py-2.5 px-3">RISK</th>
                    <th className="py-2.5 px-3">STATE</th>
                    <th className="py-2.5 px-3">SCAM CATEGORY</th>
                    <th className="py-2.5 px-3 text-right">ACTION</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {filteredHeld.map((txn) => {
                    const isPending =
                      txn.state === 'PENDING_BANK_REVIEW' ||
                      txn.state === 'PENDING_CUSTOMER_VERIFICATION';

                    return (
                      <tr key={txn.id} className="hover:bg-background-elevated/60 transition-colors">
                        <td className="py-3 px-3">
                          <span className="font-bold text-foreground block">
                            {txn.referenceNumber}
                          </span>
                          <span className="text-[10px] text-foreground-muted">
                            {formatDate(txn.createdAt)}
                          </span>
                        </td>
                        <td className="py-3 px-3">
                          <span className="text-foreground">{txn.sourceAccountHolder || 'Customer'}</span>
                          <span className="block text-[10px] text-foreground-muted">
                            {txn.sourceAccountNumber}
                          </span>
                        </td>
                        <td className="py-3 px-3">
                          <span className="text-foreground">{txn.destinationAccountHolder || 'Recipient'}</span>
                          <span className="block text-[10px] text-foreground-muted">
                            {txn.destinationAccountNumber}
                          </span>
                        </td>
                        <td className="py-3 px-3 font-bold text-accent">
                          {formatCurrency(txn.amount)}
                        </td>
                        <td className="py-3 px-3">{getRiskBadge(txn.riskLevel)}</td>
                        <td className="py-3 px-3">{getStateBadge(txn.state)}</td>
                        <td className="py-3 px-3">
                          <span className="text-[11px] text-foreground-secondary">
                            {txn.scamCategory || 'ANOMALY'}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-right">
                          {isPending ? (
                            <div className="flex items-center justify-end gap-1.5">
                              <Button
                                variant="primary"
                                size="sm"
                                onClick={() => handleOpenReview(txn, 'APPROVE')}
                                leftIcon={<FileCheck className="w-3.5 h-3.5" />}
                              >
                                APPROVE
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleOpenReview(txn, 'REJECT')}
                                leftIcon={<FileX className="w-3.5 h-3.5" />}
                              >
                                REJECT
                              </Button>
                            </div>
                          ) : (
                            <span className="text-[10px] text-foreground-muted">PROCESSED</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Grid: Live Fraud Alerts & Immutable Audit Log */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Recent Alerts */}
        <div className="lg:col-span-5 space-y-4">
          <Card withCrosshairs>
            <CardHeader>
              <CardTitle>
                <AlertTriangle className="w-4 h-4 text-status-warning" />
                <span>INCIDENT FEED</span>
              </CardTitle>
              <CardDescription>// Live scam detections and threshold alerts</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3 max-h-[420px] overflow-y-auto">
                {data?.recentAlerts.slice(0, 10).map((alert) => (
                  <div
                    key={alert.id}
                    className="p-3 bg-background-elevated border border-border rounded-sm text-xs font-mono space-y-1"
                  >
                    <div className="flex justify-between items-center">
                      <span className="font-semibold text-foreground">{alert.title}</span>
                      <span className="text-[10px] text-foreground-muted">
                        {formatDate(alert.createdAt)}
                      </span>
                    </div>
                    <p className="text-[11px] text-foreground-secondary leading-relaxed">
                      {alert.message}
                    </p>
                    <span className="text-[10px] text-accent block">
                      Ref: {alert.transactionReference || 'N/A'}
                    </span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Audit Log */}
        <div className="lg:col-span-7 space-y-4">
          <Card withCrosshairs>
            <CardHeader>
              <CardTitle>
                <History className="w-4 h-4 text-accent" />
                <span>IMMUTABLE FRAUD AUDIT TRAIL</span>
              </CardTitle>
              <CardDescription>// Complete evidentiary log for financial compliance & security events</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2 max-h-[420px] overflow-y-auto font-mono text-[11px]">
                {data?.recentAuditLogs.slice(0, 15).map((log) => (
                  <div
                    key={log.id}
                    className="p-2.5 bg-background-elevated border border-border/80 rounded-sm flex items-start justify-between gap-3"
                  >
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-foreground">[{log.eventType}]</span>
                        <span className="text-foreground-secondary">{log.action}</span>
                      </div>
                      <p className="text-[10px] text-foreground-muted">{log.details}</p>
                      <span className="text-[9px] text-accent">
                        By: {log.performedBy} | Entity: {log.entityType} ({log.entityId})
                      </span>
                    </div>
                    <span className="text-[9px] text-foreground-muted shrink-0">
                      {formatDate(log.timestamp)}
                    </span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Review Modal */}
      {selectedTxn && (
        <Modal
          isOpen={!!selectedTxn}
          onClose={() => setSelectedTxn(null)}
          title={`BANK OFFICER REVIEW: ${reviewAction} TRANSACTION`}
          maxWidth="md"
        >
          <div className="space-y-4 font-mono text-xs">
            <div className="p-3 bg-background-elevated border border-border rounded-sm space-y-1.5">
              <div className="flex justify-between">
                <span className="text-foreground-muted">REFERENCE:</span>
                <span className="font-bold text-foreground">{selectedTxn.referenceNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-foreground-muted">AMOUNT:</span>
                <span className="font-bold text-accent">{formatCurrency(selectedTxn.amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-foreground-muted">SOURCE ACCOUNT:</span>
                <span className="text-foreground">
                  {selectedTxn.sourceAccountHolder} ({selectedTxn.sourceAccountNumber})
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-foreground-muted">DESTINATION:</span>
                <span className="text-foreground">
                  {selectedTxn.destinationAccountHolder} ({selectedTxn.destinationAccountNumber})
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-foreground-muted">RISK LEVEL:</span>
                <span>{getRiskBadge(selectedTxn.riskLevel)}</span>
              </div>
            </div>

            {selectedTxn.aiExplanation && (
              <div className="p-3 bg-status-danger/10 border border-status-danger/30 rounded-sm space-y-1">
                <strong className="text-status-danger block text-[11px]">
                  AI FRAUD & SCAM ASSESSMENT:
                </strong>
                <p className="text-[11px] text-foreground leading-relaxed">
                  {selectedTxn.aiExplanation}
                </p>
              </div>
            )}

            <div className="space-y-1">
              <label className="text-[10px] uppercase text-foreground-muted block">
                Administrative Decision Notes / Reason
              </label>
              <textarea
                rows={3}
                value={reviewNotes}
                onChange={(e) => setReviewNotes(e.target.value)}
                placeholder="Enter mandatory audit justification for approving or rejecting this high-risk transfer..."
                className="w-full p-2.5 bg-background border border-border rounded-sm text-foreground focus:outline-none focus:border-accent text-xs font-mono"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-border">
              <Button variant="outline" size="sm" onClick={() => setSelectedTxn(null)}>
                CANCEL
              </Button>
              <Button
                variant={reviewAction === 'APPROVE' ? 'primary' : 'danger'}
                size="sm"
                onClick={handleSubmitReview}
                isLoading={isSubmittingReview}
              >
                CONFIRM {reviewAction}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
