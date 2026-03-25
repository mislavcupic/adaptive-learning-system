package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SkillMasteryResponse;
import hr.algebra.adaptive.learning.backend.service.SkillMasteryService;
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
@RequestMapping("/api/mastery")
@RequiredArgsConstructor
public class SkillMasteryController {

    private final SkillMasteryService masteryService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<SkillMasteryResponse>>> getMyMastery(
            @AuthenticationPrincipal User user) {
        log.info("Getting skill mastery for student: {}", user.getEmail());
        List<SkillMasteryResponse> response = masteryService.getByStudent(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my/average")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Double>> getMyAverageMastery(
            @AuthenticationPrincipal User user) {
        log.info("Getting average mastery for student: {}", user.getEmail());
        double average = masteryService.getAverageMastery(user.getId());
        return ResponseEntity.ok(ApiResponse.success(average));
    }

    @GetMapping("/my/skill/{skillName}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SkillMasteryResponse>> getMySkillMastery(
            @AuthenticationPrincipal User user,
            @PathVariable String skillName) {
        log.info("Getting mastery for skill {} for student: {}", skillName, user.getEmail());
        SkillMasteryResponse response = masteryService.getByStudentAndSkill(user.getId(), skillName);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SkillMasteryResponse>>> getStudentMastery(
            @PathVariable UUID studentId) {
        log.info("Getting skill mastery for student: {}", studentId);
        List<SkillMasteryResponse> response = masteryService.getByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/student/{studentId}/average")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Double>> getStudentAverageMastery(
            @PathVariable UUID studentId) {
        log.info("Getting average mastery for student: {}", studentId);
        double average = masteryService.getAverageMastery(studentId);
        return ResponseEntity.ok(ApiResponse.success(average));
    }

    @GetMapping("/student/{studentId}/skill/{skillName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SkillMasteryResponse>> getStudentSkillMastery(
            @PathVariable UUID studentId,
            @PathVariable String skillName) {
        log.info("Getting mastery for skill {} for student: {}", skillName, studentId);
        SkillMasteryResponse response = masteryService.getByStudentAndSkill(studentId, skillName);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
