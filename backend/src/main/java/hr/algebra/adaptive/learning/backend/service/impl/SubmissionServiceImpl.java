package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionRequest;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MLFeedbackResponse;
import hr.algebra.adaptive.learning.backend.dto.request.SubmissionRequest;
import hr.algebra.adaptive.learning.backend.dto.response.PaginatedResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SubmissionResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.SubmissionRepository;
import hr.algebra.adaptive.learning.backend.repository.TaskRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CodeExecutorClient codeExecutorClient;
    private final MLServiceClient mlServiceClient;

    @Override
    @Transactional
    public SubmissionResponse submit(SubmissionRequest request, UUID studentId) {
        log.info("Student {} submitting code for task {}", studentId, request.getTaskId());

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        // 1. Kreiraj submission
        Submission submission = Submission.builder()
                .student(student)
                .task(task)
                .submittedCode(request.getCode())
                .status(SubmissionStatus.PENDING)
                .build();

        Submission saved = submissionRepository.save(submission);
        log.info("Submission created with ID: {}", saved.getId());

        // 2. Izvršavanje koda
        saved.setStatus(SubmissionStatus.COMPILING);
        submissionRepository.save(saved);

        CodeExecutionResponse execResult = executeCode(request.getCode(), task);

        // 3. Spremi rezultate izvršavanja
        saved.setCompilerOutput(execResult.getCompilerOutput());
        saved.setTestsPassed(execResult.getTestsPassed());
        saved.setTestsTotal(execResult.getTestsTotal());

        if (!execResult.isSuccess()) {
            saved.setStatus(SubmissionStatus.COMPILE_ERROR);
            saved.setExecutionOutput(execResult.getError());
            return SubmissionResponse.fromEntity(submissionRepository.save(saved));
        }

        saved.setStatus(SubmissionStatus.COMPLETED);
        saved.setExecutionOutput(execResult.getExecutionOutput());

        // 4. Pozovi ML servis za AI feedback
        MLFeedbackResponse mlResponse = callMLService(saved, execResult);
        if (mlResponse != null) {
            saved.setAiFeedback(mlResponse.getAiFeedback());
            saved.setAiScore(mlResponse.getAiScore());
            saved.setFinalScore(mlResponse.getAiScore());
        }

        return SubmissionResponse.fromEntity(submissionRepository.save(saved));
    }

    private CodeExecutionResponse executeCode(String code, Task task) {
        CodeExecutionRequest execRequest = CodeExecutionRequest.builder()
                .code(code)
                .language(task.getOutcome().getCourse().getLanguageType().name())
                .testCases(buildTestCases(task))
                .timeoutSeconds(10)
                .build();

        return codeExecutorClient.execute(execRequest);
    }

    private List<CodeExecutionRequest.TestCase> buildTestCases(Task task) {
        // TODO: dohvati prave test caseove iz Task entiteta
        // Za sad vraćamo praznu listu - treba dodati TestCase entitet
        return List.of();
    }

    private MLFeedbackResponse callMLService(Submission submission, CodeExecutionResponse execResult) {
        try {
            MLFeedbackRequest mlRequest = MLFeedbackRequest.builder()
                    .submissionId(submission.getId().toString())
                    .studentId(submission.getStudent().getId().toString())
                    .taskId(submission.getTask().getId().toString())
                    .taskTitle(submission.getTask().getTitle())
                    .languageType(submission.getTask().getOutcome().getCourse().getLanguageType().name())
                    .submittedCode(submission.getSubmittedCode())
                    .compilerOutput(execResult.getCompilerOutput())
                    .executionOutput(execResult.getExecutionOutput())
                    .testResults(execResult.getTestResults())
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