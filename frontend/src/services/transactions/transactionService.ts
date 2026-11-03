import { apiClient, isMockSession } from '../api/apiClient';
import { PageResponse, TransactionQueryFilters, TransactionRecord } from '@/types';
import { getMockTransactionsPage } from '../mock/mockData';

export const transactionService = {
  /**
   * Retrieve paginated and filtered transactions for an account
   * GET /api/accounts/{accountId}/transactions
   */
  async getAccountTransactions(
    accountId: number,
    filters: TransactionQueryFilters = {}
  ): Promise<PageResponse<TransactionRecord>> {
    if (isMockSession()) {
      return getMockTransactionsPage(filters);
    }

    try {
      const params = new URLSearchParams();

      if (filters.page !== undefined) params.append('page', filters.page.toString());
      if (filters.size !== undefined) params.append('size', filters.size.toString());
      if (filters.sortBy) params.append('sortBy', filters.sortBy);
      if (filters.direction) params.append('direction', filters.direction);
      if (filters.type) params.append('type', filters.type);
      if (filters.startDate) params.append('startDate', filters.startDate);
      if (filters.endDate) params.append('endDate', filters.endDate);

      const response = await apiClient.get<PageResponse<TransactionRecord>>(
        `/api/accounts/${accountId}/transactions`,
        { params }
      );
      return response.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock transactions fallback:', err);
      return getMockTransactionsPage(filters);
    }
  },
};
