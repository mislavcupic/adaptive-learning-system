package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.ExamAttempt;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    Optional<ExamAttempt> findByStudentIdAndExamId(UUID studentId, UUID examId);

    boolean existsByStudentIdAndExamId(UUID studentId, UUID examId);

    List<ExamAttempt> findByStudentIdOrderByStartedAtDesc(UUID studentId);

    List<ExamAttempt> findByExamIdOrderByStartedAtDesc(UUID examId);

    long countByExamIdAndStatus(UUID examId, ExamAttemptStatus status);

    /**
     * Pokusaj sa svim odgovorima i pripadajucim zadacima u jednom upitu.
     */
    @Query("""
            SELECT a FROM ExamAttempt a
            JOIN FETCH a.exam e
            JOIN FETCH a.student
            LEFT JOIN FETCH a.answers ans
            LEFT JOIN FETCH ans.task
            WHERE a.id = :id
            """)
    Optional<ExamAttempt> findByIdWithAnswers(@Param("id") UUID id);

    @Query("""
            SELECT a FROM ExamAttempt a
            JOIN FETCH a.exam e
            JOIN FETCH a.student
            LEFT JOIN FETCH a.answers ans
            LEFT JOIN FETCH ans.task
            WHERE a.student.id = :studentId AND a.exam.id = :examId
            """)
    Optional<ExamAttempt> findByStudentAndExamWithAnswers(
            @Param("studentId") UUID studentId,
            @Param("examId") UUID examId);

    /**
     * Pregled svih pokusaja za nastavnika, sa studentom i ispitom.
     */
    @Query("""
            SELECT a FROM ExamAttempt a
            JOIN FETCH a.exam e
            JOIN FETCH a.student s
            WHERE e.id = :examId
            ORDER BY s.lastName ASC, s.firstName ASC
            """)
    List<ExamAttempt> findAllForExamOverview(@Param("examId") UUID examId);

    /**
     * Pokusaji kojima je rok istekao, a jos su otvoreni.
     * Koristi se za automatsko zatvaranje.
     */
    @Query("""
            SELECT a FROM ExamAttempt a
            WHERE a.status = 'IN_PROGRESS'
              AND a.deadlineAt IS NOT NULL
              AND a.deadlineAt < CURRENT_TIMESTAMP
            """)
    List<ExamAttempt> findExpiredOpenAttempts();
}