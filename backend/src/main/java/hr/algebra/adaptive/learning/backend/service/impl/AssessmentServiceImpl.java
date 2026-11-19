package hr.algebra.adaptive.learning.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import hr.algebra.adaptive.learning.backend.domain.enums.QuestionType;
import hr.algebra.adaptive.learning.backend.dto.assessment.*;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionRequest;
import hr.algebra.adaptive.learning.backend.dto.execution.CodeExecutionResponse;
import hr.algebra.adaptive.learning.backend.exception.AssessmentAlreadyCompletedException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.*;
import hr.algebra.adaptive.learning.backend.service.AssessmentService;
import hr.algebra.adaptive.learning.backend.service.CodeExecutorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CodeExecutorClient codeExecutorClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AssessmentResponse create(AssessmentRequest request, UUID createdById) {
        log.info("Creating assessment: {} for course: {}", request.getTitle(), request.getCourseId());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        User createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdById));

        Assessment assessment = Assessment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .assessmentType(request.getAssessmentType())
                .course(course)
                .createdBy(createdBy)
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 50)
                .isActive(true)
                .build();

        Assessment saved = assessmentRepository.save(assessment);
        return AssessmentResponse.fromEntity(saved);
    }

    @Override
    public AssessmentResponse getById(UUID id) {
        Assessment assessment = findAssessmentOrThrow(id);
        return AssessmentResponse.fromEntity(assessment);
    }

    @Override
    public AssessmentResponse getByIdWithQuestions(UUID id) {
        Assessment assessment = findAssessmentOrThrow(id);
        return AssessmentResponse.fromEntityWithQuestions(assessment);
    }

    @Override
    public List<AssessmentResponse> getByCourse(UUID courseId) {
        return assessmentRepository.findByCourseIdAndIsActiveTrue(courseId).stream()
                .map(AssessmentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public AssessmentResponse addQuestion(QuestionRequest request) {
        log.info("Adding question to assessment: {}", request.getAssessmentId());

        Assessment assessment = findAssessmentOrThrow(request.getAssessmentId());

        AssessmentQuestion question = AssessmentQuestion.builder()
                .assessment(assessment)
                .questionText(request.getQuestionText())
                .questionType(request.getQuestionType())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .codeTemplate(request.getCodeTemplate())
                .testCases(request.getTestCases())
                .points(request.getPoints() != null ? request.getPoints() : 1)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : assessment.getQuestions().size())
                .build();

        questionRepository.save(question);
        return AssessmentResponse.fromEntityWithQuestions(assessment);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId) {
        questionRepository.findById(questionId).ifPresent(question -> {
            Assessment assessment = question.getAssessment();
            assessment.getQuestions().remove(question);
            questionRepository.delete(question);
            regrade(assessment, null);
        });
    }

    @Override
    public AssessmentResponse getByIdForTeacher(UUID id) {
        return AssessmentResponse.fromEntityForTeacher(findAssessmentOrThrow(id));
    }

    @Override
    @Transactional
    public AssessmentResponse update(UUID id, AssessmentRequest request) {
        Assessment assessment = findAssessmentOrThrow(id);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            assessment.setTitle(request.getTitle());
        }
        assessment.setDescription(request.getDescription());
        assessment.setTimeLimitMinutes(request.getTimeLimitMinutes());
        if (request.getPassingScore() != null) {
            assessment.setPassingScore(request.getPassingScore());
        }

        return AssessmentResponse.fromEntityForTeacher(assessmentRepository.save(assessment));
    }

    @Override
    @Transactional
    public AssessmentResponse updateQuestion(UUID questionId, QuestionRequest request) {
        AssessmentQuestion question = findQuestionOrThrow(questionId);

        QuestionType newType = request.getQuestionType() != null
                ? request.getQuestionType() : question.getQuestionType();
        Integer newPoints = request.getPoints() != null ? request.getPoints() : question.getPoints();

        boolean gradingChanged = newType != question.getQuestionType()
                || !Objects.equals(newPoints, question.getPoints())
                || !Objects.equals(request.getOptions(), question.getOptions())
                || !Objects.equals(request.getCorrectAnswer(), question.getCorrectAnswer())
                || !Objects.equals(request.getTestCases(), question.getTestCases());

        if (request.getQuestionText() != null && !request.getQuestionText().isBlank()) {
            question.setQuestionText(request.getQuestionText());
        }
        question.setQuestionType(newType);
        question.setPoints(newPoints);
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setCodeTemplate(request.getCodeTemplate());
        question.setTestCases(request.getTestCases());
        if (request.getOrderIndex() != null) {
            question.setOrderIndex(request.getOrderIndex());
        }

        questionRepository.save(question);

        Assessment assessment = question.getAssessment();
        if (gradingChanged) {
            regrade(assessment, question.getId());
        }
        return AssessmentResponse.fromEntityForTeacher(assessment);
    }

    @Override
    @Transactional
    public AssessmentResponse setQuestionPublished(UUID questionId, boolean published) {
        AssessmentQuestion question = findQuestionOrThrow(questionId);
        question.setPublished(published);
        questionRepository.save(question);

        Assessment assessment = question.getAssessment();
        regrade(assessment, null);
        return AssessmentResponse.fromEntityForTeacher(assessment);
    }

    @Override
    @Transactional
    public AssessmentResponse reorderQuestions(UUID assessmentId, List<UUID> orderedIds) {
        Assessment assessment = findAssessmentOrThrow(assessmentId);

        for (int i = 0; i < orderedIds.size(); i++) {
            UUID questionId = orderedIds.get(i);
            AssessmentQuestion question = findQuestionOrThrow(questionId);
            if (!assessment.getId().equals(question.getAssessment().getId())) {
                throw new ResourceNotFoundException("AssessmentQuestion", "id", questionId);
            }
            question.setOrderIndex(i);
            questionRepository.save(question);
        }

        return AssessmentResponse.fromEntityForTeacher(assessment);
    }

    @Override
    @Transactional
    public AssessmentAttemptResponse startAttempt(UUID assessmentId, UUID studentId) {
        log.info("Student {} starting assessment {}", studentId, assessmentId);

        if (attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(studentId, assessmentId)) {
            throw new AssessmentAlreadyCompletedException("Ovaj test ste već riješili i ne možete ga ponovno pisati.");
        }

        Optional<AssessmentAttempt> existing = attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId);
        if (existing.isPresent()) {
            return AssessmentAttemptResponse.fromEntity(existing.get());
        }

        Assessment assessment = findAssessmentOrThrow(assessmentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        int maxScore = assessment.getQuestions().stream()
                .filter(AssessmentQuestion::isVisible)
                .mapToInt(AssessmentQuestion::getPoints)
                .sum();

        AssessmentAttempt attempt = AssessmentAttempt.builder()
                .student(student)
                .assessment(assessment)
                .startedAt(LocalDateTime.now())
                .score(0)
                .maxScore(maxScore)
                .isCompleted(false)
                .build();

        AssessmentAttempt saved = attemptRepository.save(attempt);
        return AssessmentAttemptResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AssessmentAttemptResponse submitAttempt(AssessmentAttemptRequest request, UUID studentId) {
        log.info("Student {} submitting assessment {}", studentId, request.getAssessmentId());

        if (attemptRepository.existsByStudentIdAndAssessmentIdAndIsCompletedTrue(studentId, request.getAssessmentId())) {
            throw new AssessmentAlreadyCompletedException("Ovaj test ste već riješili i ne možete ga ponovno predati.");
        }

        AssessmentAttempt attempt = attemptRepository.findByStudentIdAndAssessmentId(studentId, request.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentAttempt", "assessmentId", request.getAssessmentId()));

        Assessment assessment = attempt.getAssessment();
        Map<String, String> answers = request.getAnswers() != null ? request.getAnswers() : Map.of();

        applyScores(attempt, assessment, answers, null);

        try {
            attempt.setAnswers(objectMapper.writeValueAsString(answers));
        } catch (JsonProcessingException e) {
            log.error("Error serializing answers: {}", e.getMessage());
        }

        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setCompleted(true);

        AssessmentAttempt saved = attemptRepository.save(attempt);
        return AssessmentAttemptResponse.fromEntity(saved);
    }

    @Override
    public AssessmentAttemptResponse getAttempt(UUID attemptId) {
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentAttempt", "id", attemptId));
        return AssessmentAttemptResponse.fromEntity(attempt);
    }

    @Override
    public List<AssessmentAttemptResponse> getStudentAttempts(UUID studentId) {
        return attemptRepository.findByStudentId(studentId).stream()
                .map(AssessmentAttemptResponse::fromEntity)
                .toList();
    }

    @Override
    public boolean hasCompletedPretest(UUID studentId, UUID courseId) {
        return attemptRepository
                .findCompletedByStudentTypeAndCourse(studentId, AssessmentType.PRETEST, courseId)
                .isPresent();
    }

    @Override
    public boolean hasCompletedPosttest(UUID studentId, UUID courseId) {
        return attemptRepository
                .findCompletedByStudentTypeAndCourse(studentId, AssessmentType.POSTTEST, courseId)
                .isPresent();
    }

    private void regrade(Assessment assessment, UUID changedQuestionId) {
        List<AssessmentAttempt> attempts =
                attemptRepository.findByAssessmentIdAndIsCompletedTrue(assessment.getId());

        for (AssessmentAttempt attempt : attempts) {
            if (attempt.getAnswers() == null) {
                continue;
            }
            applyScores(attempt, assessment, readMap(attempt.getAnswers(), new TypeReference<Map<String, String>>() {}),
                    changedQuestionId);
            attemptRepository.save(attempt);
        }
        log.info("Preracunati bodovi za {} pokusaja testa {}", attempts.size(), assessment.getId());
    }

    private void applyScores(AssessmentAttempt attempt, Assessment assessment,
                             Map<String, String> answers, UUID changedQuestionId) {
        Map<String, Integer> cached = readMap(attempt.getQuestionScores(),
                new TypeReference<Map<String, Integer>>() {});
        Map<String, Integer> scores = new LinkedHashMap<>();
        int total = 0;
        int max = 0;

        for (AssessmentQuestion question : assessment.getQuestions()) {
            if (!countsFor(question, attempt)) {
                continue;
            }
            max += question.getPoints();

            String key = question.getId().toString();
            String studentAnswer = answers.get(key);
            if (studentAnswer == null) {
                continue;
            }

            Integer points = cached.get(key);
            if (points == null || question.getId().equals(changedQuestionId)) {
                points = gradeQuestion(question, studentAnswer);
            }
            scores.put(key, points);
            total += points;
        }

        attempt.setScore(total);
        attempt.setMaxScore(max);
        try {
            attempt.setQuestionScores(objectMapper.writeValueAsString(scores));
        } catch (JsonProcessingException e) {
            log.error("Error serializing question scores: {}", e.getMessage());
        }
    }

    private boolean countsFor(AssessmentQuestion question, AssessmentAttempt attempt) {
        if (!question.isVisible()) {
            return false;
        }
        LocalDateTime created = question.getCreatedAt();
        LocalDateTime started = attempt.getStartedAt();
        return created == null || started == null || !created.isAfter(started);
    }

    private <T> Map<String, T> readMap(String json, TypeReference<Map<String, T>> type) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            Map<String, T> parsed = objectMapper.readValue(json, type);
            return parsed != null ? parsed : new HashMap<>();
        } catch (Exception e) {
            log.error("Error parsing stored JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private int gradeQuestion(AssessmentQuestion question, String studentAnswer) {
        if (question.getQuestionType() == QuestionType.CODE) {
            return gradeCodeQuestion(question, studentAnswer);
        } else {
            if (question.getCorrectAnswer() != null &&
                    question.getCorrectAnswer().trim().equalsIgnoreCase(studentAnswer.trim())) {
                return question.getPoints();
            }
            return 0;
        }
    }

    private int gradeCodeQuestion(AssessmentQuestion question, String code) {
        try {
            List<CodeExecutionRequest.TestCase> testCases = parseTestCases(question.getTestCases());

            String language = mapLanguageType(question.getAssessment().getCourse().getLanguageType().name());

            CodeExecutionRequest execRequest = CodeExecutionRequest.builder()
                    .code(code)
                    .language(language)
                    .testCases(testCases)
                    .timeoutSeconds(10)
                    .build();

            CodeExecutionResponse response = codeExecutorClient.execute(execRequest);

            if (response.getTestsTotal() == 0) return 0;

            double ratio = (double) response.getTestsPassed() / response.getTestsTotal();
            return (int) Math.round(ratio * question.getPoints());

        } catch (Exception e) {
            log.error("Error grading code question: {}", e.getMessage());
            return 0;
        }
    }

    private String mapLanguageType(String languageType) {
        return switch (languageType) {
            case "CSHARP" -> "CSHARP";
            case "PYTHON" -> "PYTHON";
            default -> "C";
        };
    }

    private List<CodeExecutionRequest.TestCase> parseTestCases(String testCasesJson) {
        if (testCasesJson == null || testCasesJson.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, String>> parsed = objectMapper.readValue(
                    testCasesJson,
                    new TypeReference<List<Map<String, String>>>() {}
            );
            return parsed.stream()
                    .map(tc -> CodeExecutionRequest.TestCase.builder()
                            .input(tc.getOrDefault("input", ""))
                            .expectedOutput(tc.getOrDefault("expectedOutput", ""))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Error parsing test cases: {}", e.getMessage());
            return List.of();
        }
    }

    private AssessmentQuestion findQuestionOrThrow(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentQuestion", "id", id));
    }

    private Assessment findAssessmentOrThrow(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", id));
    }
}