package in.ashish.combined.controller;

import in.ashish.combined.dto.fraud.AdminFraudDashboardResponse;
import in.ashish.combined.dto.fraud.AdminReviewRequest;
import in.ashish.combined.dto.fraud.HeldTransactionResponse;
import in.ashish.combined.entity.User;
import in.ashish.combined.entity.fraud.HeldTransactionState;
import in.ashish.combined.entity.fraud.RiskLevel;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.service.fraud.FraudAlertService;
import in.ashish.combined.service.fraud.FraudDetectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Bank Administrators to monitor fraud alerts and authorize/reject held transfers.
 *
 * Base Path: /api/admin/fraud
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/fraud")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFraudController {

    private final FraudDetectionService fraudDetectionService;
    private final FraudAlertService fraudAlertService;
    private final UserRepository userRepository;

    /**
     * Get Central Fraud Monitoring Dashboard Metrics & Queue.
     * GET /api/admin/fraud/monitoring
     */
    @GetMapping("/monitoring")
    public ResponseEntity<AdminFraudDashboardResponse> getMonitoringDashboard() {
        log.info("Admin accessing fraud monitoring dashboard");

        List<HeldTransactionResponse> allHeld = fraudDetectionService.getAllHeldTransactions();

        long pendingReview = allHeld.stream()
                .filter(h -> h.getState() == HeldTransactionState.PENDING_BANK_REVIEW
                        || h.getState() == HeldTransactionState.PENDING_CUSTOMER_VERIFICATION)
                .count();

        long lowCount = allHeld.stream().filter(h -> h.getRiskLevel() == RiskLevel.LOW).count();
        long medCount = allHeld.stream().filter(h -> h.getRiskLevel() == RiskLevel.MEDIUM).count();
        long highCount = allHeld.stream().filter(h -> h.getRiskLevel() == RiskLevel.HIGH).count();
        long critCount = allHeld.stream().filter(h -> h.getRiskLevel() == RiskLevel.CRITICAL).count();
        long scamCount = allHeld.stream().filter(HeldTransactionResponse::isScamDetected).count();

        AdminFraudDashboardResponse response = AdminFraudDashboardResponse.builder()
                .totalFlaggedTransactions(allHeld.size())
                .pendingReviewCount(pendingReview)
                .lowRiskCount(lowCount)
                .mediumRiskCount(medCount)
                .highRiskCount(highCount)
                .criticalRiskCount(critCount)
                .scamDetectedCount(scamCount)
                .heldTransactions(allHeld)
                .recentAlerts(fraudAlertService.getAllAlerts())
                .recentAuditLogs(fraudAlertService.getRecentAuditLogs())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Bank Officer review action (Approve or Reject).
     * POST /api/admin/fraud/transactions/{id}/review
     */
    @PostMapping("/transactions/{id}/review")
    public ResponseEntity<HeldTransactionResponse> reviewTransaction(
            @PathVariable Long id,
            @Valid @RequestBody AdminReviewRequest request,
            HttpServletRequest servletRequest) {

        User adminUser = getAuthenticatedUser();
        String clientIp = servletRequest.getRemoteAddr();

        log.info("Admin '{}' executing review action '{}' on held transaction ID {}",
                adminUser.getUsername(), request.getAction(), id);

        HeldTransactionResponse response = fraudDetectionService.adminReviewHeldTransaction(
                id,
                adminUser,
                request.getAction(),
                request.getNotes() != null ? request.getNotes() : request.getReason(),
                clientIp
        );

        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidTransferException("Authentication required.");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new InvalidTransferException("Admin user not found: " + auth.getName()));
    }
}

