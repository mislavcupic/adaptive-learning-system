package hr.algebra.adaptive.learning.backend.dto.assessment;

import hr.algebra.adaptive.learning.backend.domain.entity.AssessmentQuestion;
import hr.algebra.adaptive.learning.backend.domain.enums.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class QuestionResponse {
    private UUID id;
    private String questionText;
    private QuestionType questionType;
    private String options;        // JSON array za MULTIPLE_CHOICE
    private String codeTemplate;   // Za CODE pitanja
    private Integer points;
    private Integer orderIndex;
    // NE vraćamo correctAnswer studentu!

    public static QuestionResponse fromEntity(AssessmentQuestion entity) {
        return QuestionResponse.builder()
                .id(entity.getId())
                .questionText(entity.getQuestionText())
                .questionType(entity.getQuestionType())
                .options(entity.getOptions())
                .codeTemplate(entity.getCodeTemplate())
                .points(entity.getPoints())
                .orderIndex(entity.getOrderIndex())
                .build();
    }
}