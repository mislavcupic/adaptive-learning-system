package hr.algebra.adaptive.learning.backend.domain.entity;

import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "submitted_code", columnDefinition = "TEXT", nullable = false)
    private String submittedCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "compiler_output", columnDefinition = "TEXT")
    private String compilerOutput;

    @Column(name = "execution_output", columnDefinition = "TEXT")
    private String executionOutput;

    @Column(name = "test_results", columnDefinition = "TEXT")
    private String testResults; // JSON format

    @Column(name = "valgrind_output", columnDefinition = "TEXT")
    private String valgrindOutput;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "memory_used_kb")
    private Long memoryUsedKb;

    // AI Feedback (GPT-4o-mini)
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "ai_score")
    private Integer aiScore;

    // Teacher Feedback
    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    @Column(name = "teacher_score")
    private Integer teacherScore;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "tests_passed")
    @Builder.Default
    private Integer testsPassed = 0;

    @Column(name = "tests_total")
    @Builder.Default
    private Integer testsTotal = 0;
}
