package hr.algebra.adaptive.learning.backend.security.oauth;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AuthProvider;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomOAuth2UserService")
class CustomOAuth2UserServiceTest {

    private static final String EMAIL = "ines@skola.hr";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(service, "allowedDomain", "");
        when(passwordEncoder.encode(anyString())).thenReturn("hashirano");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Provjera Googleovih atributa")
    class Validation {

        @Test
        @DisplayName("prihvaca potvrdenu adresu")
        void acceptsVerifiedEmail() {
            assertThat(parse(attributes())).isNotNull();
        }

        @Test
        @DisplayName("odbija racun bez e-mail adrese")
        void rejectsMissingEmail() {
            Map<String, Object> attrs = attributes();
            attrs.remove("email");

            assertThatThrownBy(() -> parse(attrs))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessageContaining("e-mail");
        }

        @Test
        @DisplayName("odbija praznu e-mail adresu")
        void rejectsBlankEmail() {
            Map<String, Object> attrs = attributes();
            attrs.put("email", "   ");

            assertThatThrownBy(() -> parse(attrs))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }

        @Test
        @DisplayName("odbija nepotvrdenu adresu")
        void rejectsUnverifiedEmail() {
            Map<String, Object> attrs = attributes();
            attrs.put("email_verified", false);

            assertThatThrownBy(() -> parse(attrs))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessageContaining("potvr");
        }

        @Test
        @DisplayName("odbija kad Google ne posalje oznaku potvrde")
        void rejectsMissingVerifiedFlag() {
            Map<String, Object> attrs = attributes();
            attrs.remove("email_verified");

            assertThatThrownBy(() -> parse(attrs))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("Ogranicenje domene")
    class DomainRestriction {

        @Test
        @DisplayName("propusta adresu dozvoljene domene")
        void allowsMatchingDomain() {
            ReflectionTestUtils.setField(service, "allowedDomain", "skola.hr");

            assertThat(parse(attributes())).isNotNull();
        }

        @Test
        @DisplayName("velicina slova ne utjece na provjeru")
        void domainCheckIgnoresCase() {
            ReflectionTestUtils.setField(service, "allowedDomain", "SKOLA.HR");
            Map<String, Object> attrs = attributes();
            attrs.put("email", "Ines@Skola.hr");

            assertThat(parse(attrs)).isNotNull();
        }

        @Test
        @DisplayName("odbija adresu izvan dozvoljene domene")
        void rejectsForeignDomain() {
            ReflectionTestUtils.setField(service, "allowedDomain", "skola.hr");
            Map<String, Object> attrs = attributes();
            attrs.put("email", "netko@gmail.com");

            assertThatThrownBy(() -> parse(attrs))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasMessageContaining("skola.hr");
        }

        @Test
        @DisplayName("prazna postavka znaci da je svaka domena dozvoljena")
        void emptySettingAllowsAll() {
            Map<String, Object> attrs = attributes();
            attrs.put("email", "netko@gmail.com");

            assertThat(parse(attrs)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Novi korisnik")
    class NewUser {

        @BeforeEach
        void noExistingUser() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("stvara racun koji ceka odobrenje")
        void createsPendingAccount() {
            User saved = resolve(attributes());

            assertThat(saved.getRole()).isEqualTo(UserRole.GUEST);
            assertThat(saved.isActive()).isFalse();
            assertThat(saved.isEmailVerified()).isTrue();
            assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("sprema Googleov identifikator")
        void storesProviderId() {
            User saved = resolve(attributes());

            assertThat(saved.getProviderId()).isEqualTo("111545373032881004436");
        }

        @Test
        @DisplayName("postavlja nasumicnu lozinku koju nitko ne zna")
        void setsRandomPassword() {
            resolve(attributes());

            verify(passwordEncoder).encode(anyString());
        }

        @Test
        @DisplayName("preuzima ime i prezime s Googlea")
        void copiesName() {
            User saved = resolve(attributes());

            assertThat(saved.getFirstName()).isEqualTo("Ines");
            assertThat(saved.getLastName()).isEqualTo("Inic");
        }

        @Test
        @DisplayName("bez imena koristi zamjenske vrijednosti")
        void fallsBackWhenNameMissing() {
            Map<String, Object> attrs = attributes();
            attrs.remove("given_name");
            attrs.remove("family_name");

            User saved = resolve(attrs);

            assertThat(saved.getFirstName()).isEqualTo("Korisnik");
            assertThat(saved.getLastName()).isEqualTo("Google");
        }
    }

    @Nested
    @DisplayName("Postojeci korisnik")
    class ExistingUser {

        @Test
        @DisplayName("ne stvara novi racun")
        void doesNotCreateDuplicate() {
            User existing = existing(true, "111545373032881004436");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            User result = resolve(attributes());

            assertThat(result).isSameAs(existing);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("potvrduje e-mail ako prije nije bio potvrden")
        void verifiesEmail() {
            User existing = existing(false, "111545373032881004436");
            existing.setVerificationToken("stari-token");
            existing.setVerificationTokenExpiry(Instant.now().plusSeconds(86400));            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            User result = resolve(attributes());

            assertThat(result.isEmailVerified()).isTrue();
            assertThat(result.getVerificationToken()).isNull();
            assertThat(result.getVerificationTokenExpiry()).isNull();
            verify(userRepository).save(existing);
        }

        @Test
        @DisplayName("dopisuje Googleov identifikator racunu otvorenom lozinkom")
        void attachesProviderId() {
            User existing = existing(true, null);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            User result = resolve(attributes());

            assertThat(result.getProviderId()).isEqualTo("111545373032881004436");
            verify(userRepository).save(existing);
        }

        @Test
        @DisplayName("sprema samo jednom kad se mijenja vise polja")
        void savesOnceForMultipleChanges() {
            User existing = existing(false, null);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            resolve(attributes());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getProviderId()).isEqualTo("111545373032881004436");
        }

        @Test
        @DisplayName("ne dira racun kojem nista ne nedostaje")
        void leavesCompleteAccountAlone() {
            User existing = existing(true, "111545373032881004436");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            resolve(attributes());

            verifyNoMoreInteractions(ignoreStubs(userRepository));
        }
    }

    private Map<String, Object> attributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", EMAIL);
        attrs.put("email_verified", true);
        attrs.put("sub", "111545373032881004436");
        attrs.put("given_name", "Ines");
        attrs.put("family_name", "Inic");
        return attrs;
    }

    private Object parse(Map<String, Object> attributes) {
        return ReflectionTestUtils.invokeMethod(service, "parseAndValidateAttributes", attributes);
    }

    private User resolve(Map<String, Object> attributes) {
        return ReflectionTestUtils.invokeMethod(
                service, "getOrCreateOrUpdateUser", parse(attributes));
    }

    private User existing(boolean emailVerified, String providerId) {
        User u = User.builder()
                .email(EMAIL)
                .password("hash")
                .firstName("Ines")
                .lastName("Inic")
                .role(UserRole.TEACHER)
                .isActive(true)
                .emailVerified(emailVerified)
                .authProvider(AuthProvider.LOCAL)
                .providerId(providerId)
                .build();
        u.setId(UUID.randomUUID());
        return u;
    }
}
