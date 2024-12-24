package ru.service.event.dto.event.response.create;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.service.event.model.Decision;
import ru.service.event.model.DecisionType;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DecisionResponseWithResultForCreate {

    private DecisionType decisionType;
    private String decisionDescr;
    private Integer difficulty;
    private String decisionLog;
    private String eventTitle;
    private Map<Boolean, DecisionResultResponseForCreate> results;

    public DecisionResponseWithResultForCreate(Decision decision) {
        this.decisionType = decision.getDecisionType();
        this.decisionDescr = decision.getDescription();
        this.difficulty = decision.getDifficulty();
        this.decisionLog = decision.getDecisionLog();
        this.eventTitle = decision.getEvent().getTitle();
        results = Map.of(
                true, new DecisionResultResponseForCreate(decision.getResults().get(true).getResultDescr()),
                false, new DecisionResultResponseForCreate(decision.getResults().get(false).getResultDescr())
        );
    }
}
