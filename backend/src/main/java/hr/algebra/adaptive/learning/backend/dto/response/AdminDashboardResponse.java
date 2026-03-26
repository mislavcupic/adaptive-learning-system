package hr.algebra.adaptive.learning.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private int totalUsers;
    private int totalStudents;
    private int totalTeachers;
    private int totalAdmins;
    private int activeUsers;
    private int totalCourses;
    private int activeCourses;
    private int totalClasses;
    private int totalTasks;
    private int totalSubmissions;
    private SystemHealth systemHealth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealth {
        private String databaseStatus;
        private String mlServiceStatus;
        private String codeExecutorStatus;
    }
}
