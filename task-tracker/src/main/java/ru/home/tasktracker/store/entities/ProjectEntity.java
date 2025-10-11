package ru.home.tasktracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "project",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_name"})
)
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    private String ownerName;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToMany
    @Builder.Default
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private List<TaskStateEntity> taskStates = new ArrayList<>();

}
