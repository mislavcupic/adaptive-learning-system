package hr.algebra.adaptive.learning.backend.domain.entity;

import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Jedan studentov pokusaj rjesavanja ispita.
 *
 * Pokusaj se otvara kad student zapocne ispit i zakljucava se pri predaji.
 * Pojedinacni odgovori cuvaju se u ExamAnswer zapisima, pa student moze
 * prelaziti izmedu zadataka bez gubitka rada.
 */
@Entity
@Table(name = "exam_attempts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamAnswer> answers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExamAttemptStatus status = ExamAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Rok do kojeg predaja mora stici; racuna se iz vremenskog ogranicenja. */
    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** Je li pokusaj jos otvoren za izmjene. */
    @Transient
    public boolean isOpen() {
        return status == ExamAttemptStatus.IN_PROGRESS;
    }

    /** Je li rok istekao. */
    @Transient
    public boolean isExpired() {
        return deadlineAt != null && LocalDateTime.now().isAfter(deadlineAt);
    }

    @Transient
    public Double getPercentage() {
        if (totalScore == null || maxScore == null || maxScore == 0) return null;
        return Math.round(totalScore * 10000.0 / maxScore) / 100.0;
    }
}
