package ru.service.event.dto.event.response.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.service.event.model.Decision;
import ru.service.event.model.DecisionType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DecisionResponse {

    private Long id;
    private DecisionType decisionType;
    private String description;
    private String decisionLog;
    private Integer difficulty;
    private String eventTitle;

    public DecisionResponse(Decision decision) {
        this.id = decision.getId();
        this.decisionType = decision.getDecisionType();
        this.description = decision.getDescription();
        this.decisionLog = decision.getDecisionLog();
        this.eventTitle = decision.getEvent().getTitle();
        this.difficulty = decision.getDifficulty();
    }
}
