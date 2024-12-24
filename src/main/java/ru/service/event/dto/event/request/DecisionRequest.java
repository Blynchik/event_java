package ru.service.event.dto.event.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DecisionRequest {

    @Pattern(regexp = "STR|DEX|CON|INT|WIS|CHA|TEXT", message = "Invalid decision type")
    private String decisionType;

    @Size(max = 100, message = "Should be no more than 100 characters long")
    @NotBlank(message = "Should be no more than 100 characters long")
    private String description;

    @NotNull(message = "Decision log must contain at least one entry")
    @Size(min = 1, max = 5, message = "Decision log must contain at least one entry")
    @Valid
    private List<
            @NotBlank(message = "Decision log entry cannot exceed 100 characters")
            @Size(max = 100, message = "Decision log entry cannot exceed 100 characters")
                    String> decisionLog;

    @PositiveOrZero(message = "Difficulty must be at least 0")
    private int difficulty;

    @NotNull(message = "It is necessary to indicate the positive and negative result")
    @Valid
    private Map<Boolean, DecisionResultRequest> results;
}
