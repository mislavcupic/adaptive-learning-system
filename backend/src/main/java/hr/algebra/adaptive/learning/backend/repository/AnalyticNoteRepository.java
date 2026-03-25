package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.AnalyticNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticNoteRepository extends JpaRepository<AnalyticNote, UUID> {
    List<AnalyticNote> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<AnalyticNote> findByStudentIdAndSkillName(UUID studentId, String skillName);
    List<AnalyticNote> findBySubmissionId(UUID submissionId);
}
