package hr.algebra.adaptive.learning.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Zahtjev za stvaranje ili izmjenu ispita.
 */
@Data
public class ExamRequest {

    @NotBlank(message = "Naziv ispita je obavezan")
    private String title;

    private String description;

    private String instructions;

    @NotNull(message = "Kolegij je obavezan")
    private UUID courseId;

    /** Zadaci ispita, redoslijedom kojim ih student rjesava. */
    private List<UUID> taskIds;

    private Integer timeLimitMinutes;

    private Integer passingScore = 50;

    private LocalDateTime availableFrom;

    private LocalDateTime availableUntil;

    private Boolean showTestResults = true;

    private Boolean isPublished = false;
}