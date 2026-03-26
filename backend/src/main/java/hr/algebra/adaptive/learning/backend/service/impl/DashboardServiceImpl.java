package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.*;
import hr.algebra.adaptive.learning.backend.repository.*;
import hr.algebra.adaptive.learning.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SchoolClassRepository classRepository;
    private final SubmissionRepository submissionRepository;
    private final SkillMasteryRepository skillMasteryRepository;
    private final TaskRepository taskRepository;

    @Override
    public StudentDashboardResponse getStudentDashboard(UUID studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));

        List<SchoolClass> classes = classRepository.findByStudentsId(studentId);
        List<Course> enrolledCourses = classes.stream()
                .flatMap(sc -> sc.getCourses().stream())
                .distinct()
                .toList();

        List<SkillMastery> masteries = skillMasteryRepository.findByStudentId(studentId);
        List<Submission> recentSubmissions = submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        return StudentDashboardResponse.builder()
                .student(mapToStudentResponse(student))
                .enrolledCourses(enrolledCourses.stream().map(this::mapToCourseResponse).toList())
                .recentSubmissions(recentSubmissions.stream().limit(5).map(this::mapToSubmissionResponse).toList())
                .skillMasteries(masteries.stream().map(this::mapToSkillMasteryResponse).toList())
                .build();
    }

    @Override
    public TeacherDashboardResponse getTeacherDashboard(UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Nastavnik nije pronađen"));

        List<Course> courses = courseRepository.findByCreatedById(teacherId);

        return TeacherDashboardResponse.builder()
                .teacher(mapToUserResponse(teacher))
                .courses(courses.stream().map(this::mapToCourseResponse).toList())
                .totalCourses(courses.size())
                .build();
    }

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        AdminDashboardResponse.SystemHealth systemHealth = AdminDashboardResponse.SystemHealth.builder()
                .databaseStatus("UP")
                .mlServiceStatus("UP")
                .codeExecutorStatus("UP")
                .build();

        return AdminDashboardResponse.builder()
                .totalUsers((int) userRepository.count())
                .totalStudents((int) userRepository.countByRole(UserRole.STUDENT))
                .totalTeachers((int) userRepository.countByRole(UserRole.TEACHER))
                .totalAdmins((int) userRepository.countByRole(UserRole.ADMIN))
                .activeUsers((int) userRepository.countByIsActiveTrue())
                .totalCourses((int) courseRepository.count())
                .activeCourses((int) courseRepository.countByIsActiveTrue())
                .totalClasses((int) classRepository.count())
                .totalTasks((int) taskRepository.count())
                .totalSubmissions((int) submissionRepository.count())
                .systemHealth(systemHealth)
                .build();
    }

    private StudentResponse mapToStudentResponse(User user) {
        return StudentResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private CourseResponse mapToCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .languageType(course.getLanguageType())
                .isActive(course.isActive())
                .outcomesCount(course.getOutcomes() != null ? course.getOutcomes().size() : 0)
                .build();
    }

    private SubmissionResponse mapToSubmissionResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .status(s.getStatus())
                .taskId(s.getTask().getId())
                .taskTitle(s.getTask().getTitle())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private SkillMasteryResponse mapToSkillMasteryResponse(SkillMastery sm) {
        return SkillMasteryResponse.builder()
                .id(sm.getId())
                .skillName(sm.getSkillName())
                .masteryLevel(sm.getMasteryLevel())
                .build();
    }
}