package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Su Widget 前端脚本会在浏览器内直接请求 Su 的 Config 服务（connect(-sandbox).su-api.com/Config/...），
 * 但 Su 侧并不会给第三方域名返回 CORS 响应头，导致浏览器拦截并出现白屏。
 *
 * 本控制器提供一个受控的反向代理：前端把这些请求改打到本后端，由本后端转发到 Su Config。
 *
 * 安全注意：
 * - 仅允许代理到 Su 的固定域名（生产/沙盒）且仅允许 /Config/jservice/** 路径；
 * - 不会将本系统的 Authorization（JWT）转发给 Su；
 * - 若 Su 需要 Authorization，则前端需通过 X-Su-Authorization 传入（由前端重写拦截完成）。
 */
@RestController
@RequestMapping("/api/v1/su/config")
public class SuConfigProxyController {

    private static final String UPSTREAM_PROD = "https://connect.su-api.com";
    private static final String UPSTREAM_SANDBOX = "https://connect-sandbox.su-api.com";
    private static final String SU_AUTH_HEADER = "X-Su-Authorization";

    private final RestTemplate restTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public SuConfigProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
        String upstreamBase = resolveUpstreamBase(env);
        if (upstreamBase == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"success\":false,\"message\":\"不支持的Su环境：" + env + "\",\"data\":null}").getBytes());
        }

        String remainingPath = extractRemainingPath(request);
        if (remainingPath == null || remainingPath.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"message\":\"缺少代理路径\",\"data\":null}".getBytes());
        }

        // 仅允许代理 /Config/jservice/**，避免成为通用代理
        if (!remainingPath.startsWith("jservice/") && !remainingPath.equals("jservice")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"message\":\"仅允许代理 /Config/jservice/**\",\"data\":null}".getBytes());
        }

        String query = request.getQueryString();
        String targetUrl = upstreamBase + "/Config/" + remainingPath + (query != null && !query.isBlank() ? "?" + query : "");

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"message\":\"不支持的HTTP方法\",\"data\":null}".getBytes());
        }

        HttpHeaders outgoingHeaders = buildOutgoingHeaders(request);
        ResponseEntity<byte[]> upstreamResponse;
        try {
            upstreamResponse = restTemplate.exchange(
                    targetUrl,
                    method,
                    new org.springframework.http.HttpEntity<>(body, outgoingHeaders),
                    byte[].class
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"success\":false,\"message\":\"Su Config代理失败：" + safeMessage(ex) + "\",\"data\":null}").getBytes());
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        MediaType contentType = upstreamResponse.getHeaders().getContentType();
        if (contentType != null) {
            responseHeaders.setContentType(contentType);
        } else {
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        List<String> cacheControl = upstreamResponse.getHeaders().get(HttpHeaders.CACHE_CONTROL);
        if (cacheControl != null) {
            responseHeaders.put(HttpHeaders.CACHE_CONTROL, cacheControl);
        }

        return new ResponseEntity<>(upstreamResponse.getBody(), responseHeaders, upstreamResponse.getStatusCode());
    }

    /**
     * 处理浏览器预检请求（CORS preflight）。
     */
    @RequestMapping(
            value = "/{env}/**",
            method = {RequestMethod.OPTIONS}
    )
    public ResponseEntity<Void> preflight() {
        return ResponseEntity.ok().build();
    }

    private String resolveUpstreamBase(String env) {
        if (env == null) {
            return null;
        }
        String normalized = env.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "prod", "production" -> UPSTREAM_PROD;
            case "sandbox" -> UPSTREAM_SANDBOX;
            default -> null;
        };
    }

    private String extractRemainingPath(HttpServletRequest request) {
        String pathWithinMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pathWithinMapping == null || bestMatchPattern == null) {
            return null;
        }
        return pathMatcher.extractPathWithinPattern(bestMatchPattern, pathWithinMapping);
    }

    private HttpHeaders buildOutgoingHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();

        String suAuthorization = request.getHeader(SU_AUTH_HEADER);

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
                    || lower.equals("cookie")
                    || lower.equals("authorization") // 本系统JWT，不应转发
                    || lower.equals("x-store-id")
                    || lower.equals(SU_AUTH_HEADER.toLowerCase(Locale.ROOT))) {
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

        if (headers.getAccept() == null || headers.getAccept().isEmpty()) {
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        }

        return headers;
    }

    private String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            return "未知错误";
        }
        msg = msg.replace("\"", "'");
        if (msg.length() > 500) {
            return msg.substring(0, 500);
        }
        return msg;
    }
}
