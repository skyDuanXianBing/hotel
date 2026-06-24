package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import server.demo.entity.Store;
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
import server.demo.repository.StoreRepository;
import server.demo.util.SmartLockCredentialCrypto;
import server.demo.util.SmartLockMaskingUtils;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private static final Logger logger = LoggerFactory.getLogger(SmartLockService.class);
    private static final int CONFIRM_TOKEN_BYTES = 32;
    private static final int GENERATED_PASSCODE_MIN = 100000;
    private static final int GENERATED_PASSCODE_BOUND = 900000;
    private static final int MIN_PASSCODE_LENGTH = 4;
    private static final int MAX_PASSCODE_LENGTH = 12;
    private static final String SWITCHBOT_NO_ID_CREATE_PENDING_MESSAGE =
            "SwitchBot 已返回 success 但未返回 commandId 或 keyId，等待 keyList 同步";
    private static final String LOCAL_NO_REMOTE_PASSCODE_CLEANUP_MESSAGE =
            "本地密码记录已清理，未调用供应商删除";
    private static final String MISSING_PROVIDER_PASSCODE_ID_DELETE_MESSAGE =
            "该门锁密码尚未取得供应商密码 ID，已拒绝远程删除；请等待同步完成后再试";
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
    private final SmartLockIntegrationRepository integrationRepository;
    private final SmartLockDeviceRepository deviceRepository;
    private final SmartLockRoomBindingRepository bindingRepository;
    private final SmartLockConfirmationRepository confirmationRepository;
    private final SmartLockPasscodeRecordRepository passcodeRepository;
    private final SmartLockTaskRepository taskRepository;
    private final RoomRepository roomRepository;
    private final StoreRepository storeRepository;
    private final SmartLockProviderClientRegistry providerRegistry;
    private final SmartLockCredentialCrypto credentialCrypto;
    private final SmartLockMapper mapper;
    private final SmartLockConfig config;
    private final ObjectMapper objectMapper;
    private final SmartLockDeviceRoleResolver roleResolver;
    private final SmartLockPasscodeReconciliationService passcodeReconciliationService;
    private final Clock clock;

    public SmartLockService(
            SmartLockIntegrationRepository integrationRepository,
            SmartLockDeviceRepository deviceRepository,
            SmartLockRoomBindingRepository bindingRepository,
            SmartLockConfirmationRepository confirmationRepository,
            SmartLockPasscodeRecordRepository passcodeRepository,
            SmartLockTaskRepository taskRepository,
            RoomRepository roomRepository,
            StoreRepository storeRepository,
            SmartLockProviderClientRegistry providerRegistry,
            SmartLockCredentialCrypto credentialCrypto,
            SmartLockMapper mapper,
            SmartLockConfig config,
            ObjectMapper objectMapper,
            SmartLockPasscodeReconciliationService passcodeReconciliationService,
            Clock clock
    ) {
        this.integrationRepository = integrationRepository;
        this.deviceRepository = deviceRepository;
        this.bindingRepository = bindingRepository;
        this.confirmationRepository = confirmationRepository;
        this.passcodeRepository = passcodeRepository;
        this.taskRepository = taskRepository;
        this.roomRepository = roomRepository;
        this.storeRepository = storeRepository;
        this.providerRegistry = providerRegistry;
        this.credentialCrypto = credentialCrypto;
        this.mapper = mapper;
        this.config = config;
        this.objectMapper = objectMapper;
        this.roleResolver = new SmartLockDeviceRoleResolver(objectMapper);
        this.passcodeReconciliationService = passcodeReconciliationService;
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
        BindingRoleSelection roles = resolveBindingRoleSelection(storeId, request);
        SmartLockIntegration integration = resolveBindingIntegration(storeId, request, roles);
        Long existingBindingId = null;
        Optional<SmartLockRoomBinding> existingBinding = bindingRepository.findByStoreIdAndRoomIdAndStatus(
                storeId,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        );
        if (existingBinding.isPresent()) {
            existingBindingId = existingBinding.get().getId();
        }
        validateBindingRoles(storeId, room.getId(), existingBindingId, integration, roles);

        SmartLockRoomBinding binding = existingBinding.orElseGet(SmartLockRoomBinding::new);
        binding.setStoreId(storeId);
        binding.setRoom(room);
        binding.setIntegration(integration);
        binding.setProvider(integration.getProvider());
        applyBindingRoles(binding, roles);
        binding.setStatus(SmartLockBindingStatus.ACTIVE);
        if (binding.getCreatedBy() == null) {
            binding.setCreatedBy(userId);
        }
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
        requireControlTarget(binding);
        return mapper.toStatusDto(binding);
    }

    @Transactional
    public SmartLockStatusDTO refreshRoomStatus(Long roomId) {
        Long storeId = StoreContextUtils.requireStoreId();
        requireRoom(storeId, roomId);
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, null);
        validateBindingConsistency(storeId, binding);
        RoleTarget target = requireControlTarget(binding);
        SmartLockDevice device = target.device();
        SmartLockProviderClient client = providerRegistry.getClient(target.provider());
        SmartLockCredentialData credentials = ensureProviderToken(target.integration());
        if (hasText(target.providerLockId()) && device != null) {
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
        requireControlTarget(binding);

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

    @Transactional
    public List<SmartLockPasscodeDTO> listPasscodes(Long roomId) {
        Long storeId = StoreContextUtils.requireStoreId();
        requireRoom(storeId, roomId);
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, null);
        validateBindingConsistency(storeId, binding);
        requirePasscodeTarget(binding);
        List<SmartLockPasscodeRecord> records =
                passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(storeId, roomId);
        passcodeReconciliationService.reconcileSwitchBotPasscodeRecords(
                storeId,
                records,
                config.getPasscodeReconcileTimeoutMinutes()
        );
        return records.stream()
                .filter(record -> record.getStatus() != SmartLockPasscodeStatus.DELETED)
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
        RoleTarget passcodeTarget = requirePasscodeTarget(binding);
        ZoneId storeZoneId = resolveStoreZoneId(storeId);
        validatePasscodeWindow(request.getValidFrom(), request.getValidUntil(), storeZoneId);
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
        record.setIntegration(passcodeTarget.integration());
        record.setProvider(passcodeTarget.provider());
        record.setProviderLockId(passcodeTarget.providerLockId());
        record.setPasscodeRole("PASSCODE");
        record.setPasscodeDevice(passcodeTarget.device());
        record.setPasscodeProviderLockId(passcodeTarget.providerLockId());
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
        logPasscodeProviderCallStart(
                "createPasscode",
                record,
                task,
                passcodeTarget,
                request.getValidFrom(),
                request.getValidUntil()
        );
        SmartLockCredentialData credentials = ensureProviderToken(passcodeTarget.integration());
        try {
            SmartLockProviderClient.PasscodeCommand command = new SmartLockProviderClient.PasscodeCommand(
                    passcodeName,
                    passcode,
                    request.getValidFrom(),
                    request.getValidUntil()
            );
            SmartLockProviderClient.ProviderTaskResult result = providerRegistry
                    .getClient(passcodeTarget.provider())
                    .createPasscode(credentials, passcodeTarget.providerLockId(), command);
            result = normalizeCreatePasscodeResult(passcodeTarget.provider(), result);
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
            logPasscodeProviderCallResult("createPasscode", record, task, passcodeTarget, result);
            String oneTimePasscode = result.status() == SmartLockTaskStatus.SUCCESS ? passcode : null;
            return mapper.toPasscodeDto(record, oneTimePasscode);
        } catch (RuntimeException ex) {
            failTask(task, ex);
            record.setStatus(SmartLockPasscodeStatus.FAILED);
            record.setLastError(safeError(ex));
            passcodeRepository.save(record);
            logPasscodeProviderCallException("createPasscode", record, task, passcodeTarget, ex);
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
            logPasscodeDeleteLocalPath("alreadyDeleted", record);
            return mapper.toPasscodeDto(record, null);
        }
        if (record.getStatus() == SmartLockPasscodeStatus.DELETE_PENDING) {
            logPasscodeDeleteLocalPath("deletePending", record);
            return mapper.toPasscodeDto(record, null);
        }
        if (canCleanupLocalNoRemotePasscode(record)) {
            cleanupLocalNoRemotePasscode(record);
            return mapper.toPasscodeDto(record, null);
        }
        RoleTarget passcodeTarget = requirePasscodeSnapshotTarget(storeId, record);
        if (!hasText(record.getProviderPasscodeId())) {
            logPasscodeDeleteMissingProviderPasscodeId(record, passcodeTarget);
            throw new IllegalArgumentException(MISSING_PROVIDER_PASSCODE_ID_DELETE_MESSAGE);
        }
        record.setStatus(SmartLockPasscodeStatus.DELETE_PENDING);
        passcodeRepository.save(record);
        SmartLockTask task = createTask(
                storeId,
                userId,
                SmartLockTaskType.DELETE_PASSCODE,
                record.getBinding(),
                record,
                null,
                null,
                credentialCrypto.sha256Hex("PASSCODE_DELETE|" + storeId + "|" + recordId),
                null
        );
        logPasscodeProviderCallStart("deletePasscode", record, task, passcodeTarget, null, null);
        SmartLockCredentialData credentials = ensureProviderToken(passcodeTarget.integration());
        try {
            SmartLockProviderClient.ProviderTaskResult result = providerRegistry
                    .getClient(passcodeTarget.provider())
                    .deletePasscode(credentials, passcodeTarget.providerLockId(), record.getProviderPasscodeId());
            completeTask(task, result);
            applyTaskResultToPasscodeRecord(task, result);
            logPasscodeProviderCallResult("deletePasscode", record, task, passcodeTarget, result);
            return mapper.toPasscodeDto(record, null);
        } catch (RuntimeException ex) {
            failTask(task, ex);
            record.setStatus(SmartLockPasscodeStatus.FAILED);
            record.setLastError(safeError(ex));
            passcodeRepository.save(record);
            logPasscodeProviderCallException("deletePasscode", record, task, passcodeTarget, ex);
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

        RoleTarget commandTarget = requireControlTarget(binding);
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
        SmartLockCredentialData credentials = ensureProviderToken(commandTarget.integration());
        try {
            SmartLockProviderClient.ProviderTaskResult result;
            if (action == SmartLockTaskType.UNLOCK) {
                result = providerRegistry.getClient(commandTarget.provider()).unlock(
                        credentials,
                        commandTarget.providerLockId()
                );
            } else {
                result = providerRegistry.getClient(commandTarget.provider()).lock(
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

    private BindingRoleSelection resolveBindingRoleSelection(
            Long storeId,
            SmartLockRequests.CreateBindingRequest request
    ) {
        if (!hasRoleInput(request)) {
            SmartLockDevice legacyDevice = resolveDevice(storeId, request);
            SmartLockDevice controlDevice = roleResolver.supportsControl(legacyDevice) ? legacyDevice : null;
            SmartLockDevice passcodeDevice = roleResolver.supportsPasscode(legacyDevice) ? legacyDevice : null;
            if (controlDevice == null && passcodeDevice == null) {
                throw new IllegalArgumentException("所选门锁设备不支持控制或密码能力");
            }
            return new BindingRoleSelection(controlDevice, passcodeDevice);
        }

        SmartLockDevice controlDevice = resolveRoleDevice(
                storeId,
                request,
                request.getControlDeviceId(),
                request.getControlProviderLockId(),
                "控制设备"
        );
        SmartLockDevice passcodeDevice = resolveRoleDevice(
                storeId,
                request,
                request.getPasscodeDeviceId(),
                request.getPasscodeProviderLockId(),
                "密码设备"
        );
        if (controlDevice == null && passcodeDevice == null) {
            throw new IllegalArgumentException("至少需要绑定控制设备或密码设备");
        }
        return new BindingRoleSelection(controlDevice, passcodeDevice);
    }

    private boolean hasRoleInput(SmartLockRequests.CreateBindingRequest request) {
        return request.getControlDeviceId() != null
                || hasText(request.getControlProviderLockId())
                || request.getPasscodeDeviceId() != null
                || hasText(request.getPasscodeProviderLockId());
    }

    private SmartLockDevice resolveRoleDevice(
            Long storeId,
            SmartLockRequests.CreateBindingRequest request,
            Long deviceId,
            String requestedProviderLockId,
            String roleName
    ) {
        String providerLockId = SmartLockMaskingUtils.trimToNull(requestedProviderLockId);
        if (deviceId == null && !hasText(providerLockId)) {
            return null;
        }

        SmartLockDevice device;
        if (deviceId != null) {
            device = deviceRepository.findByStoreIdAndId(storeId, deviceId)
                    .orElseThrow(() -> new IllegalArgumentException(roleName + "不存在"));
            if (hasText(providerLockId) && !providerLockId.equals(device.getProviderLockId())) {
                throw new IllegalArgumentException(roleName + "与 providerLockId 不一致");
            }
        } else {
            SmartLockProvider provider = resolveProviderHint(storeId, request);
            device = deviceRepository.findByStoreIdAndProviderAndProviderLockId(storeId, provider, providerLockId)
                    .orElseThrow(() -> new IllegalArgumentException(roleName + "不存在，请先同步设备"));
        }

        if (request.getProvider() != null && request.getProvider() != device.getProvider()) {
            throw new IllegalArgumentException(roleName + "与指定服务商不一致");
        }
        return device;
    }

    private SmartLockProvider resolveProviderHint(Long storeId, SmartLockRequests.CreateBindingRequest request) {
        if (request.getProvider() != null) {
            return request.getProvider();
        }
        if (request.getIntegrationId() != null) {
            return requireIntegration(storeId, request.getIntegrationId()).getProvider();
        }
        throw new IllegalArgumentException("provider 不能为空");
    }

    private SmartLockIntegration resolveBindingIntegration(
            Long storeId,
            SmartLockRequests.CreateBindingRequest request,
            BindingRoleSelection roles
    ) {
        SmartLockIntegration integration = null;
        if (roles.controlDevice() != null) {
            integration = roles.controlDevice().getIntegration();
        }
        if (roles.passcodeDevice() != null) {
            SmartLockIntegration passcodeIntegration = roles.passcodeDevice().getIntegration();
            if (integration == null) {
                integration = passcodeIntegration;
            } else if (passcodeIntegration == null || !integration.getId().equals(passcodeIntegration.getId())) {
                throw new IllegalArgumentException("控制设备与密码设备不属于同一集成");
            }
        }
        if (request.getIntegrationId() != null) {
            SmartLockIntegration requestedIntegration = requireIntegration(storeId, request.getIntegrationId());
            if (integration == null || !requestedIntegration.getId().equals(integration.getId())) {
                throw new IllegalArgumentException("门锁设备不属于指定集成");
            }
        }
        if (integration == null) {
            throw new IllegalArgumentException("门锁集成不存在");
        }
        return integration;
    }

    private void validateBindingRoles(
            Long storeId,
            Long roomId,
            Long existingBindingId,
            SmartLockIntegration integration,
            BindingRoleSelection roles
    ) {
        SmartLockDevice controlDevice = roles.controlDevice();
        SmartLockDevice passcodeDevice = roles.passcodeDevice();
        if (controlDevice != null) {
            validateDeviceConsistency(storeId, controlDevice, integration);
            if (!roleResolver.supportsControl(controlDevice)) {
                throw new IllegalArgumentException("所选控制设备不支持开关锁或状态能力");
            }
            ensureProviderLockNotBoundElsewhere(
                    storeId,
                    roomId,
                    existingBindingId,
                    controlDevice.getProvider(),
                    controlDevice.getProviderLockId()
            );
        }
        if (passcodeDevice != null) {
            validateDeviceConsistency(storeId, passcodeDevice, integration);
            if (!roleResolver.supportsPasscode(passcodeDevice)) {
                throw new IllegalArgumentException("所选密码设备不支持门锁密码能力");
            }
            ensureProviderLockNotBoundElsewhere(
                    storeId,
                    roomId,
                    existingBindingId,
                    passcodeDevice.getProvider(),
                    passcodeDevice.getProviderLockId()
            );
            validatePasscodeDeviceAssociation(storeId, roomId, existingBindingId, controlDevice, passcodeDevice);
        }
    }

    private void validatePasscodeDeviceAssociation(
            Long storeId,
            Long roomId,
            Long existingBindingId,
            SmartLockDevice controlDevice,
            SmartLockDevice passcodeDevice
    ) {
        if (passcodeDevice.getProvider() != SmartLockProvider.SWITCHBOT) {
            return;
        }
        if (roleResolver.hasConflictingSwitchBotLinkedControl(passcodeDevice)) {
            throw new IllegalArgumentException("SwitchBot 密码设备的 lockDeviceId 与辅助设备 ID 不一致，请重新同步设备");
        }

        if (roleResolver.isSwitchBotAuthenticationPanel(passcodeDevice)) {
            String linkedControlProviderLockId = roleResolver.linkedControlProviderLockId(passcodeDevice);
            if (controlDevice != null) {
                if (!hasText(linkedControlProviderLockId)) {
                    throw new IllegalArgumentException("SwitchBot 密码设备未返回关联的控制门锁，请重新同步设备");
                }
                if (!linkedControlProviderLockId.equals(controlDevice.getProviderLockId())) {
                    throw new IllegalArgumentException("SwitchBot 密码设备关联的控制门锁与所选控制设备不一致");
                }
            } else if (hasText(linkedControlProviderLockId)) {
                ensureProviderLockNotBoundElsewhere(
                        storeId,
                        roomId,
                        existingBindingId,
                        passcodeDevice.getProvider(),
                        linkedControlProviderLockId
                );
            }
            return;
        }

        if (controlDevice != null && !passcodeDevice.getProviderLockId().equals(controlDevice.getProviderLockId())) {
            throw new IllegalArgumentException("SwitchBot 双能力密码设备必须与控制设备一致");
        }
    }

    private void ensureProviderLockNotBoundElsewhere(
            Long storeId,
            Long roomId,
            Long existingBindingId,
            SmartLockProvider provider,
            String providerLockId
    ) {
        if (!hasText(providerLockId)) {
            return;
        }
        List<SmartLockRoomBinding> conflicts = bindingRepository.findActiveByAnyRoleProviderLockId(
                storeId,
                provider,
                providerLockId,
                SmartLockBindingStatus.ACTIVE,
                existingBindingId
        );
        for (SmartLockRoomBinding conflict : conflicts) {
            if (conflict.getRoom() == null || !roomId.equals(conflict.getRoom().getId())) {
                throw new IllegalArgumentException("该门锁设备已绑定其他房间");
            }
        }
    }

    private void applyBindingRoles(SmartLockRoomBinding binding, BindingRoleSelection roles) {
        SmartLockDevice controlDevice = roles.controlDevice();
        SmartLockDevice passcodeDevice = roles.passcodeDevice();
        binding.setControlDevice(controlDevice);
        binding.setControlProviderLockId(controlDevice != null ? controlDevice.getProviderLockId() : null);
        binding.setPasscodeDevice(passcodeDevice);
        binding.setPasscodeProviderLockId(passcodeDevice != null ? passcodeDevice.getProviderLockId() : null);

        SmartLockDevice legacyDevice = controlDevice != null ? controlDevice : passcodeDevice;
        binding.setDevice(legacyDevice);
        binding.setProviderLockId(legacyDevice.getProviderLockId());
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
        task.setProvider(passcodeRecord != null ? passcodeRecord.getProvider() : binding.getProvider());
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

    private SmartLockProviderClient.ProviderTaskResult normalizeCreatePasscodeResult(
            SmartLockProvider provider,
            SmartLockProviderClient.ProviderTaskResult result
    ) {
        if (provider != SmartLockProvider.SWITCHBOT || result.status() != SmartLockTaskStatus.PENDING) {
            return result;
        }
        if (hasText(result.providerTaskId()) || hasText(result.providerPasscodeId())) {
            return result;
        }
        return new SmartLockProviderClient.ProviderTaskResult(
                SmartLockTaskStatus.PENDING,
                null,
                null,
                SWITCHBOT_NO_ID_CREATE_PENDING_MESSAGE
        );
    }

    private boolean canCleanupLocalNoRemotePasscode(SmartLockPasscodeRecord record) {
        if (record == null) {
            return false;
        }
        boolean cleanupStatus = record.getStatus() == SmartLockPasscodeStatus.FAILED
                || record.getStatus() == SmartLockPasscodeStatus.PENDING;
        return cleanupStatus
                && !hasText(record.getProviderPasscodeId())
                && !hasText(record.getProviderTaskId());
    }

    private void cleanupLocalNoRemotePasscode(SmartLockPasscodeRecord record) {
        record.setStatus(SmartLockPasscodeStatus.DELETED);
        record.setDeletedAt(now());
        record.setLastError(null);
        passcodeRepository.save(record);
        passcodeReconciliationService.completeMatchingCreateTasksWithoutProviderTaskId(
                record,
                SmartLockTaskStatus.FAILED,
                LOCAL_NO_REMOTE_PASSCODE_CLEANUP_MESSAGE
        );
        logPasscodeDeleteLocalPath("noRemoteIdLocalCleanup", record);
    }

    private void logPasscodeProviderCallStart(
            String command,
            SmartLockPasscodeRecord record,
            SmartLockTask task,
            RoleTarget target,
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ) {
        logger.info(
                "SmartLock passcode provider call start command={} recordId={} taskId={} provider={} "
                        + "passcodeDeviceDbId={} passcodeDeviceIdSuffix={} providerLockIdSuffix={} "
                        + "validFrom={} validUntil={} providerTaskIdPresent={} providerPasscodeIdPresent={} "
                        + "recordStatus={} taskStatus={}",
                command,
                record.getId(),
                task.getId(),
                providerLabel(target, record),
                passcodeDeviceDbId(target, record),
                passcodeDeviceIdSuffix(target, record),
                providerLockIdSuffix(target, record),
                validFrom,
                validUntil,
                hasText(record.getProviderTaskId()),
                hasText(record.getProviderPasscodeId()),
                record.getStatus(),
                task.getStatus()
        );
    }

    private void logPasscodeProviderCallResult(
            String command,
            SmartLockPasscodeRecord record,
            SmartLockTask task,
            RoleTarget target,
            SmartLockProviderClient.ProviderTaskResult result
    ) {
        logger.info(
                "SmartLock passcode provider call result command={} recordId={} taskId={} provider={} "
                        + "passcodeDeviceDbId={} passcodeDeviceIdSuffix={} providerLockIdSuffix={} "
                        + "providerTaskIdPresent={} providerPasscodeIdPresent={} taskStatus={} recordStatus={}",
                command,
                record.getId(),
                task.getId(),
                providerLabel(target, record),
                passcodeDeviceDbId(target, record),
                passcodeDeviceIdSuffix(target, record),
                providerLockIdSuffix(target, record),
                hasText(result.providerTaskId()),
                hasText(result.providerPasscodeId()),
                task.getStatus(),
                record.getStatus()
        );
    }

    private void logPasscodeProviderCallException(
            String command,
            SmartLockPasscodeRecord record,
            SmartLockTask task,
            RoleTarget target,
            RuntimeException ex
    ) {
        logger.warn(
                "SmartLock passcode provider call failed command={} recordId={} taskId={} provider={} "
                        + "passcodeDeviceDbId={} passcodeDeviceIdSuffix={} providerLockIdSuffix={} "
                        + "providerTaskIdPresent={} providerPasscodeIdPresent={} taskStatus={} "
                        + "recordStatus={} finalStatus={} errorClass={}",
                command,
                record.getId(),
                task.getId(),
                providerLabel(target, record),
                passcodeDeviceDbId(target, record),
                passcodeDeviceIdSuffix(target, record),
                providerLockIdSuffix(target, record),
                hasText(record.getProviderTaskId()),
                hasText(record.getProviderPasscodeId()),
                task.getStatus(),
                record.getStatus(),
                SmartLockTaskStatus.FAILED,
                ex.getClass().getSimpleName()
        );
    }

    private void logPasscodeDeleteLocalPath(String action, SmartLockPasscodeRecord record) {
        logger.info(
                "SmartLock passcode delete local path command=deletePasscode action={} recordId={} "
                        + "taskId={} provider={} passcodeDeviceDbId={} passcodeDeviceIdSuffix={} "
                        + "providerLockIdSuffix={} providerTaskIdPresent={} providerPasscodeIdPresent={} "
                        + "recordStatus={}",
                action,
                record.getId(),
                null,
                providerLabel(null, record),
                passcodeDeviceDbId(null, record),
                passcodeDeviceIdSuffix(null, record),
                providerLockIdSuffix(null, record),
                hasText(record.getProviderTaskId()),
                hasText(record.getProviderPasscodeId()),
                record.getStatus()
        );
    }

    private void logPasscodeDeleteMissingProviderPasscodeId(
            SmartLockPasscodeRecord record,
            RoleTarget target
    ) {
        logger.warn(
                "SmartLock passcode delete rejected command=deletePasscode action=missingProviderPasscodeId "
                        + "recordId={} taskId={} provider={} passcodeDeviceDbId={} passcodeDeviceIdSuffix={} "
                        + "providerLockIdSuffix={} providerTaskIdPresent={} providerPasscodeIdPresent={} "
                        + "finalStatus={} errorClass={}",
                record.getId(),
                null,
                providerLabel(target, record),
                passcodeDeviceDbId(target, record),
                passcodeDeviceIdSuffix(target, record),
                providerLockIdSuffix(target, record),
                hasText(record.getProviderTaskId()),
                hasText(record.getProviderPasscodeId()),
                SmartLockTaskStatus.FAILED,
                IllegalArgumentException.class.getSimpleName()
        );
    }

    private SmartLockProvider providerLabel(RoleTarget target, SmartLockPasscodeRecord record) {
        if (target != null && target.provider() != null) {
            return target.provider();
        }
        if (record == null) {
            return null;
        }
        return record.getProvider();
    }

    private Long passcodeDeviceDbId(RoleTarget target, SmartLockPasscodeRecord record) {
        SmartLockDevice device = passcodeDevice(target, record);
        if (device == null) {
            return null;
        }
        return device.getId();
    }

    private String passcodeDeviceIdSuffix(RoleTarget target, SmartLockPasscodeRecord record) {
        SmartLockDevice device = passcodeDevice(target, record);
        if (device == null) {
            return null;
        }
        return identifierSuffix(device.getProviderLockId());
    }

    private SmartLockDevice passcodeDevice(RoleTarget target, SmartLockPasscodeRecord record) {
        if (target != null && target.device() != null) {
            return target.device();
        }
        if (record == null) {
            return null;
        }
        return record.getPasscodeDevice();
    }

    private String providerLockIdSuffix(RoleTarget target, SmartLockPasscodeRecord record) {
        String providerLockId = null;
        if (target != null) {
            providerLockId = target.providerLockId();
        }
        if (!hasText(providerLockId) && record != null) {
            providerLockId = firstText(record.getPasscodeProviderLockId(), record.getProviderLockId());
        }
        return identifierSuffix(providerLockId);
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

    private void refreshPendingTask(SmartLockTask task) {
        if (task.getPasscodeRecord() != null) {
            refreshPendingPasscodeTask(task);
            return;
        }
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

    private void refreshPendingPasscodeTask(SmartLockTask task) {
        try {
            SmartLockPasscodeRecord record = task.getPasscodeRecord();
            if (task.getProvider() == SmartLockProvider.SWITCHBOT) {
                SmartLockPasscodeReconciliationService.ReconciliationOutcome outcome =
                        passcodeReconciliationService.reconcileSwitchBotPendingPasscodeTask(
                        task,
                        config.getPasscodeReconcileTimeoutMinutes()
                );
                if (outcome.terminal() || outcome.changed()) {
                    return;
                }
            }
            RoleTarget target = requirePasscodeSnapshotTarget(task.getStoreId(), task.getPasscodeRecord());
            SmartLockCredentialData credentials = ensureProviderToken(target.integration());
            SmartLockProviderClient.ProviderTaskResult result = providerRegistry
                    .getClient(target.provider())
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

    private RoleTarget requireControlTarget(SmartLockRoomBinding binding) {
        SmartLockDevice controlDevice = binding.getControlDevice();
        if (controlDevice != null) {
            if (!roleResolver.supportsControl(controlDevice)) {
                throw new IllegalArgumentException("该房间控制设备不支持门锁状态或开关锁操作");
            }
            String providerLockId = firstText(binding.getControlProviderLockId(), controlDevice.getProviderLockId());
            if (!hasText(providerLockId)) {
                throw new IllegalArgumentException("该房间控制设备缺少供应商设备 ID");
            }
            return new RoleTarget(binding.getIntegration(), binding.getProvider(), controlDevice, providerLockId);
        }

        if (!hasRoleColumns(binding)) {
            SmartLockDevice legacyDevice = binding.getDevice();
            if (roleResolver.supportsControl(legacyDevice)) {
                return new RoleTarget(
                        binding.getIntegration(),
                        binding.getProvider(),
                        legacyDevice,
                        firstText(
                                binding.getProviderLockId(),
                                legacyDevice != null ? legacyDevice.getProviderLockId() : null
                        )
                );
            }
            if (roleResolver.isSwitchBotAuthenticationPanel(legacyDevice)) {
                String linkedControlProviderLockId = roleResolver.linkedControlProviderLockId(legacyDevice);
                if (hasText(linkedControlProviderLockId)) {
                    return new RoleTarget(
                            binding.getIntegration(),
                            binding.getProvider(),
                            legacyDevice,
                            linkedControlProviderLockId
                    );
                }
                throw new IllegalArgumentException("SwitchBot Keypad 缺少 lockDeviceId，无法推导控制设备，请重新同步设备");
            }
        }

        throw new IllegalArgumentException("该房间未绑定控制设备，无法执行门锁状态或开关锁操作");
    }

    private RoleTarget requirePasscodeTarget(SmartLockRoomBinding binding) {
        SmartLockDevice passcodeDevice = binding.getPasscodeDevice();
        if (passcodeDevice != null) {
            if (!roleResolver.supportsPasscode(passcodeDevice)) {
                throw new IllegalArgumentException("该房间密码设备不支持门锁密码操作");
            }
            String providerLockId = firstText(binding.getPasscodeProviderLockId(), passcodeDevice.getProviderLockId());
            if (!hasText(providerLockId)) {
                throw new IllegalArgumentException("该房间密码设备缺少供应商设备 ID");
            }
            return new RoleTarget(binding.getIntegration(), binding.getProvider(), passcodeDevice, providerLockId);
        }

        if (!hasRoleColumns(binding) && roleResolver.supportsPasscode(binding.getDevice())) {
            SmartLockDevice legacyDevice = binding.getDevice();
            return new RoleTarget(
                    binding.getIntegration(),
                    binding.getProvider(),
                    legacyDevice,
                    firstText(
                            binding.getProviderLockId(),
                            legacyDevice != null ? legacyDevice.getProviderLockId() : null
                    )
            );
        }

        throw new IllegalArgumentException("该房间未绑定密码设备，无法执行门锁密码操作");
    }

    private RoleTarget requirePasscodeSnapshotTarget(Long storeId, SmartLockPasscodeRecord record) {
        if (record.getRoom() == null || record.getRoom().getId() == null) {
            throw new IllegalArgumentException("门锁密码记录缺少房间快照");
        }
        if (!storeId.equals(record.getRoom().getStoreId())) {
            throw new IllegalArgumentException("门锁密码记录与当前门店不一致");
        }
        if (record.getBinding() == null || !storeId.equals(record.getBinding().getStoreId())) {
            throw new IllegalArgumentException("门锁密码记录缺少绑定快照");
        }
        SmartLockIntegration integration = record.getIntegration();
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException("门锁密码记录缺少集成快照");
        }
        if (record.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException("门锁密码记录与集成服务商不一致，已拒绝删除远程密码");
        }
        String providerLockId = firstText(record.getPasscodeProviderLockId(), record.getProviderLockId());
        if (!hasText(providerLockId)) {
            throw new IllegalArgumentException("门锁密码记录缺少密码设备快照，无法删除远程密码");
        }
        SmartLockDevice passcodeDevice = record.getPasscodeDevice();
        if (passcodeDevice != null) {
            validateDeviceConsistency(storeId, passcodeDevice, integration);
            if (!providerLockId.equals(passcodeDevice.getProviderLockId())) {
                throw new IllegalArgumentException("门锁密码记录与密码设备快照不一致，已拒绝删除远程密码");
            }
        }
        return new RoleTarget(integration, record.getProvider(), passcodeDevice, providerLockId);
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
        SmartLockIntegration integration = binding.getIntegration();
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException("门锁绑定与集成门店不一致");
        }
        if (binding.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException("门锁绑定与集成服务商不一致");
        }
        if (binding.getControlDevice() != null) {
            validateRoleDeviceConsistency(
                    storeId,
                    binding.getControlDevice(),
                    integration,
                    binding.getControlProviderLockId(),
                    "控制设备"
            );
        }
        if (binding.getPasscodeDevice() != null) {
            validateRoleDeviceConsistency(
                    storeId,
                    binding.getPasscodeDevice(),
                    integration,
                    binding.getPasscodeProviderLockId(),
                    "密码设备"
            );
        }
        if (!hasRoleColumns(binding)) {
            validateDeviceConsistency(storeId, binding.getDevice(), integration);
            if (binding.getProvider() != binding.getDevice().getProvider()) {
                throw new IllegalArgumentException("门锁绑定与设备服务商不一致");
            }
            if (!binding.getProviderLockId().equals(binding.getDevice().getProviderLockId())) {
                throw new IllegalArgumentException("门锁绑定与设备 ID 不一致");
            }
        } else if (binding.getControlDevice() == null && binding.getPasscodeDevice() == null) {
            throw new IllegalArgumentException("门锁绑定缺少控制设备和密码设备");
        }
    }

    private void validateRoleDeviceConsistency(
            Long storeId,
            SmartLockDevice device,
            SmartLockIntegration integration,
            String providerLockId,
            String roleName
    ) {
        validateDeviceConsistency(storeId, device, integration);
        if (!device.getProviderLockId().equals(providerLockId)) {
            throw new IllegalArgumentException("门锁绑定" + roleName + "快照与设备 ID 不一致");
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

    private void validatePasscodeWindow(LocalDateTime validFrom, LocalDateTime validUntil, ZoneId storeZoneId) {
        if (validFrom == null || validUntil == null) {
            throw new IllegalArgumentException("门锁密码必须提供有效开始和结束时间");
        }
        if (!validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("门锁密码结束时间必须晚于开始时间");
        }
        if (!validUntil.isAfter(now(storeZoneId))) {
            throw new IllegalArgumentException("门锁密码有效期已过期，请选择未来的结束时间");
        }
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

    private LocalDateTime now(ZoneId zoneId) {
        if (zoneId == null) {
            return now();
        }
        return LocalDateTime.now(clock.withZone(zoneId));
    }

    private ZoneId resolveStoreZoneId(Long storeId) {
        Store store = storeId == null || storeRepository == null ? null : storeRepository.findById(storeId).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
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

    private boolean hasRoleColumns(SmartLockRoomBinding binding) {
        return binding.getControlDevice() != null
                || hasText(binding.getControlProviderLockId())
                || binding.getPasscodeDevice() != null
                || hasText(binding.getPasscodeProviderLockId());
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record StatusLookupTarget(String providerLockId, String source) {
    }

    private record BindingRoleSelection(SmartLockDevice controlDevice, SmartLockDevice passcodeDevice) {
    }

    private record RoleTarget(
            SmartLockIntegration integration,
            SmartLockProvider provider,
            SmartLockDevice device,
            String providerLockId
    ) {
    }
}
