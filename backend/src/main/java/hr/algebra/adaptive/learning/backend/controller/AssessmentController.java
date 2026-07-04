package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.assessment.*;
import hr.algebra.adaptive.learning.backend.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    // ==================== TEACHER ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<AssessmentResponse> create(
            @RequestBody AssessmentRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating assessment: {}", request.getTitle());
        return ResponseEntity.ok(assessmentService.create(request, user.getId()));
    }

    @PostMapping("/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<AssessmentResponse> addQuestion(@RequestBody QuestionRequest request) {
        log.info("Adding question to assessment: {}", request.getAssessmentId());
        return ResponseEntity.ok(assessmentService.addQuestion(request));
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID questionId) {
        log.info("Deleting question: {}", questionId);
        assessmentService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== COMMON ENDPOINTS ====================

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(assessmentService.getByIdWithQuestions(id));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AssessmentResponse>> getByCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(assessmentService.getByCourse(courseId));
    }

    // ==================== STUDENT ENDPOINTS ====================

    @PostMapping("/{assessmentId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssessmentAttemptResponse> startAttempt(
            @PathVariable UUID assessmentId,
            @AuthenticationPrincipal User user) {
        log.info("Student {} starting assessment {}", user.getEmail(), assessmentId);
        return ResponseEntity.ok(assessmentService.startAttempt(assessmentId, user.getId()));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssessmentAttemptResponse> submitAttempt(
            @RequestBody AssessmentAttemptRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Student {} submitting assessment {}", user.getEmail(), request.getAssessmentId());
        return ResponseEntity.ok(assessmentService.submitAttempt(request, user.getId()));
    }

    @GetMapping("/attempts/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AssessmentAttemptResponse>> getMyAttempts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.getStudentAttempts(user.getId()));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AssessmentAttemptResponse> getAttempt(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(assessmentService.getAttempt(attemptId));
    }

    @GetMapping("/check/pretest/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Boolean> hasCompletedPretest(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.hasCompletedPretest(user.getId(), courseId));
    }

    @GetMapping("/check/posttest/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Boolean> hasCompletedPosttest(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.hasCompletedPosttest(user.getId(), courseId));
    }
}