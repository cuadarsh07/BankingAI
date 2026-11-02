package in.ashish.combined.repository.fraud;

import in.ashish.combined.entity.fraud.FraudReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudReviewRepository extends JpaRepository<FraudReview, Long> {
    Optional<FraudReview> findByReviewId(String reviewId);
    List<FraudReview> findByTransactionReferenceOrderByReviewedAtDesc(String transactionReference);
    List<FraudReview> findAllByOrderByReviewedAtDesc();
}

