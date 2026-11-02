import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import { transferService } from '@/services/transfers/transferService';
import { accountService } from '@/services/accounts/accountService';
import { fraudService } from '@/services/fraud/fraudService';
import { formatCurrency } from '@/lib/utils';
import { toast } from 'sonner';
import {
  Send,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Search,
  UserCheck,
  ShieldAlert,
  AlertOctagon,
  HelpCircle,
  XCircle,
} from 'lucide-react';
import { TransferResponse, AccountResponse, HeldTransaction } from '@/types';

export const TransfersPage: React.FC = () => {
  const { user } = useAuth();

  const [sourceAccountId] = useState(user?.accountId || 1);
  const [sourceAccountNumber] = useState(user?.accountNumber || '—');
  const [sourceBalance, setSourceBalance] = useState<number>(0);
  const [isLoadingBalance, setIsLoadingBalance] = useState(true);

  // Transfer form fields
  const [destAccNumber, setDestAccNumber] = useState('');
  const [resolvedAccount, setResolvedAccount] = useState<AccountResponse | null>(null);
  const [isResolving, setIsResolving] = useState(false);
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [userExplanation, setUserExplanation] = useState('');

  // Flow states
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [completedTransfer, setCompletedTransfer] = useState<TransferResponse | null>(null);

  // Held Transaction / Fraud Protection states
  const [heldTransfer, setHeldTransfer] = useState<TransferResponse | null>(null);
  const [heldDetails, setHeldDetails] = useState<HeldTransaction | null>(null);
  const [isVerifyModalOpen, setIsVerifyModalOpen] = useState(false);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [isSubmittingVerify, setIsSubmittingVerify] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);

  // Load live balance
  const loadBalance = useCallback(async () => {
    if (!user?.accountId) return;
    try {
      setIsLoadingBalance(true);
      const res = await accountService.getBalance(user.accountId);
      setSourceBalance(Number(res.balance));
    } catch {
      // Keep previous balance
    } finally {
      setIsLoadingBalance(false);
    }
  }, [user?.accountId]);

  useEffect(() => {
    loadBalance();
  }, [loadBalance]);

  // Lookup destination account by account number
  const handleResolveAccount = async () => {
    const trimmed = destAccNumber.trim();
    if (!trimmed) {
      toast.error('Please enter a recipient account number');
      return;
    }

    if (trimmed.toUpperCase() === sourceAccountNumber.toUpperCase()) {
      toast.error('Cannot transfer to your own account');
      return;
    }

    setIsResolving(true);
    setResolvedAccount(null);
    try {
      const acc = await accountService.getAccountByNumber(trimmed);
      setResolvedAccount(acc);
      toast.success(`Account verified: ${acc.accountHolderName}`);
    } catch {
      toast.error(`No active bank account found for "${trimmed}"`);
    } finally {
      setIsResolving(false);
    }
  };

  const handleInitiate = (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseFloat(amount);

    if (isNaN(numAmount) || numAmount <= 0) {
      toast.error('Please enter a valid transfer amount greater than 0');
      return;
    }

    if (numAmount > sourceBalance) {
      toast.error('Insufficient funds available in your account');
      return;
    }

    if (!resolvedAccount) {
      toast.error('Please verify the recipient account first using the verify button');
      return;
    }

    if (resolvedAccount.id === sourceAccountId) {
      toast.error('Source and Destination accounts must be different');
      return;
    }

    setIsConfirmOpen(true);
  };

  const handleExecuteTransfer = async () => {
    if (!resolvedAccount) return;
    const numAmount = parseFloat(amount);
    setIsSubmitting(true);
    try {
      const response = await transferService.transferMoney({
        sourceAccountId,
        destinationAccountId: resolvedAccount.id,
        amount: numAmount,
        description: description.trim() || 'Direct Account Transfer',
        userExplanation: userExplanation.trim() || undefined,
      });

      setIsConfirmOpen(false);

      if (response.status === 'ON_HOLD') {
        // Transfer flagged by AI fraud detection
        setHeldTransfer(response);
        setCompletedTransfer(null);
        toast.warning('Transaction flagged by AI Security Shield and placed on hold!');

        // Fetch full held details with questions
        if (response.heldTransactionId) {
          try {
            const fullHeld = await fraudService.getHeldTransactionById(response.heldTransactionId);
            setHeldDetails(fullHeld);
            // Prepopulate questions
            const initAnswers: Record<string, string> = {};
            fullHeld.verificationQuestions.forEach((q) => {
              initAnswers[q] = '';
            });
            setAnswers(initAnswers);
          } catch {
            // Use basic response
          }
        }
      } else {
        // Immediate success
        setCompletedTransfer(response);
        setHeldTransfer(null);
        setHeldDetails(null);
        setSourceBalance(response.sourceBalanceAfter);
        toast.success('Funds successfully transferred!');
        setAmount('');
        setDescription('');
        setUserExplanation('');
        setResolvedAccount(null);
        setDestAccNumber('');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Transfer failed';
      toast.error(msg);
      setIsConfirmOpen(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAnswerQuestions = async () => {
    if (!heldTransfer?.heldTransactionId) return;

    const unanswered = Object.values(answers).some((val) => !val);
    if (unanswered) {
      toast.error('Please answer all security questions before proceeding');
      return;
    }

    try {
      setIsSubmittingVerify(true);
      const updated = await fraudService.verifyTransaction(heldTransfer.heldTransactionId, answers);

      if (updated.state === 'SUCCESS' || updated.state === 'APPROVED') {
        toast.success('Security verification passed! Funds have been safely transferred.');
        setHeldTransfer(null);
        setHeldDetails(null);
        setIsVerifyModalOpen(false);
        setAmount('');
        setDescription('');
        setUserExplanation('');
        setResolvedAccount(null);
        setDestAccNumber('');
        loadBalance();
      } else if (updated.state === 'PENDING_BANK_REVIEW') {
        toast.warning('Digital arrest or scam risk detected. Transfer held for Bank Officer Review.');
        setHeldDetails(updated);
        setIsVerifyModalOpen(false);
      } else {
        toast.error('Verification failed. Transaction cancelled or blocked for safety.');
        setHeldDetails(updated);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Verification failed';
      toast.error(msg);
    } finally {
      setIsSubmittingVerify(false);
    }
  };

  const handleCancelHeldTransfer = async () => {
    if (!heldTransfer?.heldTransactionId) return;
    try {
      setIsCancelling(true);
      await fraudService.cancelTransaction(heldTransfer.heldTransactionId);
      toast.success('Held transaction cancelled. No funds were debited from your account.');
      setHeldTransfer(null);
      setHeldDetails(null);
      setIsVerifyModalOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Cancel failed';
      toast.error(msg);
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <div className="space-y-8">
      <PageHeader
        index="[03]"
        tag="MONEY TRANSFER"
        title="SEND MONEY"
        subtitle="INSTANT INTER-ACCOUNT FUND TRANSFERS WITH REAL-TIME BALANCE CLEARING"
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Transfer Form Card */}
        <div className="lg:col-span-7 space-y-6">
          <Card withCrosshairs variant="elevated">
            <CardHeader>
              <CardTitle>
                <Send className="w-4 h-4 text-accent" />
                <span>TRANSFER DETAILS</span>
              </CardTitle>
              <CardDescription>// Direct Bank-to-Bank Transfer</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleInitiate} className="space-y-5">
                {/* Source Account Display */}
                <div className="p-4 bg-background-elevated border border-border rounded-sm space-y-1">
                  <span className="text-[10px] font-mono uppercase text-foreground-muted tracking-tech">
                    FROM (YOUR ACCOUNT)
                  </span>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-heading font-semibold text-foreground">
                        {user?.fullName || 'Active User'}
                      </p>
                      <p className="text-xs font-mono text-foreground-secondary">
                        {sourceAccountNumber}
                      </p>
                    </div>
                    <div className="text-right">
                      <span className="text-[10px] font-mono text-foreground-muted block">
                        AVAILABLE
                      </span>
                      <span className="text-sm font-mono font-bold text-accent">
                        {isLoadingBalance ? '...' : formatCurrency(sourceBalance)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Destination Account Number + Verify */}
                <div className="space-y-2">
                  <div className="flex gap-2 items-end">
                    <div className="flex-1">
                      <Input
                        label="Recipient Account Number"
                        placeholder="e.g. ACC-1002-3941"
                        value={destAccNumber}
                        onChange={(e) => {
                          setDestAccNumber(e.target.value);
                          setResolvedAccount(null);
                        }}
                        required
                        isMonospace
                      />
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      size="md"
                      onClick={handleResolveAccount}
                      isLoading={isResolving}
                      leftIcon={<Search className="w-4 h-4" />}
                    >
                      VERIFY
                    </Button>
                  </div>

                  {/* Resolved Account Banner */}
                  {resolvedAccount && (
                    <div className="p-3 bg-status-success/10 border border-status-success/30 rounded-sm flex items-center justify-between text-xs font-mono">
                      <div className="flex items-center gap-2">
                        <UserCheck className="w-4 h-4 text-status-success" />
                        <div>
                          <span className="font-bold text-foreground">
                            {resolvedAccount.accountHolderName}
                          </span>
                          <span className="text-foreground-secondary ml-2">
                            ({resolvedAccount.accountNumber})
                          </span>
                        </div>
                      </div>
                      <span className="text-status-success font-semibold">VERIFIED</span>
                    </div>
                  )}
                </div>

                {/* Amount */}
                <Input
                  label="Transfer Amount (₹ INR)"
                  placeholder="0.00"
                  type="number"
                  step="0.01"
                  min="1"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                  isMonospace
                />

                {/* Description */}
                <Input
                  label="Payment Memo / Remarks (Optional)"
                  placeholder="e.g. Rent payment, Dinner split, Invoice #12"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />

                {/* Optional Purpose / Explanation for AI Shield */}
                <Input
                  label="Transfer Reason / Explanation (Scam Protection Check)"
                  placeholder="e.g. Paying family member, Online purchase, Or note instructions received"
                  value={userExplanation}
                  onChange={(e) => setUserExplanation(e.target.value)}
                />

                {/* Submit Action */}
                <div className="pt-2">
                  <Button
                    variant="primary"
                    size="lg"
                    type="submit"
                    className="w-full justify-center"
                    disabled={isSubmitting || !resolvedAccount}
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                  >
                    CONTINUE TO CONFIRMATION
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        {/* Transfer Confirmation or Safety Guidelines */}
        <div className="lg:col-span-5 space-y-6">
          {heldTransfer ? (
            <Card withCrosshairs variant="elevated" className="p-6 border-status-danger/40 bg-status-danger/10">
              <div className="flex items-center gap-2 text-status-danger mb-3">
                <ShieldAlert className="w-6 h-6 text-status-danger animate-pulse" />
                <div>
                  <h3 className="font-heading font-bold text-sm uppercase text-foreground">
                    TRANSACTION PLACED ON HOLD
                  </h3>
                  <span className="text-[10px] font-mono text-status-danger font-semibold">
                    AI FRAUD SHIELD INTERVENTION
                  </span>
                </div>
              </div>

              <div className="mb-3 flex items-center gap-2">
                <Badge variant={heldTransfer.riskLevel === 'CRITICAL' ? 'error' : 'warning'}>
                  {heldTransfer.riskLevel} RISK
                </Badge>
                {heldTransfer.requiresAdminReview && (
                  <Badge variant="error">ADMIN REVIEW REQUIRED</Badge>
                )}
              </div>

              <p className="text-xs font-mono text-foreground leading-relaxed mb-4 bg-background-elevated p-3 border border-border rounded-sm">
                <strong className="text-status-danger block mb-1">सुरक्षा चेतावनी / SECURITY ADVISORY:</strong>
                {heldTransfer.warningMessage ||
                  'This transaction has been held to protect your account against unauthorized transfers or potential digital arrest scams.'}
              </p>

              <div className="space-y-2 text-xs font-mono border-t border-border pt-3">
                <div className="flex justify-between py-1 border-b border-border/60">
                  <span className="text-foreground-muted">HOLD REFERENCE:</span>
                  <span className="text-foreground font-semibold">{heldTransfer.transferId}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-border/60">
                  <span className="text-foreground-muted">HOLD AMOUNT:</span>
                  <span className="text-accent font-bold">
                    {formatCurrency(heldTransfer.amount)}
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-border/60">
                  <span className="text-foreground-muted">ACCOUNT DEBITED:</span>
                  <span className="text-status-success font-semibold">NO (₹0.00 DEBITED)</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-foreground-muted">STATUS:</span>
                  <span className="text-status-warning font-semibold">
                    {heldTransfer.requiresAdminReview ? 'PENDING BANK OFFICER REVIEW' : 'AWAITING VERIFICATION'}
                  </span>
                </div>
              </div>

              <div className="mt-5 pt-4 border-t border-border space-y-2">
                {heldTransfer.verificationRequired && (
                  <Button
                    variant="primary"
                    size="sm"
                    className="w-full text-xs font-mono justify-center"
                    onClick={() => setIsVerifyModalOpen(true)}
                  >
                    ANSWER VERIFICATION QUESTIONS
                  </Button>
                )}

                <Button
                  variant="outline"
                  size="sm"
                  className="w-full text-xs font-mono justify-center"
                  onClick={handleCancelHeldTransfer}
                  isLoading={isCancelling}
                >
                  CANCEL & ABORT TRANSFER
                </Button>
              </div>
            </Card>
          ) : completedTransfer ? (
            <Card withCrosshairs variant="elevated" className="p-6 border-status-success/40 bg-status-success/5">
              <div className="flex items-center gap-2 text-status-success mb-3">
                <CheckCircle2 className="w-5 h-5 text-status-success" />
                <h3 className="font-heading font-bold text-sm uppercase text-foreground">
                  TRANSFER COMPLETED
                </h3>
              </div>
              <p className="text-xs font-mono text-foreground-secondary mb-4 leading-relaxed">
                The money has been debited and instantly credited to the recipient.
              </p>

              <div className="space-y-2.5 text-xs font-mono border-t border-border pt-4">
                <div className="flex justify-between py-1 border-b border-border/60">
                  <span className="text-foreground-muted">REFERENCE ID:</span>
                  <span className="text-foreground font-semibold">{completedTransfer.transferId}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-border/60">
                  <span className="text-foreground-muted">AMOUNT SENT:</span>
                  <span className="text-accent font-bold">
                    {formatCurrency(completedTransfer.amount)}
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-border/60">
                  <span className="text-foreground-muted">TO ACCOUNT:</span>
                  <span className="text-foreground font-semibold">
                    {completedTransfer.destinationAccountNumber || `#${completedTransfer.destinationAccountId}`}
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-border/60">
                  <span className="text-foreground-muted">UPDATED BALANCE:</span>
                  <span className="text-status-success font-semibold">
                    {formatCurrency(completedTransfer.sourceBalanceAfter)}
                  </span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-foreground-muted">STATUS:</span>
                  <span className="text-status-success font-semibold">
                    {completedTransfer.status}
                  </span>
                </div>
              </div>

              <div className="mt-5 pt-4 border-t border-border">
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full text-xs font-mono"
                  onClick={() => setCompletedTransfer(null)}
                >
                  SEND ANOTHER PAYMENT
                </Button>
              </div>
            </Card>
          ) : (
            <Card withCrosshairs className="p-6">
              <h3 className="font-heading font-bold text-sm uppercase text-foreground mb-3 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-accent" />
                <span>TRANSFER SAFETY RULES</span>
              </h3>
              <p className="text-xs font-mono text-foreground-secondary leading-relaxed mb-4">
                All transactions are monitored in real time by our AI Fraud Shield. High-risk spikes,
                first-time transfers, and digital arrest threats automatically enter protective hold.
              </p>
              <div className="space-y-3 text-[11px] font-mono text-foreground-muted">
                <div className="p-2.5 bg-background-elevated border border-border rounded-sm">
                  <span className="text-accent font-bold">01. RECIPIENT VERIFICATION:</span>
                  <p className="text-foreground-secondary mt-0.5">
                    Always use the "VERIFY" button to double-check recipient details before sending funds.
                  </p>
                </div>
                <div className="p-2.5 bg-background-elevated border border-border rounded-sm">
                  <span className="text-accent font-bold">02. DIGITAL ARREST DEFENSE:</span>
                  <p className="text-foreground-secondary mt-0.5">
                    Police, CBI, ED, and RBI never demand money on Skype or video calls. Such transfers are blocked.
                  </p>
                </div>
                <div className="p-2.5 bg-background-elevated border border-border rounded-sm">
                  <span className="text-accent font-bold">03. ZERO PRE-DEBIT:</span>
                  <p className="text-foreground-secondary mt-0.5">
                    Held transfers never deduct money from your account until all security verifications pass.
                  </p>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* Confirmation Dialog before Transfer */}
      <ConfirmationDialog
        isOpen={isConfirmOpen}
        onClose={() => setIsConfirmOpen(false)}
        onConfirm={handleExecuteTransfer}
        title="CONFIRM MONEY TRANSFER"
        message={`Are you sure you want to send ${formatCurrency(
          parseFloat(amount) || 0
        )} to ${resolvedAccount?.accountHolderName} (${resolvedAccount?.accountNumber})?`}
        confirmLabel="YES, SEND MONEY"
        variant="primary"
        isLoading={isSubmitting}
      />

      {/* Security Verification Modal */}
      {heldDetails && (
        <Modal
          isOpen={isVerifyModalOpen}
          onClose={() => setIsVerifyModalOpen(false)}
          title="SECURITY CHECK: SCAM PREVENTION"
          maxWidth="lg"
        >
          <div className="space-y-5 font-mono text-xs">
            <div className="p-3 bg-status-danger/10 border border-status-danger/30 rounded-sm">
              <div className="flex items-center gap-2 text-status-danger font-bold mb-1">
                <AlertOctagon className="w-4 h-4" />
                <span>FRAUD DEFENSE VERIFICATION REQUIRED</span>
              </div>
              <p className="text-[11px] text-foreground leading-relaxed">
                {heldDetails.customerWarning ||
                  'To safeguard your savings, please answer these contextual security questions truthfully.'}
              </p>
            </div>

            <div className="space-y-4">
              {heldDetails.verificationQuestions && heldDetails.verificationQuestions.map((q, idx) => (
                <div key={idx} className="p-3 bg-background-elevated border border-border rounded-sm space-y-2">
                  <p className="font-semibold text-foreground text-[11px] flex items-start gap-1.5">
                    <HelpCircle className="w-3.5 h-3.5 text-accent shrink-0 mt-0.5" />
                    <span>{q}</span>
                  </p>
                  <div className="flex gap-4 pt-1">
                    <label className="flex items-center gap-1.5 cursor-pointer text-[11px]">
                      <input
                        type="radio"
                        name={`modal-q-${idx}`}
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
                        name={`modal-q-${idx}`}
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
                onClick={handleCancelHeldTransfer}
                isLoading={isCancelling}
                leftIcon={<XCircle className="w-4 h-4" />}
              >
                CANCEL & KEEP SAFE
              </Button>

              <Button
                variant="primary"
                size="sm"
                onClick={handleAnswerQuestions}
                isLoading={isSubmittingVerify}
                rightIcon={<CheckCircle2 className="w-4 h-4" />}
              >
                SUBMIT ANSWERS
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
