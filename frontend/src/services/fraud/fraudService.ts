import { apiClient, isMockSession } from '@/services/api/apiClient';
import {
  FraudAlert,
  FraudAssessment,
  HeldTransaction,
  AdminFraudDashboardData,
  AssistantChatResponse,
  AdminReviewRequest,
  TransferRequest,
} from '@/types';
import {
  MOCK_ALERTS,
  MOCK_HELD_TRANSACTIONS,
  MOCK_ADMIN_DASHBOARD,
  getMockAssistantReply,
} from '../mock/mockData';

export const fraudService = {
  // Pre-transfer Risk Assessment
  assessRisk: async (data: Partial<TransferRequest>): Promise<FraudAssessment> => {
    if (isMockSession()) {
      const isSuspect =
        (data.amount && data.amount > 20000) ||
        (data.description && /police|cbi|arrest|customs|escrow/i.test(data.description));

      return {
        assessmentId: `ASM-MOCK-${Date.now()}`,
        transactionReference: `TRF-REF-${Date.now().toString().slice(-4)}`,
        sourceAccountId: data.sourceAccountId || 1,
        destinationAccountId: 999,
        amount: data.amount || 0,
        riskLevel: isSuspect ? 'CRITICAL' : 'LOW',
        scamDetected: Boolean(isSuspect),
        scamCategories: isSuspect ? ['DIGITAL_ARREST', 'COERCIVE_EXTORTION'] : [],
        confidence: isSuspect ? 0.94 : 0.05,
        detectedIndicators: isSuspect
          ? ['Urgency extortion keywords detected', 'Unverified new beneficiary', 'Panic pattern']
          : ['Normal pattern verified'],
        explanation: isSuspect
          ? 'Transfer triggers Aethel AI Digital Arrest Shield. High risk of impersonation fraud.'
          : 'Low risk transaction. Within normal operating profile.',
        customerWarning: isSuspect
          ? 'WARNING: Real police or CBI officers never demand escrow money transfers.'
          : 'No threats detected.',
        recommendedAction: isSuspect ? 'HOLD_FOR_CUSTOMER_VERIFICATION' : 'APPROVE',
        requiresHumanReview: Boolean(isSuspect),
        aiModelUsed: 'Gemini 1.5 Flash (Aethel Defense)',
        createdAt: new Date().toISOString(),
      };
    }

    try {
      const res = await apiClient.post<FraudAssessment>('/api/fraud/assessments', data);
      return res.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock risk assessment fallback:', err);
      return {
        assessmentId: `ASM-FALLBACK-${Date.now()}`,
        transactionReference: `TRF-REF-${Date.now().toString().slice(-4)}`,
        sourceAccountId: data.sourceAccountId || 1,
        destinationAccountId: 999,
        amount: data.amount || 0,
        riskLevel: 'LOW',
        scamDetected: false,
        scamCategories: [],
        confidence: 0.1,
        detectedIndicators: ['Standard low-risk profile'],
        explanation: 'Standard verified payment.',
        customerWarning: 'None',
        recommendedAction: 'APPROVE',
        requiresHumanReview: false,
        aiModelUsed: 'Aethel Neural Heuristics',
        createdAt: new Date().toISOString(),
      };
    }
  },

  // Customer Fraud Alerts
  getAlerts: async (): Promise<FraudAlert[]> => {
    if (isMockSession()) {
      return MOCK_ALERTS;
    }

    try {
      const res = await apiClient.get<FraudAlert[]>('/api/fraud/alerts');
      return res.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock alerts fallback:', err);
      return MOCK_ALERTS;
    }
  },

  markAlertRead: async (id: number): Promise<void> => {
    if (isMockSession()) {
      const alert = MOCK_ALERTS.find((a) => a.id === id);
      if (alert) alert.isRead = true;
      return;
    }

    try {
      await apiClient.patch(`/api/fraud/alerts/${id}/read`);
    } catch (err) {
      console.warn('Backend unavailable, marked mock alert read:', err);
    }
  },

  // Customer Held Transactions
  getHeldTransactions: async (): Promise<HeldTransaction[]> => {
    if (isMockSession()) {
      return MOCK_HELD_TRANSACTIONS;
    }

    try {
      const res = await apiClient.get<HeldTransaction[]>('/api/fraud/held-transactions');
      return res.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock held transactions fallback:', err);
      return MOCK_HELD_TRANSACTIONS;
    }
  },

  getHeldTransactionById: async (id: number): Promise<HeldTransaction> => {
    if (isMockSession()) {
      const match = MOCK_HELD_TRANSACTIONS.find((t) => t.id === id);
      return match || MOCK_HELD_TRANSACTIONS[0];
    }

    try {
      const res = await apiClient.get<HeldTransaction>(`/api/fraud/held-transactions/${id}`);
      return res.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock held transaction fallback:', err);
      const match = MOCK_HELD_TRANSACTIONS.find((t) => t.id === id);
      return match || MOCK_HELD_TRANSACTIONS[0];
    }
  },

  // Verification submission
  verifyTransaction: async (
    id: number,
    answers: Record<string, string>
  ): Promise<HeldTransaction> => {
    if (isMockSession()) {
      const tx = MOCK_HELD_TRANSACTIONS.find((t) => t.id === id) || MOCK_HELD_TRANSACTIONS[0];
      tx.state = 'PENDING_BANK_REVIEW';
      tx.customerVerificationAnswers = JSON.stringify(answers);
      return tx;
    }

    const res = await apiClient.post<HeldTransaction>(
      `/api/fraud/transactions/${id}/verify`,
      { answers }
    );
    return res.data;
  },

  // Cancel held transaction
  cancelTransaction: async (id: number): Promise<HeldTransaction> => {
    if (isMockSession()) {
      const tx = MOCK_HELD_TRANSACTIONS.find((t) => t.id === id) || MOCK_HELD_TRANSACTIONS[0];
      tx.state = 'CANCELLED';
      return tx;
    }

    const res = await apiClient.post<HeldTransaction>(
      `/api/fraud/transactions/${id}/cancel`
    );
    return res.data;
  },

  // Request Bank Review
  requestReview: async (id: number): Promise<HeldTransaction> => {
    if (isMockSession()) {
      const tx = MOCK_HELD_TRANSACTIONS.find((t) => t.id === id) || MOCK_HELD_TRANSACTIONS[0];
      tx.state = 'PENDING_BANK_REVIEW';
      return tx;
    }

    const res = await apiClient.post<HeldTransaction>(
      `/api/fraud/transactions/${id}/request-review`
    );
    return res.data;
  },

  // Admin Monitoring Dashboard
  getAdminMonitoring: async (): Promise<AdminFraudDashboardData> => {
    if (isMockSession()) {
      return MOCK_ADMIN_DASHBOARD;
    }

    try {
      const res = await apiClient.get<AdminFraudDashboardData>(
        '/api/admin/fraud/monitoring'
      );
      return res.data;
    } catch (err) {
      console.warn('Backend unavailable, using mock admin monitoring fallback:', err);
      return MOCK_ADMIN_DASHBOARD;
    }
  },

  // Admin Review action
  reviewTransaction: async (
    id: number,
    request: AdminReviewRequest
  ): Promise<HeldTransaction> => {
    if (isMockSession()) {
      const tx = MOCK_HELD_TRANSACTIONS.find((t) => t.id === id) || MOCK_HELD_TRANSACTIONS[0];
      tx.state = request.action === 'APPROVE' ? 'APPROVED' : 'REJECTED';
      tx.adminReviewNotes = request.notes;
      tx.reviewedBy = 'Marcus Vance (Admin)';
      return tx;
    }

    const res = await apiClient.post<HeldTransaction>(
      `/api/admin/fraud/transactions/${id}/review`,
      request
    );
    return res.data;
  },

  // Contextual Assistant Chat
  chatWithAssistant: async (message: string): Promise<AssistantChatResponse> => {
    if (isMockSession()) {
      return getMockAssistantReply(message);
    }

    try {
      const res = await apiClient.post<AssistantChatResponse>('/api/assistant/chat', {
        message,
      });
      return res.data;
    } catch (err) {
      console.warn('AI Assistant service offline/warming up, using mock copilot fallback:', err);
      return getMockAssistantReply(message);
    }
  },
};
