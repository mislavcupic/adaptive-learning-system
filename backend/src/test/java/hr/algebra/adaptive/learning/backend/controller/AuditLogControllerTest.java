package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuditLogController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("AuditLogController")
class AuditLogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuditLogService auditLogService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private User admin;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        admin = account("admin@test.hr", UserRole.ADMIN);
        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Test
    @DisplayName("administrator dobiva i administratorske zapise")
    void adminSeesEverything() throws Exception {
        when(auditLogService.getLogs(true)).thenReturn(List.of());

        mockMvc.perform(get("/api/teacher/audit-logs").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(auditLogService).getLogs(true);
    }

    @Test
    @DisplayName("nastavnik dobiva zapise bez administratorskih")
    void teacherSeesFiltered() throws Exception {
        when(auditLogService.getLogs(false)).thenReturn(List.of());

        mockMvc.perform(get("/api/teacher/audit-logs").with(user(teacher)))
                .andExpect(status().isOk());

        verify(auditLogService).getLogs(false);
    }

    @Test
    @DisplayName("student ne vidi zapise")
    void studentForbidden() throws Exception {
        mockMvc.perform(get("/api/teacher/audit-logs").with(user(studentUser)))
                .andExpect(status().isForbidden());

        verify(auditLogService, never()).getLogs(anyBoolean());
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
