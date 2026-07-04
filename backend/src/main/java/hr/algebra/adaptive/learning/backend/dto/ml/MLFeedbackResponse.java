package hr.algebra.adaptive.learning.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLFeedbackResponse {
    @JsonProperty("submission_id")
    private String submissionId;

    @JsonProperty("ai_feedback")
    private String aiFeedback;

    @JsonProperty("ai_score")
    private int aiScore;

    @JsonProperty("skills_updated")
    private List<String> skillsUpdated;
}