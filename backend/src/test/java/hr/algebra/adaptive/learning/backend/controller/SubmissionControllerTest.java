package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.SubmissionStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.SubmissionRequest;
import hr.algebra.adaptive.learning.backend.dto.response.StudentSubmissionsOverviewResponse;
import hr.algebra.adaptive.learning.backend.dto.response.SubmissionResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.SubmissionOverviewService;
import hr.algebra.adaptive.learning.backend.service.SubmissionService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SubmissionController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("SubmissionController")
class SubmissionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SubmissionService submissionService;
    @MockitoBean private SubmissionOverviewService submissionOverviewService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID submissionId;
    private UUID taskId;
    private User teacher;
    private User admin;
    private User studentUser;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        teacher = account("ines@test.hr", UserRole.TEACHER);
        admin = account("admin@test.hr", UserRole.ADMIN);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Predaja zadatka")
    class Submitting {

        @Test
        @DisplayName("student smije predati rješenje")
        void studentCanSubmit() throws Exception {
            when(submissionService.submit(any(), any())).thenReturn(submission());

            mockMvc.perform(post("/api/submissions")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("nastavnik ne smije predavati rješenja")
        void teacherCannotSubmit() throws Exception {
            mockMvc.perform(post("/api/submissions")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());

            verify(submissionService, never()).submit(any(), any());
        }

        @Test
        @DisplayName("predaju povezuje s prijavljenim studentom")
        void linksSubmissionToLoggedInStudent() throws Exception {
            when(submissionService.submit(any(), any())).thenReturn(submission());

            mockMvc.perform(post("/api/submissions")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated());

            verify(submissionService).submit(any(), eq(studentUser.getId()));
        }
    }

    @Nested
    @DisplayName("Pregled vlastitih predaja")
    class OwnSubmissions {

        @Test
        @DisplayName("student vidi svoje predaje")
        void studentSeesOwn() throws Exception {
            when(submissionService.getByStudent(studentUser.getId()))
                    .thenReturn(List.of(submission()));

            mockMvc.perform(get("/api/submissions/my").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].taskTitle").value("Zbroj niza"));
        }

        @Test
        @DisplayName("nastavnik nema rutu za vlastite predaje")
        void teacherHasNoOwnRoute() throws Exception {
            mockMvc.perform(get("/api/submissions/my").with(user(teacher)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("student vidi broj svojih predaja")
        void studentSeesOwnCount() throws Exception {
            when(submissionService.countByStudent(studentUser.getId())).thenReturn(4L);

            mockMvc.perform(get("/api/submissions/my/count").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(4));
        }
    }

    @Nested
    @DisplayName("Nastavnički pregled")
    class TeacherAccess {

        @Test
        @DisplayName("nastavnik vidi sve predaje")
        void teacherSeesAll() throws Exception {
            when(submissionService.getAll()).thenReturn(List.of(submission()));

            mockMvc.perform(get("/api/submissions").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi sve predaje")
        void studentCannotSeeAll() throws Exception {
            mockMvc.perform(get("/api/submissions").with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(submissionService, never()).getAll();
        }

        @Test
        @DisplayName("nastavnik vidi predaje pojedinog studenta")
        void teacherSeesStudentSubmissions() throws Exception {
            UUID otherStudent = UUID.randomUUID();
            when(submissionService.getByStudent(otherStudent)).thenReturn(List.of());

            mockMvc.perform(get("/api/submissions/student/{id}", otherStudent)
                            .with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi tuđe predaje")
        void studentCannotSeeOthers() throws Exception {
            mockMvc.perform(get("/api/submissions/student/{id}", UUID.randomUUID())
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("nastavnik vidi predaje po zadatku")
        void teacherSeesByTask() throws Exception {
            when(submissionService.getByTask(taskId)).thenReturn(List.of(submission()));

            mockMvc.perform(get("/api/submissions/task/{id}", taskId).with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("nastavnik vidi skupni pregled")
        void teacherSeesOverview() throws Exception {
            when(submissionOverviewService.getOverview(any(), any(), any()))
                    .thenReturn(StudentSubmissionsOverviewResponse.builder()
                            .totalStudents(0)
                            .totalSubmissions(0)
                            .students(List.of())
                            .build());

            mockMvc.perform(get("/api/submissions/overview").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi skupni pregled")
        void studentCannotSeeOverview() throws Exception {
            mockMvc.perform(get("/api/submissions/overview").with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(submissionOverviewService, never()).getOverview(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Ocjenjivanje")
    class Grading {

        @Test
        @DisplayName("nastavnik smije dodati ocjenu i komentar")
        void teacherCanGrade() throws Exception {
            when(submissionService.addTeacherFeedback(any(), any(), any()))
                    .thenReturn(submission());

            mockMvc.perform(patch("/api/submissions/{id}/feedback", submissionId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"feedback\":\"Dobro riješeno\",\"score\":18}"))
                    .andExpect(status().isOk());

            verify(submissionService).addTeacherFeedback(submissionId, "Dobro riješeno", 18);
        }

        @Test
        @DisplayName("administrator smije ocjenjivati")
        void adminCanGrade() throws Exception {
            when(submissionService.addTeacherFeedback(any(), any(), any()))
                    .thenReturn(submission());

            mockMvc.perform(patch("/api/submissions/{id}/feedback", submissionId)
                            .with(user(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"feedback\":\"U redu\",\"score\":15}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije ocjenjivati")
        void studentCannotGrade() throws Exception {
            mockMvc.perform(patch("/api/submissions/{id}/feedback", submissionId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"feedback\":\"Odlično\",\"score\":20}"))
                    .andExpect(status().isForbidden());

            verify(submissionService, never()).addTeacherFeedback(any(), any(), any());
        }
    }

    private SubmissionRequest request() {
        SubmissionRequest r = new SubmissionRequest();
        r.setTaskId(taskId);
        r.setCode("int main() { return 0; }");
        return r;
    }

    private SubmissionResponse submission() {
        return SubmissionResponse.builder()
                .id(submissionId)
                .studentId(studentUser != null ? studentUser.getId() : UUID.randomUUID())
                .studentName("Ana Anić")
                .taskId(taskId)
                .taskTitle("Zbroj niza")
                .status(SubmissionStatus.COMPLETED)
                .finalScore(18)
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