package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.Assessment;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findByCourseIdAndIsActiveTrue(UUID courseId);

    List<Assessment> findByCourseIdAndAssessmentTypeAndIsActiveTrue(UUID courseId, AssessmentType type);

    Optional<Assessment> findByCourseIdAndAssessmentType(UUID courseId, AssessmentType type);
}
