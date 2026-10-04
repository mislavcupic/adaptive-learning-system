package hr.algebra.adaptive.learning.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.QuestionType;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentAttemptRequest;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentAttemptResponse;
import hr.algebra.adaptive.learning.backend.exception.AssessmentAlreadyCompletedException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.*;
import hr.algebra.adaptive.learning.backend.service.CodeExecutorClient;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssessmentServiceImpl")
class AssessmentServiceImplTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentQuestionRepository questionRepository;
    @Mock private AssessmentAttemptRepository attemptRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CodeExecutorClient codeExecutorClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AssessmentServiceImpl assessmentService;

    private UUID studentId;
    private UUID courseId;
    private UUID assessmentId;
    private User student;
    private Course course;
    private Assessment assessment;
    private AssessmentQuestion choiceQuestion;
    private AssessmentQuestion codeQuestion;

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentServiceImpl(
                assessmentRepository, questionRepository, attemptRepository,
                courseRepository, userRepository, codeExecutorClient, objectMapper);

        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        assessmentId = UUID.randomUUID();

        course = new Course();
        course.setId(courseId);
        course.setName("Uvod u programiranje");
        course.setLanguageType(LanguageType.C);

        student = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .build();
        student.setId(studentId);

        assessment = Assessment.builder()
                .title("Predtest")
                .course(course)
                .assessmentType(AssessmentType.PRETEST)
                .questions(new ArrayList<>())
                .build();
        assessment.setId(assessmentId);

        choiceQuestion = question(QuestionType.MULTIPLE_CHOICE, 10, "Petlja");
        codeQuestion = question(QuestionType.CODE, 20, null);

        assessment.getQuestions().add(choiceQuestion);
        assessment.getQuestions().add(codeQuestion);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(attemptRepository.save(any(AssessmentAttempt.class))).thenAnswer(i -> {
            AssessmentAttempt a = i.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
    }

    @Nested
    @DisplayName("Otvaranje pokušaja")
    class StartingAttempt {

        @Test
        @DisplayName("stvara pokušaj s maksimalnim brojem bodova")
        void createsAttemptWithMaxScore() {
            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, assessmentId)).thenReturn(false);
            when(attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId))
                    .thenReturn(Optional.empty());

            AssessmentAttemptResponse result =
                    assessmentService.startAttempt(assessmentId, studentId);

            assertThat(result.getMaxScore()).isEqualTo(30);
            assertThat(result.getScore()).isZero();
        }

        @Test
        @DisplayName("odbija ponovno rješavanje završenog testa")
        void rejectsRetakeOfCompleted() {
            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, assessmentId)).thenReturn(true);

            assertThatThrownBy(() -> assessmentService.startAttempt(assessmentId, studentId))
                    .isInstanceOf(AssessmentAlreadyCompletedException.class)
                    .hasMessageContaining("već riješili");

            verify(attemptRepository, never()).save(any());
        }

        @Test
        @DisplayName("vraća započeti pokušaj umjesto da stvori novi")
        void returnsExistingAttempt() {
            AssessmentAttempt existing = attempt(false, 0);

            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, assessmentId)).thenReturn(false);
            when(attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId))
                    .thenReturn(Optional.of(existing));

            assessmentService.startAttempt(assessmentId, studentId);

            verify(attemptRepository, never()).save(any());
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći test")
        void throwsForMissingAssessment() {
            UUID unknown = UUID.randomUUID();
            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, unknown)).thenReturn(false);
            when(attemptRepository.findByStudentIdAndAssessmentId(studentId, unknown))
                    .thenReturn(Optional.empty());
            when(assessmentRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assessmentService.startAttempt(unknown, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Predaja testa")
    class Submitting {

        @Test
        @DisplayName("dodjeljuje bodove za točan odgovor")
        void gradesCorrectChoice() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            Map<String, String> answers = new HashMap<>();
            answers.put(choiceQuestion.getId().toString(), "Petlja");

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(answers), studentId);

            assertThat(result.getScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("ne dodjeljuje bodove za netočan odgovor")
        void zeroForWrongChoice() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            Map<String, String> answers = new HashMap<>();
            answers.put(choiceQuestion.getId().toString(), "Grananje");

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(answers), studentId);

            assertThat(result.getScore()).isZero();
        }

        @Test
        @DisplayName("zanemaruje razliku u velikim i malim slovima")
        void ignoresCase() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            Map<String, String> answers = new HashMap<>();
            answers.put(choiceQuestion.getId().toString(), "  PETLJA  ");

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(answers), studentId);

            assertThat(result.getScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("preskače pitanja bez odgovora")
        void skipsUnanswered() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(new HashMap<>()), studentId);

            assertThat(result.getScore()).isZero();
            verify(codeExecutorClient, never()).execute(any());
        }

        @Test
        @DisplayName("označava pokušaj završenim i bilježi vrijeme")
        void marksCompleted() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            assessmentService.submitAttempt(requestWith(new HashMap<>()), studentId);

            assertThat(open.isCompleted()).isTrue();
            assertThat(open.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("odbija dvostruku predaju")
        void rejectsDoubleSubmit() {
            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, assessmentId)).thenReturn(true);

            assertThatThrownBy(() ->
                    assessmentService.submitAttempt(requestWith(new HashMap<>()), studentId))
                    .isInstanceOf(AssessmentAlreadyCompletedException.class);
        }

        @Test
        @DisplayName("baca iznimku ako pokušaj nije započet")
        void throwsWhenNotStarted() {
            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, assessmentId)).thenReturn(false);
            when(attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    assessmentService.submitAttempt(requestWith(new HashMap<>()), studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Ocjenjivanje programskog pitanja")
    class CodeGrading {

        @Test
        @DisplayName("dodjeljuje bodove razmjerno prolaznim testovima")
        void scoresProportionally() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            CodeExecutionResponse exec = new CodeExecutionResponse();
            exec.setSuccess(true);
            exec.setTestsPassed(3);
            exec.setTestsTotal(4);
            when(codeExecutorClient.execute(any())).thenReturn(exec);

            Map<String, String> answers = new HashMap<>();
            answers.put(codeQuestion.getId().toString(), "int main() {}");

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(answers), studentId);

            assertThat(result.getScore()).isEqualTo(15);
        }

        @Test
        @DisplayName("dodjeljuje sve bodove kad svi testovi prolaze")
        void fullScoreWhenAllPass() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            CodeExecutionResponse exec = new CodeExecutionResponse();
            exec.setSuccess(true);
            exec.setTestsPassed(4);
            exec.setTestsTotal(4);
            when(codeExecutorClient.execute(any())).thenReturn(exec);

            Map<String, String> answers = new HashMap<>();
            answers.put(codeQuestion.getId().toString(), "int main() {}");

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(answers), studentId);

            assertThat(result.getScore()).isEqualTo(20);
        }

        @Test
        @DisplayName("ne dodjeljuje bodove kad nema testova")
        void zeroWhenNoTests() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            CodeExecutionResponse exec = new CodeExecutionResponse();
            exec.setTestsPassed(0);
            exec.setTestsTotal(0);
            when(codeExecutorClient.execute(any())).thenReturn(exec);

            Map<String, String> answers = new HashMap<>();
            answers.put(codeQuestion.getId().toString(), "int main() {}");

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(answers), studentId);

            assertThat(result.getScore()).isZero();
        }

        @Test
        @DisplayName("ne ruši predaju kad izvršitelj koda zakaže")
        void survivesExecutorFailure() {
            AssessmentAttempt open = attempt(false, 0);
            stubOpenAttempt(open);

            when(codeExecutorClient.execute(any()))
                    .thenThrow(new RuntimeException("Izvršitelj nedostupan"));

            Map<String, String> answers = new HashMap<>();
            answers.put(codeQuestion.getId().toString(), "int main() {}");

            AssessmentAttemptResponse result =
                    assessmentService.submitAttempt(requestWith(answers), studentId);

            assertThat(result.getScore()).isZero();
            assertThat(open.isCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Provjera riješenosti pretesta")
    class PretestCheck {

        @Test
        @DisplayName("vraća true kad pretest postoji")
        void trueWhenPretestDone() {
            when(attemptRepository.findCompletedByStudentTypeAndCourse(
                    studentId, AssessmentType.PRETEST, courseId))
                    .thenReturn(Optional.of(attempt(true, 25)));

            assertThat(assessmentService.hasCompletedPretest(studentId, courseId)).isTrue();
        }

        @Test
        @DisplayName("vraća false kad pretest nije riješen")
        void falseWhenNoPretest() {
            when(attemptRepository.findCompletedByStudentTypeAndCourse(
                    studentId, AssessmentType.PRETEST, courseId))
                    .thenReturn(Optional.empty());

            assertThat(assessmentService.hasCompletedPretest(studentId, courseId)).isFalse();
        }

        @Test
        @DisplayName("posttest se provjerava neovisno o pretestu")
        void posttestIsSeparate() {
            when(attemptRepository.findCompletedByStudentTypeAndCourse(
                    studentId, AssessmentType.POSTTEST, courseId))
                    .thenReturn(Optional.empty());

            assertThat(assessmentService.hasCompletedPosttest(studentId, courseId)).isFalse();
        }
    }

    private void stubOpenAttempt(AssessmentAttempt open) {
        when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                studentId, assessmentId)).thenReturn(false);
        when(attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId))
                .thenReturn(Optional.of(open));
    }

    private AssessmentAttemptRequest requestWith(Map<String, String> answers) {
        AssessmentAttemptRequest r = new AssessmentAttemptRequest();
        r.setAssessmentId(assessmentId);
        r.setAnswers(answers);
        return r;
    }

    private AssessmentAttempt attempt(boolean completed, int score) {
        AssessmentAttempt a = AssessmentAttempt.builder()
                .student(student)
                .assessment(assessment)
                .score(score)
                .maxScore(30)
                .isCompleted(completed)
                .build();
        a.setId(UUID.randomUUID());
        return a;
    }

    private AssessmentQuestion question(QuestionType type, int points, String correct) {
        AssessmentQuestion q = AssessmentQuestion.builder()
                .assessment(assessment)
                .questionType(type)
                .questionText("Pitanje")
                .points(points)
                .correctAnswer(correct)
                .testCases("[]")
                .build();
        q.setId(UUID.randomUUID());
        return q;
    }
}
