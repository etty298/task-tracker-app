package ru.home.tasktracker.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.home.tasktracker.store.entities.TaskEntity;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
}
