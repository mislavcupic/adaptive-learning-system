package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.ml.MlBktRequest;
import hr.algebra.adaptive.learning.backend.repository.SubmissionRepository;
import hr.algebra.adaptive.learning.backend.service.MLServiceClient;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubmissionPostProcessor")
class SubmissionPostProcessorTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private MLServiceClient mlServiceClient;

    @InjectMocks private SubmissionPostProcessor postProcessor;

    private UUID studentId;
    private User student;
    private LearningOutcome outcome;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setName("Uvod u programiranje");

        outcome = new LearningOutcome();
        outcome.setId(UUID.randomUUID());
        outcome.setName("Razumijevanje petlji");
        outcome.setCourse(course);

        student = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .build();
        student.setId(studentId);
    }

    @Nested
    @DisplayName("Programski zadaci")
    class CodeSubmissions {

        @Test
        @DisplayName("prijavljuje uspjeh kad svi testovi prolaze")
        void correctWhenAllTestsPass() {
            Submission s = submission(TaskType.CODE, 20);
            s.setTestsPassed(5);
            s.setTestsTotal(5);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().isCorrect()).isTrue();
        }

        @Test
        @DisplayName("prijavljuje neuspjeh kad dio testova pada")
        void incorrectWhenSomeTestsFail() {
            Submission s = submission(TaskType.CODE, 20);
            s.setTestsPassed(3);
            s.setTestsTotal(5);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().isCorrect()).isFalse();
        }

        @Test
        @DisplayName("koristi AI ocjenu kad zadatak nema testova")
        void fallsBackToAiScore() {
            Submission s = submission(TaskType.CODE, 20);
            s.setTestsTotal(0);
            s.setAiScore(16);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().isCorrect()).isTrue();
        }

        @Test
        @DisplayName("AI ocjena ispod polovice znači neuspjeh")
        void aiScoreBelowHalfIsFailure() {
            Submission s = submission(TaskType.CODE, 20);
            s.setTestsTotal(0);
            s.setAiScore(8);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().isCorrect()).isFalse();
        }

        @Test
        @DisplayName("šalje naziv ishoda učenja kao vještinu")
        void sendsOutcomeNameAsSkill() {
            Submission s = submission(TaskType.CODE, 20);
            s.setTestsPassed(5);
            s.setTestsTotal(5);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().getSkillName()).isEqualTo("Razumijevanje petlji");
            assertThat(capturedRequest().getStudentId()).isEqualTo(studentId);
        }
    }

    @Nested
    @DisplayName("Zadaci koje ocjenjuje nastavnik")
    class TeacherGraded {

        @Test
        @DisplayName("preskače tekstualni zadatak bez ocjene nastavnika")
        void skipsUngradedText() {
            Submission s = submission(TaskType.TEXT, 15);
            stub(s);

            postProcessor.process(s.getId());

            verify(mlServiceClient, never()).updateBkt(any());
        }

        @Test
        @DisplayName("preskače ček-listu bez ocjene nastavnika")
        void skipsUngradedChecklist() {
            Submission s = submission(TaskType.CHECKLIST, 10);
            stub(s);

            postProcessor.process(s.getId());

            verify(mlServiceClient, never()).updateBkt(any());
        }

        @Test
        @DisplayName("uzima u obzir tekstualni zadatak nakon ocjene nastavnika")
        void usesTeacherScoreWhenPresent() {
            Submission s = submission(TaskType.TEXT, 15);
            s.setTeacherScore(12);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().isCorrect()).isTrue();
        }

        @Test
        @DisplayName("niska ocjena nastavnika znači neuspjeh")
        void lowTeacherScoreIsFailure() {
            Submission s = submission(TaskType.TEXT, 15);
            s.setTeacherScore(4);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().isCorrect()).isFalse();
        }

        @Test
        @DisplayName("ocjena nastavnika ima prednost pred rezultatom testova")
        void teacherScoreOverridesTests() {
            Submission s = submission(TaskType.CODE, 20);
            s.setTestsPassed(1);
            s.setTestsTotal(5);
            s.setTeacherScore(18);
            stub(s);

            postProcessor.process(s.getId());

            assertThat(capturedRequest().isCorrect()).isTrue();
        }
    }

    @Nested
    @DisplayName("Otpornost na pogreške")
    class Resilience {

        @Test
        @DisplayName("ne radi ništa kad predaja ne postoji")
        void doesNothingForMissingSubmission() {
            UUID unknown = UUID.randomUUID();
            when(submissionRepository.findById(unknown)).thenReturn(Optional.empty());

            postProcessor.process(unknown);

            verify(mlServiceClient, never()).updateBkt(any());
        }

        @Test
        @DisplayName("preskače zadatak bez ishoda učenja")
        void skipsTaskWithoutOutcome() {
            Submission s = submission(TaskType.CODE, 20);
            s.getTask().setOutcome(null);
            stub(s);

            postProcessor.process(s.getId());

            verify(mlServiceClient, never()).updateBkt(any());
        }

        @Test
        @DisplayName("ne baca iznimku kad ML servis zakaže")
        void survivesMlFailure() {
            Submission s = submission(TaskType.CODE, 20);
            s.setTestsPassed(5);
            s.setTestsTotal(5);
            stub(s);

            when(mlServiceClient.updateBkt(any()))
                    .thenThrow(new RuntimeException("ML servis nedostupan"));

            postProcessor.process(s.getId());

            verify(mlServiceClient).updateBkt(any());
        }
    }

    private MlBktRequest capturedRequest() {
        ArgumentCaptor<MlBktRequest> captor = ArgumentCaptor.forClass(MlBktRequest.class);
        verify(mlServiceClient).updateBkt(captor.capture());
        return captor.getValue();
    }

    private void stub(Submission s) {
        when(submissionRepository.findById(s.getId())).thenReturn(Optional.of(s));
    }

    private Submission submission(TaskType type, int maxScore) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("Zadatak");
        task.setTaskType(type);
        task.setMaxScore(maxScore);
        task.setOutcome(outcome);

        Submission s = Submission.builder()
                .student(student)
                .task(task)
                .submittedCode("odgovor")
                .status(SubmissionStatus.COMPLETED)
                .build();
        s.setId(UUID.randomUUID());
        return s;
    }
}

