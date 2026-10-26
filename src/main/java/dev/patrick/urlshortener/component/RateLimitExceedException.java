package dev.patrick.urlshortener.component;

public class RateLimitExceedException extends RuntimeException{
    public RateLimitExceedException() {
        super("Rate limit exceeded.");
    }
}
