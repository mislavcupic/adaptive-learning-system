package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.SkillMastery;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SkillMasteryResponse {
    private UUID id;
    private String skillName;
    private double masteryLevel;
    private int attemptsCount;
    private int correctCount;
    private String outcomeName;

    public static SkillMasteryResponse fromEntity(SkillMastery mastery) {
        return SkillMasteryResponse.builder()
                .id(mastery.getId())
                .skillName(mastery.getSkillName())
                .masteryLevel(mastery.getMasteryLevel())
                .attemptsCount(mastery.getAttemptsCount())
                .correctCount(mastery.getCorrectCount())
                .outcomeName(mastery.getOutcome() != null ? mastery.getOutcome().getName() : null)
                .build();
    }
}
