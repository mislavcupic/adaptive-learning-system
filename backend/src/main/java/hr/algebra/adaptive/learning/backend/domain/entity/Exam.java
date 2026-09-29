package hr.algebra.adaptive.learning.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ispit kao skup zadataka koje student predaje kao cjelinu.
 *
 * Razlika u odnosu na Assessment: Assessment sluzi istrazivanju i sastoji
 * se od pitanja s automatskim ocjenjivanjem, dok se Exam sastoji od
 * postojecih zadataka i koristi se u redovitoj nastavi.
 */
@Entity
@Table(name = "exams")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Exam extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * Zadaci koji cine ispit. Veza vise-na-vise jer se isti zadatak moze
     * naci u vise ispita, a redoslijed se cuva u posebnom stupcu.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "exam_tasks",
            joinColumns = @JoinColumn(name = "exam_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    @OrderColumn(name = "order_index")
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ExamAttempt> attempts = new ArrayList<>();

    /** Vremensko ogranicenje u minutama; prazno znaci bez ogranicenja. */
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    /** Postotak bodova potreban za prolaz. */
    @Column(name = "passing_score")
    @Builder.Default
    private Integer passingScore = 50;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_until")
    private LocalDateTime availableUntil;

    /**
     * Smiju li studenti tijekom ispita vidjeti prolaze li testovi kod
     * programskih zadataka. AI feedback se u svakom slucaju daje tek
     * nakon predaje.
     */
    @Column(name = "show_test_results")
    @Builder.Default
    private boolean showTestResults = true;

    @Column(name = "is_published")
    @Builder.Default
    private boolean isPublished = false;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    /** Zbroj maksimalnih bodova svih zadataka u ispitu. */
    @Transient
    public int getMaxScore() {
        if (tasks == null) return 0;
        return tasks.stream()
                .mapToInt(t -> t.getMaxScore() != null ? t.getMaxScore() : 0)
                .sum();
    }
}
