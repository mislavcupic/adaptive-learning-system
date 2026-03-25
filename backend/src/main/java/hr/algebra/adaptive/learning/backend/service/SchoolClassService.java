package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.request.SchoolClassRequest;
import hr.algebra.adaptive.learning.backend.dto.response.SchoolClassResponse;
import hr.algebra.adaptive.learning.backend.dto.response.StudentResponse;

import java.util.List;
import java.util.UUID;

public interface SchoolClassService {

    SchoolClassResponse create(SchoolClassRequest request, UUID teacherId);

    SchoolClassResponse getById(UUID id);

    List<SchoolClassResponse> getAll();

    List<SchoolClassResponse> getByTeacher(UUID teacherId);

    List<SchoolClassResponse> getByStudent(UUID studentId);

    SchoolClassResponse update(UUID id, SchoolClassRequest request);

    void delete(UUID id);

    void addStudent(UUID classId, UUID studentId);

    void removeStudent(UUID classId, UUID studentId);

    void addCourse(UUID classId, UUID courseId);

    void removeCourse(UUID classId, UUID courseId);

    List<StudentResponse> getStudents(UUID classId);
}