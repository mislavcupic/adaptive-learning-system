package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.request.LearningOutcomeRequest;
import hr.algebra.adaptive.learning.backend.dto.response.LearningOutcomeResponse;

import java.util.List;
import java.util.UUID;

public interface LearningOutcomeService {

    LearningOutcomeResponse create(LearningOutcomeRequest request);

    LearningOutcomeResponse getById(UUID id);

    List<LearningOutcomeResponse> getByCourse(UUID courseId);

    LearningOutcomeResponse update(UUID id, LearningOutcomeRequest request);

    void delete(UUID id);

    void reorder(UUID courseId, List<UUID> orderedIds);
}