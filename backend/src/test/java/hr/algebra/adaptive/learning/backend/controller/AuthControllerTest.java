package hr.algebra.adaptive.learning.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.LoginRequest;
import hr.algebra.adaptive.learning.backend.dto.request.RegisterRequest;
import hr.algebra.adaptive.learning.backend.dto.response.AuthResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.exception.UnauthorizedException;
import hr.algebra.adaptive.learning.backend.filter.JwtAuthenticationFilter;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.AuthService;
import hr.algebra.adaptive.learning.backend.service.MessageService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Import(ControllerTestSecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private MessageService messageService;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private User studentUser;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .email("ana@test.hr")
                .password("hash")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .emailVerified(true)
                .build();
        studentUser.setId(UUID.randomUUID());

        when(messageService.logoutSuccess()).thenReturn("Odjava uspješna");
    }

    @Nested
    @DisplayName("Registracija")
    class Registration {

        @Test
        @DisplayName("prihvaća ispravan zahtjev")
        void acceptsValidRequest() throws Exception {
            when(authService.register(any())).thenReturn(authResponse());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("odbija zahtjev bez e-mail adrese")
        void rejectsMissingEmail() throws Exception {
            RegisterRequest invalid = registerRequest();
            invalid.setEmail(null);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("odbija neispravan oblik adrese")
        void rejectsMalformedEmail() throws Exception {
            RegisterRequest invalid = registerRequest();
            invalid.setEmail("nije-email");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("registracija ne traži prijavu")
        void worksWithoutAuthentication() throws Exception {
            when(authService.register(any())).thenReturn(authResponse());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Prijava")
    class Login {

        @Test
        @DisplayName("vraća tokene za ispravne podatke")
        void returnsTokens() throws Exception {
            when(authService.login(any())).thenReturn(authResponse());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("access.token"));
        }

        @Test
        @DisplayName("vraća 401 za neispravne podatke")
        void returnsUnauthorized() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new UnauthorizedException("Neispravni podaci"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("odbija prijavu bez lozinke")
        void rejectsMissingPassword() throws Exception {
            LoginRequest invalid = loginRequest();
            invalid.setPassword(null);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }
    }

    @Nested
    @DisplayName("Potvrda e-pošte")
    class Verification {

        @Test
        @DisplayName("potvrđuje adresu putem tokena")
        void verifiesWithToken() throws Exception {
            mockMvc.perform(get("/api/auth/verify").param("token", "abc123"))
                    .andExpect(status().isOk());

            verify(authService).verifyEmail("abc123");
        }

        @Test
        @DisplayName("traži token kao parametar")
        void requiresToken() throws Exception {
            mockMvc.perform(get("/api/auth/verify"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ponovno šalje poveznicu")
        void resendsLink() throws Exception {
            mockMvc.perform(post("/api/auth/resend-verification")
                            .param("email", "ana@test.hr"))
                    .andExpect(status().isOk());

            verify(authService).resendVerification("ana@test.hr");
        }
    }

    @Nested
    @DisplayName("Odjava i trenutni korisnik")
    class Session {

        @Test
        @DisplayName("odjava uklanja token")
        void logoutRevokesToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .with(user(studentUser))
                            .header("Authorization", "Bearer access.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"refresh.token\"}"))
                    .andExpect(status().isOk());

            verify(authService).logout(anyString(), anyString());
        }

        @Test
        @DisplayName("odjava radi i bez tijela zahtjeva")
        void logoutWorksWithoutBody() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .with(user(studentUser))
                            .header("Authorization", "Bearer access.token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("vraća podatke prijavljenog korisnika")
        void returnsCurrentUser() throws Exception {
            when(authService.getCurrentUser(studentUser.getId()))
                    .thenReturn(userResponse());

            mockMvc.perform(get("/api/auth/me").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("ana@test.hr"));
        }

        @Test
        @DisplayName("odjava sa svih uređaja poziva servis")
        void logoutAllDevices() throws Exception {
            mockMvc.perform(post("/api/auth/logout-all").with(user(studentUser)))
                    .andExpect(status().isOk());

            verify(authService).logoutAllDevices(any(User.class));
        }
    }

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail("novi@test.hr");
        r.setPassword("Lozinka123");
        r.setFirstName("Ivo");
        r.setLastName("Ivić");
        return r;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("ana@test.hr");
        r.setPassword("Lozinka123");
        return r;
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .accessToken("access.token")
                .refreshToken("refresh.token")
                .user(userResponse())
                .build();
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(studentUser.getId())
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
    }
}
