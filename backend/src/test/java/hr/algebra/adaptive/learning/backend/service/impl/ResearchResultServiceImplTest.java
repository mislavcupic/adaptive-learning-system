package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Assessment;
import hr.algebra.adaptive.learning.backend.domain.entity.AssessmentAttempt;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.dto.ml.MLAncovaRequest;
import hr.algebra.adaptive.learning.backend.dto.ml.MLAncovaResponse;
import hr.algebra.adaptive.learning.backend.dto.response.ResearchResultResponse;
import hr.algebra.adaptive.learning.backend.repository.AssessmentAttemptRepository;
import hr.algebra.adaptive.learning.backend.service.MLServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResearchResultServiceImpl")
class ResearchResultServiceImplTest {

    @Mock private AssessmentAttemptRepository attemptRepository;
    @Mock private MLServiceClient mlServiceClient;

    @InjectMocks private ResearchResultServiceImpl researchService;

    private User experimentalStudent;
    private User controlStudent;

    @BeforeEach
    void setUp() {
        experimentalStudent = student("ana@test.hr", "Ana", ResearchGroup.EXPERIMENTAL);
        controlStudent = student("ivo@test.hr", "Ivo", ResearchGroup.CONTROL);
    }

    @Nested
    @DisplayName("Sastavljanje rezultata")
    class BuildingResults {

        @Test
        @DisplayName("spaja pretest i posttest istog studenta u jedan zapis")
        void mergesBothTestsPerStudent() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(attempt(experimentalStudent, AssessmentType.PRETEST, 12, 20),
                    attempt(experimentalStudent, AssessmentType.POSTTEST, 18, 20)));

