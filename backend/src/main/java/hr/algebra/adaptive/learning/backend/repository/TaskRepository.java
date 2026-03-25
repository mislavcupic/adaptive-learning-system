package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByOutcomeIdOrderByOrderIndexAsc(UUID outcomeId);
    List<Task> findByIsActiveTrue();
    List<Task> findByDueDateAfterAndIsActiveTrue(LocalDateTime now);
    List<Task> findByCreatedById(UUID userId);
}
