package ru.service.event.dto.event.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.service.event.model.Decision;
import ru.service.event.model.DecisionType;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DecisionResponse {

    private Long id;
    private DecisionType decisionType;
    private String decisionDescr;
    private Integer difficulty;
    private String decisionLog;
    private String eventTitle;
    private Map<Boolean, DecisionResultResponse> results;

    public DecisionResponse(Decision decision) {
        this.id = decision.getId();
        this.decisionType = decision.getDecisionType();
        this.decisionDescr = decision.getDescription();
        this.difficulty = decision.getDifficulty();
        this.decisionLog = decision.getDecisionLog();
        this.eventTitle = decision.getEvent().getTitle();
        results = Map.of(
                true, new DecisionResultResponse(decision.getResults().get(true).getResultDescr()),
                false, new DecisionResultResponse(decision.getResults().get(false).getResultDescr())
        );
    }
}
