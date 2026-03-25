package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.dto.request.CourseRequest;
import hr.algebra.adaptive.learning.backend.dto.response.CourseResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.CourseRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.CourseService;
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
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CourseResponse create(CourseRequest request, UUID createdById) {
        log.info("Creating course: {}", request.getName());

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdById));

        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .languageType(request.getLanguageType())
                .createdBy(creator)
                .isActive(true)
                .build();

        Course saved = courseRepository.save(course);
        log.info("Course created with ID: {}", saved.getId());

        return CourseResponse.fromEntity(saved);
    }

    @Override
    public CourseResponse getById(UUID id) {
        Course course = findCourseOrThrow(id);
        return CourseResponse.fromEntity(course);
    }

    @Override
    public List<CourseResponse> getAll() {
        return courseRepository.findByIsActiveTrue().stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @Override
    public List<CourseResponse> getByLanguageType(LanguageType languageType) {
        return courseRepository.findByLanguageType(languageType).stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @Override
    public List<CourseResponse> getByTeacher(UUID teacherId) {
        return courseRepository.findByCreatedById(teacherId).stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public CourseResponse update(UUID id, CourseRequest request) {
        log.info("Updating course: {}", id);

        Course course = findCourseOrThrow(id);

        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setLanguageType(request.getLanguageType());

        Course saved = courseRepository.save(course);
        return CourseResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        log.info("Deleting course: {}", id);

        Course course = findCourseOrThrow(id);
        courseRepository.delete(course);
        return false;
    }

    @Override
    @Transactional
    public boolean activate(UUID id) {
        log.info("Activating course: {}", id);

        Course course = findCourseOrThrow(id);
        course.setActive(true);
        courseRepository.save(course);
        return false;
    }

    @Override
    @Transactional
    public boolean deactivate(UUID id) {
        log.info("Deactivating course: {}", id);

        Course course = findCourseOrThrow(id);
        course.setActive(false);
        courseRepository.save(course);
        return false;
    }

    private Course findCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }
}