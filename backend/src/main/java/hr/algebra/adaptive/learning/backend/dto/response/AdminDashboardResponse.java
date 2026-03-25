package hr.algebra.adaptive.learning.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalStudents;
    private long totalTeachers;
    private long totalClasses;
    private long totalCourses;
    private long totalSubmissions;
    private Map<String, Long> usersByRole;
    private Map<String, Long> submissionsByStatus;
    private Map<String, Double> averageMasteryByClass;
}