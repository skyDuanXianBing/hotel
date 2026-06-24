package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import server.demo.config.SmartLockConfig;
import server.demo.entity.Store;
import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class SmartLockTtLockClient implements SmartLockProviderClient {
    private static final String TOKEN_PATH = "/oauth2/token";
    private static final String LOCK_LIST_PATH = "/v3/lock/list";
    private static final String LOCK_STATUS_PATH = "/v3/lock/queryOpenState";
    private static final String LOCK_BATTERY_PATH = "/v3/lock/queryElectricQuantity";
    private static final String UNLOCK_PATH = "/v3/lock/unlock";
    private static final String LOCK_PATH = "/v3/lock/lock";
    private static final String PASSCODE_ADD_PATH = "/v3/keyboardPwd/add";
    private static final String PASSCODE_DELETE_PATH = "/v3/keyboardPwd/delete";
    private static final int LOCK_LIST_PAGE_SIZE = 10000;

    private final SmartLockConfig config;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Clock clock;
    private final StoreRepository storeRepository;

    @Autowired
    public SmartLockTtLockClient(
            SmartLockConfig config,
            ObjectMapper objectMapper,
            Clock clock,
            StoreRepository storeRepository
    ) {
        this(config, objectMapper, clock, storeRepository, new RestTemplate());
    }

    SmartLockTtLockClient(
            SmartLockConfig config,
            ObjectMapper objectMapper,
            Clock clock,
            StoreRepository storeRepository,
            RestTemplate restTemplate
    ) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.storeRepository = storeRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public SmartLockProvider getProvider() {
        return SmartLockProvider.TTLOCK;
    }

    @Override
    public void testConnection(SmartLockCredentialData credentials) {
        SmartLockCredentialData tokenReady = refreshToken(credentials);
        listDevices(tokenReady);
    }

    @Override
    public SmartLockCredentialData refreshToken(SmartLockCredentialData credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", credentials.getTtLockClientId());
        form.add("client_secret", credentials.getTtLockClientSecret());
        if (hasText(credentials.getTtLockRefreshToken())) {
            form.add("grant_type", "refresh_token");
            form.add("refresh_token", credentials.getTtLockRefreshToken());
        } else {
            form.add("grant_type", "password");
            form.add("username", credentials.getTtLockUsername());
            form.add("password", credentials.getTtLockPasswordMd5());
        }

        ResponseEntity<String> response = restTemplate.exchange(
                config.getTtLockBaseUrl() + TOKEN_PATH,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class
        );
        JsonNode root = parseJson(response.getBody());
        String accessToken = firstText(root, "access_token", "accessToken");
        if (!hasText(accessToken)) {
            throw new RuntimeException("TTLock token 获取失败: " + fallback(firstText(root, "errmsg", "description"), "无 access_token"));
        }

        SmartLockCredentialData refreshed = credentials.copy();
        refreshed.setTtLockAccessToken(accessToken);
        refreshed.setTtLockRefreshToken(fallback(
                firstText(root, "refresh_token", "refreshToken"),
                credentials.getTtLockRefreshToken()
        ));
        long expiresIn = root.path("expires_in").asLong(root.path("expiresIn").asLong(7200));
        refreshed.setTtLockTokenExpiresAt(LocalDateTime.now(clock).plusSeconds(expiresIn));
        return refreshed;
    }

    @Override
    public List<DeviceSnapshot> listDevices(SmartLockCredentialData credentials) {
        List<DeviceSnapshot> devices = new ArrayList<>();
        int pageNo = 1;
        int pageCount = 1;
        do {
            JsonNode root = get(credentials, LOCK_LIST_PATH, params(credentials)
                    .queryParam("pageNo", String.valueOf(pageNo))
                    .queryParam("pageSize", String.valueOf(LOCK_LIST_PAGE_SIZE)));
            JsonNode listNode = root.path("list");
            if (listNode.isArray()) {
                for (JsonNode node : listNode) {
                    String lockId = text(node, "lockId");
                    if (!hasText(lockId)) {
                        continue;
                    }
                    devices.add(new DeviceSnapshot(
                            lockId,
                            fallback(firstText(node, "lockAlias", "lockName"), lockId),
                            firstText(node, "lockName", "lockMac"),
                            null,
                            intValue(node, "electricQuantity"),
                            firstText(node, "lockStatus", "state"),
                            null,
                            toJson(node)
                    ));
                }
            }
            pageCount = Math.max(pageCount, root.path("pages").asInt(pageCount));
            pageNo++;
        } while (pageNo <= pageCount);
        return devices;
    }

    @Override
    public LockStatusSnapshot getStatus(SmartLockCredentialData credentials, String providerLockId) {
        JsonNode statusRoot = get(credentials, LOCK_STATUS_PATH, params(credentials)
                .queryParam("lockId", providerLockId));
        JsonNode batteryRoot = get(credentials, LOCK_BATTERY_PATH, params(credentials)
                .queryParam("lockId", providerLockId));
        return new LockStatusSnapshot(
                firstText(statusRoot, "state", "lockStatus"),
                intValue(batteryRoot, "electricQuantity"),
                null,
                toJson(statusRoot)
        );
    }

    @Override
    public ProviderTaskResult unlock(SmartLockCredentialData credentials, String providerLockId) {
        JsonNode root = postForm(credentials, UNLOCK_PATH, paramsMap(credentials, providerLockId));
        return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, text(root, "taskId"), null, "TTLock 开锁命令已提交");
    }

    @Override
    public ProviderTaskResult lock(SmartLockCredentialData credentials, String providerLockId) {
        JsonNode root = postForm(credentials, LOCK_PATH, paramsMap(credentials, providerLockId));
        return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, text(root, "taskId"), null, "TTLock 上锁命令已提交");
    }

    @Override
    public ProviderTaskResult createPasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            PasscodeCommand command
    ) {
        MultiValueMap<String, String> form = paramsMap(credentials, providerLockId);
        form.add("keyboardPwd", command.passcode());
        form.add("keyboardPwdName", command.passcodeName());
        form.add("addType", "2");
        if (command.validFrom() == null || command.validUntil() == null) {
            throw new IllegalArgumentException("TTLock 密码必须提供 startDate 和 endDate");
        }
        form.add("startDate", String.valueOf(toEpochMillis(command.validFrom())));
        form.add("endDate", String.valueOf(toEpochMillis(command.validUntil())));
        JsonNode root = postForm(credentials, PASSCODE_ADD_PATH, form);
        return new ProviderTaskResult(
                SmartLockTaskStatus.SUCCESS,
                text(root, "taskId"),
                firstText(root, "keyboardPwdId", "pwdId"),
                "TTLock 密码创建命令已提交"
        );
    }

    @Override
    public ProviderTaskResult deletePasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            String providerPasscodeId
    ) {
        MultiValueMap<String, String> form = paramsMap(credentials, providerLockId);
        form.add("keyboardPwdId", providerPasscodeId);
        form.add("deleteType", "2");
        JsonNode root = postForm(credentials, PASSCODE_DELETE_PATH, form);
        return new ProviderTaskResult(
                SmartLockTaskStatus.SUCCESS,
                text(root, "taskId"),
                providerPasscodeId,
                "TTLock 密码删除命令已提交"
        );
    }

    @Override
    public ProviderTaskResult queryTask(SmartLockCredentialData credentials, String providerTaskId) {
        return new ProviderTaskResult(
                SmartLockTaskStatus.PENDING,
                providerTaskId,
                null,
                "TTLock 未配置可靠远程任务查询 API，返回本地任务状态"
        );
    }

    private UriComponentsBuilder params(SmartLockCredentialData credentials) {
        return UriComponentsBuilder.fromHttpUrl(config.getTtLockBaseUrl())
                .queryParam("clientId", credentials.getTtLockClientId())
                .queryParam("accessToken", credentials.getTtLockAccessToken())
                .queryParam("date", nowMillis());
    }

    private MultiValueMap<String, String> paramsMap(SmartLockCredentialData credentials, String providerLockId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("clientId", credentials.getTtLockClientId());
        form.add("accessToken", credentials.getTtLockAccessToken());
        form.add("lockId", providerLockId);
        form.add("date", String.valueOf(nowMillis()));
        return form;
    }

    private JsonNode get(
            SmartLockCredentialData credentials,
            String path,
            UriComponentsBuilder builder
    ) {
        String url = builder.replacePath(path).build(true).toUriString();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);
        JsonNode root = parseJson(response.getBody());
        ensureSuccess(root);
        return root;
    }

    private JsonNode postForm(
            SmartLockCredentialData credentials,
            String path,
            MultiValueMap<String, String> form
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> response = restTemplate.exchange(
                config.getTtLockBaseUrl() + path,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class
        );
        JsonNode root = parseJson(response.getBody());
        ensureSuccess(root);
        return root;
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new RuntimeException("TTLock 返回格式无法解析", ex);
        }
    }

    private void ensureSuccess(JsonNode root) {
        JsonNode errCode = root.path("errcode");
        if (errCode.isMissingNode() || errCode.asInt() == 0) {
            return;
        }
        throw new RuntimeException("TTLock 请求失败: " + fallback(firstText(root, "errmsg", "description"), errCode.asText()));
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private long toEpochMillis(LocalDateTime value) {
        return value.atZone(resolveStoreZoneId()).toInstant().toEpochMilli();
    }

    private ZoneId resolveStoreZoneId() {
        Long storeId = StoreContextUtils.requireStoreId();
        Store store = storeRepository == null ? null : storeRepository.findById(storeId).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
    }

    private long nowMillis() {
        return clock.millis();
    }

    private static String firstText(JsonNode node, String firstField, String secondField) {
        String first = text(node, firstField);
        if (hasText(first)) {
            return first;
        }
        return text(node, secondField);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static Integer intValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asInt();
    }

    private static String fallback(String value, String fallback) {
        if (hasText(value)) {
            return value;
        }
        return fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
