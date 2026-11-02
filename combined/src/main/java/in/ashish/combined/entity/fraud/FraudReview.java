package in.ashish.combined.entity.fraud;

import in.ashish.combined.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fraud_reviews",
        indexes = {
                @Index(name = "idx_fraud_rev_uuid", columnList = "review_id", unique = true),
                @Index(name = "idx_fraud_rev_ref", columnList = "transaction_reference")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false, unique = true, length = 64)
    private String reviewId;

    @Column(name = "transaction_reference", nullable = false, length = 64)
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewed_by_user_id", nullable = false)
    private User reviewedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ReviewAction action;

    @Column(name = "reason", length = 255)
    private String reason;

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
    }
}

