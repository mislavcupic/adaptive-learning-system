package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.response.StudentSubmissionsOverviewResponse;

import java.util.UUID;

public interface SubmissionOverviewService {

    StudentSubmissionsOverviewResponse getOverview(UUID courseId, UUID studentId, Integer lastDays);
}