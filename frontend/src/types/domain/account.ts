import { AccountStatus } from './enums';

export interface Account {
  id: number;
  accountNumber: string;
  accountHolderName: string;
  email: string;
  balance: number;
  status: AccountStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BalanceInfo {
  accountId: number;
  accountNumber: string;
  balance: number;
  status: AccountStatus;
}

