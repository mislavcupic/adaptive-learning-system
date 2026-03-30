package hr.algebra.adaptive.learning.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "school_classes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "academic_year"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    // Studenti u razredu (1 student = 1 razred)
    @OneToMany(mappedBy = "schoolClass", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> students = new HashSet<>();

    // Kolegiji dodijeljeni razredu
    @ManyToMany
    @JoinTable(
            name = "class_courses",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    private Set<Course> courses = new HashSet<>();

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}