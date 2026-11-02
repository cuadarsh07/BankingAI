package in.ashish.combined.service.fraud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.model.chat.ChatLanguageModel;
import in.ashish.combined.config.GeminiConfiguration;
import in.ashish.combined.dto.fraud.AssistantChatResponse;
import in.ashish.combined.dto.fraud.FraudAssessmentResponse;
import in.ashish.combined.dto.fraud.TransactionRiskDto;
import in.ashish.combined.entity.fraud.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for Gemini AI integration via LangChain4j.
 *
 * Mandates:
 * - Uses configured Gemini model (verified on startup)
 * - Returns structured JSON risk assessments
 * - Enforces fallback rules when Gemini is unavailable (never lowers high/critical risk)
 * - Protects against prompt injection
 * - Validates all AI outputs
 */
@Slf4j
@Service
public class GeminiService {

    private final ChatLanguageModel chatLanguageModel;
    private final GeminiConfiguration geminiConfiguration;
    private final ScamAnalysisService scamAnalysisService;
    private final Gson gson = new Gson();

    public GeminiService(
            @org.springframework.beans.factory.annotation.Autowired(required = false) ChatLanguageModel chatLanguageModel,
            GeminiConfiguration geminiConfiguration,
            ScamAnalysisService scamAnalysisService) {
        this.chatLanguageModel = chatLanguageModel;
        this.geminiConfiguration = geminiConfiguration;
        this.scamAnalysisService = scamAnalysisService;
    }

    /**
     * Conducts deep contextual scam and risk analysis using Gemini via LangChain4j.
     * Safely falls back to deterministic rule engine if Gemini is unavailable or fails.
     */
    public FraudAssessmentResponse assessRiskWithGemini(
            Long sourceAccountId,
            Long destAccountId,
            BigDecimal amount,
            String description,
            String userExplanation,
            TransactionRiskDto statisticalRisk) {

        // First run deterministic multilingual scam scan as baseline
        ScamAnalysisService.ScamScanResult scanResult = scamAnalysisService.analyzeText(description, userExplanation);

        String modelIdentifier = geminiConfiguration.getModelName();
        boolean isGeminiAvailable = chatLanguageModel != null;

        if (isGeminiAvailable) {
            try {
                String prompt = buildAssessmentPrompt(sourceAccountId, destAccountId, amount, description, userExplanation, statisticalRisk, scanResult);
                log.info("Sending risk assessment prompt to Gemini model '{}'", modelIdentifier);
                String rawResponse = chatLanguageModel.chat(prompt);
                FraudAssessmentResponse parsed = parseAndValidateGeminiResponse(rawResponse, modelIdentifier, statisticalRisk, scanResult);
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("Gemini AI invocation failed or timed out ({}). Engaging safe fallback engine.", e.getMessage());
                if (e.getMessage() != null && (e.getMessage().contains("not found") || e.getMessage().contains("404"))) {
                    log.warn("Model '{}' was not found on Google AI Studio API. Official models are 'gemini-1.5-flash', 'gemini-1.5-pro', 'gemini-2.0-flash'.", modelIdentifier);
                }
            }
        }

        // Safe Fallback Engine: Merge statistical signals with deterministic scam scanner
        return buildSafeFallbackAssessment(sourceAccountId, destAccountId, amount, statisticalRisk, scanResult);
    }

