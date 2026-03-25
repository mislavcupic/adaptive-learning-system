package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SchoolClassResponse {
    private UUID id;
    private String name;
    private String description;
    private String academicYear;
    private String teacherName;
    private UUID teacherId;
    private int studentsCount;
    private int coursesCount;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static SchoolClassResponse fromEntity(SchoolClass schoolClass) {
        return SchoolClassResponse.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .description(schoolClass.getDescription())
                .academicYear(schoolClass.getAcademicYear())
                .teacherName(schoolClass.getTeacher() != null
                        ? schoolClass.getTeacher().getFirstName() + " " + schoolClass.getTeacher().getLastName()
                        : null)
                .teacherId(schoolClass.getTeacher() != null ? schoolClass.getTeacher().getId() : null)
                .studentsCount(schoolClass.getStudents() != null ? schoolClass.getStudents().size() : 0)
                .coursesCount(schoolClass.getCourses() != null ? schoolClass.getCourses().size() : 0)
                .isActive(schoolClass.isActive())
                .createdAt(schoolClass.getCreatedAt())
                .build();
    }
}