package dev.patrick.urlshortener.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class TurnstileVerifierTest {
    private HttpServer fakeCloudflareServer;
    private String fakeEndpoint;
    private final AtomicInteger receivedRequestCount = new AtomicInteger();

    private void startFakeCloudflare(int statusCode, String responseBody) throws IOException {
        fakeCloudflareServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeCloudflareServer.createContext("/", exchange -> {
            receivedRequestCount.incrementAndGet();
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream responseStream = exchange.getResponseBody()) {
                responseStream.write(responseBytes);
            }
        });
        fakeCloudflareServer.start();
        fakeEndpoint = "http://127.0.0.1:" + fakeCloudflareServer.getAddress().getPort() + "/siteverify";
    }

    private TurnstileVerifier createVerifier(boolean enabled) {
        return new TurnstileVerifier(fakeEndpoint, "site-key", "secret-key", enabled, Duration.ofSeconds(2));
    }

    @AfterEach
    void stopFakeCloudflare() {
        if (fakeCloudflareServer != null) {
            fakeCloudflareServer.stop(0);
        }
    }

    @Test
    void isTokenValid_returnsTrueWhenCloudflareReportsSuccess() throws IOException {
        startFakeCloudflare(200, "{\"success\":true,\"error-codes\":[]}");

        assertTrue(createVerifier(true).isTokenValid("token", "203.0.113.1"));
    }

    @Test
    void isTokenValid_returnsFalseWhenCloudflareRejectsToken() throws IOException {
        startFakeCloudflare(200, "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}");

        assertFalse(createVerifier(true).isTokenValid("token", "203.0.113.1"));
    }

    @Test
    void isTokenValid_throwsWhenCloudflareRejectsSecretKey() throws IOException {
        startFakeCloudflare(200, "{\"success\":false,\"error-codes\":[\"invalid-input-secret\"]}");

        TurnstileVerifier verifier = createVerifier(true);

        assertThrows(CaptchaUnavailableException.class, () -> verifier.isTokenValid("token", "203.0.113.1"));
    }

    @Test
    void isTokenValid_throwsWhenCloudflareReturnsServerError() throws IOException {
        startFakeCloudflare(500, "{}");

        TurnstileVerifier verifier = createVerifier(true);

        assertThrows(CaptchaUnavailableException.class, () -> verifier.isTokenValid("token", "203.0.113.1"));
    }

    @Test
    void isTokenValid_skipsCloudflareWhenDisabled() throws IOException {
        startFakeCloudflare(200, "{\"success\":false}");

        assertTrue(createVerifier(false).isTokenValid("token", "203.0.113.1"));
        assertEquals(0, receivedRequestCount.get());
    }
}
