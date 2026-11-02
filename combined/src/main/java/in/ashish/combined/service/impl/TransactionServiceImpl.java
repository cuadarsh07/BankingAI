package in.ashish.combined.service.impl;

import in.ashish.combined.dto.PageResponse;
import in.ashish.combined.dto.TransactionRecordResponse;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of TransactionService for querying transaction records.
 *
 * Interview Concepts:
 * - Pagination & Sorting: Prevents out-of-memory errors by retrieving a fixed chunk of data
 *   (e.g., LIMIT 10 OFFSET 0) instead of thousands of historical rows at once.
 * - Read-Only Transaction: @Transactional(readOnly = true) hints Hibernate and JDBC driver
 *   to avoid dirty-checking overhead, boosting query throughput.
 * - Preventing N+1: The query uses JOIN FETCH so the parent accounts are joined in a single
 *   SQL SELECT rather than issuing 10 additional SELECTs for each row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionRecordResponse> getAccountTransactions(
            Long accountId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.info("Fetching transactions for account ID: {}, type: {}, start: {}, end: {}, page: {}, size: {}",
                accountId, type, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        // Validate account exists
        if (!accountRepository.existsById(accountId)) {
            log.warn("Cannot fetch transactions: Account ID {} not found", accountId);
            throw new ResourceNotFoundException(String.format("Account not found with ID: %d", accountId));
        }

        Page<Transaction> transactionPage = transactionRepository.findTransactionsWithFilters(
                accountId, type, startDate, endDate, pageable
        );

        Page<TransactionRecordResponse> responsePage = transactionPage.map(TransactionRecordResponse::fromEntity);

        return PageResponse.fromPage(responsePage);
    }
}
