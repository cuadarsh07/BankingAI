package in.ashish.combined.dto.fraud;

import in.ashish.combined.entity.fraud.AlertSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertResponse {
    private Long id;
    private String alertId;
    private Long userId;
    private Long accountId;
    private String transactionReference;
    private AlertSeverity severity;
    private String title;
    private String message;
    private String scamType;
    private boolean isRead;
    private boolean isDismissed;
    private LocalDateTime createdAt;
}

