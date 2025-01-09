package ru.service.event.facade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import ru.service.event.dto.event.request.EventRequest;
import ru.service.event.dto.event.response.EventResponse;
import ru.service.event.model.Decision;
import ru.service.event.model.Event;
import ru.service.event.service.EventService;
import ru.service.event.util.validation.EventValidator;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventFacade {

    private final EventService eventService;
    private final EventValidator eventValidator;

    @Autowired
    public EventFacade(EventService eventService,
                       EventValidator eventValidator) {
        this.eventService = eventService;
        this.eventValidator = eventValidator;
    }

    public EventResponse getRandomEvent() {
        log.info("Getting random event");
        Event event = eventService.getRandomEvent();
        return new EventResponse(event);
    }

    public EventResponse create(EventRequest eventRequest,
                                BindingResult bindingResult) {
        log.info("Creating new event: {}", eventRequest.getTitle());
        eventValidator.validate(eventRequest, bindingResult);
        Event eventToSave = new Event(eventRequest);
        List<Decision> decisionsToSave = eventRequest.getDecisions().stream()
                .map(decisionRequest -> {
                    Decision decisionToSave = new Decision(decisionRequest);
                    decisionToSave.setEvent(eventToSave);
                    return decisionToSave;
                })
                .collect(Collectors.toSet()).stream()
                .toList();
        eventToSave.setDecisions(decisionsToSave);
        Event createdEvent = eventService.create(eventToSave);
        return new EventResponse(createdEvent);
    }
}
