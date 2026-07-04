package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    List<AssessmentQuestion> findByAssessmentIdOrderByOrderIndexAsc(UUID assessmentId);
}