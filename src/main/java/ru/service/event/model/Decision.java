package ru.service.event.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.service.event.dto.event.request.DecisionRequest;

import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "decision")
public class Decision {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type")
    private DecisionType decisionType;

    @Column(name = "description", length = 100)
    @Size(max = 100)
    @NotBlank
    private String description;

    // содержит еще знаки разделения между логами
    @Column(name = "decision_log", length = 1100)
    @Size(max = 1100)
    @NotBlank
    private String decisionLog;

    @Column(name = "difficulty")
    @PositiveOrZero
    private int difficulty;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "decision_result", joinColumns = @JoinColumn(name = "decision_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "success")
    private Map<Boolean, DecisionResult> results;

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private Event event;

    public Decision(DecisionType decisionType,
                    String description,
                    String decisionLog,
                    int difficulty) {
        this.decisionType = decisionType;
        this.description = description;
        this.decisionLog = decisionLog;
        this.difficulty = difficulty;
    }

    public Decision(DecisionRequest decisionRequest) {
        this.decisionType = DecisionType.valueOf(decisionRequest.getDecisionType());
        this.description = decisionRequest.getDescription();
        this.decisionLog = new HashSet<>(decisionRequest.getDecisionLog())
                .toString();
        this.difficulty = decisionRequest.getDifficulty();
        this.results = Map.of(
                true, new DecisionResult(decisionRequest.getResults().get(true).getResultDescr()),
                false, new DecisionResult(decisionRequest.getResults().get(false).getResultDescr())
        );
    }
}
