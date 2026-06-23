package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.config.SmartLockConfig;
import server.demo.dto.SmartLockBindingDTO;
import server.demo.dto.SmartLockConfirmationDTO;
import server.demo.dto.SmartLockDeviceDTO;
import server.demo.dto.SmartLockIntegrationDTO;
import server.demo.dto.SmartLockPasscodeDTO;
import server.demo.dto.SmartLockRequests;
import server.demo.dto.SmartLockRoomDTO;
import server.demo.dto.SmartLockStatusDTO;
import server.demo.dto.SmartLockTaskDTO;
import server.demo.dto.SmartLockTestResultDTO;
import server.demo.entity.Room;
import server.demo.entity.SmartLockConfirmation;
import server.demo.entity.SmartLockDevice;
import server.demo.entity.SmartLockIntegration;
import server.demo.entity.SmartLockPasscodeRecord;
import server.demo.entity.SmartLockRoomBinding;
import server.demo.entity.SmartLockTask;
import server.demo.enums.SmartLockBindingStatus;
import server.demo.enums.SmartLockIntegrationStatus;
import server.demo.enums.SmartLockPasscodeStatus;
import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.enums.SmartLockTaskType;
import server.demo.repository.RoomRepository;
import server.demo.repository.SmartLockConfirmationRepository;
import server.demo.repository.SmartLockDeviceRepository;
import server.demo.repository.SmartLockIntegrationRepository;
import server.demo.repository.SmartLockPasscodeRecordRepository;
import server.demo.repository.SmartLockRoomBindingRepository;
import server.demo.repository.SmartLockTaskRepository;
import server.demo.util.SmartLockCredentialCrypto;
import server.demo.util.SmartLockMaskingUtils;
import server.demo.util.StoreContextUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SmartLockService {
    private static final int CONFIRM_TOKEN_BYTES = 32;
    private static final int GENERATED_PASSCODE_MIN = 100000;
    private static final int GENERATED_PASSCODE_BOUND = 900000;
    private static final int MIN_PASSCODE_LENGTH = 4;
    private static final int MAX_PASSCODE_LENGTH = 12;
    private static final List<SmartLockPasscodeStatus> BINDING_DELETE_RISKY_PASSCODE_STATUSES = List.of(
            SmartLockPasscodeStatus.ACTIVE,
            SmartLockPasscodeStatus.PENDING,
            SmartLockPasscodeStatus.DELETE_PENDING
    );
    private static final String STATUS_SOURCE_DEVICE = "DEVICE";
    private static final String STATUS_SOURCE_BOUND_LOCK = "BOUND_LOCK";
    private static final String STATUS_SOURCE_UNAVAILABLE = "UNAVAILABLE";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<String> SWITCHBOT_AUTHENTICATION_PANEL_TYPES = List.of(
            "Keypad",
            "Keypad Touch",
            "Keypad Vision",
            "Keypad Vision Pro"
    );
    private static final List<String> SWITCHBOT_PASSCODE_DEVICE_TYPES = List.of(
            "Keypad",
            "Keypad Touch",
            "Keypad Vision",
            "Keypad Vision Pro",
            "Lock Vision",
            "Lock Vision Pro"
    );

    private final SmartLockIntegrationRepository integrationRepository;
    private final SmartLockDeviceRepository deviceRepository;
    private final SmartLockRoomBindingRepository bindingRepository;
    private final SmartLockConfirmationRepository confirmationRepository;
    private final SmartLockPasscodeRecordRepository passcodeRepository;
    private final SmartLockTaskRepository taskRepository;
    private final RoomRepository roomRepository;
    private final SmartLockProviderClientRegistry providerRegistry;
    private final SmartLockCredentialCrypto credentialCrypto;
    private final SmartLockMapper mapper;
    private final SmartLockConfig config;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SmartLockService(
            SmartLockIntegrationRepository integrationRepository,
            SmartLockDeviceRepository deviceRepository,
            SmartLockRoomBindingRepository bindingRepository,
            SmartLockConfirmationRepository confirmationRepository,
            SmartLockPasscodeRecordRepository passcodeRepository,
            SmartLockTaskRepository taskRepository,
            RoomRepository roomRepository,
            SmartLockProviderClientRegistry providerRegistry,
            SmartLockCredentialCrypto credentialCrypto,
            SmartLockMapper mapper,
            SmartLockConfig config,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.integrationRepository = integrationRepository;
        this.deviceRepository = deviceRepository;
        this.bindingRepository = bindingRepository;
        this.confirmationRepository = confirmationRepository;
        this.passcodeRepository = passcodeRepository;
        this.taskRepository = taskRepository;
        this.roomRepository = roomRepository;
        this.providerRegistry = providerRegistry;
        this.credentialCrypto = credentialCrypto;
        this.mapper = mapper;
        this.config = config;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SmartLockIntegrationDTO> listIntegrations() {
        Long storeId = StoreContextUtils.requireStoreId();
        return integrationRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream()
                .map(integration -> mapper.toIntegrationDto(integration, decryptCredentials(integration)))
                .collect(Collectors.toList());
    }

    @Transactional
    public SmartLockIntegrationDTO saveIntegration(SmartLockRequests.UpsertIntegrationRequest request) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockProvider provider = requireProvider(request.getProvider());
        SmartLockIntegration integration = integrationRepository.findByStoreIdAndProvider(storeId, provider)
                .orElseGet(SmartLockIntegration::new);
        integration.setStoreId(storeId);
        integration.setProvider(provider);
        applyIntegrationRequest(integration, request);
        SmartLockIntegration saved = integrationRepository.save(integration);
        return mapper.toIntegrationDto(saved, decryptCredentials(saved));
    }

    @Transactional
    public SmartLockIntegrationDTO updateIntegration(Long integrationId, SmartLockRequests.UpsertIntegrationRequest request) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockIntegration integration = requireIntegration(storeId, integrationId);
        applyIntegrationRequest(integration, request);
        SmartLockIntegration saved = integrationRepository.save(integration);
        return mapper.toIntegrationDto(saved, decryptCredentials(saved));
    }

    @Transactional
    public SmartLockTestResultDTO testIntegration(Long integrationId) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockIntegration integration = requireIntegration(storeId, integrationId);
        LocalDateTime now = now();
        try {
            SmartLockCredentialData credentials = ensureProviderToken(integration);
            providerRegistry.getClient(integration.getProvider()).testConnection(credentials);
            integration.setConnectionStatus(SmartLockIntegrationStatus.CONNECTED);
            integration.setLastTestAt(now);
            integration.setLastError(null);
            integrationRepository.save(integration);
            return new SmartLockTestResultDTO(true, "连接测试成功");
        } catch (RuntimeException ex) {
            integration.setConnectionStatus(SmartLockIntegrationStatus.ERROR);
            integration.setLastTestAt(now);
            integration.setLastError(safeError(ex));
            integrationRepository.save(integration);
            return new SmartLockTestResultDTO(false, "连接测试失败: " + safeError(ex));
        }
    }

    @Transactional
    public SmartLockIntegrationDTO refreshToken(Long integrationId) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockIntegration integration = requireIntegration(storeId, integrationId);
        SmartLockCredentialData credentials = decryptCredentials(integration);
        SmartLockProviderClient client = providerRegistry.getClient(integration.getProvider());
        SmartLockCredentialData refreshed = client.refreshToken(credentials);
        persistCredentials(integration, refreshed);
        integration.setConnectionStatus(SmartLockIntegrationStatus.CONNECTED);
        integration.setLastError(null);
        SmartLockIntegration saved = integrationRepository.save(integration);
        return mapper.toIntegrationDto(saved, refreshed);
    }

    @Transactional
    public List<SmartLockDeviceDTO> syncDevices(Long integrationId) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockIntegration integration = requireIntegration(storeId, integrationId);
        SmartLockCredentialData credentials = ensureProviderToken(integration);
        SmartLockProviderClient client = providerRegistry.getClient(integration.getProvider());
        List<SmartLockProviderClient.DeviceSnapshot> snapshots = client.listDevices(credentials);
        LocalDateTime syncTime = now();
        List<SmartLockDeviceDTO> result = new ArrayList<>();
        for (SmartLockProviderClient.DeviceSnapshot snapshot : snapshots) {
            Optional<SmartLockDevice> existingDevice = deviceRepository
                    .findByStoreIdAndProviderAndProviderLockId(
                            storeId,
                            integration.getProvider(),
                            snapshot.providerLockId()
                    );
            SmartLockDevice device = existingDevice.orElseGet(SmartLockDevice::new);
            device.setStoreId(storeId);
            device.setIntegration(integration);
            device.setProvider(integration.getProvider());
            device.setProviderLockId(snapshot.providerLockId());
            device.setLockName(fallback(snapshot.lockName(), snapshot.providerLockId()));
            device.setDeviceType(snapshot.deviceType());
            device.setAuxiliaryDeviceId(snapshot.auxiliaryDeviceId());
            applySyncedStatus(device, snapshot, client, credentials, syncTime);
            device.setRawDataJson(snapshot.rawJson());
            device.setLastSyncedAt(syncTime);
            result.add(toDeviceDtoWithStatusContext(deviceRepository.save(device)));
        }
        integration.setLastSyncAt(syncTime);
        integration.setConnectionStatus(SmartLockIntegrationStatus.CONNECTED);
        integration.setLastError(null);
        integrationRepository.save(integration);
        return result;
    }

    @Transactional(readOnly = true)
    public List<SmartLockDeviceDTO> listDevices(SmartLockProvider provider) {
        Long storeId = StoreContextUtils.requireStoreId();
        List<SmartLockDevice> devices;
        if (provider == null) {
            devices = deviceRepository.findByStoreIdOrderByLockNameAsc(storeId);
        } else {
            devices = deviceRepository.findByStoreIdAndProviderOrderByLockNameAsc(storeId, provider);
        }
        return devices.stream().map(this::toDeviceDtoWithStatusContext).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SmartLockRoomDTO> listRooms(SmartLockProvider provider, Long roomTypeId) {
        Long storeId = StoreContextUtils.requireStoreId();
        List<Room> rooms;
        if (roomTypeId == null) {
            rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        } else {
            rooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomTypeId);
        }
        List<Long> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());
        Map<Long, SmartLockRoomBinding> bindingByRoomId = new HashMap<>();
        if (!roomIds.isEmpty()) {
            List<SmartLockRoomBinding> bindings = bindingRepository.findByStoreIdAndRoomIdInAndStatus(
                    storeId,
                    roomIds,
                    SmartLockBindingStatus.ACTIVE
            );
            for (SmartLockRoomBinding binding : bindings) {
                if (provider == null || provider == binding.getProvider()) {
                    bindingByRoomId.put(binding.getRoom().getId(), binding);
                }
            }
        }
        return rooms.stream()
                .map(room -> mapper.toRoomDto(room, bindingByRoomId.get(room.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public SmartLockBindingDTO createBinding(SmartLockRequests.CreateBindingRequest request) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        Room room = requireRoom(storeId, request.getRoomId());
        SmartLockDevice device = resolveDevice(storeId, request);
        SmartLockIntegration integration = device.getIntegration();
        if (request.getIntegrationId() != null && !request.getIntegrationId().equals(integration.getId())) {
            throw new IllegalArgumentException("门锁设备不属于指定集成");
        }
        validateDeviceConsistency(storeId, device, integration);
        bindingRepository.findByStoreIdAndRoomIdAndStatus(storeId, room.getId(), SmartLockBindingStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("该房间已绑定门锁");
                });
        bindingRepository.findByStoreIdAndProviderAndProviderLockIdAndStatus(
                storeId,
                device.getProvider(),
                device.getProviderLockId(),
                SmartLockBindingStatus.ACTIVE
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("该门锁已绑定其他房间");
        });

        SmartLockRoomBinding binding = new SmartLockRoomBinding();
        binding.setStoreId(storeId);
        binding.setRoom(room);
        binding.setIntegration(integration);
        binding.setDevice(device);
        binding.setProvider(device.getProvider());
        binding.setProviderLockId(device.getProviderLockId());
        binding.setStatus(SmartLockBindingStatus.ACTIVE);
        binding.setCreatedBy(userId);
        return mapper.toBindingDto(bindingRepository.save(binding));
    }

    @Transactional
    public void deleteBinding(Long bindingId) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockRoomBinding binding = requireBinding(storeId, bindingId);
        validateBindingConsistency(storeId, binding);
        ensureBindingCanBeSoftDeleted(storeId, binding);
        binding.setStatus(SmartLockBindingStatus.DELETED);
        bindingRepository.save(binding);
    }

    @Transactional(readOnly = true)
    public SmartLockStatusDTO getRoomStatus(Long roomId) {
        Long storeId = StoreContextUtils.requireStoreId();
        requireRoom(storeId, roomId);
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, null);
        validateBindingConsistency(storeId, binding);
        return mapper.toStatusDto(binding);
    }

    @Transactional
    public SmartLockStatusDTO refreshRoomStatus(Long roomId) {
        Long storeId = StoreContextUtils.requireStoreId();
        requireRoom(storeId, roomId);
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, null);
        validateBindingConsistency(storeId, binding);
        SmartLockDevice device = binding.getDevice();
        SmartLockProviderClient client = providerRegistry.getClient(binding.getProvider());
        SmartLockCredentialData credentials = ensureProviderToken(binding.getIntegration());
        StatusLookupTarget target = resolveDeviceLookupTarget(binding.getProvider(), binding.getProviderLockId(), device);
        if (target != null && device != null) {
            SmartLockProviderClient.LockStatusSnapshot snapshot = client.getStatus(credentials, target.providerLockId());
            device.setLockStatus(snapshot.lockStatus());
            device.setBattery(snapshot.battery());
            device.setOnline(snapshot.online());
            device.setRawDataJson(snapshot.rawJson());
            device.setLastStatusAt(now());
        } else if (device != null) {
            clearDeviceStatus(device);
        }
        deviceRepository.save(device);
        return mapper.toStatusDto(binding);
    }

    @Transactional
    public SmartLockConfirmationDTO createConfirmation(
            Long roomId,
            SmartLockRequests.ConfirmationRequest request
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        requireRoom(storeId, roomId);
        SmartLockTaskType action = requireLockAction(request.getAction());
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, request.getBindingId());
        validateBindingConsistency(storeId, binding);

        String token = generateConfirmationToken();
        SmartLockConfirmation confirmation = new SmartLockConfirmation();
        confirmation.setStoreId(storeId);
        confirmation.setRoom(binding.getRoom());
        confirmation.setBinding(binding);
        confirmation.setAction(action);
        confirmation.setTokenHash(credentialCrypto.sha256Hex(token));
        confirmation.setReason(SmartLockMaskingUtils.trimToNull(request.getReason()));
        confirmation.setExpiresAt(now().plusSeconds(config.getConfirmationTtlSeconds()));
        confirmation.setCreatedBy(userId);
        SmartLockConfirmation saved = confirmationRepository.save(confirmation);

        SmartLockConfirmationDTO dto = new SmartLockConfirmationDTO();
        dto.setRoomId(roomId);
        dto.setBindingId(binding.getId());
        dto.setAction(action);
        dto.setConfirmToken(token);
        dto.setExpiresAt(saved.getExpiresAt());
        return dto;
    }

    @Transactional
    public SmartLockTaskDTO unlock(Long roomId, SmartLockRequests.LockOperationRequest request) {
        return executeLockOperation(roomId, request, SmartLockTaskType.UNLOCK);
    }

    @Transactional
    public SmartLockTaskDTO lock(Long roomId, SmartLockRequests.LockOperationRequest request) {
        return executeLockOperation(roomId, request, SmartLockTaskType.LOCK);
    }

    @Transactional(readOnly = true)
    public List<SmartLockPasscodeDTO> listPasscodes(Long roomId) {
        Long storeId = StoreContextUtils.requireStoreId();
        requireRoom(storeId, roomId);
        return passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(storeId, roomId)
                .stream()
                .map(record -> mapper.toPasscodeDto(record, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public SmartLockPasscodeDTO createPasscode(
            Long roomId,
            SmartLockRequests.CreatePasscodeRequest request
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        requireRoom(storeId, roomId);
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, null);
        validateBindingConsistency(storeId, binding);
        validatePasscodeWindow(request.getValidFrom(), request.getValidUntil());
        validatePasscodeCapability(binding);
        String passcode = normalizePasscode(request.getPasscode());
        String passcodeName = fallback(SmartLockMaskingUtils.trimToNull(request.getPasscodeName()), "门锁密码");
        String requestHash = credentialCrypto.sha256Hex(
                "PASSCODE_CREATE|" + storeId + "|" + userId + "|" + roomId + "|" + binding.getId() + "|" + passcodeName
        );
        Optional<SmartLockTask> duplicate = findDuplicateTask(storeId, request.getIdempotencyKey(), requestHash);
        if (duplicate.isPresent()) {
            SmartLockTask existingTask = duplicate.get();
            if (existingTask.getPasscodeRecord() != null) {
                return mapper.toPasscodeDto(existingTask.getPasscodeRecord(), null);
            }
            throw new IllegalStateException("幂等任务缺少门锁密码记录");
        }

        SmartLockPasscodeRecord record = new SmartLockPasscodeRecord();
        record.setStoreId(storeId);
        record.setRoom(binding.getRoom());
        record.setBinding(binding);
        record.setIntegration(binding.getIntegration());
        record.setProvider(binding.getProvider());
        record.setProviderLockId(binding.getProviderLockId());
        record.setPasscodeName(passcodeName);
        record.setPasscodeMasked(SmartLockMaskingUtils.maskPasscode(passcode));
        record.setPasscodeHash(credentialCrypto.sha256Hex(storeId + "|" + roomId + "|" + passcode));
        record.setValidFrom(request.getValidFrom());
        record.setValidUntil(request.getValidUntil());
        record.setStatus(SmartLockPasscodeStatus.PENDING);
        record.setCreatedBy(userId);
        record = passcodeRepository.save(record);

        SmartLockTask task = createTask(
                storeId,
                userId,
                SmartLockTaskType.CREATE_PASSCODE,
                binding,
                record,
                null,
                request.getIdempotencyKey(),
                requestHash,
                null
        );
        SmartLockCredentialData credentials = ensureProviderToken(binding.getIntegration());
        try {
            SmartLockProviderClient.PasscodeCommand command = new SmartLockProviderClient.PasscodeCommand(
                    passcodeName,
                    passcode,
                    request.getValidFrom(),
                    request.getValidUntil()
            );
            SmartLockProviderClient.ProviderTaskResult result = providerRegistry
                    .getClient(binding.getProvider())
                    .createPasscode(credentials, binding.getProviderLockId(), command);
            completeTask(task, result);
            record.setProviderTaskId(result.providerTaskId());
            record.setProviderPasscodeId(result.providerPasscodeId());
            if (result.status() == SmartLockTaskStatus.SUCCESS) {
                record.setStatus(SmartLockPasscodeStatus.ACTIVE);
            } else if (result.status() == SmartLockTaskStatus.FAILED) {
                record.setStatus(SmartLockPasscodeStatus.FAILED);
                record.setLastError(safeProviderMessage(result.message()));
            } else {
                record.setStatus(SmartLockPasscodeStatus.PENDING);
                record.setLastError(null);
            }
            passcodeRepository.save(record);
            String oneTimePasscode = result.status() == SmartLockTaskStatus.SUCCESS ? passcode : null;
            return mapper.toPasscodeDto(record, oneTimePasscode);
        } catch (RuntimeException ex) {
            failTask(task, ex);
            record.setStatus(SmartLockPasscodeStatus.FAILED);
            record.setLastError(safeError(ex));
            passcodeRepository.save(record);
            return mapper.toPasscodeDto(record, null);
        }
    }

    @Transactional
    public SmartLockPasscodeDTO deletePasscode(Long recordId) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        SmartLockPasscodeRecord record = passcodeRepository.findByStoreIdAndId(storeId, recordId)
                .orElseThrow(() -> new IllegalArgumentException("门锁密码记录不存在"));
        if (record.getStatus() == SmartLockPasscodeStatus.DELETED) {
            return mapper.toPasscodeDto(record, null);
        }
        SmartLockRoomBinding binding = requireCurrentActiveBindingForPasscode(storeId, record);
        validateBindingConsistency(storeId, binding);
        validatePasscodeRecordConsistency(record, binding);
        validatePasscodeCapability(binding);
        if (record.getStatus() == SmartLockPasscodeStatus.DELETE_PENDING) {
            return mapper.toPasscodeDto(record, null);
        }
        if (!hasText(record.getProviderPasscodeId())) {
            throw new IllegalArgumentException("缺少供应商密码 ID，无法删除");
        }
        record.setStatus(SmartLockPasscodeStatus.DELETE_PENDING);
        passcodeRepository.save(record);
        SmartLockTask task = createTask(
                storeId,
                userId,
                SmartLockTaskType.DELETE_PASSCODE,
                binding,
                record,
                null,
                null,
                credentialCrypto.sha256Hex("PASSCODE_DELETE|" + storeId + "|" + recordId),
                null
        );
        SmartLockCredentialData credentials = ensureProviderToken(binding.getIntegration());
        try {
            SmartLockProviderClient.ProviderTaskResult result = providerRegistry
                    .getClient(binding.getProvider())
                    .deletePasscode(credentials, binding.getProviderLockId(), record.getProviderPasscodeId());
            completeTask(task, result);
            applyTaskResultToPasscodeRecord(task, result);
            return mapper.toPasscodeDto(record, null);
        } catch (RuntimeException ex) {
            failTask(task, ex);
            record.setStatus(SmartLockPasscodeStatus.FAILED);
            record.setLastError(safeError(ex));
            passcodeRepository.save(record);
            return mapper.toPasscodeDto(record, null);
        }
    }

    @Transactional
    public SmartLockTaskDTO getTask(Long taskId) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockTask task = taskRepository.findByStoreIdAndId(storeId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("门锁任务不存在"));
        if (task.getStatus() == SmartLockTaskStatus.PENDING && hasText(task.getProviderTaskId())) {
            refreshPendingTask(task);
        }
        return mapper.toTaskDto(task);
    }

    @Transactional
    public Map<String, Object> handleSwitchBotWebhook(String token, Map<String, Object> payload) {
        validateSwitchBotWebhookToken(token);
        JsonNode root = objectMapper.valueToTree(payload != null ? payload : Map.of());
        String commandId = findFirstText(root, "commandId", "taskId", "providerTaskId");
        String eventName = findFirstText(root, "eventName", "eventType", "event", "command");
        SmartLockTaskStatus status = resolveSwitchBotWebhookStatus(root);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", SmartLockProvider.SWITCHBOT.name());
        result.put("eventName", eventName);
        result.put("commandId", commandId);

        if (!hasText(commandId)) {
            result.put("processed", false);
            result.put("reason", "missing_command_id");
            return result;
        }
        if (status == null) {
            result.put("processed", false);
            result.put("reason", "unknown_result");
            return result;
        }

        List<SmartLockTask> tasks = taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                commandId
        );
        if (tasks.isEmpty()) {
            result.put("processed", false);
            result.put("reason", "task_not_found");
            return result;
        }
        if (tasks.size() > 1) {
            result.put("processed", false);
            result.put("reason", "ambiguous_command_id");
            return result;
        }

        SmartLockTask task = tasks.get(0);
        String providerPasscodeId = findFirstText(root, "providerPasscodeId", "passcodeId", "keyId");
        String message = fallback(
                findFirstText(root, "message", "resultMessage", "errorMessage", "error"),
                "SwitchBot webhook " + status.name()
        );
        SmartLockProviderClient.ProviderTaskResult providerResult =
                new SmartLockProviderClient.ProviderTaskResult(status, commandId, providerPasscodeId, message);
        completeTask(task, providerResult);
        applyTaskResultToPasscodeRecord(task, providerResult);

        result.put("processed", true);
        result.put("taskId", task.getId());
        result.put("taskStatus", task.getStatus().name());
        if (task.getPasscodeRecord() != null) {
            result.put("passcodeRecordId", task.getPasscodeRecord().getId());
            result.put("passcodeStatus", task.getPasscodeRecord().getStatus().name());
        }
        return result;
    }

    private SmartLockTaskDTO executeLockOperation(
            Long roomId,
            SmartLockRequests.LockOperationRequest request,
            SmartLockTaskType action
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireUserId();
        requireRoom(storeId, roomId);
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, request.getBindingId());
        validateBindingConsistency(storeId, binding);
        String requestHash = credentialCrypto.sha256Hex(
                "LOCK_OP|" + action + "|" + storeId + "|" + userId + "|" + roomId + "|" + binding.getId()
        );
        Optional<SmartLockTask> duplicate = findDuplicateTask(storeId, request.getIdempotencyKey(), requestHash);
        if (duplicate.isPresent()) {
            return mapper.toTaskDto(duplicate.get());
        }

        StatusLookupTarget commandTarget = resolveLockCommandTarget(binding);
        SmartLockConfirmation confirmation = consumeConfirmation(storeId, userId, roomId, binding, action, request);
        SmartLockTask task = createTask(
                storeId,
                userId,
                action,
                binding,
                null,
                confirmation,
                request.getIdempotencyKey(),
                requestHash,
                request.getReason()
        );
        SmartLockCredentialData credentials = ensureProviderToken(binding.getIntegration());
        try {
            SmartLockProviderClient.ProviderTaskResult result;
            if (action == SmartLockTaskType.UNLOCK) {
                result = providerRegistry.getClient(binding.getProvider()).unlock(
                        credentials,
                        commandTarget.providerLockId()
                );
            } else {
                result = providerRegistry.getClient(binding.getProvider()).lock(
                        credentials,
                        commandTarget.providerLockId()
                );
            }
            completeTask(task, result);
        } catch (RuntimeException ex) {
            failTask(task, ex);
        }
        return mapper.toTaskDto(task);
    }

    private void applyIntegrationRequest(
            SmartLockIntegration integration,
            SmartLockRequests.UpsertIntegrationRequest request
    ) {
        SmartLockProvider provider = integration.getProvider();
        SmartLockCredentialData existing = null;
        if (hasText(integration.getCredentialCiphertext())) {
            existing = decryptCredentials(integration);
        }
        SmartLockCredentialData credentials = SmartLockCredentialData.fromRequest(provider, request, existing);
        integration.setName(resolveIntegrationName(provider, request.getName(), integration.getName()));
        if (request.getEnabled() != null) {
            integration.setEnabled(request.getEnabled());
        } else if (integration.getEnabled() == null) {
            integration.setEnabled(true);
        }
        persistCredentials(integration, credentials);
        if (integration.getConnectionStatus() == null) {
            integration.setConnectionStatus(SmartLockIntegrationStatus.DISCONNECTED);
        }
    }

    private void persistCredentials(SmartLockIntegration integration, SmartLockCredentialData credentials) {
        try {
            String json = objectMapper.writeValueAsString(credentials);
            integration.setCredentialCiphertext(credentialCrypto.encrypt(json));
            integration.setCredentialFingerprint(credentials.fingerprint(credentialCrypto));
            integration.setTokenExpiresAt(credentials.getTtLockTokenExpiresAt());
        } catch (Exception ex) {
            throw new IllegalStateException("门锁凭证序列化失败", ex);
        }
    }

    private SmartLockCredentialData decryptCredentials(SmartLockIntegration integration) {
        try {
            String json = credentialCrypto.decrypt(integration.getCredentialCiphertext());
            SmartLockCredentialData data = objectMapper.readValue(json, SmartLockCredentialData.class);
            data.setProvider(integration.getProvider());
            return data;
        } catch (Exception ex) {
            throw new IllegalStateException("门锁凭证读取失败", ex);
        }
    }

    private SmartLockCredentialData ensureProviderToken(SmartLockIntegration integration) {
        SmartLockCredentialData credentials = decryptCredentials(integration);
        if (credentials.getProvider() == SmartLockProvider.TTLOCK && credentials.shouldRefreshTtLockToken(now())) {
            SmartLockProviderClient client = providerRegistry.getClient(SmartLockProvider.TTLOCK);
            credentials = client.refreshToken(credentials);
            persistCredentials(integration, credentials);
            integrationRepository.save(integration);
        }
        return credentials;
    }

    private SmartLockDevice resolveDevice(Long storeId, SmartLockRequests.CreateBindingRequest request) {
        if (request.getDeviceId() != null) {
            return deviceRepository.findByStoreIdAndId(storeId, request.getDeviceId())
                    .orElseThrow(() -> new IllegalArgumentException("门锁设备不存在"));
        }
        SmartLockProvider provider = requireProvider(request.getProvider());
        String providerLockId = SmartLockMaskingUtils.trimToNull(request.getProviderLockId());
        if (!hasText(providerLockId)) {
            throw new IllegalArgumentException("providerLockId 不能为空");
        }
        return deviceRepository.findByStoreIdAndProviderAndProviderLockId(storeId, provider, providerLockId)
                .orElseThrow(() -> new IllegalArgumentException("门锁设备不存在，请先同步设备"));
    }

    private SmartLockConfirmation consumeConfirmation(
            Long storeId,
            Long userId,
            Long roomId,
            SmartLockRoomBinding binding,
            SmartLockTaskType action,
            SmartLockRequests.LockOperationRequest request
    ) {
        if (!Boolean.TRUE.equals(request.getConfirm())) {
            throw new IllegalArgumentException("远程门锁操作需要二次确认");
        }
        String token = SmartLockMaskingUtils.trimToNull(request.getConfirmToken());
        if (!hasText(token)) {
            throw new IllegalArgumentException("缺少二次确认 token");
        }
        String tokenHash = credentialCrypto.sha256Hex(token);
        SmartLockConfirmation confirmation = confirmationRepository.findByStoreIdAndTokenHash(storeId, tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("二次确认 token 无效"));
        if (!userId.equals(confirmation.getCreatedBy())) {
            throw new IllegalArgumentException("二次确认 token 与当前用户不匹配");
        }
        if (confirmation.getUsedAt() != null) {
            throw new IllegalArgumentException("二次确认 token 已使用");
        }
        if (!confirmation.getExpiresAt().isAfter(now())) {
            throw new IllegalArgumentException("二次确认 token 已过期");
        }
        if (!roomId.equals(confirmation.getRoom().getId())) {
            throw new IllegalArgumentException("二次确认 token 与房间不匹配");
        }
        if (!binding.getId().equals(confirmation.getBinding().getId())) {
            throw new IllegalArgumentException("二次确认 token 与门锁绑定不匹配");
        }
        if (confirmation.getAction() != action) {
            throw new IllegalArgumentException("二次确认 token 与操作不匹配");
        }
        confirmation.setUsedAt(now());
        return confirmationRepository.save(confirmation);
    }

    private Optional<SmartLockTask> findDuplicateTask(Long storeId, String idempotencyKey, String requestHash) {
        String normalizedKey = SmartLockMaskingUtils.trimToNull(idempotencyKey);
        if (!hasText(normalizedKey)) {
            return Optional.empty();
        }
        Optional<SmartLockTask> existing = taskRepository.findByStoreIdAndIdempotencyKey(storeId, normalizedKey);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        String existingHash = existing.get().getRequestHash();
        if (hasText(existingHash) && !existingHash.equals(requestHash)) {
            throw new IllegalArgumentException("幂等键已用于其他门锁请求");
        }
        return existing;
    }

    private SmartLockTask createTask(
            Long storeId,
            Long userId,
            SmartLockTaskType taskType,
            SmartLockRoomBinding binding,
            SmartLockPasscodeRecord passcodeRecord,
            SmartLockConfirmation confirmation,
            String idempotencyKey,
            String requestHash,
            String reason
    ) {
        SmartLockTask task = new SmartLockTask();
        task.setStoreId(storeId);
        task.setCreatedBy(userId);
        task.setTaskType(taskType);
        task.setProvider(binding.getProvider());
        task.setRoom(binding.getRoom());
        task.setBinding(binding);
        task.setPasscodeRecord(passcodeRecord);
        task.setConfirmation(confirmation);
        task.setIdempotencyKey(SmartLockMaskingUtils.trimToNull(idempotencyKey));
        task.setRequestHash(requestHash);
        task.setReason(SmartLockMaskingUtils.trimToNull(reason));
        task.setStatus(SmartLockTaskStatus.PENDING);
        return taskRepository.save(task);
    }

    private void completeTask(SmartLockTask task, SmartLockProviderClient.ProviderTaskResult result) {
        task.setStatus(result.status());
        if (hasText(result.providerTaskId())) {
            task.setProviderTaskId(result.providerTaskId());
        }
        task.setResultMessage(safeProviderMessage(result.message()));
        if (result.status() == SmartLockTaskStatus.PENDING) {
            task.setCompletedAt(null);
        } else {
            task.setCompletedAt(now());
        }
        taskRepository.save(task);
    }

    private void failTask(SmartLockTask task, RuntimeException ex) {
        task.setStatus(SmartLockTaskStatus.FAILED);
        task.setErrorMessage(safeError(ex));
        task.setCompletedAt(now());
        taskRepository.save(task);
    }

    private void refreshPendingTask(SmartLockTask task) {
        SmartLockRoomBinding binding = task.getBinding();
        if (binding == null) {
            return;
        }
        validateBindingConsistency(task.getStoreId(), binding);
        try {
            SmartLockCredentialData credentials = ensureProviderToken(binding.getIntegration());
            SmartLockProviderClient.ProviderTaskResult result = providerRegistry
                    .getClient(binding.getProvider())
                    .queryTask(credentials, task.getProviderTaskId());
            completeTask(task, result);
            applyTaskResultToPasscodeRecord(task, result);
        } catch (RuntimeException ex) {
            failTask(task, ex);
        }
    }

    private void applyTaskResultToPasscodeRecord(
            SmartLockTask task,
            SmartLockProviderClient.ProviderTaskResult result
    ) {
        SmartLockPasscodeRecord record = task.getPasscodeRecord();
        if (record == null) {
            return;
        }
        if (hasText(result.providerTaskId())) {
            record.setProviderTaskId(result.providerTaskId());
        } else if (hasText(task.getProviderTaskId())) {
            record.setProviderTaskId(task.getProviderTaskId());
        }
        if (hasText(result.providerPasscodeId())) {
            record.setProviderPasscodeId(result.providerPasscodeId());
        }

        if (task.getTaskType() == SmartLockTaskType.CREATE_PASSCODE) {
            applyCreatePasscodeResult(record, result);
        } else if (task.getTaskType() == SmartLockTaskType.DELETE_PASSCODE) {
            applyDeletePasscodeResult(record, result);
        }
        passcodeRepository.save(record);
    }

    private void applyCreatePasscodeResult(
            SmartLockPasscodeRecord record,
            SmartLockProviderClient.ProviderTaskResult result
    ) {
        if (result.status() == SmartLockTaskStatus.SUCCESS) {
            record.setStatus(SmartLockPasscodeStatus.ACTIVE);
            record.setLastError(null);
        } else if (result.status() == SmartLockTaskStatus.FAILED) {
            record.setStatus(SmartLockPasscodeStatus.FAILED);
            record.setLastError(safeProviderMessage(result.message()));
        } else {
            record.setStatus(SmartLockPasscodeStatus.PENDING);
            record.setLastError(null);
        }
    }

    private void applyDeletePasscodeResult(
            SmartLockPasscodeRecord record,
            SmartLockProviderClient.ProviderTaskResult result
    ) {
        if (result.status() == SmartLockTaskStatus.SUCCESS) {
            record.setStatus(SmartLockPasscodeStatus.DELETED);
            record.setDeletedAt(now());
            record.setLastError(null);
        } else if (result.status() == SmartLockTaskStatus.FAILED) {
            record.setStatus(SmartLockPasscodeStatus.FAILED);
            record.setLastError(safeProviderMessage(result.message()));
        } else {
            record.setStatus(SmartLockPasscodeStatus.DELETE_PENDING);
            record.setLastError(null);
        }
    }

    private void applySyncedStatus(
            SmartLockDevice device,
            SmartLockProviderClient.DeviceSnapshot snapshot,
            SmartLockProviderClient client,
            SmartLockCredentialData credentials,
            LocalDateTime syncTime
    ) {
        if (device.getProvider() != SmartLockProvider.SWITCHBOT) {
            device.setBattery(snapshot.battery());
            device.setLockStatus(snapshot.lockStatus());
            device.setOnline(snapshot.online());
            return;
        }

        StatusLookupTarget target = resolveStatusLookupTarget(
                device.getProvider(),
                snapshot.providerLockId(),
                snapshot.deviceType(),
                snapshot.auxiliaryDeviceId()
        );
        if (target == null) {
            clearDeviceStatus(device);
            return;
        }

        try {
            SmartLockProviderClient.LockStatusSnapshot status = client.getStatus(credentials, target.providerLockId());
            device.setBattery(status.battery());
            device.setLockStatus(status.lockStatus());
            device.setOnline(status.online());
            device.setLastStatusAt(syncTime);
        } catch (RuntimeException ex) {
            clearDeviceStatus(device);
        }
    }

    private SmartLockDeviceDTO toDeviceDtoWithStatusContext(SmartLockDevice device) {
        SmartLockDeviceDTO dto = mapper.toDeviceDto(device);
        StatusLookupTarget target = resolveDeviceLookupTarget(device.getProvider(), device.getProviderLockId(), device);
        if (target == null) {
            dto.setStatusSource(STATUS_SOURCE_UNAVAILABLE);
            dto.setStatusSourceDeviceId(null);
            return dto;
        }

        dto.setStatusSource(target.source());
        dto.setStatusSourceDeviceId(target.providerLockId());
        return dto;
    }

    private StatusLookupTarget resolveStatusLookupTarget(
            SmartLockProvider provider,
            String providerLockId,
            String deviceType,
            String auxiliaryDeviceId
    ) {
        if (!hasText(providerLockId)) {
            return null;
        }

        if (provider != SmartLockProvider.SWITCHBOT) {
            return new StatusLookupTarget(providerLockId, STATUS_SOURCE_DEVICE);
        }

        if (!isSwitchBotAuthenticationPanel(deviceType)) {
            return new StatusLookupTarget(providerLockId, STATUS_SOURCE_DEVICE);
        }

        String boundLockDeviceId = SmartLockMaskingUtils.trimToNull(auxiliaryDeviceId);
        if (!hasText(boundLockDeviceId)) {
            return null;
        }
        return new StatusLookupTarget(boundLockDeviceId, STATUS_SOURCE_BOUND_LOCK);
    }

    private StatusLookupTarget resolveLockCommandTarget(SmartLockRoomBinding binding) {
        SmartLockDevice device = binding.getDevice();
        StatusLookupTarget target = resolveDeviceLookupTarget(binding.getProvider(), binding.getProviderLockId(), device);
        if (target != null) {
            return target;
        }

        if (binding.getProvider() == SmartLockProvider.SWITCHBOT
                && isSwitchBotAuthenticationPanel(device != null ? device.getDeviceType() : null)) {
            throw new IllegalArgumentException(
                    "SwitchBot 键盘设备未返回绑定的真实门锁 lockDeviceId，请在 SwitchBot App 配对 Lock 后重新同步设备，或直接绑定真实 Lock"
            );
        }

        throw new IllegalArgumentException("门锁命令目标不可用，请重新同步设备后再试");
    }

    private StatusLookupTarget resolveDeviceLookupTarget(
            SmartLockProvider provider,
            String providerLockId,
            SmartLockDevice device
    ) {
        String deviceType = device != null ? device.getDeviceType() : null;
        if (provider != SmartLockProvider.SWITCHBOT || !isSwitchBotAuthenticationPanel(deviceType)) {
            return resolveStatusLookupTarget(
                    provider,
                    providerLockId,
                    deviceType,
                    device != null ? device.getAuxiliaryDeviceId() : null
            );
        }

        String boundLockDeviceId = resolveSwitchBotBoundLockDeviceId(device);
        if (!hasText(boundLockDeviceId)) {
            return null;
        }
        return new StatusLookupTarget(boundLockDeviceId, STATUS_SOURCE_BOUND_LOCK);
    }

    private String resolveSwitchBotBoundLockDeviceId(SmartLockDevice device) {
        if (device == null) {
            return null;
        }

        String rawLockDeviceId = readRawDeviceField(device, "lockDeviceId");
        if (hasText(rawLockDeviceId)) {
            return rawLockDeviceId;
        }

        String auxiliaryDeviceId = SmartLockMaskingUtils.trimToNull(device.getAuxiliaryDeviceId());
        String rawHubDeviceId = readRawDeviceField(device, "hubDeviceId");
        if (hasText(rawHubDeviceId) && rawHubDeviceId.equals(auxiliaryDeviceId)) {
            return null;
        }
        return auxiliaryDeviceId;
    }

    private String readRawDeviceField(SmartLockDevice device, String fieldName) {
        if (device == null || !hasText(device.getRawDataJson())) {
            return null;
        }
        try {
            return SmartLockMaskingUtils.trimToNull(
                    objectMapper.readTree(device.getRawDataJson()).path(fieldName).asText(null)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private void clearDeviceStatus(SmartLockDevice device) {
        device.setBattery(null);
        device.setLockStatus(null);
        device.setOnline(null);
        device.setLastStatusAt(null);
    }

    private SmartLockIntegration requireIntegration(Long storeId, Long integrationId) {
        if (integrationId == null) {
            throw new IllegalArgumentException("integrationId 不能为空");
        }
        return integrationRepository.findByStoreIdAndId(storeId, integrationId)
                .orElseThrow(() -> new IllegalArgumentException("门锁集成不存在"));
    }

    private Room requireRoom(Long storeId, Long roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId 不能为空");
        }
        return roomRepository.findByStoreIdAndId(storeId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在或无权访问"));
    }

    private SmartLockRoomBinding requireBinding(Long storeId, Long bindingId) {
        if (bindingId == null) {
            throw new IllegalArgumentException("bindingId 不能为空");
        }
        return bindingRepository.findByStoreIdAndIdAndStatus(storeId, bindingId, SmartLockBindingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("门锁绑定不存在"));
    }

    private SmartLockRoomBinding requireBindingForRoom(Long storeId, Long roomId, Long bindingId) {
        SmartLockRoomBinding binding;
        if (bindingId == null) {
            binding = bindingRepository.findByStoreIdAndRoomIdAndStatus(
                            storeId,
                            roomId,
                            SmartLockBindingStatus.ACTIVE
                    )
                    .orElseThrow(() -> new IllegalArgumentException("该房间未绑定门锁"));
        } else {
            binding = requireBinding(storeId, bindingId);
        }
        if (binding.getRoom() == null || !roomId.equals(binding.getRoom().getId())) {
            throw new IllegalArgumentException("门锁绑定与房间不匹配");
        }
        return binding;
    }

    private void validateBindingConsistency(Long storeId, SmartLockRoomBinding binding) {
        if (binding == null || !storeId.equals(binding.getStoreId())) {
            throw new IllegalArgumentException("门锁绑定不存在");
        }
        if (binding.getStatus() != SmartLockBindingStatus.ACTIVE) {
            throw new IllegalArgumentException("门锁绑定已解绑");
        }
        if (binding.getRoom() == null || !storeId.equals(binding.getRoom().getStoreId())) {
            throw new IllegalArgumentException("门锁绑定与房间门店不一致");
        }
        validateDeviceConsistency(storeId, binding.getDevice(), binding.getIntegration());
        if (binding.getProvider() != binding.getDevice().getProvider()) {
            throw new IllegalArgumentException("门锁绑定与设备服务商不一致");
        }
        if (!binding.getProviderLockId().equals(binding.getDevice().getProviderLockId())) {
            throw new IllegalArgumentException("门锁绑定与设备 ID 不一致");
        }
    }

    private void ensureBindingCanBeSoftDeleted(Long storeId, SmartLockRoomBinding binding) {
        boolean hasRiskyPasscode = passcodeRepository.existsRiskyStatusForBinding(
                storeId,
                binding.getId(),
                BINDING_DELETE_RISKY_PASSCODE_STATUSES
        );
        if (hasRiskyPasscode) {
            throw new IllegalArgumentException("该门锁仍有有效或处理中的密码，请先完成清理后再解绑");
        }
        boolean hasPendingTask = taskRepository.existsByBindingAndStatus(
                storeId,
                binding.getId(),
                SmartLockTaskStatus.PENDING
        );
        if (hasPendingTask) {
            throw new IllegalArgumentException("该门锁仍有未完成任务，请等待任务完成后再解绑");
        }
        boolean hasPendingConfirmation = confirmationRepository.existsUnfinishedForBinding(
                storeId,
                binding.getId(),
                now()
        );
        if (hasPendingConfirmation) {
            throw new IllegalArgumentException("该门锁仍有未完成的二次确认，请稍后再解绑");
        }
    }

    private SmartLockRoomBinding requireCurrentActiveBindingForPasscode(
            Long storeId,
            SmartLockPasscodeRecord record
    ) {
        if (record.getRoom() == null || record.getRoom().getId() == null) {
            throw new IllegalArgumentException("门锁密码记录缺少房间快照");
        }
        return bindingRepository.findByStoreIdAndRoomIdAndStatus(
                        storeId,
                        record.getRoom().getId(),
                        SmartLockBindingStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException("当前房间没有可用的门锁绑定，不能删除远程密码"));
    }

    private void validatePasscodeRecordConsistency(
            SmartLockPasscodeRecord record,
            SmartLockRoomBinding binding
    ) {
        if (record.getBinding() == null || !binding.getId().equals(record.getBinding().getId())) {
            throw new IllegalArgumentException("门锁密码记录与当前绑定不一致，已拒绝删除远程密码");
        }
        if (record.getRoom() == null || binding.getRoom() == null
                || !binding.getRoom().getId().equals(record.getRoom().getId())) {
            throw new IllegalArgumentException("门锁密码记录与当前房间不一致，已拒绝删除远程密码");
        }
        if (record.getIntegration() == null || binding.getIntegration() == null
                || !binding.getIntegration().getId().equals(record.getIntegration().getId())) {
            throw new IllegalArgumentException("门锁密码记录与当前集成不一致，已拒绝删除远程密码");
        }
        if (record.getProvider() != binding.getProvider()) {
            throw new IllegalArgumentException("门锁密码记录与当前服务商不一致，已拒绝删除远程密码");
        }
        if (!binding.getProviderLockId().equals(record.getProviderLockId())) {
            throw new IllegalArgumentException("门锁密码记录与当前门锁设备不一致，已拒绝删除远程密码");
        }
        SmartLockDevice device = binding.getDevice();
        if (device == null || device.getIntegration() == null
                || !binding.getIntegration().getId().equals(device.getIntegration().getId())) {
            throw new IllegalArgumentException("当前门锁设备快照不完整，已拒绝删除远程密码");
        }
        if (device.getProvider() != record.getProvider()
                || !record.getProviderLockId().equals(device.getProviderLockId())) {
            throw new IllegalArgumentException("门锁密码记录与当前设备快照不一致，已拒绝删除远程密码");
        }
    }

    private void validateDeviceConsistency(
            Long storeId,
            SmartLockDevice device,
            SmartLockIntegration integration
    ) {
        if (device == null || !storeId.equals(device.getStoreId())) {
            throw new IllegalArgumentException("门锁设备不存在");
        }
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException("门锁集成不存在");
        }
        if (device.getIntegration() == null || !integration.getId().equals(device.getIntegration().getId())) {
            throw new IllegalArgumentException("门锁设备与集成不一致");
        }
        if (device.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException("门锁设备与集成服务商不一致");
        }
    }

    private SmartLockProvider requireProvider(SmartLockProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider 不能为空");
        }
        return provider;
    }

    private SmartLockTaskType requireLockAction(SmartLockTaskType action) {
        if (action == SmartLockTaskType.LOCK || action == SmartLockTaskType.UNLOCK) {
            return action;
        }
        throw new IllegalArgumentException("只支持 LOCK 或 UNLOCK 二次确认");
    }

    private String normalizePasscode(String requested) {
        String passcode = SmartLockMaskingUtils.trimToNull(requested);
        if (passcode == null) {
            return String.valueOf(GENERATED_PASSCODE_MIN + SECURE_RANDOM.nextInt(GENERATED_PASSCODE_BOUND));
        }
        if (!passcode.matches("\\d{" + MIN_PASSCODE_LENGTH + "," + MAX_PASSCODE_LENGTH + "}")) {
            throw new IllegalArgumentException("门锁密码必须是 4 到 12 位数字");
        }
        return passcode;
    }

    private void validatePasscodeWindow(LocalDateTime validFrom, LocalDateTime validUntil) {
        if (validFrom == null || validUntil == null) {
            throw new IllegalArgumentException("门锁密码必须提供有效开始和结束时间");
        }
        if (!validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("门锁密码结束时间必须晚于开始时间");
        }
    }

    private void validatePasscodeCapability(SmartLockRoomBinding binding) {
        if (binding.getProvider() != SmartLockProvider.SWITCHBOT) {
            return;
        }
        SmartLockDevice device = binding.getDevice();
        String deviceType = device != null ? device.getDeviceType() : null;
        for (String supportedType : SWITCHBOT_PASSCODE_DEVICE_TYPES) {
            if (supportedType.equalsIgnoreCase(deviceType)) {
                return;
            }
        }
        throw new IllegalArgumentException("SwitchBot 普通门锁不支持直接创建或删除密码，请绑定 Keypad 或 Lock Vision 设备");
    }

    private static boolean isSwitchBotAuthenticationPanel(String deviceType) {
        if (!hasText(deviceType)) {
            return false;
        }
        for (String supportedType : SWITCHBOT_AUTHENTICATION_PANEL_TYPES) {
            if (supportedType.equalsIgnoreCase(deviceType)) {
                return true;
            }
        }
        return false;
    }

    private void validateSwitchBotWebhookToken(String token) {
        String expectedToken = config.getSwitchBotWebhookToken();
        if (!hasText(expectedToken)) {
            throw new IllegalStateException("SwitchBot webhook token 未配置");
        }
        String actualToken = SmartLockMaskingUtils.trimToNull(token);
        if (!hasText(actualToken) || !constantTimeEquals(expectedToken, actualToken)) {
            throw new IllegalArgumentException("SwitchBot webhook token 无效");
        }
    }

    private SmartLockTaskStatus resolveSwitchBotWebhookStatus(JsonNode root) {
        Boolean success = findFirstBoolean(root, "success", "isSuccess", "succeeded");
        if (success != null) {
            return success ? SmartLockTaskStatus.SUCCESS : SmartLockTaskStatus.FAILED;
        }

        String status = findFirstText(root, "result", "status", "commandStatus", "state");
        if (!hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("failed")
                || normalized.equals("failure")
                || normalized.equals("fail")
                || normalized.equals("error")
                || normalized.equals("false")
                || normalized.equals("0")
                || normalized.equals("timeout")
                || normalized.equals("rejected")
                || normalized.equals("denied")
                || normalized.contains("unsuccess")
                || normalized.contains("fail")
                || normalized.contains("error")) {
            return SmartLockTaskStatus.FAILED;
        }
        if (normalized.equals("success")
                || normalized.equals("succeeded")
                || normalized.equals("complete")
                || normalized.equals("completed")
                || normalized.equals("done")
                || normalized.equals("ok")
                || normalized.equals("true")
                || normalized.equals("1")
                || normalized.contains("success")) {
            return SmartLockTaskStatus.SUCCESS;
        }
        if (normalized.equals("pending") || normalized.equals("processing")) {
            return SmartLockTaskStatus.PENDING;
        }
        return null;
    }

    private String findFirstText(JsonNode root, String... fieldNames) {
        if (root == null || root.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            String value = findTextByFieldName(root, fieldName);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String findTextByFieldName(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();
                if (entry.getKey().equalsIgnoreCase(fieldName)
                        && !value.isContainerNode()
                        && !value.isNull()) {
                    return value.asText();
                }
                String nested = findTextByFieldName(value, fieldName);
                if (hasText(nested)) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = findTextByFieldName(child, fieldName);
                if (hasText(nested)) {
                    return nested;
                }
            }
        }
        return null;
    }

    private Boolean findFirstBoolean(JsonNode root, String... fieldNames) {
        if (root == null || root.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            Boolean value = findBooleanByFieldName(root, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Boolean findBooleanByFieldName(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();
                if (entry.getKey().equalsIgnoreCase(fieldName) && value.isBoolean()) {
                    return value.asBoolean();
                }
                Boolean nested = findBooleanByFieldName(value, fieldName);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                Boolean nested = findBooleanByFieldName(child, fieldName);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String generateConfirmationToken() {
        byte[] bytes = new byte[CONFIRM_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resolveIntegrationName(SmartLockProvider provider, String requestedName, String existingName) {
        String name = SmartLockMaskingUtils.trimToNull(requestedName);
        if (hasText(name)) {
            return name;
        }
        if (hasText(existingName)) {
            return existingName;
        }
        if (provider == SmartLockProvider.SWITCHBOT) {
            return "SwitchBot";
        }
        if (provider == SmartLockProvider.TTLOCK) {
            return "TTLock";
        }
        return "Smart Lock";
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String safeError(RuntimeException ex) {
        return SmartLockMaskingUtils.safeExceptionMessage(ex);
    }

    private String safeProviderMessage(String message) {
        return SmartLockMaskingUtils.redactSensitiveMessage(message);
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

    private record StatusLookupTarget(String providerLockId, String source) {
    }
}
