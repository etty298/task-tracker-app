package ru.home.tasktracker.api.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.home.tasktracker.api.controller.helpers.ControllerHelper;
import ru.home.tasktracker.api.dto.AnswerDto;
import ru.home.tasktracker.api.dto.TaskDto;
import ru.home.tasktracker.api.exceptions.BadRequestException;
import ru.home.tasktracker.api.factories.TaskDtoFactory;
import ru.home.tasktracker.store.entities.TaskEntity;
import ru.home.tasktracker.store.entities.TaskStateEntity;
import ru.home.tasktracker.store.repositories.TaskRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for tasks inside a task state (column).
 * Supports listing, creating, updating, reordering, and deleting tasks.
 * Ordering is maintained via a doubly-linked list using upper/lower pointers.
 */
@RestController
@RequiredArgsConstructor
@Transactional
public class TaskController {
    private final TaskRepository taskRepository;
    private final TaskDtoFactory taskDtoFactory;
    private final ControllerHelper controllerHelper;
    private static final String CREATE_TASK = "/api/task-states/{task_state_id}/tasks";
    private static final String GET_TASKS = "/api/task-states/{task_state_id}/tasks";
    private static final String UPDATE_TASK_STATES = "/api/tasks/{task_id}";
    private static final String CHANGE_TASK_POSITIONS = "/api/tasks/{task_id}/positions/change";
    private static final String DELETE_TASK = "/api/tasks/{task_id}";

