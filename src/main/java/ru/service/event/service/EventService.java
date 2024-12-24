package ru.service.event.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.service.event.model.Event;
import ru.service.event.repo.EventRepo;

import java.util.Optional;

@Service
@Transactional
@Slf4j
public class EventService {

    private final EventRepo eventRepo;

    @Autowired
    public EventService(EventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    @Transactional
    public Event create(Event event) {
        log.info("Creating new event: {}", event.getTitle());
        return this.eventRepo.save(event);
    }

    public Event getRandomEvent() {
        log.info("Searching random event");
        return this.eventRepo.findRandomEvent()
                .orElseThrow((() -> new EntityNotFoundException("Event not found")));
    }

    public Event getByTitle(String title) {
        log.info("Searching for a event by title: {}", title);
        return eventRepo.findEventByTitle(title)
                .orElseThrow(() ->
                        new EntityNotFoundException("The event was not found"));
    }

    public Optional<Event> getByTitleOptional(String title) {
        log.info("Searching for a event by title: {}", title);
        return eventRepo.findEventByTitle(title);
    }
}
