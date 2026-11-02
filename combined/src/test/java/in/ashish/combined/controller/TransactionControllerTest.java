package in.ashish.combined.controller;

import in.ashish.combined.dto.PageResponse;
import in.ashish.combined.dto.TransactionRecordResponse;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.exception.ResourceNotFoundException;
import in.ashish.combined.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TransactionController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {in.ashish.combined.config.SecurityConfig.class, in.ashish.combined.security.JwtAuthenticationFilter.class}
        )
)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    @DisplayName("GET /api/accounts/{id}/transactions with default pagination should return 200 OK")
    void getTransactions_DefaultParams_Returns200OK() throws Exception {
        TransactionRecordResponse record = TransactionRecordResponse.builder()
                .id(1L)
                .referenceNumber("TXN-12345678")
                .accountId(1L)
                .accountNumber("1001234567")
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .description("Initial deposit")
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<TransactionRecordResponse> pageResponse = PageResponse.<TransactionRecordResponse>builder()
                .content(List.of(record))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(transactionService.getAccountTransactions(eq(1L), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/accounts/1/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].referenceNumber").value("TXN-12345678"))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].amount").value(500.00));
    }

    @Test
    @DisplayName("GET /api/accounts/{id}/transactions when account does not exist should return 404 NOT FOUND")
    void getTransactions_AccountNotFound_Returns404() throws Exception {
        when(transactionService.getAccountTransactions(eq(99L), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Account not found with ID: 99"));

        mockMvc.perform(get("/api/accounts/99/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Account not found with ID: 99"));
    }
}
