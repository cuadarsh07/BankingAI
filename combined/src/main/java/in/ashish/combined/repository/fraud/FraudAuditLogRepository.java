package in.ashish.combined.repository.fraud;

import in.ashish.combined.entity.fraud.FraudAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAuditLogRepository extends JpaRepository<FraudAuditLog, Long> {
    List<FraudAuditLog> findByEntityIdOrderByTimestampDesc(String entityId);
    List<FraudAuditLog> findTop50ByOrderByTimestampDesc();
}

