import { apiClient, isMockSession } from '../api/apiClient';
import { TransferRequest, TransferResponse } from '@/types';
import { MOCK_ALICE_ACCOUNT } from '../mock/mockData';

export const transferService = {
  /**
   * Transfer funds between two bank accounts
   * POST /api/transfers
   */
  async transferMoney(request: TransferRequest): Promise<TransferResponse> {
    if (isMockSession()) {
      MOCK_ALICE_ACCOUNT.balance = Math.max(0, MOCK_ALICE_ACCOUNT.balance - request.amount);
      return {
        transferId: `TRF-MOCK-${Date.now().toString().slice(-6)}`,
        sourceAccountId: request.sourceAccountId,
        sourceAccountNumber: '1000000001',
        destinationAccountId: request.destinationAccountId,
        destinationAccountNumber: `100000000${request.destinationAccountId}`,
        amount: request.amount,
        sourceBalanceAfter: MOCK_ALICE_ACCOUNT.balance,
        status: 'SUCCESS',
        description: request.description || 'Instant Transfer',
        timestamp: new Date().toISOString(),
      };
    }

    const response = await apiClient.post<TransferResponse>('/api/transfers', request);
    return response.data;
  },
};
