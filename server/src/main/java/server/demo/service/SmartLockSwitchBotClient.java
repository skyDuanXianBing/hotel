package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SmartLockConfig;
import server.demo.entity.Store;
import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.repository.StoreRepository;
import server.demo.util.SmartLockCredentialCrypto;
import server.demo.util.SmartLockMaskingUtils;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SmartLockSwitchBotClient implements SmartLockProviderClient {
    private static final Logger logger = LoggerFactory.getLogger(SmartLockSwitchBotClient.class);
    private static final String DEVICES_PATH = "/v1.1/devices";
    private static final String STATUS_PATH_TEMPLATE = "/v1.1/devices/%s/status";
    private static final String COMMAND_PATH_TEMPLATE = "/v1.1/devices/%s/commands";
    private static final String JSON_UTF8_CONTENT_TYPE = "application/json; charset=utf8";
    private static final String SMART_LOCK_DEVICE_TYPE_PREFIX = "smart lock";
    private static final int SUCCESS_STATUS_CODE = 100;
    private static final int DIAGNOSTIC_HASH_HEX_LENGTH = 12;
    private static final int MAX_DIAGNOSTIC_FIELD_NAMES = 12;
    private static final String PASSCODE_PARAMETER_TYPE = "timeLimit";
    private static final String CREATE_KEY_WAITING_KEYLIST_MESSAGE =
            "SwitchBot 已返回 success 但未返回 commandId 或 keyId，等待 keyList 同步";
    private static final String DELETE_KEY_WAITING_RECONCILIATION_MESSAGE =
            "SwitchBot 已返回 success 但未返回 commandId，等待 keyList 对账确认密码删除结果";
    private static final String REDACTED = "[REDACTED]";
    private static final Pattern CIPHER_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)([\"']?\\b[A-Za-z0-9_-]*cipher(?:[-_\\s]*text)?[A-Za-z0-9_-]*\\b[\"']?\\s*[:=]\\s*)"
                    + "(\"[^\"]*\"|'[^']*'|[^\\s,;{}\\]\\)&]+)"
    );
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
    private final StoreRepository storeRepository;

    @Autowired
    public SmartLockSwitchBotClient(
            SmartLockConfig config,
            SmartLockCredentialCrypto crypto,
            ObjectMapper objectMapper,
            StoreRepository storeRepository
    ) {
        this(config, crypto, objectMapper, storeRepository, new RestTemplate());
    }

    SmartLockSwitchBotClient(
            SmartLockConfig config,
            SmartLockCredentialCrypto crypto,
            ObjectMapper objectMapper,
            StoreRepository storeRepository,
            RestTemplate restTemplate
    ) {
        this.config = config;
        this.crypto = crypto;
        this.objectMapper = objectMapper;
        this.storeRepository = storeRepository;
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
        parameter.put("type", PASSCODE_PARAMETER_TYPE);
        parameter.put("password", command.passcode());
        ZoneId storeZoneId = resolveStoreZoneId();
        Long startTime = toEpochSeconds(command.validFrom(), storeZoneId);
        Long endTime = toEpochSeconds(command.validUntil(), storeZoneId);
        if (startTime != null) {
            parameter.put("startTime", startTime);
        }
        if (endTime != null) {
            parameter.put("endTime", endTime);
        }
        logCreateKeyRequest(providerLockId, command, startTime, endTime, storeZoneId);
        JsonNode root = command(credentials, providerLockId, "createKey", parameter);
        String taskId = extractCommandTaskId(root);
        String keyId = extractCommandPasscodeId(root);
        logCommandResponseParse("createKey", providerLockId, root, taskId, keyId);
        if (!hasText(taskId) && !hasText(keyId)) {
            return new ProviderTaskResult(
                    SmartLockTaskStatus.PENDING,
                    null,
                    null,
                    CREATE_KEY_WAITING_KEYLIST_MESSAGE
            );
        }
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
        String taskId = extractCommandTaskId(root);
        String responsePasscodeId = extractCommandPasscodeId(root);
        logCommandResponseParse("deleteKey", providerLockId, root, taskId, responsePasscodeId);
        if (!hasText(taskId)) {
            return new ProviderTaskResult(
                    SmartLockTaskStatus.PENDING,
                    null,
                    providerPasscodeId,
                    DELETE_KEY_WAITING_RECONCILIATION_MESSAGE
            );
        }
        return new ProviderTaskResult(SmartLockTaskStatus.PENDING, taskId, providerPasscodeId, "SwitchBot 密码删除命令已受理，等待 webhook 确认");
    }

    @Override
    public List<ProviderPasscodeSnapshot> listPasscodes(
            SmartLockCredentialData credentials,
            String providerLockId
    ) {
        return inspectPasscodes(credentials, providerLockId).passcodes();
    }

    @Override
    public ProviderPasscodeListSnapshot inspectPasscodes(
            SmartLockCredentialData credentials,
            String providerLockId
    ) {
        JsonNode root = exchangeJson(credentials, HttpMethod.GET, DEVICES_PATH, null);
        JsonNode listNode = root.path("body").path("deviceList");
        if (!listNode.isArray()) {
            return new ProviderPasscodeListSnapshot(false, false, null, null, null, null, List.of());
        }
        for (JsonNode deviceNode : listNode) {
            if (!providerLockId.equals(text(deviceNode, "deviceId"))) {
                continue;
            }
            String deviceType = text(deviceNode, "deviceType");
            Boolean online = onlineValue(deviceNode);
            String linkedLockDeviceId = text(deviceNode, "lockDeviceId");
            String hubDeviceId = text(deviceNode, "hubDeviceId");
            JsonNode keyListNode = deviceNode.path("keyList");
            if (!keyListNode.isArray()) {
                return new ProviderPasscodeListSnapshot(
                        true,
                        false,
                        deviceType,
                        online,
                        linkedLockDeviceId,
                        hubDeviceId,
                        List.of()
                );
            }
            List<ProviderPasscodeSnapshot> passcodes = new ArrayList<>();
            int entryIndex = 0;
            for (JsonNode keyNode : keyListNode) {
                String passcodeId = firstText(keyNode, "id", "keyId", "passcodeId");
                String passcodeName = firstText(keyNode, "name", "keyName", "passcodeName");
                logKeyListEntryInspection(providerLockId, deviceType, entryIndex, keyNode, passcodeId, passcodeName);
                entryIndex += 1;
                if (!hasText(passcodeId) && !hasText(passcodeName)) {
                    continue;
                }
                passcodes.add(new ProviderPasscodeSnapshot(
                        passcodeId,
                        passcodeName,
                        text(keyNode, "status")
                ));
            }
            return new ProviderPasscodeListSnapshot(
                    true,
                    true,
                    deviceType,
                    online,
                    linkedLockDeviceId,
                    hubDeviceId,
                    passcodes
            );
        }
        return new ProviderPasscodeListSnapshot(false, false, null, null, null, null, List.of());
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
        String taskId = extractCommandTaskId(root);
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
                payload,
                command,
                providerLockId
        );
    }

    private JsonNode exchangeJson(
            SmartLockCredentialData credentials,
            HttpMethod method,
            String path,
            Object body
    ) {
        return exchangeJson(credentials, method, path, body, null, null);
    }

    private JsonNode exchangeJson(
            SmartLockCredentialData credentials,
            HttpMethod method,
            String path,
            Object body,
            String command,
            String providerLockId
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
            if (hasText(command)) {
                logCommandResponseParse(command, providerLockId, root, null, null);
            }
            String message = firstText(root, "message", "error");
            throw new RuntimeException(
                    "SwitchBot 请求失败: " + fallback(safeDiagnosticMessage(message), String.valueOf(statusCode))
            );
        }
        return root;
    }

    private void logCreateKeyRequest(
            String providerLockId,
            PasscodeCommand command,
            Long startTime,
            Long endTime,
            ZoneId storeZoneId
    ) {
        LocalDateTime storeLocalNow = LocalDateTime.now(storeZoneId);
        logger.info(
                "SwitchBot command request prepared deviceSuffix={} {}",
                identifierSuffix(providerLockId),
                createKeyRequestDiagnostic(command, startTime, endTime, storeZoneId, storeLocalNow)
        );
    }

    private void logCommandResponseParse(
            String command,
            String providerLockId,
            JsonNode root,
            String extractedTaskId,
            String extractedPasscodeId
    ) {
        int statusCode = root != null ? root.path("statusCode").asInt(-1) : -1;
        String message = root != null ? firstText(root, "message", "error") : null;
        logger.info(
                "SwitchBot command response parsed command={} deviceSuffix={} statusCode={} message={} "
                        + "bodyPresent={} dataPresent={} bodyKeySummary={} fields={} extractedTaskIdPresent={} "
                        + "extractedPasscodeIdPresent={}",
                command,
                identifierSuffix(providerLockId),
                statusCode,
                safeDiagnosticMessage(message),
                hasField(root, "body"),
                hasDataField(root),
                responseBodyKeySummary(root),
                commandFieldPresenceSummary(root),
                hasText(extractedTaskId),
                hasText(extractedPasscodeId)
        );
    }

    private void logKeyListEntryInspection(
            String providerLockId,
            String deviceType,
            int entryIndex,
            JsonNode keyNode,
            String passcodeId,
            String passcodeName
    ) {
        logger.info(
                "SwitchBot keyList entry inspect deviceSuffix={} deviceType={} entryIndex={} {}",
                identifierSuffix(providerLockId),
                safeDiagnosticMessage(deviceType),
                entryIndex,
                keyListEntryDiagnostic(keyNode, passcodeId, passcodeName)
        );
    }

    static String createKeyRequestDiagnostic(
            PasscodeCommand command,
            Long startTime,
            Long endTime,
            ZoneId storeZoneId,
            LocalDateTime storeLocalNow
    ) {
        LocalDateTime validFrom = command != null ? command.validFrom() : null;
        LocalDateTime validUntil = command != null ? command.validUntil() : null;
        String passcodeName = command != null ? command.passcodeName() : null;
        String passcode = command != null ? command.passcode() : null;
        return "command=createKey"
                + " parameterType=" + PASSCODE_PARAMETER_TYPE
                + " nameHash=" + diagnosticHash(passcodeName)
                + " nameLength=" + textLength(passcodeName)
                + " passwordLength=" + textLength(passcode)
                + " startTime=" + startTime
                + " endTime=" + endTime
                + " storeZone=" + storeZoneId
                + " storeLocalNow=" + storeLocalNow
                + " validFromLocal=" + validFrom
                + " validUntilLocal=" + validUntil
                + " startRelation=" + startRelation(validFrom, storeLocalNow)
                + " endAfterStart=" + endAfterStart(validFrom, validUntil)
                + " endAfterNow=" + endAfterNow(validUntil, storeLocalNow);
    }

    static String keyListEntryDiagnostic(
            JsonNode keyNode,
            String passcodeId,
            String passcodeName
    ) {
        return "idPresent=" + hasText(passcodeId)
                + " nameHash=" + diagnosticHash(passcodeName)
                + " nameLength=" + textLength(passcodeName)
                + " type=" + safeDiagnosticMessage(firstText(keyNode, "type", "keyType"))
                + " status=" + safeDiagnosticMessage(text(keyNode, "status"))
                + " createTimePresent=" + hasAnyField(
                        keyNode,
                        "createTime",
                        "create_time",
                        "createdAt",
                        "created_at"
                );
    }

    static String responseBodyKeySummary(JsonNode root) {
        JsonNode body = root == null ? null : root.path("body");
        JsonNode data = root == null ? null : root.path("data");
        JsonNode bodyData = body == null ? null : body.path("data");
        return "root" + safeFieldNames(root)
                + ",body" + safeFieldNames(body)
                + ",data" + safeFieldNames(data)
                + ",bodyData" + safeFieldNames(bodyData);
    }

    private static String commandFieldPresenceSummary(JsonNode root) {
        JsonNode body = root == null ? null : root.path("body");
        JsonNode data = root == null ? null : root.path("data");
        JsonNode bodyData = body == null ? null : body.path("data");
        return "root{" + commandFieldPresenceFor(root)
                + "},body{" + commandFieldPresenceFor(body)
                + "},data{" + commandFieldPresenceFor(data)
                + "},bodyData{" + commandFieldPresenceFor(bodyData) + "}";
    }

    private static String commandFieldPresenceFor(JsonNode node) {
        return "commandId=" + hasField(node, "commandId")
                + ",commandID=" + hasField(node, "commandID")
                + ",command_id=" + hasField(node, "command_id")
                + ",taskId=" + hasField(node, "taskId")
                + ",task_id=" + hasField(node, "task_id")
                + ",providerTaskId=" + hasField(node, "providerTaskId")
                + ",provider_task_id=" + hasField(node, "provider_task_id")
                + ",keyId=" + hasField(node, "keyId")
                + ",key_id=" + hasField(node, "key_id")
                + ",passcodeId=" + hasField(node, "passcodeId")
                + ",passcode_id=" + hasField(node, "passcode_id")
                + ",providerPasscodeId=" + hasField(node, "providerPasscodeId")
                + ",provider_passcode_id=" + hasField(node, "provider_passcode_id")
                + ",id=" + hasField(node, "id");
    }

    private static String extractCommandTaskId(JsonNode root) {
        return firstTextFromNodes(
                root,
                "commandId",
                "commandID",
                "command_id",
                "taskId",
                "task_id",
                "providerTaskId",
                "provider_task_id"
        );
    }

    private static String extractCommandPasscodeId(JsonNode root) {
        return firstTextFromNodes(
                root,
                "keyId",
                "key_id",
                "passcodeId",
                "passcode_id",
                "providerPasscodeId",
                "provider_passcode_id",
                "id"
        );
    }

    private static String firstTextFromNodes(JsonNode root, String... fields) {
        if (root == null) {
            return null;
        }
        JsonNode body = root.path("body");
        String value = firstText(body, fields);
        if (hasText(value)) {
            return value;
        }
        JsonNode bodyData = body.path("data");
        value = firstText(bodyData, fields);
        if (hasText(value)) {
            return value;
        }
        JsonNode data = root.path("data");
        value = firstText(data, fields);
        if (hasText(value)) {
            return value;
        }
        return firstText(root, fields);
    }

    private static boolean hasDataField(JsonNode root) {
        return hasField(root, "data") || hasField(root == null ? null : root.path("body"), "data");
    }

    private static boolean hasField(JsonNode node, String field) {
        return node != null && node.has(field);
    }

    private static boolean hasAnyField(JsonNode node, String... fields) {
        if (node == null) {
            return false;
        }
        for (String field : fields) {
            if (node.has(field)) {
                return true;
            }
        }
        return false;
    }

    private static String safeFieldNames(JsonNode node) {
        if (node == null || !node.isObject()) {
            return "[]";
        }
        List<String> fieldNames = new ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext() && fieldNames.size() < MAX_DIAGNOSTIC_FIELD_NAMES) {
            String fieldName = iterator.next();
            if (isSensitiveFieldName(fieldName)) {
                continue;
            }
            fieldNames.add(fieldName);
        }
        if (iterator.hasNext()) {
            fieldNames.add("...");
        }
        return fieldNames.toString();
    }

    private static boolean isSensitiveFieldName(String fieldName) {
        if (!hasText(fieldName)) {
            return false;
        }
        String normalized = fieldName.trim().toLowerCase();
        return normalized.equals("password")
                || normalized.equals("passcode")
                || normalized.equals("iv")
                || normalized.contains("cipher")
                || normalized.equals("token")
                || normalized.equals("secret")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("authorization")
                || normalized.contains("credential");
    }

    private static String identifierSuffix(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    private static String safeDiagnosticMessage(String message) {
        if (!hasText(message)) {
            return null;
        }
        String redacted = SmartLockMaskingUtils.redactSensitiveMessage(message);
        if (redacted == null) {
            return null;
        }
        redacted = redactCipherKeyValues(redacted);
        return redacted.length() <= 200 ? redacted : redacted.substring(0, 200);
    }

    private static String redactCipherKeyValues(String message) {
        Matcher matcher = CIPHER_KEY_VALUE_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + redactedValue(matcher.group(2));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String redactedValue(String value) {
        if (value == null || value.length() < 2) {
            return REDACTED;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return first + REDACTED + String.valueOf(last);
        }
        return REDACTED;
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

    private Long toEpochSeconds(LocalDateTime value, ZoneId storeZoneId) {
        if (value == null) {
            return null;
        }
        return value.atZone(storeZoneId).toEpochSecond();
    }

    private ZoneId resolveStoreZoneId() {
        Long storeId = StoreContextUtils.requireStoreId();
        Store store = storeRepository == null ? null : storeRepository.findById(storeId).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
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

    private static String startRelation(LocalDateTime validFrom, LocalDateTime storeLocalNow) {
        if (validFrom == null || storeLocalNow == null) {
            return "missing";
        }
        if (validFrom.isBefore(storeLocalNow)) {
            return "past";
        }
        if (validFrom.isEqual(storeLocalNow)) {
            return "now";
        }
        return "future";
    }

    private static Boolean endAfterStart(LocalDateTime validFrom, LocalDateTime validUntil) {
        if (validFrom == null || validUntil == null) {
            return null;
        }
        return validUntil.isAfter(validFrom);
    }

    private static Boolean endAfterNow(LocalDateTime validUntil, LocalDateTime storeLocalNow) {
        if (validUntil == null || storeLocalNow == null) {
            return null;
        }
        return validUntil.isAfter(storeLocalNow);
    }

    private static Integer textLength(String value) {
        if (value == null) {
            return null;
        }
        return value.length();
    }

    private static String diagnosticHash(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(bytes);
            return hex.substring(0, DIAGNOSTIC_HASH_HEX_LENGTH);
        } catch (Exception ex) {
            return "unavailable";
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
