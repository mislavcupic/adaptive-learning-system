package hr.algebra.adaptive.learning.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.ExamAnswer;
import hr.algebra.adaptive.learning.backend.domain.entity.ExamAttempt;
import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.repository.ExamAttemptRepository;
import hr.algebra.adaptive.learning.backend.repository.TaskRepository;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.AssessmentService;
import hr.algebra.adaptive.learning.backend.service.ConsoleExecutorClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConsoleServiceImpl")
class ConsoleServiceImplTest {

    private static final String TOKEN = "valjani.jwt.token";
    private static final String EMAIL = "ucenik@test.hr";
    private static final String CODE = "#include <stdio.h>\nint main(void) { return 0; }";

    @Mock private ConsoleExecutorClient executorClient;
    @Mock private ConsoleExecutorClient.ConsoleConnection connection;
    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private TaskRepository taskRepository;
    @Mock private ExamAttemptRepository examAttemptRepository;
    @Mock private AssessmentService assessmentService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private ConsoleServiceImpl service;

    @Mock private WebSocketSession session;

    private final ObjectMapper json = new ObjectMapper();

    private User student;
    private Course course;
    private Task task;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .email(EMAIL)
                .firstName("Test")
                .lastName("Učenik")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
        student.setId(UUID.randomUUID());

        course = new Course();
        course.setId(UUID.randomUUID());
        course.setLanguageType(LanguageType.C);

        LearningOutcome outcome = new LearningOutcome();
        outcome.setId(UUID.randomUUID());
        outcome.setName("Funkcije");
        outcome.setCourse(course);

        task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("Veći od dva broja");
        task.setTaskType(TaskType.CODE);
        task.setOutcome(outcome);

