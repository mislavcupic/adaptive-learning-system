package hr.algebra.adaptive.learning.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hr.algebra.adaptive.learning.backend.domain.entity.Exam;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExamResponse {

    private UUID id;
    private String title;
    private String description;
    private String instructions;

    private UUID courseId;
    private String courseName;
    private String languageType;

    private String createdByName;

    private Integer timeLimitMinutes;
    private Integer passingScore;
    private Integer maxScore;
    private Integer taskCount;

    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    @JsonProperty("showTestResults")
    private boolean showTestResults;
    @JsonProperty("isPublished")
    private boolean isPublished;
    @JsonProperty("isActive")
    private boolean isActive;

    private LocalDateTime createdAt;

    /** Popunjava se samo kad se dohvaca pojedini ispit, ne u popisu. */
    private List<ExamTaskResponse> tasks;

    /** Stanje trenutnog studenta za ovaj ispit; prazno za nastavnika. */
    private String myAttemptStatus;
    private UUID myAttemptId;

    public static ExamResponse fromEntity(Exam exam) {
        return base(exam).build();
    }

    public static ExamResponse fromEntityWithTasks(Exam exam) {
        return base(exam)
                .tasks(exam.getTasks().stream()
                        .map(ExamTaskResponse::fromEntity)
                        .toList())
                .build();
    }

    private static ExamResponseBuilder base(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .instructions(exam.getInstructions())
                .courseId(exam.getCourse().getId())
                .courseName(exam.getCourse().getName())
                .languageType(exam.getCourse().getLanguageType() != null
                        ? exam.getCourse().getLanguageType().name() : null)
                .createdByName(exam.getCreatedBy() != null
                        ? exam.getCreatedBy().getFirstName() + " " + exam.getCreatedBy().getLastName()
                        : null)
                .timeLimitMinutes(exam.getTimeLimitMinutes())
                .passingScore(exam.getPassingScore())
                .maxScore(exam.getMaxScore())
                .taskCount(exam.getTasks() != null ? exam.getTasks().size() : 0)
                .availableFrom(exam.getAvailableFrom())
                .availableUntil(exam.getAvailableUntil())
                .showTestResults(exam.isShowTestResults())
                .isPublished(exam.isPublished())
                .isActive(exam.isActive())
                .createdAt(exam.getCreatedAt());
    }

    /**
     * Zadatak unutar ispita. Namjerno NE sadrzi rjesenje ni tocan
     * odgovor, jer ovaj DTO ide i studentu.
     */
    @Data
    @Builder
    public static class ExamTaskResponse {
        private UUID taskId;
        private String title;
        private String description;
        private String instructions;
        private TaskType taskType;
        private String options;
        private String starterCode;
        private Integer maxScore;
        private Integer timeLimitSeconds;
        private String languageType;
        private Integer orderIndex;

        public static ExamTaskResponse fromEntity(Task task) {
            return ExamTaskResponse.builder()
                    .taskId(task.getId())
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .instructions(task.getInstructions())
                    .taskType(task.getTaskType())
                    .options(task.getOptions())
                    .starterCode(task.getStarterCode())
                    .maxScore(task.getMaxScore())
                    .timeLimitSeconds(task.getTimeLimitSeconds())
                    .languageType(task.getOutcome() != null
                            && task.getOutcome().getCourse() != null
                            && task.getOutcome().getCourse().getLanguageType() != null
                            ? task.getOutcome().getCourse().getLanguageType().name()
                            : null)
                    .orderIndex(task.getOrderIndex())
                    .build();
        }
    }
}