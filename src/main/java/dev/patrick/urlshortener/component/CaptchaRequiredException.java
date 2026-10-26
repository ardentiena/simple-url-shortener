package dev.patrick.urlshortener.component;

public class CaptchaRequiredException extends RuntimeException{
    private final String siteKey;
    public CaptchaRequiredException(String message, String siteKey) {
        super(message);
        this.siteKey = siteKey;
    }

    public String getSiteKey() {
        return siteKey;
    }
}
