-- =============================================================================
-- PostgreSQL Migration Schema: AI Fraud Detection & Digital Arrest Prevention
-- =============================================================================

CREATE TABLE IF NOT EXISTS transaction_risks (
    id BIGSERIAL PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL,
    source_account_id BIGINT NOT NULL REFERENCES accounts(id),
    destination_account_id BIGINT REFERENCES accounts(id),
    amount NUMERIC(15, 2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    statistical_score NUMERIC(5, 2),
    user_avg_amount NUMERIC(15, 2),
    user_max_amount NUMERIC(15, 2),
    historical_txn_count BIGINT,
    is_first_time_recipient BOOLEAN,
    is_rapid_successive_txn BOOLEAN,
    is_unusual_timing BOOLEAN,
    amount_multiplier_of_avg DOUBLE PRECISION,
    risk_factors VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tx_risk_ref ON transaction_risks(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_tx_risk_account ON transaction_risks(source_account_id, created_at DESC);

CREATE TABLE IF NOT EXISTS fraud_assessments (
    id BIGSERIAL PRIMARY KEY,
    assessment_id VARCHAR(64) NOT NULL UNIQUE,
    transaction_reference VARCHAR(64) NOT NULL,
    source_account_id BIGINT NOT NULL REFERENCES accounts(id),
    destination_account_id BIGINT REFERENCES accounts(id),
    amount NUMERIC(15, 2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    scam_detected BOOLEAN NOT NULL DEFAULT FALSE,
    scam_categories VARCHAR(500),
    confidence NUMERIC(4, 2),
    detected_indicators TEXT,
    explanation TEXT,
    customer_warning TEXT,
    recommended_action VARCHAR(50),
    requires_human_review BOOLEAN NOT NULL DEFAULT FALSE,
    ai_model_used VARCHAR(64),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fraud_assess_uuid ON fraud_assessments(assessment_id);
CREATE INDEX IF NOT EXISTS idx_fraud_assess_ref ON fraud_assessments(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_fraud_assess_account ON fraud_assessments(source_account_id, created_at DESC);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    account_id BIGINT REFERENCES accounts(id),
    transaction_reference VARCHAR(64),
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    scam_type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fraud_alert_user ON fraud_alerts(user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_alert_ref ON fraud_alerts(transaction_reference);

CREATE TABLE IF NOT EXISTS transaction_verifications (
    id BIGSERIAL PRIMARY KEY,
    verification_id VARCHAR(64) NOT NULL UNIQUE,
    transaction_reference VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    questions_asked TEXT,
    answers_provided TEXT,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    fraud_risk_result VARCHAR(20),
    submitted_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tx_verify_uuid ON transaction_verifications(verification_id);
CREATE INDEX IF NOT EXISTS idx_tx_verify_ref ON transaction_verifications(transaction_reference);

CREATE TABLE IF NOT EXISTS fraud_reviews (
    id BIGSERIAL PRIMARY KEY,
    review_id VARCHAR(64) NOT NULL UNIQUE,
    transaction_reference VARCHAR(64) NOT NULL,
    reviewed_by_user_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    notes TEXT,
    ip_address VARCHAR(50),
    reviewed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fraud_rev_uuid ON fraud_reviews(review_id);
CREATE INDEX IF NOT EXISTS idx_fraud_rev_ref ON fraud_reviews(transaction_reference);

CREATE TABLE IF NOT EXISTS fraud_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    risk_level VARCHAR(20),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_entity ON fraud_audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON fraud_audit_logs(timestamp DESC);

CREATE TABLE IF NOT EXISTS held_transactions (
    id BIGSERIAL PRIMARY KEY,
    reference_number VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    source_account_id BIGINT NOT NULL REFERENCES accounts(id),
    counterparty_account_id BIGINT NOT NULL REFERENCES accounts(id),
    amount NUMERIC(15, 2) NOT NULL,
    description VARCHAR(255),
    user_explanation VARCHAR(500),
    state VARCHAR(35) NOT NULL DEFAULT 'INITIATED',
    risk_level VARCHAR(20) NOT NULL,
    scam_detected BOOLEAN NOT NULL DEFAULT FALSE,
    scam_category VARCHAR(100),
    ai_explanation TEXT,
    customer_warning TEXT,
    verification_questions TEXT,
    customer_verification_answers TEXT,
    admin_review_notes TEXT,
    reviewed_by VARCHAR(100),
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_held_txn_ref ON held_transactions(reference_number);
CREATE INDEX IF NOT EXISTS idx_held_txn_idempotency ON held_transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_held_txn_state ON held_transactions(state, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_held_txn_source ON held_transactions(source_account_id, created_at DESC);

