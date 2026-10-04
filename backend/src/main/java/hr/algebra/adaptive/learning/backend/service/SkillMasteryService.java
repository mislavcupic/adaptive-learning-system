package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.response.SkillMasteryResponse;

import java.util.List;
import java.util.UUID;

public interface SkillMasteryService {

    List<SkillMasteryResponse> getByStudent(UUID studentId);

    SkillMasteryResponse getByStudentAndSkill(UUID studentId, String skillName);

    double getAverageMastery(UUID studentId);

}