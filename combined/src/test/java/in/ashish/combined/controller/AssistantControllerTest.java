package in.ashish.combined.controller;

import in.ashish.combined.dto.fraud.AssistantChatResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.User;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.service.fraud.FraudAlertService;
import in.ashish.combined.service.fraud.FraudDetectionService;
import in.ashish.combined.service.fraud.GeminiService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AssistantController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {in.ashish.combined.config.SecurityConfig.class, in.ashish.combined.security.JwtAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiService geminiService;

    @MockitoBean
    private FraudDetectionService fraudDetectionService;

    @MockitoBean
    private FraudAlertService fraudAlertService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "customer1")
    @DisplayName("POST /api/assistant/chat returns 200 OK with AI response")
    void testChatEndpointWithAccount() throws Exception {
        Account mockAccount = Account.builder()
                .id(1L)
                .accountNumber("1001234567")
                .balance(new BigDecimal("25000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        User mockUser = User.builder()
                .id(1L)
                .username("customer1")
                .fullName("Rajesh Kumar")
                .role(Role.ROLE_CUSTOMER)
                .account(mockAccount)
                .build();

        when(userRepository.findByUsernameWithAccount("customer1")).thenReturn(Optional.of(mockUser));
        when(fraudDetectionService.getHeldTransactionsForAccount(1L)).thenReturn(Collections.emptyList());
        when(fraudAlertService.getAlertsForUser(1L)).thenReturn(Collections.emptyList());

        AssistantChatResponse response = AssistantChatResponse.builder()
                .reply("I am your Aethel Banking Security Copilot.")
                .timestamp(LocalDateTime.now())
                .suggestedActions(Arrays.asList("Explain Digital Arrest Scams", "Why is my transfer on hold?"))
                .isSecurityAlert(false)
                .build();

        when(geminiService.chatWithAssistant(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("I am your Aethel Banking Security Copilot."))
                .andExpect(jsonPath("$.isSecurityAlert").value(false));
    }

    @Test
    @WithMockUser(username = "admin1")
    @DisplayName("POST /api/assistant/chat works when user has no account (e.g. admin)")
    void testChatEndpointWithoutAccount() throws Exception {
        User mockUser = User.builder()
                .id(2L)
                .username("admin1")
                .fullName("Admin User")
                .role(Role.ROLE_ADMIN)
                .account(null)
                .build();

        when(userRepository.findByUsernameWithAccount("admin1")).thenReturn(Optional.of(mockUser));

        AssistantChatResponse response = AssistantChatResponse.builder()
                .reply("Hello Admin. All banking security monitoring systems are active.")
                .timestamp(LocalDateTime.now())
                .isSecurityAlert(false)
                .build();

        when(geminiService.chatWithAssistant(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello Admin. All banking security monitoring systems are active."));
    }
}

