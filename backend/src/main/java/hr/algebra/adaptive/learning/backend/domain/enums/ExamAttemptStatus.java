package hr.algebra.adaptive.learning.backend.domain.enums;

/**
 * Stanje studentovog pokusaja rjesavanja ispita.
 */
public enum ExamAttemptStatus {

    /** Ispit je otvoren, student jos moze mijenjati odgovore. */
    IN_PROGRESS,

    /** Student je predao ispit; odgovori su zakljucani. */
    SUBMITTED,

    /** Rok je istekao prije nego je student predao. */
    EXPIRED,

    /** Automatsko ocjenjivanje je dovrseno. */
    GRADED,

    /** Nastavnik je pregledao i potvrdio ocjenu. */
    REVIEWED
}
