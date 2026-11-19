package hr.algebra.adaptive.learning.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.QuestionType;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentAttemptRequest;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentRequest;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentResponse;
import hr.algebra.adaptive.learning.backend.dto.assessment.QuestionRequest;
import hr.algebra.adaptive.learning.backend.exception.AssessmentAlreadyCompletedException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.*;
import hr.algebra.adaptive.learning.backend.service.CodeExecutorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
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

            var request = requestWith(new HashMap<>());

            assertThatExceptionOfType(AssessmentAlreadyCompletedException.class)
                    .isThrownBy(() -> assessmentService.submitAttempt(request, studentId));
        }

        @Test
        @DisplayName("baca iznimku ako pokušaj nije započet")
        void throwsWhenNotStarted() {
            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, assessmentId)).thenReturn(false);
            when(attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId))
                    .thenReturn(Optional.empty());

            var request = requestWith(new HashMap<>());

            assertThatExceptionOfType(ResourceNotFoundException.class)
                    .isThrownBy(() -> assessmentService.submitAttempt(request, studentId));
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

    @Nested
    @DisplayName("Uređivanje testa")
    class EditingAssessment {

        @Test
        @DisplayName("mijenja naziv, opis, vrijeme i prag prolaza")
        void updatesFields() {
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> i.getArgument(0));

            AssessmentRequest request = new AssessmentRequest();
            request.setTitle("Inicijalni test");
            request.setDescription("Opis");
            request.setTimeLimitMinutes(40);
            request.setPassingScore(70);

            AssessmentResponse result = assessmentService.update(assessmentId, request);

            assertThat(result.getTitle()).isEqualTo("Inicijalni test");
            assertThat(assessment.getDescription()).isEqualTo("Opis");
            assertThat(assessment.getTimeLimitMinutes()).isEqualTo(40);
            assertThat(assessment.getPassingScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("prazan naziv i prag ostavljaju stare vrijednosti")
        void keepsTitleAndPassingScore() {
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> i.getArgument(0));
            assessment.setPassingScore(55);

            AssessmentRequest blank = new AssessmentRequest();
            blank.setTitle("   ");
            assessmentService.update(assessmentId, blank);

            AssessmentRequest empty = new AssessmentRequest();
            assessmentService.update(assessmentId, empty);

            assertThat(assessment.getTitle()).isEqualTo("Predtest");
            assertThat(assessment.getPassingScore()).isEqualTo(55);
            assertThat(assessment.getTimeLimitMinutes()).isNull();
        }

        @Test
        @DisplayName("nastavnik vidi točne odgovore i test primjere")
        void teacherViewIncludesAnswers() {
            choiceQuestion.setPublished(false);

            AssessmentResponse result = assessmentService.getByIdForTeacher(assessmentId);

            assertThat(result.getQuestions()).hasSize(2);
            assertThat(result.getQuestions().get(0).getCorrectAnswer()).isEqualTo("Petlja");
            assertThat(result.getQuestions().get(0).getPublished()).isFalse();
            assertThat(result.getQuestions().get(1).getTestCases()).isEqualTo("[]");
        }

        @Test
        @DisplayName("učenik ne vidi skrivena pitanja ni točne odgovore")
        void studentViewHidesUnpublished() {
            choiceQuestion.setPublished(false);

            AssessmentResponse result = assessmentService.getByIdWithQuestions(assessmentId);

            assertThat(result.getQuestions()).hasSize(1);
            assertThat(result.getQuestionCount()).isEqualTo(1);
            assertThat(result.getQuestions().get(0).getCorrectAnswer()).isNull();
        }

        @Test
        @DisplayName("početak pokušaja ne broji skrivena pitanja u maksimum")
        void maxScoreSkipsHidden() {
            codeQuestion.setPublished(false);
            when(attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(
                    studentId, assessmentId)).thenReturn(false);
            when(attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId))
                    .thenReturn(Optional.empty());

            AssessmentAttemptResponse result = assessmentService.startAttempt(assessmentId, studentId);

            assertThat(result.getMaxScore()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Uređivanje pitanja i preračun bodova")
    class EditingQuestions {

        @BeforeEach
        void stubQuestions() {
            when(questionRepository.findById(choiceQuestion.getId())).thenReturn(Optional.of(choiceQuestion));
            when(questionRepository.findById(codeQuestion.getId())).thenReturn(Optional.of(codeQuestion));
        }

        @Test
        @DisplayName("nova točna opcija preračunava bodove predanih pokušaja")
        void newCorrectAnswerRegrades() {
            AssessmentAttempt done = completedWith(Map.of(choiceQuestion.getId().toString(), "Grananje"));
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId))
                    .thenReturn(List.of(done));

            assessmentService.updateQuestion(choiceQuestion.getId(), choiceRequest("Grananje"));

            assertThat(choiceQuestion.getCorrectAnswer()).isEqualTo("Grananje");
            assertThat(done.getScore()).isEqualTo(10);
            assertThat(done.getMaxScore()).isEqualTo(30);
            verify(attemptRepository).save(done);
        }

        @Test
        @DisplayName("izmjena samo teksta ne preračunava bodove")
        void textOnlyChangeSkipsRegrade() {
            QuestionRequest request = choiceRequest("Petlja");
            request.setQuestionText("Novi tekst pitanja");
            request.setOrderIndex(3);

            assessmentService.updateQuestion(choiceQuestion.getId(), request);

            assertThat(choiceQuestion.getQuestionText()).isEqualTo("Novi tekst pitanja");
            assertThat(choiceQuestion.getOrderIndex()).isEqualTo(3);
            verify(attemptRepository, never()).findByAssessmentIdAndIsCompletedTrue(any());
        }

        @Test
        @DisplayName("prazan tekst, tip i bodovi ostavljaju stare vrijednosti")
        void keepsMissingValues() {
            QuestionRequest request = new QuestionRequest();
            request.setQuestionText("  ");
            request.setCorrectAnswer("Petlja");
            request.setTestCases("[]");

            assessmentService.updateQuestion(choiceQuestion.getId(), request);

            assertThat(choiceQuestion.getQuestionText()).isEqualTo("Pitanje");
            assertThat(choiceQuestion.getQuestionType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
            assertThat(choiceQuestion.getPoints()).isEqualTo(10);
        }

        @Test
        @DisplayName("svaka promjena bitna za bodovanje pokreće preračun")
        void gradingFieldsTriggerRegrade() {
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId)).thenReturn(List.of());

            QuestionRequest type = choiceRequest("Petlja");
            type.setQuestionType(QuestionType.SHORT_ANSWER);
            assessmentService.updateQuestion(choiceQuestion.getId(), type);

            QuestionRequest points = choiceRequest("Petlja");
            points.setQuestionType(QuestionType.SHORT_ANSWER);
            points.setPoints(5);
            assessmentService.updateQuestion(choiceQuestion.getId(), points);

            QuestionRequest options = choiceRequest("Petlja");
            options.setQuestionType(QuestionType.SHORT_ANSWER);
            options.setPoints(5);
            options.setOptions("[\"Petlja\"]");
            assessmentService.updateQuestion(choiceQuestion.getId(), options);

            QuestionRequest tests = choiceRequest("Petlja");
            tests.setQuestionType(QuestionType.SHORT_ANSWER);
            tests.setPoints(5);
            tests.setOptions("[\"Petlja\"]");
            tests.setTestCases("[{\"input\":\"1\",\"expectedOutput\":\"1\"}]");
            assessmentService.updateQuestion(choiceQuestion.getId(), tests);

            verify(attemptRepository, times(4)).findByAssessmentIdAndIsCompletedTrue(assessmentId);
        }

        @Test
        @DisplayName("novi test primjeri ponovno izvršavaju samo izmijenjeno pitanje")
        void newTestCasesRerunOnlyChangedQuestion() {
            Map<String, String> answers = new HashMap<>();
            answers.put(choiceQuestion.getId().toString(), "Petlja");
            answers.put(codeQuestion.getId().toString(), "int main() {}");
            AssessmentAttempt done = completedWith(answers);
            done.setQuestionScores("{\"" + choiceQuestion.getId() + "\":10,\""
                    + codeQuestion.getId() + "\":0}");
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId))
                    .thenReturn(List.of(done));

            CodeExecutionResponse exec = new CodeExecutionResponse();
            exec.setSuccess(true);
            exec.setTestsPassed(2);
            exec.setTestsTotal(2);
            when(codeExecutorClient.execute(any())).thenReturn(exec);

            QuestionRequest request = new QuestionRequest();
            request.setQuestionType(QuestionType.CODE);
            request.setTestCases("[{\"input\":\"2\",\"expectedOutput\":\"paran\"}]");
            assessmentService.updateQuestion(codeQuestion.getId(), request);

            assertThat(done.getScore()).isEqualTo(30);
            verify(codeExecutorClient, times(1)).execute(any());
        }

        @Test
        @DisplayName("skrivanje pitanja ga miče iz bodova i maksimuma, vraćanje ga vraća")
        void hidingAndShowingQuestion() {
            AssessmentAttempt done = completedWith(Map.of(choiceQuestion.getId().toString(), "Petlja"));
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId))
                    .thenReturn(List.of(done));

            assessmentService.setQuestionPublished(choiceQuestion.getId(), false);

            assertThat(choiceQuestion.getPublished()).isFalse();
            assertThat(done.getScore()).isZero();
            assertThat(done.getMaxScore()).isEqualTo(20);

            assessmentService.setQuestionPublished(choiceQuestion.getId(), true);

            assertThat(done.getScore()).isEqualTo(10);
            assertThat(done.getMaxScore()).isEqualTo(30);
        }

        @Test
        @DisplayName("pitanje dodano nakon početka pokušaja ne broji se tom učeniku")
        void laterQuestionDoesNotCount() {
            AssessmentAttempt done = completedWith(Map.of(choiceQuestion.getId().toString(), "Petlja"));
            done.setStartedAt(LocalDateTime.now().minusHours(2));
            choiceQuestion.setCreatedAt(LocalDateTime.now().minusHours(3));
            codeQuestion.setCreatedAt(LocalDateTime.now().minusHours(1));
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId))
                    .thenReturn(List.of(done));

            assessmentService.setQuestionPublished(choiceQuestion.getId(), true);

            assertThat(done.getScore()).isEqualTo(10);
            assertThat(done.getMaxScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("pokušaj bez spremljenih odgovora ostaje nepromijenjen")
        void skipsAttemptWithoutAnswers() {
            AssessmentAttempt legacy = attempt(true, 7);
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId))
                    .thenReturn(List.of(legacy));

            assessmentService.setQuestionPublished(choiceQuestion.getId(), false);

            assertThat(legacy.getScore()).isEqualTo(7);
            verify(attemptRepository, never()).save(legacy);
        }

        @Test
        @DisplayName("neispravno spremljeni odgovori daju nula bodova")
        void invalidStoredAnswersScoreZero() {
            AssessmentAttempt broken = attempt(true, 7);
            broken.setAnswers("nije json");
            AssessmentAttempt nullJson = attempt(true, 5);
            nullJson.setAnswers("null");
            AssessmentAttempt blank = attempt(true, 3);
            blank.setAnswers("  ");
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId))
                    .thenReturn(List.of(broken, nullJson, blank));

            assessmentService.setQuestionPublished(choiceQuestion.getId(), true);

            assertThat(broken.getScore()).isZero();
            assertThat(nullJson.getScore()).isZero();
            assertThat(blank.getScore()).isZero();
            assertThat(nullJson.getMaxScore()).isEqualTo(30);
        }

        @Test
        @DisplayName("brisanje pitanja preračunava bodove")
        void deletingRegrades() {
            AssessmentAttempt done = completedWith(Map.of(choiceQuestion.getId().toString(), "Petlja"));
            when(attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessmentId))
                    .thenReturn(List.of(done));

            assessmentService.deleteQuestion(choiceQuestion.getId());

            verify(questionRepository).delete(choiceQuestion);
            assertThat(assessment.getQuestions()).containsExactly(codeQuestion);
            assertThat(done.getScore()).isZero();
            assertThat(done.getMaxScore()).isEqualTo(20);
        }

        @Test
        @DisplayName("brisanje nepostojećeg pitanja ne radi ništa")
        void deletingMissingDoesNothing() {
            UUID unknown = UUID.randomUUID();
            when(questionRepository.findById(unknown)).thenReturn(Optional.empty());

            assessmentService.deleteQuestion(unknown);

            verify(questionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("izmjena nepostojećeg pitanja baca iznimku")
        void updatingMissingThrows() {
            UUID unknown = UUID.randomUUID();
            when(questionRepository.findById(unknown)).thenReturn(Optional.empty());

            QuestionRequest request = choiceRequest("Petlja");
            assertThatThrownBy(() -> assessmentService.updateQuestion(unknown, request))
                    .isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> assessmentService.setQuestionPublished(unknown, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("mijenja redoslijed pitanja")
        void reordersQuestions() {
            assessmentService.reorderQuestions(assessmentId, List.of(codeQuestion.getId(), choiceQuestion.getId()));

            assertThat(codeQuestion.getOrderIndex()).isZero();
            assertThat(choiceQuestion.getOrderIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("odbija pitanje iz drugog testa")
        void rejectsQuestionFromOtherAssessment() {
            Assessment other = Assessment.builder().title("Drugi").course(course).questions(new ArrayList<>()).build();
            other.setId(UUID.randomUUID());
            choiceQuestion.setAssessment(other);
            List<UUID> ids = List.of(choiceQuestion.getId());

            assertThatThrownBy(() -> assessmentService.reorderQuestions(assessmentId, ids))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        private QuestionRequest choiceRequest(String correct) {
            QuestionRequest r = new QuestionRequest();
            r.setAssessmentId(assessmentId);
            r.setQuestionText("Pitanje");
            r.setQuestionType(QuestionType.MULTIPLE_CHOICE);
            r.setCorrectAnswer(correct);
            r.setTestCases("[]");
            r.setPoints(10);
            return r;
        }

        private AssessmentAttempt completedWith(Map<String, String> answers) {
            AssessmentAttempt a = attempt(true, 0);
            try {
                a.setAnswers(objectMapper.writeValueAsString(answers));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return a;
        }
    }

    @Test
    @DisplayName("točno/netočno priznaje odgovor na hrvatskom i engleskom")
    void trueFalseAcceptsBothLanguages() {
        AssessmentQuestion tf = question(QuestionType.TRUE_FALSE, 1, "Netočno");
        assessment.getQuestions().add(tf);

        assertThat(scoreFor(tf, "False")).isEqualTo(1);
        assertThat(scoreFor(tf, "netocno")).isEqualTo(1);
        assertThat(scoreFor(tf, "True")).isZero();
        assertThat(scoreFor(tf, "Točno")).isZero();
        assertThat(scoreFor(tf, "  ")).isZero();
        assertThat(scoreFor(tf, "možda")).isZero();

        tf.setCorrectAnswer(null);
        assertThat(scoreFor(tf, "False")).isZero();
    }

    private int scoreFor(AssessmentQuestion q, String answer) {
        AssessmentAttempt open = attempt(false, 0);
        stubOpenAttempt(open);
        Map<String, String> answers = new HashMap<>();
        answers.put(q.getId().toString(), answer);
        assessmentService.submitAttempt(requestWith(answers), studentId);
        return open.getScore();
    }

    @Test
    @DisplayName("predaja bez mape odgovora daje nula bodova")
    void submitWithoutAnswersMap() {
        AssessmentAttempt open = attempt(false, 0);
        stubOpenAttempt(open);

        AssessmentAttemptRequest request = new AssessmentAttemptRequest();
        request.setAssessmentId(assessmentId);

        AssessmentAttemptResponse result = assessmentService.submitAttempt(request, studentId);

        assertThat(result.getScore()).isZero();
        assertThat(open.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("predaja ne broji skrivena pitanja")
    void submitSkipsHiddenQuestion() {
        AssessmentAttempt open = attempt(false, 0);
        stubOpenAttempt(open);
        choiceQuestion.setPublished(false);

        Map<String, String> answers = new HashMap<>();
        answers.put(choiceQuestion.getId().toString(), "Petlja");

        AssessmentAttemptResponse result =
                assessmentService.submitAttempt(requestWith(answers), studentId);

        assertThat(result.getScore()).isZero();
        assertThat(result.getMaxScore()).isEqualTo(20);
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