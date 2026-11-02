package in.ashish.combined.dto.fraud;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationSubmitRequest {

    /**
     * Map of question identifier or question text to user answer ("YES" / "NO" or text)
     */
    @NotEmpty(message = "Answers are required")
    private Map<String, String> answers;
}

