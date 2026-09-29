package hr.algebra.adaptive.learning.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Spremanje odgovora na jedan zadatak dok je ispit jos otvoren.
 *
 * Poziva se svaki put kad student prijede na sljedeci zadatak ili
 * rucno spremi, pa rad ne propada ako se stranica osvjezi.
 */
@Data
public class ExamAnswerRequest {

    @NotNull(message = "Zadatak je obavezan")
    private UUID taskId;

    /**
     * Sadrzaj odgovora prema tipu zadatka:
     * CODE            - izvorni kod
     * TEXT            - tekst odgovora
     * MULTIPLE_CHOICE - odabrana opcija
     * CHECKLIST       - JSON popis odabranih opcija
     */
    private String answerContent;

    /** Je li student oznacio zadatak kao dovrsen. */
    private Boolean markedDone;

    /**
     * Treba li odmah izvrsiti kod i vratiti rezultate testova.
     * Vrijedi samo za programske zadatke i samo ako ispit to dopusta.
     */
    private Boolean runTests = false;
}