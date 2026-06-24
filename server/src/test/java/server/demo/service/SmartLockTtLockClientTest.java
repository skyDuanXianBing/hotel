package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import server.demo.config.SmartLockConfig;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.Store;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.repository.StoreRepository;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmartLockTtLockClientTest {
    private static final String BASE_URL = "http://ttlock.test";
    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 26L;
    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-21T10:00:00Z"), ZoneOffset.UTC);

    private MockRestServiceServer server;
    private SmartLockTtLockClient client;
    private SmartLockCredentialData credentials;
    private StoreRepository storeRepository;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        SmartLockConfig config = mock(SmartLockConfig.class);
        when(config.getTtLockBaseUrl()).thenReturn(BASE_URL);
        storeRepository = mock(StoreRepository.class);
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store(STORE_ZONE.getId())));
        client = new SmartLockTtLockClient(config, new ObjectMapper(), FIXED_CLOCK, storeRepository, restTemplate);

        credentials = new SmartLockCredentialData();
        credentials.setTtLockClientId("client-1");
        credentials.setTtLockClientSecret("secret-1");
        credentials.setTtLockUsername("owner@example.com");
        credentials.setTtLockPasswordMd5("5f4dcc3b5aa765d61d8327deb882cf99");
        credentials.setTtLockAccessToken("access-1");
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "owner"));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void refreshToken_withoutRefreshToken_shouldUseOfficialPasswordGrantFields() {
        server.expect(once(), requestTo(BASE_URL + "/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(formMatches(Map.of(
                        "client_id", "client-1",
                        "client_secret", "secret-1",
                        "grant_type", "password",
                        "username", "owner@example.com",
                        "password", "5f4dcc3b5aa765d61d8327deb882cf99"
                ), "clientId", "clientSecret", "refresh_token"))
                .andRespond(withSuccess("""
                        {"access_token":"access-2","refresh_token":"refresh-2","expires_in":90}
                        """, MediaType.APPLICATION_JSON));

        SmartLockCredentialData refreshed = client.refreshToken(credentials);

        assertEquals("access-2", refreshed.getTtLockAccessToken());
        assertEquals("refresh-2", refreshed.getTtLockRefreshToken());
        server.verify();
    }

    @Test
    void refreshToken_withRefreshToken_shouldUseRefreshGrant() {
        credentials.setTtLockRefreshToken("refresh-1");

        server.expect(once(), requestTo(BASE_URL + "/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(formMatches(Map.of(
                        "client_id", "client-1",
                        "client_secret", "secret-1",
                        "grant_type", "refresh_token",
                        "refresh_token", "refresh-1"
                ), "username", "password", "clientId", "clientSecret"))
                .andRespond(withSuccess("""
                        {"access_token":"access-3","refresh_token":"refresh-3","expires_in":90}
                        """, MediaType.APPLICATION_JSON));

        SmartLockCredentialData refreshed = client.refreshToken(credentials);

        assertEquals("access-3", refreshed.getTtLockAccessToken());
        assertEquals("refresh-3", refreshed.getTtLockRefreshToken());
        server.verify();
    }

    @Test
    void createPasscode_shouldSendGatewayAddWithRequiredDates() {
        LocalDateTime validFrom = LocalDateTime.of(2026, 6, 21, 18, 0);
        LocalDateTime validUntil = LocalDateTime.of(2026, 6, 22, 10, 0);
        String startDate = String.valueOf(validFrom.atZone(STORE_ZONE).toInstant().toEpochMilli());
        String endDate = String.valueOf(validUntil.atZone(STORE_ZONE).toInstant().toEpochMilli());

        server.expect(once(), requestTo(BASE_URL + "/v3/keyboardPwd/add"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(formContains(Map.of(
                        "clientId", "client-1",
                        "accessToken", "access-1",
                        "lockId", "101",
                        "keyboardPwd", "123456",
                        "keyboardPwdName", "Guest",
                        "addType", "2",
                        "startDate", startDate,
                        "endDate", endDate,
                        "date", String.valueOf(FIXED_CLOCK.millis())
                )))
                .andRespond(withSuccess("{\"keyboardPwdId\":24242}", MediaType.APPLICATION_JSON));

        SmartLockProviderClient.ProviderTaskResult result = client.createPasscode(
                credentials,
                "101",
                new SmartLockProviderClient.PasscodeCommand("Guest", "123456", validFrom, validUntil)
        );

        assertEquals(SmartLockTaskStatus.SUCCESS, result.status());
        assertEquals("24242", result.providerPasscodeId());
        server.verify();
    }

    @Test
    void createPasscode_shouldRejectMissingDatesBeforeSendingRequest() {
        SmartLockProviderClient.PasscodeCommand command =
                new SmartLockProviderClient.PasscodeCommand("Guest", "123456", null, null);

        assertThrows(IllegalArgumentException.class, () -> client.createPasscode(credentials, "101", command));
        server.verify();
    }

    @Test
    void deletePasscode_shouldSendRemoteDeleteType() {
        server.expect(once(), requestTo(BASE_URL + "/v3/keyboardPwd/delete"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(formContains(Map.of(
                        "clientId", "client-1",
                        "accessToken", "access-1",
                        "lockId", "101",
                        "keyboardPwdId", "24242",
                        "deleteType", "2",
                        "date", String.valueOf(FIXED_CLOCK.millis())
                )))
                .andRespond(withSuccess("{\"errcode\":0,\"errmsg\":\"none error message\"}", MediaType.APPLICATION_JSON));

        SmartLockProviderClient.ProviderTaskResult result = client.deletePasscode(credentials, "101", "24242");

        assertEquals(SmartLockTaskStatus.SUCCESS, result.status());
        assertEquals("24242", result.providerPasscodeId());
        server.verify();
    }

    @Test
    void getStatus_shouldQueryElectricQuantityAndReturnBattery() {
        server.expect(once(), pathAndParams("/v3/lock/queryOpenState", Map.of(
                        "clientId", "client-1",
                        "accessToken", "access-1",
                        "lockId", "101",
                        "date", String.valueOf(FIXED_CLOCK.millis())
                )))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"state\":\"locked\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), pathAndParams("/v3/lock/queryElectricQuantity", Map.of(
                        "clientId", "client-1",
                        "accessToken", "access-1",
                        "lockId", "101",
                        "date", String.valueOf(FIXED_CLOCK.millis())
                )))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"electricQuantity\":77}", MediaType.APPLICATION_JSON));

        SmartLockProviderClient.LockStatusSnapshot status = client.getStatus(credentials, "101");

        assertEquals("locked", status.lockStatus());
        assertEquals(77, status.battery());
        server.verify();
    }

    @Test
    void queryTask_shouldNotCallKeyboardPwdGet() {
        SmartLockProviderClient.ProviderTaskResult result = client.queryTask(credentials, "task-1");

        assertEquals(SmartLockTaskStatus.PENDING, result.status());
        assertEquals("task-1", result.providerTaskId());
        server.verify();
    }

    @Test
    void listDevices_shouldUseLargePageSizeAndMapBattery() {
        server.expect(once(), pathAndParams("/v3/lock/list", Map.of(
                        "clientId", "client-1",
                        "accessToken", "access-1",
                        "pageNo", "1",
                        "pageSize", "10000",
                        "date", String.valueOf(FIXED_CLOCK.millis())
                )))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"list":[{"lockId":101,"lockAlias":"Room 101","electricQuantity":88}],"pages":1}
                        """, MediaType.APPLICATION_JSON));

        List<SmartLockProviderClient.DeviceSnapshot> devices = client.listDevices(credentials);

        assertEquals(1, devices.size());
        assertEquals(88, devices.get(0).battery());
        server.verify();
    }

    @Test
    void springContext_shouldCreateBeanWithAutowiredConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TtLockSpringTestConfig.class, SmartLockTtLockClient.class);
            context.refresh();

            assertNotNull(context.getBean(SmartLockTtLockClient.class));
        }
    }

    private RequestMatcher formMatches(Map<String, String> expected, String... missingNames) {
        return request -> {
            MultiValueMap<String, String> params = formParams(((MockHttpOutputMessage) request).getBodyAsString());
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                assertEquals(entry.getValue(), params.getFirst(entry.getKey()), entry.getKey());
            }
            for (String name : missingNames) {
                assertFalse(params.containsKey(name), name);
            }
        };
    }

    private RequestMatcher formContains(Map<String, String> expected) {
        return request -> {
            MultiValueMap<String, String> params = formParams(((MockHttpOutputMessage) request).getBodyAsString());
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                assertEquals(entry.getValue(), params.getFirst(entry.getKey()), entry.getKey());
            }
        };
    }

    private RequestMatcher pathAndParams(String path, Map<String, String> expected) {
        return request -> {
            URI uri = request.getURI();
            assertEquals(BASE_URL + path, uri.getScheme() + "://" + uri.getHost() + uri.getPath());
            MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                assertEquals(entry.getValue(), params.getFirst(entry.getKey()), entry.getKey());
            }
        };
    }

    private MultiValueMap<String, String> formParams(String body) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (body == null || body.isBlank()) {
            return params;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            params.add(name, value);
        }
        return params;
    }

    private static Store store(String timezone) {
        Store store = new Store();
        store.setId(STORE_ID);
        store.setUserId(USER_ID);
        store.setName("TTLock Test Store");
        store.setTimezone(timezone);
        return store;
    }

    @Configuration(proxyBeanMethods = false)
    private static class TtLockSpringTestConfig {
        @Bean
        SmartLockConfig smartLockConfig() {
            SmartLockConfig config = mock(SmartLockConfig.class);
            when(config.getTtLockBaseUrl()).thenReturn(BASE_URL);
            return config;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Clock clock() {
            return FIXED_CLOCK;
        }

        @Bean
        StoreRepository storeRepository() {
            return mock(StoreRepository.class);
        }
    }
}
