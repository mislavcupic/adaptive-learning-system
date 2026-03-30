package hr.algebra.adaptive.learning.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.GroupType;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StudentResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private GroupType groupType;
    @JsonProperty("isActive")
    private boolean isActive;
    private Double averageMastery;
    private Long submissionsCount;
    private String schoolClassName;
    private ResearchGroup researchGroup;

    public static StudentResponse fromEntity(User user) {
        return StudentResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .groupType(user.getGroupType())
                .isActive(user.isActive())
                .schoolClassName(user.getSchoolClass() != null ? user.getSchoolClass().getName() : null)
                .researchGroup(user.getResearchGroup())
                .build();
    }

    public static StudentResponse fromEntity(User user, Double avgMastery, Long submissions) {
        return StudentResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .groupType(user.getGroupType())
                .isActive(user.isActive())
                .averageMastery(avgMastery)
                .submissionsCount(submissions)
                .schoolClassName(user.getSchoolClass() != null ? user.getSchoolClass().getName() : null)
                .researchGroup(user.getResearchGroup())
                .build();
    }
}