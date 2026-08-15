package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByRoleAndIsActiveTrue(UserRole role);
    long countByRole(UserRole role);
    long countByRoleAndIsActiveTrue(UserRole role);
    long countByIsActiveTrue();


    @Query("SELECT COUNT(u) FROM User u WHERE u.schoolClass.id = :classId")
    long countStudentsByClassId(@Param("classId") UUID classId);

    List<User> findByRoleAndIsActiveFalse(UserRole role);
    long countByRoleAndIsActiveFalse(UserRole role);

    long countBySchoolClassIdAndResearchGroup(UUID schoolClassId, ResearchGroup researchGroup);

    // Nastavnik vidi samo one koji su POTVRDILI email.
    List<User> findByRoleAndIsActiveFalseAndEmailVerifiedTrue(UserRole role);
    long countByRoleAndIsActiveFalseAndEmailVerifiedTrue(UserRole role);

    Optional<User> findByVerificationToken(String verificationToken);
}