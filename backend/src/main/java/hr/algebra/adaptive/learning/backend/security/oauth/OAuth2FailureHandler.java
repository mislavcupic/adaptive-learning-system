package hr.algebra.adaptive.learning.backend.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.warn("Google prijava nije uspjela: {}", exception.getMessage());

        String message = exception.getMessage() != null
                ? exception.getMessage()
                : "Prijava putem Googlea nije uspjela.";

        getRedirectStrategy().sendRedirect(request, response,
                frontendUrl + "/login?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
