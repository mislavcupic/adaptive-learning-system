package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.request.TaskRequest;
import hr.algebra.adaptive.learning.backend.dto.response.TaskResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.LearningOutcomeRepository;
import hr.algebra.adaptive.learning.backend.repository.TaskRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final LearningOutcomeRepository outcomeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TaskResponse create(TaskRequest request, UUID createdById) {
        log.info("Creating task: {}", request.getTitle());

        LearningOutcome outcome = outcomeRepository.findById(request.getOutcomeId())
                .orElseThrow(() -> new ResourceNotFoundException("LearningOutcome", "id", request.getOutcomeId()));

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdById));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .starterCode(request.getStarterCode())
                .solutionCode(request.getSolutionCode())
                .testCases(request.getTestCases())
                .gradingCriteria(request.getGradingCriteria())
                .maxScore(request.getMaxScore())
                .timeLimitSeconds(request.getTimeLimitSeconds())
                .memoryLimitMb(request.getMemoryLimitMb())
                .outcome(outcome)
                .createdBy(creator)
                .dueDate(request.getDueDate())
                .orderIndex(request.getOrderIndex())
                .isActive(true)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Task created with ID: {}", saved.getId());

        return TaskResponse.fromEntity(saved);
    }

    @Override
    public TaskResponse getById(UUID id) {
        Task task = findTaskOrThrow(id);
        return TaskResponse.fromEntity(task);
    }

    @Override
    public List<TaskResponse> getAll() {
        return taskRepository.findByIsActiveTrue().stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Override
    public List<TaskResponse> getByOutcome(UUID outcomeId) {
        return taskRepository.findByOutcomeIdOrderByOrderIndexAsc(outcomeId).stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Override
    public List<TaskResponse> getByTeacher(UUID teacherId) {
        return taskRepository.findByCreatedById(teacherId).stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Override
    public List<TaskResponse> getPendingForStudent(UUID studentId) {
        return taskRepository.findByDueDateAfterAndIsActiveTrue(LocalDateTime.now()).stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        log.info("Updating task: {}", id);

        Task task = findTaskOrThrow(id);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStarterCode(request.getStarterCode());
        task.setSolutionCode(request.getSolutionCode());
        task.setTestCases(request.getTestCases());
        task.setGradingCriteria(request.getGradingCriteria());
        task.setMaxScore(request.getMaxScore());
        task.setTimeLimitSeconds(request.getTimeLimitSeconds());
        task.setMemoryLimitMb(request.getMemoryLimitMb());
        task.setDueDate(request.getDueDate());
        task.setOrderIndex(request.getOrderIndex());

        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.info("Deleting task: {}", id);

        Task task = findTaskOrThrow(id);
        taskRepository.delete(task);
    }

    @Override
    @Transactional
    public TaskResponse duplicate(UUID id, UUID createdById) {
        log.info("Duplicating task: {}", id);

        Task original = findTaskOrThrow(id);
        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdById));

        Task duplicate = Task.builder()
                .title(original.getTitle() + " (kopija)")
                .description(original.getDescription())
                .starterCode(original.getStarterCode())
                .solutionCode(original.getSolutionCode())
                .testCases(original.getTestCases())
                .gradingCriteria(original.getGradingCriteria())
                .maxScore(original.getMaxScore())
                .timeLimitSeconds(original.getTimeLimitSeconds())
                .memoryLimitMb(original.getMemoryLimitMb())
                .outcome(original.getOutcome())
                .createdBy(creator)
                .orderIndex(original.getOrderIndex())
                .isActive(true)
                .build();

        Task saved = taskRepository.save(duplicate);
        return TaskResponse.fromEntity(saved);
    }

    private Task findTaskOrThrow(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }
}