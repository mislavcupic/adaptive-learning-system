package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AssessmentType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentAttemptRequest;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentRequest;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.assessment.AssessmentResponse;
import hr.algebra.adaptive.learning.backend.exception.AssessmentAlreadyCompletedException;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.AssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AssessmentController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("AssessmentController")
class AssessmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AssessmentService assessmentService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID assessmentId;
    private UUID courseId;
    private UUID attemptId;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        assessmentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        attemptId = UUID.randomUUID();

        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Sastavljanje testa")
    class Authoring {

        @Test
        @DisplayName("nastavnik smije kreirati test")
        void teacherCanCreate() throws Exception {
            when(assessmentService.create(any(), any())).thenReturn(assessment());

            mockMvc.perform(post("/api/assessments")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije kreirati test")
        void studentCannotCreate() throws Exception {
            mockMvc.perform(post("/api/assessments")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());

            verify(assessmentService, never()).create(any(), any());
        }

        @Test
        @DisplayName("student ne smije brisati pitanja")
        void studentCannotDeleteQuestion() throws Exception {
            mockMvc.perform(delete("/api/assessments/questions/{id}", UUID.randomUUID())
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(assessmentService, never()).deleteQuestion(any());
        }

        @Test
        @DisplayName("nastavnik smije brisati pitanja")
        void teacherCanDeleteQuestion() throws Exception {
            UUID questionId = UUID.randomUUID();

            mockMvc.perform(delete("/api/assessments/questions/{id}", questionId)
                            .with(user(teacher)))
                    .andExpect(status().isNoContent());

            verify(assessmentService).deleteQuestion(questionId);
        }
    }

    @Nested
    @DisplayName("Rješavanje testa")
    class Solving {

        @Test
        @DisplayName("student može započeti test")
        void studentCanStart() throws Exception {
            when(assessmentService.startAttempt(eq(assessmentId), any()))
                    .thenReturn(attempt());

            mockMvc.perform(post("/api/assessments/{id}/start", assessmentId)
                            .with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(assessmentService).startAttempt(assessmentId, studentUser.getId());
        }

        @Test
        @DisplayName("nastavnik ne može rješavati test")
        void teacherCannotStart() throws Exception {
            mockMvc.perform(post("/api/assessments/{id}/start", assessmentId)
                            .with(user(teacher)))
                    .andExpect(status().isForbidden());

            verify(assessmentService, never()).startAttempt(any(), any());
        }

        @Test
        @DisplayName("ponovno rješavanje vraća 409")
        void rejectsRetakeWithConflict() throws Exception {
            when(assessmentService.startAttempt(any(), any()))
                    .thenThrow(new AssessmentAlreadyCompletedException(
                            "Ovaj test ste već riješili."));

            mockMvc.perform(post("/api/assessments/{id}/start", assessmentId)
                            .with(user(studentUser)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("student može predati test")
        void studentCanSubmit() throws Exception {
            when(assessmentService.submitAttempt(any(), any())).thenReturn(attempt());

            AssessmentAttemptRequest request = new AssessmentAttemptRequest();
            request.setAssessmentId(assessmentId);
            request.setAnswers(new HashMap<>());

            mockMvc.perform(post("/api/assessments/submit")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student vidi vlastite pokušaje")
        void studentSeesOwnAttempts() throws Exception {
            when(assessmentService.getStudentAttempts(studentUser.getId()))
                    .thenReturn(List.of(attempt()));

            mockMvc.perform(get("/api/assessments/attempts/my").with(user(studentUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("nastavnik nema rutu za vlastite pokušaje")
        void teacherHasNoOwnAttempts() throws Exception {
            mockMvc.perform(get("/api/assessments/attempts/my").with(user(teacher)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Zajednički dohvat")
    class Common {

        @Test
        @DisplayName("test je dostupan i studentu i nastavniku")
        void visibleToBoth() throws Exception {
            when(assessmentService.getById(assessmentId)).thenReturn(assessment());

            mockMvc.perform(get("/api/assessments/{id}", assessmentId).with(user(studentUser)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/assessments/{id}", assessmentId).with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("vraća testove kolegija")
        void returnsByCourse() throws Exception {
            when(assessmentService.getByCourse(courseId)).thenReturn(List.of(assessment()));

            mockMvc.perform(get("/api/assessments/course/{id}", courseId)
                            .with(user(studentUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("vraća pojedini pokušaj")
        void returnsAttempt() throws Exception {
            when(assessmentService.getAttempt(attemptId)).thenReturn(attempt());

            mockMvc.perform(get("/api/assessments/attempts/{id}", attemptId)
                            .with(user(teacher)))
                    .andExpect(status().isOk());
        }
    }

    private AssessmentRequest request() {
        AssessmentRequest r = new AssessmentRequest();
        r.setTitle("Predtest");
        r.setCourseId(courseId);
        r.setAssessmentType(AssessmentType.PRETEST);
        return r;
    }

    private AssessmentResponse assessment() {
        return AssessmentResponse.builder()
                .id(assessmentId)
                .title("Predtest")
                .courseId(courseId)
                .assessmentType(AssessmentType.PRETEST)
                .build();
    }

    private AssessmentAttemptResponse attempt() {
        return AssessmentAttemptResponse.builder()
                .id(attemptId)
                .assessmentId(assessmentId)
                .score(0)
                .maxScore(30)
                .isCompleted(false)
                .build();
    }

    private User account(String email, UserRole role) {
        User u = User.builder()
                .email(email)
                .password("hash")
                .firstName("Ime")
                .lastName("Prezime")
                .role(role)
                .isActive(true)
                .emailVerified(true)
                .build();
        u.setId(UUID.randomUUID());
        return u;
    }
}
