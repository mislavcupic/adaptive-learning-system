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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String LOGOUT_URL = "/api/auth/logout";

    private static final String ANA_EMAIL = "ana@test.hr";
    private static final String ACCESS_TOKEN = "access.token";
    private static final String PASSWORD = "Lozinka123";

    private static final String ACCESS_TOKEN_PATH = "$.data.accessToken";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private User studentUser;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .email(ANA_EMAIL)
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
        @DisplayName("prihvaća ispravan zahtjev i vraća tokene")
        void acceptsValidRequest() throws Exception {
            when(authService.register(any())).thenReturn(authResponse());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(ACCESS_TOKEN_PATH).value(ACCESS_TOKEN));
        }

        @ParameterizedTest(name = "adresa: {0}")
        @NullSource
        @ValueSource(strings = {"", "   ", "nije-email", "ana@", "@test.hr"})
        @DisplayName("odbija zahtjev s neispravnom e-mail adresom")
        void rejectsInvalidEmail(String email) throws Exception {
            RegisterRequest invalid = registerRequest();
            invalid.setEmail(email);

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("registracija ne traži prijavu")
        void worksWithoutAuthentication() throws Exception {
            when(authService.register(any())).thenReturn(authResponse());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest())))
                    .andExpect(status().isOk());

            verify(authService).register(any());
        }
    }

    @Nested
    @DisplayName("Prijava")
    class Login {

        @Test
        @DisplayName("vraća tokene za ispravne podatke")
        void returnsTokens() throws Exception {
            when(authService.login(any())).thenReturn(authResponse());

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(ACCESS_TOKEN_PATH).value(ACCESS_TOKEN));
        }

        @Test
        @DisplayName("vraća 401 za neispravne podatke")
        void returnsUnauthorized() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new UnauthorizedException("Neispravni podaci"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("odbija prijavu bez lozinke")
        void rejectsMissingPassword() throws Exception {
            LoginRequest invalid = loginRequest();
            invalid.setPassword(null);

            mockMvc.perform(post(LOGIN_URL)
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
                            .param("email", ANA_EMAIL))
                    .andExpect(status().isOk());

            verify(authService).resendVerification(ANA_EMAIL);
        }
    }

    @Nested
    @DisplayName("Odjava i trenutni korisnik")
    class Session {

        @Test
        @DisplayName("odjava uklanja token")
        void logoutRevokesToken() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .with(user(studentUser))
                            .header("Authorization", "Bearer " + ACCESS_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"refresh.token\"}"))
                    .andExpect(status().isOk());

            verify(authService).logout(anyString(), anyString());
        }

        @Test
        @DisplayName("odjava radi i bez tijela zahtjeva")
        void logoutWorksWithoutBody() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .with(user(studentUser))
                            .header("Authorization", "Bearer " + ACCESS_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("vraća podatke prijavljenog korisnika")
        void returnsCurrentUser() throws Exception {
            when(authService.getCurrentUser(studentUser.getId()))
                    .thenReturn(userResponse());

            mockMvc.perform(get("/api/auth/me").with(user(studentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(ANA_EMAIL));
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
        r.setPassword(PASSWORD);
        r.setFirstName("Ivo");
        r.setLastName("Ivić");
        return r;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail(ANA_EMAIL);
        r.setPassword(PASSWORD);
        return r;
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .accessToken(ACCESS_TOKEN)
                .refreshToken("refresh.token")
                .user(userResponse())
                .build();
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(studentUser.getId())
                .email(ANA_EMAIL)
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
    }
}