package in.ashish.combined.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "TestingSecretKeyForJwtUnitTestsMustBeAtLeast256BitsLength123456789";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secretKey, 3600, new ObjectMapper());
    }

    @Test
    @DisplayName("Should generate a valid 3-part JWT token")
    void generateToken_Success() {
        String token = jwtService.generateToken("john@example.com", "ROLE_CUSTOMER", 1L);

        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("Should validate genuine token and extract username and role correctly")
    void validateToken_AndExtractClaims_Success() {
        String token = jwtService.generateToken("alice@example.com", "ROLE_ADMIN", null);

        assertTrue(jwtService.validateToken(token));
        assertEquals("alice@example.com", jwtService.extractUsername(token));
        assertEquals("ROLE_ADMIN", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should reject tampered token with altered payload")
    void validateToken_TamperedPayload_ReturnsFalse() {
        String token = jwtService.generateToken("user@example.com", "ROLE_CUSTOMER", 5L);
        String[] parts = token.split("\\.");

        // Tamper with payload
        String tamperedToken = parts[0] + ".dGFtcGVyZWRfcGF5bG9hZA." + parts[2];

        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject expired token")
    void validateToken_Expired_ReturnsFalse() {
        // Expiration of -10 seconds (already expired)
        JwtService expiredJwtService = new JwtService(secretKey, -10, new ObjectMapper());
        String token = expiredJwtService.generateToken("expired@example.com", "ROLE_CUSTOMER", 1L);

        assertFalse(jwtService.validateToken(token));
    }
}
