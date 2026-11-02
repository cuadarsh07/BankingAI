import { Role, TransactionType } from '../domain/enums';

export interface LoginRequest {
  username: string; // Email
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  fullName: string;
  role?: Role;
  initialDeposit?: number;
}

export interface CreateAccountRequest {
  accountHolderName: string;
  email: string;
  accountNumber?: string;
  initialDeposit?: number;
}

export interface UpdateAccountRequest {
  accountHolderName: string;
  email: string;
}

export interface PatchAccountRequest {
  accountHolderName?: string;
  email?: string;
  status?: import('../domain/enums').AccountStatus;
}

export interface TransactionRequest {
  amount: number;
  description?: string;
}

export interface TransferRequest {
  sourceAccountId: number;
  destinationAccountId: number;
  amount: number;
  description?: string;
  userExplanation?: string;
}

export interface VerificationSubmitRequest {
  answers: Record<string, string>;
}

export interface AdminReviewRequest {
  action: 'APPROVE' | 'REJECT';
  notes?: string;
  reason?: string;
}

export interface AssistantChatRequest {
  message: string;
}

export interface TransactionQueryFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'ASC' | 'DESC';
  type?: TransactionType;
  startDate?: string;
  endDate?: string;
}

