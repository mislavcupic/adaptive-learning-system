package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.dto.request.LearningOutcomeRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.LearningOutcomeResponse;
import hr.algebra.adaptive.learning.backend.service.LearningOutcomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/outcomes")
@RequiredArgsConstructor
public class LearningOutcomeController {

    private final LearningOutcomeService outcomeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LearningOutcomeResponse>> create(
            @Valid @RequestBody LearningOutcomeRequest request) {
        log.info("Creating learning outcome: {}", request.getName());
        LearningOutcomeResponse response = outcomeService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("outcome.created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LearningOutcomeResponse>> getById(@PathVariable UUID id) {
        log.info("Getting learning outcome: {}", id);
        LearningOutcomeResponse response = outcomeService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<LearningOutcomeResponse>>> getByCourse(
            @PathVariable UUID courseId) {
        log.info("Getting learning outcomes for course: {}", courseId);
        List<LearningOutcomeResponse> response = outcomeService.getByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LearningOutcomeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LearningOutcomeRequest request) {
        log.info("Updating learning outcome: {}", id);
        LearningOutcomeResponse response = outcomeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("outcome.updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting learning outcome: {}", id);
        outcomeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/course/{courseId}/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> reorder(
            @PathVariable UUID courseId,
            @RequestBody List<UUID> orderedIds) {
        log.info("Reordering outcomes for course: {}", courseId);
        outcomeService.reorder(courseId, orderedIds);
        return ResponseEntity.ok(ApiResponse.success("outcomes.reordered", null));
    }
}
