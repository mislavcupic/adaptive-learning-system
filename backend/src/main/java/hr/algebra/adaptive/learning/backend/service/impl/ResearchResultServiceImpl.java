package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.AssessmentAttempt;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import hr.algebra.adaptive.learning.backend.dto.response.ResearchResultResponse;
import hr.algebra.adaptive.learning.backend.repository.AssessmentAttemptRepository;
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
}
