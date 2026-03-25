package hr.algebra.adaptive.learning.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Task extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "solution_code", columnDefinition = "TEXT")
    private String solutionCode;

    @Column(name = "test_cases", columnDefinition = "TEXT")
    private String testCases; // JSON format

    @Column(name = "grading_criteria", columnDefinition = "TEXT")
    private String gradingCriteria;

    @Column(name = "max_score")
    @Builder.Default
    private Integer maxScore = 100;

    @Column(name = "time_limit_seconds")
    @Builder.Default
    private Integer timeLimitSeconds = 30;

    @Column(name = "memory_limit_mb")
    @Builder.Default
    private Integer memoryLimitMb = 256;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outcome_id", nullable = false)
    private LearningOutcome outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Submission> submissions = new ArrayList<>();

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;
}