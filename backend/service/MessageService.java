package hr.algebra.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Dohvaća poruku za trenutni locale
     */
    public String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    /**
     * Dohvaća poruku s argumentima za trenutni locale
     */
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * Dohvaća poruku za specifični locale
     */
    public String getMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, locale);
    }

    /**
     * Dohvaća poruku s argumentima za specifični locale
     */
    public String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }

    // ==================== AUTH MESSAGES ====================

    public String loginSuccess() {
        return getMessage("auth.login.success");
    }

    public String registerSuccess() {
        return getMessage("auth.register.success");
    }

    public String logoutSuccess() {
        return getMessage("auth.logout.success");
    }

    public String refreshSuccess() {
        return getMessage("auth.refresh.success");
    }

    public String invalidCredentials() {
        return getMessage("auth.invalid.credentials");
    }

    public String emailExists() {
        return getMessage("auth.email.exists");
    }

    public String tokenInvalid() {
        return getMessage("auth.token.invalid");
    }

    public String tokenExpired() {
        return getMessage("auth.token.expired");
    }

    public String refreshTokenInvalid() {
        return getMessage("auth.refresh.token.invalid");
    }

    // ==================== USER MESSAGES ====================

    public String userNotFound() {
        return getMessage("user.not.found");
    }

    // ==================== GENERAL MESSAGES ====================

    public String serverError() {
        return getMessage("error.server");
    }

    public String notFound() {
        return getMessage("error.not.found");
    }

    public String validationError() {
        return getMessage("validation.error");
    }
}
