package hr.algebra.adaptive.learning.backend.controller;
import hr.algebra.adaptive.learning.backend.dto.request.ApproveRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.ResearchResultResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.service.ResearchResultService;
import hr.algebra.adaptive.learning.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherController {

    private final UserService userService;
    private final ResearchResultService researchResultService;

    //guest za odobrenje
    @GetMapping("/pending-registrations")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingRegistrations() {
        return ResponseEntity.ok(ApiResponse.success(userService.getPendingRegistrations()));
    }

    // Broj novih prijava (za badge)
    @GetMapping("/pending-registrations/count")
    public ResponseEntity<ApiResponse<Long>> getPendingCount() {
        return ResponseEntity.ok(ApiResponse.success(userService.getPendingRegistrationsCount()));
    }

    // Odobri korisnika i tip grupe (eksperimentalna ili kontrolna)
    @PutMapping("/approve/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.approveUser(
                        id,
                        request != null ? request.getSchoolClassId() : null,
                        request != null && request.isIncludeInResearch()
                )
        ));
    }

    // Odbij prijavu
    @DeleteMapping("/reject/{id}")
    public ResponseEntity<ApiResponse<String>> rejectUser(@PathVariable UUID id) {
        userService.rejectPendingUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Prijava odbijena"));
    }

    @GetMapping("/research-results")
    public ResponseEntity<ApiResponse<List<ResearchResultResponse>>> getResearchResults() {
        return ResponseEntity.ok(ApiResponse.success(researchResultService.getResults()));
    }
}
