package ru.service.event.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.service.event.dto.event.request.EventRequest;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "event",
        indexes = {
                @Index(name = "idx_event_title", columnList = "title"),
                @Index(name = "idx_event_id", columnList = "id")
        })
public class Event {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 100, unique = true)
    @Size(max = 100)
    @NotBlank
    private String title;

    @Column(name = "description", length = 1000)
    @Size(max = 1000)
    @NotBlank
    private String description;

    @OneToMany(mappedBy = "event",
            fetch = FetchType.EAGER,
            cascade = CascadeType.PERSIST)
    private List<Decision> decisions;

    public Event(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Event(EventRequest eventRequest) {
        this.title = eventRequest.getTitle();
        this.description = eventRequest.getDescription();
    }
}
