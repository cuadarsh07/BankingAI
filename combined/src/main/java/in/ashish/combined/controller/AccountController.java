package in.ashish.combined.controller;

import in.ashish.combined.dto.AccountResponse;
import in.ashish.combined.dto.CreateAccountRequest;
import in.ashish.combined.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST Controller exposing Account Management APIs.
 *
 * Base Path: /api/accounts
 *
 * Interview Concepts:
 * - @RestController = @Controller + @ResponseBody (every method returns serialized JSON).
 * - ResponseEntity: Allows explicit control over HTTP Status Code, Headers, and Response Body.
 * - HTTP Status Codes:
 *     201 Created: Returned on successful resource creation with a Location header pointing to the new URI.
 *     200 OK: Standard response for successful retrieval.
 *     400 Bad Request: Automatically triggered by @Valid when payload violates validation constraints.
 *     404 Not Found: Handled via GlobalExceptionHandler when account doesn't exist.
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Create a new bank account.
     * POST /api/accounts
     * Status: 201 Created on success, 400 Bad Request on validation or duplicate error.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("REST request to create account: {}", request.getAccountNumber());
        AccountResponse response = accountService.createAccount(request);

        // Building URI: /api/accounts/{id}
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Retrieve account by ID.
     * GET /api/accounts/{id}
     * Status: 200 OK on success, 404 Not Found if missing.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        log.info("REST request to get account with ID: {}", id);
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve account by Account Number.
     * GET /api/accounts/by-number/{accountNumber}
     * Status: 200 OK on success, 404 Not Found if missing.
     */
    @GetMapping("/by-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        log.info("REST request to get account with number: {}", accountNumber);
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve all accounts.
     * GET /api/accounts
     * Status: 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.info("REST request to list all accounts");
        List<AccountResponse> responseList = accountService.getAllAccounts();
        return ResponseEntity.ok(responseList);
    }

    /**
     * Full update of account profile details.
     * PUT /api/accounts/{id}
     * Status: 200 OK on success, 400 Bad Request on validation or duplicate error, 404 Not Found if missing.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody in.ashish.combined.dto.UpdateAccountRequest request) {
        log.info("REST request to update account ID: {}", id);
        AccountResponse response = accountService.updateAccount(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partial update of account details.
     * PATCH /api/accounts/{id}
     * Status: 200 OK on success, 400 Bad Request on validation, 404 Not Found if missing.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> patchAccount(
            @PathVariable Long id,
            @Valid @RequestBody in.ashish.combined.dto.PatchAccountRequest request) {
        log.info("REST request to patch account ID: {}", id);
        AccountResponse response = accountService.patchAccount(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deposit funds into an account.
     * POST /api/accounts/{id}/deposit
     * Status: 200 OK on success, 400 Bad Request on negative amount or inactive account, 404 Not Found if missing.
     */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<in.ashish.combined.dto.TransactionResponse> deposit(
            @PathVariable Long id,
            @Valid @RequestBody in.ashish.combined.dto.TransactionRequest request) {
        log.info("REST request to deposit ₹{} into account ID: {}", request.getAmount(), id);
        in.ashish.combined.dto.TransactionResponse response = accountService.deposit(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw funds from an account.
     * POST /api/accounts/{id}/withdraw
     * Status: 200 OK on success, 400 Bad Request on insufficient funds/inactive account, 404 Not Found if missing.
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<in.ashish.combined.dto.TransactionResponse> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody in.ashish.combined.dto.TransactionRequest request) {
        log.info("REST request to withdraw ₹{} from account ID: {}", request.getAmount(), id);
        in.ashish.combined.dto.TransactionResponse response = accountService.withdraw(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Check account balance.
     * GET /api/accounts/{id}/balance
     * Status: 200 OK on success, 404 Not Found if missing.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<in.ashish.combined.dto.BalanceResponse> getBalance(@PathVariable Long id) {
        log.info("REST request to check balance for account ID: {}", id);
        in.ashish.combined.dto.BalanceResponse response = accountService.getBalance(id);
        return ResponseEntity.ok(response);
    }
}
