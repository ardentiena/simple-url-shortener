package dev.patrick.urlshortener.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RateLimiter {
    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final int CAPTCHA_REQUIRED_AFTER_ATTEMPTS = 5;
    private static final long WINDOW_DURATION_MILLIS = 60_000;

    private final ConcurrentHashMap<String, AttemptWindow> attemptWindowByClientIp = new ConcurrentHashMap<>();

    public AttemptDecision registerAttempt(String clientIp) {
        long currentTimeMillis = System.currentTimeMillis();
        AtomicReference<AttemptDecision> decision = new AtomicReference<>(AttemptDecision.BLOCKED);

        attemptWindowByClientIp.compute(clientIp, (ipKey, currentWindow) -> {
            boolean windowMissingOrExpired = currentWindow == null
                    || currentTimeMillis - currentWindow.startTimeMillis() >= WINDOW_DURATION_MILLIS;
            if (windowMissingOrExpired) {
                decision.set(decisionForAttemptNumber(1));
                return new AttemptWindow(currentTimeMillis, 1);
            }
            if (currentWindow.attemptCount() < MAX_ATTEMPTS_PER_WINDOW) {
                int newAttemptCount = currentWindow.attemptCount() + 1;
                decision.set(decisionForAttemptNumber(newAttemptCount));
                return new AttemptWindow(currentWindow.startTimeMillis(), newAttemptCount);
            }
            return currentWindow;
        });

        return decision.get();
    }

    private AttemptDecision decisionForAttemptNumber(int attemptNumber) {
        return attemptNumber > CAPTCHA_REQUIRED_AFTER_ATTEMPTS
                ? AttemptDecision.CAPTCHA_REQUIRED
                : AttemptDecision.ALLOWED;
    }

    @Scheduled(fixedRate = WINDOW_DURATION_MILLIS)
    void removeExpiredWindows() {
        long currentTimeMillis = System.currentTimeMillis();
        attemptWindowByClientIp.entrySet().removeIf(
                entry -> currentTimeMillis - entry.getValue().startTimeMillis() >= WINDOW_DURATION_MILLIS);
    }

    private record AttemptWindow(long startTimeMillis, int attemptCount) {}
}
