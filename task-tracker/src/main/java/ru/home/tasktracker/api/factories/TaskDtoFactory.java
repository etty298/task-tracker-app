package ru.home.tasktracker.api.factories;

import org.springframework.stereotype.Component;
import ru.home.tasktracker.api.dto.TaskDto;
import ru.home.tasktracker.store.entities.TaskEntity;

@Component
public class TaskDtoFactory {

    public TaskDto makeTaskDto(TaskEntity entity) {
        return TaskDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .upperTaskId(entity.getUpperTask().map(TaskEntity::getId).orElse(null))
                .lowerTaskId(entity.getLowerTask().map(TaskEntity::getId).orElse(null))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
