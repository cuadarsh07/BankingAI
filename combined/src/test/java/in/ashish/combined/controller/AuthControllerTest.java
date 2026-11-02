package in.ashish.combined.controller;

import in.ashish.combined.dto.AuthRequest;
import in.ashish.combined.dto.AuthResponse;
import in.ashish.combined.dto.RegisterRequest;
import in.ashish.combined.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {in.ashish.combined.config.SecurityConfig.class, in.ashish.combined.security.JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register with valid payload should return 201 Created and JWT")
    void register_ValidPayload_Returns201Created() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("jwt.sample.token")
                .tokenType("Bearer")
                .username("newuser@example.com")
                .role("ROLE_CUSTOMER")
                .accountId(1L)
                .accountNumber("1234567890")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "username": "newuser@example.com",
                    "password": "Password123",
                    "fullName": "New User",
                    "initialDeposit": 500.00
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.sample.token"))
                .andExpect(jsonPath("$.username").value("newuser@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.accountId").value(1));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials should return 200 OK and JWT")
    void login_ValidCredentials_Returns200OK() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("jwt.login.token")
                .tokenType("Bearer")
                .username("user@example.com")
                .role("ROLE_CUSTOMER")
                .build();

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "username": "user@example.com",
                    "password": "Password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.login.token"))
                .andExpect(jsonPath("$.username").value("user@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login with incorrect credentials should return 401 Unauthorized")
    void login_InvalidCredentials_Returns401Unauthorized() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        String jsonPayload = """
                {
                    "username": "user@example.com",
                    "password": "WrongPassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
