package dev.patrick.urlshortener.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RateLimitInterceptor implements HandlerInterceptor{
    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;
    private static final String RETRY_AFTER_SECONDS = "60";

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter){
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {
        String clientIp = request.getRemoteAddr();

        if(rateLimiter.isAttemptAllowed(clientIp)){
            return true;
        }

        response.setStatus(HTTP_STATUS_TOO_MANY_REQUESTS);
        response.setHeader("Retry-After", RETRY_AFTER_SECONDS);
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Limit is 10 attempts per minute\"}"
        );
        return false;
    }
}
