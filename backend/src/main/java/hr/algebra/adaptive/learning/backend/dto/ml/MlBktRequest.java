package hr.algebra.adaptive.learning.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlBktRequest {

    @JsonProperty("student_id")
    private UUID studentId;

    @JsonProperty("skill_name")
    private String skillName;

    @JsonProperty("is_correct")
    private boolean isCorrect;
}