package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.SchoolClassRequest;
import hr.algebra.adaptive.learning.backend.dto.response.SchoolClassResponse;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.CourseRepository;
import hr.algebra.adaptive.learning.backend.repository.SchoolClassRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SchoolClassServiceImpl")
class SchoolClassServiceImplTest {

    @Mock private SchoolClassRepository classRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks private SchoolClassServiceImpl classService;

    private UUID classId;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private User teacher;
    private User studentUser;
    private Course course;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        classId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        teacher = User.builder()
                .email("ines@test.hr")
                .firstName("Ines")
                .lastName("Inić")
                .role(UserRole.TEACHER)
                .build();
        teacher.setId(teacherId);

        studentUser = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .build();
        studentUser.setId(studentId);

        course = Course.builder()
                .name("Uvod u programiranje")
                .languageType(LanguageType.C)
                .isActive(true)
                .build();
        course.setId(courseId);

        schoolClass = SchoolClass.builder()
                .name("2.A")
                .academicYear("2026/2027")
                .teacher(teacher)
                .isActive(true)
                .students(new HashSet<>())
                .courses(new HashSet<>())
                .build();
        schoolClass.setId(classId);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(classRepository.save(any(SchoolClass.class))).thenAnswer(i -> {
            SchoolClass c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
    }

    @Nested
    @DisplayName("Kreiranje razreda")
    class Creating {

        @Test
        @DisplayName("sprema razred s predanim podacima")
        void savesWithGivenData() {
            SchoolClassResponse result = classService.create(request(), teacherId);

            assertThat(result.getName()).isEqualTo("3.B");
            assertThat(result.getAcademicYear()).isEqualTo("2026/2027");
        }

        @Test
        @DisplayName("novi razred je aktivan i povezan s nastavnikom")
        void activeAndLinkedToTeacher() {
            classService.create(request(), teacherId);

            verify(classRepository).save(argThat(c ->
                    c.isActive() && c.getTeacher().equals(teacher)));
        }

        @Test
        @DisplayName("pridružuje navedene kolegije")
        void attachesCourses() {
            SchoolClassRequest req = request();
            req.setCourseIds(java.util.Set.of(courseId));
            classService.create(req, teacherId);

            verify(classRepository).save(argThat(c -> c.getCourses().contains(course)));
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg nastavnika")
        void throwsForUnknownTeacher() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> classService.create(request(), unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Upravljanje studentima")
    class Students {

        @Test
        @DisplayName("dodaje studenta u razred")
        void addsStudent() {
            classService.addStudent(classId, studentId);

            assertThat(schoolClass.getStudents()).contains(studentUser);
            verify(classRepository).save(schoolClass);
        }

        @Test
        @DisplayName("odbija dodavanje studenta koji je već u razredu")
        void rejectsDuplicateStudent() {
            schoolClass.getStudents().add(studentUser);

            assertThatThrownBy(() -> classService.addStudent(classId, studentId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already");
        }

        @Test
        @DisplayName("uklanja studenta iz razreda")
        void removesStudent() {
            schoolClass.getStudents().add(studentUser);

            classService.removeStudent(classId, studentId);

            assertThat(schoolClass.getStudents()).doesNotContain(studentUser);
        }

        @Test
        @DisplayName("vraća popis studenata")
        void listsStudents() {
            schoolClass.getStudents().add(studentUser);

            assertThat(classService.getStudents(classId)).hasSize(1);
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg studenta")
        void throwsForUnknownStudent() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> classService.addStudent(classId, unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Upravljanje kolegijima")
    class Courses {

        @Test
        @DisplayName("dodaje kolegij razredu")
        void addsCourse() {
            classService.addCourse(classId, courseId);

            assertThat(schoolClass.getCourses()).contains(course);
        }

        @Test
        @DisplayName("odbija dodavanje kolegija koji je već pridružen")
        void rejectsDuplicateCourse() {
            schoolClass.getCourses().add(course);

            assertThatThrownBy(() -> classService.addCourse(classId, courseId))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("uklanja kolegij iz razreda")
        void removesCourse() {
            schoolClass.getCourses().add(course);

            classService.removeCourse(classId, courseId);

            assertThat(schoolClass.getCourses()).doesNotContain(course);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći kolegij")
        void throwsForUnknownCourse() {
            UUID unknown = UUID.randomUUID();
            when(courseRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> classService.addCourse(classId, unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Brisanje razreda")
    class Deleting {

        @Test
        @DisplayName("briše prazan razred")
        void deletesEmptyClass() {
            classService.delete(classId);

            verify(classRepository).delete(schoolClass);
        }

        @Test
        @DisplayName("odbija brisanje razreda koji ima studente")
        void rejectsDeletingWithStudents() {
            schoolClass.getStudents().add(studentUser);

            assertThatThrownBy(() -> classService.delete(classId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("premjestite");

            verify(classRepository, never()).delete(any());
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći razred")
        void throwsForUnknownClass() {
            UUID unknown = UUID.randomUUID();
            when(classRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> classService.delete(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private SchoolClassRequest request() {
        SchoolClassRequest r = new SchoolClassRequest();
        r.setName("3.B");
        r.setDescription("Treći razred");
        r.setAcademicYear("2026/2027");
        return r;
    }
}
