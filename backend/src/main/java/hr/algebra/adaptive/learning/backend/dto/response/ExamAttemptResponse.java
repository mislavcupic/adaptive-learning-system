package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.ExamAnswer;
import hr.algebra.adaptive.learning.backend.domain.entity.ExamAttempt;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExamAttemptResponse {

    private UUID id;

    private UUID examId;
    private String examTitle;

    private UUID studentId;
    private String studentName;
    private String studentEmail;

    private ExamAttemptStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime deadlineAt;

    /** Preostalo vrijeme u sekundama; prazno ako nema ogranicenja. */
    private Long remainingSeconds;

    private Integer totalScore;
    private Integer maxScore;
    private Double percentage;
    private Boolean passed;

    private String teacherFeedback;
    private LocalDateTime reviewedAt;

    private List<AnswerEntry> answers;

    public static ExamAttemptResponse fromEntity(ExamAttempt attempt) {
        return base(attempt).build();
    }

    public static ExamAttemptResponse fromEntityWithAnswers(ExamAttempt attempt) {
        return base(attempt)
                .answers(attempt.getAnswers().stream()
                        .sorted(Comparator.comparing(
                                a -> a.getTask().getOrderIndex() != null
                                        ? a.getTask().getOrderIndex() : 0))
                        .map(AnswerEntry::fromEntity)
                        .toList())
                .build();
    }

    private static ExamAttemptResponseBuilder base(ExamAttempt attempt) {
        Double percentage = attempt.getPercentage();
        Integer passingScore = attempt.getExam().getPassingScore();

        return ExamAttemptResponse.builder()
                .id(attempt.getId())
                .examId(attempt.getExam().getId())
                .examTitle(attempt.getExam().getTitle())
                .studentId(attempt.getStudent().getId())
                .studentName(attempt.getStudent().getFirstName() + " "
                        + attempt.getStudent().getLastName())
                .studentEmail(attempt.getStudent().getEmail())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .deadlineAt(attempt.getDeadlineAt())
                .remainingSeconds(calculateRemaining(attempt))
                .totalScore(attempt.getTotalScore())
                .maxScore(attempt.getMaxScore())
                .percentage(percentage)
                .passed(percentage != null && passingScore != null
                        ? percentage >= passingScore : null)
                .teacherFeedback(attempt.getTeacherFeedback())
                .reviewedAt(attempt.getReviewedAt());
    }

    /**
     * Preostalo vrijeme racuna se na posluzitelju kako ga student ne bi
     * mogao produljiti mijenjanjem sata na svom uredaju.
     */
    private static Long calculateRemaining(ExamAttempt attempt) {
        if (attempt.getDeadlineAt() == null) return null;
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) return 0L;

        long seconds = java.time.Duration.between(
                LocalDateTime.now(), attempt.getDeadlineAt()).getSeconds();
        return Math.max(0, seconds);
    }

    @Data
    @Builder
    public static class AnswerEntry {
        private UUID taskId;
        private String taskTitle;
        private TaskType taskType;

        private String answerContent;
        private boolean markedDone;
        private LocalDateTime answeredAt;

        private Integer aiScore;
        private Integer teacherScore;
        private Integer finalScore;
        private Integer maxScore;
        private String teacherFeedback;

        /** Rezultati izvrsavanja koda; prazno za ostale tipove. */
        private Integer testsPassed;
        private Integer testsTotal;
        private String compilerOutput;

        /** Dostupan tek nakon predaje ispita. */
        private String aiFeedback;

        public static AnswerEntry fromEntity(ExamAnswer answer) {
            var builder = AnswerEntry.builder()
                    .taskId(answer.getTask().getId())
                    .taskTitle(answer.getTask().getTitle())
                    .taskType(answer.getTask().getTaskType())
                    .answerContent(answer.getAnswerContent())
                    .markedDone(answer.isMarkedDone())
                    .answeredAt(answer.getAnsweredAt())
                    .aiScore(answer.getAiScore())
                    .teacherScore(answer.getTeacherScore())
                    .finalScore(answer.getFinalScore())
                    .maxScore(answer.getTask().getMaxScore())
                    .teacherFeedback(answer.getTeacherFeedback());

            if (answer.getSubmission() != null) {
                builder.testsPassed(answer.getSubmission().getTestsPassed())
                        .testsTotal(answer.getSubmission().getTestsTotal())
                        .compilerOutput(answer.getSubmission().getCompilerOutput());

                // AI feedback se otkriva tek kad je ispit predan
                if (answer.getAttempt().getStatus() != ExamAttemptStatus.IN_PROGRESS) {
                    builder.aiFeedback(answer.getSubmission().getAiFeedback());
                }
            }

            return builder.build();
        }
    }
}
