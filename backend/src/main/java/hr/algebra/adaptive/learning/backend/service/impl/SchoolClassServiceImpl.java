package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.request.SchoolClassRequest;
import hr.algebra.adaptive.learning.backend.dto.response.SchoolClassResponse;
import hr.algebra.adaptive.learning.backend.dto.response.StudentResponse;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.CourseRepository;
import hr.algebra.adaptive.learning.backend.repository.SchoolClassRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.SchoolClassService;
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
public class SchoolClassServiceImpl implements SchoolClassService {

    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public SchoolClassResponse create(SchoolClassRequest request, UUID teacherId) {
        log.info("Creating class: {}", request.getName());

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        SchoolClass schoolClass = SchoolClass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .academicYear(request.getAcademicYear())
                .teacher(teacher)
                .isActive(true)
                .build();

        if (request.getCourseIds() != null && !request.getCourseIds().isEmpty()) {
            request.getCourseIds().forEach(courseId -> {
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
                schoolClass.getCourses().add(course);
            });
        }

        SchoolClass saved = classRepository.save(schoolClass);
        log.info("Class created with ID: {}", saved.getId());

        return SchoolClassResponse.fromEntity(saved);
    }

    @Override
    public SchoolClassResponse getById(UUID id) {
        SchoolClass schoolClass = findClassOrThrow(id);
        return SchoolClassResponse.fromEntity(schoolClass);
    }

    @Override
    public List<SchoolClassResponse> getAll() {
        return classRepository.findByIsActiveTrue().stream()
                .map(SchoolClassResponse::fromEntity)
                .toList();
    }

    @Override
    public List<SchoolClassResponse> getByTeacher(UUID teacherId) {
        return classRepository.findByTeacherId(teacherId).stream()
                .map(SchoolClassResponse::fromEntity)
                .toList();
    }

    @Override
    public List<SchoolClassResponse> getByStudent(UUID studentId) {
        return classRepository.findByStudentsId(studentId).stream()
                .map(SchoolClassResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public SchoolClassResponse update(UUID id, SchoolClassRequest request) {
        log.info("Updating class: {}", id);

        SchoolClass schoolClass = findClassOrThrow(id);

        schoolClass.setName(request.getName());
        schoolClass.setDescription(request.getDescription());
        schoolClass.setAcademicYear(request.getAcademicYear());

        SchoolClass saved = classRepository.save(schoolClass);
        return SchoolClassResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.info("Deleting class: {}", id);

        SchoolClass schoolClass = findClassOrThrow(id);
        classRepository.delete(schoolClass);
    }

    @Override
    @Transactional
    public void addStudent(UUID classId, UUID studentId) {
        log.info("Adding student {} to class {}", studentId, classId);

        SchoolClass schoolClass = findClassOrThrow(classId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        if (schoolClass.getStudents().contains(student)) {
            throw new BadRequestException("Student is already in this class");
        }

        schoolClass.getStudents().add(student);
        classRepository.save(schoolClass);
    }

    @Override
    @Transactional
    public void removeStudent(UUID classId, UUID studentId) {
        log.info("Removing student {} from class {}", studentId, classId);

        SchoolClass schoolClass = findClassOrThrow(classId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        schoolClass.getStudents().remove(student);
        classRepository.save(schoolClass);
    }

    @Override
    @Transactional
    public void addCourse(UUID classId, UUID courseId) {
        log.info("Adding course {} to class {}", courseId, classId);

        SchoolClass schoolClass = findClassOrThrow(classId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (schoolClass.getCourses().contains(course)) {
            throw new BadRequestException("Course is already assigned to this class");
        }

        schoolClass.getCourses().add(course);
        classRepository.save(schoolClass);
    }

    @Override
    @Transactional
    public void removeCourse(UUID classId, UUID courseId) {
        log.info("Removing course {} from class {}", courseId, classId);

        SchoolClass schoolClass = findClassOrThrow(classId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        schoolClass.getCourses().remove(course);
        classRepository.save(schoolClass);
    }

    @Override
    public List<StudentResponse> getStudents(UUID classId) {
        SchoolClass schoolClass = findClassOrThrow(classId);
        return schoolClass.getStudents().stream()
                .map(StudentResponse::fromEntity)
                .toList();
    }

    private SchoolClass findClassOrThrow(UUID id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass", "id", id));
    }
}