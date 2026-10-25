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

@Component
public class SafeBrowsingClient {
    private static final Logger log =  LoggerFactory.getLogger(SafeBrowsingClient.class);

    private final String endpoint;
    private final String apiKey;
    private final boolean enabled;
    private final boolean failOpen;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    public SafeBrowsingClient(@Value("${safe-browsing.endpoint:https://safebrowsing.googleapis.com/v4/threatMatches:find}") String endpoint,
                              @Value("${safe-browsing.api-key:}") String apiKey,
                              @Value("${safe-browsing.enabled:true}") boolean enabled,
                              @Value("${safe-browsing.fail-open:false}") boolean failOpen,
                              @Value("${safe-browsing.timeout:2s}") Duration requestTimeout){
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();

        if(enabled && apiKey.isBlank()){
            log.warn("Safe Browsing is enabled but safe-browsing.api-key is empty, link creation will be refused");
        }
    }

    public boolean isUrlSafe(String url){
        if(!enabled){
            return true;
        }
        if(apiKey.isBlank()){
            return handleUnavailable("API key is not configured");
        }

        try{
            String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            HttpRequest lookupRequest = HttpRequest.newBuilder(URI.create(endpoint + "?key=" + encodedApiKey))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(url)))
                    .build();
            HttpResponse<String> lookupResponse = httpClient.send(
                    lookupRequest, HttpResponse.BodyHandlers.ofString()
            );

            if(lookupResponse.statusCode() != 200){
                return handleUnavailable("Google returned HTTP " +  lookupResponse.statusCode());
            }
            return !lookupResponse.body().contains("\"matches\"");
        } catch (IOException requestFailure) {
            return handleUnavailable("request failed with: " + requestFailure.getMessage());
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            return handleUnavailable("Request was interrupted");
        }
    }
    
    private boolean handleUnavailable(String reason){
        if(failOpen){
            log.warn("Safe Browsing check skipped ({}), link allowed because safe-browsing.fail-open is true", reason);
            return true;
        }
        log.warn("Safe Browsing check failed ({}), link refused", reason);
        throw new SafeBrowsingUnavailableException(reason);
    }
    
    private String buildRequestBody(String url) {
        return """
                {
                  "client": {"clientId": "urlshortener", "clientVersion": "0.0.1"},
                  "threatInfo": {
                    "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING"],
                    "platformTypes": ["ANY_PLATFORM"],
                    "threatEntryTypes": ["URL"],
                    "threatEntries": [{"url": "%s"}]
                  }
                }
                """.formatted(escapeForJson(url));
    }

    private Object escapeForJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
