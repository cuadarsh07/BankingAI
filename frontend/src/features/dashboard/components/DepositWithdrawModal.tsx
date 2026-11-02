import React, { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { accountService } from '@/services/accounts/accountService';
import { formatCurrency } from '@/lib/utils';
import { toast } from 'sonner';
import { ArrowDownLeft, ArrowUpRight } from 'lucide-react';

interface DepositWithdrawModalProps {
  isOpen: boolean;
  onClose: () => void;
  type: 'DEPOSIT' | 'WITHDRAW';
  accountId: number;
  currentBalance: number;
  onSuccess: (newBalance?: number) => void;
}

export const DepositWithdrawModal: React.FC<DepositWithdrawModalProps> = ({
  isOpen,
  onClose,
  type,
  accountId,
  currentBalance,
  onSuccess,
}) => {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const isDeposit = type === 'DEPOSIT';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseFloat(amount);

    if (isNaN(numAmount) || numAmount <= 0) {
      toast.error('Please enter a valid amount greater than 0');
      return;
    }

    if (!isDeposit && numAmount > currentBalance) {
      toast.error('Insufficient funds for this withdrawal amount');
      return;
    }

    setIsLoading(true);
    try {
      if (isDeposit) {
        const res = await accountService.deposit(accountId, {
          amount: numAmount,
          description: description.trim() || 'Cash Deposit',
        });
        toast.success(`Successfully deposited ${formatCurrency(numAmount)}`);
        onSuccess(res.newBalance);
      } else {
        const res = await accountService.withdraw(accountId, {
          amount: numAmount,
          description: description.trim() || 'Cash Withdrawal',
        });
        toast.success(`Successfully withdrew ${formatCurrency(numAmount)}`);
        onSuccess(res.newBalance);
      }
      setAmount('');
      setDescription('');
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Transaction failed';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isDeposit ? 'DEPOSIT FUNDS' : 'WITHDRAW FUNDS'}
      description={`// Account #${accountId} • Instant Balance Adjustment`}
      maxWidth="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="p-3 bg-background-elevated border border-border rounded-sm flex items-center justify-between text-xs font-mono">
          <span className="text-foreground-secondary">AVAILABLE BALANCE:</span>
          <span className="text-foreground font-bold">{formatCurrency(currentBalance)}</span>
        </div>

        <Input
          label="Amount (₹ INR)"
          placeholder="0.00"
          type="number"
          step="0.01"
          min="1"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
          isMonospace
          leftIcon={
            isDeposit ? (
              <ArrowDownLeft className="w-4 h-4 text-status-success" />
            ) : (
              <ArrowUpRight className="w-4 h-4 text-status-error" />
            )
          }
        />

        <Input
          label="Description / Purpose (Optional)"
          placeholder={isDeposit ? 'e.g. Salary deposit, Cash add' : 'e.g. ATM withdrawal, Bill payment'}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-border">
          <Button variant="outline" size="sm" type="button" onClick={onClose} disabled={isLoading}>
            CANCEL
          </Button>
          <Button
            variant={isDeposit ? 'primary' : 'secondary'}
            size="sm"
            type="submit"
            isLoading={isLoading}
          >
            {isDeposit ? 'CONFIRM DEPOSIT' : 'CONFIRM WITHDRAWAL'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
