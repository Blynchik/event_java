package ru.service.event.dto.event.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventRequest {

    @NotBlank(message = "Should be no more than 100 characters long")
    @Size(max = 100, message = "Should be no more than 100 characters long")
    private String title;

    @NotBlank(message = "Should be no more than 1000 characters long")
    @Size(max = 1000, message = "Should be no more than 1000 characters long")
    private String description;

    @NotNull(message = "The decisions list must contain at least one element")
    @Size(min = 1, max = 20, message = "The decisions list must contain at least one element")
    @Valid
    private List<DecisionRequest> decisions;
}
