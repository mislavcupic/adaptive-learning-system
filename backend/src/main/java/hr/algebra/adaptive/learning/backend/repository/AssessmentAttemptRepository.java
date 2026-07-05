package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.AssessmentAttempt;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    List<AssessmentAttempt> findByStudentId(UUID studentId);

    Optional<AssessmentAttempt> findByStudentIdAndAssessmentId(UUID studentId, UUID assessmentId);

    @Query("SELECT aa FROM AssessmentAttempt aa WHERE aa.student.id = :studentId AND aa.assessment.assessmentType = :type AND aa.isCompleted = true")
    Optional<AssessmentAttempt> findCompletedByStudentAndType(@Param("studentId") UUID studentId, @Param("type") AssessmentType type);

    @Query("SELECT aa FROM AssessmentAttempt aa " +
            "WHERE aa.student.id = :studentId " +
            "AND aa.assessment.assessmentType = :type " +
            "AND aa.assessment.course.id = :courseId " +
            "AND aa.isCompleted = true")
    Optional<AssessmentAttempt> findCompletedByStudentTypeAndCourse(
            @Param("studentId") UUID studentId,
            @Param("type") AssessmentType type,
            @Param("courseId") UUID courseId);

    @Query("SELECT aa FROM AssessmentAttempt aa " +
            "JOIN FETCH aa.student s " +
            "JOIN FETCH aa.assessment a " +
            "WHERE aa.isCompleted = true " +
            "AND s.researchGroup <> hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup.NOT_ASSIGNED")
    List<AssessmentAttempt> findAllCompletedForResearch();

    boolean existsByStudentIdAndAssessmentIdAndIsCompletedTrue(UUID studentId, UUID assessmentId);
}

