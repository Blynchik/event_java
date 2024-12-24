package ru.service.event.dto.event.response.create;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.service.event.model.Event;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventResponseForCreate {

    private Long id;
    private String title;
    private String description;
    private List<DecisionResponseWithResultForCreate> decisions;

    public EventResponseForCreate(Event event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.description = event.getDescription();
        this.decisions = event.getDecisions().stream()
                .map(DecisionResponseWithResultForCreate::new)
                .toList();
    }
}
