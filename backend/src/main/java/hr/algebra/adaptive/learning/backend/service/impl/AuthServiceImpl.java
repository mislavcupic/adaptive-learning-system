package hr.algebra.adaptive.learning.backend.service.impl;


import hr.algebra.adaptive.learning.backend.domain.entity.RefreshToken;
import hr.algebra.adaptive.learning.backend.domain.entity.TokenBlacklist;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.AuthProvider;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.LoginRequest;
import hr.algebra.adaptive.learning.backend.dto.request.RefreshTokenRequest;
import hr.algebra.adaptive.learning.backend.dto.request.RegisterRequest;
import hr.algebra.adaptive.learning.backend.dto.response.AuthResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.exception.UnauthorizedException;
import hr.algebra.adaptive.learning.backend.repository.RefreshTokenRepository;
import hr.algebra.adaptive.learning.backend.repository.TokenBlacklistRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.security.JwtService;
import hr.algebra.adaptive.learning.backend.service.AuthService;
import hr.algebra.adaptive.learning.backend.service.EmailService;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long VERIFICATION_TOKEN_VALID_HOURS = 24;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MessageService messageService;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        User existing = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Ako racun postoji, ali e-mail nikad nije potvrden, posalji novu poveznicu
        // umjesto da korisnika zaustavimo porukom da adresa vec postoji.
        if (existing != null) {
            if (!existing.isEmailVerified() && existing.getAuthProvider() == AuthProvider.LOCAL) {
                issueVerificationToken(existing);
                userRepository.save(existing);
                emailService.sendVerificationEmail(existing, existing.getVerificationToken());

                return AuthResponse.builder()
                        .user(UserResponse.fromEntity(existing))
                        .message("Poslali smo novu poveznicu za potvrdu na vašu e-mail adresu.")
                        .build();
            }
            throw new IllegalArgumentException(messageService.emailExists());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.GUEST)
                .isActive(false)
                .emailVerified(false)
                .authProvider(AuthProvider.LOCAL)
                .build();

        issueVerificationToken(user);

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(savedUser, savedUser.getVerificationToken());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(savedUser))
                .message("Registracija uspješna. Provjerite e-mail i potvrdite adresu.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        // Provjeri stanje racuna prije autentikacije kako bismo mogli
        // vratiti tocnu poruku umjesto opceg "neispravni podaci".
        User candidate = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (candidate != null && !candidate.isEmailVerified()) {
            log.warn("Login blocked, email not verified: {}", request.getEmail());
            throw new UnauthorizedException(
                    "E-mail adresa nije potvrđena. Provjerite poštu ili zatražite novu poveznicu.");
        }
        if (candidate != null && !candidate.isActive()) {
            log.warn("Login blocked, account not approved: {}", request.getEmail());
            throw new UnauthorizedException(
                    "Vaš račun čeka odobrenje nastavnika.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}, exception message: {}", request.getEmail(), e.getMessage());
            throw new UnauthorizedException(messageService.invalidCredentials());
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.userNotFound()));

        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("User logged in successfully: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        log.info("Verifying email with token");

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Poveznica za potvrdu nije valjana ili je već iskorištena."));

        if (user.isEmailVerified()) {
            log.info("Email already verified: {}", user.getEmail());
            return;
        }

        if (user.getVerificationTokenExpiry() == null
                || user.getVerificationTokenExpiry().isBefore(Instant.now())) {
            throw new BadRequestException(
                    "Poveznica za potvrdu je istekla. Zatražite novu.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        log.info("Resend verification requested for: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);

        // Namjerno ne otkrivamo postoji li racun s tom adresom.
        if (user == null || user.isEmailVerified()) {
            return;
        }

        issueVerificationToken(user);
        userRepository.save(user);
        emailService.sendVerificationEmail(user, user.getVerificationToken());
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException(messageService.refreshTokenInvalid()));

        if (storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException(messageService.getMessage("auth.refresh.token.expired"));
        }

        User user = storedToken.getUser();

        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            throw new UnauthorizedException(messageService.refreshTokenInvalid());
        }

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

    private void issueVerificationToken(User user) {
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(
                Instant.now().plus(VERIFICATION_TOKEN_VALID_HOURS, ChronoUnit.HOURS));
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user);

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