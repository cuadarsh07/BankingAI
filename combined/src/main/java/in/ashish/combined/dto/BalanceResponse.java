package in.ashish.combined.dto;

import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dedicated DTO for checking account balance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    private Long accountId;
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime checkedAt;

    public static BalanceResponse fromEntity(Account account) {
        return BalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .balance(account.getBalance())
                .status(account.getStatus())
                .checkedAt(LocalDateTime.now())
                .build();
    }
}
