package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.AdminDashboardResponse;
import hr.algebra.adaptive.learning.backend.dto.response.StudentDashboardResponse;
import hr.algebra.adaptive.learning.backend.dto.response.TeacherDashboardResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.DashboardService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DashboardController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DashboardService dashboardService;

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
    @DisplayName("student vidi svoju nadzornu ploču")
    void studentSeesOwnDashboard() throws Exception {
        when(dashboardService.getStudentDashboard(any()))
                .thenReturn(StudentDashboardResponse.builder().build());

        mockMvc.perform(get("/api/dashboard/student").with(user(studentUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("nastavnik ne otvara studentsku ploču")
    void teacherCannotOpenStudentDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/student").with(user(teacher)))
                .andExpect(status().isForbidden());

        verify(dashboardService, never()).getStudentDashboard(any());
    }

    @Test
    @DisplayName("nastavnik vidi svoju nadzornu ploču")
    void teacherSeesOwnDashboard() throws Exception {
        when(dashboardService.getTeacherDashboardByEmail(any()))
                .thenReturn(TeacherDashboardResponse.builder().build());

        mockMvc.perform(get("/api/dashboard/teacher").with(user(teacher)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("student ne otvara nastavničku ploču")
    void studentCannotOpenTeacherDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/teacher").with(user(studentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrator vidi administratorsku ploču")
    void adminSeesAdminDashboard() throws Exception {
        when(dashboardService.getAdminDashboard())
                .thenReturn(AdminDashboardResponse.builder().build());

        mockMvc.perform(get("/api/dashboard/admin").with(user(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("nastavnik ne otvara administratorsku ploču")
    void teacherCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin").with(user(teacher)))
                .andExpect(status().isForbidden());

        verify(dashboardService, never()).getAdminDashboard();
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