package hr.algebra.adaptive.learning.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentDashboardResponse {
    private int coursesCount;
    private long submissionsCount;
    private double averageMastery;
    private int pendingTasksCount;
    private List<TaskResponse> pendingTasks;
    private List<SubmissionResponse> recentSubmissions;
    private List<SkillMasteryResponse> skillProgress;
}