package in.ashish.combined.service.impl;

import in.ashish.combined.dto.TransferRequest;
import in.ashish.combined.dto.TransferResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.exception.AccountInactiveException;
import in.ashish.combined.exception.InsufficientBalanceException;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service Implementation for executing atomic funds transfers.
 *
 * Core Interview Concepts:
 * - ACID Atomicity: All balance updates (source debit + destination credit) execute in a
 *   single @Transactional method. If any exception occurs, Spring triggers an automatic rollback,
 *   guaranteeing money cannot be deducted from source without crediting destination.
 * - Deadlock Prevention (Ordered Locking): When two concurrent transfers occur between Account A
 *   and Account B in opposite directions, cyclic lock waiting causes a database DEADLOCK.
 *   We solve this by sorting account IDs and always acquiring locks in ascending order.
 * - Pessimistic Write Locking: Uses SELECT ... FOR UPDATE to prevent race conditions and lost updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferResponse transferMoney(TransferRequest request) {
        log.info("Initiating transfer of ₹{} from Account ID {} to Account ID {}",
                request.getAmount(), request.getSourceAccountId(), request.getDestinationAccountId());

        // Business Rule 1: Cannot transfer to the same account
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            log.error("Transfer rejected: Source and destination account IDs are identical ({})",
                    request.getSourceAccountId());
            throw new InvalidTransferException("Cannot transfer money to the same account.");
        }

        // Deadlock Prevention: Order account IDs in ascending order for lock acquisition
        Long firstLockId = Math.min(request.getSourceAccountId(), request.getDestinationAccountId());
        Long secondLockId = Math.max(request.getSourceAccountId(), request.getDestinationAccountId());

        // Acquire pessimistic write locks in strict order
        Account firstLockedAccount = accountRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Account not found with ID: %d", firstLockId)));

        Account secondLockedAccount = accountRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Account not found with ID: %d", secondLockId)));

        // Assign source and destination references
        Account sourceAccount = request.getSourceAccountId().equals(firstLockedAccount.getId())
                ? firstLockedAccount : secondLockedAccount;
        Account destinationAccount = request.getDestinationAccountId().equals(firstLockedAccount.getId())
                ? firstLockedAccount : secondLockedAccount;

        // Business Rule 2: Verify both accounts are ACTIVE
        validateAccountIsActive(sourceAccount, "Source");
        validateAccountIsActive(destinationAccount, "Destination");

        // Business Rule 3: Verify source account has sufficient funds
        BigDecimal currentSourceBalance = sourceAccount.getBalance();
        if (currentSourceBalance.compareTo(request.getAmount()) < 0) {
            log.error("Transfer rejected: Insufficient funds in source account ID {}. Available: {}, Requested: {}",
                    sourceAccount.getId(), currentSourceBalance, request.getAmount());
            throw new InsufficientBalanceException(
                    String.format("Insufficient funds in source account '%s'. Current balance: %.2f, Transfer amount: %.2f",
                            sourceAccount.getAccountNumber(), currentSourceBalance, request.getAmount())
            );
        }

        // Execute Debit on Source and Credit on Destination
        BigDecimal newSourceBalance = currentSourceBalance.subtract(request.getAmount());
        BigDecimal newDestBalance = destinationAccount.getBalance().add(request.getAmount());

        sourceAccount.setBalance(newSourceBalance);
        destinationAccount.setBalance(newDestBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        String transferId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        // 1. Ledger Entry for Source (Debit / TRANSFER_OUT)
        Transaction debitTxn = Transaction.builder()
                .referenceNumber(transferId + "-OUT")
                .account(sourceAccount)
                .counterpartyAccount(destinationAccount)
                .type(TransactionType.TRANSFER_OUT)
                .amount(request.getAmount())
                .balanceAfter(newSourceBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Transfer to " + destinationAccount.getAccountNumber())
                .createdAt(now)
                .build();
        transactionRepository.save(debitTxn);

        // 2. Ledger Entry for Destination (Credit / TRANSFER_IN)
        Transaction creditTxn = Transaction.builder()
                .referenceNumber(transferId + "-IN")
                .account(destinationAccount)
                .counterpartyAccount(sourceAccount)
                .type(TransactionType.TRANSFER_IN)
                .amount(request.getAmount())
                .balanceAfter(newDestBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Transfer from " + sourceAccount.getAccountNumber())
                .createdAt(now)
                .build();
        transactionRepository.save(creditTxn);

        log.info("Transfer {} SUCCESSFUL: ₹{} transferred from Account {} to Account {}. New source balance: {}",
                transferId, request.getAmount(), sourceAccount.getAccountNumber(), destinationAccount.getAccountNumber(), newSourceBalance);

        return TransferResponse.builder()
                .transferId(transferId)
                .sourceAccountId(sourceAccount.getId())
                .sourceAccountNumber(sourceAccount.getAccountNumber())
                .destinationAccountId(destinationAccount.getId())
                .destinationAccountNumber(destinationAccount.getAccountNumber())
                .amount(request.getAmount())
                .sourceBalanceAfter(newSourceBalance)
                .status("SUCCESS")
                .description(request.getDescription())
                .timestamp(now)
                .build();
    }

    private void validateAccountIsActive(Account account, String role) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.error("{} account ID {} is inactive with status: {}", role, account.getId(), account.getStatus());
            throw new AccountInactiveException(
                    String.format("%s account '%s' is %s. Only ACTIVE accounts can process transfers.",
                            role, account.getAccountNumber(), account.getStatus())
            );
        }
    }
}
