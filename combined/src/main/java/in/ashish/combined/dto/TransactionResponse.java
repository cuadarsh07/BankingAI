package in.ashish.combined.dto;

import in.ashish.combined.entity.AccountStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO returned after a successful deposit or withdrawal transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private String transactionType; // DEPOSIT or WITHDRAWAL
    private Long accountId;
    private String accountNumber;
    private BigDecimal transactionAmount;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private AccountStatus accountStatus;
    private String description;
    private LocalDateTime timestamp;
}
