package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SmartLockConfig;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.util.SmartLockCredentialCrypto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmartLockSwitchBotClientTest {
    private static final String BASE_URL = "http://switchbot.test";

    private ObjectMapper objectMapper;
    private MockRestServiceServer server;
    private SmartLockSwitchBotClient client;
    private SmartLockCredentialData credentials;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        SmartLockConfig config = mock(SmartLockConfig.class);
        when(config.getSwitchBotBaseUrl()).thenReturn(BASE_URL);
        client = new SmartLockSwitchBotClient(
                config,
                new SmartLockCredentialCrypto("switchbot-contract-key"),
                objectMapper,
                restTemplate
        );

        credentials = new SmartLockCredentialData();
        credentials.setSwitchBotToken("token-1");
        credentials.setSwitchBotSecret("secret-1");
    }

    @Test
    void createPasscode_shouldUseSecondsTimestampHeadersAndReturnPending() {
        LocalDateTime validFrom = LocalDateTime.of(2026, 6, 21, 18, 0);
        LocalDateTime validUntil = LocalDateTime.of(2026, 6, 22, 10, 0);

        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/keypad-1/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requiredHeaders())
                .andExpect(jsonPayload(root -> {
                    assertEquals("createKey", root.path("command").asText());
                    JsonNode parameter = root.path("parameter");
                    assertEquals("Guest", parameter.path("name").asText());
                    assertEquals("timeLimit", parameter.path("type").asText());
                    assertEquals("123456", parameter.path("password").asText());
                    long expectedStart = validFrom.atZone(ZoneId.systemDefault()).toEpochSecond();
                    long expectedEnd = validUntil.atZone(ZoneId.systemDefault()).toEpochSecond();
                    assertEquals(expectedStart, parameter.path("startTime").asLong());
                    assertEquals(expectedEnd, parameter.path("endTime").asLong());
                    assertTrue(parameter.path("startTime").asLong() < 10_000_000_000L);
                    assertTrue(parameter.path("endTime").asLong() < 10_000_000_000L);
                }))
                .andRespond(withSuccess(
                        "{\"statusCode\":100,\"body\":{\"commandId\":\"cmd-1\",\"keyId\":\"key-1\"},\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        SmartLockProviderClient.ProviderTaskResult result = client.createPasscode(
                credentials,
                "keypad-1",
                new SmartLockProviderClient.PasscodeCommand("Guest", "123456", validFrom, validUntil)
        );

        assertEquals(SmartLockTaskStatus.PENDING, result.status());
        assertEquals("cmd-1", result.providerTaskId());
        assertEquals("key-1", result.providerPasscodeId());
        server.verify();
    }

    @Test
    void deletePasscode_shouldUseOfficialIdParameterAndReturnPending() {
        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/keypad-1/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requiredHeaders())
                .andExpect(jsonPayload(root -> {
                    assertEquals("deleteKey", root.path("command").asText());
                    JsonNode parameter = root.path("parameter");
                    assertEquals("key-9", parameter.path("id").asText());
                    assertFalse(parameter.has("keyId"));
                }))
                .andRespond(withSuccess(
                        "{\"statusCode\":100,\"body\":{\"commandId\":\"cmd-2\"},\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        SmartLockProviderClient.ProviderTaskResult result = client.deletePasscode(credentials, "keypad-1", "key-9");

        assertEquals(SmartLockTaskStatus.PENDING, result.status());
        assertEquals("cmd-2", result.providerTaskId());
        assertEquals("key-9", result.providerPasscodeId());
        server.verify();
    }

    @Test
    void listDevices_shouldIncludeSmartLockTypesKeypadsAndLockVisionWithLockDeviceId() {
        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess("""
                        {
                          "statusCode": 100,
                          "body": {
                            "deviceList": [
                              {"deviceId":"lock-1","deviceName":"Door Lock","deviceType":"Lock"},
                              {"deviceId":"smart-lock-1","deviceName":"Smart Door Lock","deviceType":"Smart Lock"},
                              {"deviceId":"smart-lock-wifi-1","deviceName":"Smart Door Lock Wifi","deviceType":"Smart Lock Pro Wifi"},
                              {"deviceId":"keypad-1","deviceName":"Keypad","deviceType":"Keypad Touch","lockDeviceId":"lock-1"},
                              {"deviceId":"vision-1","deviceName":"Vision","deviceType":"Keypad Vision","lockDeviceId":"lock-2"},
                              {"deviceId":"keypad-2","deviceName":"Keypad Hub Only","deviceType":"Keypad","hubDeviceId":"hub-1"},
                              {"deviceId":"bot-1","deviceName":"Bot","deviceType":"Bot"}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<SmartLockProviderClient.DeviceSnapshot> devices = client.listDevices(credentials);

        assertEquals(6, devices.size());
        assertEquals("lock-1", devices.get(0).providerLockId());
        assertEquals("smart-lock-1", devices.get(1).providerLockId());
        assertEquals("smart-lock-wifi-1", devices.get(2).providerLockId());
        assertEquals("keypad-1", devices.get(3).providerLockId());
        assertEquals("lock-1", devices.get(3).auxiliaryDeviceId());
        assertEquals("vision-1", devices.get(4).providerLockId());
        assertEquals("lock-2", devices.get(4).auxiliaryDeviceId());
        assertEquals("keypad-2", devices.get(5).providerLockId());
        assertEquals(null, devices.get(5).auxiliaryDeviceId());
        server.verify();
    }

    @Test
    void getStatus_shouldMapOnlineStatus() {
        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/lock-1/status"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess("""
                        {
                          "statusCode": 100,
                          "body": {
                            "lockState": "locked",
                            "battery": 81,
                            "onlineStatus": "offline"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        SmartLockProviderClient.LockStatusSnapshot status = client.getStatus(credentials, "lock-1");

        assertEquals("locked", status.lockStatus());
        assertEquals(81, status.battery());
        assertEquals(Boolean.FALSE, status.online());
        server.verify();
    }

    @Test
    void springContext_shouldCreateBeanWithAutowiredConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(
                    SwitchBotSpringTestConfig.class,
                    SmartLockCredentialCrypto.class,
                    SmartLockSwitchBotClient.class
            );
            context.refresh();

            assertNotNull(context.getBean(SmartLockSwitchBotClient.class));
        }
    }

    private RequestMatcher requiredHeaders() {
        return request -> {
            HttpHeaders headers = request.getHeaders();
            assertEquals("token-1", headers.getFirst("Authorization"));
            assertNotNull(headers.getFirst("sign"));
            assertNotNull(headers.getFirst("t"));
            assertNotNull(headers.getFirst("nonce"));
            String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
            if (request.getMethod() == HttpMethod.POST) {
                assertEquals("application/json; charset=utf8", contentType);
            }
        };
    }

    private RequestMatcher jsonPayload(JsonAssertion assertion) {
        return request -> {
            try {
                String body = ((MockHttpOutputMessage) request).getBodyAsString();
                assertion.accept(objectMapper.readTree(body));
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        };
    }

    @FunctionalInterface
    private interface JsonAssertion {
        void accept(JsonNode root) throws Exception;
    }

    @Configuration(proxyBeanMethods = false)
    private static class SwitchBotSpringTestConfig {
        @Bean
        SmartLockConfig smartLockConfig() {
            SmartLockConfig config = mock(SmartLockConfig.class);
            when(config.getSwitchBotBaseUrl()).thenReturn(BASE_URL);
            when(config.getEncryptionKey()).thenReturn("switchbot-contract-key");
            return config;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
