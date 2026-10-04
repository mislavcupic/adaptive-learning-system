package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.dto.request.LearningOutcomeRequest;
import hr.algebra.adaptive.learning.backend.dto.response.LearningOutcomeResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.CourseRepository;
import hr.algebra.adaptive.learning.backend.repository.LearningOutcomeRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LearningOutcomeServiceImpl")
class LearningOutcomeServiceImplTest {

    @Mock private LearningOutcomeRepository outcomeRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks private LearningOutcomeServiceImpl outcomeService;

    private UUID outcomeId;
    private UUID courseId;
    private Course course;
    private LearningOutcome outcome;

    @BeforeEach
    void setUp() {
        outcomeId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        course = Course.builder()
                .name("Uvod u programiranje")
                .languageType(LanguageType.C)
                .isActive(true)
                .build();
        course.setId(courseId);

        outcome = LearningOutcome.builder()
                .name("Petlje")
                .description("Razumijevanje petlji")
                .orderIndex(0)
                .course(course)
                .build();
        outcome.setId(outcomeId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(outcomeRepository.findById(outcomeId)).thenReturn(Optional.of(outcome));
        when(outcomeRepository.save(any(LearningOutcome.class))).thenAnswer(i -> {
            LearningOutcome o = i.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });
    }

    @Nested
    @DisplayName("Kreiranje ishoda")
    class Creating {

        @Test
        @DisplayName("sprema ishod s predanim podacima")
        void savesWithGivenData() {
            LearningOutcomeResponse result = outcomeService.create(request());

            assertThat(result.getName()).isEqualTo("Polja");
            assertThat(result.getOrderIndex()).isEqualTo(2);
        }

        @Test
        @DisplayName("povezuje ishod s kolegijem")
        void linksCourse() {
            outcomeService.create(request());

            verify(outcomeRepository).save(argThat(o -> o.getCourse().equals(course)));
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći kolegij")
        void throwsForUnknownCourse() {
            UUID unknown = UUID.randomUUID();
            when(courseRepository.findById(unknown)).thenReturn(Optional.empty());

            LearningOutcomeRequest req = request();
            req.setCourseId(unknown);

            assertThatThrownBy(() -> outcomeService.create(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Dohvat ishoda")
    class Fetching {

        @Test
        @DisplayName("vraća ishod po identifikatoru")
        void returnsById() {
            assertThat(outcomeService.getById(outcomeId).getName()).isEqualTo("Petlje");
        }

        @Test
        @DisplayName("vraća ishode kolegija poredane po redoslijedu")
        void returnsByCourseOrdered() {
            when(outcomeRepository.findByCourseIdOrderByOrderIndexAsc(courseId))
                    .thenReturn(List.of(outcome));

            assertThat(outcomeService.getByCourse(courseId)).hasSize(1);
            verify(outcomeRepository).findByCourseIdOrderByOrderIndexAsc(courseId);
        }

        @Test
        @DisplayName("vraća sve ishode")
        void returnsAll() {
            when(outcomeRepository.findAll()).thenReturn(List.of(outcome));

            assertThat(outcomeService.getAll()).hasSize(1);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći ishod")
        void throwsForUnknown() {
            UUID unknown = UUID.randomUUID();
            when(outcomeRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> outcomeService.getById(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Izmjena i brisanje")
    class Modifying {

        @Test
        @DisplayName("mijenja naziv i opis")
        void updatesNameAndDescription() {
            LearningOutcomeRequest req = request();
            req.setName("Napredne petlje");
            req.setDescription("Ugniježđene petlje");

            LearningOutcomeResponse result = outcomeService.update(outcomeId, req);

            assertThat(result.getName()).isEqualTo("Napredne petlje");
            assertThat(result.getDescription()).isEqualTo("Ugniježđene petlje");
        }

        @Test
        @DisplayName("briše ishod")
        void deletes() {
            outcomeService.delete(outcomeId);

            verify(outcomeRepository).delete(outcome);
        }

        @Test
        @DisplayName("baca iznimku pri izmjeni nepostojećeg ishoda")
        void throwsWhenUpdatingUnknown() {
            UUID unknown = UUID.randomUUID();
            when(outcomeRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> outcomeService.update(unknown, request()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Promjena redoslijeda")
    class Reordering {

        @Test
        @DisplayName("dodjeljuje redne brojeve prema zadanom nizu")
        void assignsSequentialIndexes() {
            LearningOutcome first = outcomeWith("Prvi");
            LearningOutcome second = outcomeWith("Drugi");
            LearningOutcome third = outcomeWith("Treći");

            when(outcomeRepository.findById(first.getId())).thenReturn(Optional.of(first));
            when(outcomeRepository.findById(second.getId())).thenReturn(Optional.of(second));
            when(outcomeRepository.findById(third.getId())).thenReturn(Optional.of(third));

            outcomeService.reorder(courseId,
                    List.of(third.getId(), first.getId(), second.getId()));

            assertThat(third.getOrderIndex()).isZero();
            assertThat(first.getOrderIndex()).isEqualTo(1);
            assertThat(second.getOrderIndex()).isEqualTo(2);
        }

        @Test
        @DisplayName("sprema svaki izmijenjeni ishod")
        void savesEachOutcome() {
            LearningOutcome first = outcomeWith("Prvi");
            LearningOutcome second = outcomeWith("Drugi");

            when(outcomeRepository.findById(first.getId())).thenReturn(Optional.of(first));
            when(outcomeRepository.findById(second.getId())).thenReturn(Optional.of(second));

            outcomeService.reorder(courseId, List.of(first.getId(), second.getId()));

            verify(outcomeRepository, times(2)).save(any(LearningOutcome.class));
        }

        @Test
        @DisplayName("baca iznimku kad niz sadrži nepostojeći ishod")
        void throwsForUnknownInList() {
            UUID unknown = UUID.randomUUID();
            when(outcomeRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> outcomeService.reorder(courseId, List.of(unknown)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("prazan niz ne mijenja ništa")
        void emptyListDoesNothing() {
            outcomeService.reorder(courseId, List.of());

            verify(outcomeRepository, never()).save(any());
        }
    }

    private LearningOutcome outcomeWith(String name) {
        LearningOutcome o = LearningOutcome.builder()
                .name(name)
                .orderIndex(99)
                .course(course)
                .build();
        o.setId(UUID.randomUUID());
        return o;
    }

    private LearningOutcomeRequest request() {
        LearningOutcomeRequest r = new LearningOutcomeRequest();
        r.setName("Polja");
        r.setDescription("Rad s poljima");
        r.setOrderIndex(2);
        r.setCourseId(courseId);
        return r;
    }
}
