package in.ashish.combined.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing the confirmation of a completed money transfer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    private String transferId;
    private Long sourceAccountId;
    private String sourceAccountNumber;
    private Long destinationAccountId;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private BigDecimal sourceBalanceAfter;
    private String status;
    private String description;
    private LocalDateTime timestamp;
    private String riskLevel;
    private Long heldTransactionId;
    private String warningMessage;
    private Boolean verificationRequired;
    private Boolean requiresAdminReview;
}
