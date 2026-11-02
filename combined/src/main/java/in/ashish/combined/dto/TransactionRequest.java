package in.ashish.combined.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for deposit and withdrawal requests.
 *
 * Interview Concepts:
 * - @DecimalMin(value = "0.01"): Guarantees amounts are strictly positive at the API boundary.
 * - @Digits: Enforces 2 decimal places (standard financial currency precision) and prevents overflow.
 * - BigDecimal: Prevents binary floating-point roundoff errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Transaction amount must be at least 0.01")
    @Digits(integer = 13, fraction = 2, message = "Amount can have up to 13 digits with 2 decimal places")
    private BigDecimal amount;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}
