package hr.algebra.adaptive.learning.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TaskRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    private String starterCode;

    private String solutionCode;

    private String testCases;

    private String gradingCriteria;

    private Integer maxScore = 100;

    private Integer timeLimitSeconds = 30;

    private Integer memoryLimitMb = 256;

    @NotNull(message = "Learning outcome ID is required")
    private UUID outcomeId;

    private LocalDateTime dueDate;

    private Integer orderIndex = 0;
}