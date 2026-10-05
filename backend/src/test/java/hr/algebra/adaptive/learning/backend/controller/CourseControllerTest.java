package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.CourseRequest;
import hr.algebra.adaptive.learning.backend.dto.response.CourseResponse;
import hr.algebra.adaptive.learning.backend.service.CourseService;
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

import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CourseController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("CourseController")
class CourseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CourseService courseService;
    @MockitoBean private LearningOutcomeService outcomeService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID courseId;
    private User admin;
    private User teacher;
    private User studentUser;
    private CourseResponse course;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();

        admin = account("admin@test.hr", UserRole.ADMIN);
        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);

        course = CourseResponse.builder()
                .id(courseId)
                .name("Uvod u programiranje")
                .languageType(LanguageType.C)
                .build();
    }

    @Nested
    @DisplayName("Dohvat kolegija")
    class Reading {

        @Test
        @DisplayName("popis je dostupan studentu")
        void listAvailableToStudent() throws Exception {
            when(courseService.getAll()).thenReturn(List.of(course));

            mockMvc.perform(get("/api/courses").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].name").value("Uvod u programiranje"));
        }

        @Test
        @DisplayName("filtrira po programskom jeziku kad je parametar zadan")
        void filtersByLanguage() throws Exception {
            when(courseService.getByLanguageType(LanguageType.PYTHON))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/courses")
                            .param("languageType", "PYTHON")
                            .with(user(teacher)))
                    .andExpect(status().isOk());

            verify(courseService).getByLanguageType(LanguageType.PYTHON);
            verify(courseService, never()).getAll();
        }

        @Test
        @DisplayName("vraća pojedini kolegij")
        void returnsSingleCourse() throws Exception {
            when(courseService.getById(courseId)).thenReturn(course);

            mockMvc.perform(get("/api/courses/{id}", courseId).with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(courseId.toString()));
        }

        @Test
        @DisplayName("moji kolegiji nisu dostupni studentu")
        void myCoursesForbiddenForStudent() throws Exception {
            mockMvc.perform(get("/api/courses/my").with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(courseService, never()).getByTeacher(any());
        }

        @Test
        @DisplayName("nastavnik vidi svoje kolegije")
        void teacherSeesOwnCourses() throws Exception {
            when(courseService.getByTeacher(teacher.getId())).thenReturn(List.of(course));

            mockMvc.perform(get("/api/courses/my").with(user(teacher)))
                    .andExpect(status().isOk());

            verify(courseService).getByTeacher(teacher.getId());
        }
    }

    @Nested
    @DisplayName("Kreiranje kolegija")
    class Creating {

        @Test
        @DisplayName("nastavnik smije kreirati kolegij")
        void teacherCanCreate() throws Exception {
            when(courseService.create(any(), any())).thenReturn(course);

            mockMvc.perform(post("/api/courses")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("student ne smije kreirati kolegij")
        void studentCannotCreate() throws Exception {
            mockMvc.perform(post("/api/courses")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());

            verify(courseService, never()).create(any(), any());
        }

        @Test
        @DisplayName("odbija zahtjev bez naziva kolegija")
        void rejectsMissingName() throws Exception {
            CourseRequest invalid = request();
            invalid.setName(null);

            mockMvc.perform(post("/api/courses")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(courseService, never()).create(any(), any());
        }
    }

    @Nested
    @DisplayName("Izmjena i brisanje")
    class Modifying {

        @Test
        @DisplayName("nastavnik smije mijenjati kolegij")
        void teacherCanUpdate() throws Exception {
            when(courseService.update(any(), any())).thenReturn(course);

            mockMvc.perform(put("/api/courses/{id}", courseId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije mijenjati kolegij")
        void studentCannotUpdate() throws Exception {
            mockMvc.perform(put("/api/courses/{id}", courseId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("samo administrator smije obrisati kolegij")
        void onlyAdminCanDelete() throws Exception {
            mockMvc.perform(delete("/api/courses/{id}", courseId).with(user(admin)))
                    .andExpect(status().isNoContent());

            verify(courseService).delete(courseId);
        }

        @Test
        @DisplayName("nastavnik ne smije obrisati kolegij")
        void teacherCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/courses/{id}", courseId).with(user(teacher)))
                    .andExpect(status().isForbidden());

            verify(courseService, never()).delete(any());
        }

        @Test
        @DisplayName("nastavnik smije deaktivirati kolegij")
        void teacherCanDeactivate() throws Exception {
            mockMvc.perform(patch("/api/courses/{id}/deactivate", courseId)
                            .with(user(teacher)))
                    .andExpect(status().isOk());

            verify(courseService).deactivate(courseId);
        }

        @Test
        @DisplayName("student ne smije deaktivirati kolegij")
        void studentCannotDeactivate() throws Exception {
            mockMvc.perform(patch("/api/courses/{id}/deactivate", courseId)
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }
    }

    private CourseRequest request() {
        CourseRequest r = new CourseRequest();
        r.setName("Novi kolegij");
        r.setDescription("Opis");
        r.setLanguageType(LanguageType.PYTHON);
        return r;
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