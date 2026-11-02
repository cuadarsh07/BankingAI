package in.ashish.combined.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Entity representing audit ledger records in the database.
 *
 * Interview Concepts:
 * - ManyToOne with FetchType.LAZY: Avoids eager loading of parent accounts, preventing
 *   the notorious N+1 query problem when listing transaction pages.
 * - Compound Indexes: @Index(name = "idx_account_created", columnList = "account_id, created_at DESC")
 *   allows MySQL/H2 to jump directly to an account's recent transactions without a full-table scan.
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_txn_account_created", columnList = "account_id, created_at DESC"),
                @Index(name = "idx_txn_reference", columnList = "reference_number", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 36)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_account_id")
    private Account counterpartyAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
