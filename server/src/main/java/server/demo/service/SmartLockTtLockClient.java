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
    private static final String PASSCODE_GET_PATH = "/v3/keyboardPwd/get";
    private static final String PASSCODE_ADD_PATH = "/v3/keyboardPwd/add";
    private static final String PASSCODE_DELETE_PATH = "/v3/keyboardPwd/delete";
    private static final String PASSCODE_LIST_PATH = "/v3/lock/listKeyboardPwd";
    private static final int LOCK_LIST_PAGE_SIZE = 10000;
    private static final int PASSCODE_LIST_PAGE_SIZE = 100;
    private static final String TTLOCK_PERIOD_PASSCODE_TYPE = "3";
    private static final String TTLOCK_DEFAULT_KEYBOARD_PWD_VERSION = "4";
    private static final String LOCK_STATUS_LOCKED = "locked";
    private static final String LOCK_STATUS_UNLOCKED = "unlocked";
    private static final String LOCK_STATUS_UNKNOWN = "unknown";

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
        listDevices(credentials);
    }

    @Override
    public SmartLockCredentialData refreshToken(SmartLockCredentialData credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", credentials.getTtLockClientId());
        form.add("client_secret", credentials.getTtLockClientSecret());
        boolean refreshGrant = hasText(credentials.getTtLockRefreshToken());
        if (refreshGrant) {
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
            throw tokenException(root, refreshGrant);
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
                            null,
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
                normalizeOpenState(firstText(statusRoot, "state", "lockStatus")),
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
        if (!hasText(command.passcode())) {
            throw new IllegalArgumentException("TTLock 自定义密码不能为空");
        }
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

    public TtLockPasscodeCommandResult createPeriodPasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            PasscodeCommand command
    ) {
        return createPeriodPasscode(credentials, providerLockId, command, TTLOCK_DEFAULT_KEYBOARD_PWD_VERSION);
    }

    public TtLockPasscodeCommandResult createPeriodPasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            PasscodeCommand command,
            String keyboardPwdVersion
    ) {
        if (command.validFrom() == null || command.validUntil() == null) {
            throw new IllegalArgumentException("TTLock 密码必须提供 startDate 和 endDate");
        }
        MultiValueMap<String, String> form = paramsMap(credentials, providerLockId);
        form.add("keyboardPwdType", TTLOCK_PERIOD_PASSCODE_TYPE);
        form.add("keyboardPwdVersion", fallback(keyboardPwdVersion, TTLOCK_DEFAULT_KEYBOARD_PWD_VERSION));
        form.add("startDate", String.valueOf(toEpochMillis(command.validFrom())));
        form.add("endDate", String.valueOf(toEpochMillis(command.validUntil())));
        JsonNode root = postForm(credentials, PASSCODE_GET_PATH, form);
        String passcode = firstText(root, "keyboardPwd", "pwd");
        if (!hasText(passcode)) {
            throw new RuntimeException("TTLock 自动密码创建成功但未返回 keyboardPwd");
        }
        String providerPasscodeId = firstText(root, "keyboardPwdId", "pwdId");
        String message = hasText(providerPasscodeId)
                ? "TTLock 自动密码已创建"
                : "TTLock 自动密码已返回但缺少 keyboardPwdId，请刷新密码列表同步";
        ProviderTaskResult taskResult = new ProviderTaskResult(
                SmartLockTaskStatus.SUCCESS,
                text(root, "taskId"),
                providerPasscodeId,
                message
        );
        return new TtLockPasscodeCommandResult(taskResult, passcode);
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
    public List<ProviderPasscodeSnapshot> listPasscodes(
            SmartLockCredentialData credentials,
            String providerLockId
    ) {
        return listKeyboardPasscodes(credentials, providerLockId)
                .stream()
                .map(snapshot -> new ProviderPasscodeSnapshot(
                        snapshot.providerPasscodeId(),
                        snapshot.passcodeName(),
                        snapshot.status()
                ))
                .toList();
    }

    public List<TtLockPasscodeSnapshot> listKeyboardPasscodes(
            SmartLockCredentialData credentials,
            String providerLockId
    ) {
        List<TtLockPasscodeSnapshot> passcodes = new ArrayList<>();
        int pageNo = 1;
        int pageCount = 1;
        do {
            JsonNode root = get(credentials, PASSCODE_LIST_PATH, params(credentials)
                    .queryParam("lockId", providerLockId)
                    .queryParam("pageNo", String.valueOf(pageNo))
                    .queryParam("pageSize", String.valueOf(PASSCODE_LIST_PAGE_SIZE)));
            JsonNode listNode = root.path("list");
            if (listNode.isArray()) {
                for (JsonNode node : listNode) {
                    String providerPasscodeId = firstText(node, "keyboardPwdId", "pwdId");
                    passcodes.add(new TtLockPasscodeSnapshot(
                            providerPasscodeId,
                            firstText(node, "keyboardPwdName", "pwdName", "name"),
                            firstText(node, "status", "keyboardPwdStatus"),
                            firstText(node, "keyboardPwd", "pwd"),
                            epochMillisValue(node, "startDate"),
                            epochMillisValue(node, "endDate"),
                            booleanValue(node, "isCustom"),
                            toJson(node)
                    ));
                }
            }
            pageCount = Math.max(pageCount, root.path("pages").asInt(pageCount));
            pageNo++;
        } while (pageNo <= pageCount);
        return passcodes;
    }

    @Override
    public ProviderTaskResult queryTask(SmartLockCredentialData credentials, String providerTaskId) {
        return new ProviderTaskResult(
                SmartLockTaskStatus.FAILED,
                providerTaskId,
                null,
                "TTLock 未配置可靠远程任务查询 API，请刷新密码列表同步最终状态"
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
        throw new RuntimeException("TTLock 请求失败: errcode=" + errCode.asText()
                + " errmsg=" + fallback(firstText(root, "errmsg", "description"), errCode.asText()));
    }

    private TtLockTokenException tokenException(JsonNode root, boolean refreshGrant) {
        String errcode = firstText(root, "errcode", "error", "errorCode", "code");
        String errmsg = fallback(
                firstText(root, "errmsg", "description", "error_description", "message"),
                "无 access_token"
        );
        String operation = refreshGrant ? "refresh_token" : "password_grant";
        return new TtLockTokenException(operation, TOKEN_PATH, errcode, errmsg);
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

    private LocalDateTime fromEpochMillis(long epochMillis) {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epochMillis),
                resolveStoreZoneId()
        );
    }

    private ZoneId resolveStoreZoneId() {
        Long storeId = StoreContextUtils.requireStoreId();
        Store store = storeRepository == null ? null : storeRepository.findById(storeId).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
    }

    private long nowMillis() {
        return clock.millis();
    }

    private String normalizeOpenState(String state) {
        String normalized = state == null ? "" : state.trim().toLowerCase();
        if ("0".equals(normalized) || "locked".equals(normalized) || "lock".equals(normalized)) {
            return LOCK_STATUS_LOCKED;
        }
        if ("1".equals(normalized) || "unlocked".equals(normalized) || "unlock".equals(normalized)) {
            return LOCK_STATUS_UNLOCKED;
        }
        if ("2".equals(normalized) || "unknown".equals(normalized)) {
            return LOCK_STATUS_UNKNOWN;
        }
        return LOCK_STATUS_UNKNOWN;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
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

    private LocalDateTime epochMillisValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        long epochMillis = value.asLong(0L);
        if (epochMillis <= 0L) {
            return null;
        }
        return fromEpochMillis(epochMillis);
    }

    private static Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String normalized = value.asText("").trim().toLowerCase();
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized)) {
            return false;
        }
        return null;
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

    public record TtLockPasscodeCommandResult(
            ProviderTaskResult taskResult,
            String passcode
    ) {
    }

    public record TtLockPasscodeSnapshot(
            String providerPasscodeId,
            String passcodeName,
            String status,
            String passcode,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            Boolean custom,
            String rawJson
    ) {
    }

    public static final class TtLockTokenException extends RuntimeException {
        private final String operation;
        private final String endpoint;
        private final String errcode;
        private final String errmsg;

        TtLockTokenException(String operation, String endpoint, String errcode, String errmsg) {
            super(buildMessage(errcode, errmsg));
            this.operation = operation;
            this.endpoint = endpoint;
            this.errcode = errcode;
            this.errmsg = errmsg;
        }

        private static String buildMessage(String errcode, String errmsg) {
            if (hasText(errcode)) {
                return "TTLock token 获取失败: errcode=" + errcode + " errmsg=" + fallback(errmsg, "未知错误");
            }
            return "TTLock token 获取失败: " + fallback(errmsg, "未知错误");
        }

        public String getOperation() {
            return operation;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getErrcode() {
            return errcode;
        }

        public String getErrmsg() {
            return errmsg;
        }
    }
}
