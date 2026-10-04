package hr.algebra.adaptive.learning.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlBktResponse {

    @JsonProperty("student_id")
    private UUID studentId;

    @JsonProperty("skill_name")
    private String skillName;

    @JsonProperty("mastery_level")
    private Double masteryLevel;

    @JsonProperty("previous_level")
    private Double previousLevel;
}