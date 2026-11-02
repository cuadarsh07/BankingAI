package in.ashish.combined.repository.fraud;

import in.ashish.combined.entity.fraud.TransactionRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRiskRepository extends JpaRepository<TransactionRisk, Long> {
    Optional<TransactionRisk> findByTransactionReference(String transactionReference);
    List<TransactionRisk> findBySourceAccountIdOrderByCreatedAtDesc(Long sourceAccountId);
}

