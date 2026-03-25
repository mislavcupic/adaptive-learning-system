package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.GroupType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import hr.algebra.adaptive.learning.backend.dto.response.StudentResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal User user) {
        log.info("Getting current user: {}", user.getEmail());
        UserResponse response = UserResponse.fromEntity(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateUserRequest request) {
        log.info("Updating current user: {}", user.getEmail());
        UserResponse response = userService.updateProfile(user.getId(), request.firstName(), request.lastName());
        return ResponseEntity.ok(ApiResponse.success("profile.updated", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) UserRole role) {
        log.info("Getting all users, role filter: {}", role);
        List<UserResponse> response = role != null
                ? userService.getByRole(role)
                : userService.getAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        log.info("Getting user: {}", id);
        UserResponse response = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getAllStudents(
            @RequestParam(required = false) GroupType groupType) {
        log.info("Getting all students, groupType filter: {}", groupType);
        List<StudentResponse> response = groupType != null
                ? userService.getStudentsByGroup(groupType)
                : userService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllTeachers() {
        log.info("Getting all teachers");
        List<UserResponse> response = userService.getAllTeachers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @RequestBody UpdateRoleRequest request) {
        log.info("Updating role for user: {} to {}", id, request.role());
        UserResponse response = userService.updateRole(id, request.role());
        return ResponseEntity.ok(ApiResponse.success("role.updated", response));
    }

    @PatchMapping("/{id}/group")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStudentGroup(
            @PathVariable UUID id,
            @RequestBody UpdateGroupRequest request) {
        log.info("Updating group for student: {} to {}", id, request.groupType());
        UserResponse response = userService.updateGroup(id, request.groupType());
        return ResponseEntity.ok(ApiResponse.success("group.updated", response));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID id) {
        log.info("Activating user: {}", id);
        userService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("user.activated", null));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable UUID id) {
        log.info("Deactivating user: {}", id);
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("user.deactivated", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("Deleting user: {}", id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record UpdateUserRequest(String firstName, String lastName) {}
    public record UpdateRoleRequest(UserRole role) {}
    public record UpdateGroupRequest(GroupType groupType) {}
}
