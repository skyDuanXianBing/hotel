package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;
import server.demo.config.SuApiConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Su Widget scripts call Su Config endpoints directly in browser runtime
 * (connect(-sandbox).su-api.com/Config/...). Su does not always return CORS
 * headers for third-party origins, which can cause browser blocking and blank UI.
 *
 * This controller provides a controlled reverse proxy: frontend routes these
 * requests to backend, and backend forwards them to Su Config.
 *
 * Security notes:
 * - only fixed Su hosts are allowed (prod/sandbox), and only /Config/jservice/** paths.
 * - local Authorization (JWT) is never forwarded to Su.
 * - if Su Authorization is needed, frontend passes it via X-Su-Authorization.
 */
@RestController
@RequestMapping("/api/v1/su/config")
public class SuConfigProxyController {

    private static final Logger logger = LoggerFactory.getLogger(SuConfigProxyController.class);
    private static final String UPSTREAM_PROD = "https://connect.su-api.com";
    private static final String UPSTREAM_SANDBOX = "https://connect-sandbox.su-api.com";
    private static final String SU_AUTH_HEADER = "X-Su-Authorization";
    private static final String SU_PROXY_TOKEN_ID_HEADER = "X-Su-Token-Id";
    private static final String SU_PROXY_APP_ID_HEADER = "X-Su-App-Id";
    private static final String SU_PROXY_CLIENT_ID_HEADER = "X-Su-Client-Id";
    private static final String SU_PROXY_CHANNEL_ID_HEADER = "X-Su-Channel-Id";
    private static final String SU_UPSTREAM_TOKEN_ID_HEADER = "token-id";
    private static final String SU_UPSTREAM_APP_ID_HEADER = "app-id";
    private static final String SU_UPSTREAM_CLIENT_ID_HEADER = "client-id";
    private static final String SU_UPSTREAM_CLIENT_SECRET_HEADER = "client-secret";
    private static final String BOOKING_CHANNEL_ID = "19";
    private static final String AIRBNB_CHANNEL_ID = "244";

    private final RestTemplate restTemplate;
    private final SuApiConfig suApiConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private enum TargetType {
        REAL_PROD,
        REAL_SANDBOX,
        LOCAL_MOCK
    }

    private record ProxyTarget(TargetType type, String baseUrl) {
    }

    public SuConfigProxyController(RestTemplate restTemplate, SuApiConfig suApiConfig) {
        this.restTemplate = restTemplate;
        this.suApiConfig = suApiConfig;
    }

    @RequestMapping(
            value = "/{env}/**",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    public ResponseEntity<byte[]> proxy(
            @PathVariable("env") String env,
            @RequestBody(required = false) byte[] body,
            HttpServletRequest request
    ) {
        TargetType requestedTargetType = resolveRequestedTargetType(env);
        if (requestedTargetType == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"success\":false,\"message\":\"Unsupported Su env: " + env + "\",\"data\":null}").getBytes());
        }

