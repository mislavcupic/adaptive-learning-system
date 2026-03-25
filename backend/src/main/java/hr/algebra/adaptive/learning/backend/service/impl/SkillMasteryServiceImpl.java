package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.SkillMastery;
import hr.algebra.adaptive.learning.backend.dto.response.SkillMasteryResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.SkillMasteryRepository;
import hr.algebra.adaptive.learning.backend.service.SkillMasteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillMasteryServiceImpl implements SkillMasteryService {

    private final SkillMasteryRepository skillMasteryRepository;

    @Override
    public List<SkillMasteryResponse> getByStudent(UUID studentId) {
        return skillMasteryRepository.findByStudentId(studentId).stream()
                .map(SkillMasteryResponse::fromEntity)
                .toList();
    }

    @Override
    public SkillMasteryResponse getByStudentAndSkill(UUID studentId, String skillName) {
        SkillMastery mastery = skillMasteryRepository.findByStudentIdAndSkillName(studentId, skillName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("SkillMastery not found for student %s and skill %s", studentId, skillName)));
        return SkillMasteryResponse.fromEntity(mastery);
    }

    @Override
    public double getAverageMastery(UUID studentId) {
        List<SkillMastery> masteries = skillMasteryRepository.findByStudentId(studentId);
        if (masteries.isEmpty()) {
            return 0.0;
        }
        return masteries.stream()
                .mapToDouble(SkillMastery::getMasteryLevel)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional
    public void updateMasteryAfterSubmission(UUID studentId, UUID submissionId, boolean correct) {
        // TODO: Implementirati BKT algoritam
        // Ovo će biti pozivano iz ML servisa nakon analize submission-a
        log.info("Updating mastery for student {} after submission {}, correct: {}",
                studentId, submissionId, correct);
    }
}