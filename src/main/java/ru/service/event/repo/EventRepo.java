package ru.service.event.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.service.event.model.Event;

import java.util.Optional;

@Repository
public interface EventRepo extends JpaRepository<Event, Long> {

    @Query(value = "SELECT * FROM event ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Event> findRandomEvent();

    Optional<Event> findEventByTitle(String title);
}