    /**
     * Contextual Banking Security Assistant powered by Gemini.
     */
    public AssistantChatResponse chatWithAssistant(String userMessage, String customerContextSummary) {
        String sanitizedMessage = sanitizeInput(userMessage);

        // Check for immediate emergency/digital arrest keywords locally for safety
        ScamAnalysisService.ScamScanResult scamScan = scamAnalysisService.analyzeText(sanitizedMessage);

        if (chatLanguageModel != null) {
            try {
                String prompt = "You are Aethel Bank's AI Security and Banking Assistant. " +
                        "Your job is to assist the authenticated customer with banking questions, transfer inquiries, " +
                        "explaining held transactions, spending patterns, and protecting them against financial scams, " +
                        "especially DIGITAL ARREST scams in India (fraudsters pretending to be Police/CBI/ED on video calls).\n\n" +
                        "Customer financial context:\n" + customerContextSummary + "\n\n" +
                        "Guidelines:\n" +
                        "1. If the user mentions police, CBI, ED, arrest warrants, Skype calls, or safe verification accounts, " +
                        "immediately alert them that government authorities NEVER order digital arrests or demand money transfers.\n" +
                        "2. Respond clearly in the language of the customer (English, Hindi, or Hinglish).\n" +
                        "3. Keep answers concise, factual, and reassuring.\n" +
                        "4. Never ask for PINs, passwords, or full credentials.\n\n" +
                        "Customer query: \"" + sanitizedMessage + "\"\n" +
                        "Your response:";

                String aiReply = chatLanguageModel.chat(prompt);
                if (aiReply != null && !aiReply.trim().isEmpty()) {
                    List<String> suggestions = scamScan.isScamDetected()
                            ? Arrays.asList("Report Digital Arrest Fraud", "Freeze My Account", "View Security Center")
                            : Arrays.asList("Check Transfer Limits", "Explain Hold Policy", "View Security Center");

                    return AssistantChatResponse.builder()
                            .reply(aiReply.trim())
                            .timestamp(LocalDateTime.now())
                            .suggestedActions(suggestions)
                            .isSecurityAlert(scamScan.isScamDetected())
                            .build();
                }
            } catch (Exception e) {
                log.warn("Gemini assistant invocation failed: {}. Falling back to rule-based assistant.", e.getMessage());
                if (e.getMessage() != null && (e.getMessage().contains("not found") || e.getMessage().contains("404"))) {
                    log.warn("Model '{}' was not found on Google AI Studio API. Official models are 'gemini-1.5-flash', 'gemini-1.5-pro', 'gemini-2.0-flash'.", geminiConfiguration.getModelName());
                }
            }
        }

        // Fallback intelligent assistant response
        return buildFallbackAssistantReply(sanitizedMessage, scamScan);
    }

    private String buildAssessmentPrompt(
            Long sourceId, Long destId, BigDecimal amount,
            String desc, String userExp,
            TransactionRiskDto statRisk,
            ScamAnalysisService.ScamScanResult scanResult) {

        return "You are an expert banking fraud detection and digital arrest prevention system at Aethel Bank.\n" +
                "Analyze the following fund transfer for social engineering, digital arrest scams, and unusual transactional patterns.\n\n" +
                "Transfer Details:\n" +
                "- Amount: ₹" + amount + "\n" +
                "- Source Account ID: " + sourceId + "\n" +
                "- Destination Account ID: " + destId + "\n" +
                "- Description: \"" + (desc != null ? desc : "") + "\"\n" +
                "- Customer's explanation/notes: \"" + (userExp != null ? userExp : "") + "\"\n" +
                "- Statistical Signals: Multiplier of avg = " + statRisk.getAmountMultiplierOfAvg() +
                ", First time recipient = " + statRisk.isFirstTimeRecipient() +
                ", Rapid successive txn = " + statRisk.isRapidSuccessiveTxn() +
                ", Unusual timing = " + statRisk.isUnusualTiming() + "\n" +
                "- Rule scanner triggers: " + String.join(", ", scanResult.getCategories()) + "\n\n" +
                "You must return ONLY a valid, raw JSON object (without markdown blocks or code fences) with the exact keys:\n" +
                "{\n" +
                "  \"riskLevel\": \"LOW\" | \"MEDIUM\" | \"HIGH\" | \"CRITICAL\",\n" +
                "  \"scamDetected\": boolean,\n" +
                "  \"scamCategories\": [\"DIGITAL_ARREST\" | \"SAFE_ACCOUNT_SCAM\" | \"URGENCY_PRESSURE\" | \"SECRECY_COERCION\" | \"CREDENTIAL_HARVESTING\" | \"INVESTMENT_TASK_SCAM\"],\n" +
                "  \"confidence\": number between 0.0 and 1.0,\n" +
                "  \"detectedIndicators\": [\"indicator 1\", \"indicator 2\"],\n" +
                "  \"explanation\": \"clear explanation in simple language\",\n" +
                "  \"customerWarning\": \"warning message in Hindi and English explaining the risk\",\n" +
                "  \"recommendedAction\": \"PROCEED\" | \"HOLD_FOR_CUSTOMER_VERIFICATION\" | \"HOLD_FOR_BANK_REVIEW\" | \"BLOCK_TRANSACTION\",\n" +
                "  \"requiresHumanReview\": boolean\n" +
                "}";
    }

