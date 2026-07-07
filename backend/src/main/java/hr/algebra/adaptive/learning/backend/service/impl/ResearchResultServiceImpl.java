package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.AssessmentAttempt;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import hr.algebra.adaptive.learning.backend.dto.ml.MLAncovaRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MLAncovaResponse;
import hr.algebra.adaptive.learning.backend.dto.response.ResearchResultResponse;
import hr.algebra.adaptive.learning.backend.repository.AssessmentAttemptRepository;
import hr.algebra.adaptive.learning.backend.service.MLServiceClient;
import hr.algebra.adaptive.learning.backend.service.ResearchResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResearchResultServiceImpl implements ResearchResultService {

    private final AssessmentAttemptRepository attemptRepository;
    private final MLServiceClient mlServiceClient;

    @Override
    public List<ResearchResultResponse> getResults() {
        List<AssessmentAttempt> attempts = attemptRepository.findAllCompletedForResearch();

        // Grupiraj po studentu
        Map<UUID, ResearchResultResponse.ResearchResultResponseBuilder> byStudent = new LinkedHashMap<>();

        for (AssessmentAttempt attempt : attempts) {
            User student = attempt.getStudent();
            UUID studentId = student.getId();

            ResearchResultResponse.ResearchResultResponseBuilder builder =
                    byStudent.computeIfAbsent(studentId, k ->
                            ResearchResultResponse.builder()
                                    .studentId(studentId)
                                    .firstName(student.getFirstName())
                                    .lastName(student.getLastName())
                                    .email(student.getEmail())
                                    .researchGroup(student.getResearchGroup()));

            AssessmentType type = attempt.getAssessment().getAssessmentType();
            Integer score = attempt.getScore() != null ? attempt.getScore() : 0;
            Integer maxScore = attempt.getMaxScore() != null ? attempt.getMaxScore() : 0;
            Double percentage = maxScore > 0 ? (score * 100.0 / maxScore) : 0.0;

            if (type == AssessmentType.PRETEST) {
                builder.pretestScore(score)
                        .pretestMaxScore(maxScore)
                        .pretestPercentage(percentage);
            } else if (type == AssessmentType.POSTTEST) {
                builder.posttestScore(score)
                        .posttestMaxScore(maxScore)
                        .posttestPercentage(percentage);
            }
        }

        return byStudent.values().stream()
                .map(ResearchResultResponse.ResearchResultResponseBuilder::build)
                .toList();
    }

    @Override
    public MLAncovaResponse getAncova() {
        // Iskoristi već pivotirane rezultate po studentu
        List<ResearchResultResponse> results = getResults();

        // ANCOVA zahtijeva SAMO ispitanike koji imaju I pretest I posttest,
        // pripadaju grupi CONTROL/EXPERIMENTAL, i imaju smisleni max_score (> 0).
        List<MLAncovaRequest.Record> records = new ArrayList<>();

        for (ResearchResultResponse r : results) {
            if (r.getResearchGroup() == null) {
                continue;
            }
            String group = r.getResearchGroup().name();
            if (!group.equals("CONTROL") && !group.equals("EXPERIMENTAL")) {
                continue; // preskoči NOT_ASSIGNED
            }
            if (r.getPretestPercentage() == null || r.getPosttestPercentage() == null) {
                continue;
            }
            // Preskoči degenerirane attempte gdje je max_score bio 0
            if (r.getPretestMaxScore() == null || r.getPretestMaxScore() <= 0
                    || r.getPosttestMaxScore() == null || r.getPosttestMaxScore() <= 0) {
                continue;
            }

            records.add(MLAncovaRequest.Record.builder()
                    .group(group)
                    .pretest(r.getPretestPercentage())
                    .posttest(r.getPosttestPercentage())
                    .build());
        }

        log.info("Prepared {} records for ANCOVA (students with both pretest and posttest)", records.size());

        // Ako nema dovoljno podataka, NE zovi ML servis (vratio bi 400) —
        // vrati prazan rezultat s objašnjenjem.
        Set<String> distinctGroups = new HashSet<>();
        for (MLAncovaRequest.Record rec : records) {
            distinctGroups.add(rec.getGroup());
        }

        if (records.isEmpty() || distinctGroups.size() < 2) {
            log.info("Not enough data for ANCOVA (records={}, groups={}). Skipping ML call.",
                    records.size(), distinctGroups.size());
            return MLAncovaResponse.builder()
                    .nTotal(records.size())
                    .groups(new ArrayList<>(distinctGroups))
                    .descriptives(new ArrayList<>())
                    .ancova(null)
                    .warning("Nema dovoljno podataka za ANCOVA analizu. " +
                            "Potrebni su ispitanici u obje skupine (CONTROL i EXPERIMENTAL) " +
                            "koji su riješili i pretest i posttest.")
                    .build();
        }

        MLAncovaRequest request = MLAncovaRequest.builder()
                .records(records)
                .build();

        try {
            return mlServiceClient.runAncova(request);
        } catch (Exception e) {
            log.error("ANCOVA ML call failed: {}", e.getMessage());
            return MLAncovaResponse.builder()
                    .nTotal(records.size())
                    .groups(new ArrayList<>(distinctGroups))
                    .descriptives(new ArrayList<>())
                    .ancova(null)
                    .warning("Statistička analiza trenutno nije dostupna: " + e.getMessage())
                    .build();
        }
    }
}