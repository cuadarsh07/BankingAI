package in.ashish.combined.service;

import in.ashish.combined.dto.PageResponse;
import in.ashish.combined.dto.TransactionRecordResponse;
import in.ashish.combined.entity.TransactionType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Service interface for querying bank transaction history with pagination and filters.
 */
public interface TransactionService {

    /**
     * Retrieve paginated transactions for an account with optional type and date filtering.
     *
     * @param accountId   Account ID to retrieve transactions for
     * @param type        Optional filter by TransactionType (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)
     * @param startDate   Optional filter starting date/time (inclusive)
     * @param endDate     Optional filter ending date/time (inclusive)
     * @param pageable    Pagination and sorting parameters (page, size, sort)
     * @return Paginated transaction history
     */
    PageResponse<TransactionRecordResponse> getAccountTransactions(
            Long accountId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}