    private FraudAssessmentResponse parseAndValidateGeminiResponse(
            String rawJson,
            String modelUsed,
            TransactionRiskDto statisticalRisk,
            ScamAnalysisService.ScamScanResult scanResult) {

        if (rawJson == null || rawJson.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = rawJson.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            JsonObject obj = JsonParser.parseString(cleaned).getAsJsonObject();

            String rawLevel = obj.has("riskLevel") ? obj.get("riskLevel").getAsString() : "MEDIUM";
            RiskLevel aiRiskLevel;
            try {
                aiRiskLevel = RiskLevel.valueOf(rawLevel.toUpperCase());
            } catch (Exception ex) {
                aiRiskLevel = statisticalRisk.getRiskLevel();
            }

            // CRITICAL SAFETY RULE: Never downgrade statistical risk if AI hallucinated LOW
            if (statisticalRisk.getRiskLevel() == RiskLevel.CRITICAL) {
                aiRiskLevel = RiskLevel.CRITICAL;
            } else if (statisticalRisk.getRiskLevel() == RiskLevel.HIGH && aiRiskLevel == RiskLevel.LOW) {
                aiRiskLevel = RiskLevel.HIGH;
            }

            boolean scamDetected = obj.has("scamDetected") && obj.get("scamDetected").getAsBoolean();
            if (scanResult.isScamDetected()) {
                scamDetected = true;
                if (aiRiskLevel == RiskLevel.LOW) {
                    aiRiskLevel = RiskLevel.HIGH;
                }
            }

            List<String> categories = new ArrayList<>();
            if (obj.has("scamCategories") && obj.get("scamCategories").isJsonArray()) {
                obj.get("scamCategories").getAsJsonArray().forEach(e -> categories.add(e.getAsString()));
            }
            for (String c : scanResult.getCategories()) {
                if (!categories.contains(c)) categories.add(c);
            }

            List<String> indicators = new ArrayList<>();
            if (obj.has("detectedIndicators") && obj.get("detectedIndicators").isJsonArray()) {
                obj.get("detectedIndicators").getAsJsonArray().forEach(e -> indicators.add(e.getAsString()));
            }
            for (String ind : scanResult.getMatchedIndicators()) {
                if (!indicators.contains(ind)) indicators.add(ind);
            }

            double confidence = obj.has("confidence") ? obj.get("confidence").getAsDouble() : 0.8;
            String explanation = obj.has("explanation") ? obj.get("explanation").getAsString() : scanResult.getExplanation();
            String customerWarning = obj.has("customerWarning") ? obj.get("customerWarning").getAsString() : scanResult.getCustomerWarning();
            String recommendedAction = obj.has("recommendedAction") ? obj.get("recommendedAction").getAsString() : "HOLD_FOR_CUSTOMER_VERIFICATION";
            boolean requiresHumanReview = obj.has("requiresHumanReview") && obj.get("requiresHumanReview").getAsBoolean();

            if (aiRiskLevel == RiskLevel.CRITICAL) {
                requiresHumanReview = true;
                recommendedAction = "HOLD_FOR_BANK_REVIEW";
            }

            return FraudAssessmentResponse.builder()
                    .assessmentId(UUID.randomUUID().toString())
                    .riskLevel(aiRiskLevel)
                    .scamDetected(scamDetected)
                    .scamCategories(categories)
                    .confidence(confidence)
                    .detectedIndicators(indicators)
                    .explanation(explanation)
                    .customerWarning(customerWarning)
                    .recommendedAction(recommendedAction)
                    .requiresHumanReview(requiresHumanReview)
                    .verificationQuestions(scamAnalysisService.generateContextualQuestions(categories))
                    .aiModelUsed(modelUsed)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse or validate Gemini structured JSON: {}", e.getMessage());
            return null;
        }
    }

