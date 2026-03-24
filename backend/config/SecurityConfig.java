package hr.algebra.backend.config;

import hr.algebra.backend.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfigurationSource;

    // Javne rute - bez autentikacije
    private static final String[] PUBLIC_URLS = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh-token"
    };

    // Swagger/OpenAPI (ako budeš koristio)
    private static final String[] SWAGGER_URLS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (koristimo JWT)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS konfiguracija
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Autorizacija
            .authorizeHttpRequests(auth -> auth
                // Javne rute
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers(SWAGGER_URLS).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Admin rute
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Teacher rute
                .requestMatchers("/teacher/**").hasAnyRole("ADMIN", "TEACHER")

                // Student rute
                .requestMatchers("/student/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                // Sve ostalo zahtijeva autentikaciju
                .anyRequest().authenticated()
            )

            // Stateless session (JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Authentication provider
            .authenticationProvider(authenticationProvider)

            // JWT filter prije UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
