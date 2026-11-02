package in.ashish.combined.repository;

import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository interface for Transaction persistence, filtering, and pagination.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    Page<Transaction> findByAccountIdAndType(Long accountId, TransactionType type, Pageable pageable);

    @Query(value = "SELECT t FROM Transaction t " +
                   "JOIN FETCH t.account a " +
                   "LEFT JOIN FETCH t.counterpartyAccount cp " +
                   "WHERE a.id = :accountId " +
                   "AND (:type IS NULL OR t.type = :type) " +
                   "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
                   "AND (:endDate IS NULL OR t.createdAt <= :endDate)",
           countQuery = "SELECT COUNT(t) FROM Transaction t " +
                        "WHERE t.account.id = :accountId " +
                        "AND (:type IS NULL OR t.type = :type) " +
                        "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
                        "AND (:endDate IS NULL OR t.createdAt <= :endDate)")
    Page<Transaction> findTransactionsWithFilters(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.account.id = :accountId AND t.counterpartyAccount.id = :counterpartyAccountId AND t.type = 'TRANSFER_OUT'")
    boolean hasTransferredToCounterpartyBefore(
            @Param("accountId") Long accountId,
            @Param("counterpartyAccountId") Long counterpartyAccountId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId AND t.type = 'TRANSFER_OUT' AND t.createdAt >= :afterTime")
    long countRecentTransfers(
            @Param("accountId") Long accountId,
            @Param("afterTime") LocalDateTime afterTime);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.type = 'TRANSFER_OUT' AND t.createdAt >= :afterTime")
    java.math.BigDecimal sumRecentTransferAmount(
            @Param("accountId") Long accountId,
            @Param("afterTime") LocalDateTime afterTime);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.type = 'TRANSFER_OUT' ORDER BY t.createdAt DESC")
    java.util.List<Transaction> findHistoricalTransfers(@Param("accountId") Long accountId, Pageable pageable);
}
