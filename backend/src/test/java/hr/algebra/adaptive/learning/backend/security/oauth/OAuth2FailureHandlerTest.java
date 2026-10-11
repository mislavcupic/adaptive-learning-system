package hr.algebra.adaptive.learning.backend.security.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2FailureHandler")
class OAuth2FailureHandlerTest {

    private static final String FRONTEND = "http://localhost:3000";

    private OAuth2FailureHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "frontendUrl", FRONTEND);
        request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("vraca korisnika na prijavu")
    void redirectsToLogin() throws Exception {
        handler.onAuthenticationFailure(request, response, error("Nesto je puklo"));

        assertThat(response.getRedirectedUrl()).startsWith(FRONTEND + "/login?error=");
    }

    @Test
    @DisplayName("poruku greske nosi u parametru")
    void carriesMessage() throws Exception {
        handler.onAuthenticationFailure(request, response,
                error("Dozvoljene su samo adrese domene skola.hr."));

        assertThat(response.getRedirectedUrl()).contains("skola.hr");
    }

    @Test
    @DisplayName("kodira razmake i dijakritiku")
    void encodesMessage() throws Exception {
        handler.onAuthenticationFailure(request, response,
                error("Google račun nema e-mail adresu."));

        String url = response.getRedirectedUrl();
        assertThat(url).doesNotContain(" ");
        assertThat(url).contains("%");
    }

    @Test
    @DisplayName("bez poruke koristi zadani tekst")
    void fallsBackToDefaultMessage() throws Exception {
        handler.onAuthenticationFailure(request, response, new SilentFailure());

        assertThat(response.getRedirectedUrl()).contains("Googlea");
    }

    private AuthenticationException error(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_request"), message);
    }

    private static class SilentFailure extends AuthenticationException {
        SilentFailure() {
            super(null);
        }
    }
}