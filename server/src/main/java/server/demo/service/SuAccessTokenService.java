package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SuApiConfig;
import server.demo.exception.SuApiUnauthorizedException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import server.demo.i18n.ApiMessages;
/**
 * Su Access Token 管理（缓存复用 + 401 自动刷新重试）。
 * <p>
 * 注意：Su access token 是 PMS 级别的全局凭证，与 storeId 无关，因此在应用内缓存一份即可。
 */
@Service
public class SuAccessTokenService {

    private static final Logger logger = LoggerFactory.getLogger(SuAccessTokenService.class);
    private static final Duration EXPIRY_SAFETY_WINDOW = Duration.ofSeconds(60);
    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofHours(1);

    private final SuApiConfig suApiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Object lock = new Object();
    private volatile CachedToken cachedToken;

    public SuAccessTokenService(SuApiConfig suApiConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.suApiConfig = suApiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String getAccessToken() {
        CachedToken token = cachedToken;
        if (token != null && token.isValid()) {
            return token.value();
        }

        synchronized (lock) {
            token = cachedToken;
            if (token != null && token.isValid()) {
                return token.value();
            }
            cachedToken = requestNewToken();
            return cachedToken.value();
        }
    }

    public String refreshAccessToken() {
        synchronized (lock) {
            cachedToken = null;
            cachedToken = requestNewToken();
            return cachedToken.value();
        }
    }

    public <T> T executeWithTokenRetry(Function<String, T> action, String actionName) {
        String token = getAccessToken();
        try {
            return action.apply(token);
        } catch (SuApiUnauthorizedException unauthorized) {
            logger.warn("Su {} unauthorized (401), refreshing access token and retrying once.", actionName);
            String refreshed = refreshAccessToken();
            return action.apply(refreshed);
        }
    }

    private CachedToken requestNewToken() {
        String baseUrl = requireNonBlankTrimmed(suApiConfig.getBaseUrl(), ApiMessages.get("api.t.bb7a42ad59ae"));
        String clientId = requireNonBlankTrimmed(suApiConfig.getClientId(), ApiMessages.get("api.t.c3a2a183d27c"));
        String clientSecret = requireNonBlankTrimmed(suApiConfig.getClientSecret(), ApiMessages.get("api.t.92a93afcec24"));

        String url = baseUrl + "/SUAPI/jservice/auth/generate-access-token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("client-id", clientId);
        headers.set("client-secret", clientSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            logger.info("Generating Su API access token (with cache)");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException(ApiMessages.get("api.t.b07263f4619c"));
            }

            JsonNode json = objectMapper.readTree(body);
            if (!isSuSuccess(json)) {
                throw new RuntimeException(buildSuErrorMessage(ApiMessages.get("api.t.bbb986c25b49"), json));
            }

            String token = readText(json, "/data/token");
            if (token == null) {
                token = readText(json, "/Data/token");
            }
            if (token == null || token.isBlank()) {
                throw new RuntimeException(ApiMessages.get("api.t.da5d4675a602"));
            }

            Duration ttl = parseExpireIn(json);
            Instant expiresAt = Instant.now().plus(ttl);
            logger.info("Su API access token generated successfully. ttlSeconds={}", ttl.getSeconds());
            return new CachedToken(token.trim(), expiresAt);
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                try {
                    JsonNode json = objectMapper.readTree(body);
                    throw new RuntimeException(buildSuErrorMessage(ApiMessages.get("api.t.bbb986c25b49"), json), e);
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception ignore) {
                    // fallthrough
                }
            }
            throw new RuntimeException(ApiMessages.get("api.t.81afd1b08b1f") + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new RuntimeException(ApiMessages.get("api.t.81afd1b08b1f") + e.getMessage(), e);
        }
    }

    private static String requireNonBlankTrimmed(String value, String helpMessage) {
        if (value == null) {
            throw new IllegalStateException(helpMessage);
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalStateException(helpMessage);
        }
        return trimmed;
    }

