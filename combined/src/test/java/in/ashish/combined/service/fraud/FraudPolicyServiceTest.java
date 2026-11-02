package in.ashish.combined.service.fraud;

import in.ashish.combined.entity.fraud.HeldTransactionState;
import in.ashish.combined.entity.fraud.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FraudPolicyServiceTest {

    private FraudPolicyService fraudPolicyService;

    @BeforeEach
    void setUp() {
        fraudPolicyService = new FraudPolicyService();
    }

    @Test
    @DisplayName("Deterministic policy: Low risk allows immediate transfer, High/Critical hold transfer")
    void testInitialPolicyDecisions() {
        assertEquals(FraudPolicyService.PolicyDecision.ALLOW_IMMEDIATE,
                fraudPolicyService.decideTransferAction(RiskLevel.LOW, false, false));

        assertEquals(FraudPolicyService.PolicyDecision.HOLD_CUSTOMER_VERIFICATION,
                fraudPolicyService.decideTransferAction(RiskLevel.HIGH, false, false));

        assertEquals(FraudPolicyService.PolicyDecision.HOLD_BANK_REVIEW,
                fraudPolicyService.decideTransferAction(RiskLevel.CRITICAL, false, true));

        // If scam detected even at low risk, hold for customer verification
        assertEquals(FraudPolicyService.PolicyDecision.HOLD_CUSTOMER_VERIFICATION,
                fraudPolicyService.decideTransferAction(RiskLevel.LOW, true, false));
    }

    @Test
    @DisplayName("Customer answering questions must NOT automatically release a critical-risk transfer")
    void testCriticalRiskCannotBeSelfReleased() {
        Map<String, String> answers = new HashMap<>();
        answers.put("Did anyone claiming to be police contact you?", "NO");
        answers.put("Do you personally know the recipient?", "YES");

        var result = fraudPolicyService.evaluateVerificationAnswers(RiskLevel.CRITICAL, answers);

        assertFalse(result.passed());
        assertEquals(RiskLevel.CRITICAL, result.finalRiskLevel());
        assertEquals(HeldTransactionState.PENDING_BANK_REVIEW, result.nextState());
        assertTrue(result.message().contains("bank officer authorization"));
    }

    @Test
    @DisplayName("Confirming scam or authority pressure in answers immediately halts transfer and upgrades to CRITICAL")
    void testActiveScamConfirmedInAnswers() {
        Map<String, String> answers = new HashMap<>();
        answers.put("Did anyone claiming to be from Police or CBI instruct you?", "YES");
        answers.put("Do you personally know the recipient?", "NO");

        var result = fraudPolicyService.evaluateVerificationAnswers(RiskLevel.MEDIUM, answers);

        assertFalse(result.passed());
        assertEquals(RiskLevel.CRITICAL, result.finalRiskLevel());
        assertEquals(HeldTransactionState.PENDING_BANK_REVIEW, result.nextState());
        assertTrue(result.message().contains("Active digital arrest"));
    }

    @Test
    @DisplayName("Clean verification answers on High-risk transfer permits release")
    void testHighRiskVerificationPasses() {
        Map<String, String> answers = new HashMap<>();
        answers.put("Did anyone claiming to be from Police or CBI instruct you?", "NO");
        answers.put("Were you told this is a safe verification account?", "NO");
        answers.put("Do you personally know and have physically met the recipient?", "YES");

        var result = fraudPolicyService.evaluateVerificationAnswers(RiskLevel.HIGH, answers);

        assertTrue(result.passed());
        assertEquals(HeldTransactionState.APPROVED, result.nextState());
    }
}