    /**
     * Получить список задач в конкретном состоянии.
     */
    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_state_id") Long taskStateId) {

        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        if (!taskState.getProject().getOwnerName().equals(ownerName)) {
            throw new BadRequestException(String.format("Task state \"%s\" not found.", taskStateId));
        }
        return taskState
                .getTasks()
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());
    }

    /**
     * Создать новую задачу в конце списка состояния.
     */
    @PostMapping(CREATE_TASK)
    public TaskDto createTask(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_name") String taskName,
            @RequestParam(name = "task_description", required = false) Optional<String>optionalTaskDescription) {

        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        if (!taskState.getProject().getOwnerName().equals(ownerName)) {
            throw new BadRequestException(String.format("Task state \"%s\" not found.", taskStateId));
        }

        if (taskName.trim().isEmpty()) {
            throw new BadRequestException("Task name cannot be empty.");
        }

        Optional<TaskEntity> optionalLowerTask = Optional.empty();
        for (TaskEntity task : taskState.getTasks()) {
            if (task.getLowerTask().isEmpty()) {
                optionalLowerTask = Optional.of(task);
                break;
            }
        }
        TaskEntity task = taskRepository.saveAndFlush(
                TaskEntity
                        .builder()
                        .name(taskName)
                        .description(optionalTaskDescription.orElse(null))
                        .taskState(taskState)
                        .build()
        );
        optionalLowerTask
                .ifPresent(lowerTask -> {
                    task.setUpperTask(lowerTask);
                    lowerTask.setLowerTask(task);
                    taskRepository.saveAndFlush(lowerTask);
                });
        final TaskEntity savedTask = taskRepository.saveAndFlush(task);
        return taskDtoFactory.makeTaskDto(savedTask);
    }

    /**
     * Обновить имя и/или описание задачи.
     */
    @PatchMapping(UPDATE_TASK_STATES)
    public TaskDto updateTask(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_id") Long taskId,
            @RequestParam(name = "task_name", required = false) Optional<String> optionalTaskName,
            @RequestParam(name = "task_description", required = false) Optional<String> optionalTaskDescription) {

        TaskEntity task = controllerHelper.getTaskOrThrowException(taskId);

        if (!task.getTaskState().getProject().getOwnerName().equals(ownerName)) {
            throw new BadRequestException(String.format("Task \"%s\" not found.", taskId));
        }

        if (optionalTaskName.isEmpty() && optionalTaskDescription.isEmpty()) {
            throw new BadRequestException("Task name should not be empty or task description should not be empty.");
        }

        optionalTaskName.ifPresent(task::setName);
        optionalTaskDescription.ifPresent(task::setDescription);
        task = taskRepository.saveAndFlush(task);
        return taskDtoFactory.makeTaskDto(task);
    }

    /**
     * Переместить задачу относительно другой задачи в том же состоянии.
     */
    @PatchMapping(CHANGE_TASK_POSITIONS)
    public TaskDto changeTaskPositions(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_id") Long taskId,
            @RequestParam(name = "upper_task_id", required = false) Optional<Long> optionalUpperTaskId) {

        TaskEntity changeTask = controllerHelper.getTaskOrThrowException(taskId);

        TaskStateEntity taskState = changeTask.getTaskState();

        if (!taskState.getProject().getOwnerName().equals(ownerName)) {
            throw new BadRequestException(String.format("Task \"%s\" not found.", changeTask.getName()));
        }

        Optional<Long> optionalOldUpperTaskId = changeTask
                .getUpperTask()
                .map(TaskEntity::getId);
        if (optionalOldUpperTaskId.equals(optionalUpperTaskId)) {
            return taskDtoFactory.makeTaskDto(changeTask);
        }
        Optional<TaskEntity> optionalNewUpperTask = optionalUpperTaskId
                .map(upperTaskId -> {
                    if (taskId.equals(upperTaskId)) {
                        throw new BadRequestException("Left task state id equals task state.");
                    }
                    TaskEntity upperTaskEntity = controllerHelper.getTaskOrThrowException(upperTaskId);
                    if (!taskState.getId().equals(upperTaskEntity.getTaskState().getId())) {
                        throw new BadRequestException("Task position can be changed within the same task state.");
                    }
                    return upperTaskEntity;
                });
        Optional<TaskEntity> optionalNewLowerTask;
        if (optionalNewUpperTask.isEmpty()) {
            optionalNewLowerTask = taskState
                    .getTasks()
                    .stream()
                    .filter(anotherTask -> anotherTask.getUpperTask().isEmpty())
                    .findAny();
        } else {
            optionalNewLowerTask = optionalNewUpperTask.get().getLowerTask();
        }
        replaceOldTasksPositions(changeTask);
        if (optionalNewUpperTask.isPresent()) {
            TaskEntity upperTaskEntity = optionalNewUpperTask.get();
            upperTaskEntity.setLowerTask(changeTask);
            changeTask.setUpperTask(upperTaskEntity);
        } else {
            changeTask.setUpperTask(null);
        }
        if (optionalNewLowerTask.isPresent()) {
            TaskEntity lowerTaskEntity = optionalNewLowerTask.get();
            lowerTaskEntity.setUpperTask(changeTask);
            changeTask.setLowerTask(lowerTaskEntity);
        } else {
            changeTask.setLowerTask(null);
        }
        changeTask = taskRepository.saveAndFlush(changeTask);
        optionalNewUpperTask.ifPresent(taskRepository::saveAndFlush);
        optionalNewLowerTask.ifPresent(taskRepository::saveAndFlush);
        return taskDtoFactory.makeTaskDto(changeTask);
    }

    /**
     * Удалить задачу из состояния.
     */
    @DeleteMapping(DELETE_TASK)
    public AnswerDto deleteTask(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_id") Long TaskId) {

        TaskEntity task = controllerHelper.getTaskOrThrowException(TaskId);

        if (!task.getTaskState().getProject().getOwnerName().equals(ownerName)) {
            throw new BadRequestException(String.format("Task \"%s\" not found.", task.getName()));
        }

        replaceOldTasksPositions(task);

        taskRepository.deleteById(task.getId());

        return AnswerDto.setAnswer(true);
    }
    private void replaceOldTasksPositions(TaskEntity task) {
        Optional<TaskEntity> optionalOldUpperTask = task.getUpperTask();
        Optional<TaskEntity> optionalOldLowerTask = task.getLowerTask();
        // Если есть верхний сосед — теперь он должен ссылаться на правого
        optionalOldUpperTask
                .ifPresent(it -> {
                    it.setLowerTask(optionalOldLowerTask.orElse(null));
                    taskRepository.saveAndFlush(it);
                });
        // Если есть нижний сосед — теперь он должен ссылаться на левого
        optionalOldLowerTask
                .ifPresent(it -> {
                    it.setUpperTask(optionalOldUpperTask.orElse(null));
                    taskRepository.saveAndFlush(it);
                });
    }
}