package in.ashish.combined.repository.fraud;

import in.ashish.combined.entity.fraud.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    Optional<FraudAlert> findByAlertId(String alertId);
    List<FraudAlert> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<FraudAlert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    List<FraudAlert> findAllByOrderByCreatedAtDesc();
}

