package in.ashish.combined.dto.fraud;

import in.ashish.combined.entity.fraud.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAssessmentResponse {
    private String assessmentId;
    private String transactionReference;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private RiskLevel riskLevel;
    private boolean scamDetected;
    private List<String> scamCategories;
    private double confidence;
    private List<String> detectedIndicators;
    private String explanation;
    private String customerWarning;
    private String recommendedAction;
    private boolean requiresHumanReview;
    private List<String> verificationQuestions;
    private boolean onHold;
    private Long heldTransactionId;
    private String aiModelUsed;
    private LocalDateTime createdAt;
}

