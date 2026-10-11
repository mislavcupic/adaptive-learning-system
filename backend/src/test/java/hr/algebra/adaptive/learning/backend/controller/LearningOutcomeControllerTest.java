package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.LearningOutcomeRequest;
import hr.algebra.adaptive.learning.backend.dto.response.LearningOutcomeResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.LearningOutcomeService;
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
        controllers = LearningOutcomeController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("LearningOutcomeController")
class LearningOutcomeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private LearningOutcomeService outcomeService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID outcomeId;
    private UUID courseId;
    private User admin;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        outcomeId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        admin = account("admin@test.hr", UserRole.ADMIN);
        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Dohvat ishoda")
    class Reading {

        @Test
        @DisplayName("popis je dostupan studentu")
        void listOpenToStudent() throws Exception {
            when(outcomeService.getAll()).thenReturn(List.of(response()));

            mockMvc.perform(get("/api/outcomes").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].name").value("Razumijevanje petlji"));
        }

        @Test
        @DisplayName("vraca pojedini ishod")
        void returnsSingleOutcome() throws Exception {
            when(outcomeService.getById(outcomeId)).thenReturn(response());

            mockMvc.perform(get("/api/outcomes/{id}", outcomeId).with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(outcomeService).getById(outcomeId);
        }

        @Test
        @DisplayName("vraca ishode kolegija")
        void returnsOutcomesByCourse() throws Exception {
            when(outcomeService.getByCourse(courseId)).thenReturn(List.of(response()));

            mockMvc.perform(get("/api/outcomes/course/{courseId}", courseId).with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(outcomeService).getByCourse(courseId);
        }

        @Test
        @DisplayName("neispravan id vraca 400")
        void rejectsMalformedId() throws Exception {
            mockMvc.perform(get("/api/outcomes/{id}", "ne-uuid").with(user(teacher)))
                    .andExpect(status().isBadRequest());

            verify(outcomeService, never()).getById(any());
        }
    }

    @Nested
    @DisplayName("Kreiranje ishoda")
    class Creating {

        @Test
        @DisplayName("nastavnik smije kreirati ishod")
        void teacherCanCreate() throws Exception {
            when(outcomeService.create(any())).thenReturn(response());

            mockMvc.perform(post("/api/outcomes")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("student ne smije kreirati ishod")
        void studentCannotCreate() throws Exception {
            mockMvc.perform(post("/api/outcomes")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());

            verify(outcomeService, never()).create(any());
        }

        @Test
        @DisplayName("odbija ishod bez naziva")
        void rejectsMissingName() throws Exception {
            LearningOutcomeRequest invalid = request();
            invalid.setName(null);

            mockMvc.perform(post("/api/outcomes")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(outcomeService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("Izmjena, brisanje i poredak")
    class Modifying {

        @Test
        @DisplayName("nastavnik smije mijenjati ishod")
        void teacherCanUpdate() throws Exception {
            when(outcomeService.update(any(), any())).thenReturn(response());

            mockMvc.perform(put("/api/outcomes/{id}", outcomeId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isOk());

            verify(outcomeService).update(eq(outcomeId), any());
        }

        @Test
        @DisplayName("student ne smije mijenjati ishod")
        void studentCannotUpdate() throws Exception {
            mockMvc.perform(put("/api/outcomes/{id}", outcomeId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("administrator smije obrisati ishod")
        void adminCanDelete() throws Exception {
            mockMvc.perform(delete("/api/outcomes/{id}", outcomeId).with(user(admin)))
                    .andExpect(status().isNoContent());

            verify(outcomeService).delete(outcomeId);
        }

        @Test
        @DisplayName("student ne smije brisati ishod")
        void studentCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/outcomes/{id}", outcomeId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(outcomeService, never()).delete(any());
        }

        @Test
        @DisplayName("nastavnik smije promijeniti poredak ishoda")
        void teacherCanReorder() throws Exception {
            List<UUID> ordered = List.of(UUID.randomUUID(), outcomeId);

            mockMvc.perform(patch("/api/outcomes/course/{courseId}/reorder", courseId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ordered)))
                    .andExpect(status().isOk());

            verify(outcomeService).reorder(courseId, ordered);
        }

        @Test
        @DisplayName("student ne mijenja poredak ishoda")
        void studentCannotReorder() throws Exception {
            mockMvc.perform(patch("/api/outcomes/course/{courseId}/reorder", courseId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isForbidden());

            verify(outcomeService, never()).reorder(any(), any());
        }
    }

    private LearningOutcomeRequest request() {
        LearningOutcomeRequest r = new LearningOutcomeRequest();
        r.setName("Razumijevanje petlji");
        r.setDescription("Student razumije for i while petlje");
        r.setOrderIndex(1);
        r.setCourseId(courseId);
        return r;
    }

    private LearningOutcomeResponse response() {
        return LearningOutcomeResponse.builder()
                .id(outcomeId)
                .name("Razumijevanje petlji")
                .description("Student razumije for i while petlje")
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
