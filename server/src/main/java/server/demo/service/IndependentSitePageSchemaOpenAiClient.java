package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 独立站页面 schema 生成专用 OpenAI 直连客户端。
 *
 * 项目全局 langchain4j（0.34.0）的 OpenAiChatModel 不支持 reasoning_effort，
 * 因此独立站生成单独走 /chat/completions 直连，以便使用 gpt-5.6-terra + 最强推理，
 * 并通过 response_format=json_object 强制结构化输出。
 */
@Component
public class IndependentSitePageSchemaOpenAiClient implements IndependentSitePageSchemaAiClient {

    private static final Logger logger = LoggerFactory.getLogger(IndependentSitePageSchemaOpenAiClient.class);
    private static final int ERROR_BODY_LOG_LIMIT = 500;

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String reasoningEffort;
    private final Duration timeout;
    private final HttpClient httpClient;

    @Autowired
    public IndependentSitePageSchemaOpenAiClient(
            ObjectMapper objectMapper,
            @Value("${independent-site.ai.api-key:}") String apiKey,
            @Value("${independent-site.ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${independent-site.ai.model:gpt-5.6-terra}") String model,
            @Value("${independent-site.ai.reasoning-effort:xhigh}") String reasoningEffort,
            @Value("${independent-site.ai.timeout-seconds:180}") long timeoutSeconds
    ) {
        this(
                objectMapper,
                apiKey,
                baseUrl,
                model,
                reasoningEffort,
                Duration.ofSeconds(Math.max(timeoutSeconds, 1L)),
                defaultHttpClient()
        );
    }

    IndependentSitePageSchemaOpenAiClient(
            ObjectMapper objectMapper,
            String apiKey,
            String baseUrl,
            String model,
            String reasoningEffort,
            Duration timeout,
            HttpClient httpClient
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = model == null || model.isBlank() ? "gpt-5.6-terra" : model.trim();
        this.reasoningEffort = reasoningEffort == null || reasoningEffort.isBlank()
                ? "xhigh"
                : reasoningEffort.trim();
        this.timeout = timeout;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public String complete(String userMessage) {
        if (!isConfigured()) {
            throw new IndependentSiteServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_CHANNEL_UNAVAILABLE",
                    "当前无法确认系统 OpenAI 通道可用，未生成可发布草稿"
            );
        }
        String body = buildRequestBody(userMessage).toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            logger.warn("Independent-site AI request failed: {}", e.getMessage());
            throw generationFailed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw generationFailed();
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.warn(
                    "Independent-site AI request returned {}: {}",
                    response.statusCode(),
                    truncate(response.body())
            );
            throw generationFailed();
        }
        return extractContent(response.body());
    }

    ObjectNode buildRequestBody(String userMessage) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("reasoning_effort", reasoningEffort);
        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");
        ArrayNode messages = root.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userMessage);
        return root;
    }

    private String extractContent(String responseBody) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            logger.warn("Independent-site AI response is not valid JSON: {}", truncate(responseBody));
            throw generationFailed();
        }
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            logger.warn("Independent-site AI response missing message content: {}", truncate(responseBody));
            throw generationFailed();
        }
        return content.asText();
    }

    private IndependentSiteServiceException generationFailed() {
        return new IndependentSiteServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "OPENAI_GENERATION_FAILED",
                "OpenAI 页面草稿生成失败"
        );
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? "https://api.openai.com/v1"
                : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 构建默认 HttpClient。Java 不识别 shell 的 https_proxy 等环境变量，这里显式桥接：
     * 优先 https_proxy/HTTPS_PROXY（HTTP 代理），其次 all_proxy/ALL_PROXY（SOCKS5），未设置则直连。
     * 仅作用于独立站 AI 调用，不影响 JVM 内其他出站请求。
     */
    static HttpClient defaultHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        Proxy proxy = proxyFromEnv();
        if (proxy != null) {
            builder.proxy(ProxySelector.of((InetSocketAddress) proxy.address()));
        }
        return builder.build();
    }

    static Proxy proxyFromEnv() {
        String httpProxy = firstNonBlank(System.getenv("https_proxy"), System.getenv("HTTPS_PROXY"));
        if (httpProxy != null) {
            return toProxy(httpProxy, Proxy.Type.HTTP);
        }
        String socksProxy = firstNonBlank(System.getenv("all_proxy"), System.getenv("ALL_PROXY"));
        if (socksProxy != null) {
            return toProxy(socksProxy, Proxy.Type.SOCKS);
        }
        return null;
    }

    private static Proxy toProxy(String value, Proxy.Type type) {
        try {
            String normalized = value.trim();
            if (!normalized.contains("://")) {
                normalized = (type == Proxy.Type.SOCKS ? "socks5://" : "http://") + normalized;
            }
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || port <= 0) {
                return null;
            }
            return new Proxy(type, new InetSocketAddress(host, port));
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= ERROR_BODY_LOG_LIMIT
                ? value
                : value.substring(0, ERROR_BODY_LOG_LIMIT) + "...";
    }
}
