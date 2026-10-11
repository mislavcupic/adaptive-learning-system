package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.request.ExamAnswerRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.request.ExamRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SubmissionResponse;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.*;
import hr.algebra.adaptive.learning.backend.service.SubmissionService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testovi poslovnih pravila ispita.
 *
 * Repozitoriji su mockani, pa se testira logika servisa bez baze.
 * Naglasak je na pravilima koja stite ispit: jedan pokusaj po studentu,
 * zakljucavanje nakon predaje, postivanje roka i automatsko ocjenjivanje.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExamServiceImpl")
class ExamServiceImplTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamAttemptRepository attemptRepository;
    @Mock private ExamAnswerRepository answerRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private SubmissionService submissionService;

    @InjectMocks private ExamServiceImpl examService;

    private UUID examId;
    private UUID studentId;
    private UUID teacherId;
    private UUID courseId;
    private User student;
    private User teacher;
    private Course course;
    private Exam exam;
    private Task codeTask;
    private Task choiceTask;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        course = new Course();
        course.setId(courseId);
        course.setName("Uvod u programiranje");

        student = new User();
        student.setId(studentId);
        student.setEmail("student@test.hr");
        student.setFirstName("Ana");
        student.setLastName("Anić");

        teacher = new User();
        teacher.setId(teacherId);
        teacher.setEmail("ines@test.hr");
        teacher.setFirstName("Ines");
        teacher.setLastName("Inić");

        LearningOutcome outcome = new LearningOutcome();
        outcome.setId(UUID.randomUUID());
        outcome.setName("Petlje");
        outcome.setCourse(course);

        codeTask = new Task();
        codeTask.setId(UUID.randomUUID());
        codeTask.setTitle("Zbroj niza");
        codeTask.setTaskType(TaskType.CODE);
        codeTask.setMaxScore(20);
        codeTask.setStarterCode("int main() {}");
        codeTask.setOutcome(outcome);

        choiceTask = new Task();
        choiceTask.setId(UUID.randomUUID());
        choiceTask.setTitle("Što radi for petlja?");
        choiceTask.setTaskType(TaskType.MULTIPLE_CHOICE);
        choiceTask.setMaxScore(10);
        choiceTask.setCorrectAnswer("Ponavlja blok koda");
        choiceTask.setOutcome(outcome);

        exam = Exam.builder()
                .title("Kolokvij 1")
                .course(course)
                .createdBy(teacher)
                .tasks(new ArrayList<>(List.of(codeTask, choiceTask)))
                .attempts(new ArrayList<>())
                .passingScore(50)
                .isPublished(true)
                .isActive(true)
                .showTestResults(true)
                .build();
        exam.setId(examId);

        when(examRepository.save(any(Exam.class))).thenAnswer(i -> i.getArgument(0));
        when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));
        when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> {
            ExamAttempt a = i.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
    }

    // ==================================================================
    // NASTAVNIK
    // ==================================================================

    @Nested
    @DisplayName("Kreiranje ispita")
    class Creating {

        @Test
        @DisplayName("sprema ispit sa zadacima")
        void createsExamWithTasks() {
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
            when(taskRepository.findById(codeTask.getId())).thenReturn(Optional.of(codeTask));

            ExamResponse result = examService.create(examRequest(List.of(codeTask.getId())), teacherId);

            assertThat(result.getTitle()).isEqualTo("Kolokvij 1");
            verify(examRepository).save(any(Exam.class));
        }

        @Test
        @DisplayName("bez zadanog praga prolaza postavlja 50")
        void defaultsPassingScore() {
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

            ExamRequest request = examRequest(null);
            request.setPassingScore(null);

            assertThat(examService.create(request, teacherId).getPassingScore()).isEqualTo(50);
        }

        @Test
        @DisplayName("postuje zadani prag prolaza")
        void keepsGivenPassingScore() {
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

            ExamRequest request = examRequest(null);
            request.setPassingScore(70);

            assertThat(examService.create(request, teacherId).getPassingScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("prazan popis zadataka daje ispit bez zadataka")
        void emptyTaskListIsAllowed() {
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

            examService.create(examRequest(List.of()), teacherId);

            verify(taskRepository, never()).findById(any());
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći kolegij")
        void missingCourse() {
            when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.create(examRequest(null), teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg nastavnika")
        void missingCreator() {
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.create(examRequest(null), teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći zadatak")
        void missingTask() {
            UUID strangerId = UUID.randomUUID();
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
            when(taskRepository.findById(strangerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.create(examRequest(List.of(strangerId)), teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Dohvat ispita")
    class Reading {

        @Test
        @DisplayName("vraća ispit sa zadacima")
        void returnsExam() {
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));

            assertThat(examService.getById(examId).getTitle()).isEqualTo("Kolokvij 1");
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći ispit")
        void missingExam() {
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.getById(examId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("vraća ispite kolegija")
        void returnsByCourse() {
            when(examRepository.findByCourseIdAndIsActiveTrueOrderByCreatedAtDesc(courseId))
                    .thenReturn(List.of(exam));

            assertThat(examService.getByCourse(courseId)).hasSize(1);
        }

        @Test
        @DisplayName("vraća ispite nastavnika")
        void returnsByTeacher() {
            when(examRepository.findByCreatedByIdAndIsActiveTrueOrderByCreatedAtDesc(teacherId))
                    .thenReturn(List.of(exam));

            assertThat(examService.getByTeacher(teacherId)).hasSize(1);
        }

        @Test
        @DisplayName("prazan popis kad nastavnik nema ispita")
        void emptyForTeacher() {
            when(examRepository.findByCreatedByIdAndIsActiveTrueOrderByCreatedAtDesc(teacherId))
                    .thenReturn(List.of());

            assertThat(examService.getByTeacher(teacherId)).isEmpty();
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Objava ispita")
    class Publishing {

        @Test
        @DisplayName("objavljuje ispit koji ima zadatke")
        void publishesExamWithTasks() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

            examService.publish(examId);

            assertThat(exam.isPublished()).isTrue();
            verify(examRepository).save(exam);
        }

        @Test
        @DisplayName("odbija objavu ispita bez zadataka")
        void rejectsPublishingEmptyExam() {
            exam.setTasks(new ArrayList<>());
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

            assertThatThrownBy(() -> examService.publish(examId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("bez zadataka");

            verify(examRepository, never()).save(any());
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći ispit")
        void throwsForMissingExam() {
            when(examRepository.findById(examId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.publish(examId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("skida ispit s objave")
        void unpublishes() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

            examService.unpublish(examId);

            assertThat(exam.isPublished()).isFalse();
            verify(examRepository).save(exam);
        }

        @Test
        @DisplayName("povlačenje objave traži postojeći ispit")
        void unpublishRequiresExam() {
            when(examRepository.findById(examId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.unpublish(examId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Izmjena ispita")
    class Updating {

        @Test
        @DisplayName("odbija izmjenu zadataka kad je ispit već započet")
        void rejectsTaskChangeAfterStart() {
            ExamAttempt existing = attempt(ExamAttemptStatus.IN_PROGRESS, null);

            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(attemptRepository.findByExamIdOrderByStartedAtDesc(examId))
                    .thenReturn(List.of(existing));

            ExamRequest request = new ExamRequest();
            request.setTitle("Novi naziv");
            request.setCourseId(courseId);
            request.setTaskIds(List.of(codeTask.getId()));

            assertThatThrownBy(() -> examService.update(examId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("već započet");
        }

        @Test
        @DisplayName("dopušta izmjenu naziva dok nema pokušaja")
        void allowsRenameWhenNoAttempts() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(attemptRepository.findByExamIdOrderByStartedAtDesc(examId))
                    .thenReturn(List.of());

            ExamRequest request = new ExamRequest();
            request.setTitle("Kolokvij 1 - ispravak");
            request.setCourseId(courseId);
            request.setPassingScore(60);

            ExamResponse result = examService.update(examId, request);

            assertThat(result.getTitle()).isEqualTo("Kolokvij 1 - ispravak");
            assertThat(result.getPassingScore()).isEqualTo(60);
        }

        @Test
        @DisplayName("zamjenjuje zadatke kad ispit nitko nije započeo")
        void replacesTasksWhenNoAttempts() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(attemptRepository.findByExamIdOrderByStartedAtDesc(examId)).thenReturn(List.of());
            when(taskRepository.findById(choiceTask.getId())).thenReturn(Optional.of(choiceTask));

            ExamRequest request = new ExamRequest();
            request.setTitle("Kolokvij 1");
            request.setCourseId(courseId);
            request.setTaskIds(List.of(choiceTask.getId()));

            examService.update(examId, request);

            assertThat(exam.getTasks()).containsExactly(choiceTask);
        }

        @Test
        @DisplayName("dopušta izmjenu naziva i nakon što je ispit započet")
        void allowsRenameAfterStartWhenTasksUntouched() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(attemptRepository.findByExamIdOrderByStartedAtDesc(examId))
                    .thenReturn(List.of(attempt(ExamAttemptStatus.IN_PROGRESS, null)));

            ExamRequest request = new ExamRequest();
            request.setTitle("Ispravljen naziv");
            request.setCourseId(courseId);
            request.setTaskIds(null);

            assertThat(examService.update(examId, request).getTitle())
                    .isEqualTo("Ispravljen naziv");
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Brisanje ispita")
    class Deleting {

        @Test
        @DisplayName("briše ispit koji nema pokušaja")
        void deletesExamWithoutAttempts() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(attemptRepository.findByExamIdOrderByStartedAtDesc(examId))
                    .thenReturn(List.of());

            examService.delete(examId);

            verify(examRepository).delete(exam);
        }

        @Test
        @DisplayName("samo deaktivira ispit koji ima pokušaje")
        void deactivatesExamWithAttempts() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(attemptRepository.findByExamIdOrderByStartedAtDesc(examId))
                    .thenReturn(List.of(attempt(ExamAttemptStatus.SUBMITTED, null)));

            examService.delete(examId);

            assertThat(exam.isActive()).isFalse();
            assertThat(exam.isPublished()).isFalse();
            verify(examRepository, never()).delete(any());
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Pregled pokušaja")
    class Attempts {

        @Test
        @DisplayName("vraća sve pokušaje na ispitu")
        void listsAttemptsForExam() {
            when(attemptRepository.findAllForExamOverview(examId))
                    .thenReturn(List.of(attempt(ExamAttemptStatus.SUBMITTED, null)));

            assertThat(examService.getAttemptsForExam(examId)).hasSize(1);
        }

        @Test
        @DisplayName("vraća pojedini pokušaj s odgovorima")
        void returnsSingleAttempt() {
            ExamAttempt a = attempt(ExamAttemptStatus.SUBMITTED, null);
            when(attemptRepository.findByIdWithAnswers(a.getId())).thenReturn(Optional.of(a));

            assertThat(examService.getAttempt(a.getId())).isNotNull();
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći pokušaj")
        void missingAttempt() {
            UUID id = UUID.randomUUID();
            when(attemptRepository.findByIdWithAnswers(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.getAttempt(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("vraća pokušaje studenta")
        void listsStudentAttempts() {
            when(attemptRepository.findByStudentIdOrderByStartedAtDesc(studentId))
                    .thenReturn(List.of(attempt(ExamAttemptStatus.GRADED, null)));

            assertThat(examService.getMyAttempts(studentId)).hasSize(1);
        }

        @Test
        @DisplayName("vraća studentov pokušaj na ispitu")
        void returnsMyAttempt() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(attempt(ExamAttemptStatus.IN_PROGRESS, null)));

            assertThat(examService.getMyAttempt(examId, studentId)).isNotNull();
        }

        @Test
        @DisplayName("baca iznimku kad student nije započeo ispit")
        void myAttemptMissing() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.getMyAttempt(examId, studentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Niste započeli");
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Dostupni ispiti studentu")
    class AvailableForStudent {

        @Test
        @DisplayName("označava ispit koji je student već započeo")
        void marksStartedExam() {
            ExamAttempt a = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            when(examRepository.findAvailableForStudents(courseId)).thenReturn(List.of(exam));
            when(attemptRepository.findByStudentIdAndExamId(studentId, examId))
                    .thenReturn(Optional.of(a));

            List<ExamResponse> result = examService.getAvailableForStudent(courseId, studentId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMyAttemptStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.get(0).getMyAttemptId()).isEqualTo(a.getId());
        }

        @Test
        @DisplayName("ispit koji student nije dirao nema oznaku pokušaja")
        void leavesUntouchedExamClean() {
            when(examRepository.findAvailableForStudents(courseId)).thenReturn(List.of(exam));
            when(attemptRepository.findByStudentIdAndExamId(studentId, examId))
                    .thenReturn(Optional.empty());

            List<ExamResponse> result = examService.getAvailableForStudent(courseId, studentId);

            assertThat(result.get(0).getMyAttemptStatus()).isNull();
            assertThat(result.get(0).getMyAttemptId()).isNull();
        }

        @Test
        @DisplayName("prazan popis kad nema dostupnih ispita")
        void noExamsAvailable() {
            when(examRepository.findAvailableForStudents(courseId)).thenReturn(List.of());

            assertThat(examService.getAvailableForStudent(courseId, studentId)).isEmpty();
        }
    }

    // ==================================================================
    // STUDENT
    // ==================================================================

    @Nested
    @DisplayName("Otvaranje pokušaja")
    class StartingAttempt {

        @Test
        @DisplayName("stvara pokušaj i prazne odgovore za svaki zadatak")
        void createsAttemptWithEmptyAnswers() {
            stubNewAttempt();

            ExamAttemptResponse result = examService.startAttempt(examId, studentId);

            assertThat(result.getStatus()).isEqualTo(ExamAttemptStatus.IN_PROGRESS);
            verify(answerRepository, times(2)).save(any(ExamAnswer.class));
        }

        @Test
        @DisplayName("postavlja rok kad ispit ima vremensko ograničenje")
        void setsDeadlineWhenTimeLimited() {
            exam.setTimeLimitMinutes(90);
            stubNewAttempt();

            ExamAttemptResponse result = examService.startAttempt(examId, studentId);

            assertThat(result.getDeadlineAt()).isNotNull();
            assertThat(result.getRemainingSeconds()).isGreaterThan(5000);
        }

        @Test
        @DisplayName("rok se skraćuje na kraj dostupnosti ispita")
        void capsDeadlineAtAvailableUntil() {
            exam.setTimeLimitMinutes(600);
            LocalDateTime until = LocalDateTime.now().plusMinutes(30);
            exam.setAvailableUntil(until);
            stubNewAttempt();

            assertThat(examService.startAttempt(examId, studentId).getDeadlineAt())
                    .isEqualTo(until);
        }

        @Test
        @DisplayName("bez vremenskog ograničenja nema roka")
        void noDeadlineWithoutTimeLimit() {
            exam.setTimeLimitMinutes(null);
            stubNewAttempt();

            assertThat(examService.startAttempt(examId, studentId).getDeadlineAt()).isNull();
        }

        @Test
        @DisplayName("vraća postojeći pokušaj umjesto da stvori novi")
        void returnsExistingAttempt() {
            ExamAttempt existing = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(existing));

            examService.startAttempt(examId, studentId);

            verify(attemptRepository, never()).save(any());
            verify(answerRepository, never()).save(any());
        }

        @Test
        @DisplayName("odbija ponovno otvaranje predanog ispita")
        void rejectsReopeningSubmitted() {
            ExamAttempt submitted = attempt(ExamAttemptStatus.SUBMITTED, null);
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(submitted));

            assertThatThrownBy(() -> examService.startAttempt(examId, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("već predali");
        }

        @Test
        @DisplayName("odbija otvaranje neobjavljenog ispita")
        void rejectsUnpublishedExam() {
            exam.setPublished(false);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));

            assertThatThrownBy(() -> examService.startAttempt(examId, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("nije dostupan");
        }

        @Test
        @DisplayName("odbija otvaranje deaktiviranog ispita")
        void rejectsInactiveExam() {
            exam.setActive(false);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));

            assertThatThrownBy(() -> examService.startAttempt(examId, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("nije dostupan");
        }

        @Test
        @DisplayName("odbija otvaranje prije početka dostupnosti")
        void rejectsBeforeAvailableFrom() {
            exam.setAvailableFrom(LocalDateTime.now().plusDays(1));

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));

            assertThatThrownBy(() -> examService.startAttempt(examId, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("još nije otvoren");
        }

        @Test
        @DisplayName("odbija otvaranje nakon isteka dostupnosti")
        void rejectsAfterAvailableUntil() {
            exam.setAvailableUntil(LocalDateTime.now().minusHours(1));

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));

            assertThatThrownBy(() -> examService.startAttempt(examId, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Rok");
        }

        @Test
        @DisplayName("baca iznimku kad ispit ne postoji")
        void missingExam() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.startAttempt(examId, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("baca iznimku kad student ne postoji")
        void missingStudent() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));
            when(userRepository.findById(studentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.startAttempt(examId, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        private void stubNewAttempt() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Spremanje odgovora")
    class SavingAnswer {

        @Test
        @DisplayName("sprema sadržaj odgovora i oznaku dovršenosti")
        void savesAnswerContent() {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, choiceTask);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));
            when(answerRepository.findByAttemptIdAndTaskId(open.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));

            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setTaskId(choiceTask.getId());
            request.setAnswerContent("Ponavlja blok koda");
            request.setMarkedDone(true);

            examService.saveAnswer(examId, request, studentId);

            assertThat(answer.getAnswerContent()).isEqualTo("Ponavlja blok koda");
            assertThat(answer.isMarkedDone()).isTrue();
            assertThat(answer.getAnsweredAt()).isNotNull();
        }

        @Test
        @DisplayName("odbija izmjenu nakon predaje")
        void rejectsEditAfterSubmit() {
            ExamAttempt submitted = attempt(ExamAttemptStatus.SUBMITTED, null);
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(submitted));

            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setTaskId(codeTask.getId());
            request.setAnswerContent("nešto");

            assertThatThrownBy(() -> examService.saveAnswer(examId, request, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("predan");
        }

        @Test
        @DisplayName("odbija zadatak koji nije dio ispita")
        void rejectsForeignTask() {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            UUID strangerId = UUID.randomUUID();

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));
            when(answerRepository.findByAttemptIdAndTaskId(open.getId(), strangerId))
                    .thenReturn(Optional.empty());

            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setTaskId(strangerId);
            request.setAnswerContent("x");

            assertThatThrownBy(() -> examService.saveAnswer(examId, request, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("nije dio");
        }

        @Test
        @DisplayName("baca iznimku kad pokušaj ne postoji")
        void rejectsWithoutAttempt() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());

            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setTaskId(codeTask.getId());

            assertThatThrownBy(() -> examService.saveAnswer(examId, request, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("zatvara pokušaj kad je rok istekao")
        void closesExpiredAttempt() {
            ExamAttempt expired = attempt(
                    ExamAttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5));

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(expired));

            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setTaskId(codeTask.getId());
            request.setAnswerContent("x");

            assertThatThrownBy(() -> examService.saveAnswer(examId, request, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("isteklo");

            assertThat(expired.getStatus()).isEqualTo(ExamAttemptStatus.EXPIRED);
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Izvršavanje koda tijekom ispita")
    class RunningCode {

        @Test
        @DisplayName("pokreće testove za programski zadatak")
        void runsTestsForCodeTask() {
            ExamAnswer answer = prepareCodeAnswer();
            Submission submission = new Submission();
            submission.setId(UUID.randomUUID());

            SubmissionResponse result = mock(SubmissionResponse.class);
            when(result.getId()).thenReturn(submission.getId());
            when(submissionService.submit(any(), eq(studentId))).thenReturn(result);
            when(submissionRepository.findById(submission.getId()))
                    .thenReturn(Optional.of(submission));

            examService.saveAnswer(examId, codeRequest(true, "int main() { return 0; }"), studentId);

            verify(submissionService).submit(any(), eq(studentId));
            assertThat(answer.getSubmission()).isEqualTo(submission);
        }

        @Test
        @DisplayName("pad izvršavanja ne ruši spremanje odgovora")
        void swallowsExecutionFailure() {
            ExamAnswer answer = prepareCodeAnswer();
            when(submissionService.submit(any(), eq(studentId)))
                    .thenThrow(new RuntimeException("Code Executor nedostupan"));

            examService.saveAnswer(examId, codeRequest(true, "int main() {}"), studentId);

            assertThat(answer.getAnswerContent()).isEqualTo("int main() {}");
            verify(answerRepository).save(answer);
        }

        @Test
        @DisplayName("ne pokreće testove kad student to nije tražio")
        void skipsWhenNotRequested() {
            prepareCodeAnswer();

            examService.saveAnswer(examId, codeRequest(false, "int main() {}"), studentId);

            verify(submissionService, never()).submit(any(), any());
        }

        @Test
        @DisplayName("ne pokreće testove kad ispit ne prikazuje rezultate")
        void skipsWhenResultsHidden() {
            exam.setShowTestResults(false);
            prepareCodeAnswer();

            examService.saveAnswer(examId, codeRequest(true, "int main() {}"), studentId);

            verify(submissionService, never()).submit(any(), any());
        }

        @Test
        @DisplayName("ne pokreće testove za prazan odgovor")
        void skipsBlankAnswer() {
            prepareCodeAnswer();

            examService.saveAnswer(examId, codeRequest(true, "   "), studentId);

            verify(submissionService, never()).submit(any(), any());
        }

        @Test
        @DisplayName("ne pokreće testove za zadatak koji nije programski")
        void skipsNonCodeTask() {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, choiceTask);
            open.getAnswers().add(answer);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));
            when(answerRepository.findByAttemptIdAndTaskId(open.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));

            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setTaskId(choiceTask.getId());
            request.setAnswerContent("Ponavlja blok koda");
            request.setRunTests(true);

            examService.saveAnswer(examId, request, studentId);

            verify(submissionService, never()).submit(any(), any());
        }

        private ExamAnswer prepareCodeAnswer() {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, codeTask);
            open.getAnswers().add(answer);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));
            when(answerRepository.findByAttemptIdAndTaskId(open.getId(), codeTask.getId()))
                    .thenReturn(Optional.of(answer));
            return answer;
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Predaja ispita")
    class Submitting {

        @Test
        @DisplayName("ocjenjuje točan odgovor punim brojem bodova")
        void gradesCorrectChoice() {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, choiceTask);
            answer.setAnswerContent("Ponavlja blok koda");
            open.getAnswers().add(answer);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));

            examService.submitAttempt(examId, studentId);

            assertThat(answer.getAiScore()).isEqualTo(10);
            assertThat(open.getSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("ocjenjuje netočan odgovor nulom")
        void gradesWrongChoice() {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, choiceTask);
            answer.setAnswerContent("Nešto posve drugo");
            open.getAnswers().add(answer);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));

            examService.submitAttempt(examId, studentId);

            assertThat(answer.getAiScore()).isZero();
        }

        @Test
        @DisplayName("prazan odgovor na pitanje s ponuđenim odgovorima nosi nulu")
        void gradesMissingChoiceAsZero() {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, choiceTask);
            open.getAnswers().add(answer);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));

            examService.submitAttempt(examId, studentId);

            assertThat(answer.getAiScore()).isZero();
        }

        @Test
        @DisplayName("ne ocjenjuje tekstualni odgovor automatski")
        void leavesTextUngraded() {
            Task textTask = new Task();
            textTask.setId(UUID.randomUUID());
            textTask.setTitle("Objasni petlju");
            textTask.setTaskType(TaskType.TEXT);
            textTask.setMaxScore(15);
            textTask.setOutcome(codeTask.getOutcome());

            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, textTask);
            answer.setAnswerContent("Petlja ponavlja naredbe.");
            open.getAnswers().add(answer);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));

            examService.submitAttempt(examId, studentId);

            assertThat(answer.getAiScore()).isNull();
        }

        @Test
        @DisplayName("boduje programski zadatak prema udjelu prošlih testova")
        void scoresCodeByTestRatio() {
            Submission submission = new Submission();
            submission.setTestsPassed(3);
            submission.setTestsTotal(4);

            assertThat(submitWithCodeSubmission(submission).getAiScore()).isEqualTo(15);
        }

        @Test
        @DisplayName("koristi AI ocjenu kad testova nema")
        void fallsBackToAiScore() {
            Submission submission = new Submission();
            submission.setTestsTotal(0);
            submission.setAiScore(12);

            assertThat(submitWithCodeSubmission(submission).getAiScore()).isEqualTo(12);
        }

        @Test
        @DisplayName("bez predaje koda zadatak ostaje neocijenjen")
        void leavesUngradedWithoutSubmission() {
            assertThat(submitWithCodeSubmission(null).getAiScore()).isNull();
        }

        @Test
        @DisplayName("bez testova i bez AI ocjene zadatak ostaje neocijenjen")
        void leavesUngradedWithoutAnyScore() {
            Submission submission = new Submission();
            submission.setTestsTotal(0);

            assertThat(submitWithCodeSubmission(submission).getAiScore()).isNull();
        }

        @Test
        @DisplayName("odbija dvostruku predaju")
        void rejectsDoubleSubmit() {
            ExamAttempt submitted = attempt(ExamAttemptStatus.SUBMITTED, null);
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(submitted));

            assertThatThrownBy(() -> examService.submitAttempt(examId, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("već predan");
        }

        @Test
        @DisplayName("baca iznimku ako pokušaj ne postoji")
        void throwsWhenNoAttempt() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.submitAttempt(examId, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        private ExamAnswer submitWithCodeSubmission(Submission submission) {
            ExamAttempt open = attempt(ExamAttemptStatus.IN_PROGRESS, null);
            ExamAnswer answer = answerFor(open, codeTask);
            answer.setSubmission(submission);
            open.getAnswers().add(answer);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(open));

            examService.submitAttempt(examId, studentId);
            return answer;
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Ručno ocjenjivanje")
    class ManualGrading {

        @Test
        @DisplayName("upisuje bodove i komentar nastavnika")
        void savesScoreAndFeedback() {
            ExamAttempt a = attempt(ExamAttemptStatus.SUBMITTED, null);
            ExamAnswer answer = answerFor(a, choiceTask);
            a.getAnswers().add(answer);

            when(answerRepository.findByAttemptIdAndTaskId(a.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));
            when(attemptRepository.findByIdWithAnswers(a.getId())).thenReturn(Optional.of(a));

            examService.gradeAnswer(a.getId(), choiceTask.getId(), 8, "Dobro", teacherId);

            assertThat(answer.getTeacherScore()).isEqualTo(8);
            assertThat(answer.getFinalScore()).isEqualTo(8);
            assertThat(answer.getTeacherFeedback()).isEqualTo("Dobro");
        }

        @Test
        @DisplayName("granični broj bodova je dopušten")
        void acceptsBoundaryScore() {
            ExamAttempt a = attempt(ExamAttemptStatus.SUBMITTED, null);
            ExamAnswer answer = answerFor(a, choiceTask);
            a.getAnswers().add(answer);

            when(answerRepository.findByAttemptIdAndTaskId(a.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));
            when(attemptRepository.findByIdWithAnswers(a.getId())).thenReturn(Optional.of(a));

            examService.gradeAnswer(a.getId(), choiceTask.getId(), 10, "Maksimum", teacherId);

            assertThat(answer.getTeacherScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("prazni bodovi poništavaju ocjenu")
        void acceptsNullScore() {
            ExamAttempt a = attempt(ExamAttemptStatus.SUBMITTED, null);
            ExamAnswer answer = answerFor(a, choiceTask);
            a.getAnswers().add(answer);

            when(answerRepository.findByAttemptIdAndTaskId(a.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));
            when(attemptRepository.findByIdWithAnswers(a.getId())).thenReturn(Optional.of(a));

            examService.gradeAnswer(a.getId(), choiceTask.getId(), null, "Bez ocjene", teacherId);

            assertThat(answer.getTeacherScore()).isNull();
        }

        @Test
        @DisplayName("zbraja ukupan rezultat pokušaja")
        void recalculatesTotal() {
            ExamAttempt a = attempt(ExamAttemptStatus.SUBMITTED, null);
            ExamAnswer first = answerFor(a, choiceTask);
            first.setTeacherScore(6);
            first.setFinalScore(6);
            ExamAnswer second = answerFor(a, codeTask);
            second.setAiScore(14);
            a.getAnswers().add(first);
            a.getAnswers().add(second);

            when(answerRepository.findByAttemptIdAndTaskId(a.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(first));
            when(attemptRepository.findByIdWithAnswers(a.getId())).thenReturn(Optional.of(a));

            examService.gradeAnswer(a.getId(), choiceTask.getId(), 6, "ok", teacherId);

            assertThat(a.getTotalScore()).isEqualTo(20);
        }

        @Test
        @DisplayName("odbija bodove iznad maksimuma")
        void rejectsScoreAboveMax() {
            ExamAttempt open = attempt(ExamAttemptStatus.SUBMITTED, null);
            ExamAnswer answer = answerFor(open, choiceTask);

            when(answerRepository.findByAttemptIdAndTaskId(open.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));

            assertThatThrownBy(() -> examService.gradeAnswer(
                    open.getId(), choiceTask.getId(), 999, "previše", teacherId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("između");
        }

        @Test
        @DisplayName("odbija negativne bodove")
        void rejectsNegativeScore() {
            ExamAttempt open = attempt(ExamAttemptStatus.SUBMITTED, null);
            ExamAnswer answer = answerFor(open, choiceTask);

            when(answerRepository.findByAttemptIdAndTaskId(open.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));

            assertThatThrownBy(() -> examService.gradeAnswer(
                    open.getId(), choiceTask.getId(), -5, "greška", teacherId))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("baca iznimku kad odgovor ne postoji")
        void missingAnswer() {
            UUID attemptId = UUID.randomUUID();
            when(answerRepository.findByAttemptIdAndTaskId(attemptId, choiceTask.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.gradeAnswer(
                    attemptId, choiceTask.getId(), 5, "x", teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Zaključivanje pregleda")
    class FinalizingReview {

        @Test
        @DisplayName("bilježi nastavnika, vrijeme i status")
        void marksReviewed() {
            ExamAttempt a = attempt(ExamAttemptStatus.GRADED, null);
            when(attemptRepository.findById(a.getId())).thenReturn(Optional.of(a));
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
            when(attemptRepository.findByIdWithAnswers(a.getId())).thenReturn(Optional.of(a));

            examService.finalizeReview(a.getId(), "Lijepo riješeno", teacherId);

            assertThat(a.getStatus()).isEqualTo(ExamAttemptStatus.REVIEWED);
            assertThat(a.getTeacherFeedback()).isEqualTo("Lijepo riješeno");
            assertThat(a.getReviewedAt()).isNotNull();
            assertThat(a.getReviewedBy()).isEqualTo(teacher);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći pokušaj")
        void missingAttempt() {
            UUID id = UUID.randomUUID();
            when(attemptRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.finalizeReview(id, "x", teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg nastavnika")
        void missingTeacher() {
            ExamAttempt a = attempt(ExamAttemptStatus.GRADED, null);
            when(attemptRepository.findById(a.getId())).thenReturn(Optional.of(a));
            when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> examService.finalizeReview(a.getId(), "x", teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================================================================
    // Pomocne metode
    // ==================================================================

    private ExamRequest examRequest(List<UUID> taskIds) {
        ExamRequest r = new ExamRequest();
        r.setTitle("Kolokvij 1");
        r.setDescription("Prvi kolokvij");
        r.setInstructions("Rješavajte samostalno.");
        r.setCourseId(courseId);
        r.setPassingScore(50);
        r.setTaskIds(taskIds);
        return r;
    }

    private ExamAnswerRequest codeRequest(boolean runTests, String content) {
        ExamAnswerRequest r = new ExamAnswerRequest();
        r.setTaskId(codeTask.getId());
        r.setAnswerContent(content);
        r.setRunTests(runTests);
        return r;
    }

    private ExamAttempt attempt(ExamAttemptStatus status, LocalDateTime deadline) {
        ExamAttempt a = ExamAttempt.builder()
                .exam(exam)
                .student(student)
                .status(status)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .deadlineAt(deadline)
                .answers(new ArrayList<>())
                .build();
        a.setId(UUID.randomUUID());
        return a;
    }

    private ExamAnswer answerFor(ExamAttempt attempt, Task task) {
        ExamAnswer answer = ExamAnswer.builder()
                .attempt(attempt)
                .task(task)
                .markedDone(false)
                .build();
        answer.setId(UUID.randomUUID());
        return answer;
    }
}