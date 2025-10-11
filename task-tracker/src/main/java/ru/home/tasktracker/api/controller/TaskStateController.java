package ru.home.tasktracker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.home.tasktracker.api.controller.helpers.ControllerHelper;
import ru.home.tasktracker.api.dto.AnswerDto;
import ru.home.tasktracker.api.dto.TaskStateDto;
import ru.home.tasktracker.api.exceptions.BadRequestException;
import ru.home.tasktracker.api.factories.TaskStateDtoFactory;
import ru.home.tasktracker.store.entities.ProjectEntity;
import ru.home.tasktracker.store.entities.TaskStateEntity;
import ru.home.tasktracker.store.repositories.TaskStateRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for managing task states (columns) inside a project.
 * Provides endpoints to list, create, rename, reorder, and delete task states.
 * The ordering is implemented via a doubly-linked list using left/right pointers.
 */
@RestController
@RequiredArgsConstructor
@Transactional
public class TaskStateController {

    private final TaskStateRepository taskStateRepository;

    private final TaskStateDtoFactory taskStateDtoFactory;

    private final ControllerHelper controllerHelper;

    private static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states";
    private static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    private static final String UPDATE_TASK_STATES = "/api/task-states/{task_state_id}";
    private static final String CHANGE_TASK_STATES_POSITIONS = "/api/task-states/{task_state_id}/positions/change";
    private static final String DELETE_TASK_STATE = "/api/task-states/{task_state_id}";

