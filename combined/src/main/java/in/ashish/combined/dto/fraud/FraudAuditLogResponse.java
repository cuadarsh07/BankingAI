package in.ashish.combined.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAuditLogResponse {
    private Long id;
    private String eventType;
    private String entityType;
    private String entityId;
    private String performedBy;
    private String action;
    private String details;
    private String riskLevel;
    private LocalDateTime timestamp;
}