    private boolean isSuSuccess(JsonNode response) {
        if (response == null) {
            return false;
        }
        JsonNode successNode = response.get("success");
        if (successNode != null && successNode.isBoolean()) {
            return successNode.asBoolean(false);
        }
        JsonNode statusNode = response.get("Status");
        if (statusNode == null) {
            statusNode = response.get("status");
        }
        String status = statusNode != null ? statusNode.asText("") : "";
        return "Success".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    private static String readText(JsonNode root, String jsonPointer) {
        if (root == null) {
            return null;
        }
        JsonNode node = root.at(jsonPointer);
        if (node == null || node.isMissingNode()) {
            return null;
        }
        String value = node.asText(null);
        return value != null && !value.isBlank() ? value : null;
    }

    private Duration parseExpireIn(JsonNode response) {
        String expireIn = readText(response, "/data/expire_in");
        if (expireIn == null) {
            expireIn = readText(response, "/Data/expire_in");
        }
        if (expireIn == null) {
            expireIn = readText(response, "/data/expireIn");
        }
        if (expireIn == null) {
            expireIn = readText(response, "/Data/expireIn");
        }

        Duration ttl = DEFAULT_TOKEN_TTL;
        if (expireIn != null) {
            try {
                long seconds = Long.parseLong(expireIn.trim());
                if (seconds > 0) {
                    ttl = Duration.ofSeconds(seconds);
                }
            } catch (NumberFormatException ignore) {
                // keep default
            }
        }
        if (ttl.compareTo(EXPIRY_SAFETY_WINDOW) <= 0) {
            return ttl;
        }
        return ttl.minus(EXPIRY_SAFETY_WINDOW);
    }

    private String buildSuErrorMessage(String prefix, JsonNode response) {
        if (response == null) {
            return prefix;
        }
        String shortText = readErrorsShortText(response);
        String status = readText(response, "/Status");
        if (status == null) {
            status = readText(response, "/status");
        }
        String code = readErrorsCode(response);

        StringBuilder sb = new StringBuilder(prefix);
        if (code != null) {
            sb.append("（Code=").append(code).append("）");
        }
        if (shortText != null) {
            sb.append("：").append(shortText);
        } else if (status != null) {
            sb.append("：").append(status);
        }
        if ("548".equals(code) || (shortText != null && shortText.toLowerCase().contains("limit"))) {
            sb.append(ApiMessages.get("api.t.510bc7a55761"));
        }
        return sb.toString();
    }

    private static String readErrorsShortText(JsonNode response) {
        JsonNode errors = response.get("Errors");
        if (errors == null) {
            errors = response.get("errors");
        }
        if (errors == null) {
            return null;
        }
        if (errors.isArray() && errors.size() > 0) {
            JsonNode first = errors.get(0);
            if (first != null) {
                JsonNode shortText = first.get("ShortText");
                if (shortText == null) {
                    shortText = first.get("shortText");
                }
                String value = shortText != null ? shortText.asText(null) : null;
                return value != null && !value.isBlank() ? value : null;
            }
        }
        return null;
    }

    private static String readErrorsCode(JsonNode response) {
        JsonNode errors = response.get("Errors");
        if (errors == null) {
            errors = response.get("errors");
        }
        if (errors == null) {
            return null;
        }
        if (errors.isArray() && errors.size() > 0) {
            JsonNode first = errors.get(0);
            if (first != null) {
                JsonNode code = first.get("Code");
                if (code == null) {
                    code = first.get("code");
                }
                String value = code != null ? code.asText(null) : null;
                return value != null && !value.isBlank() ? value : null;
            }
        }
        return null;
    }

    private record CachedToken(String value, Instant expiresAt) {
        boolean isValid() {
            return value != null
                    && !value.isBlank()
                    && expiresAt != null
                    && Instant.now().isBefore(expiresAt);
        }
    }
}
