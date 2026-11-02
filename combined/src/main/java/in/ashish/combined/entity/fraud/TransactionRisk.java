package in.ashish.combined.entity.fraud;

import in.ashish.combined.entity.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transaction_risks",
        indexes = {
                @Index(name = "idx_tx_risk_ref", columnList = "transaction_reference"),
                @Index(name = "idx_tx_risk_account", columnList = "source_account_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "statistical_score")
    private Double statisticalScore;

    @Column(name = "user_avg_amount", precision = 15, scale = 2)
    private BigDecimal userAvgAmount;

    @Column(name = "user_max_amount", precision = 15, scale = 2)
    private BigDecimal userMaxAmount;

    @Column(name = "historical_txn_count")
    private Long historicalTxnCount;

    @Column(name = "is_first_time_recipient")
    private Boolean isFirstTimeRecipient;

    @Column(name = "is_rapid_successive_txn")
    private Boolean isRapidSuccessiveTxn;

    @Column(name = "is_unusual_timing")
    private Boolean isUnusualTiming;

    @Column(name = "amount_multiplier_of_avg")
    private Double amountMultiplierOfAvg;

    @Column(name = "risk_factors", length = 1000)
    private String riskFactors;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
