package hr.algebra.adaptive.learning.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardResponse {

    // Teacher info
    private UserResponse teacher;
    
    // Statistike
    private int totalStudents;
    private int totalCourses;
    private int totalTasks;
    private int totalSubmissions;
    private int pendingReviews;
    
    // Liste
    private List<SubmissionResponse> recentSubmissions;
    private List<StudentProgressSummary> studentProgress;
    private List<CourseResponse> courses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProgressSummary {
        private String studentId;
        private String studentName;
        private double averageMastery;
        private int totalSubmissions;
        private LocalDateTime lastActivity;
    }
}
