package hr.algebra.adaptive.learning.backend.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sigurnosna konfiguracija za testove kontrolera.
 *
 * Prava konfiguracija povlaci JWT filtar, OAuth2 klijenta i vezu prema
 * bazi, sto je za test web sloja nepotrebno. Ovdje se zadrzava samo
 * provjera uloga preko @PreAuthorize, koja je zapravo ono sto se testira.
 */
@TestConfiguration
@EnableMethodSecurity
public class ControllerTestSecurityConfig {

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .anonymous(Customizer.withDefaults());

        return http.build();
    }
}
