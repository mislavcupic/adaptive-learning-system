package hr.algebra.adaptive.learning.backend.security.oauth;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AuthProvider;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2UserPrincipal")
class OAuth2UserPrincipalTest {

    private User source;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        source = User.builder()
                .email("ines@gmail.com")
                .password("hash")
                .firstName("Ines")
                .lastName("Inic")
                .role(UserRole.TEACHER)
                .isActive(true)
                .emailVerified(true)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("111545373032881004436")
                .build();
        source.setId(UUID.randomUUID());

        attributes = Map.of(
                "email", "ines@gmail.com",
                "sub", "111545373032881004436",
                "given_name", "Ines",
                "family_name", "Inic");
    }

    @Test
    @DisplayName("ime je e-mail, a ne Google sub")
    void nameIsEmail() {
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(source, attributes);

        assertThat(principal.getName()).isEqualTo("ines@gmail.com");
        assertThat(principal.getName()).isNotEqualTo(source.getProviderId());
    }

    @Test
    @DisplayName("zadrzava Googleove atribute")
    void keepsAttributes() {
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(source, attributes);

        assertThat(principal.getAttributes()).containsEntry("sub", "111545373032881004436");
        assertThat(principal.<String>getAttribute("email")).isEqualTo("ines@gmail.com");
    }

    @Test
    @DisplayName("preslikava polja ucitanog korisnika")
    void copiesUserFields() {
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(source, attributes);

        assertThat(principal.getId()).isEqualTo(source.getId());
        assertThat(principal.getEmail()).isEqualTo(source.getEmail());
        assertThat(principal.getFirstName()).isEqualTo("Ines");
        assertThat(principal.getLastName()).isEqualTo("Inic");
        assertThat(principal.getRole()).isEqualTo(UserRole.TEACHER);
        assertThat(principal.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(principal.getProviderId()).isEqualTo("111545373032881004436");
        assertThat(principal.isActive()).isTrue();
        assertThat(principal.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("ponasa se kao obican User prema kontrolerima")
    void behavesAsUser() {
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(source, attributes);

        assertThat(principal).isInstanceOf(User.class);
        assertThat(principal.getUsername()).isEqualTo("ines@gmail.com");
        assertThat(principal.getAuthorities()).isNotEmpty();
    }

    @Test
    @DisplayName("neaktivan racun ostaje neaktivan nakon omotavanja")
    void keepsInactiveFlag() {
        source.setActive(false);

        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(source, attributes);

        assertThat(principal.isActive()).isFalse();
    }
}