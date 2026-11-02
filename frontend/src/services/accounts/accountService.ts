import { apiClient } from '../api/apiClient';
import {
  AccountResponse,
  BalanceResponse,
  CreateAccountRequest,
  PatchAccountRequest,
  TransactionRequest,
  TransactionResponse,
  UpdateAccountRequest,
} from '@/types';

export const accountService = {
  /**
   * Retrieve account by ID
   * GET /api/accounts/{id}
   */
  async getAccountById(id: number): Promise<AccountResponse> {
    const response = await apiClient.get<AccountResponse>(`/api/accounts/${id}`);
    return response.data;
  },

  /**
   * Retrieve account by Account Number
   * GET /api/accounts/by-number/{accountNumber}
   */
  async getAccountByNumber(accountNumber: string): Promise<AccountResponse> {
    const response = await apiClient.get<AccountResponse>(`/api/accounts/by-number/${accountNumber}`);
    return response.data;
  },

  /**
   * Retrieve all accounts (Admin only)
   * GET /api/accounts
   */
  async getAllAccounts(): Promise<AccountResponse[]> {
    const response = await apiClient.get<AccountResponse[]>('/api/accounts');
    return response.data;
  },

  /**
   * Create a new bank account
   * POST /api/accounts
   */
  async createAccount(request: CreateAccountRequest): Promise<AccountResponse> {
    const response = await apiClient.post<AccountResponse>('/api/accounts', request);
    return response.data;
  },

  /**
   * Update full account profile
   * PUT /api/accounts/{id}
   */
  async updateAccount(id: number, request: UpdateAccountRequest): Promise<AccountResponse> {
    const response = await apiClient.put<AccountResponse>(`/api/accounts/${id}`, request);
    return response.data;
  },

  /**
   * Patch account details
   * PATCH /api/accounts/{id}
   */
  async patchAccount(id: number, request: PatchAccountRequest): Promise<AccountResponse> {
    const response = await apiClient.patch<AccountResponse>(`/api/accounts/${id}`, request);
    return response.data;
  },

  /**
   * Deposit funds
   * POST /api/accounts/{id}/deposit
   */
  async deposit(id: number, request: TransactionRequest): Promise<TransactionResponse> {
    const response = await apiClient.post<TransactionResponse>(`/api/accounts/${id}/deposit`, request);
    return response.data;
  },

  /**
   * Withdraw funds
   * POST /api/accounts/{id}/withdraw
   */
  async withdraw(id: number, request: TransactionRequest): Promise<TransactionResponse> {
    const response = await apiClient.post<TransactionResponse>(`/api/accounts/${id}/withdraw`, request);
    return response.data;
  },

  /**
   * Check balance
   * GET /api/accounts/{id}/balance
   */
  async getBalance(id: number): Promise<BalanceResponse> {
    const response = await apiClient.get<BalanceResponse>(`/api/accounts/${id}/balance`);
    return response.data;
  },
};

