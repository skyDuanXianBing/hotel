package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSitePageSchemaOpenAiClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void buildRequestBody_shouldPinModelReasoningEffortAndJsonFormat() throws Exception {
        IndependentSitePageSchemaOpenAiClient client = newClient("test-key", "http://localhost:1/v1");

        JsonNode body = OBJECT_MAPPER.readTree(
                client.buildRequestBody("hello").toString()
        );

        assertEquals("gpt-5.6-terra", body.path("model").asText());
        assertEquals("xhigh", body.path("reasoning_effort").asText());
        assertEquals("json_object", body.path("response_format").path("type").asText());
        assertEquals(1, body.path("messages").size());
        assertEquals("user", body.path("messages").path(0).path("role").asText());
        assertEquals("hello", body.path("messages").path(0).path("content").asText());
        assertFalse(body.has("temperature"));
        assertFalse(body.has("max_tokens"));
    }

    @Test
    void complete_shouldPostChatCompletionAndReturnContent() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        startServer(exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response = """
                    {"choices":[{"message":{"role":"assistant","content":"{\\"schemaVersion\\":\\"independent_site_page_v1\\"}"}}]}
                    """;
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });

        IndependentSitePageSchemaOpenAiClient client = newClient("secret-key", baseUrl());
        String content = client.complete("generate page");

        assertEquals("{\"schemaVersion\":\"independent_site_page_v1\"}", content);
        assertEquals("Bearer secret-key", capturedAuth.get());
        JsonNode body = OBJECT_MAPPER.readTree(capturedBody.get());
        assertEquals("gpt-5.6-terra", body.path("model").asText());
        assertEquals("xhigh", body.path("reasoning_effort").asText());
        assertEquals("generate page", body.path("messages").path(0).path("content").asText());
    }

    @Test
    void complete_shouldMapUpstreamErrorToGenerationFailed() throws Exception {
        startServer(exchange -> {
            byte[] response = "{\"error\":{\"message\":\"bad request\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        IndependentSitePageSchemaOpenAiClient client = newClient("secret-key", baseUrl());

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> client.complete("generate page")
        );
        assertEquals("OPENAI_GENERATION_FAILED", exception.getCode());
    }

    @Test
    void complete_shouldFailClosedWhenApiKeyMissing() {
        IndependentSitePageSchemaOpenAiClient client = newClient("  ", "http://localhost:1/v1");

        assertFalse(client.isConfigured());
        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> client.complete("generate page")
        );
        assertEquals("OPENAI_CHANNEL_UNAVAILABLE", exception.getCode());
    }

    private IndependentSitePageSchemaOpenAiClient newClient(String apiKey, String baseUrl) {
        return new IndependentSitePageSchemaOpenAiClient(
                OBJECT_MAPPER,
                apiKey,
                baseUrl,
                null,
                null,
                Duration.ofSeconds(5),
                java.net.http.HttpClient.newBuilder().build()
        );
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", handler);
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }
}
