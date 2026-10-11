package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.SchoolClassRequest;
import hr.algebra.adaptive.learning.backend.dto.response.SchoolClassResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.SchoolClassService;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SchoolClassController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("SchoolClassController")
class SchoolClassControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SchoolClassService classService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID classId;
    private UUID studentId;
    private UUID courseId;
    private User admin;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        classId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        admin = account("admin@test.hr", UserRole.ADMIN);
        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Kreiranje razreda")
    class Creating {

        @Test
        @DisplayName("nastavnik smije kreirati razred")
        void teacherCanCreate() throws Exception {
            when(classService.create(any(), any())).thenReturn(response());

            mockMvc.perform(post("/api/classes")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("1.a"));
        }

        @Test
        @DisplayName("razred se veze na prijavljenog nastavnika")
        void linksTeacher() throws Exception {
            when(classService.create(any(), any())).thenReturn(response());

            mockMvc.perform(post("/api/classes")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isCreated());

            verify(classService).create(any(), eq(teacher.getId()));
        }

        @Test
        @DisplayName("student ne smije kreirati razred")
        void studentCannotCreate() throws Exception {
            mockMvc.perform(post("/api/classes")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());

            verify(classService, never()).create(any(), any());
        }

        @Test
        @DisplayName("odbija razred bez naziva")
        void rejectsMissingName() throws Exception {
            SchoolClassRequest invalid = request();
            invalid.setName(null);

            mockMvc.perform(post("/api/classes")
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(classService, never()).create(any(), any());
        }
    }

    @Nested
    @DisplayName("Dohvat razreda")
    class Reading {

        @Test
        @DisplayName("pojedini razred je dostupan studentu")
        void singleClassOpenToStudent() throws Exception {
            when(classService.getById(classId)).thenReturn(response());

            mockMvc.perform(get("/api/classes/{id}", classId).with(user(studentUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("nastavnik vidi sve razrede")
        void teacherSeesAll() throws Exception {
            when(classService.getAll()).thenReturn(List.of(response()));

            mockMvc.perform(get("/api/classes").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi popis svih razreda")
        void studentCannotSeeAll() throws Exception {
            mockMvc.perform(get("/api/classes").with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(classService, never()).getAll();
        }

        @Test
        @DisplayName("nastavniku moji razredi vracaju razrede koje vodi")
        void teacherMyClasses() throws Exception {
            when(classService.getByTeacher(teacher.getId())).thenReturn(List.of(response()));

            mockMvc.perform(get("/api/classes/my").with(user(teacher)))
                    .andExpect(status().isOk());

            verify(classService).getByTeacher(teacher.getId());
            verify(classService, never()).getByStudent(any());
        }

        @Test
        @DisplayName("studentu moji razredi vracaju razred u koji je upisan")
        void studentMyClasses() throws Exception {
            when(classService.getByStudent(studentUser.getId())).thenReturn(List.of(response()));

            mockMvc.perform(get("/api/classes/my").with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(classService).getByStudent(studentUser.getId());
            verify(classService, never()).getByTeacher(any());
        }

        @Test
        @DisplayName("nastavnik vidi studente razreda")
        void teacherSeesStudents() throws Exception {
            when(classService.getStudents(classId)).thenReturn(List.of());

            mockMvc.perform(get("/api/classes/{id}/students", classId).with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi popis studenata razreda")
        void studentCannotSeeStudents() throws Exception {
            mockMvc.perform(get("/api/classes/{id}/students", classId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(classService, never()).getStudents(any());
        }
    }

    @Nested
    @DisplayName("Izmjena i brisanje")
    class Modifying {

        @Test
        @DisplayName("nastavnik smije mijenjati razred")
        void teacherCanUpdate() throws Exception {
            when(classService.update(any(), any())).thenReturn(response());

            mockMvc.perform(put("/api/classes/{id}", classId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije mijenjati razred")
        void studentCannotUpdate() throws Exception {
            mockMvc.perform(put("/api/classes/{id}", classId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("samo administrator smije obrisati razred")
        void onlyAdminCanDelete() throws Exception {
            mockMvc.perform(delete("/api/classes/{id}", classId).with(user(admin)))
                    .andExpect(status().isNoContent());

            verify(classService).delete(classId);
        }

        @Test
        @DisplayName("nastavnik ne smije obrisati razred")
        void teacherCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/classes/{id}", classId).with(user(teacher)))
                    .andExpect(status().isForbidden());

            verify(classService, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Studenti i kolegiji razreda")
    class Membership {

        @Test
        @DisplayName("nastavnik dodaje studenta u razred")
        void teacherAddsStudent() throws Exception {
            mockMvc.perform(post("/api/classes/{classId}/students/{studentId}", classId, studentId)
                            .with(user(teacher)))
                    .andExpect(status().isCreated());

            verify(classService).addStudent(classId, studentId);
        }

        @Test
        @DisplayName("student ne dodaje studente u razred")
        void studentCannotAddStudent() throws Exception {
            mockMvc.perform(post("/api/classes/{classId}/students/{studentId}", classId, studentId)
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(classService, never()).addStudent(any(), any());
        }

        @Test
        @DisplayName("nastavnik izbacuje studenta iz razreda")
        void teacherRemovesStudent() throws Exception {
            mockMvc.perform(delete("/api/classes/{classId}/students/{studentId}", classId, studentId)
                            .with(user(teacher)))
                    .andExpect(status().isNoContent());

            verify(classService).removeStudent(classId, studentId);
        }

        @Test
        @DisplayName("nastavnik dodaje kolegij razredu")
        void teacherAddsCourse() throws Exception {
            mockMvc.perform(post("/api/classes/{classId}/courses/{courseId}", classId, courseId)
                            .with(user(teacher)))
                    .andExpect(status().isCreated());

            verify(classService).addCourse(classId, courseId);
        }

        @Test
        @DisplayName("nastavnik sklanja kolegij s razreda")
        void teacherRemovesCourse() throws Exception {
            mockMvc.perform(delete("/api/classes/{classId}/courses/{courseId}", classId, courseId)
                            .with(user(teacher)))
                    .andExpect(status().isNoContent());

            verify(classService).removeCourse(classId, courseId);
        }

        @Test
        @DisplayName("student ne dira kolegije razreda")
        void studentCannotTouchCourses() throws Exception {
            mockMvc.perform(post("/api/classes/{classId}/courses/{courseId}", classId, courseId)
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(classService, never()).addCourse(any(), any());
        }
    }

    private SchoolClassRequest request() {
        SchoolClassRequest r = new SchoolClassRequest();
        r.setName("1.a");
        r.setDescription("Prvi razred");
        r.setAcademicYear("2025/2026");
        r.setCourseIds(Set.of(courseId));
        return r;
    }

    private SchoolClassResponse response() {
        return SchoolClassResponse.builder()
                .id(classId)
                .name("1.a")
                .description("Prvi razred")
                .academicYear("2025/2026")
                .teacherId(UUID.randomUUID())
                .teacherName("Ines Ines")
                .studentCount(3)
                .coursesCount(1)
                .isActive(true)
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