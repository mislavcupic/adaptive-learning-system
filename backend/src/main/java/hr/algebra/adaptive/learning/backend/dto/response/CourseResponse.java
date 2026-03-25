package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CourseResponse {
    private UUID id;
    private String name;
    private String description;
    private LanguageType languageType;
    private String createdByName;
    private int outcomesCount;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourseResponse fromEntity(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .languageType(course.getLanguageType())
                .createdByName(course.getCreatedBy() != null
                        ? course.getCreatedBy().getFirstName() + " " + course.getCreatedBy().getLastName()
                        : null)
                .outcomesCount(course.getOutcomes() != null ? course.getOutcomes().size() : 0)
                .isActive(course.isActive())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
