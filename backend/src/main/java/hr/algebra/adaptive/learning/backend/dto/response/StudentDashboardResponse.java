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


    private StudentResponse student;
    

    private int totalSubmissions;
    private int completedTasks;
    private int pendingTasks;
    private double averageMastery;
    

    private List<SubmissionResponse> recentSubmissions;
    private List<SkillMasteryResponse> skillMasteries;
    private List<CourseResponse> enrolledCourses;
}
