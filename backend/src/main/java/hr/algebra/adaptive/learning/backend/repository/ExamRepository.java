package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {

    List<Exam> findByCourseIdAndIsActiveTrueOrderByCreatedAtDesc(UUID courseId);

    List<Exam> findByCreatedByIdAndIsActiveTrueOrderByCreatedAtDesc(UUID teacherId);

    /**
     * Ispiti koje student smije vidjeti: objavljeni, aktivni i unutar
     * razdoblja dostupnosti ako je ono postavljeno.
     */
    @Query("""
            SELECT e FROM Exam e
            WHERE e.course.id = :courseId
              AND e.isPublished = true
              AND e.isActive = true
              AND (e.availableFrom IS NULL OR e.availableFrom <= CURRENT_TIMESTAMP)
              AND (e.availableUntil IS NULL OR e.availableUntil >= CURRENT_TIMESTAMP)
            ORDER BY e.availableFrom ASC NULLS LAST, e.createdAt DESC
            """)
    List<Exam> findAvailableForStudents(@Param("courseId") UUID courseId);

    /**
     * Dohvat ispita zajedno sa zadacima. Bez JOIN FETCH svaki pristup
     * zadatku pokrenuo bi zaseban upit.
     */
    @Query("""
            SELECT e FROM Exam e
            LEFT JOIN FETCH e.tasks t
            LEFT JOIN FETCH t.outcome o
            LEFT JOIN FETCH o.course
            WHERE e.id = :id
            """)
    Optional<Exam> findByIdWithTasks(@Param("id") UUID id);
}