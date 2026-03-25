package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.request.SchoolClassRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SchoolClassResponse;
import hr.algebra.adaptive.learning.backend.dto.response.StudentResponse;
import hr.algebra.adaptive.learning.backend.service.SchoolClassService;
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
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService classService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> create(
            @Valid @RequestBody SchoolClassRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating class: {} by teacher: {}", request.getName(), user.getEmail());
        SchoolClassResponse response = classService.create(request, user.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("class.created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> getById(@PathVariable UUID id) {
        log.info("Getting class: {}", id);
        SchoolClassResponse response = classService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SchoolClassResponse>>> getAll() {
        log.info("Getting all classes");
        List<SchoolClassResponse> response = classService.getAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<SchoolClassResponse>>> getMyClasses(
            @AuthenticationPrincipal User user) {
        log.info("Getting classes for user: {}", user.getEmail());
        List<SchoolClassResponse> response;
        if (user.getRole().name().equals("TEACHER")) {
            response = classService.getByTeacher(user.getId());
        } else {
            response = classService.getByStudent(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SchoolClassResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SchoolClassRequest request) {
        log.info("Updating class: {}", id);
        SchoolClassResponse response = classService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("class.updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting class: {}", id);
        classService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{classId}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(@PathVariable UUID classId) {
        log.info("Getting students for class: {}", classId);
        List<StudentResponse> response = classService.getStudents(classId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{classId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> addStudent(
            @PathVariable UUID classId,
            @PathVariable UUID studentId) {
        log.info("Adding student {} to class {}", studentId, classId);
        classService.addStudent(classId, studentId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("student.added", null));
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> removeStudent(
            @PathVariable UUID classId,
            @PathVariable UUID studentId) {
        log.info("Removing student {} from class {}", studentId, classId);
        classService.removeStudent(classId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{classId}/courses/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> addCourse(
            @PathVariable UUID classId,
            @PathVariable UUID courseId) {
        log.info("Adding course {} to class {}", courseId, classId);
        classService.addCourse(classId, courseId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("course.added", null));
    }

    @DeleteMapping("/{classId}/courses/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> removeCourse(
            @PathVariable UUID classId,
            @PathVariable UUID courseId) {
        log.info("Removing course {} from class {}", courseId, classId);
        classService.removeCourse(classId, courseId);
        return ResponseEntity.noContent().build();
    }
}
