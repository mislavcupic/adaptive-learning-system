package hr.algebra.adaptive.learning.backend.exception;

import java.util.UUID;

public class PretestRequiredException extends RuntimeException {
    public PretestRequiredException(UUID courseId) {
        super("Morate riješiti pretest za ovaj kolegij prije rješavanja zadataka.");
    }
}
