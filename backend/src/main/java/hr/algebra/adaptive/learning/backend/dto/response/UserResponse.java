package hr.algebra.adaptive.learning.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.GroupType;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private GroupType groupType;
    private String schoolClassName;
    private String schoolClassId;
    @JsonProperty("isActive")
    private boolean isActive;
    private LocalDateTime createdAt;
    private ResearchGroup researchGroup;

    public static UserResponse fromEntity(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole())
                    .isActive(user.isActive())
                    .schoolClassId(user.getSchoolClass() != null ? user.getSchoolClass().getId().toString() : null)
                    .schoolClassName(user.getSchoolClass() != null ? user.getSchoolClass().getName() : null)
                    .createdAt(user.getCreatedAt())
                    .researchGroup(user.getResearchGroup())
                    .build();
        }
}