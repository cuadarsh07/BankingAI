import React, { useState, useEffect, useCallback } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { fraudService } from '@/services/fraud/fraudService';
import { FraudAlert, HeldTransaction, RiskLevel } from '@/types';
import { formatCurrency, formatDate } from '@/lib/utils';
import { toast } from 'sonner';
import {
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  Lock,
  PhoneCall,
  Clock,
  Eye,
  CheckCircle2,
  XCircle,
  HelpCircle,
  RefreshCw,
} from 'lucide-react';

export const SecurityCenterPage: React.FC = () => {
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [heldTransactions, setHeldTransactions] = useState<HeldTransaction[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Verification modal state
  const [selectedHeld, setSelectedHeld] = useState<HeldTransaction | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [isSubmittingVerify, setIsSubmittingVerify] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true);
      const [fetchedAlerts, fetchedHeld] = await Promise.all([
        fraudService.getAlerts(),
        fraudService.getHeldTransactions(),
      ]);
      setAlerts(fetchedAlerts);
      setHeldTransactions(fetchedHeld);
    } catch {
      toast.error('Failed to load security center records');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleOpenVerifyModal = (held: HeldTransaction) => {
    setSelectedHeld(held);
    // Initialize default answers
    const initAnswers: Record<string, string> = {};
    held.verificationQuestions.forEach((q) => {
      initAnswers[q] = '';
    });
    setAnswers(initAnswers);
  };

  const handleSubmitVerification = async () => {
    if (!selectedHeld) return;

    // Check all questions answered
    const unanswered = Object.values(answers).some((val) => !val);
    if (unanswered) {
      toast.error('Please answer all security questions before proceeding');
      return;
    }

    try {
      setIsSubmittingVerify(true);
      const updated = await fraudService.verifyTransaction(selectedHeld.id, answers);
      if (updated.state === 'SUCCESS' || updated.state === 'APPROVED') {
        toast.success('Security verification passed! Funds transferred successfully.');
      } else if (updated.state === 'PENDING_BANK_REVIEW') {
        toast.warning('Transaction escalated for Bank Administrative Review.');
      } else {
        toast.error('Security checks failed. Transaction remains on hold.');
      }
      setSelectedHeld(null);
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Verification failed';
      toast.error(msg);
    } finally {
      setIsSubmittingVerify(false);
    }
  };

  const handleCancelTransfer = async (id: number) => {
    try {
      setIsCancelling(true);
      await fraudService.cancelTransaction(id);
      toast.success('Held transaction cancelled. No money was deducted.');
      setSelectedHeld(null);
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Cancellation failed';
      toast.error(msg);
    } finally {
      setIsCancelling(false);
    }
  };

  const handleMarkAlertRead = async (id: number) => {
    try {
      await fraudService.markAlertRead(id);
      setAlerts((prev) =>
        prev.map((a) => (a.id === id ? { ...a, isRead: true } : a))
      );
    } catch {
      // Ignore
    }
  };

  const getRiskBadge = (level: RiskLevel) => {
    switch (level) {
      case 'CRITICAL':
        return <Badge variant="error">CRITICAL RISK</Badge>;
      case 'HIGH':
        return <Badge variant="warning">HIGH RISK</Badge>;
      case 'MEDIUM':
        return <Badge variant="accent">MEDIUM RISK</Badge>;
      default:
        return <Badge variant="neutral">LOW RISK</Badge>;
    }
  };

  const hasCriticalAlerts = alerts.some((a) => a.severity === 'CRITICAL' && !a.isRead);
  const pendingHeldCount = heldTransactions.filter(
    (h) => h.state === 'PENDING_CUSTOMER_VERIFICATION' || h.state === 'PENDING_BANK_REVIEW'
  ).length;

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <PageHeader
          index="[08]"
          tag="CYBER DEFENSE & SCAM SHIELD"
          title="SECURITY CENTER"
          subtitle="REAL-TIME FRAUD MONITORING, DIGITAL ARREST DEFENSE & TRANSACTION HOLD RELEASE"
        />
        <Button
          variant="outline"
          size="sm"
          onClick={loadData}
          isLoading={isLoading}
          leftIcon={<RefreshCw className="w-4 h-4" />}
        >
          REFRESH SHIELD
        </Button>
      </div>

      {/* Security Status Banner */}
      <div
        className={`p-5 rounded-sm border flex flex-col md:flex-row items-start md:items-center justify-between gap-4 ${
          hasCriticalAlerts || pendingHeldCount > 0
            ? 'bg-status-danger/10 border-status-danger/40 text-foreground'
            : 'bg-status-success/10 border-status-success/40 text-foreground'
        }`}
      >
        <div className="flex items-center gap-4">
          <div
            className={`w-12 h-12 rounded-sm flex items-center justify-center shrink-0 ${
              hasCriticalAlerts || pendingHeldCount > 0
                ? 'bg-status-danger/20 text-status-danger'
                : 'bg-status-success/20 text-status-success'
            }`}
          >
            {hasCriticalAlerts || pendingHeldCount > 0 ? (
              <ShieldAlert className="w-6 h-6 animate-pulse" />
            ) : (
              <ShieldCheck className="w-6 h-6" />
            )}
          </div>
          <div>
            <h3 className="text-base font-heading font-bold uppercase tracking-tight">
              {hasCriticalAlerts || pendingHeldCount > 0
                ? 'SECURITY ALERT: ATTENTION REQUIRED'
                : 'ACCOUNT SECURE & PROTECTED'}
            </h3>
            <p className="text-xs font-mono text-foreground-secondary mt-0.5">
              {pendingHeldCount > 0
                ? `${pendingHeldCount} outgoing transaction(s) are currently on protective hold awaiting review.`
                : 'AI-powered social engineering and digital arrest heuristic monitoring active.'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Badge
            variant={hasCriticalAlerts || pendingHeldCount > 0 ? 'error' : 'success'}
            size="md"
          >
            {hasCriticalAlerts || pendingHeldCount > 0 ? 'ACTION REQUIRED' : 'NORMAL STATUS'}
          </Badge>
        </div>
      </div>

      {/* Grid: Held Transactions & Digital Arrest Advisory */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Held Transactions Queue */}
        <div className="lg:col-span-7 space-y-6">
          <Card withCrosshairs variant="elevated">
            <CardHeader>
              <CardTitle>
                <Lock className="w-4 h-4 text-accent" />
                <span>TRANSACTIONS ON PROTECTIVE HOLD</span>
              </CardTitle>
              <CardDescription>
                // Transactions paused by AI fraud detection to prevent unauthorized transfers
              </CardDescription>
            </CardHeader>
            <CardContent>
              {heldTransactions.length === 0 ? (
                <div className="p-8 text-center text-xs font-mono text-foreground-muted border border-dashed border-border rounded-sm">
                  <ShieldCheck className="w-8 h-8 text-status-success mx-auto mb-2 opacity-80" />
                  No transactions currently on hold. All funds transfers are operating normally.
                </div>
              ) : (
                <div className="space-y-4">
                  {heldTransactions.map((held) => (
                    <div
                      key={held.id}
                      className="p-4 bg-background-elevated border border-border rounded-sm space-y-3 font-mono text-xs"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border/60 pb-2.5">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-foreground">
                            {held.referenceNumber}
                          </span>
                          {getRiskBadge(held.riskLevel)}
                          {held.scamDetected && (
                            <Badge variant="error" size="sm">
                              SCAM DETECTED
                            </Badge>
                          )}
                        </div>
                        <span className="text-[11px] text-foreground-muted">
                          {formatDate(held.createdAt)}
                        </span>
                      </div>

                      <div className="grid grid-cols-2 gap-2 text-[11px]">
                        <div>
                          <span className="text-foreground-muted block">RECIPIENT:</span>
                          <span className="font-semibold text-foreground">
                            {held.destinationAccountHolder || held.destinationAccountNumber}
                          </span>
                        </div>
                        <div className="text-right">
                          <span className="text-foreground-muted block">AMOUNT:</span>
                          <span className="font-bold text-accent text-sm">
                            {formatCurrency(held.amount)}
                          </span>
                        </div>
                      </div>

                      {held.customerWarning && (
                        <div className="p-2.5 bg-status-danger/10 border border-status-danger/30 rounded-sm text-[11px] text-foreground leading-relaxed">
                          <strong className="text-status-danger block mb-0.5">
                            WARNING ADVISORY:
                          </strong>
                          {held.customerWarning}
                        </div>
                      )}

                      <div className="flex items-center justify-between pt-1">
                        <div className="flex items-center gap-1.5 text-[10px] text-foreground-muted">
                          <Clock className="w-3.5 h-3.5" />
                          <span>Status: <strong className="text-foreground">{held.state}</strong></span>
                        </div>

                        <div className="flex items-center gap-2">
                          {held.state === 'PENDING_CUSTOMER_VERIFICATION' && (
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() => handleOpenVerifyModal(held)}
                            >
                              VERIFY NOW
                            </Button>
                          )}

                          {held.state === 'PENDING_BANK_REVIEW' && (
                            <Badge variant="warning" size="sm">
                              BANK REVIEW PENDING
                            </Badge>
                          )}

                          {held.state !== 'SUCCESS' && held.state !== 'CANCELLED' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleCancelTransfer(held.id)}
                              isLoading={isCancelling}
                            >
                              CANCEL TRANSFER
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Recent Security Alerts */}
          <Card withCrosshairs>
            <CardHeader>
              <CardTitle>
                <AlertTriangle className="w-4 h-4 text-status-warning" />
                <span>FRAUD & SECURITY ALERTS</span>
              </CardTitle>
              <CardDescription>// Real-time account risk signals and scam detections</CardDescription>
            </CardHeader>
            <CardContent>
              {alerts.length === 0 ? (
                <div className="p-6 text-center text-xs font-mono text-foreground-muted">
                  No active security alerts recorded for your account.
                </div>
              ) : (
                <div className="space-y-3">
                  {alerts.map((alert) => (
                    <div
                      key={alert.id}
                      className={`p-3.5 rounded-sm border text-xs font-mono space-y-1.5 transition-colors ${
                        alert.isRead
                          ? 'bg-background-elevated/50 border-border text-foreground-secondary'
                          : 'bg-background-elevated border-accent/40 text-foreground'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2">
                          <Badge
                            variant={
                              alert.severity === 'CRITICAL'
                                ? 'error'
                                : alert.severity === 'HIGH'
                                ? 'warning'
                                : 'neutral'
                            }
                            size="sm"
                          >
                            {alert.severity}
                          </Badge>
                          <span className="font-semibold text-foreground">{alert.title}</span>
                        </div>
                        <span className="text-[10px] text-foreground-muted">
                          {formatDate(alert.createdAt)}
                        </span>
                      </div>
                      <p className="text-[11px] text-foreground-secondary leading-relaxed">
                        {alert.message}
                      </p>
                      {!alert.isRead && (
                        <div className="pt-1 text-right">
                          <button
                            type="button"
                            onClick={() => handleMarkAlertRead(alert.id)}
                            className="text-[10px] text-accent hover:underline inline-flex items-center gap-1"
                          >
                            <Eye className="w-3 h-3" /> Mark as Reviewed
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right Column: Digital Arrest Prevention Knowledge Base */}
        <div className="lg:col-span-5 space-y-6">
          <Card withCrosshairs variant="elevated" className="border-status-danger/30 bg-status-danger/5">
            <CardHeader>
              <CardTitle className="text-status-danger flex items-center gap-2">
                <PhoneCall className="w-4 h-4" />
                <span>DIGITAL ARREST PREVENTION GUIDE</span>
              </CardTitle>
              <CardDescription className="text-foreground-secondary">
                // सावधान: डिजिटल अरेस्ट साइबर ठगी से सुरक्षित रहें
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 text-xs font-mono">
              <div className="p-3 bg-background-elevated border border-border rounded-sm space-y-1">
                <span className="font-bold text-accent block">
                  1. NO POLICE / CBI DIGITAL ARREST:
                </span>
                <p className="text-foreground-secondary text-[11px] leading-relaxed">
                  Indian law enforcement agencies (CBI, ED, Police, Customs) NEVER conduct arrests, interrogations, or court hearings over Skype or WhatsApp video calls.
                </p>
                <p className="text-foreground-muted text-[10px] italic">
                  पुलिस या सीबीआई कभी भी वीडियो कॉल पर डिजिटल अरेस्ट या पूछताछ नहीं करती।
                </p>
              </div>

              <div className="p-3 bg-background-elevated border border-border rounded-sm space-y-1">
                <span className="font-bold text-accent block">
                  2. NO "SAFE" GOVERNMENT ACCOUNTS:
                </span>
                <p className="text-foreground-secondary text-[11px] leading-relaxed">
                  RBI or Government never requests customers to transfer money to a "safe custody account" to verify funds or unfreeze bank accounts.
                </p>
                <p className="text-foreground-muted text-[10px] italic">
                  आरबीआई या सरकार का कोई "सुरक्षित खाता" नहीं होता। ऐसे खाते में पैसे भेजना धोखाधड़ी है।
                </p>
              </div>

              <div className="p-3 bg-background-elevated border border-border rounded-sm space-y-1">
                <span className="font-bold text-accent block">
                  3. NEVER KEEP IT SECRET:
                </span>
                <p className="text-foreground-secondary text-[11px] leading-relaxed">
                  Scammers demand immediate secrecy. If pressured, hang up immediately and speak to your family or local police station.
                </p>
              </div>

              <div className="p-3 bg-background-elevated border border-border rounded-sm space-y-1">
                <span className="font-bold text-status-danger block">
                  NATIONAL CYBER CRIME HELPLINE:
                </span>
                <p className="text-sm font-bold text-foreground">DIAL 1930</p>
                <p className="text-[10px] text-foreground-muted">
                  Or report immediately at: <a href="https://cybercrime.gov.in" target="_blank" rel="noreferrer" className="underline text-accent">cybercrime.gov.in</a>
                </p>
              </div>
            </CardContent>
          </Card>

          {/* General Security Best Practices */}
          <Card withCrosshairs className="p-5 space-y-3 font-mono text-xs">
            <h4 className="font-heading font-bold text-foreground uppercase flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-accent" />
              <span>SECURITY RECOMMENDATIONS</span>
            </h4>
            <ul className="space-y-2 text-[11px] text-foreground-secondary">
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-status-success shrink-0 mt-0.5" />
                <span>Verify all recipients using the account number verification button before sending funds.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-status-success shrink-0 mt-0.5" />
                <span>Never install remote screen-sharing apps like AnyDesk or TeamViewer on stranger requests.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-status-success shrink-0 mt-0.5" />
                <span>Our AI Banking Copilot is always available in the 'COPILOT' tab to analyze suspicious demands.</span>
              </li>
            </ul>
          </Card>
        </div>
      </div>

      {/* Verification Questions Modal */}
      {selectedHeld && (
        <Modal
          isOpen={!!selectedHeld}
          onClose={() => setSelectedHeld(null)}
          title="CUSTOMER SECURITY VERIFICATION"
          maxWidth="lg"
        >
          <div className="space-y-5 font-mono text-xs">
            <div className="p-3 bg-background-elevated border border-border rounded-sm space-y-1">
              <div className="flex justify-between">
                <span className="text-foreground-muted">HELD TRANSACTION:</span>
                <span className="font-bold text-foreground">{selectedHeld.referenceNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-foreground-muted">AMOUNT:</span>
                <span className="font-bold text-accent">{formatCurrency(selectedHeld.amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-foreground-muted">RECIPIENT:</span>
                <span className="text-foreground font-semibold">
                  {selectedHeld.destinationAccountHolder || selectedHeld.destinationAccountNumber}
                </span>
              </div>
            </div>

            <div className="p-3 bg-status-danger/10 border border-status-danger/30 rounded-sm">
              <p className="text-[11px] text-foreground leading-relaxed">
                <strong>Safety Verification:</strong> To protect your funds against Digital Arrest and Social Engineering scams, please answer these contextual security questions truthfully:
              </p>
            </div>

            <div className="space-y-4">
              {selectedHeld.verificationQuestions.map((q, idx) => (
                <div key={idx} className="p-3 bg-background-elevated border border-border rounded-sm space-y-2">
                  <p className="font-semibold text-foreground text-[11px] flex items-start gap-1.5">
                    <HelpCircle className="w-3.5 h-3.5 text-accent shrink-0 mt-0.5" />
                    <span>{q}</span>
                  </p>
                  <div className="flex gap-4 pt-1">
                    <label className="flex items-center gap-1.5 cursor-pointer text-[11px]">
                      <input
                        type="radio"
                        name={`q-${idx}`}
                        value="YES"
                        checked={answers[q] === 'YES'}
                        onChange={() => setAnswers((prev) => ({ ...prev, [q]: 'YES' }))}
                        className="text-accent focus:ring-accent"
                      />
                      <span>YES (हाँ)</span>
                    </label>

                    <label className="flex items-center gap-1.5 cursor-pointer text-[11px]">
                      <input
                        type="radio"
                        name={`q-${idx}`}
                        value="NO"
                        checked={answers[q] === 'NO'}
                        onChange={() => setAnswers((prev) => ({ ...prev, [q]: 'NO' }))}
                        className="text-accent focus:ring-accent"
                      />
                      <span>NO (नहीं)</span>
                    </label>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-between gap-3 pt-3 border-t border-border">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleCancelTransfer(selectedHeld.id)}
                isLoading={isCancelling}
                leftIcon={<XCircle className="w-4 h-4" />}
              >
                CANCEL & KEEP MONEY SAFE
              </Button>

              <Button
                variant="primary"
                size="sm"
                onClick={handleSubmitVerification}
                isLoading={isSubmittingVerify}
                rightIcon={<CheckCircle2 className="w-4 h-4" />}
              >
                SUBMIT VERIFICATION
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