    /**
     * Получение списка всех состояний задач проекта.
     */
    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "project_id") Long projectId) {

        // Берём проект — понадобится для нахождения task'ов.
        ProjectEntity project = controllerHelper.getProjectByOwnerNameOrThrowException(projectId, ownerName);

        // Возвращаем список task'ов.
        return project
                .getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    /**
     * Создать новое состояние задачи в проекте.
     * Добавляется в конец списка (правее последнего).
     */
    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName) {

        // Берём проект — понадобится для проверок принадлежности
        ProjectEntity project = controllerHelper.getProjectByOwnerNameOrThrowException(projectId, ownerName);

        // Проверяем имя taskState; выбросим exception если имя пустое
        checkIfTaskStateNameIsEmptyOrThrowException(taskStateName);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        // Перебираем все task'и в проекте:
        for (TaskStateEntity taskState : project.getTaskStates()) {
            // Проверка уникальности имени; выбросим exception в обратном случае
            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
            }
            // Ищем "крайний правый" элемент (у которого нет правого соседа)
            if (taskState.getRightTaskState().isEmpty()){
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }

        // Создаём новую сущность TaskState
        TaskStateEntity taskState = taskStateRepository.saveAndFlush(
                TaskStateEntity
                        .builder()
                        .name(taskStateName)
                        .project(project)
                        .build()
        );

        // Если найден правый крайний элемент — привязываем к нему новый
        optionalAnotherTaskState
                .ifPresent(anotherTaskState -> {

                    taskState.setLeftTaskState(anotherTaskState);

                    anotherTaskState.setRightTaskState(taskState);

                    taskStateRepository.saveAndFlush(anotherTaskState);
                });

        // Сохраняем новый state
        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        // Преобразуем TaskStateEntity → TaskStateDto
        return taskStateDtoFactory.makeTaskStateDto(savedTaskState);
    }

    /**
     * Обновить имя состояния задачи.
     * Проверяется уникальность внутри проекта.
     */
    @PatchMapping(UPDATE_TASK_STATES)
    public TaskStateDto updateTaskState(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName) {

        // Получаем сущность по taskStateId
        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        if (!taskState.getProject().getOwnerName().equals(ownerName)){
            throw new BadRequestException(String.format("Task state \"%s\" not found.", taskStateId));
        }

        // Проверяем имя taskState; выбросим exception если имя пустое
        checkIfTaskStateNameIsEmptyOrThrowException(taskStateName);

        // Проверка уникальности имени внутри того же проекта; выбросим exception в обратном случае
        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(taskState.getProject().getId(), taskStateName)
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(anotherTaskState -> {
                    throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
                });

        // Обновляем имя и сохраняем
        taskState.setName(taskStateName);
        taskState = taskStateRepository.saveAndFlush(taskState);

        // Преобразуем TaskStateEntity → TaskStateDto
        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    /**
     * Переместить состояние задачи в новое место относительно другого.
     * Реализует логику связанного списка (left/right).
     */
    @PatchMapping(CHANGE_TASK_STATES_POSITIONS)
    public TaskStateDto changeTaskStatesPositions(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId) {

        // Получаем сущность по taskStateId, которую хотим переместить.
        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        // Берём проект — понадобится для проверок принадлежности.
        ProjectEntity project = taskState.getProject();

        if (!project.getOwnerName().equals(ownerName)){
            throw new BadRequestException(String.format("Task state \"%s\" not found.", taskStateId));
        }

        // Получаем id текущего левого соседа (если он есть).
        Optional<Long> optionalOldTaskStateId = taskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        // Если новый requested left id совпадает со старым — ничего не делаем.
        // Сравнение Optional'ов корректно: оба пусты -> равны; если оба содержат одно и то же значение -> равны.
        if (optionalLeftTaskStateId.equals(optionalOldTaskStateId)) {
            return taskStateDtoFactory.makeTaskStateDto(taskState);
        }

        // Если указан новый left_task_state_id, загружаем соответствующую сущность.
        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {
                    // Защита: нельзя поставить самого себя слева
                    if (leftTaskStateId.equals(taskStateId)) {
                        throw new BadRequestException("Left task state id equals task state.");
                    }
                    // Загружаем сущность левого соседа; выбросим exception, если не найдена.
                    TaskStateEntity leftTaskStateEntity = controllerHelper.getTaskStateOrThrowException(leftTaskStateId);

                    // Проверяем, что новый левый элемент принадлежит тому же проекту.
                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException("Task state position can be changed within the same project.");
                    }
                    return leftTaskStateEntity;
                });

        // Вычисляем новое правое соседство (newRight):
        // - если newLeft присутствует -> newRight = newLeft.right
        // - иначе (вставка в начало) -> newRight = текущий "первый" элемент проекта (тот, у которого left == null)
        Optional<TaskStateEntity> optionalNewRightTaskState;
        if (optionalNewLeftTaskState.isEmpty()) {
            // Берем первый Task (left == null).
            optionalNewRightTaskState = project.getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> anotherTaskState.getLeftTaskState().isEmpty())
                    .findAny();
        } else {
            // Если новый левый известен, берем его правого соседа (если есть).
            optionalNewRightTaskState = optionalNewLeftTaskState.get().getRightTaskState();
        }

        // Развязываем changeTaskState от его старых соседей.
        replaceOldTaskStatesPositions(taskState);

        // Вставляем taskState справа от newLeft (если newLeft есть).
        if (optionalNewLeftTaskState.isPresent()) {

            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();
            // Устанавливаем ссылки: newLeft.right = changeTaskState
            newLeftTaskState.setRightTaskState(taskState);
            // changeTaskState.left = newLeft
            taskState.setLeftTaskState(newLeftTaskState);

        } else {
            // Если вставка в начало — у changeTaskState нет левого соседа.
            taskState.setLeftTaskState(null);
        }

        // Если есть новый правый — подвязываем его слева к taskState.
        if (optionalNewRightTaskState.isPresent()) {
            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();
            // newRight.left = changeTaskState
            newRightTaskState.setLeftTaskState(taskState);
            // changeTaskState.right = newRight
            taskState.setRightTaskState(newRightTaskState);

        } else {
            // Если вставляем в конец — у changeTaskState нет правого соседа.
            taskState.setRightTaskState(null);
        }

        // Сохраняем taskState
        // Это выполнит SQL UPDATE/INSERT немедленно.
        taskState = taskStateRepository.saveAndFlush(taskState);

        // Если есть новый левый — сохраняем его (сделает отдельный UPDATE).
        optionalNewLeftTaskState.ifPresent(taskStateRepository::saveAndFlush);

        // Если есть новый правый — сохраняем его (сделает отдельный UPDATE).
        optionalNewRightTaskState.ifPresent(taskStateRepository::saveAndFlush);

        // Преобразуем TaskStateEntity → TaskStateDto
        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    /**
     * Удалить состояние задачи.
     * Перед удалением разрываются связи с соседями.
     */
    @DeleteMapping(DELETE_TASK_STATE)
    public AnswerDto deleteTaskState(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(name = "task_state_id") Long taskStateId) {

        // Получаем сущность по taskStateId, которую хотим удалить
        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        if (!taskState.getProject().getOwnerName().equals(ownerName)){
            throw new BadRequestException(String.format("Task state \"%s\" not found.", taskStateId));
        }

        // Разрываем связи с соседями
        replaceOldTaskStatesPositions(taskState);

        // Удаляем из базы
        taskStateRepository.deleteById(taskState.getId());

        // Возвращаем DTO с положительным ответом
        return AnswerDto.setAnswer(true);
    }
    /**
     * Вспомогательный метод: развязывает текущее состояние от старых соседей.
     * Используется при удалении или смене позиции.
     */
    private void replaceOldTaskStatesPositions(TaskStateEntity taskState) {
        Optional<TaskStateEntity> oldLeftTaskState = taskState.getLeftTaskState();
        Optional<TaskStateEntity> oldRightTaskState = taskState.getRightTaskState();

        // Если есть левый сосед — теперь он должен ссылаться на правого
        oldLeftTaskState.ifPresent(leftTaskState -> {
            leftTaskState.setRightTaskState(oldRightTaskState.orElse(null));
            taskStateRepository.saveAndFlush(leftTaskState);
        });

        // Если есть правый сосед — теперь он должен ссылаться на левого
        oldRightTaskState.ifPresent(rightTaskState -> {
            rightTaskState.setLeftTaskState(oldLeftTaskState.orElse(null));
            taskStateRepository.saveAndFlush(rightTaskState);
        });
    }

    /**
     * Проверка, что имя состояния задачи не пустое.
     */
    private static void checkIfTaskStateNameIsEmptyOrThrowException(String taskStateName) {
        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Task state name cannot be empty.");
        }
    }
}
