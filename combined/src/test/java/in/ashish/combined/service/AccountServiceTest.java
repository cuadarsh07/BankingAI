package in.ashish.combined.service;

import in.ashish.combined.dto.AccountResponse;
import in.ashish.combined.dto.CreateAccountRequest;
import in.ashish.combined.dto.PatchAccountRequest;
import in.ashish.combined.dto.UpdateAccountRequest;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.exception.DuplicateResourceException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.service.impl.AccountServiceImpl;
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
 * Unit tests for AccountServiceImpl using Mockito.
 *
 * Interview Concepts:
 * - Isolated Unit Testing: Fast, no Spring Context or database required.
 * - @Mock: Creates a dummy stub for AccountRepository.
 * - @InjectMocks: Creates AccountServiceImpl and injects the mocked repository into it.
 * - verify(): Verifies that repository methods were called with expected arguments.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private CreateAccountRequest validRequest;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        validRequest = CreateAccountRequest.builder()
                .accountNumber("1234567890")
                .accountHolderName("Ashish Kumar")
                .email("ashish@example.com")
                .balance(new BigDecimal("1500.00"))
                .build();

        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountHolderName("Ashish Kumar")
                .email("ashish@example.com")
                .balance(new BigDecimal("1500.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create an account when account number and email are unique")
    void createAccount_Success() {
        // Arrange
        when(accountRepository.existsByAccountNumber("1234567890")).thenReturn(false);
        when(accountRepository.existsByEmail("ashish@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        // Act
        AccountResponse response = accountService.createAccount(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("1234567890", response.getAccountNumber());
        assertEquals("Ashish Kumar", response.getAccountHolderName());
        assertEquals("ashish@example.com", response.getEmail());
        assertEquals(new BigDecimal("1500.00"), response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        verify(accountRepository, times(1)).existsByAccountNumber("1234567890");
        verify(accountRepository, times(1)).existsByEmail("ashish@example.com");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when account number already exists")
    void createAccount_DuplicateAccountNumber_ThrowsException() {
        // Arrange
        when(accountRepository.existsByAccountNumber("1234567890")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> accountService.createAccount(validRequest)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(accountRepository, times(1)).existsByAccountNumber("1234567890");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void createAccount_DuplicateEmail_ThrowsException() {
        // Arrange
        when(accountRepository.existsByAccountNumber("1234567890")).thenReturn(false);
        when(accountRepository.existsByEmail("ashish@example.com")).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> accountService.createAccount(validRequest)
        );

        assertTrue(exception.getMessage().contains("already registered"));
        verify(accountRepository, times(1)).existsByEmail("ashish@example.com");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should successfully return account when ID exists")
    void getAccountById_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        // Act
        AccountResponse response = accountService.getAccountById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("1234567890", response.getAccountNumber());
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when account ID is not found")
    void getAccountById_NotFound_ThrowsException() {
        // Arrange
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getAccountById(99L)
        );

        assertTrue(exception.getMessage().contains("Account not found with ID: 99"));
        verify(accountRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Should successfully update account details on PUT request")
    void updateAccount_Success() {
        // Arrange
        UpdateAccountRequest updateReq = UpdateAccountRequest.builder()
                .accountHolderName("Ashish Updated")
                .email("newemail@example.com")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.existsByEmailAndIdNot("newemail@example.com", 1L)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        // Act
        AccountResponse response = accountService.updateAccount(1L, updateReq);

        // Assert
        assertNotNull(response);
        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).existsByEmailAndIdNot("newemail@example.com", 1L);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on PUT if new email belongs to another account")
    void updateAccount_DuplicateEmail_ThrowsException() {
        // Arrange
        UpdateAccountRequest updateReq = UpdateAccountRequest.builder()
                .accountHolderName("Ashish Updated")
                .email("existing@example.com")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.existsByEmailAndIdNot("existing@example.com", 1L)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> accountService.updateAccount(1L, updateReq)
        );

        assertTrue(exception.getMessage().contains("already registered"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should successfully apply partial update on PATCH request")
    void patchAccount_Success() {
        // Arrange
        PatchAccountRequest patchReq = PatchAccountRequest.builder()
                .status(AccountStatus.SUSPENDED)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        // Act
        AccountResponse response = accountService.patchAccount(1L, patchReq);

        // Assert
        assertNotNull(response);
        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should successfully deposit money into an active account")
    void deposit_Success() {
        // Arrange
        in.ashish.combined.dto.TransactionRequest request = in.ashish.combined.dto.TransactionRequest.builder()
                .amount(new BigDecimal("500.00"))
                .description("Cash deposit")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        in.ashish.combined.dto.TransactionResponse response = accountService.deposit(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("DEPOSIT", response.getTransactionType());
        assertEquals(new BigDecimal("1500.00"), response.getPreviousBalance());
        assertEquals(new BigDecimal("2000.00"), response.getNewBalance());
        assertEquals(new BigDecimal("500.00"), response.getTransactionAmount());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when depositing into a suspended account")
    void deposit_AccountInactive_ThrowsException() {
        // Arrange
        sampleAccount.setStatus(AccountStatus.SUSPENDED);
        in.ashish.combined.dto.TransactionRequest request = in.ashish.combined.dto.TransactionRequest.builder()
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        // Act & Assert
        in.ashish.combined.exception.AccountInactiveException exception = assertThrows(
                in.ashish.combined.exception.AccountInactiveException.class,
                () -> accountService.deposit(1L, request)
        );

        assertTrue(exception.getMessage().contains("SUSPENDED"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should successfully withdraw money from account with sufficient balance")
    void withdraw_Success() {
        // Arrange
        in.ashish.combined.dto.TransactionRequest request = in.ashish.combined.dto.TransactionRequest.builder()
                .amount(new BigDecimal("500.00"))
                .description("ATM withdrawal")
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        in.ashish.combined.dto.TransactionResponse response = accountService.withdraw(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("WITHDRAWAL", response.getTransactionType());
        assertEquals(new BigDecimal("1500.00"), response.getPreviousBalance());
        assertEquals(new BigDecimal("1000.00"), response.getNewBalance());
        assertEquals(new BigDecimal("500.00"), response.getTransactionAmount());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when withdrawal amount exceeds balance")
    void withdraw_InsufficientBalance_ThrowsException() {
        // Arrange
        in.ashish.combined.dto.TransactionRequest request = in.ashish.combined.dto.TransactionRequest.builder()
                .amount(new BigDecimal("5000.00")) // Exceeds 1500.00!
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        // Act & Assert
        in.ashish.combined.exception.InsufficientBalanceException exception = assertThrows(
                in.ashish.combined.exception.InsufficientBalanceException.class,
                () -> accountService.withdraw(1L, request)
        );

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when withdrawing from a closed account")
    void withdraw_AccountInactive_ThrowsException() {
        // Arrange
        sampleAccount.setStatus(AccountStatus.CLOSED);
        in.ashish.combined.dto.TransactionRequest request = in.ashish.combined.dto.TransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        // Act & Assert
        in.ashish.combined.exception.AccountInactiveException exception = assertThrows(
                in.ashish.combined.exception.AccountInactiveException.class,
                () -> accountService.withdraw(1L, request)
        );

        assertTrue(exception.getMessage().contains("CLOSED"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should successfully retrieve balance for an account")
    void getBalance_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        // Act
        in.ashish.combined.dto.BalanceResponse response = accountService.getBalance(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getAccountId());
        assertEquals(new BigDecimal("1500.00"), response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        verify(accountRepository, times(1)).findById(1L);
    }
}
