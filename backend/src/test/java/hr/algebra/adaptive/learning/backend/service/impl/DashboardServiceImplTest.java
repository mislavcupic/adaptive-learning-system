package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.StudentDashboardResponse;
import hr.algebra.adaptive.learning.backend.dto.response.TeacherDashboardResponse;
import hr.algebra.adaptive.learning.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardServiceImpl")
class DashboardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private SkillMasteryRepository skillMasteryRepository;
    @Mock private TaskRepository taskRepository;

    @InjectMocks private DashboardServiceImpl dashboardService;

    private UUID studentId;
    private UUID teacherId;
    private UUID courseId;
    private User student;
    private User teacher;
    private Course course;
    private LearningOutcome outcome;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        course = new Course();
        course.setId(courseId);
        course.setName("Uvod u programiranje");
        course.setLanguageType(LanguageType.C);
        course.setOutcomes(new ArrayList<>());

        outcome = new LearningOutcome();
        outcome.setId(UUID.randomUUID());
        outcome.setName("Petlje");
        outcome.setCourse(course);
        course.getOutcomes().add(outcome);

        student = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
        student.setId(studentId);

        teacher = User.builder()
                .email("ines@test.hr")
                .firstName("Ines")
                .lastName("Inić")
                .role(UserRole.TEACHER)
                .isActive(true)
                .build();
        teacher.setId(teacherId);

        schoolClass = new SchoolClass();
        schoolClass.setId(UUID.randomUUID());
        schoolClass.setName("2.A");
        schoolClass.setCourses(new java.util.HashSet<>(Set.of(course)));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    }

    @Nested
    @DisplayName("Studentska nadzorna ploča")
    class StudentDashboard {

        @Test
        @DisplayName("broji preostale zadatke kao razliku dostupnih i riješenih")
        void countsPendingTasks() {
            stubStudentBasics();
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of(submission(), submission()));
            when(submissionRepository.countByStudentId(studentId)).thenReturn(2L);
            when(taskRepository.countByOutcomeCourseIdInAndIsActiveTrue(anyCollection()))
                    .thenReturn(10L);

            StudentDashboardResponse result = dashboardService.getStudentDashboard(studentId);

            assertThat(result.getCompletedTasks()).isEqualTo(2);
            assertThat(result.getPendingTasks()).isEqualTo(8);
        }

        @Test
        @DisplayName("broji svaki zadatak jednom bez obzira na broj predaja")
        void countsDistinctTasks() {
            Task sameTask = task();
            Submission first = submissionFor(sameTask);
            Submission second = submissionFor(sameTask);

            stubStudentBasics();
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of(first, second));
            when(submissionRepository.countByStudentId(studentId)).thenReturn(2L);
            when(taskRepository.countByOutcomeCourseIdInAndIsActiveTrue(anyCollection()))
                    .thenReturn(5L);

            StudentDashboardResponse result = dashboardService.getStudentDashboard(studentId);

            assertThat(result.getTotalSubmissions()).isEqualTo(2);
            assertThat(result.getCompletedTasks()).isEqualTo(1);
            assertThat(result.getPendingTasks()).isEqualTo(4);
        }

        @Test
        @DisplayName("preostali zadaci ne padaju ispod nule")
        void pendingNeverNegative() {
            stubStudentBasics();
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of(submission(), submission(), submission()));
            when(submissionRepository.countByStudentId(studentId)).thenReturn(3L);
            when(taskRepository.countByOutcomeCourseIdInAndIsActiveTrue(anyCollection()))
                    .thenReturn(1L);

            StudentDashboardResponse result = dashboardService.getStudentDashboard(studentId);

            assertThat(result.getPendingTasks()).isZero();
        }

        @Test
        @DisplayName("računa prosječnu razinu usvojenosti")
        void averagesMastery() {
            stubStudentBasics();
            when(skillMasteryRepository.findByStudentId(studentId))
                    .thenReturn(List.of(mastery(0.4), mastery(0.8)));
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of());
            when(submissionRepository.countByStudentId(studentId)).thenReturn(0L);

            StudentDashboardResponse result = dashboardService.getStudentDashboard(studentId);

            assertThat(result.getAverageMastery()).isCloseTo(0.6, within());
        }

        @Test
        @DisplayName("prosjek je nula kad nema procjena")
        void zeroAverageWithoutMastery() {
            stubStudentBasics();
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of());
            when(submissionRepository.countByStudentId(studentId)).thenReturn(0L);

            StudentDashboardResponse result = dashboardService.getStudentDashboard(studentId);

            assertThat(result.getAverageMastery()).isZero();
        }

        @Test
        @DisplayName("prikazuje najviše pet nedavnih predaja")
        void limitsRecentSubmissions() {
            stubStudentBasics();
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of(submission(), submission(), submission(),
                            submission(), submission(), submission(), submission()));
            when(submissionRepository.countByStudentId(studentId)).thenReturn(7L);
            when(taskRepository.countByOutcomeCourseIdInAndIsActiveTrue(anyCollection()))
                    .thenReturn(20L);

            StudentDashboardResponse result = dashboardService.getStudentDashboard(studentId);

            assertThat(result.getRecentSubmissions()).hasSize(5);
        }

        @Test
        @DisplayName("ne broji zadatke kad student nije upisan ni u jedan kolegij")
        void noTasksWithoutCourses() {
            when(classRepository.findByStudentsId(studentId)).thenReturn(List.of());
            when(skillMasteryRepository.findByStudentId(studentId)).thenReturn(List.of());
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of());
            when(submissionRepository.countByStudentId(studentId)).thenReturn(0L);

            StudentDashboardResponse result = dashboardService.getStudentDashboard(studentId);

            assertThat(result.getPendingTasks()).isZero();
            assertThat(result.getEnrolledCourses()).isEmpty();
            verify(taskRepository, never()).countByOutcomeCourseIdInAndIsActiveTrue(anyCollection());
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg studenta")
        void throwsForUnknownStudent() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dashboardService.getStudentDashboard(unknown))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("nije pronađen");
        }
    }

    @Nested
    @DisplayName("Nastavnička nadzorna ploča")
    class TeacherDashboard {

        @Test
        @DisplayName("prikazuje brojeve kolegija, studenata i zadataka")
        void showsCounts() {
            stubTeacherBasics();
            when(courseRepository.findByCreatedById(teacherId)).thenReturn(List.of(course));
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT))
                    .thenReturn(List.of(student));
            when(taskRepository.count()).thenReturn(12L);
            when(submissionRepository.count()).thenReturn(40L);

            TeacherDashboardResponse result = dashboardService.getTeacherDashboard(teacherId);

            assertThat(result.getTotalCourses()).isEqualTo(1);
            assertThat(result.getTotalStudents()).isEqualTo(1);
            assertThat(result.getTotalTasks()).isEqualTo(12);
            assertThat(result.getTotalSubmissions()).isEqualTo(40);
        }

        @Test
        @DisplayName("sastavlja sažetak napretka po studentu")
        void buildsStudentProgress() {
            stubTeacherBasics();
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT))
                    .thenReturn(List.of(student));
            when(skillMasteryRepository.findByStudentId(studentId))
                    .thenReturn(List.of(mastery(0.7), mastery(0.9)));
            when(submissionRepository.countByStudentId(studentId)).thenReturn(6L);

            TeacherDashboardResponse result = dashboardService.getTeacherDashboard(teacherId);

            assertThat(result.getStudentProgress()).hasSize(1);
            var progress = result.getStudentProgress().get(0);
            assertThat(progress.getStudentName()).isEqualTo("Ana Anić");
            assertThat(progress.getAverageMastery()).isCloseTo(0.8, within());
            assertThat(progress.getTotalSubmissions()).isEqualTo(6);
        }

        @Test
        @DisplayName("prikazuje najviše deset nedavnih predaja")
        void limitsRecentSubmissions() {
            stubTeacherBasics();
            List<Submission> many = new ArrayList<>();
            for (int i = 0; i < 15; i++) many.add(submission());
            when(submissionRepository.findAllOrderByCreatedAtDesc()).thenReturn(many);

            TeacherDashboardResponse result = dashboardService.getTeacherDashboard(teacherId);

            assertThat(result.getRecentSubmissions()).hasSize(10);
        }

        @Test
        @DisplayName("radi i kad nastavnik nema kolegija")
        void handlesNoCourses() {
            stubTeacherBasics();
            when(courseRepository.findByCreatedById(teacherId)).thenReturn(List.of());

            TeacherDashboardResponse result = dashboardService.getTeacherDashboard(teacherId);

            assertThat(result.getTotalCourses()).isZero();
            assertThat(result.getCourses()).isEmpty();
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg nastavnika")
        void throwsForUnknownTeacher() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dashboardService.getTeacherDashboard(unknown))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("dohvat preko e-maila vraća istu ploču")
        void fetchesByEmail() {
            stubTeacherBasics();
            when(userRepository.findByEmail("ines@test.hr")).thenReturn(Optional.of(teacher));

            TeacherDashboardResponse result =
                    dashboardService.getTeacherDashboardByEmail("ines@test.hr");

            assertThat(result.getTeacher().getEmail()).isEqualTo("ines@test.hr");
        }
    }

    @Nested
    @DisplayName("Administratorska nadzorna ploča")
    class AdminDashboard {

        @Test
        @DisplayName("prijavljuje stanje sustava")
        void reportsSystemHealth() {
            var result = dashboardService.getAdminDashboard();

            assertThat(result.getSystemHealth()).isNotNull();
            assertThat(result.getSystemHealth().getDatabaseStatus()).isEqualTo("UP");
        }
    }

    private org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(0.001);
    }

    private void stubStudentBasics() {
        when(classRepository.findByStudentsId(studentId)).thenReturn(List.of(schoolClass));
        when(skillMasteryRepository.findByStudentId(studentId)).thenReturn(List.of());
    }

    private void stubTeacherBasics() {
        when(courseRepository.findByCreatedById(teacherId)).thenReturn(List.of(course));
        when(userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT)).thenReturn(List.of());
        when(submissionRepository.findAllOrderByCreatedAtDesc()).thenReturn(List.of());
        when(taskRepository.count()).thenReturn(0L);
        when(submissionRepository.count()).thenReturn(0L);
    }

    private Task task() {
        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setTitle("Zadatak");
        t.setTaskType(TaskType.CODE);
        t.setMaxScore(20);
        t.setOutcome(outcome);
        return t;
    }

    private Submission submission() {
        return submissionFor(task());
    }

    private Submission submissionFor(Task task) {
        Submission s = Submission.builder()
                .student(student)
                .task(task)
                .submittedCode("kod")
                .status(SubmissionStatus.COMPLETED)
                .build();
        s.setId(UUID.randomUUID());
        return s;
    }

    private SkillMastery mastery(double level) {
        SkillMastery m = new SkillMastery();
        m.setId(UUID.randomUUID());
        m.setStudent(student);
        m.setSkillName("Petlje");
        m.setMasteryLevel(level);
        m.setAttemptsCount(3);
        m.setCorrectCount(2);
        return m;
    }
}