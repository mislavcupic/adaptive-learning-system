package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SubmissionResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID taskId;
    private String taskTitle;
    private String submittedCode;
    private SubmissionStatus status;
    private String compilerOutput;
    private String executionOutput;
    private String testResults;
    private String valgrindOutput;
    private Long executionTimeMs;
    private Long memoryUsedKb;
    private String aiFeedback;
    private Integer aiScore;
    private String teacherFeedback;
    private Integer teacherScore;
    private Integer finalScore;
    private Integer testsPassed;
    private Integer testsTotal;
    private LocalDateTime createdAt;

    public static SubmissionResponse fromEntity(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .studentId(submission.getStudent().getId())
                .studentName(submission.getStudent().getFirstName() + " " + submission.getStudent().getLastName())
                .taskId(submission.getTask().getId())
                .taskTitle(submission.getTask().getTitle())
                .submittedCode(submission.getSubmittedCode())
                .status(submission.getStatus())
                .compilerOutput(submission.getCompilerOutput())
                .executionOutput(submission.getExecutionOutput())
                .testResults(submission.getTestResults())
                .valgrindOutput(submission.getValgrindOutput())
                .executionTimeMs(submission.getExecutionTimeMs())
                .memoryUsedKb(submission.getMemoryUsedKb())
                .aiFeedback(submission.getAiFeedback())
                .aiScore(submission.getAiScore())
                .teacherFeedback(submission.getTeacherFeedback())
                .teacherScore(submission.getTeacherScore())
                .finalScore(submission.getFinalScore())
                .testsPassed(submission.getTestsPassed())
                .testsTotal(submission.getTestsTotal())
                .createdAt(submission.getCreatedAt())
                .build();
    }
}
