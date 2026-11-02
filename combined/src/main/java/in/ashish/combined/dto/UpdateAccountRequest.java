package in.ashish.combined.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO for full update (HTTP PUT) of an account's mutable profile details.
 *
 * Interview Concepts:
 * - PUT Semantics: Full replacement of the resource's mutable representation.
 *   All required fields must be supplied.
 * - Sensitive Fields Excluded: Notice 'balance' and 'accountNumber' are NOT in this DTO!
 *   In banking systems, allowing account balance or account number to be updated via PUT
 *   is a severe security flaw. Balance must only change via financial transactions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccountRequest {

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Account holder name can only contain letters, spaces, dots, hyphens, and apostrophes")
    private String accountHolderName;

    @NotBlank(message = "Email is required")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Email must be a valid email format (e.g., user@example.com)")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;
}
