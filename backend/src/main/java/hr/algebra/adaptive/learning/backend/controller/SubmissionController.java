package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.request.SubmissionRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.PaginatedResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SubmissionResponse;
import hr.algebra.adaptive.learning.backend.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Student {} submitting code for task {}", user.getEmail(), request.getTaskId());
        SubmissionResponse response = submissionService.submit(request, user.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("submission.created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getById(@PathVariable UUID id) {
        log.info("Getting submission: {}", id);
        SubmissionResponse response = submissionService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getMySubmissions(
            @AuthenticationPrincipal User user) {
        log.info("Getting submissions for student: {}", user.getEmail());
        List<SubmissionResponse> response = submissionService.getByStudent(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my/paginated")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PaginatedResponse<SubmissionResponse>>> getMySubmissionsPaginated(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting paginated submissions for student: {}", user.getEmail());
        PaginatedResponse<SubmissionResponse> response = submissionService.getByStudentPaginated(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getByStudent(
            @PathVariable UUID studentId) {
        log.info("Getting submissions for student: {}", studentId);
        List<SubmissionResponse> response = submissionService.getByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PaginatedResponse<SubmissionResponse>>> getByStudentPaginated(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting paginated submissions for student: {}", studentId);
        PaginatedResponse<SubmissionResponse> response = submissionService.getByStudentPaginated(studentId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getByTask(
            @PathVariable UUID taskId) {
        log.info("Getting submissions for task: {}", taskId);
        List<SubmissionResponse> response = submissionService.getByTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/task/{taskId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PaginatedResponse<SubmissionResponse>>> getByTaskPaginated(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting paginated submissions for task: {}", taskId);
        PaginatedResponse<SubmissionResponse> response = submissionService.getByTaskPaginated(taskId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my/count")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Long>> getMySubmissionsCount(
            @AuthenticationPrincipal User user) {
        log.info("Getting submission count for student: {}", user.getEmail());
        long count = submissionService.countByStudent(user.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/task/{taskId}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Long>> getTaskSubmissionsCount(
            @PathVariable UUID taskId) {
        log.info("Getting submission count for task: {}", taskId);
        long count = submissionService.countByTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PatchMapping("/{id}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> addTeacherFeedback(
            @PathVariable UUID id,
            @RequestBody TeacherFeedbackRequest request) {
        log.info("Adding teacher feedback to submission: {}", id);
        SubmissionResponse response = submissionService.addTeacherFeedback(id, request.feedback(), request.score());
        return ResponseEntity.ok(ApiResponse.success("feedback.added", response));
    }

    public record TeacherFeedbackRequest(String feedback, Integer score) {}
}
