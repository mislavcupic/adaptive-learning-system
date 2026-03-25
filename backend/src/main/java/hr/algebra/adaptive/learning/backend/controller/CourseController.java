package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
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
    public ResponseEntity<ApiResponse<CourseResponse>> getById(@PathVariable UUID id) {
        log.info("Getting course: {}", id);
        CourseResponse response = courseService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAll(
            @RequestParam(required = false) LanguageType languageType) {
        log.info("Getting all courses, languageType filter: {}", languageType);
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
            @Valid @RequestBody CourseRequest request) {
        log.info("Updating course: {}", id);
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
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable UUID id) {
        log.info("Activating course: {}", id);
        courseService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("course.activated", null));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        log.info("Deactivating course: {}", id);
        courseService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("course.deactivated", null));
    }

    @GetMapping("/{courseId}/outcomes")
    public ResponseEntity<ApiResponse<List<LearningOutcomeResponse>>> getOutcomes(
            @PathVariable UUID courseId) {
        log.info("Getting outcomes for course: {}", courseId);
        List<LearningOutcomeResponse> response = outcomeService.getByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}