package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SuApiConfig;
import server.demo.exception.SuApiUnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuAccessTokenServiceTest {

    @Test
    void getAccessToken_shouldCacheToken_untilNearExpiry() {
        SuApiConfig config = new SuApiConfig();
        config.setBaseUrl("https://connect-sandbox.su-api.com");
        config.setClientId("client-id");
        config.setClientSecret("client-secret");

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        String url = "https://connect-sandbox.su-api.com/SUAPI/jservice/auth/generate-access-token";
        String body = "{\"success\":true,\"data\":{\"token\":\"token-1\",\"expire_in\":\"3600\"}}";

        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));

        SuAccessTokenService service = new SuAccessTokenService(config, restTemplate, objectMapper);

        String t1 = service.getAccessToken();
        String t2 = service.getAccessToken();

        assertEquals("token-1", t1);
        assertEquals("token-1", t2);
        verify(restTemplate, times(1)).exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void executeWithTokenRetry_shouldRefreshOnUnauthorizedOnce() {
        SuApiConfig config = new SuApiConfig();
        config.setBaseUrl("https://connect-sandbox.su-api.com");
        config.setClientId("client-id");
        config.setClientSecret("client-secret");

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        String url = "https://connect-sandbox.su-api.com/SUAPI/jservice/auth/generate-access-token";
        String body1 = "{\"success\":true,\"data\":{\"token\":\"token-1\",\"expire_in\":\"3600\"}}";
        String body2 = "{\"success\":true,\"data\":{\"token\":\"token-2\",\"expire_in\":\"3600\"}}";

        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body1))
                .thenReturn(ResponseEntity.ok(body2));

        SuAccessTokenService service = new SuAccessTokenService(config, restTemplate, objectMapper);

        AtomicInteger calls = new AtomicInteger(0);
        String result = service.executeWithTokenRetry(token -> {
            int i = calls.incrementAndGet();
            if (i == 1) {
                assertEquals("token-1", token);
                throw new SuApiUnauthorizedException("test", "unauthorized");
            }
            assertEquals("token-2", token);
            return "ok";
        }, "test");

        assertEquals("ok", result);
        verify(restTemplate, times(2)).exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getAccessToken_shouldSurfaceRateLimitMessage() {
        SuApiConfig config = new SuApiConfig();
        config.setBaseUrl("https://connect-sandbox.su-api.com");
        config.setClientId("client-id");
        config.setClientSecret("client-secret");

        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        String url = "https://connect-sandbox.su-api.com/SUAPI/jservice/auth/generate-access-token";
        String errorJson = "{\"Errors\":[{\"Code\":\"548\",\"ShortText\":\"Limit has been reached\"}],\"Status\":\"Fail\"}";

        HttpClientErrorException error = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable Content",
                null,
                errorJson.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(error);

        SuAccessTokenService service = new SuAccessTokenService(config, restTemplate, objectMapper);

        RuntimeException ex = assertThrows(RuntimeException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Code=548"));
        assertTrue(ex.getMessage().toLowerCase().contains("limit"));
    }
}

