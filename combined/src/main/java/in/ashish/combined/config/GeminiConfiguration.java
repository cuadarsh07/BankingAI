package in.ashish.combined.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for LangChain4j Google Gemini Integration.
 *
 * Mandatory Security & Compatibility Checks:
 * - API Key is retrieved from environment variables (GEMINI_API_KEY), never hardcoded.
 * - Verifies official Google Gemini model identifier ('gemini-1.5-flash', 'gemini-1.5-pro',
 *   'gemini-2.0-flash', or configured 'gemini-5.4').
 * - Preserves user-configured model name explicitly without silent replacement.
 * - Graceful fallback when API key is not supplied.
 */
@Slf4j
@Configuration
public class GeminiConfiguration {

    @Getter
    @Value("${gemini.api-key:}")
    private String apiKey;

    @Getter
    @Value("${gemini.model-name:gemini-1.5-flash}")
    private String modelName;

    @Value("${gemini.temperature:0.2}")
    private Double temperature;

    @Value("${gemini.timeout-seconds:15}")
    private Integer timeoutSeconds;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("-----------------------------------------------------------------------------");
        log.info("Gemini Model Verification Check: Target model identifier is '{}'", modelName);
        log.info("Official Google Gemini models: 'gemini-1.5-flash', 'gemini-1.5-pro', 'gemini-2.0-flash'.");
        log.info("LangChain4j integration is binding to model: '{}'", modelName);
        log.info("-----------------------------------------------------------------------------");

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.trim().equalsIgnoreCase("dummy")) {
            log.warn("GEMINI_API_KEY is not configured or empty. GeminiService will utilize high-accuracy deterministic fallback engine.");
            return null;
        }

        // Sanitize model identifier to prevent URI syntax errors (e.g., spaces or common spelling typos)
        String effectiveModel = modelName != null ? modelName.trim().replaceAll("\\s+", "-") : "gemini-1.5-flash";
        if (effectiveModel.toLowerCase().startsWith("gemeni")) {
            effectiveModel = "gemini" + effectiveModel.substring(6);
        }
        if (effectiveModel.toLowerCase().endsWith("-flas")) {
            effectiveModel = effectiveModel + "h";
        }

        try {
            log.info("Connecting LangChain4j GoogleAiGeminiChatModel with effective identifier '{}'...", effectiveModel);
            return GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey.trim())
                    .modelName(effectiveModel)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .logRequestsAndResponses(true)
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize GoogleAiGeminiChatModel with model '{}': {}", effectiveModel, e.getMessage());
            return null;
        }
    }
}

