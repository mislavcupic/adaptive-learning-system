package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.dto.request.TaskRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.TaskResponse;
import hr.algebra.adaptive.learning.backend.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating task: {} by user: {}", request.getTitle(), user.getEmail());
        TaskResponse response = taskService.create(request, user.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("task.created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Getting task: {}", id);
        checkTeacherAccess(id, user);
        TaskResponse response = taskService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAll(
            @RequestParam(required = false) UUID outcomeId,
            @AuthenticationPrincipal User user) {
        log.info("Getting tasks, outcomeId filter: {}", outcomeId);
        if (isTeacher(user)) {
            List<TaskResponse> own = outcomeId != null
                    ? taskService.getByOutcomeForCourseOwner(outcomeId, user.getId())
                    : taskService.getByCourseOwner(user.getId());
            return ResponseEntity.ok(ApiResponse.success(own));
        }
        List<TaskResponse> response = outcomeId != null
                ? taskService.getByOutcome(outcomeId)
                : taskService.getAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getMyTasks(
            @AuthenticationPrincipal User user) {
        log.info("Getting tasks for teacher: {}", user.getEmail());
        List<TaskResponse> response = taskService.getByTeacher(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getPendingTasks(
            @AuthenticationPrincipal User user) {
        log.info("Getting pending tasks for student: {}", user.getEmail());
        List<TaskResponse> response = taskService.getPendingForStudent(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/outcome/{outcomeId}")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getByOutcome(
            @PathVariable UUID outcomeId,
            @AuthenticationPrincipal User user) {
        log.info("Getting tasks for outcome: {}", outcomeId);
        List<TaskResponse> response = isTeacher(user)
                ? taskService.getByOutcomeForCourseOwner(outcomeId, user.getId())
                : taskService.getByOutcome(outcomeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Updating task: {}", id);
        checkTeacherAccess(id, user);
        TaskResponse response = taskService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("task.updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Deleting task: {}", id);
        checkTeacherAccess(id, user);
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<TaskResponse>> duplicate(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Duplicating task: {} by user: {}", id, user.getEmail());
        checkTeacherAccess(id, user);
        TaskResponse response = taskService.duplicate(id, user.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("task.duplicated", response));
    }

    private boolean isTeacher(User user) {
        return user != null && user.getRole() == UserRole.TEACHER;
    }

    private void checkTeacherAccess(UUID taskId, User user) {
        if (isTeacher(user) && !taskService.isInOwnCourse(taskId, user.getId())) {
            throw new ResourceNotFoundException("Task", "id", taskId);
        }
    }
}