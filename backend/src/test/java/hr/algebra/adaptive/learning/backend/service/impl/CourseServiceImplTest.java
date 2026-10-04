package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.CourseRequest;
import hr.algebra.adaptive.learning.backend.dto.response.CourseResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.CourseRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CourseServiceImpl")
class CourseServiceImplTest {

    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CourseServiceImpl courseService;

    private UUID courseId;
    private UUID teacherId;
    private User teacher;
    private Course course;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        teacherId = UUID.randomUUID();

        teacher = User.builder()
                .email("ines@test.hr")
                .firstName("Ines")
                .lastName("Inić")
                .role(UserRole.TEACHER)
                .build();
        teacher.setId(teacherId);

        course = Course.builder()
                .name("Uvod u programiranje")
                .description("Osnove jezika C")
                .languageType(LanguageType.C)
                .createdBy(teacher)
                .isActive(true)
                .outcomes(new ArrayList<>())
                .build();
        course.setId(courseId);

        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> {
            Course c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
    }

    @Nested
    @DisplayName("Kreiranje kolegija")
    class Creating {

        @Test
        @DisplayName("sprema kolegij s predanim podacima")
        void savesWithGivenData() {
            CourseResponse result = courseService.create(request(), teacherId);

            assertThat(result.getName()).isEqualTo("Novi kolegij");
            assertThat(result.getLanguageType()).isEqualTo(LanguageType.PYTHON);
        }

        @Test
        @DisplayName("novi kolegij je aktivan")
        void newCourseIsActive() {
            courseService.create(request(), teacherId);

            verify(courseRepository).save(argThat(Course::isActive));
        }

        @Test
        @DisplayName("povezuje kolegij s autorom")
        void linksCreator() {
            courseService.create(request(), teacherId);

            verify(courseRepository).save(argThat(c -> c.getCreatedBy().equals(teacher)));
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg autora")
        void throwsForUnknownCreator() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.create(request(), unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Dohvat kolegija")
    class Fetching {

        @Test
        @DisplayName("vraća kolegij po identifikatoru")
        void returnsById() {
            assertThat(courseService.getById(courseId).getName())
                    .isEqualTo("Uvod u programiranje");
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći kolegij")
        void throwsForUnknown() {
            UUID unknown = UUID.randomUUID();
            when(courseRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.getById(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("popis sadrži samo aktivne kolegije")
        void listsOnlyActive() {
            when(courseRepository.findByIsActiveTrue()).thenReturn(List.of(course));

            assertThat(courseService.getAll()).hasSize(1);
            verify(courseRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("filtrira po programskom jeziku")
        void filtersByLanguage() {
            when(courseRepository.findByLanguageType(LanguageType.C))
                    .thenReturn(List.of(course));

            assertThat(courseService.getByLanguageType(LanguageType.C)).hasSize(1);
        }

        @Test
        @DisplayName("vraća kolegije pojedinog nastavnika")
        void returnsByTeacher() {
            when(courseRepository.findByCreatedById(teacherId)).thenReturn(List.of(course));

            assertThat(courseService.getByTeacher(teacherId)).hasSize(1);
        }

        @Test
        @DisplayName("vraća prazan popis kad nema kolegija")
        void emptyWhenNone() {
            when(courseRepository.findByIsActiveTrue()).thenReturn(List.of());

            assertThat(courseService.getAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Izmjena kolegija")
    class Updating {

        @Test
        @DisplayName("mijenja naziv i opis")
        void updatesNameAndDescription() {
            CourseRequest req = request();
            req.setName("Napredno programiranje");
            req.setDescription("Novi opis");

            CourseResponse result = courseService.update(courseId, req);

            assertThat(result.getName()).isEqualTo("Napredno programiranje");
            assertThat(result.getDescription()).isEqualTo("Novi opis");
        }

        @Test
        @DisplayName("mijenja programski jezik")
        void updatesLanguage() {
            CourseRequest req = request();
            req.setLanguageType(LanguageType.CSHARP);

            courseService.update(courseId, req);

            assertThat(course.getLanguageType()).isEqualTo(LanguageType.CSHARP);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći kolegij")
        void throwsForUnknown() {
            UUID unknown = UUID.randomUUID();
            when(courseRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.update(unknown, request()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Aktivacija i brisanje")
    class Lifecycle {

        @Test
        @DisplayName("deaktivira kolegij bez brisanja")
        void deactivatesWithoutDeleting() {
            courseService.deactivate(courseId);

            assertThat(course.isActive()).isFalse();
            verify(courseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("ponovno aktivira kolegij")
        void reactivates() {
            course.setActive(false);

            courseService.activate(courseId);

            assertThat(course.isActive()).isTrue();
        }

        @Test
        @DisplayName("briše kolegij")
        void deletes() {
            courseService.delete(courseId);

            verify(courseRepository).delete(course);
        }

        @Test
        @DisplayName("baca iznimku pri brisanju nepostojećeg kolegija")
        void throwsWhenDeletingUnknown() {
            UUID unknown = UUID.randomUUID();
            when(courseRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.delete(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private CourseRequest request() {
        CourseRequest r = new CourseRequest();
        r.setName("Novi kolegij");
        r.setDescription("Opis kolegija");
        r.setLanguageType(LanguageType.PYTHON);
        return r;
    }
}
