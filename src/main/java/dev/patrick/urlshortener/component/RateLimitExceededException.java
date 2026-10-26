package dev.patrick.urlshortener.component;

public class RateLimitExceededException extends RuntimeException{
    public RateLimitExceededException() {
        super("Rate limit exceeded.");
    }
}
