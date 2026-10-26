package dev.patrick.urlshortener.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateLimiterTest {
    private final RateLimiter rateLimiter = new RateLimiter();

    private void registeredAttempts(String clientIp, int attemptCount) {
        for(int attemptNumber = 1; attemptNumber <= attemptCount; attemptNumber++) {
            rateLimiter.registerAttempt(clientIp);
        }
    }

    @Test
    void registerAttempt_allowFirstFiveAttempts() {
        for(int attemptNumber = 1; attemptNumber <= 5; attemptNumber++) {
            assertEquals(AttemptDecision.ALLOWED, rateLimiter.registerAttempt("203.0.113.1"));
        }
    }

    @Test
    void registerAttempt_requireCaptchaFromSixthToTenthAttempt() {
        registeredAttempts("203.0.113.1", 5);

        for(int attemptNumber = 6; attemptNumber <= 10; attemptNumber++) {
            assertEquals(AttemptDecision.CAPTCHA_REQUIRED, rateLimiter.registerAttempt("203.0.113.1"));
        }
    }

    @Test
    void registerAttempt_blocksAfterTenthAttempt() {
        registeredAttempts("203.0.113.1", 10);

        assertEquals(AttemptDecision.BLOCKED, rateLimiter.registerAttempt("203.0.113.1"));
        assertEquals(AttemptDecision.BLOCKED, rateLimiter.registerAttempt("203.0.113.1"));
    }

    @Test
    void registerAttempt_tracksClientIndependently() {
        registeredAttempts("203.0.113.1", 10);
        assertEquals(AttemptDecision.ALLOWED, rateLimiter.registerAttempt("203.0.113.2"));
    }
}
