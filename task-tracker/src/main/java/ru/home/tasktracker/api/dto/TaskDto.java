package ru.home.tasktracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDto {

    @NonNull
    private Long id;

    @NonNull
    private String name;

    private String description;

    @JsonProperty("upper_task_id")
    private Long upperTaskId;

    @JsonProperty("lower_task_id")
    private Long lowerTaskId;

    @NonNull
    @JsonProperty("created_at")
    private Instant createdAt;
}
