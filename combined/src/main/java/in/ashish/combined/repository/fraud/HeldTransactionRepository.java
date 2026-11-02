package in.ashish.combined.repository.fraud;

import in.ashish.combined.entity.fraud.HeldTransaction;
import in.ashish.combined.entity.fraud.HeldTransactionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HeldTransactionRepository extends JpaRepository<HeldTransaction, Long> {
    Optional<HeldTransaction> findByReferenceNumber(String referenceNumber);
    Optional<HeldTransaction> findByIdempotencyKey(String idempotencyKey);
    List<HeldTransaction> findBySourceAccountIdOrderByCreatedAtDesc(Long sourceAccountId);
    List<HeldTransaction> findByStateInOrderByCreatedAtDesc(List<HeldTransactionState> states);
    List<HeldTransaction> findAllByOrderByCreatedAtDesc();

    @Query("SELECT h FROM HeldTransaction h WHERE h.state IN ('PENDING_CUSTOMER_VERIFICATION', 'PENDING_BANK_REVIEW') AND h.expiresAt < :now")
    List<HeldTransaction> findExpiredHeldTransactions(LocalDateTime now);
}

