package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.assessment.*;

import java.util.List;
import java.util.UUID;

public interface AssessmentService {

    AssessmentResponse create(AssessmentRequest request, UUID createdById);

    AssessmentResponse getById(UUID id);

    AssessmentResponse getByIdWithQuestions(UUID id);

    List<AssessmentResponse> getByCourse(UUID courseId);

    AssessmentResponse addQuestion(QuestionRequest request);

    void deleteQuestion(UUID questionId);

    AssessmentAttemptResponse startAttempt(UUID assessmentId, UUID studentId);

    AssessmentAttemptResponse submitAttempt(AssessmentAttemptRequest request, UUID studentId);

    AssessmentAttemptResponse getAttempt(UUID attemptId);

    List<AssessmentAttemptResponse> getStudentAttempts(UUID studentId);

    boolean hasCompletedPretest(UUID studentId, UUID courseId);

    boolean hasCompletedPosttest(UUID studentId, UUID courseId);
}
