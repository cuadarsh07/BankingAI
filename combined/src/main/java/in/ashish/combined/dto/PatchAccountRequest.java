package in.ashish.combined.dto;

import in.ashish.combined.entity.AccountStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO for partial update (HTTP PATCH) of an account.
 *
 * Interview Concepts:
 * - PATCH Semantics: Partial delta update.
 *   Fields are optional (can be null). Only non-null fields sent by client are updated.
 * - Validation on Optional Fields: When a field IS provided, it must still satisfy
 *   format rules (e.g., if email is present, it must be a valid email format).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchAccountRequest {

    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Account holder name can only contain letters, spaces, dots, hyphens, and apostrophes")
    private String accountHolderName;

    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Email must be a valid email format (e.g., user@example.com)")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    private AccountStatus status;
}
