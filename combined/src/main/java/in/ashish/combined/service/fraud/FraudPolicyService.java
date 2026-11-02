package in.ashish.combined.service.fraud;

import in.ashish.combined.entity.fraud.HeldTransactionState;
import in.ashish.combined.entity.fraud.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Deterministic Policy Engine for Banking Fraud and Digital Arrest Prevention.
 *
 * Enforces banking principles:
 * - Deterministic policy overrides AI recommendations.
 * - Customer answering questions CANNOT automatically release CRITICAL-risk transfers.
 * - Confirmed scam coercion automatically halts automated release.
 */
@Slf4j
@Service
public class FraudPolicyService {

    public enum PolicyDecision {
        ALLOW_IMMEDIATE,
        HOLD_CUSTOMER_VERIFICATION,
        HOLD_BANK_REVIEW,
        REJECT
    }

    /**
     * Determines the initial workflow state for a transfer intent based on risk assessment.
     */
    public PolicyDecision decideTransferAction(RiskLevel riskLevel, boolean scamDetected, boolean requiresHumanReview) {
        if (riskLevel == RiskLevel.CRITICAL || requiresHumanReview) {
            log.warn("Policy Decision: Transfer requires Bank Administrative Review due to CRITICAL risk or review mandate.");
            return PolicyDecision.HOLD_BANK_REVIEW;
        }

        if (scamDetected || riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.MEDIUM) {
            log.info("Policy Decision: Transfer held for Customer Security Verification [Risk: {}, ScamDetected: {}].",
                    riskLevel, scamDetected);
            return PolicyDecision.HOLD_CUSTOMER_VERIFICATION;
        }

        log.debug("Policy Decision: Transfer permitted for immediate processing [Risk: LOW].");
        return PolicyDecision.ALLOW_IMMEDIATE;
    }

    /**
     * Reassesses a held transaction after customer submits verification answers.
     */
    public VerificationEvaluationResult evaluateVerificationAnswers(
            RiskLevel originalRiskLevel,
            Map<String, String> answers) {

        if (answers == null || answers.isEmpty()) {
            return new VerificationEvaluationResult(false, originalRiskLevel,
                    HeldTransactionState.PENDING_CUSTOMER_VERIFICATION,
                    "No verification answers provided.");
        }

        boolean activeScamConfirmed = false;
        StringBuilder scamTriggers = new StringBuilder();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String question = entry.getKey().toLowerCase();
            String answer = entry.getValue() != null ? entry.getValue().trim().toUpperCase() : "";

            // Check if user answered "YES" to coercion or scam questions
            if (answer.contains("YES") || answer.equals("Y") || answer.contains("HAAN") || answer.contains("TRUE")) {
                if (question.contains("police") || question.contains("cbi") || question.contains("ed") || question.contains("court")) {
                    activeScamConfirmed = true;
                    scamTriggers.append("Customer confirmed law enforcement / digital arrest coercion. ");
                } else if (question.contains("safe") || question.contains("verification account")) {
                    activeScamConfirmed = true;
                    scamTriggers.append("Customer confirmed safe verification account deception. ");
                } else if (question.contains("secret") || question.contains("disconnect") || question.contains("video call")) {
                    activeScamConfirmed = true;
                    scamTriggers.append("Customer confirmed intimidation or secrecy coercion. ");
                } else if (question.contains("anydesk") || question.contains("teamviewer") || question.contains("screen")) {
                    activeScamConfirmed = true;
                    scamTriggers.append("Customer confirmed remote screen sharing access. ");
                }
            }

            // Check if user answered "NO" to knowing the recipient personally
            if (question.contains("personally know") || question.contains("physically met")) {
                if (answer.contains("NO") || answer.equals("N") || answer.contains("NAHI") || answer.contains("FALSE")) {
                    scamTriggers.append("Customer does not personally know the recipient. ");
                }
            }
        }

        // If active scam coercion is confirmed by the customer's answers:
        if (activeScamConfirmed) {
            log.warn("Verification ALERT: Active scam confirmed by user responses! Upgrading to CRITICAL / PENDING_BANK_REVIEW.");
            return new VerificationEvaluationResult(
                    false,
                    RiskLevel.CRITICAL,
                    HeldTransactionState.PENDING_BANK_REVIEW,
                    "Active digital arrest or social engineering indicators confirmed. Transfer held for security review: " + scamTriggers
            );
        }

        // Mandatory Banking Rule: A customer answering questions CANNOT release a CRITICAL-risk transfer
        if (originalRiskLevel == RiskLevel.CRITICAL) {
            log.warn("Verification: Answers received, but CRITICAL-risk transfer cannot be self-released by customer.");
            return new VerificationEvaluationResult(
                    false,
                    RiskLevel.CRITICAL,
                    HeldTransactionState.PENDING_BANK_REVIEW,
                    "Critical risk transfers strictly require bank officer authorization prior to fund release."
            );
        }

        // For MEDIUM / HIGH risk where customer verified safety
        log.info("Verification: Customer successfully passed contextual security questions for {} risk transfer.", originalRiskLevel);
        return new VerificationEvaluationResult(
                true,
                originalRiskLevel,
                HeldTransactionState.APPROVED,
                "Customer successfully confirmed transfer legitimacy and denied any scam coercion."
        );
    }

    public record VerificationEvaluationResult(
            boolean passed,
            RiskLevel finalRiskLevel,
            HeldTransactionState nextState,
            String message
    ) {}
}

