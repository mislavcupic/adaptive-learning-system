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
        questionRepository.deleteById(questionId);
    }

    @Override
    @Transactional
    public AssessmentAttemptResponse startAttempt(UUID assessmentId, UUID studentId) {
        log.info("Student {} starting assessment {}", studentId, assessmentId);

        // Provjeri postoji li već pokušaj
        Optional<AssessmentAttempt> existing = attemptRepository.findByStudentIdAndAssessmentId(studentId, assessmentId);
        if (existing.isPresent()) {
            return AssessmentAttemptResponse.fromEntity(existing.get());
        }

        Assessment assessment = findAssessmentOrThrow(assessmentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        int maxScore = assessment.getQuestions().stream()
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

        AssessmentAttempt attempt = attemptRepository.findByStudentIdAndAssessmentId(studentId, request.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentAttempt", "assessmentId", request.getAssessmentId()));

        if (attempt.isCompleted()) {
            return AssessmentAttemptResponse.fromEntity(attempt);
        }

        Assessment assessment = attempt.getAssessment();
        int totalScore = 0;

        // Ocijeni svako pitanje
        for (AssessmentQuestion question : assessment.getQuestions()) {
            String studentAnswer = request.getAnswers().get(question.getId().toString());
            if (studentAnswer == null) continue;

            int points = gradeQuestion(question, studentAnswer);
            totalScore += points;
        }

        // Spremi odgovore kao JSON
        try {
            attempt.setAnswers(objectMapper.writeValueAsString(request.getAnswers()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing answers: {}", e.getMessage());
        }

        attempt.setScore(totalScore);
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

    private int gradeQuestion(AssessmentQuestion question, String studentAnswer) {
        if (question.getQuestionType() == QuestionType.CODE) {
            return gradeCodeQuestion(question, studentAnswer);
        } else {
            // MULTIPLE_CHOICE ili TRUE_FALSE
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

            CodeExecutionRequest execRequest = CodeExecutionRequest.builder()
                    .code(code)
                    .language("C") // TODO: dinamički iz assessmenta
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

    private Assessment findAssessmentOrThrow(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id", id));
    }
}