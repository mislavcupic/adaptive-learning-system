package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.request.ExamAnswerRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.request.ExamRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamResponse;

import java.util.List;
import java.util.UUID;

public interface ExamService {

    // ---------- Nastavnik ----------

    ExamResponse create(ExamRequest request, UUID createdById);

    ExamResponse update(UUID examId, ExamRequest request);

    ExamResponse getById(UUID examId);

    List<ExamResponse> getByCourse(UUID courseId);

    List<ExamResponse> getByTeacher(UUID teacherId);

    void publish(UUID examId);

    void unpublish(UUID examId);

    void delete(UUID examId);

    List<ExamAttemptResponse> getAttemptsForExam(UUID examId);

    ExamAttemptResponse getAttempt(UUID attemptId);

    ExamAttemptResponse gradeAnswer(UUID attemptId, UUID taskId,
                                    Integer score, String feedback, UUID teacherId);

    ExamAttemptResponse finalizeReview(UUID attemptId, String feedback, UUID teacherId);

    // ---------- Student ----------

    List<ExamResponse> getAvailableForStudent(UUID courseId, UUID studentId);

    ExamAttemptResponse startAttempt(UUID examId, UUID studentId);

    ExamAttemptResponse getMyAttempt(UUID examId, UUID studentId);

    ExamAttemptResponse saveAnswer(UUID examId, ExamAnswerRequest request, UUID studentId);

    ExamAttemptResponse submitAttempt(UUID examId, UUID studentId);

    List<ExamAttemptResponse> getMyAttempts(UUID studentId);
}