package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.request.SubmissionRequest;
import hr.algebra.adaptive.learning.backend.dto.response.PaginatedResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SubmissionResponse;

import java.util.List;
import java.util.UUID;

public interface SubmissionService {

    SubmissionResponse submit(SubmissionRequest request, UUID studentId);

    SubmissionResponse getById(UUID id);

    List<SubmissionResponse> getByStudent(UUID studentId);

    PaginatedResponse<SubmissionResponse> getByStudentPaginated(UUID studentId, int page, int size);

    List<SubmissionResponse> getByTask(UUID taskId);

    PaginatedResponse<SubmissionResponse> getByTaskPaginated(UUID taskId, int page, int size);

    long countByStudent(UUID studentId);

    long countByTask(UUID taskId);

    SubmissionResponse addTeacherFeedback(UUID id, String feedback, Integer score);
}