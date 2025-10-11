package ru.home.tasktracker.api.controller.helpers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.home.tasktracker.api.exceptions.NotFoundException;
import ru.home.tasktracker.store.entities.ProjectEntity;
import ru.home.tasktracker.store.entities.TaskEntity;
import ru.home.tasktracker.store.entities.TaskStateEntity;
import ru.home.tasktracker.store.repositories.ProjectRepository;
import ru.home.tasktracker.store.repositories.TaskRepository;
import ru.home.tasktracker.store.repositories.TaskStateRepository;

@RequiredArgsConstructor
@Transactional
@Component
public class ControllerHelper {

    private final ProjectRepository projectRepository;

    private final TaskStateRepository taskStateRepository;

    private final TaskRepository taskRepository;


    public ProjectEntity getProjectByOwnerNameOrThrowException(Long projectId, String ownerName) {
        return projectRepository
                .findByIdAndOwnerName(projectId, ownerName)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format(
                                        "Project \"%s\" not found.",
                                        projectId
                                )
                        )
                );
    }

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format(
                                        "Task state \"%s\" not found.",
                                        taskStateId
                                )
                        )
                );
    }

    public TaskEntity getTaskOrThrowException(Long taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format(
                                        "Task state \"%s\" not found.",
                                        taskId
                                )
                        )
                );
    }

}
