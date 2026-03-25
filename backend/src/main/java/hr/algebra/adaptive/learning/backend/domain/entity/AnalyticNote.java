package hr.algebra.adaptive.learning.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "analytic_notes")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String insight;

    @Column(name = "skill_name")
    private String skillName;

    @Column(name = "note_type")
    private String noteType; // ERROR_PATTERN, IMPROVEMENT, STRENGTH, etc.

    // PGVector embedding - za sada TEXT, kasnije VECTOR(1536)
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;
}
