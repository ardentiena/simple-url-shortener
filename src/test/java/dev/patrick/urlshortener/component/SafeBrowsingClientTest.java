package dev.patrick.urlshortener.component;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SafeBrowsingClientTest {
    private HttpServer fakeGoogleServer;
    private String fakeEndpoint;
    private final AtomicInteger receivedRequestCount = new AtomicInteger();

    private void startFakeGoogleServer(int statusCode, String responseBody) throws IOException {
        fakeGoogleServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeGoogleServer.createContext("/", exchange -> {
            receivedRequestCount.incrementAndGet();
            byte[] responseBodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBodyBytes.length);
            try (OutputStream responseStream = exchange.getResponseBody()) {
                responseStream.write(responseBodyBytes);
            }
        });
        fakeGoogleServer.start();
        fakeEndpoint = "http://127.0.0.1:" + fakeGoogleServer.getAddress().getPort() + "/v4/threatMatches:find";
    }

    private SafeBrowsingClient createClient(String apiKey, boolean enabled, boolean failOpen){
        return new SafeBrowsingClient(fakeEndpoint, apiKey, enabled, failOpen, Duration.ofSeconds(2));
    }

    @AfterEach
    void stopFakeGoogleServer(){
        if(fakeGoogleServer != null){
            fakeGoogleServer.stop(0);
        }
    }

    @Test
    void isUrlSafe_returnsTrueWhenGoogleFindsNoMatches() throws IOException {
        startFakeGoogleServer(200, "{}");

        assertTrue(createClient("test-key", true, false).isUrlSafe("https://spring.io"));
    }

    @Test
    void isUrlSafe_returnsFalseWhenGoogleReportsMatches() throws IOException {
        startFakeGoogleServer(200, "{\"matches\":[{\"threatType\":\"MALWARE\"}]}");

        assertFalse(createClient("test-key", true, false).isUrlSafe("https://malware.example"));
    }

    @Test
    void isUrlSafe_throwsWhenGoogleFailsAndFailOpenIsFalse() throws IOException {
        startFakeGoogleServer(403, "{\"error\":{\"code\":403}}");

        SafeBrowsingClient client = createClient("test-key", true, false);

        assertThrows(SafeBrowsingUnavailableException.class, () -> client.isUrlSafe("https://spring.io"));
    }

    @Test
    void isUrlSafe_allowsUrlWhenGoogleFailsAndFailOpenIsTrue() throws IOException {
        startFakeGoogleServer(500, "{}");

        assertTrue(createClient("test-key", true, true).isUrlSafe("https://spring.io"));
    }

    @Test
    void isUrlSafe_skipsGoogleWhenDisabled() throws IOException {
        startFakeGoogleServer(200, "{}");

        assertTrue(createClient("test-key", false, false).isUrlSafe("https://spring.io"));
        assertEquals(0, receivedRequestCount.get());
    }

    @Test
    void isUrlSafe_throwsWithoutCallingGoogleWhenApiKeyIsBlank() throws IOException {
        startFakeGoogleServer(200, "{}");

        SafeBrowsingClient client = createClient("", true, false);

        assertThrows(SafeBrowsingUnavailableException.class, () -> client.isUrlSafe("https://spring.io"));
        assertEquals(0, receivedRequestCount.get());
    }
}
