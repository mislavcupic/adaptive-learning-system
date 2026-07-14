package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResponse {
    private UUID id;
    private String title;
    private String description;
    private String instructions;
    private TaskType taskType;
    private String options;
    private String starterCode;
    private String testCases;
    private String gradingCriteria;
    private Integer maxScore;
    private Integer timeLimitSeconds;
    private Integer memoryLimitMb;
    private UUID outcomeId;
    private String outcomeName;
    private UUID courseId;
    private String courseName;
    private String languageType;
    private LocalDateTime dueDate;
    private int submissionsCount;
    private boolean isActive;
    private Integer orderIndex;
    private LocalDateTime createdAt;

    public static TaskResponse fromEntity(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .instructions(task.getInstructions())
                .taskType(task.getTaskType())
                .options(task.getOptions())
                .starterCode(task.getStarterCode())
                .testCases(task.getTestCases())
                .gradingCriteria(task.getGradingCriteria())
                .maxScore(task.getMaxScore())
                .timeLimitSeconds(task.getTimeLimitSeconds())
                .memoryLimitMb(task.getMemoryLimitMb())
                .outcomeId(task.getOutcome().getId())
                .outcomeName(task.getOutcome().getName())
                .courseId(task.getOutcome().getCourse().getId())
                .courseName(task.getOutcome().getCourse().getName())
                .languageType(task.getOutcome().getCourse().getLanguageType().name())
                .dueDate(task.getDueDate())
                .submissionsCount(task.getSubmissions() != null ? task.getSubmissions().size() : 0)
                .isActive(task.isActive())
                .orderIndex(task.getOrderIndex())
                .createdAt(task.getCreatedAt())
                .build();
    }
}