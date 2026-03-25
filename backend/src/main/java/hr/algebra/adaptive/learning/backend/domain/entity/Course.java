package hr.algebra.adaptive.learning.backend.domain.entity;

import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_type", nullable = false)
    private LanguageType languageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LearningOutcome> outcomes = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}
