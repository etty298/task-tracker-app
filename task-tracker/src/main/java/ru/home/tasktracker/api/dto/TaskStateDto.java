package ru.home.tasktracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStateDto {

    @NonNull
    private Long id;

    @NonNull
    private String name;

    @JsonProperty("left_task_state_id")
    private Long leftTaskStateId;

    @JsonProperty("right_task_state_id")
    private Long rightTaskStateId;

    @NonNull
    private List<TaskDto> tasks;

    @NonNull
    @JsonProperty("created_at")
    private Instant createdAt;
}
