package in.ashish.combined.controller;

import in.ashish.combined.dto.fraud.FraudAssessmentResponse;
import in.ashish.combined.dto.fraud.HeldTransactionResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.User;
import in.ashish.combined.entity.fraud.HeldTransactionState;
import in.ashish.combined.entity.fraud.RiskLevel;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.service.fraud.FraudAlertService;
import in.ashish.combined.service.fraud.FraudDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FraudController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {in.ashish.combined.config.SecurityConfig.class, in.ashish.combined.security.JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class FraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FraudDetectionService fraudDetectionService;

    @MockitoBean
    private FraudAlertService fraudAlertService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /api/fraud/assessments returns 200 OK with risk assessment")
    void testAssessTransferRisk() throws Exception {
        FraudAssessmentResponse response = FraudAssessmentResponse.builder()
                .assessmentId("ASSESS-123")
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("50000.00"))
                .riskLevel(RiskLevel.HIGH)
                .scamDetected(true)
                .scamCategories(Collections.singletonList("DIGITAL_ARREST"))
                .customerWarning("Scam detected warning")
                .build();

        when(fraudDetectionService.assessTransfer(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(response);

        String payload = """
                {
                    "sourceAccountId": 1,
                    "destinationAccountId": 2,
                    "amount": 50000.00,
                    "description": "Urgent transfer",
                    "userExplanation": "Police said to transfer"
                }
                """;

        mockMvc.perform(post("/api/fraud/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.scamDetected").value(true));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("POST /api/fraud/transactions/{id}/verify verifies and releases held transfer")
    void testVerifyHeldTransaction() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("customer@example.com")
                .role(Role.ROLE_CUSTOMER)
                .account(Account.builder().id(1L).build())
                .build();

        when(userRepository.findByUsername("customer@example.com")).thenReturn(Optional.of(user));

        HeldTransactionResponse response = HeldTransactionResponse.builder()
                .id(10L)
                .referenceNumber("HOLD-12345678")
                .state(HeldTransactionState.SUCCESS)
                .riskLevel(RiskLevel.HIGH)
                .amount(new BigDecimal("25000.00"))
                .build();

        when(fraudDetectionService.verifyHeldTransaction(eq(10L), eq(user), anyMap()))
                .thenReturn(response);

        String payload = """
                {
                    "answers": {
                        "Did someone claiming to be police contact you?": "NO",
                        "Do you personally know recipient?": "YES"
                    }
                }
                """;

        mockMvc.perform(post("/api/fraud/transactions/10/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("POST /api/fraud/transactions/{id}/cancel cancels held transfer")
    void testCancelHeldTransaction() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("customer@example.com")
                .role(Role.ROLE_CUSTOMER)
                .account(Account.builder().id(1L).build())
                .build();

        when(userRepository.findByUsername("customer@example.com")).thenReturn(Optional.of(user));

        HeldTransactionResponse response = HeldTransactionResponse.builder()
                .id(10L)
                .referenceNumber("HOLD-12345678")
                .state(HeldTransactionState.CANCELLED)
                .amount(new BigDecimal("25000.00"))
                .build();

        when(fraudDetectionService.cancelHeldTransaction(eq(10L), eq(user)))
                .thenReturn(response);

        mockMvc.perform(post("/api/fraud/transactions/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED"));
    }
}

