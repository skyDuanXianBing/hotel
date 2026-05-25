package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SuApiConfig;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuConfigProxyControllerTest {

    @Test
    void shouldRouteChannelmapRequestsToLocalMockWhenLocalMockEnabled() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SuApiConfig suApiConfig = buildLocalMockConfig();
        SuConfigProxyController controller = new SuConfigProxyController(restTemplate, suApiConfig);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        byte[] upstreamBody = "{\"success\":true,\"data\":{}}".getBytes(StandardCharsets.UTF_8);

        when(restTemplate.exchange(
                eq("http://localhost:4000/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstreamBody));

        mockMvc.perform(post("/api/v1/su/config/prod/jservice/channelmap/getMappingMasterData")
                        .header("X-Su-Channel-Id", "19")
                        .header("X-Su-Client-Id", "frontend-real-client-id")
                        .header("client-id", "frontend-real-raw-client-id")
                        .header("app-id", "frontend-real-raw-app-id")
                        .header("client-secret", "frontend-real-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proppmsid\":\"LOCALE2EHOTEL\",\"channel_code\":\"19\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":{}}"));

        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://localhost:4000/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(byte[].class)
        );
        assertEquals("mock-client-id", entityCaptor.getValue().getHeaders().getFirst("client-id"));
        assertEquals("mock-client-id", entityCaptor.getValue().getHeaders().getFirst("app-id"));
        assertEquals("mock-client-secret", entityCaptor.getValue().getHeaders().getFirst("client-secret"));
        assertEquals("19", entityCaptor.getValue().getHeaders().getFirst("X-Su-Channel-Id"));
    }

    @Test
    void shouldOnlyForwardAllowedLocalMockChannelIdsToLocalMock() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SuApiConfig suApiConfig = buildLocalMockConfig();
        SuConfigProxyController controller = new SuConfigProxyController(restTemplate, suApiConfig);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        byte[] upstreamBody = "{\"success\":true,\"data\":{}}".getBytes(StandardCharsets.UTF_8);

        when(restTemplate.exchange(
                eq("http://localhost:4000/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstreamBody));

        mockMvc.perform(post("/api/v1/su/config/prod/jservice/channelmap/getMappingMasterData")
                        .header("X-Su-Channel-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proppmsid\":\"LOCALE2EHOTEL\",\"channel_code\":\"999\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://localhost:4000/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(byte[].class)
        );
        assertNull(entityCaptor.getValue().getHeaders().getFirst("X-Su-Channel-Id"));
    }

    @Test
    void shouldNotForwardMockOnlyChannelIdToRealUpstream() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SuApiConfig suApiConfig = buildProductionConfig();
        SuConfigProxyController controller = new SuConfigProxyController(restTemplate, suApiConfig);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        byte[] upstreamBody = "{\"success\":true,\"data\":{}}".getBytes(StandardCharsets.UTF_8);

        when(restTemplate.exchange(
                eq("https://connect.su-api.com/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstreamBody));

        mockMvc.perform(post("/api/v1/su/config/prod/jservice/channelmap/getMappingMasterData")
                        .header("X-Su-Channel-Id", "244")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proppmsid\":\"PRODHOTEL\",\"channel_code\":\"244\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://connect.su-api.com/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(byte[].class)
        );
        assertEquals("production-client-id", entityCaptor.getValue().getHeaders().getFirst("client-id"));
        assertEquals("production-secret", entityCaptor.getValue().getHeaders().getFirst("client-secret"));
        assertNull(entityCaptor.getValue().getHeaders().getFirst("X-Su-Channel-Id"));
    }

    @Test
    void shouldRouteLanguageFileRequestsToLocalMockWhenLocalMockEnabled() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SuApiConfig suApiConfig = buildLocalMockConfig();
        SuConfigProxyController controller = new SuConfigProxyController(restTemplate, suApiConfig);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        byte[] upstreamBody = "{\"success\":true,\"data\":{}}".getBytes(StandardCharsets.UTF_8);

        when(restTemplate.exchange(
                eq("http://localhost:4000/Config/jservice/getLanguageFile/zn"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstreamBody));

        mockMvc.perform(post("/api/v1/su/config/prod/jservice/getLanguageFile/zn")
                        .header("X-Su-Channel-Id", "244")
                        .header("X-Su-Client-Id", "frontend-real-client-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":{}}"));

        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://localhost:4000/Config/jservice/getLanguageFile/zn"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(byte[].class)
        );
        assertEquals("mock-client-id", entityCaptor.getValue().getHeaders().getFirst("client-id"));
        assertEquals("mock-client-secret", entityCaptor.getValue().getHeaders().getFirst("client-secret"));
        assertEquals("244", entityCaptor.getValue().getHeaders().getFirst("X-Su-Channel-Id"));
    }

    @Test
    void shouldKeepSandboxUpstreamAndSandboxCredentialsWhenLocalMockDisabled() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SuApiConfig suApiConfig = buildDualSecretProductionConfig();
        SuConfigProxyController controller = new SuConfigProxyController(restTemplate, suApiConfig);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        byte[] upstreamBody = "{\"success\":true,\"data\":{}}".getBytes(StandardCharsets.UTF_8);

        when(restTemplate.exchange(
                eq("https://connect-sandbox.su-api.com/Config/jservice/getLanguageFile/zn"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstreamBody));

        mockMvc.perform(post("/api/v1/su/config/sandbox/jservice/getLanguageFile/zn")
                        .header("X-Su-Channel-Id", "244")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":{}}"));

        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://connect-sandbox.su-api.com/Config/jservice/getLanguageFile/zn"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(byte[].class)
        );
        assertEquals("production-client-id", entityCaptor.getValue().getHeaders().getFirst("client-id"));
        assertEquals("sandbox-secret", entityCaptor.getValue().getHeaders().getFirst("client-secret"));
        assertNull(entityCaptor.getValue().getHeaders().getFirst("X-Su-Channel-Id"));
    }

    @Test
    void shouldConvertProxyContextHeadersAndNotForwardLocalJwt() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SuApiConfig suApiConfig = buildProductionConfig();
        SuConfigProxyController controller = new SuConfigProxyController(restTemplate, suApiConfig);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        byte[] upstreamBody = "{\"success\":true,\"data\":{}}".getBytes(StandardCharsets.UTF_8);

        when(restTemplate.exchange(
                eq("https://connect.su-api.com/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(upstreamBody));

        mockMvc.perform(post("/api/v1/su/config/prod/jservice/channelmap/getMappingMasterData")
                        .header("Authorization", "Bearer local-jwt")
                        .header("X-Store-Id", "12")
                        .header("X-Su-Authorization", "Bearer su-access-token")
                        .header("X-Su-Token-Id", "widget-token-id")
                        .header("X-Su-App-Id", "widget-app-id")
                        .header("X-Su-Client-Id", "widget-client-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proppmsid\":\"PRODHOTEL\",\"channel_code\":\"19\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<HttpEntity<byte[]>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://connect.su-api.com/Config/jservice/channelmap/getMappingMasterData"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(byte[].class)
        );
        assertEquals("Bearer su-access-token", entityCaptor.getValue().getHeaders().getFirst("Authorization"));
        assertEquals("widget-token-id", entityCaptor.getValue().getHeaders().getFirst("token-id"));
        assertEquals("widget-app-id", entityCaptor.getValue().getHeaders().getFirst("app-id"));
        assertEquals("widget-client-id", entityCaptor.getValue().getHeaders().getFirst("client-id"));
        assertNull(entityCaptor.getValue().getHeaders().getFirst("X-Store-Id"));
        assertNull(entityCaptor.getValue().getHeaders().getFirst("X-Su-Token-Id"));
        assertNull(entityCaptor.getValue().getHeaders().getFirst("X-Su-App-Id"));
        assertNull(entityCaptor.getValue().getHeaders().getFirst("X-Su-Client-Id"));
    }

    private SuApiConfig buildLocalMockConfig() {
        SuApiConfig config = new SuApiConfig();
        config.setChannelE2ELocalEnabled(true);
        config.setLocalMockBaseUrl(" http://localhost:4000/ ");
        config.setLocalMockClientId("mock-client-id");
        config.setLocalMockClientSecret("mock-client-secret");
        config.setClientId("production-client-id");
        config.setClientSecret("production-secret");
        return config;
    }

    private SuApiConfig buildDualSecretProductionConfig() {
        SuApiConfig config = new SuApiConfig();
        config.setChannelE2ELocalEnabled(false);
        config.setLocalMockBaseUrl("http://localhost:4000");
        config.setLocalMockClientId("mock-client-id");
        config.setLocalMockClientSecret("mock-client-secret");
        config.setEnv("production");
        config.setClientId("production-client-id");
        config.setClientSecret(" ");
        config.setClientSecretProduction("production-secret");
        config.setClientSecretSandbox("sandbox-secret");
        return config;
    }

    private SuApiConfig buildProductionConfig() {
        SuApiConfig config = new SuApiConfig();
        config.setChannelE2ELocalEnabled(false);
        config.setClientId("production-client-id");
        config.setClientSecret("production-secret");
        return config;
    }
}
