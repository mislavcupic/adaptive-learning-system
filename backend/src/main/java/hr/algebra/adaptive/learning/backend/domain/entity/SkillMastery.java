package hr.algebra.adaptive.learning.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "skill_mastery", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "skill_name"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMastery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    // BKT Parameters
    @Column(name = "mastery_level", nullable = false)
    @Builder.Default
    private Double masteryLevel = 0.3; // P(L0) - prior knowledge

    @Column(name = "p_transit")
    @Builder.Default
    private Double pTransit = 0.2; // P(T) - probability of learning

    @Column(name = "p_guess")
    @Builder.Default
    private Double pGuess = 0.25; // P(G) - probability of guessing

    @Column(name = "p_slip")
    @Builder.Default
    private Double pSlip = 0.1; // P(S) - probability of slip

    @Column(name = "attempts_count")
    @Builder.Default
    private Integer attemptsCount = 0;

    @Column(name = "correct_count")
    @Builder.Default
    private Integer correctCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outcome_id")
    private LearningOutcome outcome;
}
