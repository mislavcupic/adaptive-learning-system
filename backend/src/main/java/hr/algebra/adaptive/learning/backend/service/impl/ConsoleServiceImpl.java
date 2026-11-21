package hr.algebra.adaptive.learning.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.ExamAttempt;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.repository.ExamAttemptRepository;
import hr.algebra.adaptive.learning.backend.repository.TaskRepository;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.AssessmentService;
import hr.algebra.adaptive.learning.backend.service.ConsoleExecutorClient;
import hr.algebra.adaptive.learning.backend.service.ConsoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsoleServiceImpl implements ConsoleService {

    static final String INVALID_START = "Neispravan početak sesije konzole.";
    static final String INVALID_LOGIN = "Prijava nije valjana. Prijavite se ponovno.";
    static final String TASK_NOT_FOUND = "Zadatak nije pronađen.";
    static final String NO_CODE = "Nema koda za pokretanje.";
    static final String PRETEST_REQUIRED = "Morate riješiti pretest za ovaj kolegij prije rješavanja zadataka.";
    static final String EXAM_NOT_OPEN = "Ispit nije u tijeku.";
    static final String TASK_NOT_IN_EXAM = "Zadatak nije dio ovog ispita.";

    private static final int SEND_TIME_LIMIT_MS = 5000;
    private static final int BUFFER_LIMIT_BYTES = 256 * 1024;
    private static final int MAX_INPUT_CHARS = 4096;

    private final ConsoleExecutorClient executorClient;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TaskRepository taskRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AssessmentService assessmentService;
    private final ObjectMapper objectMapper;

    private final Map<String, ConsoleExecutorClient.ConsoleConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> browsers = new ConcurrentHashMap<>();

    @Value("${console.session-seconds:120}")
    private int sessionSeconds = 120;

    @Override
    @Transactional(readOnly = true)
    public void onMessage(WebSocketSession session, String payload) {
        JsonNode message = parse(payload);
        if (message == null) {
            return;
        }
        ConsoleExecutorClient.ConsoleConnection connection = connections.get(session.getId());
        if (connection == null) {
            start(session, message);
        } else {
            forward(connection, message);
        }
    }

    @Override
    public void onClose(WebSocketSession session) {
        browsers.remove(session.getId());
        ConsoleExecutorClient.ConsoleConnection connection = connections.remove(session.getId());
        if (connection != null) {
            connection.close();
        }
    }

    private void start(WebSocketSession session, JsonNode message) {
        if (!"start".equals(text(message, "type"))) {
            reject(session, INVALID_START);
            return;
        }

        User student = authenticate(text(message, "token"));
        if (student == null) {
            reject(session, INVALID_LOGIN);
            return;
        }

        Task task = findCodeTask(text(message, "taskId"));
        if (task == null) {
            reject(session, TASK_NOT_FOUND);
            return;
        }

        String accessError = checkAccess(student, task, text(message, "examId"));
        if (accessError != null) {
            reject(session, accessError);
            return;
        }

        String code = text(message, "code");
        if (code == null || code.isBlank()) {
            reject(session, NO_CODE);
            return;
        }

        Map<String, Object> startMessage = new LinkedHashMap<>();
        startMessage.put("type", "start");
        startMessage.put("code", code);
        startMessage.put("language", task.getOutcome().getCourse().getLanguageType().name());
        startMessage.put("timeoutSeconds", sessionSeconds);

        String sessionId = session.getId();
        WebSocketSession browser = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES);
        browsers.put(sessionId, browser);
        ConsoleExecutorClient.ConsoleConnection connection = executorClient.connect(
                write(startMessage),
                text -> sendToBrowser(browser, text),
                () -> closeBrowser(sessionId));
        connections.put(sessionId, connection);
        log.info("Konzola pokrenuta: student {}, zadatak {}", student.getId(), task.getId());
    }

    private void forward(ConsoleExecutorClient.ConsoleConnection connection, JsonNode message) {
        String type = text(message, "type");
        if ("input".equals(type)) {
            String data = text(message, "data");
            if (data == null) {
                return;
            }
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("type", "input");
            input.put("data", data.length() > MAX_INPUT_CHARS ? data.substring(0, MAX_INPUT_CHARS) : data);
            connection.send(write(input));
        } else if ("eof".equals(type) || "stop".equals(type)) {
            connection.send(write(Map.of("type", type)));
        }
    }

    private User authenticate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String email = jwtService.extractUsername(token);
            UserDetails details = userDetailsService.loadUserByUsername(email);
            if (!(details instanceof User user) || !jwtService.isTokenValid(token, details)) {
                return null;
            }
            return user.isActive() && user.getRole() == UserRole.STUDENT ? user : null;
        } catch (Exception e) {
            log.warn("Konzola: neispravan token: {}", e.getMessage());
            return null;
        }
    }

    private Task findCodeTask(String taskId) {
        UUID id = toUuid(taskId);
        if (id == null) {
            return null;
        }
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null || task.getOutcome() == null || task.getOutcome().getCourse() == null) {
            return null;
        }
        boolean isCode = task.getTaskType() == null || task.getTaskType() == TaskType.CODE;
        return isCode ? task : null;
    }

    private String checkAccess(User student, Task task, String examId) {
        UUID courseId = task.getOutcome().getCourse().getId();
        if (!assessmentService.hasCompletedPretest(student.getId(), courseId)) {
            return PRETEST_REQUIRED;
        }
        if (examId == null) {
            return null;
        }
        UUID examUuid = toUuid(examId);
        if (examUuid == null) {
            return EXAM_NOT_OPEN;
        }
        Optional<ExamAttempt> attempt = examAttemptRepository.findByStudentAndExamWithAnswers(student.getId(), examUuid);
        if (attempt.isEmpty()
                || attempt.get().getStatus() != ExamAttemptStatus.IN_PROGRESS
                || attempt.get().isExpired()) {
            return EXAM_NOT_OPEN;
        }
        boolean taskInExam = attempt.get().getAnswers().stream()
                .anyMatch(answer -> answer.getTask() != null && task.getId().equals(answer.getTask().getId()));
        return taskInExam ? null : TASK_NOT_IN_EXAM;
    }

    private void sendToBrowser(WebSocketSession browser, String text) {
        if (!browser.isOpen()) {
            return;
        }
        try {
            browser.sendMessage(new TextMessage(text));
        } catch (IOException | IllegalStateException e) {
            log.warn("Konzola: slanje pregledniku nije uspjelo: {}", e.getMessage());
        }
    }

    private void closeBrowser(String sessionId) {
        connections.remove(sessionId);
        WebSocketSession browser = browsers.remove(sessionId);
        if (browser == null || !browser.isOpen()) {
            return;
        }
        try {
            browser.close(CloseStatus.NORMAL);
        } catch (IOException e) {
            log.warn("Konzola: zatvaranje veze nije uspjelo: {}", e.getMessage());
        }
    }

    private void reject(WebSocketSession session, String reason) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(write(Map.of("type", "error", "message", reason))));
                session.close(CloseStatus.POLICY_VIOLATION);
            }
        } catch (IOException | IllegalStateException e) {
            log.warn("Konzola: odbijanje sesije nije uspjelo: {}", e.getMessage());
        }
    }

    private JsonNode parse(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return node != null && node.isObject() ? node : null;
        } catch (JsonProcessingException _) {
            return null;
        }
    }

    private String write(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static UUID toUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }
}
