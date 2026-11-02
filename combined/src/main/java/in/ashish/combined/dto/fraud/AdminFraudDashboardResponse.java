package in.ashish.combined.dto.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFraudDashboardResponse {
    private long totalFlaggedTransactions;
    private long pendingReviewCount;
    private long lowRiskCount;
    private long mediumRiskCount;
    private long highRiskCount;
    private long criticalRiskCount;
    private long scamDetectedCount;
    private List<HeldTransactionResponse> heldTransactions;
    private List<FraudAlertResponse> recentAlerts;
    private List<FraudAuditLogResponse> recentAuditLogs;
}

