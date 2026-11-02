package in.ashish.combined.dto.fraud;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewRequest {

    /**
     * Action to execute: "APPROVE" or "REJECT"
     */
    @NotBlank(message = "Action is required")
    private String action;

    private String notes;
    private String reason;
}

