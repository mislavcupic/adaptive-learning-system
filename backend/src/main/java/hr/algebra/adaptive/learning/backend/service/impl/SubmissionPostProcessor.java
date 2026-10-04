package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.ml.MlBktRequest;
import hr.algebra.adaptive.learning.backend.repository.SubmissionRepository;
import hr.algebra.adaptive.learning.backend.service.MLServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Poslovi koji se izvode tek nakon sto je predaja potvrdena u bazi.
 *
 * ML servis se na bazu spaja zasebnom vezom i ne vidi podatke iz jos
 * nepotvrdene transakcije. Ako ga pozovemo prerano, spremanje RAG
 * biljeske pada na stranom kljucu, a BKT nema na sto vezati procjenu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionPostProcessor {

    private final SubmissionRepository submissionRepository;
    private final MLServiceClient mlServiceClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.warn("Predaja nije pronađena za naknadnu obradu: {}", submissionId);
            return;
        }

        updateBkt(submission);
    }

    /**
     * Azurira BKT procjenu za vjestinu kojoj zadatak pripada.
     *
     * Kao naziv vjestine koristi se ishod ucenja, jer je to razina na
     * kojoj nastavnik definira sto student treba savladati.
     */
    private void updateBkt(Submission submission) {
        try {
            var task = submission.getTask();
            if (task == null || task.getOutcome() == null) return;

            // Tekstualni odgovori i ček-liste ocjenjuje profesor, pa dok
            // nema njegove ocjene nemamo osnovu za BKT. Da ih tretiramo
            // kao netočne, procjena bi pala bez razloga.
            TaskType type = task.getTaskType() != null ? task.getTaskType() : TaskType.CODE;
            if (type == TaskType.TEXT || type == TaskType.CHECKLIST) {
                if (submission.getTeacherScore() == null) {
                    log.info("BKT preskočen - {} čeka ocjenu profesora", type);
                    return;
                }
            }

            String skillName = task.getOutcome().getName();

            boolean correct;
            if (submission.getTeacherScore() != null) {
                Integer max = task.getMaxScore();
                correct = max != null && max > 0
                        && (submission.getTeacherScore() * 100.0 / max) >= 50;
            } else if (submission.getTestsTotal() != null && submission.getTestsTotal() > 0) {
                correct = submission.getTestsPassed() != null
                        && submission.getTestsPassed().equals(submission.getTestsTotal());
            } else {
                Integer score = submission.getAiScore();
                Integer max = task.getMaxScore();
                correct = score != null && max != null && max > 0
                        && (score * 100.0 / max) >= 50;
            }

            mlServiceClient.updateBkt(MlBktRequest.builder()
                    .studentId(submission.getStudent().getId())
                    .skillName(skillName)
                    .isCorrect(correct)
                    .build());

            log.info("BKT ažuriran za vještinu {}, uspjeh: {}", skillName, correct);

        } catch (Exception e) {
            log.error("BKT ažuriranje nije uspjelo: {}", e.getMessage());
        }
    }
}
