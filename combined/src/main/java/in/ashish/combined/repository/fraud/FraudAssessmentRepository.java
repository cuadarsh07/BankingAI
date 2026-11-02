package in.ashish.combined.repository.fraud;

import in.ashish.combined.entity.fraud.FraudAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, Long> {
    Optional<FraudAssessment> findByAssessmentId(String assessmentId);
    Optional<FraudAssessment> findByTransactionReference(String transactionReference);
    List<FraudAssessment> findBySourceAccountIdOrderByCreatedAtDesc(Long sourceAccountId);
    List<FraudAssessment> findAllByOrderByCreatedAtDesc();
}

