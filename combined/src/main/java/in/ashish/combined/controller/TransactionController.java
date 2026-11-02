package in.ashish.combined.controller;

import in.ashish.combined.dto.PageResponse;
import in.ashish.combined.dto.TransactionRecordResponse;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for accessing Bank Account Transaction History.
 *
 * Example endpoints:
 * - GET /api/accounts/1/transactions?page=0&size=10
 * - GET /api/accounts/1/transactions?type=DEPOSIT&page=0&size=5
 * - GET /api/accounts/1/transactions?startDate=2026-10-01T00:00:00&endDate=2026-10-31T23:59:59
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<PageResponse<TransactionRecordResponse>> getAccountTransactions(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("REST request for transactions on account ID {}: page={}, size={}, sortBy={}, dir={}, type={}",
                accountId, page, size, sortBy, direction, type);

        // Max page size safeguard against denial of service / memory exhaustion
        int validatedSize = Math.min(Math.max(size, 1), 100);
        int validatedPage = Math.max(page, 0);

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(sortDirection, sortBy));

        PageResponse<TransactionRecordResponse> response = transactionService.getAccountTransactions(
                accountId, type, startDate, endDate, pageable
        );

        return ResponseEntity.ok(response);
    }
}
