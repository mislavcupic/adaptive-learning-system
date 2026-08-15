package hr.algebra.adaptive.learning.backend.security.oauth;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AuthProvider;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.oauth.allowed-domain:}")
    private String allowedDomain;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        Boolean googleVerified = (Boolean) attributes.get("email_verified");
        String providerId = (String) attributes.get("sub");
        String givenName = (String) attributes.getOrDefault("given_name", "");
        String familyName = (String) attributes.getOrDefault("family_name", "");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_missing"), "Google račun nema e-mail adresu.");
        }

        // Google je vec potvrdio vlasnistvo adrese; ako nije, ne vjerujemo mu.
        if (!Boolean.TRUE.equals(googleVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"),
                    "Google e-mail adresa nije potvrđena.");
        }

        // Opcionalno ogranicenje na domenu skole.
        if (allowedDomain != null && !allowedDomain.isBlank()
                && !email.toLowerCase().endsWith("@" + allowedDomain.toLowerCase())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("domain_not_allowed"),
                    "Dozvoljene su samo adrese domene " + allowedDomain + ".");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Novi korisnik ide u cekaonicu, jednako kao klasicna registracija.
            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .firstName(givenName.isBlank() ? "Korisnik" : givenName)
                    .lastName(familyName.isBlank() ? "Google" : familyName)
                    .role(UserRole.GUEST)
                    .isActive(false)
                    .emailVerified(true)
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .build();
            user = userRepository.save(user);
            log.info("Novi korisnik preko Googlea: {}", email);
        } else {
            // Postojeci racun: poveži s Googleom i potvrdi e-mail.
            boolean changed = false;
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                user.setVerificationToken(null);
                user.setVerificationTokenExpiry(null);
                changed = true;
            }
            if (user.getProviderId() == null) {
                user.setProviderId(providerId);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
            log.info("Postojeci korisnik prijavljen preko Googlea: {}", email);
        }

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "sub"
        );
    }
}
