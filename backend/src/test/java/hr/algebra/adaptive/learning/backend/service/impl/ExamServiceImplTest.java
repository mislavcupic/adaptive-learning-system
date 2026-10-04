package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.request.ExamAnswerRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.request.ExamRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamResponse;
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
    private UUID courseId;
    private User student;
    private Course course;
    private Exam exam;
    private Task codeTask;
    private Task choiceTask;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        course = new Course();
        course.setId(courseId);
        course.setName("Uvod u programiranje");

        student = new User();
        student.setId(studentId);
        student.setEmail("student@test.hr");
        student.setFirstName("Ana");
        student.setLastName("Anić");

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
                .tasks(new ArrayList<>(List.of(codeTask, choiceTask)))
                .attempts(new ArrayList<>())
                .passingScore(50)
                .isPublished(true)
                .isActive(true)
                .showTestResults(true)
                .build();
        exam.setId(examId);
    }

    // ==================================================================

    @Nested
    @DisplayName("Objava ispita")
    class Publishing {

        @Test
        @DisplayName("objavljuje ispit koji ima zadatke")
        void publishesExamWithTasks() {
            when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
            when(examRepository.save(any(Exam.class))).thenAnswer(i -> i.getArgument(0));

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
            when(examRepository.save(any(Exam.class))).thenAnswer(i -> i.getArgument(0));

            ExamRequest request = new ExamRequest();
            request.setTitle("Kolokvij 1 - ispravak");
            request.setCourseId(courseId);
            request.setPassingScore(60);

            ExamResponse result = examService.update(examId, request);

            assertThat(result.getTitle()).isEqualTo("Kolokvij 1 - ispravak");
            assertThat(result.getPassingScore()).isEqualTo(60);
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
            when(examRepository.save(any(Exam.class))).thenAnswer(i -> i.getArgument(0));

            examService.delete(examId);

            assertThat(exam.isActive()).isFalse();
            assertThat(exam.isPublished()).isFalse();
            verify(examRepository, never()).delete(any());
        }
    }

    // ==================================================================

    @Nested
    @DisplayName("Otvaranje pokušaja")
    class StartingAttempt {

        @Test
        @DisplayName("stvara pokušaj i prazne odgovore za svaki zadatak")
        void createsAttemptWithEmptyAnswers() {
            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> {
                ExamAttempt a = i.getArgument(0);
                if (a.getId() == null) a.setId(UUID.randomUUID());
                return a;
            });
            when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));

            ExamAttemptResponse result = examService.startAttempt(examId, studentId);

            assertThat(result.getStatus()).isEqualTo(ExamAttemptStatus.IN_PROGRESS);
            verify(answerRepository, times(2)).save(any(ExamAnswer.class));
        }

        @Test
        @DisplayName("postavlja rok kad ispit ima vremensko ograničenje")
        void setsDeadlineWhenTimeLimited() {
            exam.setTimeLimitMinutes(90);

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.empty());
            when(examRepository.findByIdWithTasks(examId)).thenReturn(Optional.of(exam));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> {
                ExamAttempt a = i.getArgument(0);
                if (a.getId() == null) a.setId(UUID.randomUUID());
                return a;
            });
            when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));

            ExamAttemptResponse result = examService.startAttempt(examId, studentId);

            assertThat(result.getDeadlineAt()).isNotNull();
            assertThat(result.getRemainingSeconds()).isGreaterThan(5000);
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
            when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));

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
        @DisplayName("zatvara pokušaj kad je rok istekao")
        void closesExpiredAttempt() {
            ExamAttempt expired = attempt(
                    ExamAttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5));

            when(attemptRepository.findByStudentAndExamWithAnswers(studentId, examId))
                    .thenReturn(Optional.of(expired));
            when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> i.getArgument(0));

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
            when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));
            when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> i.getArgument(0));

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
            when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));
            when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> i.getArgument(0));

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
            when(answerRepository.save(any(ExamAnswer.class))).thenAnswer(i -> i.getArgument(0));
            when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> i.getArgument(0));

            examService.submitAttempt(examId, studentId);

            assertThat(answer.getAiScore()).isNull();
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
    }

    // ==================================================================

    @Nested
    @DisplayName("Ručno ocjenjivanje")
    class ManualGrading {

        @Test
        @DisplayName("odbija bodove iznad maksimuma")
        void rejectsScoreAboveMax() {
            ExamAttempt open = attempt(ExamAttemptStatus.SUBMITTED, null);
            ExamAnswer answer = answerFor(open, choiceTask);

            when(answerRepository.findByAttemptIdAndTaskId(open.getId(), choiceTask.getId()))
                    .thenReturn(Optional.of(answer));

            assertThatThrownBy(() -> examService.gradeAnswer(
                    open.getId(), choiceTask.getId(), 999, "previše", UUID.randomUUID()))
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
                    open.getId(), choiceTask.getId(), -5, "greška", UUID.randomUUID()))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ==================================================================
    // Pomocne metode
    // ==================================================================

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