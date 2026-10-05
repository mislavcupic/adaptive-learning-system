package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.TaskRequest;
import hr.algebra.adaptive.learning.backend.dto.response.TaskResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.TaskService;
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
        controllers = TaskController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("TaskController")
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TaskService taskService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID taskId;
    private UUID outcomeId;
    private User admin;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        outcomeId = UUID.randomUUID();

        admin = account("admin@test.hr", UserRole.ADMIN);
        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Kreiranje zadatka")
    class Creating {

        @Test
        @DisplayName("nastavnik smije kreirati zadatak")
        void teacherCanCreate() throws Exception {
            when(taskService.create(any(), any())).thenReturn(task());

            mockMvc.perform(post("/api/tasks")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("student ne smije kreirati zadatak")
        void studentCannotCreate() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());

            verify(taskService, never()).create(any(), any());
        }

        @Test
        @DisplayName("odbija zadatak bez naslova")
        void rejectsMissingTitle() throws Exception {
            TaskRequest invalid = request();
            invalid.setTitle(null);

            mockMvc.perform(post("/api/tasks")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("zadatak povezuje s autorom")
        void linksCreator() throws Exception {
            when(taskService.create(any(), any())).thenReturn(task());

            mockMvc.perform(post("/api/tasks")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated());

            verify(taskService).create(any(), eq(teacher.getId()));
        }
    }

    @Nested
    @DisplayName("Dohvat zadataka")
    class Reading {

        @Test
        @DisplayName("popis je dostupan studentu")
        void listAvailableToStudent() throws Exception {
            when(taskService.getAll()).thenReturn(List.of(task()));

            mockMvc.perform(get("/api/tasks").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("Zbroj niza"));
        }

        @Test
        @DisplayName("filtrira po ishodu učenja kad je parametar zadan")
        void filtersByOutcome() throws Exception {
            when(taskService.getByOutcome(outcomeId)).thenReturn(List.of());

            mockMvc.perform(get("/api/tasks")
                            .param("outcomeId", outcomeId.toString())
                            .with(user(teacher)))
                    .andExpect(status().isOk());

            verify(taskService).getByOutcome(outcomeId);
            verify(taskService, never()).getAll();
        }

        @Test
        @DisplayName("vraća pojedini zadatak")
        void returnsSingleTask() throws Exception {
            when(taskService.getById(taskId)).thenReturn(task());

            mockMvc.perform(get("/api/tasks/{id}", taskId).with(user(studentUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student vidi neriješene zadatke")
        void studentSeesPending() throws Exception {
            when(taskService.getPendingForStudent(studentUser.getId()))
                    .thenReturn(List.of(task()));

            mockMvc.perform(get("/api/tasks/pending").with(user(studentUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("nastavnik nema rutu za neriješene zadatke")
        void teacherHasNoPendingRoute() throws Exception {
            mockMvc.perform(get("/api/tasks/pending").with(user(teacher)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("nastavnik vidi svoje zadatke")
        void teacherSeesOwnTasks() throws Exception {
            when(taskService.getByTeacher(teacher.getId())).thenReturn(List.of(task()));

            mockMvc.perform(get("/api/tasks/my").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi nastavničke zadatke")
        void studentCannotSeeTeacherTasks() throws Exception {
            mockMvc.perform(get("/api/tasks/my").with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Izmjena, brisanje i umnožavanje")
    class Modifying {

        @Test
        @DisplayName("nastavnik smije mijenjati zadatak")
        void teacherCanUpdate() throws Exception {
            when(taskService.update(any(), any())).thenReturn(task());

            mockMvc.perform(put("/api/tasks/{id}", taskId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije mijenjati zadatak")
        void studentCannotUpdate() throws Exception {
            mockMvc.perform(put("/api/tasks/{id}", taskId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("nastavnik smije obrisati zadatak")
        void teacherCanDelete() throws Exception {
            mockMvc.perform(delete("/api/tasks/{id}", taskId).with(user(teacher)))
                    .andExpect(status().isNoContent());

            verify(taskService).delete(taskId);
        }

        @Test
        @DisplayName("student ne smije brisati zadatak")
        void studentCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/tasks/{id}", taskId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(taskService, never()).delete(any());
        }

        @Test
        @DisplayName("nastavnik smije umnožiti zadatak")
        void teacherCanDuplicate() throws Exception {
            when(taskService.duplicate(any(), any())).thenReturn(task());

            mockMvc.perform(post("/api/tasks/{id}/duplicate", taskId).with(user(admin)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("student ne smije umnožiti zadatak")
        void studentCannotDuplicate() throws Exception {
            mockMvc.perform(post("/api/tasks/{id}/duplicate", taskId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(taskService, never()).duplicate(any(), any());
        }
    }

    private TaskRequest request() {
        TaskRequest r = new TaskRequest();
        r.setTitle("Zbroj niza");
        r.setDescription("Opis");
        r.setTaskType(TaskType.CODE);
        r.setOutcomeId(outcomeId);
        r.setMaxScore(20);
        return r;
    }

    private TaskResponse task() {
        return TaskResponse.builder()
                .id(taskId)
                .title("Zbroj niza")
                .taskType(TaskType.CODE)
                .maxScore(20)
                .outcomeId(outcomeId)
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