package in.ashish.combined.dto;

import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing an individual ledger record in account transaction history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecordResponse {

    private Long id;
    private String referenceNumber;
    private Long accountId;
    private String accountNumber;
    private Long counterpartyAccountId;
    private String counterpartyAccountNumber;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public static TransactionRecordResponse fromEntity(Transaction transaction) {
        Long counterpartyId = null;
        String counterpartyAccNo = null;

        if (transaction.getCounterpartyAccount() != null) {
            counterpartyId = transaction.getCounterpartyAccount().getId();
            counterpartyAccNo = transaction.getCounterpartyAccount().getAccountNumber();
        }

        return TransactionRecordResponse.builder()
                .id(transaction.getId())
                .referenceNumber(transaction.getReferenceNumber())
                .accountId(transaction.getAccount().getId())
                .accountNumber(transaction.getAccount().getAccountNumber())
                .counterpartyAccountId(counterpartyId)
                .counterpartyAccountNumber(counterpartyAccNo)
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
