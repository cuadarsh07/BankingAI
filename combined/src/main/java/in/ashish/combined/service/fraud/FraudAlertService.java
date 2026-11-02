package in.ashish.combined.service.fraud;

import in.ashish.combined.dto.fraud.FraudAlertResponse;
import in.ashish.combined.dto.fraud.FraudAuditLogResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.User;
import in.ashish.combined.entity.fraud.AlertSeverity;
import in.ashish.combined.entity.fraud.FraudAlert;
import in.ashish.combined.entity.fraud.FraudAuditLog;
import in.ashish.combined.repository.fraud.FraudAlertRepository;
import in.ashish.combined.repository.fraud.FraudAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing fraud alerts and immutable audit log records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAuditLogRepository fraudAuditLogRepository;

    @Transactional
    public FraudAlert createAlert(User user, Account account, String txnRef, AlertSeverity severity,
                                  String title, String message, String scamType) {
        FraudAlert alert = FraudAlert.builder()
                .alertId("ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(user)
                .account(account)
                .transactionReference(txnRef)
                .severity(severity)
                .title(title)
                .message(message)
                .scamType(scamType)
                .isRead(false)
                .isDismissed(false)
                .build();

        FraudAlert saved = fraudAlertRepository.save(alert);
        log.info("Created fraud alert {} for user ID {}: [{}] {}", saved.getAlertId(), user.getId(), severity, title);

        recordAuditLog("ALERT_GENERATED", "FraudAlert", saved.getAlertId(),
                "SYSTEM", "RAISE_ALERT", title, severity.name());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<FraudAlertResponse> getAlertsForUser(Long userId) {
        return fraudAlertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FraudAlertResponse> getAllAlerts() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long alertId, Long userId) {
        fraudAlertRepository.findById(alertId).ifPresent(alert -> {
            if (alert.getUser().getId().equals(userId)) {
                alert.setIsRead(true);
                fraudAlertRepository.save(alert);
            }
        });
    }

    @Transactional
    public void dismissAlert(Long alertId, Long userId) {
        fraudAlertRepository.findById(alertId).ifPresent(alert -> {
            if (alert.getUser().getId().equals(userId)) {
                alert.setIsDismissed(true);
                fraudAlertRepository.save(alert);
            }
        });
    }

    @Transactional
    public void recordAuditLog(String eventType, String entityType, String entityId,
                               String performedBy, String action, String details, String riskLevel) {
        FraudAuditLog auditLog = FraudAuditLog.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .action(action)
                .details(details)
                .riskLevel(riskLevel)
                .timestamp(LocalDateTime.now())
                .build();

        fraudAuditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<FraudAuditLogResponse> getRecentAuditLogs() {
        return fraudAuditLogRepository.findTop50ByOrderByTimestampDesc().stream()
                .map(log -> FraudAuditLogResponse.builder()
                        .id(log.getId())
                        .eventType(log.getEventType())
                        .entityType(log.getEntityType())
                        .entityId(log.getEntityId())
                        .performedBy(log.getPerformedBy())
                        .action(log.getAction())
                        .details(log.getDetails())
                        .riskLevel(log.getRiskLevel())
                        .timestamp(log.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    private FraudAlertResponse toResponse(FraudAlert a) {
        return FraudAlertResponse.builder()
                .id(a.getId())
                .alertId(a.getAlertId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .accountId(a.getAccount() != null ? a.getAccount().getId() : null)
                .transactionReference(a.getTransactionReference())
                .severity(a.getSeverity())
                .title(a.getTitle())
                .message(a.getMessage())
                .scamType(a.getScamType())
                .isRead(a.getIsRead())
                .isDismissed(a.getIsDismissed())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

