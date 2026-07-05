package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ResearchResultResponse {
    private UUID studentId;
    private String firstName;
    private String lastName;
    private String email;
    private ResearchGroup researchGroup;

    private Integer pretestScore;
    private Integer pretestMaxScore;
    private Double pretestPercentage;

    private Integer posttestScore;
    private Integer posttestMaxScore;
    private Double posttestPercentage;
}
