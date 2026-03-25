package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningOutcomeRepository extends JpaRepository<LearningOutcome, UUID> {
    List<LearningOutcome> findByCourseIdOrderByOrderIndexAsc(UUID courseId);
}