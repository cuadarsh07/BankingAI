import {
  AccountResponse,
  TransactionRecord,
  PageResponse,
  HeldTransaction,
  FraudAlert,
  AdminFraudDashboardData,
  AssistantChatResponse,
  TransactionQueryFilters,
  UserSession,
} from '@/types';

export const MOCK_USER_CUSTOMER: UserSession = {
  token: 'demo-jwt-customer-offline-token',
  tokenType: 'Bearer',
  username: 'alice@example.com',
  fullName: 'Alice Spender',
  role: 'ROLE_CUSTOMER',
  accountId: 1,
  accountNumber: '1000000001',
};

export const MOCK_USER_ADMIN: UserSession = {
  token: 'demo-jwt-admin-offline-token',
  tokenType: 'Bearer',
  username: 'admin@example.com',
  fullName: 'Marcus Vance',
  role: 'ROLE_ADMIN',
  accountId: null,
  accountNumber: null,
};

export const MOCK_ALICE_ACCOUNT: AccountResponse = {
  id: 1,
  accountNumber: '1000000001',
  accountHolderName: 'Alice Spender',
  email: 'alice@example.com',
  balance: 31634.93,
  status: 'ACTIVE',
  createdAt: '2024-01-15T09:00:00Z',
  updatedAt: new Date().toISOString(),
};

export const MOCK_BOB_ACCOUNT: AccountResponse = {
  id: 2,
  accountNumber: '1000000002',
  accountHolderName: 'Bob Builder',
  email: 'bob@example.com',
  balance: 145200.00,
  status: 'ACTIVE',
  createdAt: '2024-02-10T11:30:00Z',
  updatedAt: new Date().toISOString(),
};

export const MOCK_CHARLIE_ACCOUNT: AccountResponse = {
  id: 3,
  accountNumber: '1000000003',
  accountHolderName: 'Charlie Brown',
  email: 'charlie@example.com',
  balance: 1071665.07,
  status: 'ACTIVE',
  createdAt: '2024-03-01T14:15:00Z',
  updatedAt: new Date().toISOString(),
};

export const MOCK_ACCOUNTS: AccountResponse[] = [
  MOCK_ALICE_ACCOUNT,
  MOCK_BOB_ACCOUNT,
  MOCK_CHARLIE_ACCOUNT,
];

export const MOCK_ALICE_TRANSACTIONS: TransactionRecord[] = [
  {
    id: 501,
    referenceNumber: 'TXN-GRC-6623',
    accountId: 1,
    accountNumber: '1000000001',
    counterpartyAccountId: null,
    counterpartyAccountNumber: null,
    type: 'WITHDRAWAL',
    amount: 5500.00,
    balanceAfter: 31634.93,
    description: "Nature's Basket Supermarket - Groceries & Essentials",
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString(), // 4 hrs ago
  },
  {
    id: 502,
    referenceNumber: 'TXN-PWR-1290',
    accountId: 1,
    accountNumber: '1000000001',
    counterpartyAccountId: null,
    counterpartyAccountNumber: null,
    type: 'WITHDRAWAL',
    amount: 8500.00,
    balanceAfter: 37134.93,
    description: 'Tata Power Utility Bill Payment (Auto-Pay)',
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 28).toISOString(), // 1 day ago
  },
  {
    id: 503,
    referenceNumber: 'TXN-AMZ-7731',
    accountId: 1,
    accountNumber: '1000000001',
    counterpartyAccountId: null,
    counterpartyAccountNumber: null,
    type: 'WITHDRAWAL',
    amount: 14299.00,
    balanceAfter: 45634.93,
    description: 'Amazon Electronics - 4K Monitor & Ergonomic Desk Accessories',
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 72).toISOString(), // 3 days ago
  },
  {
    id: 504,
    referenceNumber: 'TXN-RNT-4412',
    accountId: 1,
    accountNumber: '1000000001',
    counterpartyAccountId: 2,
    counterpartyAccountNumber: '1000000002',
    type: 'TRANSFER_OUT',
    amount: 25000.00,
    balanceAfter: 59933.93,
    description: 'Monthly Apartment Rent Transfer to Landlord',
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 120).toISOString(), // 5 days ago
  },
  {
    id: 505,
    referenceNumber: 'TXN-SAL-9821',
    accountId: 1,
    accountNumber: '1000000001',
    counterpartyAccountId: null,
    counterpartyAccountNumber: null,
    type: 'DEPOSIT',
    amount: 75000.00,
    balanceAfter: 84933.93,
    description: 'Tech Innovations Private Ltd - Monthly Salary Credit',
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 168).toISOString(), // 7 days ago
  },
  {
    id: 506,
    referenceNumber: 'TXN-UPI-3309',
    accountId: 1,
    accountNumber: '1000000001',
    counterpartyAccountId: 2,
    counterpartyAccountNumber: '1000000002',
    type: 'TRANSFER_IN',
    amount: 3200.00,
    balanceAfter: 9933.93,
    description: 'UPI Settlement from Bob Builder',
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 240).toISOString(), // 10 days ago
  },
];

