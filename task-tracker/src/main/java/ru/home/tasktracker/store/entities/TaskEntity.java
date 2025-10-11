package ru.home.tasktracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    private String description;

    @OneToOne
    private TaskEntity upperTask;

    @OneToOne
    private TaskEntity lowerTask;

    @ManyToOne
    private TaskStateEntity taskState;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public Optional<TaskEntity> getUpperTask() {
        return Optional.ofNullable(upperTask);
    }

    public Optional<TaskEntity> getLowerTask() {
        return Optional.ofNullable(lowerTask);
    }
}
