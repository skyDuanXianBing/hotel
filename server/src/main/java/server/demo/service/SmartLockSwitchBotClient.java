package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SmartLockConfig;
import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.util.SmartLockCredentialCrypto;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SmartLockSwitchBotClient implements SmartLockProviderClient {
    private static final String DEVICES_PATH = "/v1.1/devices";
    private static final String STATUS_PATH_TEMPLATE = "/v1.1/devices/%s/status";
    private static final String COMMAND_PATH_TEMPLATE = "/v1.1/devices/%s/commands";
    private static final String JSON_UTF8_CONTENT_TYPE = "application/json; charset=utf8";
    private static final String SMART_LOCK_DEVICE_TYPE_PREFIX = "smart lock";
    private static final int SUCCESS_STATUS_CODE = 100;
    private static final List<String> SUPPORTED_LOCK_DEVICE_TYPES = List.of(
            "Lock",
            "Lock Pro",
            "Lock Lite",
            "Lock Ultra",
            "Smart Lock",
            "Smart Lock Pro",
            "Smart Lock Ultra",
            "Smart Lock Pro Wifi",
            "Lock Vision",
            "Lock Vision Pro",
            "Keypad",
            "Keypad Touch",
            "Keypad Vision",
            "Keypad Vision Pro"
    );

    private final SmartLockConfig config;
    private final SmartLockCredentialCrypto crypto;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public SmartLockSwitchBotClient(
            SmartLockConfig config,
            SmartLockCredentialCrypto crypto,
            ObjectMapper objectMapper
    ) {
        this(config, crypto, objectMapper, new RestTemplate());
    }

    SmartLockSwitchBotClient(
            SmartLockConfig config,
            SmartLockCredentialCrypto crypto,
            ObjectMapper objectMapper,
            RestTemplate restTemplate
    ) {
        this.config = config;
        this.crypto = crypto;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public SmartLockProvider getProvider() {
        return SmartLockProvider.SWITCHBOT;
    }

    @Override
    public void testConnection(SmartLockCredentialData credentials) {
        listDevices(credentials);
    }

    @Override
    public SmartLockCredentialData refreshToken(SmartLockCredentialData credentials) {
        testConnection(credentials);
        return credentials;
    }

    @Override
    public List<DeviceSnapshot> listDevices(SmartLockCredentialData credentials) {
        JsonNode root = exchangeJson(credentials, HttpMethod.GET, DEVICES_PATH, null);
        JsonNode listNode = root.path("body").path("deviceList");
        List<DeviceSnapshot> devices = new ArrayList<>();
        if (!listNode.isArray()) {
            return devices;
        }
        for (JsonNode node : listNode) {
            String deviceId = text(node, "deviceId");
            String deviceType = text(node, "deviceType");
            if (!hasText(deviceId)) {
                continue;
            }
            if (!isSupportedDeviceType(deviceType)) {
                continue;
            }
            devices.add(new DeviceSnapshot(
                    deviceId,
                    fallback(text(node, "deviceName"), deviceId),
                    deviceType,
                    text(node, "lockDeviceId"),
                    null,
                    null,
                    onlineValue(node),
                    toJson(node)
            ));
        }
        return devices;
    }

    @Override
    public LockStatusSnapshot getStatus(SmartLockCredentialData credentials, String providerLockId) {
        JsonNode root = exchangeJson(
                credentials,
                HttpMethod.GET,
                String.format(STATUS_PATH_TEMPLATE, providerLockId),
                null
        );
        JsonNode body = root.path("body");
        return new LockStatusSnapshot(
                firstText(body, "lockState", "doorState"),
                intValue(body, "battery"),
                onlineValue(body),
                toJson(body)
        );
    }

    @Override
    public ProviderTaskResult unlock(SmartLockCredentialData credentials, String providerLockId) {
        return sendLockCommand(credentials, providerLockId, "unlock");
    }

    @Override
    public ProviderTaskResult lock(SmartLockCredentialData credentials, String providerLockId) {
        return sendLockCommand(credentials, providerLockId, "lock");
    }

    @Override
    public ProviderTaskResult createPasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            PasscodeCommand command
    ) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("name", command.passcodeName());
        parameter.put("type", "timeLimit");
        parameter.put("password", command.passcode());
        Long startTime = toEpochSeconds(command.validFrom());
        Long endTime = toEpochSeconds(command.validUntil());
        if (startTime != null) {
            parameter.put("startTime", startTime);
        }
        if (endTime != null) {
            parameter.put("endTime", endTime);
        }
        JsonNode root = command(credentials, providerLockId, "createKey", parameter);
        String taskId = firstText(root.path("body"), "taskId", "commandId");
        String keyId = firstText(root.path("body"), "keyId", "passcodeId");
        return new ProviderTaskResult(SmartLockTaskStatus.PENDING, taskId, keyId, "SwitchBot 密码创建命令已受理，等待 webhook 确认");
    }

    @Override
    public ProviderTaskResult deletePasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            String providerPasscodeId
    ) {
        Map<String, Object> parameter = Map.of("id", providerPasscodeId);
        JsonNode root = command(credentials, providerLockId, "deleteKey", parameter);
        String taskId = firstText(root.path("body"), "taskId", "commandId");
        return new ProviderTaskResult(SmartLockTaskStatus.PENDING, taskId, providerPasscodeId, "SwitchBot 密码删除命令已受理，等待 webhook 确认");
    }

    @Override
    public ProviderTaskResult queryTask(SmartLockCredentialData credentials, String providerTaskId) {
        return new ProviderTaskResult(SmartLockTaskStatus.PENDING, providerTaskId, null, "SwitchBot 未提供独立任务查询 API，等待 webhook 更新");
    }

    private ProviderTaskResult sendLockCommand(
            SmartLockCredentialData credentials,
            String providerLockId,
            String command
    ) {
        JsonNode root = command(credentials, providerLockId, command, "default");
        String taskId = firstText(root.path("body"), "taskId", "commandId");
        String commandLabel = "unlock".equals(command) ? "开锁" : "上锁";
        return new ProviderTaskResult(
                SmartLockTaskStatus.SUCCESS,
                taskId,
                null,
                "SwitchBot " + commandLabel + "指令已发送，请刷新状态确认实体门锁结果"
        );
    }

    private JsonNode command(
            SmartLockCredentialData credentials,
            String providerLockId,
            String command,
            Object parameter
    ) {
        Map<String, Object> payload = Map.of(
                "command", command,
                "parameter", parameter,
                "commandType", "command"
        );
        return exchangeJson(
                credentials,
                HttpMethod.POST,
                String.format(COMMAND_PATH_TEMPLATE, providerLockId),
                payload
        );
    }

    private JsonNode exchangeJson(
            SmartLockCredentialData credentials,
            HttpMethod method,
            String path,
            Object body
    ) {
        HttpHeaders headers = switchBotHeaders(credentials);
        headers.set(HttpHeaders.CONTENT_TYPE, JSON_UTF8_CONTENT_TYPE);
        headers.set(HttpHeaders.ACCEPT, "application/json");
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                config.getSwitchBotBaseUrl() + path,
                method,
                entity,
                String.class
        );
        JsonNode root = parseJson(response.getBody());
        int statusCode = root.path("statusCode").asInt(-1);
        if (statusCode != SUCCESS_STATUS_CODE) {
            String message = firstText(root, "message", "error");
            throw new RuntimeException("SwitchBot 请求失败: " + fallback(message, String.valueOf(statusCode)));
        }
        return root;
    }

    private HttpHeaders switchBotHeaders(SmartLockCredentialData credentials) {
        String token = credentials.getSwitchBotToken();
        String secret = credentials.getSwitchBotSecret();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        String sign = crypto.hmacSha256Base64(secret, token + timestamp + nonce);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("sign", sign);
        headers.set("t", timestamp);
        headers.set("nonce", nonce);
        return headers;
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new RuntimeException("SwitchBot 返回格式无法解析", ex);
        }
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static Long toEpochSeconds(java.time.LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toEpochSecond();
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

    private static Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asBoolean();
    }

    private static Boolean onlineValue(JsonNode node) {
        Boolean explicit = booleanValue(node, "online");
        if (explicit != null) {
            return explicit;
        }
        String onlineStatus = text(node, "onlineStatus");
        if (!hasText(onlineStatus)) {
            return null;
        }
        if ("online".equalsIgnoreCase(onlineStatus)) {
            return true;
        }
        if ("offline".equalsIgnoreCase(onlineStatus)) {
            return false;
        }
        return null;
    }

    private static boolean isSupportedDeviceType(String deviceType) {
        if (!hasText(deviceType)) {
            return false;
        }
        String normalizedType = deviceType.trim().toLowerCase();
        if (normalizedType.startsWith(SMART_LOCK_DEVICE_TYPE_PREFIX)) {
            return true;
        }
        for (String supportedType : SUPPORTED_LOCK_DEVICE_TYPES) {
            if (supportedType.equalsIgnoreCase(deviceType)) {
                return true;
            }
        }
        return false;
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