            List<ResearchResultResponse> results = researchService.getResults();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPretestScore()).isEqualTo(12);
            assertThat(results.get(0).getPosttestScore()).isEqualTo(18);
        }

        @Test
        @DisplayName("računa postotak iz bodova i maksimuma")
        void computesPercentage() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 15, 20)));

            ResearchResultResponse result = researchService.getResults().get(0);

            assertThat(result.getPretestPercentage()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("postotak je nula kad je maksimum nula")
        void zeroPercentageWhenNoMaxScore() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 0, 0)));

            ResearchResultResponse result = researchService.getResults().get(0);

            assertThat(result.getPretestPercentage()).isZero();
        }

        @Test
        @DisplayName("zadržava pripadnost istraživačkoj skupini")
        void keepsResearchGroup() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 10, 20)));

            assertThat(researchService.getResults().get(0).getResearchGroup())
                    .isEqualTo(ResearchGroup.EXPERIMENTAL);
        }

        @Test
        @DisplayName("razdvaja zapise različitih studenata")
        void separatesStudents() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(attempt(experimentalStudent, AssessmentType.PRETEST, 10, 20),
                    attempt(controlStudent, AssessmentType.PRETEST, 14, 20)));

            assertThat(researchService.getResults()).hasSize(2);
        }

        @Test
        @DisplayName("vraća prazan popis kad nema pokušaja")
        void emptyWhenNoAttempts() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of());

            assertThat(researchService.getResults()).isEmpty();
        }

        @Test
        @DisplayName("student samo s pretestom ostaje bez posttesta")
        void keepsPartialRecord() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 10, 20)));

            ResearchResultResponse result = researchService.getResults().get(0);

            assertThat(result.getPretestScore()).isEqualTo(10);
            assertThat(result.getPosttestScore()).isNull();
        }
    }

    @Nested
    @DisplayName("Priprema podataka za ANCOVA analizu")
    class AncovaPreparation {

        @Test
        @DisplayName("uključuje samo studente s oba testa")
        void includesOnlyCompletePairs() {
            User incomplete = student("marko@test.hr", "Marko", ResearchGroup.CONTROL);

            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 10, 20),
                    attempt(experimentalStudent, AssessmentType.POSTTEST, 16, 20),
                    attempt(controlStudent, AssessmentType.PRETEST, 12, 20),
                    attempt(controlStudent, AssessmentType.POSTTEST, 14, 20),
                    attempt(incomplete, AssessmentType.PRETEST, 11, 20)));
            when(mlServiceClient.runAncova(any())).thenReturn(new MLAncovaResponse());

            researchService.getAncova();

            ArgumentCaptor<MLAncovaRequest> captor =
                    ArgumentCaptor.forClass(MLAncovaRequest.class);
            verify(mlServiceClient).runAncova(captor.capture());

            assertThat(captor.getValue().getRecords()).hasSize(2);
        }

        @Test
        @DisplayName("ne poziva ML servis kad postoji samo jedna skupina")
        void skipsMlWhenSingleGroup() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 10, 20),
                    attempt(experimentalStudent, AssessmentType.POSTTEST, 16, 20)));

            researchService.getAncova();

            verify(mlServiceClient, never()).runAncova(any());
        }

        @Test
        @DisplayName("ne poziva ML servis kad nema podataka")
        void skipsMlWhenNoData() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of());

            researchService.getAncova();

            verify(mlServiceClient, never()).runAncova(any());
        }

        @Test
        @DisplayName("vraća odgovor i kad podataka nema dovoljno")
        void returnsResponseWithoutData() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of());

            assertThat(researchService.getAncova()).isNotNull();
        }

        @Test
        @DisplayName("poziva ML servis kad obje skupine imaju potpune podatke")
        void callsMlWithBothGroups() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 10, 20),
                    attempt(experimentalStudent, AssessmentType.POSTTEST, 18, 20),
                    attempt(controlStudent, AssessmentType.PRETEST, 11, 20),
                    attempt(controlStudent, AssessmentType.POSTTEST, 13, 20)));
            when(mlServiceClient.runAncova(any())).thenReturn(new MLAncovaResponse());

            researchService.getAncova();

            ArgumentCaptor<MLAncovaRequest> captor =
                    ArgumentCaptor.forClass(MLAncovaRequest.class);
            verify(mlServiceClient).runAncova(captor.capture());

            assertThat(captor.getValue().getRecords()).hasSize(2);
        }

        @Test
        @DisplayName("šalje postotke, ne sirove bodove")
        void sendsPercentages() {
            when(attemptRepository.findAllCompletedForResearch()).thenReturn(List.of(
                    attempt(experimentalStudent, AssessmentType.PRETEST, 10, 20),
                    attempt(experimentalStudent, AssessmentType.POSTTEST, 18, 20),
                    attempt(controlStudent, AssessmentType.PRETEST, 11, 20),
                    attempt(controlStudent, AssessmentType.POSTTEST, 13, 20)));
            when(mlServiceClient.runAncova(any())).thenReturn(new MLAncovaResponse());

            researchService.getAncova();

            ArgumentCaptor<MLAncovaRequest> captor =
                    ArgumentCaptor.forClass(MLAncovaRequest.class);
            verify(mlServiceClient).runAncova(captor.capture());

            var first = captor.getValue().getRecords().get(0);
            assertThat(first.getPretest()).isEqualTo(50.0);
            assertThat(first.getPosttest()).isEqualTo(90.0);
        }
    }

    private User student(String email, String firstName, ResearchGroup group) {
        User u = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName("Testić")
                .researchGroup(group)
                .build();
        u.setId(UUID.randomUUID());
        return u;
    }

    private AssessmentAttempt attempt(User student, AssessmentType type,
                                      int score, int maxScore) {
        Assessment assessment = Assessment.builder()
                .title(type == AssessmentType.PRETEST ? "Predtest" : "Posttest")
                .assessmentType(type)
                .build();
        assessment.setId(UUID.randomUUID());

        AssessmentAttempt a = AssessmentAttempt.builder()
                .student(student)
                .assessment(assessment)
                .score(score)
                .maxScore(maxScore)
                .isCompleted(true)
                .build();
        a.setId(UUID.randomUUID());
        return a;
    }
}