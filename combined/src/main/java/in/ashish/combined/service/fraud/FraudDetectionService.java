package in.ashish.combined.service.fraud;

import com.google.gson.Gson;
import in.ashish.combined.dto.TransferRequest;
import in.ashish.combined.dto.TransferResponse;
import in.ashish.combined.dto.fraud.FraudAssessmentResponse;
import in.ashish.combined.dto.fraud.HeldTransactionResponse;
import in.ashish.combined.dto.fraud.TransactionRiskDto;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.User;
import in.ashish.combined.entity.fraud.*;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.repository.fraud.FraudAssessmentRepository;
import in.ashish.combined.repository.fraud.FraudReviewRepository;
import in.ashish.combined.repository.fraud.HeldTransactionRepository;
import in.ashish.combined.repository.fraud.TransactionVerificationRepository;
import in.ashish.combined.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central Orchestration Service for Banking Fraud Detection and Held Transfer Workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRiskService transactionRiskService;
    private final GeminiService geminiService;
    private final FraudPolicyService fraudPolicyService;
    private final FraudAlertService fraudAlertService;
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final HeldTransactionRepository heldTransactionRepository;
    private final TransactionVerificationRepository transactionVerificationRepository;
    private final FraudReviewRepository fraudReviewRepository;
    private final TransferService transferService;

    private final Gson gson = new Gson();

    /**
     * Conducts pre-transfer assessment without opening an extended DB transaction.
     */
    public FraudAssessmentResponse assessTransfer(
            Long sourceAccountId,
            Long destAccountId,
            BigDecimal amount,
            String description,
            String userExplanation) {

        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + sourceAccountId));

        Account destinationAccount = accountRepository.findById(destAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + destAccountId));

        // 1. Evaluate deterministic statistical anomalies
        TransactionRiskDto statisticalRisk = transactionRiskService.evaluateRisk(sourceAccount, destinationAccount, amount);

        // 2. Evaluate with Gemini AI / safe fallback
        FraudAssessmentResponse response = geminiService.assessRiskWithGemini(
                sourceAccountId, destAccountId, amount, description, userExplanation, statisticalRisk);

        response.setSourceAccountId(sourceAccountId);
        response.setDestinationAccountId(destAccountId);
        response.setAmount(amount);

        String txnRef = "ASSESS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        response.setTransactionReference(txnRef);

        // 3. Persist Risk Audit records
        transactionRiskService.recordRisk(txnRef, sourceAccount, destinationAccount, amount, statisticalRisk);

        FraudAssessment assessment = FraudAssessment.builder()
                .assessmentId(response.getAssessmentId())
                .transactionReference(txnRef)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(amount)
                .riskLevel(response.getRiskLevel())
                .scamDetected(response.isScamDetected())
                .scamCategories(String.join(",", response.getScamCategories()))
                .confidence(response.getConfidence())
                .detectedIndicators(gson.toJson(response.getDetectedIndicators()))
                .explanation(response.getExplanation())
                .customerWarning(response.getCustomerWarning())
                .recommendedAction(response.getRecommendedAction())
                .requiresHumanReview(response.isRequiresHumanReview())
                .aiModelUsed(response.getAiModelUsed())
                .build();
        fraudAssessmentRepository.save(assessment);

        return response;
    }

    /**
     * Creates a durable HeldTransaction when a transfer is flagged.
     */
    @Transactional
    public HeldTransaction createHeldTransfer(
            TransferRequest request,
            FraudAssessmentResponse assessment,
            User authenticatedUser) {

        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        Account destAccount = accountRepository.findById(request.getDestinationAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        String reference = "HOLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String idempotencyKey = "IDEMP-" + reference;

        FraudPolicyService.PolicyDecision decision = fraudPolicyService.decideTransferAction(
                assessment.getRiskLevel(), assessment.isScamDetected(), assessment.isRequiresHumanReview());

        HeldTransactionState initialState = (decision == FraudPolicyService.PolicyDecision.HOLD_BANK_REVIEW)
                ? HeldTransactionState.PENDING_BANK_REVIEW
                : HeldTransactionState.PENDING_CUSTOMER_VERIFICATION;

        String questionsJson = gson.toJson(assessment.getVerificationQuestions());

        HeldTransaction heldTxn = HeldTransaction.builder()
                .referenceNumber(reference)
                .idempotencyKey(idempotencyKey)
                .sourceAccount(sourceAccount)
                .counterpartyAccount(destAccount)
                .amount(request.getAmount())
                .description(request.getDescription())
                .userExplanation(request.getUserExplanation())
                .state(initialState)
                .riskLevel(assessment.getRiskLevel())
                .scamDetected(assessment.isScamDetected())
                .scamCategory(assessment.getScamCategories().isEmpty() ? "ANOMALY" : assessment.getScamCategories().get(0))
                .aiExplanation(assessment.getExplanation())
                .customerWarning(assessment.getCustomerWarning())
                .verificationQuestions(questionsJson)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        HeldTransaction saved = heldTransactionRepository.save(heldTxn);

        // Raise fraud alert for customer
        AlertSeverity severity = assessment.getRiskLevel() == RiskLevel.CRITICAL ? AlertSeverity.CRITICAL : AlertSeverity.HIGH;
        String alertTitle = assessment.isScamDetected()
                ? "POTENTIAL SCAM DETECTED: Transfer on Hold"
                : "SECURITY ALERT: High-Risk Transfer Placed on Hold";

        fraudAlertService.createAlert(authenticatedUser, sourceAccount, reference, severity,
                alertTitle, assessment.getCustomerWarning(), heldTxn.getScamCategory());

        fraudAlertService.recordAuditLog("TRANSFER_HELD", "HeldTransaction", reference,
                authenticatedUser.getUsername(), "HOLD_TRANSACTION",
                "Transaction held with state " + initialState + ". Risk: " + assessment.getRiskLevel(),
                assessment.getRiskLevel().name());

        return saved;
    }

    /**
     * Customer responds to contextual verification questions.
     */
    @Transactional
    public HeldTransactionResponse verifyHeldTransaction(
            Long heldTransactionId,
            User customer,
            Map<String, String> answers) {

        HeldTransaction heldTxn = heldTransactionRepository.findById(heldTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Held transaction not found with ID: " + heldTransactionId));

        // Ownership validation
        validateAccountOwnership(heldTxn.getSourceAccount(), customer);

        // State validation
        if (heldTxn.getState() != HeldTransactionState.PENDING_CUSTOMER_VERIFICATION) {
            throw new InvalidTransferException("Transaction is not awaiting customer verification. Current state: " + heldTxn.getState());
        }

        // Expiry check
        if (heldTxn.getExpiresAt().isBefore(LocalDateTime.now())) {
            heldTxn.setState(HeldTransactionState.EXPIRED);
            heldTransactionRepository.save(heldTxn);
            throw new InvalidTransferException("Verification window expired for this transaction.");
        }

        // Record verification answers
        TransactionVerification verification = TransactionVerification.builder()
                .verificationId(UUID.randomUUID().toString())
                .transactionReference(heldTxn.getReferenceNumber())
                .user(customer)
                .account(heldTxn.getSourceAccount())
                .questionsAsked(heldTxn.getVerificationQuestions())
                .answersProvided(gson.toJson(answers))
                .submittedAt(LocalDateTime.now())
                .build();

        // Run policy evaluation on answers
        FraudPolicyService.VerificationEvaluationResult result =
                fraudPolicyService.evaluateVerificationAnswers(heldTxn.getRiskLevel(), answers);

        heldTxn.setCustomerVerificationAnswers(gson.toJson(answers));
        verification.setVerificationStatus(result.passed() ? VerificationStatus.PASSED : VerificationStatus.FAILED);
        verification.setFraudRiskResult(result.finalRiskLevel());
        transactionVerificationRepository.save(verification);

        if (result.passed()) {
            heldTxn.setState(HeldTransactionState.APPROVED);
            heldTransactionRepository.save(heldTxn);

            fraudAlertService.recordAuditLog("VERIFICATION_PASSED", "HeldTransaction", heldTxn.getReferenceNumber(),
                    customer.getUsername(), "VERIFY_ANSWERS", "Customer successfully completed verification.",
                    heldTxn.getRiskLevel().name());

            // Atomically execute approved transfer
            executeApprovedTransfer(heldTxn);
        } else {
            heldTxn.setState(result.nextState());
            heldTxn.setRiskLevel(result.finalRiskLevel());
            heldTxn.setCustomerWarning(result.message());
            heldTransactionRepository.save(heldTxn);

            fraudAlertService.recordAuditLog("VERIFICATION_FLAGGED", "HeldTransaction", heldTxn.getReferenceNumber(),
                    customer.getUsername(), "VERIFY_ANSWERS", result.message(), result.finalRiskLevel().name());

            // If scam was confirmed, create urgent critical alert
            fraudAlertService.createAlert(customer, heldTxn.getSourceAccount(), heldTxn.getReferenceNumber(),
                    AlertSeverity.CRITICAL, "DIGITAL ARREST / SCAM WARNING",
                    result.message(), "DIGITAL_ARREST");
        }

        return toHeldResponse(heldTxn);
    }

    /**
     * Customer cancels a held transfer.
     */
    @Transactional
    public HeldTransactionResponse cancelHeldTransaction(Long heldTransactionId, User customer) {
        HeldTransaction heldTxn = heldTransactionRepository.findById(heldTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Held transaction not found: " + heldTransactionId));

        validateAccountOwnership(heldTxn.getSourceAccount(), customer);

        if (heldTxn.getState() == HeldTransactionState.SUCCESS || heldTxn.getState() == HeldTransactionState.PROCESSING) {
            throw new InvalidTransferException("Cannot cancel transaction already executed or processing.");
        }

        heldTxn.setState(HeldTransactionState.CANCELLED);
        HeldTransaction saved = heldTransactionRepository.save(heldTxn);

        fraudAlertService.recordAuditLog("TRANSFER_CANCELLED", "HeldTransaction", heldTxn.getReferenceNumber(),
                customer.getUsername(), "CANCEL_TRANSACTION", "Customer cancelled the held transfer.",
                heldTxn.getRiskLevel().name());

        return toHeldResponse(saved);
    }

    /**
     * Customer requests Bank Admin Review for a held transfer.
     */
    @Transactional
    public HeldTransactionResponse requestBankReview(Long heldTransactionId, User customer) {
        HeldTransaction heldTxn = heldTransactionRepository.findById(heldTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Held transaction not found: " + heldTransactionId));

        validateAccountOwnership(heldTxn.getSourceAccount(), customer);

        heldTxn.setState(HeldTransactionState.PENDING_BANK_REVIEW);
        HeldTransaction saved = heldTransactionRepository.save(heldTxn);

        fraudAlertService.recordAuditLog("REVIEW_REQUESTED", "HeldTransaction", heldTxn.getReferenceNumber(),
                customer.getUsername(), "REQUEST_REVIEW", "Customer escalated transaction for manual bank officer review.",
                heldTxn.getRiskLevel().name());

        return toHeldResponse(saved);
    }

    /**
     * Admin approves or rejects a held transfer.
     */
    @Transactional
    public HeldTransactionResponse adminReviewHeldTransaction(
            Long heldTransactionId,
            User adminUser,
            String action,
            String notes,
            String clientIp) {

        HeldTransaction heldTxn = heldTransactionRepository.findById(heldTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Held transaction not found: " + heldTransactionId));

        ReviewAction reviewAction;
        try {
            reviewAction = ReviewAction.valueOf(action.toUpperCase());
        } catch (Exception ex) {
            throw new InvalidTransferException("Invalid review action. Must be APPROVE or REJECT.");
        }

        FraudReview review = FraudReview.builder()
                .reviewId(UUID.randomUUID().toString())
                .transactionReference(heldTxn.getReferenceNumber())
                .reviewedByUser(adminUser)
                .action(reviewAction)
                .notes(notes)
                .reason(reviewAction == ReviewAction.APPROVED ? "Bank Officer Approved" : "Bank Officer Rejected")
                .ipAddress(clientIp)
                .reviewedAt(LocalDateTime.now())
                .build();
        fraudReviewRepository.save(review);

        heldTxn.setAdminReviewNotes(notes);
        heldTxn.setReviewedBy(adminUser.getUsername());

        if (reviewAction == ReviewAction.APPROVED) {
            heldTxn.setState(HeldTransactionState.APPROVED);
            heldTransactionRepository.save(heldTxn);

            fraudAlertService.recordAuditLog("ADMIN_APPROVED", "HeldTransaction", heldTxn.getReferenceNumber(),
                    adminUser.getUsername(), "APPROVE_TRANSFER", notes, heldTxn.getRiskLevel().name());

            executeApprovedTransfer(heldTxn);
        } else {
            heldTxn.setState(HeldTransactionState.REJECTED);
            heldTransactionRepository.save(heldTxn);

            fraudAlertService.recordAuditLog("ADMIN_REJECTED", "HeldTransaction", heldTxn.getReferenceNumber(),
                    adminUser.getUsername(), "REJECT_TRANSFER", notes, heldTxn.getRiskLevel().name());
        }

        return toHeldResponse(heldTxn);
    }

    /**
     * Executes the financial balance transfer atomically under strict pessimistic locking.
     */
    private void executeApprovedTransfer(HeldTransaction heldTxn) {
        heldTxn.setState(HeldTransactionState.PROCESSING);
        heldTransactionRepository.save(heldTxn);

        TransferRequest transferRequest = TransferRequest.builder()
                .sourceAccountId(heldTxn.getSourceAccount().getId())
                .destinationAccountId(heldTxn.getCounterpartyAccount().getId())
                .amount(heldTxn.getAmount())
                .description(heldTxn.getDescription() != null ? heldTxn.getDescription() : "Transfer (Held Ref: " + heldTxn.getReferenceNumber() + ")")
                .build();

        // Delegate to core atomic transfer service (which acquires ordered pessimistic write locks)
        TransferResponse result = transferService.transferMoney(transferRequest);

        heldTxn.setState(HeldTransactionState.SUCCESS);
        heldTransactionRepository.save(heldTxn);

        fraudAlertService.recordAuditLog("TRANSFER_EXECUTED", "HeldTransaction", heldTxn.getReferenceNumber(),
                "SYSTEM", "EXECUTE_TRANSFER", "Funds successfully transferred with ledger ID: " + result.getTransferId(),
                heldTxn.getRiskLevel().name());
    }

    @Transactional(readOnly = true)
    public List<HeldTransactionResponse> getHeldTransactionsForAccount(Long sourceAccountId) {
        return heldTransactionRepository.findBySourceAccountIdOrderByCreatedAtDesc(sourceAccountId).stream()
                .map(this::toHeldResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HeldTransactionResponse> getAllHeldTransactions() {
        return heldTransactionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toHeldResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HeldTransactionResponse getHeldTransactionById(Long id) {
        return heldTransactionRepository.findById(id)
                .map(this::toHeldResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Held transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public FraudAssessmentResponse getAssessmentById(Long id) {
        FraudAssessment a = fraudAssessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud assessment not found with ID: " + id));

        List<String> categories = a.getScamCategories() != null && !a.getScamCategories().isEmpty()
                ? Arrays.asList(a.getScamCategories().split(",")) : Collections.emptyList();

        List<String> indicators = Collections.emptyList();
        try {
            if (a.getDetectedIndicators() != null) {
                indicators = gson.fromJson(a.getDetectedIndicators(), List.class);
            }
        } catch (Exception ignored) {}

        return FraudAssessmentResponse.builder()
                .assessmentId(a.getAssessmentId())
                .transactionReference(a.getTransactionReference())
                .sourceAccountId(a.getSourceAccount().getId())
                .destinationAccountId(a.getDestinationAccount() != null ? a.getDestinationAccount().getId() : null)
                .amount(a.getAmount())
                .riskLevel(a.getRiskLevel())
                .scamDetected(a.getScamDetected())
                .scamCategories(categories)
                .confidence(a.getConfidence() != null ? a.getConfidence() : 0.8)
                .detectedIndicators(indicators)
                .explanation(a.getExplanation())
                .customerWarning(a.getCustomerWarning())
                .recommendedAction(a.getRecommendedAction())
                .requiresHumanReview(a.getRequiresHumanReview())
                .aiModelUsed(a.getAiModelUsed())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private void validateAccountOwnership(Account account, User user) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            return; // Admins have elevated privileges
        }
        if (user.getAccount() == null || !user.getAccount().getId().equals(account.getId())) {
            throw new InvalidTransferException("Access denied: You can only manage transactions for your own account.");
        }
    }

    public HeldTransactionResponse toHeldResponse(HeldTransaction h) {
        List<String> questions = Collections.emptyList();
        try {
            if (h.getVerificationQuestions() != null) {
                questions = gson.fromJson(h.getVerificationQuestions(), List.class);
            }
        } catch (Exception ignored) {}

        return HeldTransactionResponse.builder()
                .id(h.getId())
                .referenceNumber(h.getReferenceNumber())
                .idempotencyKey(h.getIdempotencyKey())
                .sourceAccountId(h.getSourceAccount().getId())
                .sourceAccountNumber(h.getSourceAccount().getAccountNumber())
                .sourceAccountHolder(h.getSourceAccount().getAccountHolderName())
                .destinationAccountId(h.getCounterpartyAccount().getId())
                .destinationAccountNumber(h.getCounterpartyAccount().getAccountNumber())
                .destinationAccountHolder(h.getCounterpartyAccount().getAccountHolderName())
                .amount(h.getAmount())
                .description(h.getDescription())
                .userExplanation(h.getUserExplanation())
                .state(h.getState())
                .riskLevel(h.getRiskLevel())
                .scamDetected(h.getScamDetected())
                .scamCategory(h.getScamCategory())
                .aiExplanation(h.getAiExplanation())
                .customerWarning(h.getCustomerWarning())
                .verificationQuestions(questions)
                .customerVerificationAnswers(h.getCustomerVerificationAnswers())
                .adminReviewNotes(h.getAdminReviewNotes())
                .reviewedBy(h.getReviewedBy())
                .expiresAt(h.getExpiresAt())
                .createdAt(h.getCreatedAt())
                .updatedAt(h.getUpdatedAt())
                .build();
    }
}
