package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID targetId;
    private User admin;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        targetId = UUID.randomUUID();

        admin = account("admin@test.hr", UserRole.ADMIN);
        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Vlastiti profil")
    class OwnProfile {

        @Test
        @DisplayName("svaki prijavljeni korisnik vidi svoj profil")
        void anyUserSeesOwnProfile() throws Exception {
            when(userService.getById(studentUser.getId())).thenReturn(response(UserRole.STUDENT));

            mockMvc.perform(get("/api/users/me").with(user(studentUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("korisnik smije promijeniti vlastito ime")
        void userCanUpdateOwnName() throws Exception {
            when(userService.updateProfile(any(), any(), any()))
                    .thenReturn(response(UserRole.STUDENT));

            mockMvc.perform(patch("/api/users/me")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\":\"Anamarija\",\"lastName\":\"Anić\"}"))
                    .andExpect(status().isOk());

            verify(userService).updateProfile(studentUser.getId(), "Anamarija", "Anić");
        }
    }

    @Nested
    @DisplayName("Pregled korisnika")
    class Listing {

        @Test
        @DisplayName("samo administrator vidi sve korisnike")
        void onlyAdminSeesAllUsers() throws Exception {
            when(userService.getAll()).thenReturn(List.of(response(UserRole.STUDENT)));

            mockMvc.perform(get("/api/users").with(user(admin)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("nastavnik ne vidi popis svih korisnika")
        void teacherCannotSeeAllUsers() throws Exception {
            mockMvc.perform(get("/api/users").with(user(teacher)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).getAll();
        }

        @Test
        @DisplayName("nastavnik vidi popis studenata")
        void teacherSeesStudents() throws Exception {
            when(userService.getAllStudents()).thenReturn(List.of());

            mockMvc.perform(get("/api/users/students").with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi popis studenata")
        void studentCannotSeeStudents() throws Exception {
            mockMvc.perform(get("/api/users/students").with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("nastavnik smije otvoriti pojedinog korisnika")
        void teacherCanOpenSingleUser() throws Exception {
            when(userService.getById(targetId)).thenReturn(response(UserRole.STUDENT));

            mockMvc.perform(get("/api/users/{id}", targetId).with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne smije otvarati tuđe profile")
        void studentCannotOpenOthers() throws Exception {
            mockMvc.perform(get("/api/users/{id}", targetId).with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("samo administrator vidi popis nastavnika")
        void onlyAdminSeesTeachers() throws Exception {
            mockMvc.perform(get("/api/users/teachers").with(user(teacher)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Izmjena uloge i skupine")
    class Privileges {

        @Test
        @DisplayName("samo administrator mijenja ulogu")
        void onlyAdminChangesRole() throws Exception {
            when(userService.updateRole(any(), any())).thenReturn(response(UserRole.TEACHER));

            mockMvc.perform(patch("/api/users/{id}/role", targetId)
                            .with(user(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"TEACHER\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("nastavnik ne mijenja uloge")
        void teacherCannotChangeRole() throws Exception {
            mockMvc.perform(patch("/api/users/{id}/role", targetId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isForbidden());

            verify(userService, never()).updateRole(any(), any());
        }

        @Test
        @DisplayName("nastavnik smije mijenjati skupinu studenta")
        void teacherCanChangeGroup() throws Exception {
            when(userService.updateGroup(any(), any())).thenReturn(response(UserRole.STUDENT));

            mockMvc.perform(patch("/api/users/{id}/group", targetId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"groupType\":\"EXPERIMENTAL\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne mijenja skupine")
        void studentCannotChangeGroup() throws Exception {
            mockMvc.perform(patch("/api/users/{id}/group", targetId)
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"groupType\":\"EXPERIMENTAL\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Aktivacija i brisanje")
    class Lifecycle {

        @Test
        @DisplayName("administrator smije deaktivirati korisnika")
        void adminCanDeactivate() throws Exception {
            mockMvc.perform(patch("/api/users/{id}/deactivate", targetId).with(user(admin)))
                    .andExpect(status().isOk());

            verify(userService).deactivate(targetId);
        }

        @Test
        @DisplayName("nastavnik ne smije deaktivirati korisnika")
        void teacherCannotDeactivate() throws Exception {
            mockMvc.perform(patch("/api/users/{id}/deactivate", targetId).with(user(teacher)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).deactivate(any());
        }

        @Test
        @DisplayName("administrator smije obrisati korisnika")
        void adminCanDelete() throws Exception {
            mockMvc.perform(delete("/api/users/{id}", targetId).with(user(admin)))
                    .andExpect(status().isNoContent());

            verify(userService).delete(targetId);
        }

        @Test
        @DisplayName("student ne smije brisati korisnike")
        void studentCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/users/{id}", targetId).with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).delete(any());
        }
    }

    private UserResponse response(UserRole role) {
        return UserResponse.builder()
                .id(targetId)
                .email("korisnik@test.hr")
                .firstName("Ime")
                .lastName("Prezime")
                .role(role)
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
