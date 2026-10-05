package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.ExamAnswerRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.request.ExamRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.ExamService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ExamController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("ExamController")
class ExamControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ExamService examService;
    @MockitoBean private UserRepository userRepository;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID examId;
    private UUID courseId;
    private UUID attemptId;
    private UUID taskId;

    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        attemptId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);

        when(userRepository.findByEmail("ines@test.hr")).thenReturn(Optional.of(teacher));
        when(userRepository.findByEmail("ana@test.hr")).thenReturn(Optional.of(studentUser));
    }

    @Nested
    @DisplayName("Sastavljanje ispita")
    class Authoring {

        @Test
        @DisplayName("nastavnik smije kreirati ispit")
        void teacherCanCreate() throws Exception {
            when(examService.create(any(), any())).thenReturn(exam());

            mockMvc.perform(post("/api/exams")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(examRequest())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije kreirati ispit")
        void studentCannotCreate() throws Exception {
            mockMvc.perform(post("/api/exams")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(examRequest())))
                    .andExpect(status().isForbidden());

            verify(examService, never()).create(any(), any());
        }

        @Test
        @DisplayName("odbija ispit bez naziva")
        void rejectsMissingTitle() throws Exception {
            ExamRequest invalid = examRequest();
            invalid.setTitle(null);

            mockMvc.perform(post("/api/exams")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("nastavnik smije objaviti ispit")
        void teacherCanPublish() throws Exception {
            mockMvc.perform(patch("/api/exams/{id}/publish", examId).with(user(teacher)))
                    .andExpect(status().isOk());

            verify(examService).publish(examId);
        }

        @Test
        @DisplayName("student ne smije objaviti ispit")
        void studentCannotPublish() throws Exception {
            mockMvc.perform(patch("/api/exams/{id}/publish", examId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(examService, never()).publish(any());
        }

        @Test
        @DisplayName("student ne smije obrisati ispit")
        void studentCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/exams/{id}", examId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(examService, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Pregled pokušaja")
    class Reviewing {

        @Test
        @DisplayName("nastavnik vidi pokušaje na ispitu")
        void teacherSeesAttempts() throws Exception {
            when(examService.getAttemptsForExam(examId)).thenReturn(List.of(attempt()));

            mockMvc.perform(get("/api/exams/{id}/attempts", examId).with(user(teacher)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].examTitle").value("Kolokvij 1"));
        }

        @Test
        @DisplayName("student ne vidi tuđe pokušaje")
        void studentCannotSeeAttempts() throws Exception {
            mockMvc.perform(get("/api/exams/{id}/attempts", examId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(examService, never()).getAttemptsForExam(any());
        }

        @Test
        @DisplayName("nastavnik smije ocijeniti odgovor")
        void teacherCanGrade() throws Exception {
            when(examService.gradeAnswer(any(), any(), any(), any(), any()))
                    .thenReturn(attempt());

            mockMvc.perform(patch("/api/exams/attempts/{a}/tasks/{t}/grade", attemptId, taskId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"score\":12,\"feedback\":\"Dobro\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije ocjenjivati")
        void studentCannotGrade() throws Exception {
            mockMvc.perform(patch("/api/exams/attempts/{a}/tasks/{t}/grade", attemptId, taskId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"score\":12,\"feedback\":\"Dobro\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Rješavanje ispita")
    class Solving {

        @Test
        @DisplayName("student vidi dostupne ispite")
        void studentSeesAvailable() throws Exception {
            when(examService.getAvailableForStudent(eq(courseId), any()))
                    .thenReturn(List.of(exam()));

            mockMvc.perform(get("/api/exams/available/{courseId}", courseId)
                            .with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("Kolokvij 1"));
        }

        @Test
        @DisplayName("student može započeti ispit")
        void studentCanStart() throws Exception {
            when(examService.startAttempt(eq(examId), any())).thenReturn(attempt());

            mockMvc.perform(post("/api/exams/{id}/start", examId).with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(examService).startAttempt(eq(examId), eq(studentUser.getId()));
        }

        @Test
        @DisplayName("student može spremiti odgovor")
        void studentCanSaveAnswer() throws Exception {
            when(examService.saveAnswer(eq(examId), any(), any())).thenReturn(attempt());

            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setTaskId(taskId);
            request.setAnswerContent("odgovor");

            mockMvc.perform(put("/api/exams/{id}/answer", examId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("odbija odgovor bez zadatka")
        void rejectsAnswerWithoutTask() throws Exception {
            ExamAnswerRequest request = new ExamAnswerRequest();
            request.setAnswerContent("odgovor");

            mockMvc.perform(put("/api/exams/{id}/answer", examId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(examService, never()).saveAnswer(any(), any(), any());
        }

        @Test
        @DisplayName("student može predati ispit")
        void studentCanSubmit() throws Exception {
            when(examService.submitAttempt(eq(examId), any())).thenReturn(attempt());

            mockMvc.perform(post("/api/exams/{id}/submit", examId).with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(examService).submitAttempt(eq(examId), eq(studentUser.getId()));
        }

        @Test
        @DisplayName("student vidi vlastite pokušaje")
        void studentSeesOwnAttempts() throws Exception {
            when(examService.getMyAttempts(any())).thenReturn(List.of(attempt()));

            mockMvc.perform(get("/api/exams/attempts/my").with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(examService).getMyAttempts(studentUser.getId());
        }

        @Test
        @DisplayName("pojedini ispit dostupan je i studentu i nastavniku")
        void examVisibleToBoth() throws Exception {
            when(examService.getById(examId)).thenReturn(exam());

            mockMvc.perform(get("/api/exams/{id}", examId).with(user(studentUser)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/exams/{id}", examId).with(user(teacher)))
                    .andExpect(status().isOk());
        }
    }

    private ExamRequest examRequest() {
        ExamRequest r = new ExamRequest();
        r.setTitle("Kolokvij 1");
        r.setCourseId(courseId);
        r.setTaskIds(List.of(taskId));
        return r;
    }

    private ExamResponse exam() {
        return ExamResponse.builder()
                .id(examId)
                .title("Kolokvij 1")
                .courseId(courseId)
                .courseName("Uvod u programiranje")
                .taskCount(1)
                .maxScore(20)
                .isPublished(true)
                .isActive(true)
                .build();
    }

    private ExamAttemptResponse attempt() {
        return ExamAttemptResponse.builder()
                .id(attemptId)
                .examId(examId)
                .examTitle("Kolokvij 1")
                .studentId(studentUser.getId())
                .studentName("Ana Anić")
                .studentEmail("ana@test.hr")
                .status(ExamAttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .maxScore(20)
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