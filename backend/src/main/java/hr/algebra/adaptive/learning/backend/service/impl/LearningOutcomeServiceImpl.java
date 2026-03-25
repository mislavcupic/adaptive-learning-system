package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import hr.algebra.adaptive.learning.backend.dto.request.LearningOutcomeRequest;
import hr.algebra.adaptive.learning.backend.dto.response.LearningOutcomeResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.CourseRepository;
import hr.algebra.adaptive.learning.backend.repository.LearningOutcomeRepository;
import hr.algebra.adaptive.learning.backend.service.LearningOutcomeService;
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
public class LearningOutcomeServiceImpl implements LearningOutcomeService {

    private final LearningOutcomeRepository outcomeRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public LearningOutcomeResponse create(LearningOutcomeRequest request) {
        log.info("Creating learning outcome: {}", request.getName());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        LearningOutcome outcome = LearningOutcome.builder()
                .name(request.getName())
                .description(request.getDescription())
                .orderIndex(request.getOrderIndex())
                .course(course)
                .build();

        LearningOutcome saved = outcomeRepository.save(outcome);
        log.info("Learning outcome created with ID: {}", saved.getId());

        return LearningOutcomeResponse.fromEntity(saved);
    }

    @Override
    public LearningOutcomeResponse getById(UUID id) {
        LearningOutcome outcome = findOutcomeOrThrow(id);
        return LearningOutcomeResponse.fromEntity(outcome);
    }

    @Override
    public List<LearningOutcomeResponse> getByCourse(UUID courseId) {
        return outcomeRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(LearningOutcomeResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public LearningOutcomeResponse update(UUID id, LearningOutcomeRequest request) {
        log.info("Updating learning outcome: {}", id);

        LearningOutcome outcome = findOutcomeOrThrow(id);

        outcome.setName(request.getName());
        outcome.setDescription(request.getDescription());
        outcome.setOrderIndex(request.getOrderIndex());

        LearningOutcome saved = outcomeRepository.save(outcome);
        return LearningOutcomeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.info("Deleting learning outcome: {}", id);

        LearningOutcome outcome = findOutcomeOrThrow(id);
        outcomeRepository.delete(outcome);
    }

    @Override
    @Transactional
    public void reorder(UUID courseId, List<UUID> orderedIds) {
        log.info("Reordering outcomes for course: {}", courseId);

        for (int i = 0; i < orderedIds.size(); i++) {
            LearningOutcome outcome = findOutcomeOrThrow(orderedIds.get(i));
            outcome.setOrderIndex(i);
            outcomeRepository.save(outcome);
        }
    }

    private LearningOutcome findOutcomeOrThrow(UUID id) {
        return outcomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LearningOutcome", "id", id));
    }
}