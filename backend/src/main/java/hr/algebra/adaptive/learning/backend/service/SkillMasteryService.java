package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.response.SkillMasteryResponse;

import java.util.List;
import java.util.UUID;

public interface SkillMasteryService {

    List<SkillMasteryResponse> getByStudent(UUID studentId);

    SkillMasteryResponse getByStudentAndSkill(UUID studentId, String skillName);

    double getAverageMastery(UUID studentId);

    // BKT update će se dodati kada implementiramo ML servis
    void updateMasteryAfterSubmission(UUID studentId, UUID submissionId, boolean correct);
}