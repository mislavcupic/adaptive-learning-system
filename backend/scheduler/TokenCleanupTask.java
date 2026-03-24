package hr.algebra.backend.scheduler;

import hr.algebra.backend.repository.RefreshTokenRepository;
import hr.algebra.backend.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Čisti istekle tokene svaki sat
     */
    @Scheduled(cron = "0 0 * * * *") // Svaki sat u 0 minuta
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting token cleanup task");
        
        Instant now = Instant.now();
        
        // Obriši istekle refresh tokene
        refreshTokenRepository.deleteExpiredTokens(now);
        log.debug("Expired refresh tokens cleaned up");
        
        // Obriši istekle blacklisted tokene
        tokenBlacklistRepository.deleteExpiredTokens(now);
        log.debug("Expired blacklisted tokens cleaned up");
        
        log.info("Token cleanup task completed");
    }
}
