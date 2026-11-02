package in.ashish.combined.controller;

import in.ashish.combined.dto.AccountResponse;
import in.ashish.combined.dto.CreateAccountRequest;
import in.ashish.combined.dto.PatchAccountRequest;
import in.ashish.combined.dto.UpdateAccountRequest;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.exception.DuplicateResourceException;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice test using @WebMvcTest and MockMvc.
 *
 * Interview Concepts:
 * - @WebMvcTest: Only loads the web layer (Controllers, ExceptionHandlers, Filters) without starting the full server or JPA repositories.
 * - MockMvc: Simulates HTTP requests (GET, POST, PUT, PATCH, headers, status codes, JSON response parsing).
 * - Testing HTTP Contracts: Verifies 201 Created with Location header, 200 OK, 400 Bad Request, 404 Not Found, 409 Conflict, and 415 Unsupported Media Type.
 */
@WebMvcTest(
        controllers = AccountController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {in.ashish.combined.config.SecurityConfig.class, in.ashish.combined.security.JwtAuthenticationFilter.class}
        )
)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("POST /api/accounts should return 201 Created and Location header on valid request")
    void createAccount_ValidPayload_Returns201Created() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountHolderName("Ashish Kumar")
                .email("ashish@example.com")
                .balance(new BigDecimal("2500.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

        String payload = """
                {
                    "accountNumber": "1234567890",
                    "accountHolderName": "Ashish Kumar",
                    "email": "ashish@example.com",
                    "balance": 2500.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/accounts/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accountHolderName").value("Ashish Kumar"))
                .andExpect(jsonPath("$.balance").value(2500.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/accounts with negative balance should return 400 Bad Request")
    void createAccount_NegativeBalance_Returns400BadRequest() throws Exception {
        String invalidPayload = """
                {
                    "accountNumber": "1234567890",
                    "accountHolderName": "Ashish Kumar",
                    "email": "ashish@example.com",
                    "balance": -100.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.balance").exists());
    }

    @Test
    @DisplayName("POST /api/accounts with invalid account holder name (digits) should return 400 Bad Request")
    void createAccount_InvalidNamePattern_Returns400BadRequest() throws Exception {
        String invalidPayload = """
                {
                    "accountNumber": "1234567890",
                    "accountHolderName": "Ashish12345",
                    "email": "ashish@example.com",
                    "balance": 500.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.accountHolderName").exists());
    }

    @Test
    @DisplayName("POST /api/accounts with invalid email syntax should return 400 Bad Request")
    void createAccount_InvalidEmailFormat_Returns400BadRequest() throws Exception {
        String invalidPayload = """
                {
                    "accountNumber": "1234567890",
                    "accountHolderName": "Ashish Kumar",
                    "email": "invalid-email-address",
                    "balance": 500.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/accounts with duplicate account number should return 400 Bad Request")
    void createAccount_DuplicateNumber_Returns400BadRequest() throws Exception {
        String payload = """
                {
                    "accountNumber": "1234567890",
                    "accountHolderName": "Ashish Kumar",
                    "email": "ashish@example.com",
                    "balance": 500.00
                }
                """;

        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenThrow(new DuplicateResourceException("Account with account number '1234567890' already exists"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Account with account number '1234567890' already exists"));
    }

    @Test
    @DisplayName("POST /api/accounts with database constraint violation should return 409 Conflict")
    void createAccount_DataIntegrityViolation_Returns409Conflict() throws Exception {
        String payload = """
                {
                    "accountNumber": "1234567890",
                    "accountHolderName": "Ashish Kumar",
                    "email": "ashish@example.com",
                    "balance": 500.00
                }
                """;

        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violated"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("GET /api/accounts/{id} should return 200 OK when account exists")
    void getAccountById_Found_Returns200OK() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountHolderName("Ashish Kumar")
                .email("ashish@example.com")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.getAccountById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("GET /api/accounts/{id} should return 404 Not Found when account does not exist")
    void getAccountById_NotFound_Returns404NotFound() throws Exception {
        when(accountService.getAccountById(999L))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 999"));

        mockMvc.perform(get("/api/accounts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found with ID: 999"));
    }

    @Test
    @DisplayName("PUT /api/accounts/{id} with valid payload should return 200 OK")
    void updateAccount_ValidPayload_Returns200OK() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountHolderName("Ashish Updated")
                .email("updated@example.com")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.updateAccount(eq(1L), any(UpdateAccountRequest.class))).thenReturn(response);

        String payload = """
                {
                    "accountHolderName": "Ashish Updated",
                    "email": "updated@example.com"
                }
                """;

        mockMvc.perform(put("/api/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountHolderName").value("Ashish Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    @DisplayName("PUT /api/accounts/{id} with blank name should return 400 Bad Request")
    void updateAccount_BlankName_Returns400BadRequest() throws Exception {
        String invalidPayload = """
                {
                    "accountHolderName": "",
                    "email": "updated@example.com"
                }
                """;

        mockMvc.perform(put("/api/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.accountHolderName").exists());
    }

    @Test
    @DisplayName("PATCH /api/accounts/{id} with partial payload should return 200 OK")
    void patchAccount_ValidPayload_Returns200OK() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountHolderName("Ashish Kumar")
                .email("ashish@example.com")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.SUSPENDED)
                .build();

        when(accountService.patchAccount(eq(1L), any(PatchAccountRequest.class))).thenReturn(response);

        String payload = """
                {
                    "status": "SUSPENDED"
                }
                """;

        mockMvc.perform(patch("/api/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("POST /api/accounts with text/plain Content-Type should return 415 Unsupported Media Type")
    void createAccount_TextPlainContentType_Returns415UnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("application/json")));
    }

    @Test
    @DisplayName("POST /api/accounts/{id}/deposit with valid amount should return 200 OK")
    void deposit_ValidAmount_Returns200OK() throws Exception {
        in.ashish.combined.dto.TransactionResponse response = in.ashish.combined.dto.TransactionResponse.builder()
                .transactionType("DEPOSIT")
                .accountId(1L)
                .accountNumber("1234567890")
                .transactionAmount(new BigDecimal("500.00"))
                .previousBalance(new BigDecimal("1000.00"))
                .newBalance(new BigDecimal("1500.00"))
                .accountStatus(AccountStatus.ACTIVE)
                .timestamp(LocalDateTime.now())
                .build();

        when(accountService.deposit(eq(1L), any(in.ashish.combined.dto.TransactionRequest.class))).thenReturn(response);

        String payload = """
                {
                    "amount": 500.00,
                    "description": "Salary"
                }
                """;

        mockMvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.transactionAmount").value(500.00))
                .andExpect(jsonPath("$.newBalance").value(1500.00));
    }

    @Test
    @DisplayName("POST /api/accounts/{id}/deposit with zero or negative amount should return 400 Bad Request")
    void deposit_NegativeAmount_Returns400BadRequest() throws Exception {
        String payload = """
                {
                    "amount": -50.00
                }
                """;

        mockMvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }

    @Test
    @DisplayName("POST /api/accounts/{id}/withdraw with valid amount should return 200 OK")
    void withdraw_ValidAmount_Returns200OK() throws Exception {
        in.ashish.combined.dto.TransactionResponse response = in.ashish.combined.dto.TransactionResponse.builder()
                .transactionType("WITHDRAWAL")
                .accountId(1L)
                .accountNumber("1234567890")
                .transactionAmount(new BigDecimal("300.00"))
                .previousBalance(new BigDecimal("1000.00"))
                .newBalance(new BigDecimal("700.00"))
                .accountStatus(AccountStatus.ACTIVE)
                .timestamp(LocalDateTime.now())
                .build();

        when(accountService.withdraw(eq(1L), any(in.ashish.combined.dto.TransactionRequest.class))).thenReturn(response);

        String payload = """
                {
                    "amount": 300.00,
                    "description": "ATM cash"
                }
                """;

        mockMvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.newBalance").value(700.00));
    }

    @Test
    @DisplayName("POST /api/accounts/{id}/withdraw with insufficient funds should return 400 Bad Request")
    void withdraw_InsufficientFunds_Returns400BadRequest() throws Exception {
        when(accountService.withdraw(eq(1L), any(in.ashish.combined.dto.TransactionRequest.class)))
                .thenThrow(new in.ashish.combined.exception.InsufficientBalanceException("Insufficient funds"));

        String payload = """
                {
                    "amount": 5000.00
                }
                """;

        mockMvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    @DisplayName("GET /api/accounts/{id}/balance should return 200 OK")
    void getBalance_Returns200OK() throws Exception {
        in.ashish.combined.dto.BalanceResponse response = in.ashish.combined.dto.BalanceResponse.builder()
                .accountId(1L)
                .accountNumber("1234567890")
                .accountHolderName("Ashish Kumar")
                .balance(new BigDecimal("1500.00"))
                .status(AccountStatus.ACTIVE)
                .checkedAt(LocalDateTime.now())
                .build();

        when(accountService.getBalance(1L)).thenReturn(response);

        mockMvc.perform(get("/api/accounts/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
