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
        List<ResearchResultResponse> results = getResults();
        List<MLAncovaRequest.Record> records = extractValidRecords(results);

        log.info("Prepared {} records for ANCOVA (students with both pretest and posttest)", records.size());

        Set<String> distinctGroups = extractDistinctGroups(records);

        if (records.isEmpty() || distinctGroups.size() < 2) {
            return buildInsufficientDataResponse(records.size(), distinctGroups);
        }

        return executeAncovaRequest(records, distinctGroups);
    }

    private List<MLAncovaRequest.Record> extractValidRecords(List<ResearchResultResponse> results) {
        List<MLAncovaRequest.Record> records = new ArrayList<>();
        for (ResearchResultResponse r : results) {
            if (!isValidRecord(r)) {
                continue;
            }
            String group = r.getResearchGroup().name();
            records.add(MLAncovaRequest.Record.builder()
                    .group(group)
                    .pretest(r.getPretestPercentage())
                    .posttest(r.getPosttestPercentage())
                    .build());
        }
        return records;
    }

    private boolean isValidRecord(ResearchResultResponse r) {
        if (r.getResearchGroup() == null) {
            return false;
        }
        String group = r.getResearchGroup().name();
        if (!group.equals("CONTROL") && !group.equals("EXPERIMENTAL")) {
            return false;
        }
        if (r.getPretestPercentage() == null || r.getPosttestPercentage() == null) {
            return false;
        }
        return r.getPretestMaxScore() != null && r.getPretestMaxScore() > 0
                && r.getPosttestMaxScore() != null && r.getPosttestMaxScore() > 0;
    }

    private Set<String> extractDistinctGroups(List<MLAncovaRequest.Record> records) {
        Set<String> distinctGroups = new HashSet<>();
        for (MLAncovaRequest.Record rec : records) {
            distinctGroups.add(rec.getGroup());
        }
        return distinctGroups;
    }

    private MLAncovaResponse buildInsufficientDataResponse(int recordCount, Set<String> distinctGroups) {
        log.info("Not enough data for ANCOVA (records={}, groups={}). Skipping ML call.",
                recordCount, distinctGroups.size());
        return MLAncovaResponse.builder()
                .nTotal(recordCount)
                .groups(new ArrayList<>(distinctGroups))
                .descriptives(new ArrayList<>())
                .ancova(null)
                .warning("Nema dovoljno podataka za ANCOVA analizu. " +
                        "Potrebni su ispitanici u obje skupine (CONTROL i EXPERIMENTAL) " +
                        "koji su riješili i pretest i posttest.")
                .build();
    }

    private MLAncovaResponse executeAncovaRequest(List<MLAncovaRequest.Record> records, Set<String> distinctGroups) {
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