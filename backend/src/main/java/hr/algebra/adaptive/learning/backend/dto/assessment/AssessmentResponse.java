package hr.algebra.adaptive.learning.backend.dto.assessment;

import hr.algebra.adaptive.learning.backend.domain.entity.Assessment;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AssessmentResponse {
    private UUID id;
    private String title;
    private String description;
    private AssessmentType assessmentType;
    private UUID courseId;
    private String courseName;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private Integer questionCount;
    private List<QuestionResponse> questions;

    public static AssessmentResponse fromEntity(Assessment entity) {
        return AssessmentResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .assessmentType(entity.getAssessmentType())
                .courseId(entity.getCourse().getId())
                .courseName(entity.getCourse().getName())
                .timeLimitMinutes(entity.getTimeLimitMinutes())
                .passingScore(entity.getPassingScore())
                .questionCount(entity.getQuestions().size())
                .build();
    }

    public static AssessmentResponse fromEntityWithQuestions(Assessment entity) {
        return AssessmentResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .assessmentType(entity.getAssessmentType())
                .courseId(entity.getCourse().getId())
                .courseName(entity.getCourse().getName())
                .timeLimitMinutes(entity.getTimeLimitMinutes())
                .passingScore(entity.getPassingScore())
                .questionCount(entity.getQuestions().size())
                .questions(entity.getQuestions().stream()
                        .map(QuestionResponse::fromEntity)
                        .toList())
                .build();
    }
}