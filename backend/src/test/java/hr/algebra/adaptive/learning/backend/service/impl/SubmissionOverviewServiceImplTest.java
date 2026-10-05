package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.response.StudentSubmissionsOverviewResponse;
import hr.algebra.adaptive.learning.backend.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubmissionOverviewServiceImpl")
class SubmissionOverviewServiceImplTest {

    @Mock private SubmissionRepository submissionRepository;

    @InjectMocks private SubmissionOverviewServiceImpl overviewService;

    private User experimental;
    private User control;
    private Task taskOne;
    private Task taskTwo;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();

        Course course = new Course();
        course.setId(courseId);
        course.setName("Uvod u programiranje");
        course.setLanguageType(LanguageType.C);

        LearningOutcome outcome = new LearningOutcome();
        outcome.setId(UUID.randomUUID());
        outcome.setName("Petlje");
        outcome.setCourse(course);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(UUID.randomUUID());
        schoolClass.setName("2.A");

        experimental = student("ana@test.hr", "Ana", ResearchGroup.EXPERIMENTAL, schoolClass);
        control = student("ivo@test.hr", "Ivo", ResearchGroup.CONTROL, schoolClass);

        taskOne = task("Zbroj niza", outcome);
        taskTwo = task("Najveći element", outcome);
    }

    @Nested
    @DisplayName("Grupiranje po studentu")
    class Grouping {

        @Test
        @DisplayName("svaki student dobiva vlastiti blok")
        void oneBlockPerStudent() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(
                            submission(experimental, taskOne, 18, "Dobro"),
                            submission(control, taskOne, 12, null)));

            var result = overviewService.getOverview(null, null, null);

            assertThat(result.getTotalStudents()).isEqualTo(2);
            assertThat(result.getStudents()).hasSize(2);
        }

        @Test
        @DisplayName("više predaja istog studenta ide u isti blok")
        void mergesSubmissionsOfSameStudent() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(
                            submission(experimental, taskOne, 10, "Prvi"),
                            submission(experimental, taskTwo, 15, "Drugi")));

            var result = overviewService.getOverview(null, null, null);

            assertThat(result.getStudents()).hasSize(1);
            assertThat(result.getStudents().get(0).getTotalSubmissions()).isEqualTo(2);
        }

        @Test
        @DisplayName("broji različite zadatke, ne predaje")
        void countsDistinctTasks() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(
                            submission(experimental, taskOne, 8, null),
                            submission(experimental, taskOne, 14, null),
                            submission(experimental, taskTwo, 20, null)));

            var block = overviewService.getOverview(null, null, null).getStudents().get(0);

            assertThat(block.getTotalSubmissions()).isEqualTo(3);
            assertThat(block.getTasksAttempted()).isEqualTo(2);
        }

        @Test
        @DisplayName("ukupan broj predaja odgovara dohvaćenima")
        void totalMatchesFetched() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(
                            submission(experimental, taskOne, 10, null),
                            submission(control, taskTwo, 12, null)));

            assertThat(overviewService.getOverview(null, null, null).getTotalSubmissions())
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("vraća prazan pregled kad nema predaja")
        void emptyWhenNoSubmissions() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of());

            var result = overviewService.getOverview(null, null, null);

            assertThat(result.getTotalStudents()).isZero();
            assertThat(result.getStudents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Sažetak po studentu")
    class Summary {

        @Test
        @DisplayName("broji predaje koje imaju AI povratnu informaciju")
        void countsAiFeedback() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(
                            submission(experimental, taskOne, 10, "Postoji"),
                            submission(experimental, taskTwo, 12, null),
                            submission(experimental, taskOne, 14, "   ")));

            var block = overviewService.getOverview(null, null, null).getStudents().get(0);

            assertThat(block.getWithAiFeedback()).isEqualTo(1);
        }

        @Test
        @DisplayName("računa prosječan broj bodova")
        void averagesScores() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(
                            submission(experimental, taskOne, 10, null),
                            submission(experimental, taskTwo, 20, null)));

            var block = overviewService.getOverview(null, null, null).getStudents().get(0);

            assertThat(block.getAverageScore()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("prosjek je prazan kad nijedna predaja nema bodove")
        void noAverageWithoutScores() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(submission(experimental, taskOne, null, null)));

            var block = overviewService.getOverview(null, null, null).getStudents().get(0);

            assertThat(block.getAverageScore()).isNull();
        }

        @Test
        @DisplayName("zadržava pripadnost istraživačkoj skupini")
        void keepsResearchGroup() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(submission(control, taskOne, 10, null)));

            var block = overviewService.getOverview(null, null, null).getStudents().get(0);

            assertThat(block.getResearchGroup()).isEqualTo(ResearchGroup.CONTROL);
        }

        @Test
        @DisplayName("prenosi naziv razreda")
        void carriesClassName() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of(submission(experimental, taskOne, 10, null)));

            var block = overviewService.getOverview(null, null, null).getStudents().get(0);

            assertThat(block.getSchoolClassName()).isEqualTo("2.A");
        }
    }

    @Nested
    @DisplayName("Filtriranje")
    class Filtering {

        @Test
        @DisplayName("bez razdoblja ne postavlja donju granicu")
        void noDateBoundWithoutPeriod() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of());

            overviewService.getOverview(null, null, null);

            verify(submissionRepository).findAllForTeacherOverview(isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("razdoblje postavlja donju granicu u prošlost")
        void setsLowerBoundForPeriod() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of());

            overviewService.getOverview(null, null, 7);

            ArgumentCaptor<LocalDateTime> captor =
                    ArgumentCaptor.forClass(LocalDateTime.class);
            verify(submissionRepository)
                    .findAllForTeacherOverview(isNull(), isNull(), captor.capture());

            assertThat(captor.getValue()).isBefore(LocalDateTime.now());
            assertThat(captor.getValue()).isAfter(LocalDateTime.now().minusDays(8));
        }

        @Test
        @DisplayName("nula dana se tretira kao bez ograničenja")
        void zeroDaysMeansNoBound() {
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of());

            overviewService.getOverview(null, null, 0);

            verify(submissionRepository).findAllForTeacherOverview(isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("prosljeđuje kolegij i studenta repozitoriju")
        void passesFiltersThrough() {
            UUID studentId = experimental.getId();
            when(submissionRepository.findAllForTeacherOverview(any(), any(), any()))
                    .thenReturn(List.of());

            overviewService.getOverview(courseId, studentId, null);

            verify(submissionRepository)
                    .findAllForTeacherOverview(eq(courseId), eq(studentId), isNull());
        }
    }

    private User student(String email, String firstName,
                         ResearchGroup group, SchoolClass schoolClass) {
        User u = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName("Testić")
                .researchGroup(group)
                .schoolClass(schoolClass)
                .build();
        u.setId(UUID.randomUUID());
        return u;
    }

    private Task task(String title, LearningOutcome outcome) {
        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setTitle(title);
        t.setTaskType(TaskType.CODE);
        t.setMaxScore(20);
        t.setOutcome(outcome);
        return t;
    }

    private Submission submission(User student, Task task,
                                  Integer score, String aiFeedback) {
        Submission s = Submission.builder()
                .student(student)
                .task(task)
                .submittedCode("kod")
                .status(SubmissionStatus.COMPLETED)
                .finalScore(score)
                .aiFeedback(aiFeedback)
                .build();
        s.setId(UUID.randomUUID());
        s.setCreatedAt(LocalDateTime.now().minusHours(1));
        return s;
    }
}
