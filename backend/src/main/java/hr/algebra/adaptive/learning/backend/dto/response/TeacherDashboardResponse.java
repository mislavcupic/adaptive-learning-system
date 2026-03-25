package hr.algebra.adaptive.learning.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeacherDashboardResponse {
    private int classesCount;
    private int totalStudents;
    private int coursesCount;
    private int tasksCount;
    private long pendingReviewsCount;
    private double averageClassMastery;
    private List<SchoolClassResponse> classes;
    private List<SubmissionResponse> recentSubmissions;
    private List<StudentProgressSummary> studentsNeedingHelp;

    @Data
    @Builder
    public static class StudentProgressSummary {
        private String studentId;
        private String studentName;
        private String className;
        private double masteryLevel;
        private int failedAttempts;
        private String strugglingSkill;
    }
}