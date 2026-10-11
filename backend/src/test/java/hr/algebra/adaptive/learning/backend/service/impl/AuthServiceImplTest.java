package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.RefreshToken;
import hr.algebra.adaptive.learning.backend.domain.entity.TokenBlacklist;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AuthProvider;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.LoginRequest;
import hr.algebra.adaptive.learning.backend.dto.request.RefreshTokenRequest;
import hr.algebra.adaptive.learning.backend.dto.request.RegisterRequest;
import hr.algebra.adaptive.learning.backend.dto.response.AuthResponse;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.exception.UnauthorizedException;
import hr.algebra.adaptive.learning.backend.repository.RefreshTokenRepository;
import hr.algebra.adaptive.learning.backend.repository.TokenBlacklistRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.EmailService;
import hr.algebra.adaptive.learning.backend.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private MessageService messageService;
    @Mock private EmailService emailService;

    @InjectMocks private AuthServiceImpl authService;

    private User verifiedUser;

    @BeforeEach
    void setUp() {
        verifiedUser = User.builder()
                .email("ana@test.hr")
                .password("$2a$10$hash")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .emailVerified(true)
                .authProvider(AuthProvider.LOCAL)
                .build();
        verifiedUser.setId(UUID.randomUUID());

        when(messageService.emailExists()).thenReturn("Email već postoji");
        when(messageService.invalidCredentials()).thenReturn("Neispravni podaci");
        when(messageService.userNotFound()).thenReturn("Korisnik nije pronađen");
        when(messageService.refreshTokenInvalid()).thenReturn("Token za osvježavanje nije valjan");
        when(messageService.getMessage(anyString())).thenReturn("Token je istekao");

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access.token");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh.token");
        when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
    }

    @Nested
    @DisplayName("Registracija")
    class Registration {

        @Test
        @DisplayName("novi korisnik dobiva ulogu GUEST i nije aktivan")
        void newUserIsGuestAndInactive() {
            when(userRepository.findByEmail("novi@test.hr")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            AuthResponse response = authService.register(request("novi@test.hr"));

            assertThat(response.getUser()).isNotNull();

            verify(userRepository).save(argThat(u ->
                    u.getRole() == UserRole.GUEST
                            && !u.isActive()
                            && !u.isEmailVerified()
                            && u.getAuthProvider() == AuthProvider.LOCAL));
        }

        @Test
        @DisplayName("generira token za potvrdu i šalje e-mail")
        void sendsVerificationEmail() {
            when(userRepository.findByEmail("novi@test.hr")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            authService.register(request("novi@test.hr"));

            verify(emailService).sendVerificationEmail(any(User.class), anyString());
            verify(userRepository).save(argThat(u ->
                    u.getVerificationToken() != null
                            && u.getVerificationTokenExpiry() != null));
        }

        @Test
        @DisplayName("lozinka se sprema u kriptiranom obliku")
        void hashesPassword() {
            when(userRepository.findByEmail("novi@test.hr")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            authService.register(request("novi@test.hr"));

            verify(passwordEncoder).encode("Lozinka123");
            verify(userRepository).save(argThat(u -> !u.getPassword().equals("Lozinka123")));
        }

        @Test
        @DisplayName("odbija registraciju na postojeću potvrđenu adresu")
        void rejectsExistingVerifiedEmail() {
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));

            assertThatThrownBy(() -> authService.register(request("ana@test.hr")))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("odbija registraciju na adresu već vezanu uz Google")
        void rejectsGoogleAccount() {
            User googleUser = User.builder()
                    .email("google@test.hr")
                    .password("$2a$10$hash")
                    .firstName("Ivo")
                    .lastName("Ivić")
                    .role(UserRole.GUEST)
                    .isActive(false)
                    .emailVerified(false)
                    .authProvider(AuthProvider.GOOGLE)
                    .build();
            googleUser.setId(UUID.randomUUID());

            when(userRepository.findByEmail("google@test.hr"))
                    .thenReturn(Optional.of(googleUser));

            assertThatThrownBy(() -> authService.register(request("google@test.hr")))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(emailService, never()).sendVerificationEmail(any(), anyString());
        }

        @Test
        @DisplayName("šalje novu poveznicu kad račun postoji ali nije potvrđen")
        void resendsLinkForUnverifiedAccount() {
            User unverified = User.builder()
                    .email("ceka@test.hr")
                    .password("$2a$10$hash")
                    .firstName("Ivo")
                    .lastName("Ivić")
                    .role(UserRole.GUEST)
                    .isActive(false)
                    .emailVerified(false)
                    .authProvider(AuthProvider.LOCAL)
                    .build();
            unverified.setId(UUID.randomUUID());

            when(userRepository.findByEmail("ceka@test.hr"))
                    .thenReturn(Optional.of(unverified));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.register(request("ceka@test.hr"));

            assertThat(response.getMessage()).contains("novu poveznicu");
            verify(emailService).sendVerificationEmail(any(User.class), anyString());
        }
    }

    @Nested
    @DisplayName("Prijava")
    class Login {

        @Test
        @DisplayName("uspješna prijava vraća tokene")
        void successfulLoginReturnsTokens() {
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));

            AuthResponse response = authService.login(loginRequest("ana@test.hr"));

            assertThat(response.getAccessToken()).isEqualTo("access.token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
        }

        @Test
        @DisplayName("sprema novi token za osvježavanje")
        void storesRefreshToken() {
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));

            authService.login(loginRequest("ana@test.hr"));

            verify(refreshTokenRepository).save(argThat(t ->
                    t.getToken().equals("refresh.token") && t.getExpiresAt() != null));
        }

        @Test
        @DisplayName("blokira prijavu dok e-mail nije potvrđen")
        void blocksUnverifiedEmail() {
            verifiedUser.setEmailVerified(false);
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));

            assertThatThrownBy(() -> authService.login(loginRequest("ana@test.hr")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("nije potvrđena");

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("blokira prijavu dok nastavnik nije odobrio račun")
        void blocksInactiveAccount() {
            verifiedUser.setActive(false);
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));

            assertThatThrownBy(() -> authService.login(loginRequest("ana@test.hr")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("odobrenje");
        }

        @Test
        @DisplayName("nepoznata adresa prolazi na provjeru lozinke")
        void unknownEmailGoesToAuthentication() {
            when(userRepository.findByEmail("nitko@test.hr")).thenReturn(Optional.empty());
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(loginRequest("nitko@test.hr")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("odbija pogrešnu lozinku")
        void rejectsWrongPassword() {
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(loginRequest("ana@test.hr")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("povlači prethodne tokene osvježavanja")
        void revokesOldRefreshTokens() {
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));

            authService.login(loginRequest("ana@test.hr"));

            verify(refreshTokenRepository).revokeAllUserTokens(verifiedUser);
        }
    }

    @Nested
    @DisplayName("Osvježavanje tokena")
    class RefreshingToken {

        @Test
        @DisplayName("izdaje novi pristupni token")
        void issuesNewAccessToken() {
            RefreshToken stored = storedToken(Instant.now().plus(7, ChronoUnit.DAYS));

            when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh.token"))
                    .thenReturn(Optional.of(stored));
            when(jwtService.isTokenValid("refresh.token", verifiedUser)).thenReturn(true);

            AuthResponse response = authService.refreshToken(refreshRequest("refresh.token"));

            assertThat(response.getAccessToken()).isEqualTo("access.token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
        }

        @Test
        @DisplayName("odbija nepoznat token")
        void rejectsUnknownToken() {
            when(refreshTokenRepository.findByTokenAndRevokedFalse("nepostoji"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(refreshRequest("nepostoji")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("istekli token poništava i odbija")
        void revokesExpiredToken() {
            RefreshToken stored = storedToken(Instant.now().minus(1, ChronoUnit.DAYS));

            when(refreshTokenRepository.findByTokenAndRevokedFalse("stari"))
                    .thenReturn(Optional.of(stored));

            assertThatThrownBy(() -> authService.refreshToken(refreshRequest("stari")))
                    .isInstanceOf(UnauthorizedException.class);

            assertThat(stored.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(stored);
        }

        @Test
        @DisplayName("odbija token koji ne prolazi provjeru potpisa")
        void rejectsInvalidSignature() {
            RefreshToken stored = storedToken(Instant.now().plus(7, ChronoUnit.DAYS));

            when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh.token"))
                    .thenReturn(Optional.of(stored));
            when(jwtService.isTokenValid("refresh.token", verifiedUser)).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(refreshRequest("refresh.token")))
                    .isInstanceOf(UnauthorizedException.class);

            verify(jwtService, never()).generateAccessToken(any());
        }

        private RefreshToken storedToken(Instant expiresAt) {
            RefreshToken t = RefreshToken.builder()
                    .token("refresh.token")
                    .user(verifiedUser)
                    .expiresAt(expiresAt)
                    .build();
            t.setId(UUID.randomUUID());
            return t;
        }
    }

    @Nested
    @DisplayName("Odjava")
    class LoggingOut {

        @Test
        @DisplayName("stavlja pristupni token na crnu listu")
        void blacklistsAccessToken() {
            Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);
            when(jwtService.getExpirationInstant("access.token")).thenReturn(expiry);

            authService.logout("access.token", null);

            verify(tokenBlacklistRepository).save(argThat(t ->
                    t.getToken().equals("access.token") && t.getExpiresAt().equals(expiry)));
        }

        @Test
        @DisplayName("poništava token za osvježavanje")
        void revokesRefreshToken() {
            RefreshToken stored = RefreshToken.builder()
                    .token("refresh.token")
                    .user(verifiedUser)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();
            stored.setId(UUID.randomUUID());

            when(refreshTokenRepository.findByToken("refresh.token"))
                    .thenReturn(Optional.of(stored));

            authService.logout(null, "refresh.token");

            assertThat(stored.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(stored);
        }

        @Test
        @DisplayName("prazni tokeni ne rade ništa")
        void ignoresEmptyTokens() {
            authService.logout("", "");

            verify(tokenBlacklistRepository, never()).save(any(TokenBlacklist.class));
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("izostali tokeni ne rade ništa")
        void ignoresNullTokens() {
            authService.logout(null, null);

            verify(tokenBlacklistRepository, never()).save(any(TokenBlacklist.class));
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("nepoznat token za osvježavanje se tiho preskače")
        void ignoresUnknownRefreshToken() {
            when(refreshTokenRepository.findByToken("nepostoji")).thenReturn(Optional.empty());

            authService.logout(null, "nepostoji");

            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("neispravan pristupni token ne ruši odjavu")
        void survivesBadAccessToken() {
            when(jwtService.getExpirationInstant("pokvaren"))
                    .thenThrow(new IllegalArgumentException("Malformed JWT"));

            authService.logout("pokvaren", null);

            verify(tokenBlacklistRepository, never()).save(any(TokenBlacklist.class));
        }

        @Test
        @DisplayName("odjava sa svih uređaja poništava sve tokene korisnika")
        void logsOutEverywhere() {
            authService.logoutAllDevices(verifiedUser);

            verify(refreshTokenRepository).revokeAllUserTokens(verifiedUser);
        }
    }

    @Nested
    @DisplayName("Potvrda e-pošte")
    class EmailVerification {

        @Test
        @DisplayName("potvrđuje adresu i briše token")
        void verifiesAndClearsToken() {
            User pending = pendingUser(Instant.now().plus(12, ChronoUnit.HOURS));
            when(userRepository.findByVerificationToken("token123"))
                    .thenReturn(Optional.of(pending));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            authService.verifyEmail("token123");

            assertThat(pending.isEmailVerified()).isTrue();
            assertThat(pending.getVerificationToken()).isNull();
            assertThat(pending.getVerificationTokenExpiry()).isNull();
        }

        @Test
        @DisplayName("odbija nepostojeći token")
        void rejectsUnknownToken() {
            when(userRepository.findByVerificationToken("nepostoji"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("nepostoji"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("nije valjana");
        }

        @Test
        @DisplayName("odbija istekli token")
        void rejectsExpiredToken() {
            User pending = pendingUser(Instant.now().minus(1, ChronoUnit.HOURS));
            when(userRepository.findByVerificationToken("stari"))
                    .thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> authService.verifyEmail("stari"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("istekla");

            assertThat(pending.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("odbija token bez roka valjanosti")
        void rejectsTokenWithoutExpiry() {
            User pending = pendingUser(null);
            when(userRepository.findByVerificationToken("bez-roka"))
                    .thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> authService.verifyEmail("bez-roka"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("ne radi ništa ako je adresa već potvrđena")
        void doesNothingWhenAlreadyVerified() {
            verifiedUser.setVerificationToken("vec-potvrden");
            when(userRepository.findByVerificationToken("vec-potvrden"))
                    .thenReturn(Optional.of(verifiedUser));

            authService.verifyEmail("vec-potvrden");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Ponovno slanje poveznice")
    class ResendVerification {

        @Test
        @DisplayName("šalje novu poveznicu nepotvrđenom računu")
        void sendsNewLink() {
            User pending = pendingUser(Instant.now().plus(1, ChronoUnit.HOURS));
            when(userRepository.findByEmail("ceka@test.hr"))
                    .thenReturn(Optional.of(pending));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            authService.resendVerification("ceka@test.hr");

            verify(emailService).sendVerificationEmail(any(User.class), anyString());
        }

        @Test
        @DisplayName("ne otkriva da račun ne postoji")
        void silentForUnknownEmail() {
            when(userRepository.findByEmail("nitko@test.hr")).thenReturn(Optional.empty());

            authService.resendVerification("nitko@test.hr");

            verify(emailService, never()).sendVerificationEmail(any(), anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("ne šalje ništa za već potvrđen račun")
        void silentForVerifiedAccount() {
            when(userRepository.findByEmail("ana@test.hr"))
                    .thenReturn(Optional.of(verifiedUser));

            authService.resendVerification("ana@test.hr");

            verify(emailService, never()).sendVerificationEmail(any(), anyString());
        }
    }

    @Nested
    @DisplayName("Dohvat prijavljenog korisnika")
    class CurrentUser {

        @Test
        @DisplayName("vraća podatke korisnika")
        void returnsUserData() {
            when(userRepository.findByIdWithSchoolClass(verifiedUser.getId()))
                    .thenReturn(Optional.of(verifiedUser));

            var result = authService.getCurrentUser(verifiedUser.getId());

            assertThat(result.getEmail()).isEqualTo("ana@test.hr");
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg korisnika")
        void throwsForUnknownUser() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findByIdWithSchoolClass(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private RegisterRequest request(String email) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword("Lozinka123");
        r.setFirstName("Ivo");
        r.setLastName("Ivić");
        return r;
    }

    private LoginRequest loginRequest(String email) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword("Lozinka123");
        return r;
    }

    private RefreshTokenRequest refreshRequest(String token) {
        RefreshTokenRequest r = new RefreshTokenRequest();
        r.setRefreshToken(token);
        return r;
    }

    private User pendingUser(Instant expiry) {
        User u = User.builder()
                .email("ceka@test.hr")
                .password("$2a$10$hash")
                .firstName("Ivo")
                .lastName("Ivić")
                .role(UserRole.GUEST)
                .isActive(false)
                .emailVerified(false)
                .authProvider(AuthProvider.LOCAL)
                .verificationToken("token123")
                .verificationTokenExpiry(expiry)
                .build();
        u.setId(UUID.randomUUID());
        return u;
    }
}