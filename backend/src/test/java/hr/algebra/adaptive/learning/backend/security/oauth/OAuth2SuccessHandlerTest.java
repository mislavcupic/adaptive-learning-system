package hr.algebra.adaptive.learning.backend.security.oauth;

import hr.algebra.adaptive.learning.backend.domain.entity.RefreshToken;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AuthProvider;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.repository.RefreshTokenRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OAuth2SuccessHandler")
class OAuth2SuccessHandlerTest {

    private static final String FRONTEND = "http://localhost:3000";
    private static final String EMAIL = "ines@gmail.com";

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private OAuth2User oAuth2User;
    @Mock private Authentication authentication;

    private OAuth2SuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(userRepository, refreshTokenRepository, jwtService);
        ReflectionTestUtils.setField(handler, "frontendUrl", FRONTEND);

        request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        response = new MockHttpServletResponse();

        when(oAuth2User.getAttribute("email")).thenReturn(EMAIL);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
    }

    @Nested
    @DisplayName("Racun koji ne prolazi")
    class Rejected {

        @Test
        @DisplayName("nepoznat korisnik se vraca na prijavu s greskom")
        void unknownUser() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            handler.onAuthenticationSuccess(request, response, authentication);

            assertThat(response.getRedirectedUrl()).startsWith(FRONTEND + "/login?error=");
            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("racun koji ceka odobrenje ide na stranicu cekanja")
        void pendingApproval() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(false)));

            handler.onAuthenticationSuccess(request, response, authentication);

            assertThat(response.getRedirectedUrl())
                    .isEqualTo(FRONTEND + "/registration-pending?provider=google");
        }

        @Test
        @DisplayName("neodobrenom racunu se ne izdaju tokeni")
        void pendingAccountGetsNoTokens() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(false)));

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(jwtService, never()).generateAccessToken(any());
            verify(jwtService, never()).generateRefreshToken(any());
            verify(refreshTokenRepository, never()).revokeAllUserTokens(any());
        }
    }

    @Nested
    @DisplayName("Uspjesna prijava")
    class Accepted {

        @BeforeEach
        void activeUser() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(true)));
        }

        @Test
        @DisplayName("preusmjerava na callback s oba tokena")
        void redirectsWithTokens() throws Exception {
            handler.onAuthenticationSuccess(request, response, authentication);

            String url = response.getRedirectedUrl();
            assertThat(url)
                    .startsWith(FRONTEND + "/oauth2/callback")
                    .contains("accessToken=access-token")
                    .contains("refreshToken=refresh-token");
        }

        @Test
        @DisplayName("ponistava ranije izdane tokene")
        void revokesOldTokens() throws Exception {
            handler.onAuthenticationSuccess(request, response, authentication);

            verify(refreshTokenRepository).revokeAllUserTokens(any(User.class));
        }

        @Test
        @DisplayName("sprema novi refresh token s rokom valjanosti")
        void savesRefreshToken() throws Exception {
            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getToken()).isEqualTo("refresh-token");
            assertThat(saved.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("ne preusmjerava ako je odgovor vec poslan")
        void skipsRedirectWhenCommitted() throws Exception {
            response.flushBuffer();

            handler.onAuthenticationSuccess(request, response, authentication);

            assertThat(response.getRedirectedUrl()).isNull();
        }
    }

    private User user(boolean active) {
        User u = User.builder()
                .email(EMAIL)
                .password("hash")
                .firstName("Ines")
                .lastName("Inic")
                .role(active ? UserRole.TEACHER : UserRole.GUEST)
                .isActive(active)
                .emailVerified(true)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("111545373032881004436")
                .build();
        u.setId(UUID.randomUUID());
        return u;
    }
}
