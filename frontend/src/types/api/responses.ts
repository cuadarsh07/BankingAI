import { AccountStatus, Role } from '../domain/enums';

export interface AuthResponse {
  token: string;
  tokenType: string;
  username: string;
  fullName: string;
  role: Role;
  accountId: number | null;
  accountNumber: string | null;
}

export interface AccountResponse {
  id: number;
  accountNumber: string;
  accountHolderName: string;
  email: string;
  balance: number;
  status: AccountStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BalanceResponse {
  accountId: number;
  accountNumber: string;
  balance: number;
  status: AccountStatus;
}

export interface TransactionResponse {
  transactionType: string;
  accountId: number;
  accountNumber: string;
  transactionAmount: number;
  previousBalance: number;
  newBalance: number;
  accountStatus: AccountStatus;
  description: string;
  timestamp: string;
}

export interface TransferResponse {
  transferId: string;
  sourceAccountId: number;
  sourceAccountNumber: string;
  destinationAccountId: number;
  destinationAccountNumber: string;
  amount: number;
  sourceBalanceAfter: number;
  status: string;
  description: string;
  timestamp: string;
  riskLevel?: string;
  heldTransactionId?: number;
  warningMessage?: string;
  verificationRequired?: boolean;
  requiresAdminReview?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message: string;
  path?: string;
  requestId?: string;
  validationErrors?: Record<string, string>;
}
