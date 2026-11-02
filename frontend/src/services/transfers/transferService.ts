import { apiClient } from '../api/apiClient';
import { TransferRequest, TransferResponse } from '@/types';

export const transferService = {
  /**
   * Transfer funds between two bank accounts
   * POST /api/transfers
   */
  async transferMoney(request: TransferRequest): Promise<TransferResponse> {
    const response = await apiClient.post<TransferResponse>('/api/transfers', request);
    return response.data;
  },
};

