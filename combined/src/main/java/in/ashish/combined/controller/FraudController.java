package in.ashish.combined.controller;

import in.ashish.combined.dto.fraud.*;
import in.ashish.combined.entity.User;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.service.fraud.FraudAlertService;
import in.ashish.combined.service.fraud.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * REST Controller for Customer Fraud Protection & Held Transaction Management.
 *
 * Base Path: /api/fraud
 */
@Slf4j
@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FraudController {

    private final FraudDetectionService fraudDetectionService;
    private final FraudAlertService fraudAlertService;
    private final UserRepository userRepository;

    /**
     * Pre-transfer Risk Assessment (Dry run before submitting money).
     * POST /api/fraud/assessments
     */
    @PostMapping("/assessments")
    public ResponseEntity<FraudAssessmentResponse> assessTransferRisk(
            @Valid @RequestBody FraudAssessmentRequest request) {

        log.info("API request to assess transfer risk from account {} to {} (₹{})",
                request.getSourceAccountId(), request.getDestinationAccountId(), request.getAmount());

        FraudAssessmentResponse response = fraudDetectionService.assessTransfer(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getDescription(),
                request.getUserExplanation()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get Assessment Details by ID.
     * GET /api/fraud/assessments/{id}
     */
    @GetMapping("/assessments/{id}")
    public ResponseEntity<FraudAssessmentResponse> getAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(fraudDetectionService.getAssessmentById(id));
    }

    /**
     * Get Fraud Alerts for Authenticated Customer.
     * GET /api/fraud/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<FraudAlertResponse>> getCustomerAlerts() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(fraudAlertService.getAlertsForUser(user.getId()));
    }

    /**
     * Mark an alert as read.
     * PATCH /api/fraud/alerts/{id}/read
     */
    @PatchMapping("/alerts/{id}/read")
    public ResponseEntity<Void> markAlertRead(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        fraudAlertService.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Get Transactions on Hold for the Authenticated Customer.
     * GET /api/fraud/held-transactions
     */
    @GetMapping("/held-transactions")
    public ResponseEntity<List<HeldTransactionResponse>> getHeldTransactions() {
        User user = getAuthenticatedUser();
        if (user.getAccount() == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(fraudDetectionService.getHeldTransactionsForAccount(user.getAccount().getId()));
    }

    /**
     * Get details of a single held transaction.
     * GET /api/fraud/held-transactions/{id}
     */
    @GetMapping("/held-transactions/{id}")
    public ResponseEntity<HeldTransactionResponse> getHeldTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(fraudDetectionService.getHeldTransactionById(id));
    }

    /**
     * Customer submits security verification answers.
     * POST /api/fraud/transactions/{id}/verify
     */
    @PostMapping("/transactions/{id}/verify")
    public ResponseEntity<HeldTransactionResponse> verifyTransaction(
            @PathVariable Long id,
            @Valid @RequestBody VerificationSubmitRequest request) {

        User user = getAuthenticatedUser();
        log.info("Customer '{}' submitting verification for held transaction ID {}", user.getUsername(), id);
        HeldTransactionResponse response = fraudDetectionService.verifyHeldTransaction(id, user, request.getAnswers());
        return ResponseEntity.ok(response);
    }

    /**
     * Customer cancels held transaction.
     * POST /api/fraud/transactions/{id}/cancel
     */
    @PostMapping("/transactions/{id}/cancel")
    public ResponseEntity<HeldTransactionResponse> cancelTransaction(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        log.info("Customer '{}' cancelling held transaction ID {}", user.getUsername(), id);
        HeldTransactionResponse response = fraudDetectionService.cancelHeldTransaction(id, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Customer requests manual Bank Review.
     * POST /api/fraud/transactions/{id}/request-review
     */
    @PostMapping("/transactions/{id}/request-review")
    public ResponseEntity<HeldTransactionResponse> requestReview(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        log.info("Customer '{}' requesting bank review for held transaction ID {}", user.getUsername(), id);
        HeldTransactionResponse response = fraudDetectionService.requestBankReview(id, user);
        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidTransferException("Authentication required.");
        }
        return userRepository.findByUsernameWithAccount(auth.getName())
                .or(() -> userRepository.findByUsername(auth.getName()))
                .orElseThrow(() -> new InvalidTransferException("Authenticated user not found: " + auth.getName()));
    }
}

