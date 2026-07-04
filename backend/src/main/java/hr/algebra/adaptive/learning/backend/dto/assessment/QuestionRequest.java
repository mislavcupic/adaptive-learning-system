package hr.algebra.adaptive.learning.backend.dto.assessment;

import hr.algebra.adaptive.learning.backend.domain.enums.QuestionType;
import lombok.Data;

import java.util.UUID;

@Data
public class QuestionRequest {
    private UUID assessmentId;
    private String questionText;
    private QuestionType questionType;
    private String options;
    private String correctAnswer;
    private String codeTemplate;
    private String testCases;
    private Integer points;
    private Integer orderIndex;
}
