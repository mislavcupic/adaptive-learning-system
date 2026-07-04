package hr.algebra.adaptive.learning.backend.dto.assessment;

import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import lombok.Data;

import java.util.UUID;

@Data
public class AssessmentRequest {
    private String title;
    private String description;
    private AssessmentType assessmentType;
    private UUID courseId;
    private Integer timeLimitMinutes;
    private Integer passingScore;
}