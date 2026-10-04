package hr.algebra.adaptive.learning.backend.security;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.repository.TokenBlacklistRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET =
            "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNGNDQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==";
    private static final String OTHER_SECRET =
            "WkFYTFlWQVNQVVJVR09UQVNFQ1JFVEtFWUZPUlRFU1RJTkdQVVJQT1NFU09OTFlYWVoxMjM0NTY3ODkwQUJDREU=";

    private static final long ACCESS_EXPIRATION = 86_400_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;

    @Mock private TokenBlacklistRepository tokenBlacklistRepository;

    private JwtService jwtService;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(tokenBlacklistRepository);
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_EXPIRATION);

        userId = UUID.randomUUID();
        user = User.builder()
                .email("ana@test.hr")
                .password("hash")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .emailVerified(true)
                .build();
        user.setId(userId);

        when(tokenBlacklistRepository.existsByToken(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);
    }

    @Nested
    @DisplayName("Izdavanje tokena")
    class Generation {

        @Test
        @DisplayName("pristupni token sadrži e-mail kao subjekt")
        void accessTokenCarriesEmail() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.extractUsername(token)).isEqualTo("ana@test.hr");
        }

        @Test
        @DisplayName("pristupni token sadrži identifikator korisnika")
        void accessTokenCarriesUserId() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        }

        @Test
        @DisplayName("pristupni token sadrži ulogu")
        void accessTokenCarriesRole() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.extractRole(token)).isEqualTo("STUDENT");
        }

        @Test
        @DisplayName("token osvježavanja sadrži oznaku vrste")
        void refreshTokenIsMarked() {
            String token = jwtService.generateRefreshToken(user);

            String type = jwtService.extractClaim(token, c -> c.get("type", String.class));
            assertThat(type).isEqualTo("refresh");
        }

        @Test
        @DisplayName("token osvježavanja traje dulje od pristupnog")
        void refreshLastsLonger() {
            Date accessExpiry = jwtService.extractExpiration(
                    jwtService.generateAccessToken(user));
            Date refreshExpiry = jwtService.extractExpiration(
                    jwtService.generateRefreshToken(user));

            assertThat(refreshExpiry).isAfter(accessExpiry);
        }

        @Test
        @DisplayName("dva tokena za istog korisnika nose iste podatke")
        void consistentClaims() {
            String first = jwtService.generateAccessToken(user);
            String second = jwtService.generateAccessToken(user);

            assertThat(jwtService.extractUserId(first))
                    .isEqualTo(jwtService.extractUserId(second));
            assertThat(jwtService.extractRole(first))
                    .isEqualTo(jwtService.extractRole(second));
        }
    }

    @Nested
    @DisplayName("Provjera valjanosti")
    class Validation {

        @Test
        @DisplayName("prihvaća svježe izdan token")
        void acceptsFreshToken() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("odbija token izdan drugom korisniku")
        void rejectsTokenOfAnotherUser() {
            String token = jwtService.generateAccessToken(user);

            User other = User.builder()
                    .email("ivo@test.hr")
                    .password("hash")
                    .firstName("Ivo")
                    .lastName("Ivić")
                    .role(UserRole.STUDENT)
                    .isActive(true)
                    .build();
            other.setId(UUID.randomUUID());

            assertThat(jwtService.isTokenValid(token, other)).isFalse();
        }

        @Test
        @DisplayName("odbija token s crne liste")
        void rejectsBlacklistedToken() {
            String token = jwtService.generateAccessToken(user);
            when(tokenBlacklistRepository.existsByToken(token)).thenReturn(true);

            assertThat(jwtService.isTokenValid(token, user)).isFalse();
        }

        @Test
        @DisplayName("svjež token nije istekao")
        void freshTokenNotExpired() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("prepoznaje token na crnoj listi")
        void detectsBlacklist() {
            String token = jwtService.generateAccessToken(user);
            when(tokenBlacklistRepository.existsByToken(token)).thenReturn(true);

            assertThat(jwtService.isTokenBlacklisted(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("Otpornost na neispravne tokene")
    class Tampering {

        @Test
        @DisplayName("odbija token potpisan drugim ključem")
        void rejectsForeignSignature() {
            JwtService foreign = new JwtService(tokenBlacklistRepository);
            ReflectionTestUtils.setField(foreign, "secretKey", OTHER_SECRET);
            ReflectionTestUtils.setField(foreign, "accessTokenExpiration", ACCESS_EXPIRATION);
            ReflectionTestUtils.setField(foreign, "refreshTokenExpiration", REFRESH_EXPIRATION);

            String foreignToken = foreign.generateAccessToken(user);

            assertThatThrownBy(() -> jwtService.extractUsername(foreignToken))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("odbija izmijenjen token")
        void rejectsTamperedToken() {
            String token = jwtService.generateAccessToken(user);
            String tampered = token.substring(0, token.length() - 4) + "AAAA";

            assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("odbija besmislen niz umjesto tokena")
        void rejectsGarbage() {
            assertThatThrownBy(() -> jwtService.extractUsername("ovo-nije-token"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("prepoznaje istekli token")
        void detectsExpiredToken() {
            JwtService shortLived = new JwtService(tokenBlacklistRepository);
            ReflectionTestUtils.setField(shortLived, "secretKey", SECRET);
            ReflectionTestUtils.setField(shortLived, "accessTokenExpiration", -1000L);
            ReflectionTestUtils.setField(shortLived, "refreshTokenExpiration", -1000L);

            String expired = shortLived.generateAccessToken(user);

            assertThatThrownBy(() -> jwtService.isTokenExpired(expired))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("Trajanje tokena")
    class Expiration {

        @Test
        @DisplayName("vraća postavljeno trajanje pristupnog tokena")
        void returnsAccessExpiration() {
            assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(ACCESS_EXPIRATION);
        }

        @Test
        @DisplayName("vraća postavljeno trajanje tokena osvježavanja")
        void returnsRefreshExpiration() {
            assertThat(jwtService.getRefreshTokenExpiration()).isEqualTo(REFRESH_EXPIRATION);
        }

        @Test
        @DisplayName("vrijeme isteka je u budućnosti")
        void expiryIsInFuture() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.getExpirationInstant(token))
                    .isAfter(java.time.Instant.now());
        }
    }
}
