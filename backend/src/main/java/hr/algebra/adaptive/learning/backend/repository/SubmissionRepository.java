package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<Submission> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    Page<Submission> findByStudentId(UUID studentId, Pageable pageable);

    Page<Submission> findByTaskId(UUID taskId, Pageable pageable);

    long countByStudentId(UUID studentId);

    long countByTaskId(UUID taskId);

    List<Submission> findByStudentIdAndTaskId(UUID studentId, UUID taskId);

    List<Submission> findByStatus(SubmissionStatus status);

    // Metode potrebne za Dashboard
    long countDistinctCompletedTasksByStudentId(UUID id);

    @Query("SELECT MAX(s.createdAt) FROM Submission s WHERE s.student.id = :studentId")
    Optional<LocalDateTime> findLastSubmissionDateByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT s FROM Submission s ORDER BY s.createdAt DESC")
    List<Submission> findAllOrderByCreatedAtDesc();

    /**
     * Bulk dohvat svih predaja za nastavnicki pregled.
     * <p>
     * JOIN FETCH ucitava studenta, zadatak, ishod i kolegij u jednom upitu,
     * cime se izbjegava N+1 problem pri mapiranju u DTO. Bez toga bi svaki
     * pristup s.getTask().getTitle() pokrenuo zaseban SELECT.
     */
    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.student st
            LEFT JOIN FETCH st.schoolClass
            JOIN FETCH s.task t
            JOIN FETCH t.outcome o
            JOIN FETCH o.course c
            WHERE (CAST(:courseId AS uuid) IS NULL OR c.id = :courseId)
              AND (CAST(:studentId AS uuid) IS NULL OR st.id = :studentId)
              AND (CAST(:from AS timestamp) IS NULL OR s.createdAt >= :from)
            ORDER BY st.lastName ASC, st.firstName ASC, s.createdAt DESC
            """)
    List<Submission> findAllForTeacherOverview(
            @Param("courseId") UUID courseId,
            @Param("studentId") UUID studentId,
            @Param("from") LocalDateTime from);
}