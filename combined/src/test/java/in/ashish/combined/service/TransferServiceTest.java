package in.ashish.combined.service;

import in.ashish.combined.dto.TransferRequest;
import in.ashish.combined.dto.TransferResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.exception.AccountInactiveException;
import in.ashish.combined.exception.InsufficientBalanceException;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransferServiceImpl using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Account sourceAccount;
    private Account destinationAccount;
    private TransferRequest validRequest;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1001234567")
                .accountHolderName("Source User")
                .email("source@example.com")
                .balance(new BigDecimal("3000.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        destinationAccount = Account.builder()
                .id(2L)
                .accountNumber("2001234567")
                .accountHolderName("Destination User")
                .email("dest@example.com")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("500.00"))
                .description("Rent Payment")
                .build();
    }

    @Test
    @DisplayName("Should successfully transfer funds between two active accounts")
    void transferMoney_Success() {
        // Arrange: firstLockId = 1L, secondLockId = 2L
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(destinationAccount));

        // Act
        TransferResponse response = transferService.transferMoney(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("500.00"), response.getAmount());
        assertEquals(new BigDecimal("2500.00"), response.getSourceBalanceAfter());
        assertEquals(new BigDecimal("2500.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("1500.00"), destinationAccount.getBalance());

        verify(accountRepository, times(1)).save(sourceAccount);
        verify(accountRepository, times(1)).save(destinationAccount);
    }

    @Test
    @DisplayName("Should throw InvalidTransferException when transferring to same account")
    void transferMoney_SameAccount_ThrowsInvalidTransferException() {
        // Arrange
        TransferRequest sameAccountReq = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(1L)
                .amount(new BigDecimal("100.00"))
                .build();

        // Act & Assert
        InvalidTransferException exception = assertThrows(
                InvalidTransferException.class,
                () -> transferService.transferMoney(sameAccountReq)
        );

        assertTrue(exception.getMessage().contains("same account"));
        verify(accountRepository, never()).findByIdWithLock(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when source balance is inadequate")
    void transferMoney_InsufficientFunds_ThrowsInsufficientBalanceException() {
        // Arrange
        validRequest.setAmount(new BigDecimal("10000.00")); // Exceeds 3000.00

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(destinationAccount));

        // Act & Assert
        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> transferService.transferMoney(validRequest)
        );

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        assertEquals(new BigDecimal("3000.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("1000.00"), destinationAccount.getBalance());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when source account is suspended")
    void transferMoney_SourceInactive_ThrowsAccountInactiveException() {
        // Arrange
        sourceAccount.setStatus(AccountStatus.SUSPENDED);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(destinationAccount));

        // Act & Assert
        AccountInactiveException exception = assertThrows(
                AccountInactiveException.class,
                () -> transferService.transferMoney(validRequest)
        );

        assertTrue(exception.getMessage().contains("Source"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when destination account is closed")
    void transferMoney_DestinationInactive_ThrowsAccountInactiveException() {
        // Arrange
        destinationAccount.setStatus(AccountStatus.CLOSED);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(destinationAccount));

        // Act & Assert
        AccountInactiveException exception = assertThrows(
                AccountInactiveException.class,
                () -> transferService.transferMoney(validRequest)
        );

        assertTrue(exception.getMessage().contains("Destination"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when destination account does not exist")
    void transferMoney_DestinationNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transferService.transferMoney(validRequest)
        );

        assertTrue(exception.getMessage().contains("Account not found with ID: 2"));
        verify(accountRepository, never()).save(any());
    }
}
