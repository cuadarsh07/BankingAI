package in.ashish.combined.service.fraud;

import in.ashish.combined.dto.fraud.TransactionRiskDto;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.entity.fraud.RiskLevel;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.repository.fraud.TransactionRiskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionRiskServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionRiskRepository transactionRiskRepository;

    @InjectMocks
    private TransactionRiskService transactionRiskService;

    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC-1001")
                .accountHolderName("Ashish Kumar")
                .balance(new BigDecimal("100000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        destAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC-2002")
                .accountHolderName("Rahul Sharma")
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Normal transaction conforming to past behavior is LOW risk")
    void testNormalTransactionLowRisk() {
        // Setup 5 historical transactions of ~₹3,000 each
        List<Transaction> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            history.add(Transaction.builder()
                    .amount(new BigDecimal("3000.00"))
                    .type(TransactionType.TRANSFER_OUT)
                    .createdAt(LocalDateTime.now().minusDays(i + 1))
                    .build());
        }

        when(transactionRepository.findHistoricalTransfers(eq(1L), any(Pageable.class)))
                .thenReturn(history);
        when(transactionRepository.hasTransferredToCounterpartyBefore(1L, 2L))
                .thenReturn(true);
        when(transactionRepository.countRecentTransfers(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        TransactionRiskDto risk = transactionRiskService.evaluateRisk(
                sourceAccount, destAccount, new BigDecimal("3200.00"));

        assertEquals(RiskLevel.LOW, risk.getRiskLevel());
        assertFalse(risk.isFirstTimeRecipient());
        assertFalse(risk.isRapidSuccessiveTxn());
    }

    @Test
    @DisplayName("Unusually large transaction deviating from user average triggers HIGH or CRITICAL risk")
    void testUnusuallyLargeTransaction() {
        // User typically transfers ₹2,000 - ₹5,000 (avg ₹3,000), but sends ₹5,00,000
        List<Transaction> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            history.add(Transaction.builder()
                    .amount(new BigDecimal("3000.00"))
                    .type(TransactionType.TRANSFER_OUT)
                    .createdAt(LocalDateTime.now().minusDays(i + 1))
                    .build());
        }

        when(transactionRepository.findHistoricalTransfers(eq(1L), any(Pageable.class)))
                .thenReturn(history);
        when(transactionRepository.hasTransferredToCounterpartyBefore(1L, 2L))
                .thenReturn(false);
        when(transactionRepository.countRecentTransfers(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        TransactionRiskDto risk = transactionRiskService.evaluateRisk(
                sourceAccount, destAccount, new BigDecimal("500000.00"));

        assertTrue(risk.getRiskLevel() == RiskLevel.HIGH || risk.getRiskLevel() == RiskLevel.CRITICAL);
        assertTrue(risk.isFirstTimeRecipient());
        assertTrue(risk.getAmountMultiplierOfAvg() > 10.0);
    }

    @Test
    @DisplayName("First time recipient triggers elevated risk signals")
    void testFirstTimeRecipient() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            history.add(Transaction.builder()
                    .amount(new BigDecimal("5000.00"))
                    .type(TransactionType.TRANSFER_OUT)
                    .createdAt(LocalDateTime.now().minusDays(i + 1))
                    .build());
        }

        when(transactionRepository.findHistoricalTransfers(eq(1L), any(Pageable.class)))
                .thenReturn(history);
        when(transactionRepository.hasTransferredToCounterpartyBefore(1L, 2L))
                .thenReturn(false); // First time
        when(transactionRepository.countRecentTransfers(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        TransactionRiskDto risk = transactionRiskService.evaluateRisk(
                sourceAccount, destAccount, new BigDecimal("5000.00"));

        assertTrue(risk.isFirstTimeRecipient());
        assertTrue(risk.getStatisticalScore() >= 30.0);
    }

    @Test
    @DisplayName("Rapid successive transfers (multiple transfers in 10 minutes) triggers elevated risk")
    void testRapidSuccessiveTransfers() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            history.add(Transaction.builder()
                    .amount(new BigDecimal("5000.00"))
                    .type(TransactionType.TRANSFER_OUT)
                    .createdAt(LocalDateTime.now().minusDays(i + 1))
                    .build());
        }

        when(transactionRepository.findHistoricalTransfers(eq(1L), any(Pageable.class)))
                .thenReturn(history);
        when(transactionRepository.hasTransferredToCounterpartyBefore(1L, 2L))
                .thenReturn(true);
        when(transactionRepository.countRecentTransfers(eq(1L), any(LocalDateTime.class)))
                .thenReturn(4L); // 4 transfers in past 10 minutes

        TransactionRiskDto risk = transactionRiskService.evaluateRisk(
                sourceAccount, destAccount, new BigDecimal("10000.00"));

        assertTrue(risk.isRapidSuccessiveTxn());
        assertTrue(risk.getStatisticalScore() >= 40.0);
    }

    @Test
    @DisplayName("New user with insufficient history uses safe fallback threshold rules")
    void testNewUserFallbackRules() {
        // Less than 3 transactions in history
        when(transactionRepository.findHistoricalTransfers(eq(1L), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        TransactionRiskDto smallTxn = transactionRiskService.evaluateRisk(
                sourceAccount, destAccount, new BigDecimal("5000.00"));
        assertEquals(RiskLevel.LOW, smallTxn.getRiskLevel());

        TransactionRiskDto highTxn = transactionRiskService.evaluateRisk(
                sourceAccount, destAccount, new BigDecimal("75000.00"));
        assertEquals(RiskLevel.HIGH, highTxn.getRiskLevel());

        TransactionRiskDto critTxn = transactionRiskService.evaluateRisk(
                sourceAccount, destAccount, new BigDecimal("250000.00"));
        assertEquals(RiskLevel.CRITICAL, critTxn.getRiskLevel());
    }
}

