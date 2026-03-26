package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.request.TaskRequest;
import hr.algebra.adaptive.learning.backend.dto.response.TaskResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TaskService {

    TaskResponse create(TaskRequest request, UUID createdById);

    TaskResponse getById(UUID id);

    List<TaskResponse> getAll();

    List<TaskResponse> getByOutcome(UUID outcomeId);

    List<TaskResponse> getByTeacher(UUID teacherId);

    List<TaskResponse> getPendingForStudent(UUID studentId);

    TaskResponse update(UUID id, TaskRequest request);

    void delete(UUID id);

    TaskResponse duplicate(UUID id, UUID createdById);
}