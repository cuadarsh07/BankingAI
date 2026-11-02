package in.ashish.combined.service.impl;

import in.ashish.combined.dto.*;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.exception.AccountInactiveException;
import in.ashish.combined.exception.DuplicateResourceException;
import in.ashish.combined.exception.InsufficientBalanceException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service Implementation for Bank Account business operations.
 *
 * Interview Concepts:
 * - Business Logic in Service Layer: Keeping domain rules (active status, balance checks)
 *   out of the Controller so they can be reused across REST, messaging queues, or batch jobs.
 * - BigDecimal: Used for monetary operations (add, subtract, compareTo) to prevent
 *   binary floating-point precision drift.
 * - @Transactional: Ensures ACID consistency. If InsufficientBalanceException or any
 *   runtime error occurs, all database changes are rolled back automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Attempting to create account with account number: {}", request.getAccountNumber());

        // Business Rule 1: Check for duplicate account number
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            log.error("Account creation failed: Account number '{}' already exists", request.getAccountNumber());
            throw new DuplicateResourceException(
                    String.format("Account with account number '%s' already exists", request.getAccountNumber())
            );
        }

        // Business Rule 2: Check for duplicate email
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (accountRepository.existsByEmail(normalizedEmail)) {
            log.error("Account creation failed: Email '{}' already registered", normalizedEmail);
            throw new DuplicateResourceException(
                    String.format("Email '%s' is already registered with another account", normalizedEmail)
            );
        }

        // Map DTO to Entity
        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName().trim())
                .email(normalizedEmail)
                .balance(request.getBalance())
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully created account with ID: {} and Account Number: {}", savedAccount.getId(), savedAccount.getAccountNumber());

        return AccountResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        log.info("Fetching account with ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Account lookup failed: Account with ID {} not found", id);
                    return new ResourceNotFoundException(String.format("Account not found with ID: %d", id));
                });

        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account with account number: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("Account lookup failed: Account with number {} not found", accountNumber);
                    return new ResourceNotFoundException(String.format("Account not found with account number: %s", accountNumber));
                });

        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        log.info("Fetching all bank accounts");
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        log.info("Full update (PUT) for account ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Account not found with ID: %d", id)));

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Business Rule: Check if the new email belongs to ANOTHER account (exclude current ID)
        if (accountRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            log.error("Update failed: Email '{}' is already in use by another account", normalizedEmail);
            throw new DuplicateResourceException(
                    String.format("Email '%s' is already registered with another account", normalizedEmail)
            );
        }

        account.setAccountHolderName(request.getAccountHolderName().trim());
        account.setEmail(normalizedEmail);

        Account updatedAccount = accountRepository.save(account);
        log.info("Successfully updated account ID: {}", updatedAccount.getId());
        return AccountResponse.fromEntity(updatedAccount);
    }

    @Override
    @Transactional
    public AccountResponse patchAccount(Long id, PatchAccountRequest request) {
        log.info("Partial update (PATCH) for account ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Account not found with ID: %d", id)));

        // Update name if supplied
        if (request.getAccountHolderName() != null && !request.getAccountHolderName().trim().isEmpty()) {
            account.setAccountHolderName(request.getAccountHolderName().trim());
        }

        // Update email if supplied
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (accountRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
                log.error("Patch failed: Email '{}' is already in use by another account", normalizedEmail);
                throw new DuplicateResourceException(
                        String.format("Email '%s' is already registered with another account", normalizedEmail)
                );
            }
            account.setEmail(normalizedEmail);
        }

        // Update status if supplied
        if (request.getStatus() != null) {
            account.setStatus(request.getStatus());
        }

        Account updatedAccount = accountRepository.save(account);
        log.info("Successfully patched account ID: {}", updatedAccount.getId());
        return AccountResponse.fromEntity(updatedAccount);
    }

    @Override
    @Transactional
    public TransactionResponse deposit(Long id, TransactionRequest request) {
        log.info("Processing DEPOSIT for account ID: {}, Amount: {}", id, request.getAmount());

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Account not found with ID: %d", id)));

        // Business Rule 1: Account must be ACTIVE to accept deposits
        validateAccountIsActive(account);

        BigDecimal previousBalance = account.getBalance();
        BigDecimal newBalance = previousBalance.add(request.getAmount());
        account.setBalance(newBalance);

        Account updatedAccount = accountRepository.save(account);

        // Record audit ledger entry
        String depositRef = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction depositTxn = Transaction.builder()
                .referenceNumber(depositRef)
                .account(updatedAccount)
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Account deposit")
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(depositTxn);

        log.info("DEPOSIT successful for account ID: {}. Previous: {}, New: {}, Ref: {}",
                id, previousBalance, newBalance, depositRef);

        return TransactionResponse.builder()
                .transactionType("DEPOSIT")
                .accountId(updatedAccount.getId())
                .accountNumber(updatedAccount.getAccountNumber())
                .transactionAmount(request.getAmount())
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .accountStatus(updatedAccount.getStatus())
                .description(request.getDescription())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(Long id, TransactionRequest request) {
        log.info("Processing WITHDRAWAL for account ID: {}, Amount: {}", id, request.getAmount());

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Account not found with ID: %d", id)));

        // Business Rule 1: Account must be ACTIVE to withdraw funds
        validateAccountIsActive(account);

        // Business Rule 2: Sufficient funds check
        BigDecimal currentBalance = account.getBalance();
        if (currentBalance.compareTo(request.getAmount()) < 0) {
            log.error("Withdrawal rejected: Insufficient funds in account ID {}. Available: {}, Requested: {}",
                    id, currentBalance, request.getAmount());
            throw new InsufficientBalanceException(
                    String.format("Insufficient funds in account '%s'. Current balance: %.2f, Requested withdrawal: %.2f",
                            account.getAccountNumber(), currentBalance, request.getAmount())
            );
        }

        BigDecimal newBalance = currentBalance.subtract(request.getAmount());
        account.setBalance(newBalance);

        Account updatedAccount = accountRepository.save(account);

        // Record audit ledger entry
        String withdrawRef = "WTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction withdrawTxn = Transaction.builder()
                .referenceNumber(withdrawRef)
                .account(updatedAccount)
                .type(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Account withdrawal")
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(withdrawTxn);

        log.info("WITHDRAWAL successful for account ID: {}. Previous: {}, New: {}, Ref: {}",
                id, currentBalance, newBalance, withdrawRef);

        return TransactionResponse.builder()
                .transactionType("WITHDRAWAL")
                .accountId(updatedAccount.getId())
                .accountNumber(updatedAccount.getAccountNumber())
                .transactionAmount(request.getAmount())
                .previousBalance(currentBalance)
                .newBalance(newBalance)
                .accountStatus(updatedAccount.getStatus())
                .description(request.getDescription())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long id) {
        log.info("Fetching balance for account ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Account not found with ID: %d", id)));

        return BalanceResponse.fromEntity(account);
    }

    private void validateAccountIsActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.error("Transaction rejected: Account ID {} is in state {}", account.getId(), account.getStatus());
            throw new AccountInactiveException(
                    String.format("Transaction rejected: Account '%s' is %s. Only ACTIVE accounts can process transactions.",
                            account.getAccountNumber(), account.getStatus())
            );
        }
    }
}
