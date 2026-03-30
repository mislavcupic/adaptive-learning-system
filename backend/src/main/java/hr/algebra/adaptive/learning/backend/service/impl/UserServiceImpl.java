package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.GroupType;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.StudentResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.SchoolClassRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.ResearchGroupService;
import hr.algebra.adaptive.learning.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ResearchGroupService researchGroupService;

    @Override
    public UserResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    public List<UserResponse> getByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    public List<StudentResponse> getAllStudents() {
        return userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT).stream()
                .map(StudentResponse::fromEntity)
                .toList();
    }

    @Override
    public List<StudentResponse> getStudentsByGroup(GroupType groupType) {
        return userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT).stream()
                .filter(s -> s.getGroupType() == groupType)
                .map(StudentResponse::fromEntity)
                .toList();
    }

    @Override
    public List<UserResponse> getAllTeachers() {
        return userRepository.findByRoleAndIsActiveTrue(UserRole.TEACHER).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID id, String firstName, String lastName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse updateRole(UUID id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setRole(role);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID id, boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setActive(isActive);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse updateGroup(UUID id, GroupType groupType) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setGroupType(groupType);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void activate(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
    }
    @Override
    public List<UserResponse> getPendingRegistrations() {
        return userRepository.findByRoleAndIsActiveFalse(UserRole.GUEST).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    public long getPendingRegistrationsCount() {
        return userRepository.countByRoleAndIsActiveFalse(UserRole.GUEST);
    }

    @Transactional
    @Override
    public UserResponse approveUser(UUID id, UUID schoolClassId, boolean includeInResearch) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getRole() != UserRole.GUEST) {
            throw new BadRequestException("Korisnik nije GUEST");
        }

        user.setRole(UserRole.STUDENT);
        user.setActive(true);

        if (schoolClassId != null) {
            SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
                    .orElseThrow(() -> new ResourceNotFoundException("SchoolClass not found with id: " + schoolClassId));
            user.setSchoolClass(schoolClass);
        }

        if (includeInResearch && schoolClassId != null) {
            user.setResearchGroup(researchGroupService.assignGroup(schoolClassId));
        } else {
            user.setResearchGroup(ResearchGroup.NOT_ASSIGNED);
        }

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void rejectPendingUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getRole() != UserRole.GUEST) {
            throw new BadRequestException("Korisnik nije GUEST");
        }

        userRepository.delete(user);
    }
}