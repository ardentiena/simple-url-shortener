package dev.patrick.urlshortener.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RateLimiter {
    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final long WINDOW_DURATION_MILLIS = 60_000;

    private final ConcurrentHashMap<String, AttemptWindow> attemptWindowByClientIp = new ConcurrentHashMap<>();

    public boolean isAttemptAllowed(String clientIp) {
        long currentTimeMillis = System.currentTimeMillis();
        AtomicBoolean attemptAllowed = new AtomicBoolean(false);

        attemptWindowByClientIp.compute(clientIp, (ipKey, currentWindow) -> {
            boolean windowMissingOrExpired = currentWindow == null
                    || currentTimeMillis - currentWindow.startTimeMillis() >= WINDOW_DURATION_MILLIS;
            if(windowMissingOrExpired){
                attemptAllowed.set(true);
                return new AttemptWindow(currentTimeMillis, 1);
            }
            if(currentWindow.attemptsCount() <  MAX_ATTEMPTS_PER_WINDOW){
                attemptAllowed.set(true);
                return new AttemptWindow(currentTimeMillis, currentWindow.attemptsCount() + 1);
            }
            return currentWindow;
        });
        return attemptAllowed.get();
    }

    @Scheduled(fixedRate = WINDOW_DURATION_MILLIS)
    void removeExpiredWindows(){
        long currentTimeMillis = System.currentTimeMillis();
        attemptWindowByClientIp.entrySet().removeIf(
                entry -> entry.getValue().startTimeMillis() <= currentTimeMillis);
    }

    private record AttemptWindow(long startTimeMillis, int attemptsCount) {}
}
