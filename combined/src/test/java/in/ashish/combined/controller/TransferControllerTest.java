package in.ashish.combined.controller;

import in.ashish.combined.dto.TransferRequest;
import in.ashish.combined.dto.TransferResponse;
import in.ashish.combined.exception.InsufficientBalanceException;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice test for TransferController using @WebMvcTest and MockMvc.
 */
@WebMvcTest(
        controllers = TransferController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {in.ashish.combined.config.SecurityConfig.class, in.ashish.combined.security.JwtAuthenticationFilter.class}
        )
)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private in.ashish.combined.service.fraud.FraudDetectionService fraudDetectionService;

    @MockitoBean
    private in.ashish.combined.service.fraud.FraudPolicyService fraudPolicyService;

    @MockitoBean
    private in.ashish.combined.repository.UserRepository userRepository;

    @MockitoBean
    private in.ashish.combined.repository.AccountRepository accountRepository;

    @org.junit.jupiter.api.BeforeEach
    void setupFraudDefaults() {
        org.mockito.Mockito.lenient().when(fraudDetectionService.assessTransfer(any(), any(), any(), any(), any()))
                .thenReturn(in.ashish.combined.dto.fraud.FraudAssessmentResponse.builder()
                        .riskLevel(in.ashish.combined.entity.fraud.RiskLevel.LOW)
                        .scamDetected(false)
                        .requiresHumanReview(false)
                        .build());
        org.mockito.Mockito.lenient().when(fraudPolicyService.decideTransferAction(any(), anyBoolean(), anyBoolean()))
                .thenReturn(in.ashish.combined.service.fraud.FraudPolicyService.PolicyDecision.ALLOW_IMMEDIATE);
    }

    @Test
    @DisplayName("POST /api/transfers with valid payload should return 200 OK")
    void transferMoney_ValidPayload_Returns200OK() throws Exception {
        TransferResponse response = TransferResponse.builder()
                .transferId("TXN-12345678")
                .sourceAccountId(1L)
                .sourceAccountNumber("1001234567")
                .destinationAccountId(2L)
                .destinationAccountNumber("2001234567")
                .amount(new BigDecimal("500.00"))
                .sourceBalanceAfter(new BigDecimal("2500.00"))
                .status("SUCCESS")
                .description("Rent")
                .timestamp(LocalDateTime.now())
                .build();

        when(transferService.transferMoney(any(TransferRequest.class))).thenReturn(response);

        String payload = """
                {
                    "sourceAccountId": 1,
                    "destinationAccountId": 2,
                    "amount": 500.00,
                    "description": "Rent"
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transferId").value("TXN-12345678"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.sourceBalanceAfter").value(2500.00));
    }

    @Test
    @DisplayName("POST /api/transfers with negative amount should return 400 Bad Request")
    void transferMoney_NegativeAmount_Returns400BadRequest() throws Exception {
        String payload = """
                {
                    "sourceAccountId": 1,
                    "destinationAccountId": 2,
                    "amount": -50.00
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }

    @Test
    @DisplayName("POST /api/transfers with same account should return 400 Bad Request")
    void transferMoney_SameAccount_Returns400BadRequest() throws Exception {
        when(transferService.transferMoney(any(TransferRequest.class)))
                .thenThrow(new InvalidTransferException("Cannot transfer money to the same account."));

        String payload = """
                {
                    "sourceAccountId": 1,
                    "destinationAccountId": 1,
                    "amount": 100.00
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot transfer money to the same account."));
    }

    @Test
    @DisplayName("POST /api/transfers with insufficient funds should return 400 Bad Request")
    void transferMoney_InsufficientFunds_Returns400BadRequest() throws Exception {
        when(transferService.transferMoney(any(TransferRequest.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient funds"));

        String payload = """
                {
                    "sourceAccountId": 1,
                    "destinationAccountId": 2,
                    "amount": 999999.00
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    @DisplayName("POST /api/transfers with missing account should return 404 Not Found")
    void transferMoney_AccountNotFound_Returns404NotFound() throws Exception {
        when(transferService.transferMoney(any(TransferRequest.class)))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 99"));

        String payload = """
                {
                    "sourceAccountId": 1,
                    "destinationAccountId": 99,
                    "amount": 100.00
                }
                """;

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found with ID: 99"));
    }
}
