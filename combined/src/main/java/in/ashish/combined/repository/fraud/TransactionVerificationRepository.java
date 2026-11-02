package in.ashish.combined.repository.fraud;

import in.ashish.combined.entity.fraud.TransactionVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionVerificationRepository extends JpaRepository<TransactionVerification, Long> {
    Optional<TransactionVerification> findByVerificationId(String verificationId);
    Optional<TransactionVerification> findByTransactionReference(String transactionReference);
}

