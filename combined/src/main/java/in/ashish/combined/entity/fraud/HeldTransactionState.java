package in.ashish.combined.entity.fraud;

public enum HeldTransactionState {
    INITIATED,
    RISK_ASSESSMENT,
    PENDING_CUSTOMER_VERIFICATION,
    PENDING_BANK_REVIEW,
    APPROVED,
    PROCESSING,
    SUCCESS,
    REJECTED,
    CANCELLED,
    EXPIRED
}