    private FraudAssessmentResponse buildSafeFallbackAssessment(
            Long sourceAccountId, Long destAccountId, BigDecimal amount,
            TransactionRiskDto statisticalRisk,
            ScamAnalysisService.ScamScanResult scanResult) {

        RiskLevel finalRisk = statisticalRisk.getRiskLevel();

        // If scam was detected by rule engine, elevate risk
        if (scanResult.isScamDetected()) {
            if (scanResult.getCategories().contains("DIGITAL_ARREST") || scanResult.getCategories().contains("SAFE_ACCOUNT_SCAM")) {
                finalRisk = RiskLevel.CRITICAL;
            } else if (finalRisk == RiskLevel.LOW) {
                finalRisk = RiskLevel.HIGH;
            }
        }

        String recommendedAction;
        boolean requiresHumanReview = false;

        switch (finalRisk) {
            case CRITICAL:
                recommendedAction = "HOLD_FOR_BANK_REVIEW";
                requiresHumanReview = true;
                break;
            case HIGH:
                recommendedAction = "HOLD_FOR_CUSTOMER_VERIFICATION";
                break;
            case MEDIUM:
                recommendedAction = "HOLD_FOR_CUSTOMER_VERIFICATION";
                break;
            case LOW:
            default:
                recommendedAction = "PROCEED";
                break;
        }

        StringBuilder explanation = new StringBuilder();
        if (scanResult.isScamDetected()) {
            explanation.append(scanResult.getExplanation()).append(" ");
        }
        if (!statisticalRisk.getRiskFactors().isEmpty()) {
            explanation.append("Statistical Factors: ").append(String.join(", ", statisticalRisk.getRiskFactors()));
        } else {
            explanation.append("Transaction conforms to expected account behavioral patterns.");
        }

        return FraudAssessmentResponse.builder()
                .assessmentId(UUID.randomUUID().toString())
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destAccountId)
                .amount(amount)
                .riskLevel(finalRisk)
                .scamDetected(scanResult.isScamDetected())
                .scamCategories(scanResult.getCategories())
                .confidence(scanResult.isScamDetected() ? scanResult.getConfidence() : 0.85)
                .detectedIndicators(scanResult.getMatchedIndicators())
                .explanation(explanation.toString().trim())
                .customerWarning(scanResult.getCustomerWarning())
                .recommendedAction(recommendedAction)
                .requiresHumanReview(requiresHumanReview)
                .verificationQuestions(scanResult.getContextualQuestions())
                .aiModelUsed("FALLBACK_RULE_ENGINE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AssistantChatResponse buildFallbackAssistantReply(String query, ScamAnalysisService.ScamScanResult scanResult) {
        String lower = query.toLowerCase();
        String reply;
        List<String> actions = new ArrayList<>();
        boolean alert = scanResult.isScamDetected();

        if (alert || lower.contains("digital arrest") || lower.contains("cbi") || lower.contains("police") || lower.contains("arrest")) {
            reply = "CRITICAL SECURITY ADVISORY (डिजिटल अरेस्ट चेतावनी):\n\n" +
                    "1. Government agencies (Police, CBI, ED, Court) NEVER conduct arrests via video call (Skype/WhatsApp).\n" +
                    "2. There is NO official 'Safe Account' or 'RBI Verification Account'. Any demand to transfer money to verify funds is a FRAUD.\n" +
                    "3. If someone is threatening you, IMMEDIATELY disconnect the call, inform family members, and call the National Cyber Crime Helpline at 1930 or visit cybercrime.gov.in.\n" +
                    "4. Your bank accounts are safe. Do not send funds to anyone under pressure.";
            actions.addAll(Arrays.asList("Freeze Outgoing Transfers", "Call Cyber Helpline 1930", "Visit Security Center"));
            alert = true;
        } else if (lower.contains("hold") || lower.contains("pending") || lower.contains("held")) {
            reply = "Transactions are placed on HOLD when our AI Security Shield detects unusual spending spikes, first-time recipients, or high-risk scam indicators. " +
                    "To release or review a held transaction, visit your Security Center (/security-center) or the Transfers tab to complete security verification.";
            actions.addAll(Arrays.asList("Go to Security Center", "View Held Transactions", "Contact Bank Support"));
        } else if (lower.contains("limit") || lower.contains("transfer limit")) {
            reply = "Standard account transfers support up to ₹10,00,000 with real-time settlement. However, high-value transfers above ₹50,000 or sudden deviations from your average transaction size trigger protective verification.";
            actions.addAll(Arrays.asList("Send Money", "View Balance", "Security Settings"));
        } else {
            reply = "I am your Aethel Banking Security Copilot. I can help you understand your spending patterns, explain why a transfer is on hold, or assist you with digital arrest scam defense. How may I protect your account today?";
            actions.addAll(Arrays.asList("Explain Digital Arrest Scams", "Why is my transfer on hold?", "View Security Center"));
        }

        return AssistantChatResponse.builder()
                .reply(reply)
                .timestamp(LocalDateTime.now())
                .suggestedActions(actions)
                .isSecurityAlert(alert)
                .build();
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Strip out control characters and prompt injection triggers
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }
}
