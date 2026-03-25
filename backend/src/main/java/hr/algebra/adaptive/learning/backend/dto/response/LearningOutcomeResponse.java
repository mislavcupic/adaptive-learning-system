package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LearningOutcomeResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer orderIndex;
    private UUID courseId;
    private String courseName;
    private int tasksCount;

    public static LearningOutcomeResponse fromEntity(LearningOutcome outcome) {
        return LearningOutcomeResponse.builder()
                .id(outcome.getId())
                .name(outcome.getName())
                .description(outcome.getDescription())
                .orderIndex(outcome.getOrderIndex())
                .courseId(outcome.getCourse().getId())
                .courseName(outcome.getCourse().getName())
                .tasksCount(outcome.getTasks() != null ? outcome.getTasks().size() : 0)
                .build();
    }
}