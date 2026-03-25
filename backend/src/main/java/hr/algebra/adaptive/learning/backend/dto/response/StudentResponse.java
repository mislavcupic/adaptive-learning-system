package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.GroupType;
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
    private boolean isActive;
    private Double averageMastery;
    private Long submissionsCount;

    public static StudentResponse fromEntity(User user) {
        return StudentResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .groupType(user.getGroupType())
                .isActive(user.isActive())
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
                .build();
    }
}