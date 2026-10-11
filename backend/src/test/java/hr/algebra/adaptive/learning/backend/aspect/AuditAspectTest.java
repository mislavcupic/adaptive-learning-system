package hr.algebra.adaptive.learning.backend.aspect;

import hr.algebra.adaptive.learning.backend.domain.entity.AuditLog;
import hr.algebra.adaptive.learning.backend.repository.AuditLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuditAspect")
class AuditAspectTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private Signature signature;

    private AuditAspect aspect;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        aspect = new AuditAspect(auditLogRepository);

        when(signature.getName()).thenReturn("createTask");
        when(joinPoint.getSignature()).thenReturn(signature);

        request = new MockHttpServletRequest("POST", "/api/tasks");
        request.setRemoteAddr("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Uspjesan poziv")
    class Success {

        @Test
        @DisplayName("vraca rezultat metode nepromijenjen")
        void returnsResult() throws Throwable {
            when(joinPoint.proceed()).thenReturn("ok");

            Object result = aspect.auditAction(joinPoint);

            assertThat(result).isEqualTo("ok");
        }

        @Test
        @DisplayName("zapisuje akciju, metodu i rutu")
        void savesRequestDetails() throws Throwable {
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            AuditLog log = captured();
            assertThat(log.getAction()).isEqualTo("createTask");
            assertThat(log.getHttpMethod()).isEqualTo("POST");
            assertThat(log.getEndpoint()).isEqualTo("/api/tasks");
            assertThat(log.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("mjeri trajanje poziva")
        void measuresDuration() throws Throwable {
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            assertThat(captured().getDurationMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Neuspjesan poziv")
    class Failure {

        @Test
        @DisplayName("iznimku propusta dalje")
        void rethrows() throws Throwable {
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("pukla veza"));

            assertThatThrownBy(() -> aspect.auditAction(joinPoint))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("pukla veza");
        }

        @Test
        @DisplayName("biljezi neuspjeh i poruku greske")
        void savesFailure() throws Throwable {
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("pukla veza"));

            try {
                aspect.auditAction(joinPoint);
            } catch (IllegalStateException ignored) {
            }

            AuditLog log = captured();
            assertThat(log.getErrorMessage()).isEqualTo("pukla veza");
        }

        @Test
        @DisplayName("pad zapisivanja ne rusi poziv")
        void swallowsRepositoryFailure() throws Throwable {
            when(joinPoint.proceed()).thenReturn("ok");
            when(auditLogRepository.save(any())).thenThrow(new RuntimeException("baza nedostupna"));

            Object result = aspect.auditAction(joinPoint);

            assertThat(result).isEqualTo("ok");
        }
    }

    @Nested
    @DisplayName("Korisnik iz sigurnosnog konteksta")
    class CurrentUser {

        @Test
        @DisplayName("prijavljeni korisnik ostavlja mail i ulogu")
        void authenticatedUser() throws Throwable {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "ines@test.hr",
                            "n/a",
                            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            AuditLog log = captured();
            assertThat(log.getUserEmail()).isEqualTo("ines@test.hr");
            assertThat(log.getUserRole()).isEqualTo("TEACHER");
        }

        @Test
        @DisplayName("preskace ovlasti bez ROLE_ prefiksa")
        void ignoresNonRoleAuthorities() throws Throwable {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "ana@test.hr",
                            "n/a",
                            List.of(new SimpleGrantedAuthority("SCOPE_read"),
                                    new SimpleGrantedAuthority("ROLE_STUDENT"))));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            assertThat(captured().getUserRole()).isEqualTo("STUDENT");
        }

        @Test
        @DisplayName("bez autentikacije polja korisnika ostaju prazna")
        void noAuthentication() throws Throwable {
            SecurityContextHolder.clearContext();
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            AuditLog log = captured();
            assertThat(log.getUserId()).isNull();
            assertThat(log.getUserEmail()).isNull();
            assertThat(log.getUserRole()).isNull();
        }

        @Test
        @DisplayName("anonimni korisnik se ne biljezi kao prijavljen")
        void anonymousUser() throws Throwable {
            SecurityContextHolder.getContext().setAuthentication(
                    new AnonymousAuthenticationToken(
                            "key",
                            "anonymousUser",
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            assertThat(captured().getUserEmail()).isNull();
        }
    }

    @Nested
    @DisplayName("IP adresa")
    class IpAddress {

        @Test
        @DisplayName("uzima remote addr kad nema proxy zaglavlja")
        void usesRemoteAddr() throws Throwable {
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            assertThat(captured().getIpAddress()).isEqualTo("10.0.0.5");
        }

        @Test
        @DisplayName("X-Forwarded-For ima prednost")
        void prefersForwardedHeader() throws Throwable {
            request.addHeader("X-Forwarded-For", "203.0.113.9");
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            assertThat(captured().getIpAddress()).isEqualTo("203.0.113.9");
        }

        @Test
        @DisplayName("iz lanca proxyja uzima prvu adresu")
        void takesFirstFromChain() throws Throwable {
            request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1, 172.16.0.1");
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.auditAction(joinPoint);

            assertThat(captured().getIpAddress()).isEqualTo("203.0.113.9");
        }
    }

    @Nested
    @DisplayName("Poziv izvan HTTP zahtjeva")
    class WithoutRequest {

        @Test
        @DisplayName("zapis se stvara i bez podataka o zahtjevu")
        void savesWithoutRequestData() throws Throwable {
            RequestContextHolder.resetRequestAttributes();
            when(joinPoint.proceed()).thenReturn("ok");

            Object result = aspect.auditAction(joinPoint);

            assertThat(result).isEqualTo("ok");
            AuditLog log = captured();
            assertThat(log.getAction()).isEqualTo("createTask");
            assertThat(log.getHttpMethod()).isNull();
            assertThat(log.getEndpoint()).isNull();
            assertThat(log.getIpAddress()).isNull();
        }
    }

    private AuditLog captured() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }
}
