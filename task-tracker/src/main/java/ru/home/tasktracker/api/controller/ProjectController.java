package ru.home.tasktracker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.home.tasktracker.api.controller.helpers.ControllerHelper;
import ru.home.tasktracker.api.dto.AnswerDto;
import ru.home.tasktracker.api.dto.ProjectDto;
import ru.home.tasktracker.api.exceptions.BadRequestException;
import ru.home.tasktracker.api.factories.ProjectDtoFactory;
import ru.home.tasktracker.store.entities.ProjectEntity;
import ru.home.tasktracker.store.repositories.ProjectRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST controller for CRUD operations over projects.
 * Endpoints:
 * - GET /api/projects — list projects, optionally filter by name prefix
 * - POST /api/projects — create new project (unique name)
 * - PATCH /api/projects/{project_id} — rename project (unique among others)
 * - DELETE /api/projects/{project_id} — delete a project by id
 */
@RequiredArgsConstructor
@Transactional
@RestController
public class ProjectController {

     private final ProjectRepository projectRepository;

     private final ProjectDtoFactory projectDtoFactory;

     private final ControllerHelper controllerHelper;

    public static final String FETCH_PROJECT = "/api/projects";
    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";


    /**
     * Получить список всех проектов.
     * Если указан параметр prefix_name — вернёт проекты, имя которых начинается с него.
     */
    @GetMapping(FETCH_PROJECT)
    public List<ProjectDto> fetchProjects(
            @RequestHeader("X-Username") String ownerName,
            @RequestParam(name = "prefix_name", required = false) Optional<String> optionalPrefixName) {

        // Если есть параметр prefix_name и он не пустой — фильтруем по имени
        Stream<ProjectEntity> projectStream = optionalPrefixName
            .filter(prefixName -> !prefixName.trim().isEmpty())
            // достаём проекты, у которых имя начинается с переданного префикса
            .map(prefixName -> projectRepository.streamAllByNameStartsWithIgnoreCaseAndOwnerName(prefixName, ownerName))
            // иначе берём все проекты
            .orElseGet(() -> projectRepository.streamAllByOwnerName(ownerName));

        // Преобразуем ProjectEntity → ProjectDto и собираем в список
        return projectStream
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    /**
     * Создать новый проект.
     * Проверяет, что имя не пустое и что проект с таким именем ещё не существует.
     */
    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(
            @RequestHeader("X-Username") String ownerName,
            @RequestParam(name = "project_name") String projectName) {

        // Проверяем имя проекта; выбросим exception если имя пустое
        checkIfProjectNameIsEmptyAndThrowException(projectName);

        // Проверка: есть ли уже проект с таким именем; выбросим exception если есть
        projectRepository
                .findByNameAndOwnerName(projectName, ownerName)
                .ifPresent(project -> {
                    // Если найден — выбрасываем исключение 400 (Bad Request)
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", projectName));
                });

        // Создаём новую сущность проекта и сохраняем её в базе
        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity
                        .builder()
                        .name(projectName)
                        .ownerName(ownerName)
                        .build()
        );
        // Преобразуем ProjectEntity → ProjectDto
        return projectDtoFactory.makeProjectDto(project);
    }

    /**
     * Изменить существующий проект.
     * Проверяет уникальность имени среди других проектов.
     */
    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(
            @RequestHeader("X-Username") String ownerName,
            @RequestParam(name = "project_name") String projectName,
            @PathVariable(value = "project_id") Long projectId) {

        // Проверяем имя проекта; выбросим exception если имя пустое
        checkIfProjectNameIsEmptyAndThrowException(projectName);

        // Находим проект по projectId; выбрасываем exception, если его нет
        ProjectEntity project = controllerHelper.getProjectByOwnerNameOrThrowException(projectId, ownerName);

        // Проверка: есть ли уже проект с таким именем; выбросим exception если есть
        projectRepository
                .findByNameAndOwnerName(projectName, ownerName)
                // Проверка: совпадают ли id; выбросим exception если есть
                .filter(anotherProject -> !anotherProject.getId().equals(projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(String.format("Project \"%s\" already exists.", projectName));
                });
        // Обновляем имя проекта
        project.setName(projectName);
        // Сохраняем изменения в БД
        project = projectRepository.saveAndFlush(project);
        // Преобразуем ProjectEntity → ProjectDto
        return projectDtoFactory.makeProjectDto(project);
    }

    /**
     * Удалить проект по его projectId.
     */
    @DeleteMapping(DELETE_PROJECT)
    public AnswerDto deleteProject(
            @RequestHeader("X-Username") String ownerName,
            @PathVariable(value = "project_id") Long projectId) {

        // Проверяем, что проект существует; выбросим exception если проект не существует
        controllerHelper.getProjectByOwnerNameOrThrowException(projectId, ownerName);

        // Удаляем проект из базы
        projectRepository.deleteById(projectId);

        // Возвращаем DTO с положительным ответом
        return AnswerDto.setAnswer(true);

    }

    /**
     * Проверка, что имя проекта не пустое.
     */
    private static void checkIfProjectNameIsEmptyAndThrowException(String projectName) {
        if (projectName.trim().isEmpty()) {
            throw new BadRequestException("Project name cannot be empty.");
        }
    }
}