        when(session.getId()).thenReturn("sesija-1");
        when(session.isOpen()).thenReturn(true);
        when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(student);
        when(jwtService.isTokenValid(TOKEN, student)).thenReturn(true);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(assessmentService.hasCompletedPretest(student.getId(), course.getId())).thenReturn(true);
        when(executorClient.connect(anyString(), any(), any())).thenReturn(connection);
    }

    private String startMessage(Map<String, Object> overrides) throws Exception {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "start");
        message.put("token", TOKEN);
        message.put("taskId", task.getId().toString());
        message.put("code", CODE);
        message.putAll(overrides);
        return json.writeValueAsString(message);
    }

    private String startMessage() throws Exception {
        return startMessage(Map.of());
    }

    private void assertRejectedWith(String reason) throws Exception {
        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(sent.capture());
        JsonNode body = json.readTree(sent.getValue().getPayload());
        assertThat(body.get("type").asText()).isEqualTo("error");
        assertThat(body.get("message").asText()).isEqualTo(reason);
        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(executorClient, never()).connect(anyString(), any(), any());
    }

    private String capturedStart() {
        ArgumentCaptor<String> start = ArgumentCaptor.forClass(String.class);
        verify(executorClient).connect(start.capture(), any(), any());
        return start.getValue();
    }

    @Nested
    @DisplayName("Pokretanje konzole")
    class Start {

        @Test
        @DisplayName("ucenik s valjanim tokenom pokrece program u jeziku kolegija")
        void startsForStudent() throws Exception {
            service.onMessage(session, startMessage());

            JsonNode start = json.readTree(capturedStart());
            assertThat(start.get("type").asText()).isEqualTo("start");
            assertThat(start.get("language").asText()).isEqualTo("C");
            assertThat(start.get("code").asText()).isEqualTo(CODE);
            assertThat(start.get("timeoutSeconds").asInt()).isEqualTo(120);
            assertThat(start.has("token")).isFalse();
            verify(session, never()).close(any());
        }

        @Test
        @DisplayName("zadatak bez tipa smatra se programskim")
        void taskWithoutTypeIsCode() throws Exception {
            task.setTaskType(null);

            service.onMessage(session, startMessage());

            verify(executorClient).connect(anyString(), any(), any());
        }

        @Test
        @DisplayName("prva poruka mora biti start")
        void firstMessageMustBeStart() throws Exception {
            service.onMessage(session, "{\"type\":\"input\",\"data\":\"5\\n\"}");

            assertRejectedWith(ConsoleServiceImpl.INVALID_START);
        }

        @Test
        @DisplayName("neispravan JSON se zanemaruje")
        void ignoresInvalidJson() throws Exception {
            service.onMessage(session, "nije json");
            service.onMessage(session, "[1,2]");

            verify(session, never()).sendMessage(any());
            verifyNoInteractions(executorClient);
        }

        @Test
        @DisplayName("bez tokena odbija sesiju")
        void rejectsMissingToken() throws Exception {
            Map<String, Object> overrides = new LinkedHashMap<>();
            overrides.put("token", null);

            service.onMessage(session, startMessage(overrides));

            assertRejectedWith(ConsoleServiceImpl.INVALID_LOGIN);
        }

        @Test
        @DisplayName("nevaljan token odbija sesiju")
        void rejectsInvalidToken() throws Exception {
            when(jwtService.isTokenValid(TOKEN, student)).thenReturn(false);

            service.onMessage(session, startMessage());

            assertRejectedWith(ConsoleServiceImpl.INVALID_LOGIN);
        }

        @Test
        @DisplayName("nepostojeci korisnik odbija sesiju")
        void rejectsUnknownUser() throws Exception {
            when(userDetailsService.loadUserByUsername(EMAIL)).thenThrow(new UsernameNotFoundException("nema"));

            service.onMessage(session, startMessage());

            assertRejectedWith(ConsoleServiceImpl.INVALID_LOGIN);
        }

        @Test
        @DisplayName("nastavnik ne moze pokrenuti konzolu")
        void rejectsTeacher() throws Exception {
            student.setRole(UserRole.TEACHER);

            service.onMessage(session, startMessage());

            assertRejectedWith(ConsoleServiceImpl.INVALID_LOGIN);
        }

        @Test
        @DisplayName("neaktivan racun ne moze pokrenuti konzolu")
        void rejectsInactiveUser() throws Exception {
            student.setActive(false);

            service.onMessage(session, startMessage());

            assertRejectedWith(ConsoleServiceImpl.INVALID_LOGIN);
        }

        @Test
        @DisplayName("nepostojeci zadatak odbija sesiju")
        void rejectsUnknownTask() throws Exception {
            when(taskRepository.findById(task.getId())).thenReturn(Optional.empty());

            service.onMessage(session, startMessage());

            assertRejectedWith(ConsoleServiceImpl.TASK_NOT_FOUND);
        }

        @Test
        @DisplayName("neispravan ID zadatka odbija sesiju")
        void rejectsInvalidTaskId() throws Exception {
            service.onMessage(session, startMessage(Map.of("taskId", "nije-uuid")));

            assertRejectedWith(ConsoleServiceImpl.TASK_NOT_FOUND);
        }

        @Test
        @DisplayName("zadatak koji nije programski odbija sesiju")
        void rejectsNonCodeTask() throws Exception {
            task.setTaskType(TaskType.TEXT);

            service.onMessage(session, startMessage());

            assertRejectedWith(ConsoleServiceImpl.TASK_NOT_FOUND);
        }

        @Test
        @DisplayName("bez rijesenog pretesta odbija sesiju")
        void rejectsWithoutPretest() throws Exception {
            when(assessmentService.hasCompletedPretest(student.getId(), course.getId())).thenReturn(false);

            service.onMessage(session, startMessage());

            assertRejectedWith(ConsoleServiceImpl.PRETEST_REQUIRED);
        }

        @Test
        @DisplayName("prazan kod odbija sesiju")
        void rejectsEmptyCode() throws Exception {
            service.onMessage(session, startMessage(Map.of("code", "   ")));

            assertRejectedWith(ConsoleServiceImpl.NO_CODE);
        }

        @Test
        @DisplayName("zatvorena sesija se ne pokusava odbiti porukom")
        void doesNotWriteToClosedSession() throws Exception {
            when(session.isOpen()).thenReturn(false);

            service.onMessage(session, "{\"type\":\"input\"}");

            verify(session, never()).sendMessage(any());
            verify(session, never()).close(any());
        }
    }

    @Nested
    @DisplayName("Konzola u ispitu")
    class InExam {

        private UUID examId;
        private ExamAttempt attempt;

        @BeforeEach
        void setUpExam() {
            examId = UUID.randomUUID();
            attempt = ExamAttempt.builder()
                    .student(student)
                    .status(ExamAttemptStatus.IN_PROGRESS)
                    .startedAt(LocalDateTime.now().minusMinutes(5))
                    .deadlineAt(LocalDateTime.now().plusMinutes(30))
                    .answers(new ArrayList<>(List.of(ExamAnswer.builder().task(task).build())))
                    .build();
            when(examAttemptRepository.findByStudentAndExamWithAnswers(student.getId(), examId))
                    .thenReturn(Optional.of(attempt));
        }

        @Test
        @DisplayName("pokrece se kad je ispit u tijeku i zadatak je dio ispita")
        void startsDuringExam() throws Exception {
            service.onMessage(session, startMessage(Map.of("examId", examId.toString())));

            verify(executorClient).connect(anyString(), any(), any());
        }

        @Test
        @DisplayName("predan ispit odbija sesiju")
        void rejectsSubmittedExam() throws Exception {
            attempt.setStatus(ExamAttemptStatus.SUBMITTED);

            service.onMessage(session, startMessage(Map.of("examId", examId.toString())));

            assertRejectedWith(ConsoleServiceImpl.EXAM_NOT_OPEN);
        }

        @Test
        @DisplayName("istekao rok odbija sesiju")
        void rejectsExpiredExam() throws Exception {
            attempt.setDeadlineAt(LocalDateTime.now().minusMinutes(1));

            service.onMessage(session, startMessage(Map.of("examId", examId.toString())));

            assertRejectedWith(ConsoleServiceImpl.EXAM_NOT_OPEN);
        }

        @Test
        @DisplayName("bez pokusaja ispita odbija sesiju")
        void rejectsMissingAttempt() throws Exception {
            when(examAttemptRepository.findByStudentAndExamWithAnswers(student.getId(), examId))
                    .thenReturn(Optional.empty());

            service.onMessage(session, startMessage(Map.of("examId", examId.toString())));

            assertRejectedWith(ConsoleServiceImpl.EXAM_NOT_OPEN);
        }

        @Test
        @DisplayName("neispravan ID ispita odbija sesiju")
        void rejectsInvalidExamId() throws Exception {
            service.onMessage(session, startMessage(Map.of("examId", "nije-uuid")));

            assertRejectedWith(ConsoleServiceImpl.EXAM_NOT_OPEN);
        }

        @Test
        @DisplayName("zadatak koji nije u ispitu odbija sesiju")
        void rejectsTaskOutsideExam() throws Exception {
            Task other = new Task();
            other.setId(UUID.randomUUID());
            attempt.setAnswers(new ArrayList<>(List.of(
                    ExamAnswer.builder().task(other).build(),
                    ExamAnswer.builder().build())));

            service.onMessage(session, startMessage(Map.of("examId", examId.toString())));

            assertRejectedWith(ConsoleServiceImpl.TASK_NOT_IN_EXAM);
        }
    }

    @Nested
    @DisplayName("Tijek sesije")
    class Running {

        private Consumer<String> toBrowser;
        private Runnable onExecutorClosed;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void startSession() throws Exception {
            service.onMessage(session, startMessage());
            ArgumentCaptor<Consumer<String>> consumer = ArgumentCaptor.forClass(Consumer.class);
            ArgumentCaptor<Runnable> closed = ArgumentCaptor.forClass(Runnable.class);
            verify(executorClient).connect(anyString(), consumer.capture(), closed.capture());
            toBrowser = consumer.getValue();
            onExecutorClosed = closed.getValue();
        }

        private JsonNode lastSent() throws Exception {
            ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
            verify(connection, atLeastOnce()).send(sent.capture());
            return json.readTree(sent.getValue());
        }

        @Test
        @DisplayName("unos prosljeduje izvrsitelju")
        void forwardsInput() throws Exception {
            service.onMessage(session, "{\"type\":\"input\",\"data\":\"5\\n\"}");

            JsonNode sent = lastSent();
            assertThat(sent.get("type").asText()).isEqualTo("input");
            assertThat(sent.get("data").asText()).isEqualTo("5\n");
        }

        @Test
        @DisplayName("predugacak unos skracuje")
        void truncatesLongInput() throws Exception {
            String longInput = "1".repeat(5000);

            service.onMessage(session, json.writeValueAsString(Map.of("type", "input", "data", longInput)));

            assertThat(lastSent().get("data").asText()).hasSize(4096);
        }

        @Test
        @DisplayName("kraj unosa i zaustavljanje prosljeduje izvrsitelju")
        void forwardsEofAndStop() throws Exception {
            service.onMessage(session, "{\"type\":\"eof\"}");
            assertThat(lastSent().get("type").asText()).isEqualTo("eof");

            service.onMessage(session, "{\"type\":\"stop\"}");
            assertThat(lastSent().get("type").asText()).isEqualTo("stop");
        }

        @Test
        @DisplayName("nepoznate poruke i unos bez podataka zanemaruje")
        void ignoresUnknownMessages() {
            service.onMessage(session, "{\"type\":\"nesto\"}");
            service.onMessage(session, "{\"type\":\"input\"}");
            service.onMessage(session, "{\"type\":\"start\",\"token\":\"x\"}");

            verify(connection, never()).send(anyString());
        }

        @Test
        @DisplayName("poruke izvrsitelja salje pregledniku")
        void relaysExecutorMessages() throws Exception {
            toBrowser.accept("{\"type\":\"output\",\"data\":\"Unesi broj: \"}");

            ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
            verify(session).sendMessage(sent.capture());
            assertThat(sent.getValue().getPayload()).isEqualTo("{\"type\":\"output\",\"data\":\"Unesi broj: \"}");
        }

        @Test
        @DisplayName("ne salje zatvorenom pregledniku")
        void skipsClosedBrowser() throws Exception {
            when(session.isOpen()).thenReturn(false);

            toBrowser.accept("{\"type\":\"output\",\"data\":\"x\"}");

            verify(session, never()).sendMessage(any());
        }

        @Test
        @DisplayName("kad izvrsitelj zavrsi, zatvara vezu s preglednikom")
        void closesBrowserWhenExecutorCloses() throws Exception {
            onExecutorClosed.run();

            verify(session).close(CloseStatus.NORMAL);

            service.onMessage(session, "{\"type\":\"input\",\"data\":\"5\\n\"}");
            verify(connection, never()).send(anyString());
        }

        @Test
        @DisplayName("zatvaranje preglednika zatvara vezu s izvrsiteljem")
        void closesExecutorWhenBrowserCloses() {
            service.onClose(session);
            service.onClose(session);

            verify(connection, times(1)).close();
        }
    }
}
