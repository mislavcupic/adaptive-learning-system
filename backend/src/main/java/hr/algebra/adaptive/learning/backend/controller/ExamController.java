package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.dto.request.ExamAnswerRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.request.ExamRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamResponse;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final UserRepository userRepository;

    // ==================================================================
    // NASTAVNIK
    // ==================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ExamResponse>> create(
            @Valid @RequestBody ExamRequest request,
            Authentication authentication) {

        UUID userId = currentUserId(authentication);
        log.info("Kreiranje ispita: {}", request.getTitle());

        return ResponseEntity.ok(ApiResponse.success(
                "Ispit je kreiran.",
                examService.create(request, userId)));
    }

    @PutMapping("/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ExamResponse>> update(
            @PathVariable UUID examId,
            @Valid @RequestBody ExamRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "Ispit je ažuriran.",
                examService.update(examId, request)));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ApiResponse<ExamResponse>> getById(@PathVariable UUID examId) {
        return ResponseEntity.ok(ApiResponse.success(examService.getById(examId)));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getByCourse(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(examService.getByCourse(courseId)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getMyExams(
            Authentication authentication) {

        UUID userId = currentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(examService.getByTeacher(userId)));
    }

    @PatchMapping("/{examId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable UUID examId) {
        examService.publish(examId);
        return ResponseEntity.ok(ApiResponse.success("Ispit je objavljen.", null));
    }

    @PatchMapping("/{examId}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unpublish(@PathVariable UUID examId) {
        examService.unpublish(examId);
        return ResponseEntity.ok(ApiResponse.success("Ispit je povučen.", null));
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID examId) {
        examService.delete(examId);
        return ResponseEntity.ok(ApiResponse.success("Ispit je uklonjen.", null));
    }

    @GetMapping("/{examId}/attempts")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ExamAttemptResponse>>> getAttempts(
            @PathVariable UUID examId) {
        return ResponseEntity.ok(ApiResponse.success(examService.getAttemptsForExam(examId)));
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> getAttempt(
            @PathVariable UUID attemptId) {
        return ResponseEntity.ok(ApiResponse.success(examService.getAttempt(attemptId)));
    }

    /**
     * Rucno ocjenjivanje pojedinog odgovora. Koristi se za tekstualne
     * zadatke i ceklistu, koje sustav ne ocjenjuje automatski.
     */
    @PatchMapping("/attempts/{attemptId}/tasks/{taskId}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> gradeAnswer(
            @PathVariable UUID attemptId,
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        UUID teacherId = currentUserId(authentication);

        Integer score = body.get("score") != null
                ? ((Number) body.get("score")).intValue() : null;
        String feedback = (String) body.get("feedback");

        return ResponseEntity.ok(ApiResponse.success(
                "Ocjena je spremljena.",
                examService.gradeAnswer(attemptId, taskId, score, feedback, teacherId)));
    }

    @PatchMapping("/attempts/{attemptId}/finalize")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> finalizeReview(
            @PathVariable UUID attemptId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        UUID teacherId = currentUserId(authentication);
        String feedback = body != null ? body.get("feedback") : null;

        return ResponseEntity.ok(ApiResponse.success(
                "Pregled je dovršen.",
                examService.finalizeReview(attemptId, feedback, teacherId)));
    }

    // ==================================================================
    // STUDENT
    // ==================================================================

    @GetMapping("/available/{courseId}")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getAvailable(
            @PathVariable UUID courseId,
            Authentication authentication) {

        UUID studentId = currentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                examService.getAvailableForStudent(courseId, studentId)));
    }

    @PostMapping("/{examId}/start")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> start(
            @PathVariable UUID examId,
            Authentication authentication) {

        UUID studentId = currentUserId(authentication);
        log.info("Započinjanje ispita {} za korisnika {}", examId, studentId);

        return ResponseEntity.ok(ApiResponse.success(
                examService.startAttempt(examId, studentId)));
    }

    @GetMapping("/{examId}/my-attempt")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> getMyAttempt(
            @PathVariable UUID examId,
            Authentication authentication) {

        UUID studentId = currentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                examService.getMyAttempt(examId, studentId)));
    }

    /**
     * Spremanje odgovora na jedan zadatak dok je ispit otvoren.
     * Poziva se pri prelasku na sljedeci zadatak ili rucnim spremanjem.
     */
    @PutMapping("/{examId}/answer")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> saveAnswer(
            @PathVariable UUID examId,
            @Valid @RequestBody ExamAnswerRequest request,
            Authentication authentication) {

        UUID studentId = currentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                examService.saveAnswer(examId, request, studentId)));
    }

    @PostMapping("/{examId}/submit")
    public ResponseEntity<ApiResponse<ExamAttemptResponse>> submit(
            @PathVariable UUID examId,
            Authentication authentication) {

        UUID studentId = currentUserId(authentication);
        log.info("Predaja ispita {} od korisnika {}", examId, studentId);

        return ResponseEntity.ok(ApiResponse.success(
                "Ispit je predan.",
                examService.submitAttempt(examId, studentId)));
    }

    @GetMapping("/attempts/my")
    public ResponseEntity<ApiResponse<List<ExamAttemptResponse>>> getMyAttempts(
            Authentication authentication) {

        UUID studentId = currentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(examService.getMyAttempts(studentId)));
    }


    private UUID currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Prijavljeni korisnik nije pronađen: " + email))
                .getId();
    }
}