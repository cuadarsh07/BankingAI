package in.ashish.combined.entity.fraud;

import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fraud_alerts",
        indexes = {
                @Index(name = "idx_fraud_alert_user", columnList = "user_id, is_read, created_at DESC"),
                @Index(name = "idx_fraud_alert_ref", columnList = "transaction_reference")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true, length = 64)
    private String alertId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "transaction_reference", length = 64)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "scam_type", length = 50)
    private String scamType;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "is_dismissed", nullable = false)
    private Boolean isDismissed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
        if (this.isDismissed == null) {
            this.isDismissed = false;
        }
    }
}

