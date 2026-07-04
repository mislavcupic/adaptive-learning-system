package hr.algebra.adaptive.learning.backend.dto.assessment;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class AssessmentAttemptRequest {
    private UUID assessmentId;
    private Map<String, String> answers;
}
