package in.ashish.combined.service;

import in.ashish.combined.dto.PageResponse;
import in.ashish.combined.dto.TransactionRecordResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account sampleAccount;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("1001234567")
                .accountHolderName("John Doe")
                .email("john@example.com")
                .balance(new BigDecimal("1500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        sampleTransaction = Transaction.builder()
                .id(101L)
                .referenceNumber("TXN-12345678")
                .account(sampleAccount)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .description("Initial deposit")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should return paginated transaction history when account exists")
    void getAccountTransactions_success() {
        when(accountRepository.existsById(1L)).thenReturn(true);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(sampleTransaction), pageable, 1);

        when(transactionRepository.findTransactionsWithFilters(eq(1L), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        PageResponse<TransactionRecordResponse> result = transactionService.getAccountTransactions(
                1L, null, null, null, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("TXN-12345678", result.getContent().get(0).getReferenceNumber());
        assertEquals(TransactionType.DEPOSIT, result.getContent().get(0).getType());
        assertEquals(new BigDecimal("500.00"), result.getContent().get(0).getAmount());

        verify(accountRepository, times(1)).existsById(1L);
        verify(transactionRepository, times(1)).findTransactionsWithFilters(eq(1L), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account does not exist")
    void getAccountTransactions_accountNotFound() {
        when(accountRepository.existsById(99L)).thenReturn(false);

        Pageable pageable = PageRequest.of(0, 10);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.getAccountTransactions(99L, null, null, null, pageable)
        );

        assertTrue(exception.getMessage().contains("Account not found with ID: 99"));
        verify(transactionRepository, never()).findTransactionsWithFilters(any(), any(), any(), any(), any());
    }
}
