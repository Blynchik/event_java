package ru.service.event.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.service.event.config.security.JwtUser;
import ru.service.event.dto.event.request.EventRequest;
import ru.service.event.dto.event.response.EventResponse;
import ru.service.event.facade.EventFacade;

@RestController
@Slf4j
@RequestMapping("/api/event")
public class EventController {

    private final EventFacade eventFacade;

    @Autowired
    public EventController(EventFacade eventFacade) {
        this.eventFacade = eventFacade;
    }

    @GetMapping("/random")
    public ResponseEntity<EventResponse> getRandom(@AuthenticationPrincipal JwtUser jwtUser) {
        log.info("Request to GET /api/event/random from: {}", jwtUser.getLogin());
        EventResponse eventResponse = this.eventFacade.getRandomEvent();
        return ResponseEntity.ok(eventResponse);
    }

    @PostMapping("/admin")
    public ResponseEntity<EventResponse> create(@AuthenticationPrincipal JwtUser jwtUser,
                                                @Valid @RequestBody EventRequest eventRequest,
                                                BindingResult bindingResult) {
        log.info("Request to POST /api/event/admin from: {}", jwtUser.getLogin());
        EventResponse eventResponse = this.eventFacade.create(eventRequest, bindingResult);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventResponse);
    }
}
