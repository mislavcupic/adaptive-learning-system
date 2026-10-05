package hr.algebra.adaptive.learning.backend.controller;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.SkillMasteryResponse;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.SkillMasteryService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SkillMasteryController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("SkillMasteryController")
class SkillMasteryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SkillMasteryService masteryService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private UUID otherStudentId;
    private User teacher;
    private User studentUser;

    @BeforeEach
    void setUp() {
        otherStudentId = UUID.randomUUID();
        teacher = account("ines@test.hr", UserRole.TEACHER);
        studentUser = account("ana@test.hr", UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Studentov pogled")
    class StudentView {

        @Test
        @DisplayName("student vidi vlastite procjene")
        void studentSeesOwnMastery() throws Exception {
            when(masteryService.getByStudent(studentUser.getId()))
                    .thenReturn(List.of(mastery("Petlje", 0.72)));

            mockMvc.perform(get("/api/mastery/my").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].skillName").value("Petlje"));
        }

        @Test
        @DisplayName("student vidi svoj prosjek")
        void studentSeesOwnAverage() throws Exception {
            when(masteryService.getAverageMastery(studentUser.getId())).thenReturn(0.65);

            mockMvc.perform(get("/api/mastery/my/average").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(0.65));
        }

        @Test
        @DisplayName("student vidi pojedinu vještinu")
        void studentSeesSingleSkill() throws Exception {
            when(masteryService.getByStudentAndSkill(any(), eq("Petlje")))
                    .thenReturn(mastery("Petlje", 0.72));

            mockMvc.perform(get("/api/mastery/my/skill/{skill}", "Petlje")
                            .with(user(studentUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("nastavnik nema rutu za vlastite procjene")
        void teacherHasNoOwnRoute() throws Exception {
            mockMvc.perform(get("/api/mastery/my").with(user(teacher)))
                    .andExpect(status().isForbidden());

            verify(masteryService, never()).getByStudent(any());
        }
    }

    @Nested
    @DisplayName("Nastavnički pogled")
    class TeacherView {

        @Test
        @DisplayName("nastavnik vidi procjene pojedinog studenta")
        void teacherSeesStudentMastery() throws Exception {
            when(masteryService.getByStudent(otherStudentId))
                    .thenReturn(List.of(mastery("Polja", 0.5)));

            mockMvc.perform(get("/api/mastery/student/{id}", otherStudentId)
                            .with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi tuđe procjene")
        void studentCannotSeeOthers() throws Exception {
            mockMvc.perform(get("/api/mastery/student/{id}", otherStudentId)
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());

            verify(masteryService, never()).getByStudent(otherStudentId);
        }

        @Test
        @DisplayName("nastavnik vidi prosjek studenta")
        void teacherSeesStudentAverage() throws Exception {
            when(masteryService.getAverageMastery(otherStudentId)).thenReturn(0.4);

            mockMvc.perform(get("/api/mastery/student/{id}/average", otherStudentId)
                            .with(user(teacher)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("student ne vidi tuđi prosjek")
        void studentCannotSeeOthersAverage() throws Exception {
            mockMvc.perform(get("/api/mastery/student/{id}/average", otherStudentId)
                            .with(user(studentUser)))
                    .andExpect(status().isForbidden());
        }
    }

    private SkillMasteryResponse mastery(String skillName, double level) {
        return SkillMasteryResponse.builder()
                .id(UUID.randomUUID())
                .skillName(skillName)
                .masteryLevel(level)
                .attemptsCount(5)
                .correctCount(3)
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
