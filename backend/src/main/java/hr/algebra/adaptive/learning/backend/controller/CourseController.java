package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.dto.request.CourseRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.CourseResponse;
import hr.algebra.adaptive.learning.backend.dto.response.LearningOutcomeResponse;
import hr.algebra.adaptive.learning.backend.service.CourseService;
import hr.algebra.adaptive.learning.backend.service.LearningOutcomeService;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final LearningOutcomeService outcomeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CourseRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating course: {} by user: {}", request.getName(), user.getEmail());
        CourseResponse response = courseService.create(request, user.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("course.created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Getting course: {}", id);
        checkTeacherAccess(id, user);
        CourseResponse response = courseService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAll(
            @RequestParam(required = false) LanguageType languageType,
            @AuthenticationPrincipal User user) {
        log.info("Getting all courses, languageType filter: {}", languageType);
        if (isTeacher(user)) {
            List<CourseResponse> own = languageType != null
                    ? courseService.getByTeacherAndLanguageType(user.getId(), languageType)
                    : courseService.getByTeacher(user.getId());
            return ResponseEntity.ok(ApiResponse.success(own));
        }
        List<CourseResponse> response = languageType != null
                ? courseService.getByLanguageType(languageType)
                : courseService.getAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getMyCourses(
            @AuthenticationPrincipal User user) {
        log.info("Getting courses for teacher: {}", user.getEmail());
        List<CourseResponse> response = courseService.getByTeacher(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CourseRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Updating course: {}", id);
        checkTeacherAccess(id, user);
        CourseResponse response = courseService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("course.updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting course: {}", id);
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Activating course: {}", id);
        checkTeacherAccess(id, user);
        courseService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("course.activated", null));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Deactivating course: {}", id);
        checkTeacherAccess(id, user);
        courseService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("course.deactivated", null));
    }

    @GetMapping("/{courseId}/outcomes")
    public ResponseEntity<ApiResponse<List<LearningOutcomeResponse>>> getOutcomes(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal User user) {
        log.info("Getting outcomes for course: {}", courseId);
        checkTeacherAccess(courseId, user);
        List<LearningOutcomeResponse> response = outcomeService.getByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private boolean isTeacher(User user) {
        return user != null && user.getRole() == UserRole.TEACHER;
    }

    private void checkTeacherAccess(UUID courseId, User user) {
        if (isTeacher(user) && !courseService.isOwner(courseId, user.getId())) {
            throw new ResourceNotFoundException("Course", "id", courseId);
        }
    }
}