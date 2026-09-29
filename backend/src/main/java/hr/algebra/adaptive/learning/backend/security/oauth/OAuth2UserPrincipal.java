package hr.algebra.adaptive.learning.backend.security.oauth;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * Omotac koji Google korisnika predstavlja kao nasu User klasu.
 *
 * Bez ovoga Spring nakon Google prijave u sigurnosni kontekst stavlja
 * DefaultOAuth2User, pa svaki @AuthenticationPrincipal User parametar
 * dolazi kao null. Sve rute pisane za klasicnu prijavu tada padaju.
 *
 * Nasljedivanjem korisnika i implementacijom OAuth2User sucelja isti
 * objekt zadovoljava oba svijeta: OAuth2 tijek ga prihvaca kao svog
 * korisnika, a kontroleri ga vide kao obican User.
 */
public class OAuth2UserPrincipal extends User implements OAuth2User {

    private final transient Map<String, Object> attributes;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        this.attributes = attributes;

        // Preslikaj sva polja s ucitanog korisnika
        setId(user.getId());
        setCreatedAt(user.getCreatedAt());
        setUpdatedAt(user.getUpdatedAt());

        setEmail(user.getEmail());
        setPassword(user.getPassword());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setRole(user.getRole());
        setGroupType(user.getGroupType());
        setActive(user.isActive());
        setEmailVerified(user.isEmailVerified());
        setVerificationToken(user.getVerificationToken());
        setVerificationTokenExpiry(user.getVerificationTokenExpiry());
        setAuthProvider(user.getAuthProvider());
        setProviderId(user.getProviderId());
        setSchoolClass(user.getSchoolClass());
        setResearchGroup(user.getResearchGroup());
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Authentication.getName() vraca e-mail, jednako kao kod klasicne
     * prijave, pa se rute koje se oslanjaju na njega ponasaju isto.
     */
    @Override
    public String getName() {
        return getEmail();
    }
}