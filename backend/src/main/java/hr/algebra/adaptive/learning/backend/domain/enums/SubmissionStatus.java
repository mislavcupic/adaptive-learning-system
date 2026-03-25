package hr.algebra.adaptive.learning.backend.domain.enums;

public enum SubmissionStatus {
    PENDING,
    COMPILING,
    RUNNING,
    COMPLETED,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TIMEOUT,
    FAILED
}