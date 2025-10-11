package ru.home.tasktracker.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.home.tasktracker.store.entities.ProjectEntity;

import java.util.Optional;
import java.util.stream.Stream;

    public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    Optional<ProjectEntity> findByNameAndOwnerName(String name, String ownerName);

    Stream<ProjectEntity> streamAllByOwnerName(String ownerName);

    Stream<ProjectEntity> streamAllByNameStartsWithIgnoreCaseAndOwnerName(String name, String ownerName);

    Optional<ProjectEntity> findByIdAndOwnerName(Long id, String ownerName);

}
