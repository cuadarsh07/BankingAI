import { apiClient } from '@/services/api/apiClient';
import {
  FraudAlert,
  FraudAssessment,
  HeldTransaction,
  AdminFraudDashboardData,
  AssistantChatResponse,
  AdminReviewRequest,
  TransferRequest,
} from '@/types';

export const fraudService = {
  // Pre-transfer Risk Assessment
  assessRisk: async (data: Partial<TransferRequest>): Promise<FraudAssessment> => {
    const res = await apiClient.post<FraudAssessment>('/api/fraud/assessments', data);
    return res.data;
  },

  // Customer Fraud Alerts
  getAlerts: async (): Promise<FraudAlert[]> => {
    const res = await apiClient.get<FraudAlert[]>('/api/fraud/alerts');
    return res.data;
  },

  markAlertRead: async (id: number): Promise<void> => {
    await apiClient.patch(`/api/fraud/alerts/${id}/read`);
  },

  // Customer Held Transactions
  getHeldTransactions: async (): Promise<HeldTransaction[]> => {
    const res = await apiClient.get<HeldTransaction[]>('/api/fraud/held-transactions');
    return res.data;
  },

  getHeldTransactionById: async (id: number): Promise<HeldTransaction> => {
    const res = await apiClient.get<HeldTransaction>(`/api/fraud/held-transactions/${id}`);
    return res.data;
  },

  // Verification submission
  verifyTransaction: async (
    id: number,
    answers: Record<string, string>
  ): Promise<HeldTransaction> => {
    const res = await apiClient.post<HeldTransaction>(
      `/api/fraud/transactions/${id}/verify`,
      { answers }
    );
    return res.data;
  },

  // Cancel held transaction
  cancelTransaction: async (id: number): Promise<HeldTransaction> => {
    const res = await apiClient.post<HeldTransaction>(
      `/api/fraud/transactions/${id}/cancel`
    );
    return res.data;
  },

  // Request Bank Review
  requestReview: async (id: number): Promise<HeldTransaction> => {
    const res = await apiClient.post<HeldTransaction>(
      `/api/fraud/transactions/${id}/request-review`
    );
    return res.data;
  },

  // Admin Monitoring Dashboard
  getAdminMonitoring: async (): Promise<AdminFraudDashboardData> => {
    const res = await apiClient.get<AdminFraudDashboardData>(
      '/api/admin/fraud/monitoring'
    );
    return res.data;
  },

  // Admin Review action
  reviewTransaction: async (
    id: number,
    request: AdminReviewRequest
  ): Promise<HeldTransaction> => {
    const res = await apiClient.post<HeldTransaction>(
      `/api/admin/fraud/transactions/${id}/review`,
      request
    );
    return res.data;
  },

  // Contextual Assistant Chat
  chatWithAssistant: async (message: string): Promise<AssistantChatResponse> => {
    const res = await apiClient.post<AssistantChatResponse>('/api/assistant/chat', {
      message,
    });
    return res.data;
  },
};

