package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, UUID> {

    Optional<ExamAnswer> findByAttemptIdAndTaskId(UUID attemptId, UUID taskId);

    List<ExamAnswer> findByAttemptId(UUID attemptId);

    long countByAttemptIdAndMarkedDoneTrue(UUID attemptId);
}