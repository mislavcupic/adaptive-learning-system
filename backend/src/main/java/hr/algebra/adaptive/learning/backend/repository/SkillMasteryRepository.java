package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.SkillMastery;
import org.springframework.data.jpa.repository.JpaRepository;
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
}