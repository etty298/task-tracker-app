package ru.home.tasktracker.api.factories;

import org.springframework.stereotype.Component;
import ru.home.tasktracker.api.dto.ProjectDto;
import ru.home.tasktracker.store.entities.ProjectEntity;

@Component
public class ProjectDtoFactory {

    public ProjectDto makeProjectDto(ProjectEntity entity) {
        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .ownerName(entity.getOwnerName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
