package hr.algebra.adaptive.learning.backend.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionRequest;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackResponse;
import hr.algebra.adaptive.learning.backend.dto.request.SubmissionRequest;
import hr.algebra.adaptive.learning.backend.dto.response.PaginatedResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SubmissionResponse;
import hr.algebra.adaptive.learning.backend.exception.PretestRequiredException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.SubmissionRepository;
import hr.algebra.adaptive.learning.backend.repository.TaskRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.AssessmentService;
import hr.algebra.adaptive.learning.backend.service.CodeExecutorClient;
import hr.algebra.adaptive.learning.backend.service.MLServiceClient;
import hr.algebra.adaptive.learning.backend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionServiceImpl implements SubmissionService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    // Toleriram visak kljuceva u JSON-u - nastavnik uz input i output
    // zna upisati i opis test casea.
    private static final ObjectMapper TEST_CASE_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final SubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CodeExecutorClient codeExecutorClient;
    private final MLServiceClient mlServiceClient;
    private final AssessmentService assessmentService;
    private final SubmissionPostProcessor postProcessor;

    @Override
    @Transactional
    public SubmissionResponse submit(SubmissionRequest request, UUID studentId) {
        log.info("Student {} submitting code for task {}", studentId, request.getTaskId());

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        UUID courseId = task.getOutcome().getCourse().getId();
        if (!assessmentService.hasCompletedPretest(studentId, courseId)) {
            log.warn("Student {} pokušao predati zadatak bez riješenog pretesta za kolegij {}", studentId, courseId);
            throw new PretestRequiredException(courseId);
        }

        // 1. Kreiraj submission
        Submission submission = Submission.builder()
                .student(student)
                .task(task)
                .submittedCode(request.getCode())
                .status(SubmissionStatus.PENDING)
                .build();

        Submission saved = submissionRepository.save(submission);
        log.info("Submission created with ID: {}", saved.getId());

        // 2. Zadaci koji nisu programski ne idu u compiler
        TaskType taskType = task.getTaskType() != null ? task.getTaskType() : TaskType.CODE;
        if (taskType != TaskType.CODE) {
            return handleNonCodeSubmission(saved, task, request.getCode());
        }

        // 3. Izvršavanje koda
        saved.setStatus(SubmissionStatus.COMPILING);
        submissionRepository.save(saved);

        CodeExecutionResponse execResult = executeCode(request.getCode(), task);

        saved.setCompilerOutput(execResult.getCompilerOutput());
        saved.setTestsPassed(execResult.getTestsPassed());
        saved.setTestsTotal(execResult.getTestsTotal());
        saved.setValgrindOutput(execResult.getValgrindOutput());
        saved.setTestResults(execResult.getTestResults());
        saved.setExecutionTimeMs(execResult.getExecutionTimeMs() != null
                ? execResult.getExecutionTimeMs().longValue() : null);

        if (!execResult.isSuccess()) {
            saved.setStatus(SubmissionStatus.COMPILE_ERROR);
            saved.setExecutionOutput(execResult.getError());

            // I kod greske kompilacije trazi se povratna informacija: eksperimentalna
            // skupina dobiva objasnjenje poruke kompajlera i biljesku za RAG, a
            // kontrolna od ML servisa dobiva prazan tekst, kao i inace.
            // Ako executor nije odgovorio, nema poruke kompajlera pa se ML ne zove.
            String compilerOutput = execResult.getCompilerOutput();
            if (compilerOutput != null && !compilerOutput.isBlank()) {
                MLFeedbackResponse mlResponse = callMLService(saved, execResult);
                if (mlResponse != null) {
                    saved.setAiFeedback(mlResponse.getAiFeedback());
                    saved.setAiScore(mlResponse.getAiScore());
                    saved.setFinalScore(mlResponse.getAiScore());
                }
            }

            Submission failed = submissionRepository.save(saved);
            scheduleAfterCommit(failed.getId());
            return SubmissionResponse.fromEntity(failed);
        }

        saved.setStatus(SubmissionStatus.COMPLETED);
        saved.setExecutionOutput(execResult.getExecutionOutput());

        // 5. Pozovi ML servis za AI feedback
        MLFeedbackResponse mlResponse = callMLService(saved, execResult);
        if (mlResponse != null) {
            saved.setAiFeedback(mlResponse.getAiFeedback());
            saved.setAiScore(mlResponse.getAiScore());
            saved.setFinalScore(mlResponse.getAiScore());
        }

        Submission result = submissionRepository.save(saved);
        scheduleAfterCommit(result.getId());
        return SubmissionResponse.fromEntity(result);
    }

    /**
     * Obrada odgovora na TEXT, MULTIPLE_CHOICE i CHECKLIST zadatke.
     *
     * Multiple choice ima jedan točan odgovor pa se ocjenjuje odmah,
     * ostalo ide profesoru na ručni pregled.
     */
    private SubmissionResponse handleNonCodeSubmission(Submission saved, Task task, String answer) {
        saved.setStatus(SubmissionStatus.COMPLETED);
        saved.setCompilerOutput(null);
        saved.setExecutionOutput(null);
        saved.setTestsPassed(null);
        saved.setTestsTotal(null);

        if (task.getTaskType() == TaskType.MULTIPLE_CHOICE) {
            boolean correct = task.getCorrectAnswer() != null
                    && answer != null
                    && task.getCorrectAnswer().trim().equalsIgnoreCase(answer.trim());

            int maxScore = task.getMaxScore() != null ? task.getMaxScore() : 0;
            int score = correct ? maxScore : 0;

            saved.setAiScore(score);
            saved.setFinalScore(score);
            saved.setAiFeedback(correct
                    ? "Točan odgovor."
                    : "Odgovor nije točan. Pokušajte ponovno razmotriti zadatak.");

            log.info("Multiple choice ocijenjen: {}/{} bodova", score, maxScore);
        } else {
            saved.setAiFeedback("Vaš odgovor je zaprimljen. Nastavnik će ga pregledati i ocijeniti.");
            log.info("Odgovor tipa {} spremljen za ručni pregled", task.getTaskType());
        }

        Submission result = submissionRepository.save(saved);
        scheduleAfterCommit(result.getId());
        return SubmissionResponse.fromEntity(result);
    }

    /**
     * BKT update ide tek nakon commita.
     *
     * ML servis ima vlastitu konekciju na bazu i ne vidi submission dok
     * transakcija nije potvrđena - zato prije ovoga nije mogao spremiti
     * ni RAG bilješku ni mastery zapis (foreign key na submissions).
     */
    private void scheduleAfterCommit(UUID submissionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            postProcessor.process(submissionId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        postProcessor.process(submissionId);
                    }
                });
    }

    private CodeExecutionResponse executeCode(String code, Task task) {
        CodeExecutionRequest execRequest = CodeExecutionRequest.builder()
                .code(code)
                .language(task.getOutcome().getCourse().getLanguageType().name())
                .testCases(buildTestCases(task))
                .timeoutSeconds(resolveTimeoutSeconds(task))
                .build();

        return codeExecutorClient.execute(execRequest);
    }

    private int resolveTimeoutSeconds(Task task) {
        Integer limit = task.getTimeLimitSeconds();
        return (limit != null && limit > 0) ? limit : DEFAULT_TIMEOUT_SECONDS;
    }

    /**
     * Test caseovi se na zadatku cuvaju kao JSON tekst, ne kao zaseban entitet.
     *
     * Kod neispravnog zapisa vracam praznu listu umjesto iznimke - predaja
     * tada prolazi bez provjere tocnosti, sto mi je bolje nego da student
     * ostane bez ikakvog odgovora. Razlog zapisujem u log.
     */
    private List<CodeExecutionRequest.TestCase> buildTestCases(Task task) {
        String raw = task.getTestCases();

        if (raw == null || raw.isBlank()) {
            log.warn("Zadatak {} nema definirane test caseove - kod se izvrsava bez provjere tocnosti",
                    task.getId());
            return List.of();
        }

        try {
            JsonNode root = TEST_CASE_MAPPER.readTree(raw);

            // Starije zapise iz sucelja znam naci dvostruko enkodirane.
            if (root.isTextual()) {
                root = TEST_CASE_MAPPER.readTree(root.asText());
            }

            if (!root.isArray()) {
                log.warn("Test caseovi zadatka {} nisu JSON polje nego {} - preskacem",
                        task.getId(), root.getNodeType());
                return List.of();
            }

            List<CodeExecutionRequest.TestCase> testCases = new ArrayList<>();

            for (JsonNode node : root) {
                CodeExecutionRequest.TestCase testCase = toTestCase(node, task.getId());

                if (testCase != null) {
                    testCases.add(testCase);
                }
            }

            log.info("Zadatak {}: ucitano {} test caseova", task.getId(), testCases.size());
            return testCases;

        } catch (Exception e) {
            log.error("Neispravan format test caseova za zadatak {}: {}", task.getId(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Sucelje test case sprema kao obican tekst - samo ocekivani izlaz, bez
     * ulaza, jer takvi zadaci nista ne citaju. Objektni oblik s ulazom citam
     * jednako, da forma kasnije moze prijeci na parove bez diranja backenda.
     *
     * Vracam null za zapis iz kojeg ne mogu procitati ocekivani izlaz.
     */
    private CodeExecutionRequest.TestCase toTestCase(JsonNode node, UUID taskId) {
        if (node.isTextual()) {
            return CodeExecutionRequest.TestCase.builder()
                    .input("")
                    .expectedOutput(node.asText())
                    .build();
        }

        String expected = firstText(node, "expectedOutput", "expected_output", "expected", "output");

        if (expected == null) {
            log.warn("Test case zadatka {} nema ocekivani izlaz - preskacem: {}", taskId, node);
            return null;
        }

        String input = firstText(node, "input", "stdin", "in");

        return CodeExecutionRequest.TestCase.builder()
                .input(input != null ? input : "")
                .expectedOutput(expected)
                .build();
    }

    // Nazive kljuceva sam mijenjao kroz razvoj, pa gledam sve varijante.
    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private MLFeedbackResponse callMLService(Submission submission, CodeExecutionResponse execResult) {
        try {
            MLFeedbackRequest mlRequest = MLFeedbackRequest.builder()
                    .submissionId(submission.getId().toString())
                    .studentId(submission.getStudent().getId().toString())
                    .taskId(submission.getTask().getId().toString())
                    .taskTitle(submission.getTask().getTitle())
                    .skillName(submission.getTask().getOutcome() != null
                            ? submission.getTask().getOutcome().getName() : null)
                    .languageType(submission.getTask().getOutcome().getCourse().getLanguageType().name())
                    .submittedCode(submission.getSubmittedCode())
                    .compilerOutput(execResult.getCompilerOutput())
                    .executionOutput(execResult.getExecutionOutput())
                    .testResults(execResult.getTestResults())
                    .valgrindOutput(execResult.getValgrindOutput())
                    .testsPassed(execResult.getTestsPassed())
                    .testsTotal(execResult.getTestsTotal())
                    .researchGroup(submission.getStudent().getResearchGroup().name())
                    .build();

            return mlServiceClient.generateFeedback(mlRequest);
        } catch (Exception e) {
            log.error("ML Service error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<SubmissionResponse> getAll() {
        return submissionRepository.findAll().stream()
                .map(SubmissionResponse::fromEntity)
                .toList();
    }

    @Override
    public SubmissionResponse getById(UUID id) {
        Submission submission = findSubmissionOrThrow(id);
        return SubmissionResponse.fromEntity(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getByStudent(UUID studentId) {
        return submissionRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(SubmissionResponse::fromEntity)
                .toList();
    }

    @Override
    public PaginatedResponse<SubmissionResponse> getByStudentPaginated(UUID studentId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Submission> submissionPage = submissionRepository.findByStudentId(studentId, pageRequest);
        return PaginatedResponse.fromPage(submissionPage, SubmissionResponse::fromEntity);
    }

    @Override
    public List<SubmissionResponse> getByTask(UUID taskId) {
        return submissionRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(SubmissionResponse::fromEntity)
                .toList();
    }

    @Override
    public PaginatedResponse<SubmissionResponse> getByTaskPaginated(UUID taskId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Submission> submissionPage = submissionRepository.findByTaskId(taskId, pageRequest);
        return PaginatedResponse.fromPage(submissionPage, SubmissionResponse::fromEntity);
    }

    @Override
    public long countByStudent(UUID studentId) {
        return submissionRepository.countByStudentId(studentId);
    }

    @Override
    public long countByTask(UUID taskId) {
        return submissionRepository.countByTaskId(taskId);
    }

    @Override
    @Transactional
    public SubmissionResponse addTeacherFeedback(UUID id, String feedback, Integer score) {
        log.info("Adding teacher feedback to submission: {}", id);

        Submission submission = findSubmissionOrThrow(id);

        submission.setTeacherFeedback(feedback);
        submission.setTeacherScore(score);
        submission.setStatus(SubmissionStatus.REVIEWED);

        if (score != null) {
            submission.setFinalScore(score);
        }

        Submission saved = submissionRepository.save(submission);
        return SubmissionResponse.fromEntity(saved);
    }

    private Submission findSubmissionOrThrow(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", id));
    }
}