package in.ashish.combined.controller;

import in.ashish.combined.dto.TransferRequest;
import in.ashish.combined.dto.TransferResponse;
import in.ashish.combined.dto.fraud.FraudAssessmentResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.User;
import in.ashish.combined.entity.fraud.HeldTransaction;
import in.ashish.combined.entity.fraud.RiskLevel;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.service.TransferService;
import in.ashish.combined.service.fraud.FraudDetectionService;
import in.ashish.combined.service.fraud.FraudPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST Controller exposing Money Transfer APIs with AI Fraud Shield.
 *
 * Base Path: /api/transfers
 *
 * Banking Security Architecture:
 * 1. Pre-transfer AI and Statistical Anomaly Assessment (without holding DB locks).
 * 2. Deterministic policy evaluation.
 * 3. High/Critical risk transactions enter durable on-hold state (NO DEBITS OCCUR).
 * 4. Safe low-risk transactions proceed atomically.
 */
@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final FraudDetectionService fraudDetectionService;
    private final FraudPolicyService fraudPolicyService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * Transfer funds between two bank accounts with AI Fraud & Scam Protection.
     * POST /api/transfers
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transferMoney(@Valid @RequestBody TransferRequest request) {
        log.info("REST request to transfer ₹{} from account ID {} to account ID {}",
                request.getAmount(), request.getSourceAccountId(), request.getDestinationAccountId());

        // 1. Resolve Authenticated User and Validate Ownership
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            currentUser = userRepository.findByUsernameWithAccount(authentication.getName())
                    .or(() -> userRepository.findByUsername(authentication.getName()))
                    .orElse(null);
        }

        if (currentUser != null && currentUser.getRole() != Role.ROLE_ADMIN) {
            if (currentUser.getAccount() == null || !currentUser.getAccount().getId().equals(request.getSourceAccountId())) {
                log.warn("Unauthorized transfer attempt: User '{}' does not own source account ID {}",
                        currentUser.getUsername(), request.getSourceAccountId());
                throw new InvalidTransferException("Access denied: You can only transfer funds from your own account.");
            }
        }

        // 2. Perform Pre-Transfer Risk Assessment (Outside DB transaction)
        FraudAssessmentResponse assessment = fraudDetectionService.assessTransfer(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getDescription(),
                request.getUserExplanation()
        );

        FraudPolicyService.PolicyDecision decision = fraudPolicyService.decideTransferAction(
                assessment.getRiskLevel(),
                assessment.isScamDetected(),
                assessment.isRequiresHumanReview()
        );

        // 3. If Risk Requires Hold -> Create Held Transaction without debiting accounts
        if (decision == FraudPolicyService.PolicyDecision.HOLD_CUSTOMER_VERIFICATION
                || decision == FraudPolicyService.PolicyDecision.HOLD_BANK_REVIEW) {

            Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
            Account destAccount = accountRepository.findById(request.getDestinationAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

            HeldTransaction heldTxn = fraudDetectionService.createHeldTransfer(request, assessment, currentUser);

            log.warn("Transfer HELD by AI Fraud Shield! Ref: {}, Risk: {}, ScamDetected: {}",
                    heldTxn.getReferenceNumber(), assessment.getRiskLevel(), assessment.isScamDetected());

            TransferResponse heldResponse = TransferResponse.builder()
                    .transferId(heldTxn.getReferenceNumber())
                    .sourceAccountId(sourceAccount.getId())
                    .sourceAccountNumber(sourceAccount.getAccountNumber())
                    .destinationAccountId(destAccount.getId())
                    .destinationAccountNumber(destAccount.getAccountNumber())
                    .amount(request.getAmount())
                    .sourceBalanceAfter(sourceAccount.getBalance()) // UNCHANGED - No funds debited
                    .status("ON_HOLD")
                    .description(request.getDescription())
                    .timestamp(LocalDateTime.now())
                    .riskLevel(assessment.getRiskLevel().name())
                    .heldTransactionId(heldTxn.getId())
                    .warningMessage(assessment.getCustomerWarning())
                    .verificationRequired(assessment.getRiskLevel() != RiskLevel.CRITICAL && !assessment.isRequiresHumanReview())
                    .requiresAdminReview(assessment.getRiskLevel() == RiskLevel.CRITICAL || assessment.isRequiresHumanReview())
                    .build();

            return ResponseEntity.ok(heldResponse);
        }

        // 4. Low Risk -> Execute Immediate Atomic Transfer
        TransferResponse response = transferService.transferMoney(request);
        response.setRiskLevel(RiskLevel.LOW.name());
        return ResponseEntity.ok(response);
    }
}
