package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<Submission> findByTaskIdOrderByCreatedAtDesc(UUID taskId);
    Page<Submission> findByStudentId(UUID studentId, Pageable pageable);
    Page<Submission> findByTaskId(UUID taskId, Pageable pageable);
    long countByStudentId(UUID studentId);
    long countByTaskId(UUID taskId);
    List<Submission> findByStudentIdAndTaskId(UUID studentId, UUID taskId);
    List<Submission> findByStatus(SubmissionStatus status);
}