export const MOCK_HELD_TRANSACTIONS: HeldTransaction[] = [
  {
    id: 101,
    referenceNumber: 'HLD-9981-SEC',
    idempotencyKey: 'idem-mock-101',
    sourceAccountId: 1,
    sourceAccountNumber: '1000000001',
    sourceAccountHolder: 'Alice Spender',
    destinationAccountId: 999,
    destinationAccountNumber: '9900004412',
    destinationAccountHolder: 'CBI Impersonator Fake Reserve',
    amount: 50000.00,
    description: 'Urgent Police Clearing Escrow Transfer',
    userExplanation: 'Instructed by Skype caller identifying as Delhi Police Cyber Cell',
    state: 'PENDING_CUSTOMER_VERIFICATION',
    riskLevel: 'CRITICAL',
    scamDetected: true,
    scamCategory: 'DIGITAL_ARREST',
    aiExplanation:
      'High-confidence Digital Arrest Scam pattern: Transfer initiated following coercion keywords ("police", "cyber cell", "court order") towards an untrusted newly-created mule account. Immediate safeguard lock applied.',
    customerWarning:
      'CAUTION: Law enforcement, CBI, and RBI officials NEVER conduct video call court hearings or demand escrow transfers. This is a fraudulent syndicate attempting extortion.',
    verificationQuestions: [
      'Did someone contacting you via Skype/WhatsApp claim you are implicated in money laundering or narcotics customs seizures?',
      'Were you told that transferring funds into an "official reserve account" is mandatory for identity verification?',
      'Are you on a call with this person right now instructing you what to say to the bank?',
    ],
    customerVerificationAnswers: undefined,
    adminReviewNotes: undefined,
    reviewedBy: undefined,
    expiresAt: new Date(Date.now() + 1000 * 60 * 60 * 24).toISOString(),
    createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

export const MOCK_ALERTS: FraudAlert[] = [
  {
    id: 1,
    alertId: 'ALT-101-SEC',
    userId: 1,
    accountId: 1,
    transactionReference: 'HLD-9981-SEC',
    severity: 'CRITICAL',
    title: 'Digital Arrest Threat Intercepted',
    message:
      'A suspicious outbound transfer of ₹50,000.00 was immediately intercepted. High risk of extortion syndicate detected.',
    scamType: 'DIGITAL_ARREST',
    isRead: false,
    isDismissed: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
  },
  {
    id: 2,
    alertId: 'ALT-102-SYS',
    userId: 1,
    accountId: 1,
    transactionReference: 'TXN-AMZ-7731',
    severity: 'INFO',
    title: 'Biometric Login Verified',
    message: 'Authorized banking session accessed via encrypted hardware token.',
    scamType: undefined,
    isRead: true,
    isDismissed: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 72).toISOString(),
  },
];

export const MOCK_ADMIN_DASHBOARD: AdminFraudDashboardData = {
  totalFlaggedTransactions: 14,
  pendingReviewCount: 1,
  lowRiskCount: 8,
  mediumRiskCount: 3,
  highRiskCount: 2,
  criticalRiskCount: 1,
  scamDetectedCount: 3,
  heldTransactions: MOCK_HELD_TRANSACTIONS,
  recentAlerts: MOCK_ALERTS,
  recentAuditLogs: [
    {
      id: 901,
      eventType: 'FRAUD_FLAGGED',
      entityType: 'TRANSACTION',
      entityId: 'HLD-9981-SEC',
      performedBy: 'Aethel AI Defense Engine',
      action: 'HOLD_INTERCEPT',
      details: 'Risk score 0.94 triggered instant safeguard hold on ₹50,000.00 outbound payment',
      riskLevel: 'CRITICAL',
      timestamp: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
    },
    {
      id: 902,
      eventType: 'ACCOUNT_AUDIT',
      entityType: 'ACCOUNT',
      entityId: '1000000001',
      performedBy: 'System Cron',
      action: 'INTEGRITY_CHECK',
      details: 'Pessimistic locking and balance parity verified: 100% compliant',
      riskLevel: 'LOW',
      timestamp: new Date(Date.now() - 1000 * 60 * 180).toISOString(),
    },
  ],
};

export const getMockTransactionsPage = (
  filters: TransactionQueryFilters = {}
): PageResponse<TransactionRecord> => {
  let list = [...MOCK_ALICE_TRANSACTIONS];

  if (filters.type) {
    list = list.filter((t) => t.type === filters.type);
  }

  const page = filters.page ?? 0;
  const size = filters.size ?? 10;
  const start = page * size;
  const paginated = list.slice(start, start + size);

  return {
    content: paginated,
    pageNumber: page,
    pageSize: size,
    totalElements: list.length,
    totalPages: Math.max(1, Math.ceil(list.length / size)),
    first: page === 0,
    last: start + size >= list.length,
    empty: paginated.length === 0,
  };
};

export const getMockAssistantReply = (message: string): AssistantChatResponse => {
  const lower = message.toLowerCase();

  if (lower.includes('digital arrest') || lower.includes('arrest') || lower.includes('police') || lower.includes('cbi')) {
    return {
      reply:
        '⚠️ CRITICAL SECURITY WARNING: Digital Arrest is a major cyber-fraud scam. No Indian law enforcement agency (Police, CBI, ED, Customs, or RBI) will EVER hold online video hearings on Skype or WhatsApp, nor will they demand you transfer money to a "safety escrow account" for clearance. If anyone is threatening you with an arrest warrant over video or call, immediately disconnect and report to the National Cyber Crime Helpline at 1930 or cybercrime.gov.in.',
      timestamp: new Date().toISOString(),
      suggestedActions: [
        'How to verify legitimate police communications?',
        'What should I do if I already sent money?',
        'Show my currently held transactions',
      ],
      isSecurityAlert: true,
    };
  }

  if (lower.includes('hold') || lower.includes('held') || lower.includes('pending') || lower.includes('stuck')) {
    return {
      reply:
        'Your transfer of ₹50,000.00 (HLD-9981-SEC) is currently placed on temporary protective hold under Aethel Bank\'s AI Fraud Defense System. Our neural model detected high-risk scam indicators. You can review the security questions on your Security Center page or request an immediate manual compliance officer review.',
      timestamp: new Date().toISOString(),
      suggestedActions: [
        'Go to Held Transactions',
        'Request Bank Review',
        'Cancel the held transfer',
      ],
      isSecurityAlert: true,
    };
  }

  if (lower.includes('balance') || lower.includes('how much') || lower.includes('money')) {
    return {
      reply: `Your total available balance in account 1000000001 (Aethel Black Tier) is ₹31,634.93. All ledger records are cryptographically verified with zero discrepancies.`,
      timestamp: new Date().toISOString(),
      suggestedActions: [
        'View recent transactions',
        'Send money via secure transfer',
        'Deposit funds',
      ],
      isSecurityAlert: false,
    };
  }

  return {
    reply: `I am your Aethel Banking AI Security Copilot. I analyze all transaction activity in real time, guard against coercive fraud like Digital Arrest, explain held transfers, and assist with your account. How can I assist you further?`,
    timestamp: new Date().toISOString(),
    suggestedActions: [
      'What is a Digital Arrest scam?',
      'Why is my transfer on hold?',
      'What are my daily transfer limits?',
    ],
    isSecurityAlert: false,
  };
};

