export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type HeldTransactionState =
  | 'INITIATED'
  | 'RISK_ASSESSMENT'
  | 'PENDING_CUSTOMER_VERIFICATION'
  | 'PENDING_BANK_REVIEW'
  | 'APPROVED'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'REJECTED'
  | 'CANCELLED'
  | 'EXPIRED';

export type AlertSeverity = 'INFO' | 'WARNING' | 'HIGH' | 'CRITICAL';

export interface HeldTransaction {
  id: number;
  referenceNumber: string;
  idempotencyKey: string;
  sourceAccountId: number;
  sourceAccountNumber: string;
  sourceAccountHolder?: string;
  destinationAccountId: number;
  destinationAccountNumber: string;
  destinationAccountHolder?: string;
  amount: number;
  description?: string;
  userExplanation?: string;
  state: HeldTransactionState;
  riskLevel: RiskLevel;
  scamDetected: boolean;
  scamCategory?: string;
  aiExplanation?: string;
  customerWarning?: string;
  verificationQuestions: string[];
  customerVerificationAnswers?: string;
  adminReviewNotes?: string;
  reviewedBy?: string;
  expiresAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface FraudAlert {
  id: number;
  alertId: string;
  userId: number;
  accountId?: number;
  transactionReference?: string;
  severity: AlertSeverity;
  title: string;
  message: string;
  scamType?: string;
  isRead: boolean;
  isDismissed: boolean;
  createdAt: string;
}

export interface FraudAssessment {
  assessmentId: string;
  transactionReference: string;
  sourceAccountId: number;
  destinationAccountId: number;
  amount: number;
  riskLevel: RiskLevel;
  scamDetected: boolean;
  scamCategories: string[];
  confidence: number;
  detectedIndicators: string[];
  explanation: string;
  customerWarning: string;
  recommendedAction: string;
  requiresHumanReview: boolean;
  verificationQuestions?: string[];
  onHold?: boolean;
  heldTransactionId?: number;
  aiModelUsed: string;
  createdAt: string;
}

export interface FraudAuditLog {
  id: number;
  eventType: string;
  entityType: string;
  entityId: string;
  performedBy: string;
  action: string;
  details: string;
  riskLevel?: string;
  timestamp: string;
}

export interface AdminFraudDashboardData {
  totalFlaggedTransactions: number;
  pendingReviewCount: number;
  lowRiskCount: number;
  mediumRiskCount: number;
  highRiskCount: number;
  criticalRiskCount: number;
  scamDetectedCount: number;
  heldTransactions: HeldTransaction[];
  recentAlerts: FraudAlert[];
  recentAuditLogs: FraudAuditLog[];
}

export interface AssistantChatResponse {
  reply: string;
  timestamp: string;
  suggestedActions: string[];
  isSecurityAlert: boolean;
}

