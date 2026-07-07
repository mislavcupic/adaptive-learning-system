package hr.algebra.adaptive.learning.backend.exception;

public class AssessmentAlreadyCompletedException extends RuntimeException {
    public AssessmentAlreadyCompletedException(String message) {
        super(message);
    }
}