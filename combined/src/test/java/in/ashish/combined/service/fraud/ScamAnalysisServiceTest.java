package in.ashish.combined.service.fraud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScamAnalysisServiceTest {

    private ScamAnalysisService scamAnalysisService;

    @BeforeEach
    void setUp() {
        scamAnalysisService = new ScamAnalysisService();
    }

    @Test
    @DisplayName("Normal transactions should not trigger scam detection")
    void testNormalTransactions() {
        var result1 = scamAnalysisService.analyzeText("Dinner split with friends", "Paying my portion of food");
        assertFalse(result1.isScamDetected());
        assertEquals(0, result1.getCategories().size());

        var result2 = scamAnalysisService.analyzeText("Monthly house rent payment", null);
        assertFalse(result2.isScamDetected());
    }

    @Test
    @DisplayName("Detect digital arrest scam messages in English")
    void testDigitalArrestInEnglish() {
        var result = scamAnalysisService.analyzeText(
                "Urgent transfer",
                "CBI officer on Skype video call said arrest warrant is issued, transfer to safe account immediately"
        );

        assertTrue(result.isScamDetected());
        assertTrue(result.getCategories().contains("DIGITAL_ARREST"));
        assertTrue(result.getCategories().contains("SAFE_ACCOUNT_SCAM"));
        assertTrue(result.getCategories().contains("URGENCY_PRESSURE"));
        assertTrue(result.getConfidence() >= 0.85);
        assertNotNull(result.getCustomerWarning());
    }

    @Test
    @DisplayName("Detect digital arrest scam messages in Hindi")
    void testDigitalArrestInHindi() {
        var result = scamAnalysisService.analyzeText(
                "पुलिस ने बोला है तुरंत पैसे ट्रांसफर करो वरना जेल भेज देंगे"
        );

        assertTrue(result.isScamDetected());
        assertTrue(result.getCategories().contains("DIGITAL_ARREST"));
    }

    @Test
    @DisplayName("Detect Hinglish scam messages with secrecy coercion")
    void testHinglishScamMessages() {
        var result = scamAnalysisService.analyzeText(
                "Police ne bola hai paise transfer karo, mere against case ho gaya hai, kisi ko mat batana"
        );

        assertTrue(result.isScamDetected());
        assertTrue(result.getCategories().contains("DIGITAL_ARREST"));
        assertTrue(result.getCategories().contains("SECRECY_COERCION"));
    }

    @Test
    @DisplayName("Detect remote access and credential harvesting apps")
    void testCredentialHarvesting() {
        var result = scamAnalysisService.analyzeText(
                "Bank support told me to install AnyDesk and share OTP for verification"
        );

        assertTrue(result.isScamDetected());
        assertTrue(result.getCategories().contains("CREDENTIAL_HARVESTING"));
    }

    @Test
    @DisplayName("Detect Telegram task and investment scams")
    void testInvestmentTaskScam() {
        var result = scamAnalysisService.analyzeText(
                "Telegram task scheme guaranteed profit double money in 24 hours"
        );

        assertTrue(result.isScamDetected());
        assertTrue(result.getCategories().contains("INVESTMENT_TASK_SCAM"));
    }
}

