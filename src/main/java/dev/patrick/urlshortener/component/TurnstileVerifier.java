package dev.patrick.urlshortener.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

@Component
public class TurnstileVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(TurnstileVerifier.class);
    private static final Pattern SUCCES_PATTERN = Pattern.compile("\"success\"\s*:\\s*true");
    private static final int MAX_TOKEN_LENGTH = 2048;

    private final String verifyEndpoint;
    private final String sitekey;
    private final String secretKey;
    private final boolean enabled;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    public TurnstileVerifier(
            @Value("${turnstile.verify-endpoint:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String verifyEndpoint,
            @Value("${turnstile.site-key:}") String siteKey,
            @Value("${turnstile.secret-key:}") String secretKey,
            @Value("${turnstile.enabled:true}") boolean enabled,
            @Value("${turnstile.timeout:2s}") Duration requestTimeout){
        this.verifyEndpoint = verifyEndpoint;
        this.sitekey = siteKey;
        this.secretKey = secretKey;
        this.enabled = enabled;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSiteKey() {
        return sitekey;
    }

    public boolean isTokenValid(String captchaToken, String clientIp) {
        if(!enabled) {
            return true;
        }
        if(secretKey.isBlank()) {
            throw unavailable("secret key is not configured.");
        }
        if(captchaToken.length() >  MAX_TOKEN_LENGTH) {
            return false;
        }

        String formBody = "secret=" + encode(secretKey) + "&response=" + encode(captchaToken) + "&remoteip=" + encode(clientIp);

        try{
            HttpRequest verifyRequest = HttpRequest.newBuilder(URI.create(verifyEndpoint))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();
            HttpResponse<String> verifyResponse = httpClient.send(verifyRequest, HttpResponse.BodyHandlers.ofString());

            if(verifyResponse.statusCode() != 200){
                throw unavailable("Cloudflare returned HTTP " +  verifyResponse.statusCode());
            }

            String responseBody = verifyResponse.body();
            if(responseBody.contains("invalid-input-secret") || responseBody.contains("missing-input-secret")){
                throw unavailable("Cloudflare rejected the secret key");
            }
            return SUCCES_PATTERN.matcher(responseBody).find();
        } catch (IOException requestFailure) {
            throw unavailable("Request failed with: " + requestFailure.getClass().getSimpleName());
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw unavailable("Request was interrupted");
        }
    }

    private CaptchaUnavailableException unavailable(String reason) {
        LOG.warn("CAPTCHA verification unavailable: {}", reason);
        return new CaptchaUnavailableException(reason);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
