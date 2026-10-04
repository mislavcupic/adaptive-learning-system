package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResearchGroupServiceImpl")
class ResearchGroupServiceImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private ResearchGroupServiceImpl groupService;

    private UUID classId;

    @BeforeEach
    void setUp() {
        classId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Uravnoteženje skupina")
    class Balancing {

        @Test
        @DisplayName("dodjeljuje kontrolnu kad eksperimentalna ima više članova")
        void assignsControlWhenExperimentalLarger() {
            stubCounts(5, 3);

            assertThat(groupService.assignGroup(classId))
                    .isEqualTo(ResearchGroup.CONTROL);
        }

        @Test
        @DisplayName("dodjeljuje eksperimentalnu kad kontrolna ima više članova")
        void assignsExperimentalWhenControlLarger() {
            stubCounts(2, 6);

            assertThat(groupService.assignGroup(classId))
                    .isEqualTo(ResearchGroup.EXPERIMENTAL);
        }

        @Test
        @DisplayName("razlika od jednog člana je dovoljna za usmjeravanje")
        void correctsSingleMemberDifference() {
            stubCounts(1, 0);

            assertThat(groupService.assignGroup(classId))
                    .isEqualTo(ResearchGroup.CONTROL);
        }

        @Test
        @DisplayName("odluka je uvijek ista dok je razlika prisutna")
        void deterministicWhenUnbalanced() {
            stubCounts(4, 1);

            for (int i = 0; i < 20; i++) {
                assertThat(groupService.assignGroup(classId))
                        .isEqualTo(ResearchGroup.CONTROL);
            }
        }
    }

    @Nested
    @DisplayName("Slučajnost pri izjednačenim skupinama")
    class Randomness {

        @Test
        @DisplayName("pri izjednačenom stanju dodjeljuje obje skupine")
        void bothGroupsOccurWhenTied() {
            stubCounts(0, 0);

            Map<ResearchGroup, Integer> counts = new HashMap<>();
            for (int i = 0; i < 200; i++) {
                counts.merge(groupService.assignGroup(classId), 1, Integer::sum);
            }

            assertThat(counts).containsKeys(
                    ResearchGroup.EXPERIMENTAL, ResearchGroup.CONTROL);
        }

        @Test
        @DisplayName("raspodjela je približno ravnomjerna")
        void roughlyEvenSplit() {
            stubCounts(3, 3);

            int experimental = 0;
            int total = 400;
            for (int i = 0; i < total; i++) {
                if (groupService.assignGroup(classId) == ResearchGroup.EXPERIMENTAL) {
                    experimental++;
                }
            }

            assertThat(experimental).isBetween(140, 260);
        }

        @Test
        @DisplayName("niz dodjela nije predvidljiv")
        void sequenceIsNotPredictable() {
            stubCounts(2, 2);

            StringBuilder first = new StringBuilder();
            StringBuilder second = new StringBuilder();

            for (int i = 0; i < 30; i++) {
                first.append(groupService.assignGroup(classId) == ResearchGroup.EXPERIMENTAL ? 'E' : 'K');
            }
            for (int i = 0; i < 30; i++) {
                second.append(groupService.assignGroup(classId) == ResearchGroup.EXPERIMENTAL ? 'E' : 'K');
            }

            assertThat(first.toString()).isNotEqualTo(second.toString());
        }
    }

    @Nested
    @DisplayName("Odvojenost razreda")
    class ClassIsolation {

        @Test
        @DisplayName("broji članove samo unutar zadanog razreda")
        void countsOnlyWithinClass() {
            UUID otherClass = UUID.randomUUID();

            when(userRepository.countBySchoolClassIdAndResearchGroup(
                    classId, ResearchGroup.EXPERIMENTAL)).thenReturn(0L);
            when(userRepository.countBySchoolClassIdAndResearchGroup(
                    classId, ResearchGroup.CONTROL)).thenReturn(4L);
            when(userRepository.countBySchoolClassIdAndResearchGroup(
                    otherClass, ResearchGroup.EXPERIMENTAL)).thenReturn(9L);
            when(userRepository.countBySchoolClassIdAndResearchGroup(
                    otherClass, ResearchGroup.CONTROL)).thenReturn(0L);

            assertThat(groupService.assignGroup(classId))
                    .isEqualTo(ResearchGroup.EXPERIMENTAL);
            assertThat(groupService.assignGroup(otherClass))
                    .isEqualTo(ResearchGroup.CONTROL);
        }
    }

    private void stubCounts(long experimental, long control) {
        when(userRepository.countBySchoolClassIdAndResearchGroup(
                classId, ResearchGroup.EXPERIMENTAL)).thenReturn(experimental);
        when(userRepository.countBySchoolClassIdAndResearchGroup(
                classId, ResearchGroup.CONTROL)).thenReturn(control);
    }
}