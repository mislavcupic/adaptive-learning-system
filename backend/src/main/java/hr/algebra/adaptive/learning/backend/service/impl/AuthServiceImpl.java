package hr.algebra.adaptive.learning.backend.service.impl;


import hr.algebra.adaptive.learning.backend.domain.entity.RefreshToken;
import hr.algebra.adaptive.learning.backend.domain.entity.TokenBlacklist;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.request.LoginRequest;
import hr.algebra.adaptive.learning.backend.dto.request.RefreshTokenRequest;
import hr.algebra.adaptive.learning.backend.dto.request.RegisterRequest;
import hr.algebra.adaptive.learning.backend.dto.response.AuthResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.exception.UnauthorizedException;
import hr.algebra.adaptive.learning.backend.repository.RefreshTokenRepository;
import hr.algebra.adaptive.learning.backend.repository.TokenBlacklistRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.AuthService;
import hr.algebra.adaptive.learning.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MessageService messageService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Provjeri postoji li korisnik
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(messageService.emailExists());
        }

        // Kreiraj korisnika
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generiraj tokene
        return generateAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        try {
            // Autenticiraj korisnika
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException _) {
            log.warn("Failed login attempt for user: {}", request.getEmail());
            throw new UnauthorizedException(messageService.invalidCredentials());
        }

        // Dohvati korisnika
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.userNotFound()));

        // Poništi prethodne refresh tokene
        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("User logged in successfully: {}", user.getEmail());

        // Generiraj tokene
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        // Pronađi refresh token u bazi
        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException(messageService.refreshTokenInvalid()));

        // Provjeri je li istekao
        if (storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException(messageService.getMessage("auth.refresh.token.expired"));
        }

        User user = storedToken.getUser();

        // Validiraj token s JwtService
        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            throw new UnauthorizedException(messageService.refreshTokenInvalid());
        }

        // Generiraj novi access token
        String accessToken = jwtService.generateAccessToken(user);

        log.info("Token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(request.getRefreshToken())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        log.info("Processing logout");

        // Dodaj access token u blacklist
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                Instant expiration = jwtService.getExpirationInstant(accessToken);
                TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                        .token(accessToken)
                        .expiresAt(expiration)
                        .build();
                tokenBlacklistRepository.save(blacklistedToken);
                log.debug("Access token blacklisted");
            } catch (Exception e) {
                log.warn("Could not blacklist access token: {}", e.getMessage());
            }
        }

        // Poništi refresh token
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                        log.debug("Refresh token revoked");
                    });
        }

        log.info("Logout completed successfully");
    }

    @Override
    @Transactional
    public void logoutAllDevices(User user) {
        log.info("Logging out user from all devices: {}", user.getEmail());
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    // ==================== PRIVATE METHODS ====================

    private AuthResponse generateAuthResponse(User user) {
        // Generiraj tokene
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user);

        // Spremi refresh token u bazu
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
