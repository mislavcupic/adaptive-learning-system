package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackResponse;
import hr.algebra.adaptive.learning.backend.dto.request.SubmissionRequest;
import hr.algebra.adaptive.learning.backend.dto.response.SubmissionResponse;
import hr.algebra.adaptive.learning.backend.exception.PretestRequiredException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.SubmissionRepository;
import hr.algebra.adaptive.learning.backend.repository.TaskRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.AssessmentService;
import hr.algebra.adaptive.learning.backend.service.CodeExecutorClient;
import hr.algebra.adaptive.learning.backend.service.MLServiceClient;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubmissionServiceImpl")
class SubmissionServiceImplTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private CodeExecutorClient codeExecutorClient;
    @Mock private MLServiceClient mlServiceClient;
    @Mock private AssessmentService assessmentService;
    @Mock private SubmissionPostProcessor postProcessor;

    @InjectMocks private SubmissionServiceImpl submissionService;

    private UUID studentId;
    private UUID courseId;
    private User student;
    private Course course;
    private LearningOutcome outcome;
    private Task codeTask;
    private Task choiceTask;
    private Task textTask;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        course = new Course();
        course.setId(courseId);
        course.setName("Uvod u programiranje");
        course.setLanguageType(LanguageType.C);

        outcome = new LearningOutcome();
        outcome.setId(UUID.randomUUID());
        outcome.setName("Petlje");
        outcome.setCourse(course);

        student = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .researchGroup(ResearchGroup.EXPERIMENTAL)
                .build();
        student.setId(studentId);

        codeTask = task(TaskType.CODE, 20);
        codeTask.setTitle("Zbroj niza");

        choiceTask = task(TaskType.MULTIPLE_CHOICE, 10);
        choiceTask.setTitle("Što radi for petlja?");
        choiceTask.setCorrectAnswer("Ponavlja blok koda");

        textTask = task(TaskType.TEXT, 15);
        textTask.setTitle("Objasni petlju");

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(assessmentService.hasCompletedPretest(studentId, courseId)).thenReturn(true);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(i -> {
            Submission s = i.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
    }

    @Nested
    @DisplayName("Provjera pretesta")
    class PretestGate {

        @Test
        @DisplayName("odbija predaju bez riješenog pretesta")
        void rejectsWithoutPretest() {
            when(taskRepository.findById(codeTask.getId())).thenReturn(Optional.of(codeTask));
            when(assessmentService.hasCompletedPretest(studentId, courseId)).thenReturn(false);

            assertThatThrownBy(() -> submissionService.submit(request(codeTask, "kod"), studentId))
                    .isInstanceOf(PretestRequiredException.class);

            verify(submissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("propušta predaju kad je pretest riješen")
        void allowsWithPretest() {
            when(taskRepository.findById(choiceTask.getId())).thenReturn(Optional.of(choiceTask));

            submissionService.submit(request(choiceTask, "Ponavlja blok koda"), studentId);

            verify(submissionRepository, atLeastOnce()).save(any(Submission.class));
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći zadatak")
        void throwsForMissingTask() {
            UUID unknown = UUID.randomUUID();
            when(taskRepository.findById(unknown)).thenReturn(Optional.empty());

            SubmissionRequest req = new SubmissionRequest();
            req.setTaskId(unknown);
            req.setCode("kod");

            assertThatThrownBy(() -> submissionService.submit(req, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Zadaci s višestrukim izborom")
    class MultipleChoice {

        @Test
        @DisplayName("ne pokreće prevoditelj")
        void skipsCompiler() {
            when(taskRepository.findById(choiceTask.getId())).thenReturn(Optional.of(choiceTask));

            submissionService.submit(request(choiceTask, "Ponavlja blok koda"), studentId);

            verify(codeExecutorClient, never()).execute(any());
        }

        @Test
        @DisplayName("dodjeljuje pune bodove za točan odgovor")
        void fullScoreForCorrect() {
            when(taskRepository.findById(choiceTask.getId())).thenReturn(Optional.of(choiceTask));

            SubmissionResponse result =
                    submissionService.submit(request(choiceTask, "Ponavlja blok koda"), studentId);

            assertThat(result.getAiScore()).isEqualTo(10);
            assertThat(result.getFinalScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("dodjeljuje nula bodova za netočan odgovor")
        void zeroScoreForWrong() {
            when(taskRepository.findById(choiceTask.getId())).thenReturn(Optional.of(choiceTask));

            SubmissionResponse result =
                    submissionService.submit(request(choiceTask, "Nešto drugo"), studentId);

            assertThat(result.getAiScore()).isZero();
        }

        @Test
        @DisplayName("zanemaruje razliku u velikim i malim slovima")
        void ignoresCase() {
            when(taskRepository.findById(choiceTask.getId())).thenReturn(Optional.of(choiceTask));

            SubmissionResponse result =
                    submissionService.submit(request(choiceTask, "PONAVLJA BLOK KODA"), studentId);

            assertThat(result.getAiScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("status je COMPLETED bez izlaza prevoditelja")
        void completedWithoutCompilerOutput() {
            when(taskRepository.findById(choiceTask.getId())).thenReturn(Optional.of(choiceTask));

            SubmissionResponse result =
                    submissionService.submit(request(choiceTask, "Ponavlja blok koda"), studentId);

            assertThat(result.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
            assertThat(result.getCompilerOutput()).isNull();
        }
    }

    @Nested
    @DisplayName("Tekstualni zadaci")
    class TextTasks {

        @Test
        @DisplayName("ne pokreće prevoditelj ni ocjenjivanje")
        void leavesForTeacher() {
            when(taskRepository.findById(textTask.getId())).thenReturn(Optional.of(textTask));

            SubmissionResponse result =
                    submissionService.submit(request(textTask, "Petlja ponavlja naredbe."), studentId);

            verify(codeExecutorClient, never()).execute(any());
            assertThat(result.getAiScore()).isNull();
            assertThat(result.getFinalScore()).isNull();
        }

        @Test
        @DisplayName("obavještava studenta da slijedi ručni pregled")
        void informsAboutManualReview() {
            when(taskRepository.findById(textTask.getId())).thenReturn(Optional.of(textTask));

            SubmissionResponse result =
                    submissionService.submit(request(textTask, "odgovor"), studentId);

            assertThat(result.getAiFeedback()).contains("Nastavnik");
        }
    }

    @Nested
    @DisplayName("Programski zadaci")
    class CodeTasks {

        @Test
        @DisplayName("pokreće prevoditelj i sprema rezultate testova")
        void runsCompilerAndStoresResults() {
            when(taskRepository.findById(codeTask.getId())).thenReturn(Optional.of(codeTask));
            when(codeExecutorClient.execute(any())).thenReturn(successfulExecution());
            when(mlServiceClient.generateFeedback(any())).thenReturn(feedback(18));

            SubmissionResponse result =
                    submissionService.submit(request(codeTask, "int main() { return 0; }"), studentId);

            verify(codeExecutorClient).execute(any());
            assertThat(result.getTestsPassed()).isEqualTo(5);
            assertThat(result.getTestsTotal()).isEqualTo(5);
            assertThat(result.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        }

        @Test
        @DisplayName("preuzima ocjenu i povratnu informaciju iz ML servisa")
        void appliesMlFeedback() {
            when(taskRepository.findById(codeTask.getId())).thenReturn(Optional.of(codeTask));
            when(codeExecutorClient.execute(any())).thenReturn(successfulExecution());
            when(mlServiceClient.generateFeedback(any())).thenReturn(feedback(18));

            SubmissionResponse result =
                    submissionService.submit(request(codeTask, "kod"), studentId);

            assertThat(result.getAiScore()).isEqualTo(18);
            assertThat(result.getFinalScore()).isEqualTo(18);
            assertThat(result.getAiFeedback()).isEqualTo("Dobro rješenje.");
        }

        @Test
        @DisplayName("zaustavlja se na grešci prevođenja")
        void stopsOnCompileError() {
            when(taskRepository.findById(codeTask.getId())).thenReturn(Optional.of(codeTask));
            when(codeExecutorClient.execute(any())).thenReturn(failedExecution());

            SubmissionResponse result =
                    submissionService.submit(request(codeTask, "neispravan kod"), studentId);

            assertThat(result.getStatus()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
            verify(mlServiceClient, never()).generateFeedback(any());
        }

        @Test
        @DisplayName("predaja prolazi i kad ML servis zakaže")
        void survivesMlFailure() {
            when(taskRepository.findById(codeTask.getId())).thenReturn(Optional.of(codeTask));
            when(codeExecutorClient.execute(any())).thenReturn(successfulExecution());
            when(mlServiceClient.generateFeedback(any()))
                    .thenThrow(new RuntimeException("ML servis nedostupan"));

            SubmissionResponse result =
                    submissionService.submit(request(codeTask, "kod"), studentId);

            assertThat(result.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
            assertThat(result.getAiScore()).isNull();
        }
    }

    @Nested
    @DisplayName("Povratna informacija nastavnika")
    class TeacherFeedback {

        @Test
        @DisplayName("sprema ocjenu i mijenja status u REVIEWED")
        void savesGradeAndStatus() {
            Submission submission = Submission.builder()
                    .student(student)
                    .task(textTask)
                    .submittedCode("odgovor")
                    .status(SubmissionStatus.COMPLETED)
                    .build();
            submission.setId(UUID.randomUUID());

            when(submissionRepository.findById(submission.getId()))
                    .thenReturn(Optional.of(submission));

            SubmissionResponse result = submissionService.addTeacherFeedback(
                    submission.getId(), "Dobro objašnjeno.", 13);

            assertThat(result.getStatus()).isEqualTo(SubmissionStatus.REVIEWED);
            assertThat(result.getTeacherScore()).isEqualTo(13);
            assertThat(result.getFinalScore()).isEqualTo(13);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeću predaju")
        void throwsForMissingSubmission() {
            UUID unknown = UUID.randomUUID();
            when(submissionRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    submissionService.addTeacherFeedback(unknown, "komentar", 5))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Dohvat predaja")
    class Fetching {

        @Test
        @DisplayName("vraća predaje studenta")
        void returnsStudentSubmissions() {
            Submission s = Submission.builder()
                    .student(student)
                    .task(codeTask)
                    .submittedCode("kod")
                    .status(SubmissionStatus.COMPLETED)
                    .build();
            s.setId(UUID.randomUUID());

            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of(s));

            List<SubmissionResponse> result = submissionService.getByStudent(studentId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudentName()).isEqualTo("Ana Anić");
        }

        @Test
        @DisplayName("vraća prazan popis kad student nema predaja")
        void returnsEmptyList() {
            when(submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                    .thenReturn(List.of());

            assertThat(submissionService.getByStudent(studentId)).isEmpty();
        }

        @Test
        @DisplayName("broji predaje po zadatku")
        void countsByTask() {
            when(submissionRepository.countByTaskId(codeTask.getId())).thenReturn(7L);

            assertThat(submissionService.countByTask(codeTask.getId())).isEqualTo(7);
        }
    }

    private Task task(TaskType type, int maxScore) {
        Task t = new Task();
        t.setId(UUID.randomUUID());
        t.setTaskType(type);
        t.setMaxScore(maxScore);
        t.setOutcome(outcome);
        return t;
    }

    private SubmissionRequest request(Task task, String code) {
        SubmissionRequest r = new SubmissionRequest();
        r.setTaskId(task.getId());
        r.setCode(code);
        return r;
    }

    private CodeExecutionResponse successfulExecution() {
        CodeExecutionResponse r = new CodeExecutionResponse();
        r.setSuccess(true);
        r.setCompilerOutput("");
        r.setExecutionOutput("15");
        r.setTestsPassed(5);
        r.setTestsTotal(5);
        return r;
    }

    private CodeExecutionResponse failedExecution() {
        CodeExecutionResponse r = new CodeExecutionResponse();
        r.setSuccess(false);
        r.setCompilerOutput("error: expected ';'");
        r.setError("Greška prilikom kompilacije");
        r.setTestsPassed(0);
        r.setTestsTotal(0);
        return r;
    }

    private MLFeedbackResponse feedback(int score) {
        return MLFeedbackResponse.builder()
                .aiFeedback("Dobro rješenje.")
                .aiScore(score)
                .build();
    }
}