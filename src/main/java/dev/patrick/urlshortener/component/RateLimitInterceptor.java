package dev.patrick.urlshortener.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor{
    private static final String CAPTCHA_TOKEN_HEADER = "X-Turnstile-Token";

    private final RateLimiter rateLimiter;
    private final TurnstileVerifier turnstileVerifier;

    public RateLimitInterceptor(RateLimiter rateLimiter, TurnstileVerifier turnstileVerifier) {
        this.rateLimiter = rateLimiter;
        this.turnstileVerifier = turnstileVerifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = request.getRemoteAddr();

        switch (rateLimiter.registerAttempt(clientIp)) {
            case BLOCKED -> throw new RateLimitExceededException();
            case CAPTCHA_REQUIRED -> requireValidCaptcha(request, clientIp);
            case ALLOWED -> { }
        }
        return true;
    }

    private void requireValidCaptcha(HttpServletRequest request, String clientIp) {
        if (!turnstileVerifier.isEnabled()) {
            return;
        }
        String captchaToken = request.getHeader(CAPTCHA_TOKEN_HEADER);
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new CaptchaRequiredException(
                    "CAPTCHA verification is required for this request.", turnstileVerifier.getSiteKey());
        }
        if (!turnstileVerifier.isTokenValid(captchaToken, clientIp)) {
            throw new CaptchaRequiredException(
                    "CAPTCHA verification failed, please try again.", turnstileVerifier.getSiteKey());
        }
    }
}
