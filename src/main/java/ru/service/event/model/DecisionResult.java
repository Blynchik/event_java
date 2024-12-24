package ru.service.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class DecisionResult {

    @Column(name = "result_descr", length = 1000)
    @Size(max = 1000)
    @NotBlank
    private String resultDescr;
}
