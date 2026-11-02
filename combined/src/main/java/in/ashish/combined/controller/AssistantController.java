package in.ashish.combined.controller;

import in.ashish.combined.dto.fraud.AssistantChatRequest;
import in.ashish.combined.dto.fraud.AssistantChatResponse;
import in.ashish.combined.dto.fraud.FraudAlertResponse;
import in.ashish.combined.dto.fraud.HeldTransactionResponse;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.User;
import in.ashish.combined.exception.InvalidTransferException;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.service.fraud.FraudAlertService;
import in.ashish.combined.service.fraud.FraudDetectionService;
import in.ashish.combined.service.fraud.GeminiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Contextual AI Banking & Fraud Prevention Assistant.
 *
 * Base Path: /api/assistant
 */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssistantController {

    private final GeminiService geminiService;
    private final FraudDetectionService fraudDetectionService;
    private final FraudAlertService fraudAlertService;
    private final UserRepository userRepository;

    /**
     * Chat with Contextual AI Assistant.
     * POST /api/assistant/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        User user = getAuthenticatedUser();

        // Assemble sanitized customer context summary without leaking sensitive credentials
        StringBuilder context = new StringBuilder();
        context.append("Customer Name: ").append(user.getFullName()).append("\n");
        context.append("Role: ").append(user.getRole()).append("\n");

        try {
            Account acc = user.getAccount();
            if (acc != null && acc.getId() != null) {
                String accNum = acc.getAccountNumber();
                String maskedAcc = (accNum != null && accNum.length() > 4)
                        ? "******" + accNum.substring(accNum.length() - 4)
                        : (accNum != null ? accNum : "N/A");

                context.append("Account (Masked): ").append(maskedAcc).append("\n");
                context.append("Current Available Balance: ₹").append(acc.getBalance()).append("\n");
                context.append("Account Status: ").append(acc.getStatus()).append("\n");

                // Add held transactions info
                List<HeldTransactionResponse> held = fraudDetectionService.getHeldTransactionsForAccount(acc.getId());
                long pendingCount = held.stream().filter(h -> h.getState().name().startsWith("PENDING")).count();
                context.append("Active Transfers on Hold: ").append(pendingCount).append("\n");
                if (pendingCount > 0) {
                    HeldTransactionResponse first = held.get(0);
                    context.append("Most recent held transfer: ₹").append(first.getAmount())
                            .append(" to ").append(first.getDestinationAccountNumber())
                            .append(" (Reason: ").append(first.getCustomerWarning()).append(")\n");
                }
            }
        } catch (Exception e) {
            log.warn("Could not load account details for assistant context: {}", e.getMessage());
        }

        try {
            // Add alert info
            List<FraudAlertResponse> alerts = fraudAlertService.getAlertsForUser(user.getId());
            long unreadAlerts = alerts.stream().filter(a -> !a.isRead()).count();
            context.append("Unread Security Alerts: ").append(unreadAlerts).append("\n");
        } catch (Exception e) {
            log.warn("Could not load alerts for assistant context: {}", e.getMessage());
        }

        AssistantChatResponse response = geminiService.chatWithAssistant(request.getMessage(), context.toString());
        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidTransferException("Authentication required.");
        }
        return userRepository.findByUsernameWithAccount(auth.getName())
                .or(() -> userRepository.findByUsername(auth.getName()))
                .orElseThrow(() -> new InvalidTransferException("Authenticated user not found: " + auth.getName()));
    }
}

