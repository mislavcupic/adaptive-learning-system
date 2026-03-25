package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import hr.algebra.adaptive.learning.backend.domain.entity.SkillMastery;
import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.*;
import hr.algebra.adaptive.learning.backend.repository.*;
import hr.algebra.adaptive.learning.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final SchoolClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final SkillMasteryRepository skillMasteryRepository;

    @Override
    public StudentDashboardResponse getStudentDashboard(UUID studentId) {
        log.info("Getting dashboard for student: {}", studentId);

        List<SchoolClass> studentClasses = classRepository.findByStudentsId(studentId);
        Set<UUID> courseIds = studentClasses.stream()
                .flatMap(sc -> sc.getCourses().stream())
                .map(c -> c.getId())
                .collect(Collectors.toSet());

        long submissionsCount = submissionRepository.countByStudentId(studentId);

        List<SkillMastery> masteries = skillMasteryRepository.findByStudentId(studentId);
        double averageMastery = masteries.isEmpty() ? 0.0 :
                masteries.stream()
                        .mapToDouble(SkillMastery::getMasteryLevel)
                        .average()
                        .orElse(0.0) * 100;

        List<Task> pendingTasks = taskRepository.findByDueDateAfterAndIsActiveTrue(LocalDateTime.now());

        List<Submission> recentSubmissions = submissionRepository
                .findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .limit(5)
                .toList();

        return StudentDashboardResponse.builder()
                .coursesCount(courseIds.size())
                .submissionsCount(submissionsCount)
                .averageMastery(Math.round(averageMastery * 100.0) / 100.0)
                .pendingTasksCount(pendingTasks.size())
                .pendingTasks(pendingTasks.stream()
                        .limit(5)
                        .map(TaskResponse::fromEntity)
                        .toList())
                .recentSubmissions(recentSubmissions.stream()
                        .map(SubmissionResponse::fromEntity)
                        .toList())
                .skillProgress(masteries.stream()
                        .map(SkillMasteryResponse::fromEntity)
                        .toList())
                .build();
    }

    @Override
    public TeacherDashboardResponse getTeacherDashboard(UUID teacherId) {
        log.info("Getting dashboard for teacher: {}", teacherId);

        List<SchoolClass> classes = classRepository.findByTeacherId(teacherId);

        int totalStudents = classes.stream()
                .mapToInt(sc -> sc.getStudents().size())
                .sum();

        long coursesCount = courseRepository.findByCreatedById(teacherId).size();

        List<Task> teacherTasks = taskRepository.findByCreatedById(teacherId);

        long pendingReviews = 0;
        for (Task task : teacherTasks) {
            pendingReviews += task.getSubmissions().stream()
                    .filter(s -> s.getTeacherFeedback() == null && s.getStatus() == SubmissionStatus.COMPLETED)
                    .count();
        }

        List<Submission> recentSubmissions = teacherTasks.stream()
                .flatMap(t -> t.getSubmissions().stream())
                .sorted(Comparator.comparing(Submission::getCreatedAt).reversed())
                .limit(10)
                .toList();

        return TeacherDashboardResponse.builder()
                .classesCount(classes.size())
                .totalStudents(totalStudents)
                .coursesCount((int) coursesCount)
                .tasksCount(teacherTasks.size())
                .pendingReviewsCount(pendingReviews)
                .averageClassMastery(0.0)
                .classes(classes.stream()
                        .map(SchoolClassResponse::fromEntity)
                        .toList())
                .recentSubmissions(recentSubmissions.stream()
                        .map(SubmissionResponse::fromEntity)
                        .toList())
                .studentsNeedingHelp(new ArrayList<>())
                .build();
    }

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        log.info("Getting admin dashboard");

        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(UserRole.STUDENT);
        long totalTeachers = userRepository.countByRole(UserRole.TEACHER);
        long totalClasses = classRepository.count();
        long totalCourses = courseRepository.count();
        long totalSubmissions = submissionRepository.count();

        Map<String, Long> usersByRole = new HashMap<>();
        usersByRole.put("STUDENT", totalStudents);
        usersByRole.put("TEACHER", totalTeachers);
        usersByRole.put("ADMIN", userRepository.countByRole(UserRole.ADMIN));

        Map<String, Long> submissionsByStatus = new HashMap<>();
        for (SubmissionStatus status : SubmissionStatus.values()) {
            submissionsByStatus.put(status.name(),
                    (long) submissionRepository.findByStatus(status).size());
        }

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalClasses(totalClasses)
                .totalCourses(totalCourses)
                .totalSubmissions(totalSubmissions)
                .usersByRole(usersByRole)
                .submissionsByStatus(submissionsByStatus)
                .averageMasteryByClass(new HashMap<>())
                .build();
    }
}