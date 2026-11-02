package in.ashish.combined.entity.fraud;

import in.ashish.combined.entity.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "fraud_assessments",
        indexes = {
                @Index(name = "idx_fraud_assess_uuid", columnList = "assessment_id", unique = true),
                @Index(name = "idx_fraud_assess_ref", columnList = "transaction_reference"),
                @Index(name = "idx_fraud_assess_account", columnList = "source_account_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false, unique = true, length = 64)
    private String assessmentId;

    @Column(name = "transaction_reference", nullable = false, length = 64)
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "scam_detected", nullable = false)
    private Boolean scamDetected;

    @Column(name = "scam_categories", length = 500)
    private String scamCategories;

    @Column(name = "confidence")
    private Double confidence;

    @Lob
    @Column(name = "detected_indicators", columnDefinition = "TEXT")
    private String detectedIndicators;

    @Lob
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Lob
    @Column(name = "customer_warning", columnDefinition = "TEXT")
    private String customerWarning;

    @Column(name = "recommended_action", length = 50)
    private String recommendedAction;

    @Column(name = "requires_human_review", nullable = false)
    private Boolean requiresHumanReview;

    @Column(name = "ai_model_used", length = 64)
    private String aiModelUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.scamDetected == null) {
            this.scamDetected = false;
        }
        if (this.requiresHumanReview == null) {
            this.requiresHumanReview = false;
        }
    }
}
