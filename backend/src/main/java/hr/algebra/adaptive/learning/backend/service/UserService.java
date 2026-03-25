package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.domain.enums.GroupType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.StudentResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID id);

    List<UserResponse> getAll();

    List<UserResponse> getByRole(UserRole role);

    List<StudentResponse> getAllStudents();

    List<StudentResponse> getStudentsByGroup(GroupType groupType);

    List<UserResponse> getAllTeachers();

    UserResponse updateProfile(UUID id, String firstName, String lastName);

    UserResponse updateRole(UUID id, UserRole role);

    UserResponse updateGroup(UUID id, GroupType groupType);

    void activate(UUID id);

    void deactivate(UUID id);

    void delete(UUID id);
}
