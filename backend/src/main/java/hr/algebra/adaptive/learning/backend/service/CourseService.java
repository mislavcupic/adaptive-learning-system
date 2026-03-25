package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.request.CourseRequest;
import hr.algebra.adaptive.learning.backend.dto.response.CourseResponse;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;

import java.util.List;
import java.util.UUID;

public interface CourseService {

    CourseResponse create(CourseRequest request, UUID createdById);

    CourseResponse getById(UUID id);

    List<CourseResponse> getAll();

    List<CourseResponse> getByLanguageType(LanguageType languageType);

    List<CourseResponse> getByTeacher(UUID teacherId);

    CourseResponse update(UUID id, CourseRequest request);

    boolean delete(UUID id);

    boolean activate(UUID id);

    boolean deactivate(UUID id);
}