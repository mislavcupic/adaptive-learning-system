package hr.algebra.adaptive.learning.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LearningOutcomeRequest {

    @NotBlank(message = "Outcome name is required")
    private String name;

    private String description;

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    private Integer orderIndex = 0;
}