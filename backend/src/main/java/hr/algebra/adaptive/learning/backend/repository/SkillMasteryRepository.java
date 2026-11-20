package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.SkillMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillMasteryRepository extends JpaRepository<SkillMastery, UUID> {
    List<SkillMastery> findByStudentId(UUID studentId);
    Optional<SkillMastery> findByStudentIdAndSkillName(UUID studentId, String skillName);
    List<SkillMastery> findByStudentIdAndOutcomeId(UUID studentId, UUID outcomeId);

    // Metoda potrebna za Dashboard
    @Query("SELECT AVG(sm.masteryLevel) FROM SkillMastery sm WHERE sm.student.id = :studentId")
    Optional<Double> findAverageByStudentId(@Param("studentId") UUID studentId);

    @Modifying
    @Query(value = """
            UPDATE skill_mastery sm SET skill_name = :newName
            WHERE sm.skill_name = :oldName
              AND sm.student_id IN (
                  SELECT s.student_id FROM submissions s
                  JOIN tasks t ON t.id = s.task_id
                  WHERE t.outcome_id = :outcomeId)
              AND NOT EXISTS (
                  SELECT 1 FROM skill_mastery x
                  WHERE x.student_id = sm.student_id AND x.skill_name = :newName)
            """, nativeQuery = true)
    int renameSkillForOutcome(@Param("outcomeId") UUID outcomeId,
                              @Param("oldName") String oldName,
                              @Param("newName") String newName);
}