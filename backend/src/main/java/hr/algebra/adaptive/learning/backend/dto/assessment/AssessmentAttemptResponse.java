package hr.algebra.adaptive.learning.backend.dto.assessment;

import hr.algebra.adaptive.learning.backend.domain.entity.AssessmentAttempt;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AssessmentAttemptResponse {
    private UUID id;
    private UUID assessmentId;
    private String assessmentTitle;
    private AssessmentType assessmentType;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer score;
    private Integer maxScore;
    private Double percentage;
    private boolean isCompleted;
    private boolean passed;

    public static AssessmentAttemptResponse fromEntity(AssessmentAttempt entity) {
        int passingScore = entity.getAssessment().getPassingScore() != null
                ? entity.getAssessment().getPassingScore() : 50;
        double percentage = entity.getMaxScore() > 0
                ? (entity.getScore() * 100.0 / entity.getMaxScore()) : 0;

        return AssessmentAttemptResponse.builder()
                .id(entity.getId())
                .assessmentId(entity.getAssessment().getId())
                .assessmentTitle(entity.getAssessment().getTitle())
                .assessmentType(entity.getAssessment().getAssessmentType())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .score(entity.getScore())
                .maxScore(entity.getMaxScore())
                .percentage(percentage)
                .isCompleted(entity.isCompleted())
                .passed(percentage >= passingScore)
                .build();
    }
}
