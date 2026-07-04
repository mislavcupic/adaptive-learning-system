package hr.algebra.adaptive.learning.backend.domain.entity;

import hr.algebra.adaptive.learning.backend.domain.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_questions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON za multiple choice: ["A", "B", "C", "D"]

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "code_template", columnDefinition = "TEXT")
    private String codeTemplate; // Za CODE pitanja

    @Column(name = "test_cases", columnDefinition = "TEXT")
    private String testCases; // JSON za CODE pitanja

    @Builder.Default
    private Integer points = 1;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;
}