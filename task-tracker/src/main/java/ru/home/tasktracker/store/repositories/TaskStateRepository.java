package ru.home.tasktracker.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.home.tasktracker.store.entities.TaskStateEntity;

import java.util.Optional;


public interface TaskStateRepository extends JpaRepository<TaskStateEntity, Long> {

    Optional<TaskStateEntity> findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(Long projectId, String name);

}
