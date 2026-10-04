package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import hr.algebra.adaptive.learning.backend.domain.entity.SkillMastery;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.response.SkillMasteryResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.SkillMasteryRepository;
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
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SkillMasteryServiceImpl")
class SkillMasteryServiceImplTest {

    @Mock private SkillMasteryRepository skillMasteryRepository;

    @InjectMocks private SkillMasteryServiceImpl masteryService;

    private UUID studentId;
    private User student;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        student = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .build();
        student.setId(studentId);
    }

    @Nested
    @DisplayName("Profil znanja studenta")
    class StudentProfile {

        @Test
        @DisplayName("vraća sve procjene studenta")
        void returnsAllSkills() {
            when(skillMasteryRepository.findByStudentId(studentId))
                    .thenReturn(List.of(mastery("Petlje", 0.7), mastery("Polja", 0.4)));

            List<SkillMasteryResponse> result = masteryService.getByStudent(studentId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SkillMasteryResponse::getSkillName)
                    .containsExactly("Petlje", "Polja");
        }

        @Test
        @DisplayName("vraća prazan popis kad student nema procjena")
        void emptyWhenNoData() {
            when(skillMasteryRepository.findByStudentId(studentId)).thenReturn(List.of());

            assertThat(masteryService.getByStudent(studentId)).isEmpty();
        }

        @Test
        @DisplayName("prenosi broj pokušaja i točnih odgovora")
        void carriesAttemptCounts() {
            when(skillMasteryRepository.findByStudentId(studentId))
                    .thenReturn(List.of(mastery("Petlje", 0.7)));

            SkillMasteryResponse result = masteryService.getByStudent(studentId).get(0);

            assertThat(result.getAttemptsCount()).isEqualTo(5);
            assertThat(result.getCorrectCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("prenosi naziv ishoda učenja kad postoji")
        void carriesOutcomeName() {
            SkillMastery m = mastery("Petlje", 0.7);
            LearningOutcome outcome = new LearningOutcome();
            outcome.setId(UUID.randomUUID());
            outcome.setName("Razumijevanje petlji");
            m.setOutcome(outcome);

            when(skillMasteryRepository.findByStudentId(studentId)).thenReturn(List.of(m));

            SkillMasteryResponse result = masteryService.getByStudent(studentId).get(0);

            assertThat(result.getOutcomeName()).isEqualTo("Razumijevanje petlji");
        }
    }

    @Nested
    @DisplayName("Pojedina vještina")
    class SingleSkill {

        @Test
        @DisplayName("vraća procjenu za traženu vještinu")
        void returnsRequestedSkill() {
            when(skillMasteryRepository.findByStudentIdAndSkillName(studentId, "Petlje"))
                    .thenReturn(Optional.of(mastery("Petlje", 0.85)));

            SkillMasteryResponse result =
                    masteryService.getByStudentAndSkill(studentId, "Petlje");

            assertThat(result.getSkillName()).isEqualTo("Petlje");
            assertThat(result.getMasteryLevel()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("baca iznimku kad procjena ne postoji")
        void throwsWhenMissing() {
            when(skillMasteryRepository.findByStudentIdAndSkillName(studentId, "Rekurzija"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    masteryService.getByStudentAndSkill(studentId, "Rekurzija"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Rekurzija");
        }
    }

    @Nested
    @DisplayName("Prosječna razina znanja")
    class AverageMastery {

        @Test
        @DisplayName("računa prosjek svih vještina")
        void averagesAllSkills() {
            when(skillMasteryRepository.findByStudentId(studentId))
                    .thenReturn(List.of(
                            mastery("Petlje", 0.6),
                            mastery("Polja", 0.8),
                            mastery("Grananje", 1.0)));

            assertThat(masteryService.getAverageMastery(studentId))
                    .isCloseTo(0.8, within(0.001));
        }

        @Test
        @DisplayName("vraća nulu kad nema procjena")
        void zeroWhenNoSkills() {
            when(skillMasteryRepository.findByStudentId(studentId)).thenReturn(List.of());

            assertThat(masteryService.getAverageMastery(studentId)).isZero();
        }

        @Test
        @DisplayName("jedna vještina daje vlastitu vrijednost")
        void singleSkillIsItsOwnAverage() {
            when(skillMasteryRepository.findByStudentId(studentId))
                    .thenReturn(List.of(mastery("Petlje", 0.42)));

            assertThat(masteryService.getAverageMastery(studentId))
                    .isCloseTo(0.42, within(0.001));
        }

        @Test
        @DisplayName("uračunava i vještine s nultom razinom")
        void includesZeroLevels() {
            when(skillMasteryRepository.findByStudentId(studentId))
                    .thenReturn(List.of(mastery("Petlje", 1.0), mastery("Polja", 0.0)));

            assertThat(masteryService.getAverageMastery(studentId))
                    .isCloseTo(0.5, within(0.001));
        }
    }

    private SkillMastery mastery(String skillName, double level) {
        SkillMastery m = new SkillMastery();
        m.setId(UUID.randomUUID());
        m.setStudent(student);
        m.setSkillName(skillName);
        m.setMasteryLevel(level);
        m.setAttemptsCount(5);
        m.setCorrectCount(3);
        return m;
    }
}
