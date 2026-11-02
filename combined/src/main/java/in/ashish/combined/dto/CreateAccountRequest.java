package in.ashish.combined.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for incoming account creation requests.
 *
 * Why DTO instead of Entity? (Interview Question):
 * 1. Security: Prevents mass-assignment attacks (clients submitting fields like 'id', 'status', 'createdAt').
 * 2. Encapsulation: Decouples internal database schema from the external API contract.
 * 3. Validation: Allows request-specific validations without polluting entity mapping.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{8,18}$", message = "Account number must be between 8 and 18 numeric digits")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Account holder name can only contain letters, spaces, dots, hyphens, and apostrophes")
    private String accountHolderName;

    @NotBlank(message = "Email is required")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Email must be a valid email format (e.g., user@example.com)")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Initial balance cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Balance can have up to 13 digits with 2 decimal places")
    private BigDecimal balance;
}
