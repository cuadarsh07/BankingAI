import { apiClient } from '../api/apiClient';
import { PageResponse, TransactionQueryFilters, TransactionRecord } from '@/types';

export const transactionService = {
  /**
   * Retrieve paginated and filtered transactions for an account
   * GET /api/accounts/{accountId}/transactions
   */
  async getAccountTransactions(
    accountId: number,
    filters: TransactionQueryFilters = {}
  ): Promise<PageResponse<TransactionRecord>> {
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
  },
};

