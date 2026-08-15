package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StudentSubmissionsOverviewResponse {

    private int totalStudents;
    private int totalSubmissions;
    private List<StudentBlock> students;

    @Data
    @Builder
    public static class StudentBlock {
        private UUID studentId;
        private String firstName;
        private String lastName;
        private String email;
        private String schoolClassName;
        private ResearchGroup researchGroup;

        private int totalSubmissions;
        private int tasksAttempted;
        private int withAiFeedback;
        private Double averageScore;
        private LocalDateTime lastActivity;

        private List<AttemptEntry> attempts;
    }

    @Data
    @Builder
    public static class AttemptEntry {
        private UUID submissionId;
        private UUID taskId;
        private String taskTitle;
        private String courseName;
        private SubmissionStatus status;

        private Integer aiScore;
        private Integer teacherScore;
        private Integer finalScore;
        private Integer maxScore;
        private Integer testsPassed;
        private Integer testsTotal;

        private String aiFeedback;
        private String teacherFeedback;

        private Long executionTimeMs;
        private LocalDateTime createdAt;
    }
}
