package in.ashish.combined.entity.fraud;

import in.ashish.combined.entity.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "held_transactions",
        indexes = {
                @Index(name = "idx_held_txn_ref", columnList = "reference_number", unique = true),
                @Index(name = "idx_held_txn_idempotency", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_held_txn_state", columnList = "state, created_at DESC"),
                @Index(name = "idx_held_txn_source", columnList = "source_account_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeldTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 64)
    private String referenceNumber;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counterparty_account_id", nullable = false)
    private Account counterpartyAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "user_explanation", length = 500)
    private String userExplanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 35)
    private HeldTransactionState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "scam_detected", nullable = false)
    private Boolean scamDetected;

    @Column(name = "scam_category", length = 100)
    private String scamCategory;

    @Lob
    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Lob
    @Column(name = "customer_warning", columnDefinition = "TEXT")
    private String customerWarning;

    @Lob
    @Column(name = "verification_questions", columnDefinition = "TEXT")
    private String verificationQuestions;

    @Lob
    @Column(name = "customer_verification_answers", columnDefinition = "TEXT")
    private String customerVerificationAnswers;

    @Lob
    @Column(name = "admin_review_notes", columnDefinition = "TEXT")
    private String adminReviewNotes;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.state == null) {
            this.state = HeldTransactionState.INITIATED;
        }
        if (this.scamDetected == null) {
            this.scamDetected = false;
        }
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusMinutes(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

