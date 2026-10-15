package hr.algebra.adaptive.learning.backend.filter;

import hr.algebra.adaptive.learning.backend.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.payload.potpis";
    private static final String EMAIL = "ines@test.hr";
    private static final String AUTH_HEADER = "Authorization";
    private static final String TEACHER_ROLE = "ROLE_TEACHER";

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/courses");
        response = new MockHttpServletResponse();

        userDetails = User.withUsername(EMAIL)
                .password("hash")
                .authorities(List.of(new SimpleGrantedAuthority(TEACHER_ROLE)))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Zahtjevi bez upotrebljivog zaglavlja")
    class NoUsableHeader {

        @ParameterizedTest(name = "zaglavlje: {0}")
        @NullSource
        @ValueSource(strings = {"", "Basic dXNlcjpwYXNz", "Token abc", "bearer malo-slovo"})
        @DisplayName("zahtjev bez Bearer zaglavlja prolazi dalje netaknut")
        void passesThrough(String header) throws Exception {
            if (header != null) {
                request.addHeader(AUTH_HEADER, header);
            }

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService, userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Valjani token")
    class ValidToken {

        @BeforeEach
        void stubValidToken() {
            bearer();
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);
        }

        @Test
        @DisplayName("postavlja autentikaciju u kontekst")
        void setsAuthentication() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(EMAIL);
            assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        }

        @Test
        @DisplayName("prenosi ovlasti korisnika")
        void copiesAuthorities() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                    .extracting("authority")
                    .containsExactly(TEACHER_ROLE);
        }

        @Test
        @DisplayName("biljezi podatke o zahtjevu")
        void setsRequestDetails() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails())
                    .isInstanceOf(WebAuthenticationDetails.class);
        }

        @Test
        @DisplayName("token se cita bez Bearer prefiksa")
        void stripsPrefix() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(jwtService).extractUsername(TOKEN);
        }

        @Test
        @DisplayName("lanac se nastavlja tocno jednom")
        void continuesChainOnce() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Token koji se odbija")
    class RejectedToken {

        @Test
        @DisplayName("nevazeci token ne postavlja autentikaciju")
        void invalidToken() throws Exception {
            bearer();
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("token bez subjekta ne dohvaca korisnika")
        void nullUsername() throws Exception {
            bearer();
            when(jwtService.extractUsername(TOKEN)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("ostecen token ne rusi lanac")
        void malformedToken() throws Exception {
            bearer();
            when(jwtService.extractUsername(TOKEN))
                    .thenThrow(new IllegalArgumentException("JWT signature does not match"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("nepostojeci korisnik ne rusi lanac")
        void unknownUser() throws Exception {
            bearer();
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL))
                    .thenThrow(new UsernameNotFoundException("user.not.found"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("odbijen token ne mijenja status odgovora")
        void keepsResponseUntouched() throws Exception {
            bearer();
            when(jwtService.extractUsername(TOKEN)).thenThrow(new JwtException("neispravan token"));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Postojeca sesija")
    class ExistingSession {

        @BeforeEach
        void putStaleAuthenticationInContext() {
            Authentication stale = new UsernamePasswordAuthenticationToken(
                    "ana@test.hr", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
            SecurityContextHolder.getContext().setAuthentication(stale);
        }

        @Test
        @DisplayName("valjan token pregazuje autentikaciju iz sesije")
        void tokenOverridesSession() throws Exception {
            bearer();
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getName()).isEqualTo(EMAIL);
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly(TEACHER_ROLE);
        }

        @Test
        @DisplayName("nevazeci token ostavlja sesiju kakva jest")
        void invalidTokenLeavesSession() throws Exception {
            bearer();
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                    .isEqualTo("ana@test.hr");
        }

        @Test
        @DisplayName("zahtjev bez tokena ne dira sesiju")
        void noTokenLeavesSession() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                    .isEqualTo("ana@test.hr");
            verifyNoInteractions(jwtService, userDetailsService);
        }
    }

    private void bearer() {
        request.addHeader(AUTH_HEADER, "Bearer " + TOKEN);
    }
}