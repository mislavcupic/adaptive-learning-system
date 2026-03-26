package hr.algebra.adaptive.learning.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardResponse {

    // Student info - FRONTEND OVO TREBA!
    private StudentResponse student;
    
    // Statistike
    private int totalSubmissions;      // bilo: submissionsCount
    private int completedTasks;        // NOVO - nije postojalo
    private int pendingTasks;          // bilo: pendingTasksCount (broj, ne lista)
    private double averageMastery;
    
    // Liste
    private List<SubmissionResponse> recentSubmissions;
    private List<SkillMasteryResponse> skillMasteries;  // bilo: skillProgress
    private List<CourseResponse> enrolledCourses;       // bilo: coursesCount (samo broj)
}
