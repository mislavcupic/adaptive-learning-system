package hr.algebra.adaptive.learning.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Odgovor na jedan zadatak unutar ispita.
 *
 * Postoji zasebno od Submission jer se odgovor sprema i dok je ispit
 * jos otvoren, a ocjenjuje se tek pri predaji. Kod programskih zadataka
 * cuva se i veza prema Submission zapisu koji nosi rezultate izvrsavanja.
 */
@Entity
@Table(
        name = "exam_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_answer_attempt_task",
                columnNames = {"attempt_id", "task_id"}
        )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * Sadrzaj odgovora, ovisno o tipu zadatka:
     * CODE            - izvorni kod
     * TEXT            - tekst odgovora
     * MULTIPLE_CHOICE - odabrana opcija
     * CHECKLIST       - JSON popis odabranih opcija
     */
    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    /**
     * Za programske zadatke: veza prema predaji koja nosi rezultate
     * prevodenja, testova i kasnije AI povratnu informaciju.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "teacher_score")
    private Integer teacherScore;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    /** Vrijeme posljednje izmjene odgovora dok je ispit bio otvoren. */
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    /** Je li student oznacio zadatak kao dovrsen. */
    @Column(name = "is_marked_done")
    @Builder.Default
    private boolean markedDone = false;

    @Transient
    public Integer getEffectiveScore() {
        if (finalScore != null) return finalScore;
        if (teacherScore != null) return teacherScore;
        return aiScore;
    }
}