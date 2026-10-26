package dev.patrick.urlshortener.component;

public class CaptchaUnavailableException extends RuntimeException{
    public CaptchaUnavailableException(String message) {
        super(message);
    }
}
