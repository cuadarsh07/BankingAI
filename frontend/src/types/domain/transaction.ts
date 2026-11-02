import { TransactionType } from './enums';

export interface TransactionRecord {
  id: number;
  referenceNumber: string;
  accountId: number;
  accountNumber: string;
  counterpartyAccountId: number | null;
  counterpartyAccountNumber: string | null;
  type: TransactionType;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

