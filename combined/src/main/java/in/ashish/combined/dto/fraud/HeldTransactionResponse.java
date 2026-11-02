package in.ashish.combined.dto.fraud;

import in.ashish.combined.entity.fraud.HeldTransactionState;
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
public class HeldTransactionResponse {
    private Long id;
    private String referenceNumber;
    private String idempotencyKey;
    private Long sourceAccountId;
    private String sourceAccountNumber;
    private String sourceAccountHolder;
    private Long destinationAccountId;
    private String destinationAccountNumber;
    private String destinationAccountHolder;
    private BigDecimal amount;
    private String description;
    private String userExplanation;
    private HeldTransactionState state;
    private RiskLevel riskLevel;
    private boolean scamDetected;
    private String scamCategory;
    private String aiExplanation;
    private String customerWarning;
    private List<String> verificationQuestions;
    private String customerVerificationAnswers;
    private String adminReviewNotes;
    private String reviewedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

