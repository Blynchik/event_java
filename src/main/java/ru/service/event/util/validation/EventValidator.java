package ru.service.event.util.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.service.event.dto.event.request.DecisionRequest;
import ru.service.event.dto.event.request.EventRequest;
import ru.service.event.model.DecisionType;
import ru.service.event.service.EventService;
import ru.service.event.util.exception.BindingValidationException;

@Component
@Slf4j
public class EventValidator implements Validator {

    private final EventService eventService;

    @Autowired
    public EventValidator(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return EventRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        EventRequest eventRequest = (EventRequest) target;
        log.info("Checking new event: {}", eventRequest.getTitle());
        if (errors.hasErrors()) {
            throw new BindingValidationException((BindingResult) errors);
        }
        validateCreate(eventRequest, errors);
    }

    private void validateCreate(EventRequest eventRequest, Errors errors) {
        for (int i = 0; i < eventRequest.getDecisions().size(); i++) {
            DecisionRequest decision = eventRequest.getDecisions().get(i);
            boolean hasSuccess = decision.getResults().containsKey(true);
            boolean hasFailure = decision.getResults().containsKey(false);
            if (!hasSuccess || !hasFailure) {
                errors.rejectValue(String.format("decisions[%d].results", i), "",
                        "The successful or unsuccessful result is not specified");
                log.error("The successful or unsuccessful result is not specified in decision {} for new event: {}",
                        i, eventRequest.getTitle());
            }

            if (decision.getDecisionType().equals(DecisionType.TEXT.name()) && decision.getDifficulty() > 0) {
                errors.rejectValue(String.format("decisions[%d].results", i), "",
                        "The simple decisions should be with 0 difficulty");
                log.error("The simple decisions {} should be with 0 difficulty for new event: {}",
                        i, eventRequest.getTitle());
            }

            if (!decision.getDecisionType().equals(DecisionType.TEXT.name()) && decision.getDifficulty() == 0) {
                errors.rejectValue(String.format("decisions[%d].results", i), "",
                        "The characteristic check decisions should be with difficulty more than 0");
                log.error("The characteristic check decisions {} should be with difficulty more than 0 for new event: {}",
                        i, eventRequest.getTitle());
            }
        }

        if (eventService.getByTitleOptional(eventRequest.getTitle()).isPresent()) {
            errors.rejectValue("title", "", "The title is not unique");
            log.error("The title: {} is not unique for new event: {}", eventRequest.getTitle(), eventRequest.getTitle());
        }

        if (errors.hasErrors()) {
            throw new BindingValidationException((BindingResult) errors);
        }
    }
}
