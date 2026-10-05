package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.ml.MLAncovaResponse;
import hr.algebra.adaptive.learning.backend.dto.response.ResearchResultResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.ResearchResultService;
import hr.algebra.adaptive.learning.backend.service.UserService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TeacherController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("TeacherController")
class TeacherControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private ResearchResultService researchResultService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID pendingId;
    private UUID classId;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        pendingId = UUID.randomUUID();
        classId = UUID.randomUUID();

        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Čekaonica prijava")
    class PendingQueue {

        @Test
        @DisplayName("nastavnik vidi prijave na čekanju")
        void teacherSeesPending() throws Exception {
            when(userService.getPendingRegistrations()).thenReturn(List.of(response()));

            mockMvc.perform(get("/api/teacher/pending-registrations").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi prijave na čekanju")
        void studentCannotSeePending() throws Exception {
            mockMvc.perform(get("/api/teacher/pending-registrations").with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).getPendingRegistrations();
        }

        @Test
        @DisplayName("nastavnik vidi broj prijava")
        void teacherSeesCount() throws Exception {
            when(userService.getPendingRegistrationsCount()).thenReturn(3L);

            mockMvc.perform(get("/api/teacher/pending-registrations/count")
                            .with(user(teacher)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(3));
        }
    }

    @Nested
    @DisplayName("Odobravanje i odbijanje")
    class Approval {

        @Test
        @DisplayName("nastavnik odobrava s razredom i uključenjem u istraživanje")
        void approvesWithClassAndResearch() throws Exception {
            when(userService.approveUser(any(), any(), anyBoolean())).thenReturn(response());

            mockMvc.perform(put("/api/teacher/approve/{id}", pendingId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"schoolClassId\":\"" + classId
                                    + "\",\"includeInResearch\":true}"))
                    .andExpect(status().isOk());

            verify(userService).approveUser(pendingId, classId, true);
        }

        @Test
        @DisplayName("odobrenje bez tijela ne dodjeljuje razred ni skupinu")
        void approvesWithoutBody() throws Exception {
            when(userService.approveUser(any(), any(), anyBoolean())).thenReturn(response());

            mockMvc.perform(put("/api/teacher/approve/{id}", pendingId).with(user(teacher)))
                    .andExpect(status().isOk());

            verify(userService).approveUser(eq(pendingId), isNull(), eq(false));
        }

        @Test
        @DisplayName("student ne smije odobravati prijave")
        void studentCannotApprove() throws Exception {
            mockMvc.perform(put("/api/teacher/approve/{id}", pendingId)
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).approveUser(any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("nastavnik smije odbiti prijavu")
        void teacherCanReject() throws Exception {
            mockMvc.perform(delete("/api/teacher/reject/{id}", pendingId).with(user(teacher)))
                    .andExpect(status().isOk());

            verify(userService).rejectPendingUser(pendingId);
        }

        @Test
        @DisplayName("student ne smije odbijati prijave")
        void studentCannotReject() throws Exception {
            mockMvc.perform(delete("/api/teacher/reject/{id}", pendingId)
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Rezultati istraživanja")
    class Research {

        @Test
        @DisplayName("nastavnik vidi rezultate")
        void teacherSeesResults() throws Exception {
            when(researchResultService.getResults()).thenReturn(List.of(result()));

            mockMvc.perform(get("/api/teacher/research-results").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi rezultate istraživanja")
        void studentCannotSeeResults() throws Exception {
            mockMvc.perform(get("/api/teacher/research-results").with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(researchResultService, never()).getResults();
        }

        @Test
        @DisplayName("nastavnik pokreće statističku analizu")
        void teacherRunsAncova() throws Exception {
            when(researchResultService.getAncova()).thenReturn(new MLAncovaResponse());

            mockMvc.perform(get("/api/teacher/research-results/ancova").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne pokreće statističku analizu")
        void studentCannotRunAncova() throws Exception {
            mockMvc.perform(get("/api/teacher/research-results/ancova")
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(researchResultService, never()).getAncova();
        }
    }

    private UserResponse response() {
        return UserResponse.builder()
                .id(pendingId)
                .email("novi@test.hr")
                .firstName("Ivo")
                .lastName("Ivić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
    }

    private ResearchResultResponse result() {
        return ResearchResultResponse.builder()
                .studentId(UUID.randomUUID())
                .firstName("Ana")
                .lastName("Anić")
                .email("ana@test.hr")
                .researchGroup(ResearchGroup.EXPERIMENTAL)
                .pretestScore(12)
                .posttestScore(18)
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