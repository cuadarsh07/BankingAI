import { apiClient, isMockSession } from '../api/apiClient';
import {
  AccountResponse,
  BalanceResponse,
  CreateAccountRequest,
  PatchAccountRequest,
  TransactionRequest,
  TransactionResponse,
  UpdateAccountRequest,
} from '@/types';
import {
  MOCK_ALICE_ACCOUNT,
  MOCK_ACCOUNTS,
} from '../mock/mockData';

export const accountService = {
  /**
   * Retrieve account by ID
   * GET /api/accounts/{id}
   */
  async getAccountById(id: number): Promise<AccountResponse> {
    if (isMockSession()) {
      const match = MOCK_ACCOUNTS.find((a) => a.id === id);
      return match || MOCK_ALICE_ACCOUNT;
    }

    try {
      const response = await apiClient.get<AccountResponse>(`/api/accounts/${id}`);
      return response.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock account data fallback:', err);
      const match = MOCK_ACCOUNTS.find((a) => a.id === id);
      return match || MOCK_ALICE_ACCOUNT;
    }
  },

  /**
   * Retrieve account by Account Number
   * GET /api/accounts/by-number/{accountNumber}
   */
  async getAccountByNumber(accountNumber: string): Promise<AccountResponse> {
    if (isMockSession()) {
      const match = MOCK_ACCOUNTS.find((a) => a.accountNumber === accountNumber);
      return match || MOCK_ALICE_ACCOUNT;
    }

    try {
      const response = await apiClient.get<AccountResponse>(`/api/accounts/by-number/${accountNumber}`);
      return response.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock account data fallback:', err);
      const match = MOCK_ACCOUNTS.find((a) => a.accountNumber === accountNumber);
      return match || MOCK_ALICE_ACCOUNT;
    }
  },

  /**
   * Retrieve all accounts (Admin only)
   * GET /api/accounts
   */
  async getAllAccounts(): Promise<AccountResponse[]> {
    if (isMockSession()) {
      return MOCK_ACCOUNTS;
    }

    try {
      const response = await apiClient.get<AccountResponse[]>('/api/accounts');
      return response.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock accounts fallback:', err);
      return MOCK_ACCOUNTS;
    }
  },

  /**
   * Create a new bank account
   * POST /api/accounts
   */
  async createAccount(request: CreateAccountRequest): Promise<AccountResponse> {
    if (isMockSession()) {
      const newAcc: AccountResponse = {
        id: Date.now(),
        accountNumber: `100000000${MOCK_ACCOUNTS.length + 1}`,
        accountHolderName: request.accountHolderName,
        email: request.email,
        balance: request.initialDeposit || 0,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      MOCK_ACCOUNTS.push(newAcc);
      return newAcc;
    }

    const response = await apiClient.post<AccountResponse>('/api/accounts', request);
    return response.data;
  },

  /**
   * Update full account profile
   * PUT /api/accounts/{id}
   */
  async updateAccount(id: number, request: UpdateAccountRequest): Promise<AccountResponse> {
    if (isMockSession()) {
      const acc = await this.getAccountById(id);
      acc.accountHolderName = request.accountHolderName;
      acc.email = request.email;
      return acc;
    }

    const response = await apiClient.put<AccountResponse>(`/api/accounts/${id}`, request);
    return response.data;
  },

  /**
   * Patch account details
   * PATCH /api/accounts/{id}
   */
  async patchAccount(id: number, request: PatchAccountRequest): Promise<AccountResponse> {
    if (isMockSession()) {
      const acc = await this.getAccountById(id);
      if (request.accountHolderName) acc.accountHolderName = request.accountHolderName;
      if (request.email) acc.email = request.email;
      if (request.status) acc.status = request.status;
      return acc;
    }

    const response = await apiClient.patch<AccountResponse>(`/api/accounts/${id}`, request);
    return response.data;
  },

  /**
   * Deposit funds
   * POST /api/accounts/{id}/deposit
   */
  async deposit(id: number, request: TransactionRequest): Promise<TransactionResponse> {
    if (isMockSession()) {
      const acc = await this.getAccountById(id);
      const prev = acc.balance;
      acc.balance = prev + request.amount;
      return {
        transactionType: 'DEPOSIT',
        accountId: acc.id,
        accountNumber: acc.accountNumber,
        transactionAmount: request.amount,
        previousBalance: prev,
        newBalance: acc.balance,
        accountStatus: acc.status,
        description: request.description || 'Account Deposit',
        timestamp: new Date().toISOString(),
      };
    }

    const response = await apiClient.post<TransactionResponse>(`/api/accounts/${id}/deposit`, request);
    return response.data;
  },

  /**
   * Withdraw funds
   * POST /api/accounts/{id}/withdraw
   */
  async withdraw(id: number, request: TransactionRequest): Promise<TransactionResponse> {
    if (isMockSession()) {
      const acc = await this.getAccountById(id);
      const prev = acc.balance;
      acc.balance = Math.max(0, prev - request.amount);
      return {
        transactionType: 'WITHDRAW',
        accountId: acc.id,
        accountNumber: acc.accountNumber,
        transactionAmount: request.amount,
        previousBalance: prev,
        newBalance: acc.balance,
        accountStatus: acc.status,
        description: request.description || 'Account Withdrawal',
        timestamp: new Date().toISOString(),
      };
    }

    const response = await apiClient.post<TransactionResponse>(`/api/accounts/${id}/withdraw`, request);
    return response.data;
  },

  /**
   * Check balance
   * GET /api/accounts/{id}/balance
   */
  async getBalance(id: number): Promise<BalanceResponse> {
    if (isMockSession()) {
      const acc = await this.getAccountById(id);
      return {
        accountId: acc.id,
        accountNumber: acc.accountNumber,
        balance: acc.balance,
        status: acc.status,
      };
    }

    try {
      const response = await apiClient.get<BalanceResponse>(`/api/accounts/${id}/balance`);
      return response.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock balance fallback:', err);
      const acc = await this.getAccountById(id);
      return {
        accountId: acc.id,
        accountNumber: acc.accountNumber,
        balance: acc.balance,
        status: acc.status,
      };
    }
  },
};
