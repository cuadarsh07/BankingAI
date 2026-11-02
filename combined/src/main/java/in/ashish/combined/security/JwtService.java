package in.ashish.combined.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating, signing, parsing, and validating JSON Web Tokens (JWT) using HMAC-SHA256.
 *
 * Interview Concepts:
 * - JWT Structure:
 *     1. Header: Encodes algorithm and token type (e.g. {"alg":"HS256","typ":"JWT"}).
 *     2. Payload / Claims: Encodes subject, expiration, and user roles.
 *     3. Signature: HMAC-SHA256(Base64Url(Header) + "." + Base64Url(Payload), SecretKey).
 * - Stateless Authentication: The server does not store session IDs in RAM or Redis.
 *   The token is cryptographically verified on every request.
 * - Constant-Time Comparison: MessageDigest.isEqual() is used to verify the signature,
 *   preventing side-channel timing attacks where an attacker deduces signature bytes by measuring nanosecond response differences.
 */
@Slf4j
@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final String secretKey;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.jwt.secret:BankingBackendSecretKeyForInterviewDemoOnlyRequire256BitsMinimumLength123456789}") String secretKey,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate signed JWT token with user claims.
     */
    public String generateToken(String username, String role, Long accountId) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + expirationSeconds;

            // 1. Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            String encodedHeader = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));

            // 2. Payload Claims
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", username);
            payload.put("role", role);
            if (accountId != null) {
                payload.put("accountId", accountId);
            }
            payload.put("iat", now);
            payload.put("exp", exp);
            String encodedPayload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));

            // 3. Signature
            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(dataToSign);

            return dataToSign + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Could not generate JWT token", e);
        }
    }

    /**
     * Validate JWT token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT format: Token must have exactly 3 parts");
                return false;
            }

            String dataToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(dataToSign);

            // Constant-time signature verification
            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("JWT signature verification failed");
                return false;
            }

            // Verify Expiration
            byte[] payloadBytes = URL_DECODER.decode(parts[1]);
            JsonNode payloadJson = objectMapper.readTree(payloadBytes);
            long exp = payloadJson.path("exp").asLong(0);

            long now = Instant.now().getEpochSecond();
            if (now > exp) {
                log.warn("JWT token has expired. Current: {}, Exp: {}", now, exp);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract username (sub claim) from token.
     */
    public String extractUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = URL_DECODER.decode(parts[1]);
            JsonNode payloadJson = objectMapper.readTree(payloadBytes);
            return payloadJson.path("sub").asText();
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract role from token.
     */
    public String extractRole(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = URL_DECODER.decode(parts[1]);
            JsonNode payloadJson = objectMapper.readTree(payloadBytes);
            return payloadJson.path("role").asText();
        } catch (Exception e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return URL_ENCODER.encodeToString(hmacBytes);
    }
}