        String remainingPath = extractRemainingPath(request);
        if (remainingPath == null || remainingPath.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"message\":\"Missing proxy path\",\"data\":null}".getBytes());
        }

        // Only allow /Config/jservice/** to avoid becoming a generic open proxy.
        if (!remainingPath.startsWith("jservice/") && !remainingPath.equals("jservice")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"message\":\"Only /Config/jservice/** is allowed\",\"data\":null}".getBytes());
        }

        ProxyTarget proxyTarget = resolveProxyTarget(requestedTargetType);
        String upstreamBase = proxyTarget.baseUrl();
        String query = request.getQueryString();
        String targetUrl = upstreamBase + "/Config/" + remainingPath + (query != null && !query.isBlank() ? "?" + query : "");

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"message\":\"Unsupported HTTP method\",\"data\":null}".getBytes());
        }

        HttpHeaders outgoingHeaders = buildOutgoingHeaders(request, proxyTarget.type());
        logProxyRequest(targetUrl, request, outgoingHeaders);
        ResponseEntity<byte[]> upstreamResponse;
        try {
            upstreamResponse = restTemplate.exchange(
                    targetUrl,
                    method,
                    new org.springframework.http.HttpEntity<>(body, outgoingHeaders),
                    byte[].class
            );
            logProxyResponse(targetUrl, upstreamResponse);
        } catch (Exception ex) {
            logger.warn("Su Config proxy request failed. targetUrl={}, error={}", targetUrl, safeMessage(ex));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"success\":false,\"message\":\"Su Config proxy failed: " + safeMessage(ex) + "\",\"data\":null}").getBytes());
        }

        byte[] responseBody = upstreamResponse.getBody();
        boolean decodedGzipBody = false;
        if (isGzipBody(responseBody)) {
            byte[] decompressed = tryDecompressGzip(responseBody);
            if (decompressed != null) {
                responseBody = decompressed;
                decodedGzipBody = true;
            }
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.CONTENT_TYPE);
        if (!decodedGzipBody) {
            copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.CONTENT_ENCODING);
        }
        copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.CACHE_CONTROL);
        copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.VARY);
        copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.ETAG);
        copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.LAST_MODIFIED);
        copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.EXPIRES);
        copyResponseHeaderIfPresent(upstreamResponse.getHeaders(), responseHeaders, HttpHeaders.PRAGMA);

        if (responseHeaders.getContentType() == null) {
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        }

        return new ResponseEntity<>(responseBody, responseHeaders, upstreamResponse.getStatusCode());
    }

    private void copyResponseHeaderIfPresent(HttpHeaders source, HttpHeaders target, String headerName) {
        List<String> values = source.get(headerName);
        if (values == null || values.isEmpty()) {
            return;
        }
        target.put(headerName, values);
    }

    private boolean isGzipBody(byte[] body) {
        return body != null
                && body.length >= 2
                && body[0] == (byte) 0x1f
                && body[1] == (byte) 0x8b;
    }

    private byte[] tryDecompressGzip(byte[] compressedBody) {
        try (
                ByteArrayInputStream input = new ByteArrayInputStream(compressedBody);
                GZIPInputStream gzipInputStream = new GZIPInputStream(input);
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            gzipInputStream.transferTo(output);
            return output.toByteArray();
        } catch (IOException ex) {
            logger.warn("Failed to decompress gzip response body from Su Config. error={}", safeMessage(ex));
            return null;
        }
    }

    private TargetType resolveRequestedTargetType(String env) {
        if (env == null) {
            return null;
        }
        String normalized = env.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "prod", "production" -> TargetType.REAL_PROD;
            case "sandbox" -> TargetType.REAL_SANDBOX;
            default -> null;
        };
    }

    private ProxyTarget resolveProxyTarget(TargetType requestedTargetType) {
        if (suApiConfig.isLocalMockEnabled()) {
            return new ProxyTarget(TargetType.LOCAL_MOCK, trimTrailingSlash(suApiConfig.getRequiredLocalMockBaseUrl()));
        }
        if (TargetType.REAL_SANDBOX.equals(requestedTargetType)) {
            return new ProxyTarget(TargetType.REAL_SANDBOX, UPSTREAM_SANDBOX);
        }
        return new ProxyTarget(TargetType.REAL_PROD, UPSTREAM_PROD);
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String extractRemainingPath(HttpServletRequest request) {
        String pathWithinMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pathWithinMapping == null || bestMatchPattern == null) {
            return null;
        }
        return pathMatcher.extractPathWithinPattern(bestMatchPattern, pathWithinMapping);
    }

    private HttpHeaders buildOutgoingHeaders(
            HttpServletRequest request,
            TargetType targetType
    ) {
        HttpHeaders headers = new HttpHeaders();

        String suAuthorization = normalizeHeaderValue(request.getHeader(SU_AUTH_HEADER));
        String suTokenId = firstNonBlank(
                normalizeHeaderValue(request.getHeader(SU_PROXY_TOKEN_ID_HEADER)),
                normalizeHeaderValue(request.getHeader(SU_UPSTREAM_TOKEN_ID_HEADER))
        );
        String suAppId = firstNonBlank(
                normalizeHeaderValue(request.getHeader(SU_PROXY_APP_ID_HEADER)),
                normalizeHeaderValue(request.getHeader(SU_UPSTREAM_APP_ID_HEADER))
        );
        String suClientId = firstNonBlank(
                normalizeHeaderValue(request.getHeader(SU_PROXY_CLIENT_ID_HEADER)),
                normalizeHeaderValue(request.getHeader(SU_UPSTREAM_CLIENT_ID_HEADER))
        );

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (name == null) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals("host")
                    || lower.equals("content-length")
                    || lower.equals("connection")
                    || lower.equals("accept-encoding")
                    || lower.equals("cookie")
                    || lower.equals("authorization") // Local JWT should not be forwarded upstream.
                    || lower.equals("x-store-id")
                    || lower.equals(SU_AUTH_HEADER.toLowerCase(Locale.ROOT))
                    || lower.equals(SU_PROXY_TOKEN_ID_HEADER.toLowerCase(Locale.ROOT))
                    || lower.equals(SU_PROXY_APP_ID_HEADER.toLowerCase(Locale.ROOT))
                    || lower.equals(SU_PROXY_CLIENT_ID_HEADER.toLowerCase(Locale.ROOT))
                    || lower.equals(SU_PROXY_CHANNEL_ID_HEADER.toLowerCase(Locale.ROOT))
                    || lower.equals(SU_UPSTREAM_TOKEN_ID_HEADER)
                    || lower.equals(SU_UPSTREAM_APP_ID_HEADER)
                    || lower.equals(SU_UPSTREAM_CLIENT_ID_HEADER)
                    || lower.equals(SU_UPSTREAM_CLIENT_SECRET_HEADER)) {
                continue;
            }

            Enumeration<String> values = request.getHeaders(name);
            while (values != null && values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }

        if (suAuthorization != null && !suAuthorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, suAuthorization.trim());
        }
        if (suTokenId != null && !suTokenId.isBlank()) {
            headers.set(SU_UPSTREAM_TOKEN_ID_HEADER, suTokenId);
        }

        String fallbackClientId = resolveFallbackClientId(targetType);
        if (headers.getFirst(SU_UPSTREAM_CLIENT_ID_HEADER) == null && fallbackClientId != null) {
            headers.set(SU_UPSTREAM_CLIENT_ID_HEADER, fallbackClientId);
        }
        if (shouldUseIncomingRealSuContextHeaders(targetType) && suClientId != null && !suClientId.isBlank()) {
            headers.set(SU_UPSTREAM_CLIENT_ID_HEADER, suClientId);
        }

        String finalClientId = normalizeHeaderValue(headers.getFirst(SU_UPSTREAM_CLIENT_ID_HEADER));
        if (headers.getFirst(SU_UPSTREAM_APP_ID_HEADER) == null && finalClientId != null) {
            headers.set(SU_UPSTREAM_APP_ID_HEADER, finalClientId);
        }
        if (shouldUseIncomingRealSuContextHeaders(targetType) && suAppId != null && !suAppId.isBlank()) {
            headers.set(SU_UPSTREAM_APP_ID_HEADER, suAppId);
        }

        if (headers.getFirst(SU_UPSTREAM_CLIENT_SECRET_HEADER) == null) {
            try {
                String fallbackClientSecret = resolveFallbackClientSecret(targetType);
                if (fallbackClientSecret != null) {
                    headers.set(SU_UPSTREAM_CLIENT_SECRET_HEADER, fallbackClientSecret);
                }
            } catch (Exception ex) {
                logger.warn("Su Config proxy missing client-secret fallback config. error={}", safeMessage(ex));
            }
        }

        String channelId = normalizeHeaderValue(request.getHeader(SU_PROXY_CHANNEL_ID_HEADER));
        if (TargetType.LOCAL_MOCK.equals(targetType) && isAllowedLocalMockChannelId(channelId)) {
            headers.set(SU_PROXY_CHANNEL_ID_HEADER, channelId);
        }

        if (headers.getAccept() == null || headers.getAccept().isEmpty()) {
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        }

        return headers;
    }

    private String resolveFallbackClientId(TargetType targetType) {
        if (TargetType.LOCAL_MOCK.equals(targetType)) {
            return normalizeHeaderValue(suApiConfig.getRequiredLocalMockClientId());
        }
        return normalizeHeaderValue(suApiConfig.getRealClientId());
    }

    private String resolveFallbackClientSecret(TargetType targetType) {
        if (TargetType.LOCAL_MOCK.equals(targetType)) {
            return normalizeHeaderValue(suApiConfig.getRequiredLocalMockClientSecret());
        }
        if (TargetType.REAL_SANDBOX.equals(targetType)) {
            return normalizeHeaderValue(suApiConfig.getRealSandboxClientSecret());
        }
        return normalizeHeaderValue(suApiConfig.getRealProductionClientSecret());
    }

    private boolean shouldUseIncomingRealSuContextHeaders(TargetType targetType) {
        return !TargetType.LOCAL_MOCK.equals(targetType);
    }

    private boolean isAllowedLocalMockChannelId(String channelId) {
        return BOOKING_CHANNEL_ID.equals(channelId) || AIRBNB_CHANNEL_ID.equals(channelId);
    }

    private void logProxyRequest(String targetUrl, HttpServletRequest request, HttpHeaders outgoingHeaders) {
        logger.info(
                "Su Config proxy request. targetUrl={}, inAuth={}, inTokenId={}, outAuth={}, outTokenId={}, outClientId={}, outClientSecret={}, outAppId={}",
                targetUrl,
                hasText(request.getHeader(SU_AUTH_HEADER)),
                hasText(firstNonBlank(
                        request.getHeader(SU_PROXY_TOKEN_ID_HEADER),
                        request.getHeader(SU_UPSTREAM_TOKEN_ID_HEADER)
                )),
                hasText(outgoingHeaders.getFirst(HttpHeaders.AUTHORIZATION)),
                hasText(outgoingHeaders.getFirst(SU_UPSTREAM_TOKEN_ID_HEADER)),
                hasText(outgoingHeaders.getFirst(SU_UPSTREAM_CLIENT_ID_HEADER)),
                hasText(outgoingHeaders.getFirst(SU_UPSTREAM_CLIENT_SECRET_HEADER)),
                hasText(outgoingHeaders.getFirst(SU_UPSTREAM_APP_ID_HEADER))
        );
    }

    private void logProxyResponse(String targetUrl, ResponseEntity<byte[]> upstreamResponse) {
        int bodyLength = upstreamResponse.getBody() == null ? 0 : upstreamResponse.getBody().length;
        logger.info(
                "Su Config proxy response. targetUrl={}, status={}, bodyBytes={}",
                targetUrl,
                upstreamResponse.getStatusCode().value(),
                bodyLength
        );
    }

    private String normalizeHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String first, String second) {
        String firstValue = normalizeHeaderValue(first);
        if (firstValue != null) {
            return firstValue;
        }
        return normalizeHeaderValue(second);
    }

    private boolean hasText(String value) {
        return normalizeHeaderValue(value) != null;
    }

    private String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            return "Unknown error";
        }
        msg = msg.replace("\"", "'");
        if (msg.length() > 500) {
            return msg.substring(0, 500);
        }
        return msg;
    }
}
