package in.ashish.combined.service.fraud;

import dev.langchain4j.model.chat.ChatLanguageModel;
import in.ashish.combined.config.GeminiConfiguration;
import in.ashish.combined.dto.fraud.FraudAssessmentResponse;
import in.ashish.combined.dto.fraud.TransactionRiskDto;
import in.ashish.combined.entity.fraud.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private GeminiConfiguration geminiConfiguration;

    private ScamAnalysisService scamAnalysisService;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        scamAnalysisService = new ScamAnalysisService();
        geminiService = new GeminiService(chatLanguageModel, geminiConfiguration, scamAnalysisService);
    }

    @Test
    @DisplayName("Gemini timeout and failure gracefully invokes safe fallback engine")
    void testGeminiTimeoutAndFailureFallback() {
        when(geminiConfiguration.getModelName()).thenReturn("gemini-1.5-flash");
        when(chatLanguageModel.chat(anyString())).thenThrow(new RuntimeException("Connection timed out (15000ms)"));

        TransactionRiskDto statRisk = TransactionRiskDto.builder()
                .riskLevel(RiskLevel.HIGH)
                .statisticalScore(65.0)
                .amountMultiplierOfAvg(8.0)
                .isFirstTimeRecipient(true)
                .isRapidSuccessiveTxn(false)
                .isUnusualTiming(false)
                .riskFactors(Collections.singletonList("ELEVATED_AMOUNT"))
                .build();

        FraudAssessmentResponse response = geminiService.assessRiskWithGemini(
                1L, 2L, new BigDecimal("80000.00"), "Direct transfer",
                "Police told me to send to safe account", statRisk);

        assertNotNull(response);
        // Risk must stay HIGH or CRITICAL because scam was detected in narrative and stat risk was HIGH
        assertTrue(response.getRiskLevel() == RiskLevel.HIGH || response.getRiskLevel() == RiskLevel.CRITICAL);
        assertTrue(response.isScamDetected());
        assertEquals("FALLBACK_RULE_ENGINE", response.getAiModelUsed());
    }

    @Test
    @DisplayName("Never automatically downgrade high statistical risk even if AI model returns LOW")
    void testNeverDowngradeHighRiskOnAiResponse() {
        when(geminiConfiguration.getModelName()).thenReturn("gemini-1.5-flash");

        // AI model returns a JSON claiming LOW risk
        String hallucinatedLowRiskJson = "{\n" +
                "  \"riskLevel\": \"LOW\",\n" +
                "  \"scamDetected\": false,\n" +
                "  \"scamCategories\": [],\n" +
                "  \"confidence\": 0.9,\n" +
                "  \"detectedIndicators\": [],\n" +
                "  \"explanation\": \"Seems normal\",\n" +
                "  \"customerWarning\": \"None\",\n" +
                "  \"recommendedAction\": \"PROCEED\",\n" +
                "  \"requiresHumanReview\": false\n" +
                "}";

        when(chatLanguageModel.chat(anyString())).thenReturn(hallucinatedLowRiskJson);

        TransactionRiskDto statRisk = TransactionRiskDto.builder()
                .riskLevel(RiskLevel.CRITICAL)
                .statisticalScore(95.0)
                .amountMultiplierOfAvg(50.0)
                .isFirstTimeRecipient(true)
                .riskFactors(Collections.singletonList("SUDDEN_LARGE_TRANSFER"))
                .build();

        FraudAssessmentResponse response = geminiService.assessRiskWithGemini(
                1L, 2L, new BigDecimal("500000.00"), "Transfer", null, statRisk);

        // Security rule: Statistical CRITICAL cannot be overridden to LOW by AI
        assertEquals(RiskLevel.CRITICAL, response.getRiskLevel());
        assertTrue(response.isRequiresHumanReview());
    }

    @Test
    @DisplayName("Invalid or malformed Gemini JSON safely activates fallback assessment")
    void testInvalidGeminiResponseHandling() {
        when(geminiConfiguration.getModelName()).thenReturn("gemini-1.5-flash");
        when(chatLanguageModel.chat(anyString())).thenReturn("Sorry, as an AI language model I cannot produce JSON right now.");

        TransactionRiskDto statRisk = TransactionRiskDto.builder()
                .riskLevel(RiskLevel.MEDIUM)
                .statisticalScore(35.0)
                .amountMultiplierOfAvg(2.5)
                .riskFactors(Collections.emptyList())
                .build();

        FraudAssessmentResponse response = geminiService.assessRiskWithGemini(
                1L, 2L, new BigDecimal("20000.00"), "Payment", null, statRisk);

        assertNotNull(response);
        assertEquals(RiskLevel.MEDIUM, response.getRiskLevel());
        assertEquals("FALLBACK_RULE_ENGINE", response.getAiModelUsed());
    }
}

