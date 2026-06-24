package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.Store;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.repository.StoreRepository;
import server.demo.util.SmartLockCredentialCrypto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmartLockSwitchBotClientTest {
    private static final String BASE_URL = "http://switchbot.test";
    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 26L;
    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Tokyo");

    private ObjectMapper objectMapper;
    private MockRestServiceServer server;
    private SmartLockSwitchBotClient client;
    private SmartLockCredentialData credentials;
    private StoreRepository storeRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        SmartLockConfig config = mock(SmartLockConfig.class);
        when(config.getSwitchBotBaseUrl()).thenReturn(BASE_URL);
        storeRepository = mock(StoreRepository.class);
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store(STORE_ZONE.getId())));
        client = new SmartLockSwitchBotClient(
                config,
                new SmartLockCredentialCrypto("switchbot-contract-key"),
                objectMapper,
                storeRepository,
                restTemplate
        );

        credentials = new SmartLockCredentialData();
        credentials.setSwitchBotToken("token-1");
        credentials.setSwitchBotSecret("secret-1");
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "owner"));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createPasscode_shouldUseStoreTimezoneSecondsTimestampHeadersAndReturnPending() {
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
                    long expectedStart = validFrom.atZone(STORE_ZONE).toEpochSecond();
                    long expectedEnd = validUntil.atZone(STORE_ZONE).toEpochSecond();
                    assertEquals(expectedStart, parameter.path("startTime").asLong());
                    assertEquals(expectedEnd, parameter.path("endTime").asLong());
                    assertTrue(parameter.path("startTime").asLong() < 10_000_000_000L);
                    assertTrue(parameter.path("endTime").asLong() < 10_000_000_000L);
                }))
                .andRespond(withSuccess(
                        "{\"statusCode\":100,\"body\":{\"commandId\":\"cmd-1\"},\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        SmartLockProviderClient.ProviderTaskResult result = client.createPasscode(
                credentials,
                "keypad-1",
                new SmartLockProviderClient.PasscodeCommand("Guest", "123456", validFrom, validUntil)
        );

        assertEquals(SmartLockTaskStatus.PENDING, result.status());
        assertEquals("cmd-1", result.providerTaskId());
        assertEquals(null, result.providerPasscodeId());
        server.verify();
    }

    @Test
    void createPasscode_shouldReturnPendingWhenSuccessResponseHasNoTraceableId() {
        LocalDateTime validFrom = LocalDateTime.of(2026, 6, 21, 18, 0);
        LocalDateTime validUntil = LocalDateTime.of(2026, 6, 22, 10, 0);

        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/keypad-1/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess(
                        "{\"statusCode\":100,\"body\":{},\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        SmartLockProviderClient.ProviderTaskResult result = client.createPasscode(
                credentials,
                "keypad-1",
                new SmartLockProviderClient.PasscodeCommand("Guest", "123456", validFrom, validUntil)
        );

        assertEquals(SmartLockTaskStatus.PENDING, result.status());
        assertEquals(null, result.providerTaskId());
        assertEquals(null, result.providerPasscodeId());
        assertTrue(result.message().contains("keyList"));
        server.verify();
    }

    @Test
    void createPasscode_shouldParseCommandIdVariantsOutsideBody() {
        LocalDateTime validFrom = LocalDateTime.of(2026, 6, 21, 18, 0);
        LocalDateTime validUntil = LocalDateTime.of(2026, 6, 22, 10, 0);

        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/keypad-1/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess(
                        "{\"statusCode\":100,\"data\":{\"command_id\":\"cmd-snake\"},\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        SmartLockProviderClient.ProviderTaskResult result = client.createPasscode(
                credentials,
                "keypad-1",
                new SmartLockProviderClient.PasscodeCommand("Guest", "123456", validFrom, validUntil)
        );

        assertEquals(SmartLockTaskStatus.PENDING, result.status());
        assertEquals("cmd-snake", result.providerTaskId());
        assertEquals(null, result.providerPasscodeId());
        server.verify();
    }

    @Test
    void createPasscode_shouldThrowWhenStatusCodeIsProviderError() {
        LocalDateTime validFrom = LocalDateTime.of(2026, 6, 21, 18, 0);
        LocalDateTime validUntil = LocalDateTime.of(2026, 6, 22, 10, 0);

        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/keypad-1/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess(
                        "{\"statusCode\":190,\"body\":{},\"message\":\"device offline\"}",
                        MediaType.APPLICATION_JSON
                ));

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> client.createPasscode(
                        credentials,
                        "keypad-1",
                        new SmartLockProviderClient.PasscodeCommand("Guest", "123456", validFrom, validUntil)
                )
        );

        assertTrue(error.getMessage().contains("device offline"));
        server.verify();
    }

    @Test
    void createPasscode_shouldRedactCipherFieldsInProviderErrorMessage() {
        LocalDateTime validFrom = LocalDateTime.of(2026, 6, 21, 18, 0);
        LocalDateTime validUntil = LocalDateTime.of(2026, 6, 22, 10, 0);

        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/keypad-1/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess(
                        "{\"statusCode\":190,\"body\":{},\"message\":\"failed ciphertext=secret-cipher cipherText='secret-camel' credential_ciphertext=secret-snake\"}",
                        MediaType.APPLICATION_JSON
                ));

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> client.createPasscode(
                        credentials,
                        "keypad-1",
                        new SmartLockProviderClient.PasscodeCommand("Guest", "123456", validFrom, validUntil)
                )
        );

        assertTrue(error.getMessage().contains("failed"));
        assertTrue(error.getMessage().contains("[REDACTED]"));
        assertFalse(error.getMessage().contains("secret-cipher"));
        assertFalse(error.getMessage().contains("secret-camel"));
        assertFalse(error.getMessage().contains("secret-snake"));
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
    void deletePasscode_shouldReturnPendingWhenSuccessResponseHasNoCommandId() {
        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices/keypad-1/commands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(requiredHeaders())
                .andExpect(jsonPayload(root -> {
                    assertEquals("deleteKey", root.path("command").asText());
                    assertEquals("key-9", root.path("parameter").path("id").asText());
                }))
                .andRespond(withSuccess(
                        "{\"statusCode\":100,\"body\":{},\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        SmartLockProviderClient.ProviderTaskResult result = client.deletePasscode(credentials, "keypad-1", "key-9");

        assertEquals(SmartLockTaskStatus.PENDING, result.status());
        assertEquals(null, result.providerTaskId());
        assertEquals("key-9", result.providerPasscodeId());
        assertTrue(result.message().contains("keyList"));
        server.verify();
    }

    @Test
    void listPasscodes_shouldParseKeyListForMatchedKeypadOnly() {
        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess("""
                        {
                          "statusCode": 100,
                          "body": {
                            "deviceList": [
                              {
                                "deviceId": "keypad-1",
                                "deviceType": "Keypad Touch",
                                "keyList": [
                                  {"id":"key-1","name":"Guest","status":"normal","password":"encrypted"},
                                  {"id":"key-2","name":"Cleaner","status":"normal"}
                                ]
                              },
                              {
                                "deviceId": "keypad-2",
                                "deviceType": "Keypad Touch",
                                "keyList": [
                                  {"id":"key-other","name":"Guest","status":"normal"}
                                ]
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<SmartLockProviderClient.ProviderPasscodeSnapshot> passcodes =
                client.listPasscodes(credentials, "keypad-1");

        assertEquals(2, passcodes.size());
        assertEquals("key-1", passcodes.get(0).providerPasscodeId());
        assertEquals("Guest", passcodes.get(0).passcodeName());
        assertEquals("normal", passcodes.get(0).status());
        assertEquals("key-2", passcodes.get(1).providerPasscodeId());
        assertEquals("Cleaner", passcodes.get(1).passcodeName());
        server.verify();
    }

    @Test
    void inspectPasscodes_shouldKeepNamedKeyWithoutIdForDiagnostics() {
        server.expect(once(), requestTo(BASE_URL + "/v1.1/devices"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(requiredHeaders())
                .andRespond(withSuccess("""
                        {
                          "statusCode": 100,
                          "body": {
                            "deviceList": [
                              {
                                "deviceId": "keypad-1",
                                "deviceType": "Keypad Touch",
                                "onlineStatus": "online",
                                "lockDeviceId": "lock-1",
                                "hubDeviceId": "hub-1",
                                "keyList": [
                                  {"name":"Guest","status":"normal"}
                                ]
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        SmartLockProviderClient.ProviderPasscodeListSnapshot inspection =
                client.inspectPasscodes(credentials, "keypad-1");

        assertTrue(inspection.deviceFound());
        assertTrue(inspection.keyListReadable());
        assertEquals("Keypad Touch", inspection.deviceType());
        assertEquals(Boolean.TRUE, inspection.online());
        assertEquals("lock-1", inspection.linkedLockDeviceId());
        assertEquals("hub-1", inspection.hubDeviceId());
        assertEquals(1, inspection.passcodes().size());
        assertEquals(null, inspection.passcodes().get(0).providerPasscodeId());
        assertEquals("Guest", inspection.passcodes().get(0).passcodeName());
        server.verify();
    }

    @Test
    void diagnosticHelpers_shouldNotExposePasswordIvOrPlainName() throws Exception {
        JsonNode keyNode = objectMapper.readTree("""
                {
                  "id": "key-1",
                  "name": "Guest Secret",
                  "type": "timeLimit",
                  "status": "normal ciphertext=secret-cipher",
                  "createTime": 1782046800,
                  "password": "123456",
                  "iv": "plain-iv",
                  "cipher": "secret-cipher-field",
                  "ciphertext": "secret-ciphertext-field"
                }
                """);
        String keyListSummary = SmartLockSwitchBotClient.keyListEntryDiagnostic(
                keyNode,
                "key-1",
                "Guest Secret"
        );

        assertTrue(keyListSummary.contains("idPresent=true"));
        assertTrue(keyListSummary.contains("nameHash="));
        assertTrue(keyListSummary.contains("nameLength=12"));
        assertTrue(keyListSummary.contains("type=timeLimit"));
        assertTrue(keyListSummary.contains("ciphertext=[REDACTED]"));
        assertTrue(keyListSummary.contains("createTimePresent=true"));
        assertFalse(keyListSummary.contains("Guest Secret"));
        assertFalse(keyListSummary.contains("123456"));
        assertFalse(keyListSummary.contains("plain-iv"));
        assertFalse(keyListSummary.contains("secret-cipher"));
        assertFalse(keyListSummary.contains("secret-cipher-field"));
        assertFalse(keyListSummary.contains("secret-ciphertext-field"));
        assertFalse(keyListSummary.contains("password"));
        assertFalse(keyListSummary.contains("iv"));

        SmartLockProviderClient.PasscodeCommand command =
                new SmartLockProviderClient.PasscodeCommand(
                        "Guest Secret",
                        "123456",
                        LocalDateTime.of(2026, 6, 21, 18, 0),
                        LocalDateTime.of(2026, 6, 22, 10, 0)
                );
        String requestSummary = SmartLockSwitchBotClient.createKeyRequestDiagnostic(
                command,
                1782046800L,
                1782104400L,
                STORE_ZONE,
                LocalDateTime.of(2026, 6, 21, 17, 0)
        );

        assertTrue(requestSummary.contains("command=createKey"));
        assertTrue(requestSummary.contains("nameHash="));
        assertTrue(requestSummary.contains("nameLength=12"));
        assertTrue(requestSummary.contains("passwordLength=6"));
        assertTrue(requestSummary.contains("startRelation=future"));
        assertTrue(requestSummary.contains("endAfterStart=true"));
        assertFalse(requestSummary.contains("Guest Secret"));
        assertFalse(requestSummary.contains("123456"));
    }

    @Test
    void responseBodyKeySummary_shouldListSafeKeysWithoutSensitiveValues() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "statusCode": 100,
                  "message": "success",
                  "body": {
                    "commandId": "cmd-1",
                    "password": "123456",
                    "iv": "plain-iv",
                    "cipher": "secret-cipher",
                    "ciphertext": "secret-ciphertext",
                    "credential_ciphertext": "secret-credential-ciphertext",
                    "data": {
                      "keyId": "key-1",
                      "token": "token-secret",
                      "cipher_text": "secret-snake-cipher"
                    }
                  }
                }
                """);

        String summary = SmartLockSwitchBotClient.responseBodyKeySummary(root);

        assertTrue(summary.contains("statusCode"));
        assertTrue(summary.contains("body"));
        assertTrue(summary.contains("commandId"));
        assertTrue(summary.contains("keyId"));
        assertFalse(summary.contains("123456"));
        assertFalse(summary.contains("plain-iv"));
        assertFalse(summary.contains("token-secret"));
        assertFalse(summary.contains("password"));
        assertFalse(summary.contains("iv"));
        assertFalse(summary.contains("token"));
        assertFalse(summary.toLowerCase().contains("cipher"));
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

    private static Store store(String timezone) {
        Store store = new Store();
        store.setId(STORE_ID);
        store.setUserId(USER_ID);
        store.setName("SwitchBot Test Store");
        store.setTimezone(timezone);
        return store;
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

        @Bean
        StoreRepository storeRepository() {
            return mock(StoreRepository.class);
        }
    }
}
