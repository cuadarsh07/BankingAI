package in.ashish.combined.dto.fraud;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatResponse {
    private String reply;
    private LocalDateTime timestamp;
    private List<String> suggestedActions;

    @JsonProperty("isSecurityAlert")
    @JsonAlias("securityAlert")
    private boolean isSecurityAlert;
}

