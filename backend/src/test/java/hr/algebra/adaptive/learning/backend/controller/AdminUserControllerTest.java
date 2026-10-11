package hr.algebra.adaptive.learning.backend.controller;

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
        controllers = AdminUserController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("AdminUserController")
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;

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
    @DisplayName("Pregled korisnika")
    class Reading {

        @Test
        @DisplayName("administrator vidi sve korisnike")
        void adminSeesAll() throws Exception {
            when(userService.getAll()).thenReturn(List.of(response(UserRole.STUDENT)));

            mockMvc.perform(get("/api/admin/users").with(user(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].email").value("korisnik@test.hr"));
        }

        @Test
        @DisplayName("administrator otvara pojedinog korisnika")
        void adminOpensSingle() throws Exception {
            when(userService.getById(targetId)).thenReturn(response(UserRole.STUDENT));

            mockMvc.perform(get("/api/admin/users/{id}", targetId).with(user(admin)))
                    .andExpect(status().isOk());

            verify(userService).getById(targetId);
        }

        @Test
        @DisplayName("nastavnik ne dolazi do administratorskog popisa")
        void teacherForbidden() throws Exception {
            mockMvc.perform(get("/api/admin/users").with(user(teacher)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).getAll();
        }

        @Test
        @DisplayName("student ne dolazi do administratorskog popisa")
        void studentForbidden() throws Exception {
            mockMvc.perform(get("/api/admin/users").with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Izmjena uloge")
    class RoleChange {

        @Test
        @DisplayName("administrator mijenja ulogu")
        void adminChangesRole() throws Exception {
            when(userService.updateRole(targetId, UserRole.TEACHER))
                    .thenReturn(response(UserRole.TEACHER));

            mockMvc.perform(put("/api/admin/users/{id}/role", targetId)
                            .with(user(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"TEACHER\"}"))
                    .andExpect(status().isOk());

            verify(userService).updateRole(targetId, UserRole.TEACHER);
        }

        @Test
        @DisplayName("nepoznata uloga vraca 400")
        void unknownRoleIsBadRequest() throws Exception {
            mockMvc.perform(put("/api/admin/users/{id}/role", targetId)
                            .with(user(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"SUPERVISOR\"}"))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).updateRole(any(), any());
        }

        @Test
        @DisplayName("nastavnik ne mijenja uloge")
        void teacherCannotChangeRole() throws Exception {
            mockMvc.perform(put("/api/admin/users/{id}/role", targetId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isForbidden());

            verify(userService, never()).updateRole(any(), any());
        }
    }

    @Nested
    @DisplayName("Aktivacija i brisanje")
    class Lifecycle {

        @Test
        @DisplayName("administrator deaktivira korisnika")
        void adminDeactivates() throws Exception {
            when(userService.updateStatus(targetId, false)).thenReturn(response(UserRole.STUDENT));

            mockMvc.perform(put("/api/admin/users/{id}/status", targetId)
                            .with(user(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\":false}"))
                    .andExpect(status().isOk());

            verify(userService).updateStatus(targetId, false);
        }

        @Test
        @DisplayName("administrator ponovno aktivira korisnika")
        void adminReactivates() throws Exception {
            when(userService.updateStatus(targetId, true)).thenReturn(response(UserRole.STUDENT));

            mockMvc.perform(put("/api/admin/users/{id}/status", targetId)
                            .with(user(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\":true}"))
                    .andExpect(status().isOk());

            verify(userService).updateStatus(targetId, true);
        }

        @Test
        @DisplayName("nastavnik ne mijenja status")
        void teacherCannotChangeStatus() throws Exception {
            mockMvc.perform(put("/api/admin/users/{id}/status", targetId)
                            .with(user(teacher))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isActive\":false}"))
                    .andExpect(status().isForbidden());

            verify(userService, never()).updateStatus(any(), anyBoolean());
        }

        @Test
        @DisplayName("administrator brise korisnika")
        void adminDeletes() throws Exception {
            mockMvc.perform(delete("/api/admin/users/{id}", targetId).with(user(admin)))
                    .andExpect(status().isOk());

            verify(userService).delete(targetId);
        }

        @Test
        @DisplayName("student ne brise korisnike")
        void studentCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/admin/users/{id}", targetId).with(user(studentUser)))
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