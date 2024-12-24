package ru.service.event.dto.event.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DecisionResultRequest {

    @NotBlank(message = "Should be no more than 1000 characters long")
    @Size(max = 1000, message = "Should be no more than 1000 characters long")
    private String resultDescr;
}
