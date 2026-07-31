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

import server.demo.i18n.ApiMessages;
@Service
public class SmartLockService {
    private static final Logger logger = LoggerFactory.getLogger(SmartLockService.class);
    private static final int CONFIRM_TOKEN_BYTES = 32;
    private static final int GENERATED_PASSCODE_MIN = 100000;
    private static final int GENERATED_PASSCODE_BOUND = 900000;
    private static final int MIN_PASSCODE_LENGTH = 6;
    private static final int MAX_PASSCODE_LENGTH = 12;
    private static final String SWITCHBOT_NO_ID_CREATE_PENDING_MESSAGE_KEY = "api.t.065856f6b3b7";
    private static final String TTLOCK_NO_ID_CREATE_PENDING_MESSAGE_KEY = "api.t.9bbc264250bd";
    private static final String TTLOCK_PASSCODE_SYNC_NO_MATCH_MESSAGE_KEY = "api.t.ed3a70ec9a60";
    private static final String TTLOCK_DELETE_STILL_EXISTS_MESSAGE_KEY = "api.t.f6ec1a7b964a";
    private static final String TTLOCK_DELETE_IN_PROGRESS_MESSAGE_KEY = "api.t.7812e21c0046";
    private static final String TTLOCK_STATUS_REFRESH_FAILED_PREFIX_KEY = "api.t.879ef2d82864";
    private static final String PENDING_PASSCODE_MASK = "******";
    private static final String TTLOCK_AUTO_PASSCODE_PENDING_HASH_PREFIX = "TTLOCK_AUTO_PENDING";
    private static final String TTLOCK_DEFAULT_KEYBOARD_PWD_VERSION = "4";
    private static final String TTLOCK_PASSCODE_STATUS_ACTIVE = "1";
    private static final String TTLOCK_PASSCODE_STATUS_INVALID = "2";
    private static final String TTLOCK_PASSCODE_STATUS_PENDING = "3";
    private static final String TTLOCK_PASSCODE_STATUS_ADDING = "4";
    private static final String TTLOCK_PASSCODE_STATUS_ADD_FAILED = "5";
    private static final String TTLOCK_PASSCODE_STATUS_DELETING = "8";
    private static final String TTLOCK_PASSCODE_STATUS_DELETE_FAILED = "9";
    private static final String LOCAL_NO_REMOTE_PASSCODE_CLEANUP_MESSAGE_KEY = "api.t.78d965fd3a24";
    private static final String MISSING_PROVIDER_PASSCODE_ID_DELETE_MESSAGE_KEY = "api.t.c55762eaf20a";
    private static final String SWITCHBOT_PASSCODE_WRITE_UNAVAILABLE_REASON_CODE =
            "SWITCHBOT_PASSCODE_TEMPORARILY_UNAVAILABLE";
    private static final List<SmartLockPasscodeStatus> BINDING_DELETE_RISKY_PASSCODE_STATUSES = List.of(
            SmartLockPasscodeStatus.ACTIVE,
            SmartLockPasscodeStatus.PENDING,
            SmartLockPasscodeStatus.UNKNOWN,
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
            return new SmartLockTestResultDTO(true, ApiMessages.get("api.t.bfbf35fbdce1"));
        } catch (RuntimeException ex) {
            integration.setConnectionStatus(SmartLockIntegrationStatus.ERROR);
            integration.setLastTestAt(now);
            integration.setLastError(safeError(ex));
            integrationRepository.save(integration);
            return new SmartLockTestResultDTO(false, ApiMessages.get("api.t.e52f6fc91de6") + safeError(ex));
        }
    }

    @Transactional
    public SmartLockIntegrationDTO refreshToken(Long integrationId) {
        Long storeId = StoreContextUtils.requireStoreId();
        SmartLockIntegration integration = requireIntegration(storeId, integrationId);
        SmartLockCredentialData credentials = decryptCredentials(integration);
        SmartLockProviderClient client = providerRegistry.getClient(integration.getProvider());
        SmartLockCredentialData refreshed;
        try {
            refreshed = client.refreshToken(credentials);
        } catch (RuntimeException ex) {
            logTtLockTokenFailure(integration, "refreshToken", ex);
            throw ex;
        }
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
                bindingByRoomId.put(binding.getRoom().getId(), binding);
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
        validateRoomProviderExclusivity(existingBinding, integration);
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
        return decoratePasscodeWriteCapability(mapper.toStatusDto(binding), binding.getProvider());
    }

    @Transactional
    public SmartLockStatusDTO refreshRoomStatus(Long roomId) {
        Long storeId = StoreContextUtils.requireStoreId();
        requireRoom(storeId, roomId);
        SmartLockRoomBinding binding = requireBindingForRoom(storeId, roomId, null);
        validateBindingConsistency(storeId, binding);
        RoleTarget target = requireControlTarget(binding);
        refreshControlDeviceStatus(target, now());
        return decoratePasscodeWriteCapability(mapper.toStatusDto(binding), binding.getProvider());
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
        RoleTarget passcodeTarget = requirePasscodeTarget(binding);
        List<SmartLockPasscodeRecord> records =
                passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(storeId, roomId);
        if (passcodeTarget.provider() == SmartLockProvider.TTLOCK) {
            reconcileTtLockPasscodeRecords(storeId, passcodeTarget, records);
        } else {
            passcodeReconciliationService.reconcileSwitchBotPasscodeRecords(
                    storeId,
                    records,
                    config.getPasscodeReconcileTimeoutMinutes()
            );
        }
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
        requirePasscodeWriteEnabled(passcodeTarget.provider());
        ZoneId storeZoneId = resolveStoreZoneId(storeId);
        validatePasscodeWindow(request.getValidFrom(), request.getValidUntil(), storeZoneId);
        String requestedPasscode = SmartLockMaskingUtils.trimToNull(request.getPasscode());
        boolean ttLockAutomaticPasscode = passcodeTarget.provider() == SmartLockProvider.TTLOCK
                && !hasText(requestedPasscode);
        String passcode = ttLockAutomaticPasscode ? null : normalizePasscode(request.getPasscode());
        String passcodeName = fallback(SmartLockMaskingUtils.trimToNull(request.getPasscodeName()), ApiMessages.get("api.t.f6f071cfc014"));
        String requestHash = credentialCrypto.sha256Hex(
                "PASSCODE_CREATE|" + storeId + "|" + userId + "|" + roomId + "|" + binding.getId() + "|" + passcodeName
        );
        Optional<SmartLockTask> duplicate = findDuplicateTask(storeId, request.getIdempotencyKey(), requestHash);
        if (duplicate.isPresent()) {
            SmartLockTask existingTask = duplicate.get();
            if (existingTask.getPasscodeRecord() != null) {
                return mapper.toPasscodeDto(existingTask.getPasscodeRecord(), null);
            }
            throw new IllegalStateException(ApiMessages.get("api.t.b40d3ae97a59"));
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
        record.setPasscodeMasked(hasText(passcode) ? SmartLockMaskingUtils.maskPasscode(passcode) : PENDING_PASSCODE_MASK);
        record.setPasscodeHash(hasText(passcode)
                ? passcodeHash(storeId, roomId, passcode)
                : pendingTtLockAutoPasscodeHash(storeId, userId, roomId, request.getIdempotencyKey()));
        record.setValidFrom(request.getValidFrom());
        record.setValidUntil(request.getValidUntil());
        record.setStatus(SmartLockPasscodeStatus.PENDING);
        record.setCreatedBy(userId);
        record.setSubmittedAtEpochMs(clock.millis());
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
        boolean providerCallStarted = false;
        try {
            SmartLockCredentialData credentials = ensureProviderToken(passcodeTarget.integration());
            SmartLockProviderClient.PasscodeCommand command = new SmartLockProviderClient.PasscodeCommand(
                    passcodeName,
                    passcode,
                    request.getValidFrom(),
                    request.getValidUntil()
            );
            SmartLockProviderClient client = providerRegistry.getClient(passcodeTarget.provider());
            String oneTimePasscode = passcode;
            SmartLockProviderClient.ProviderTaskResult result;
            providerCallStarted = true;
            if (ttLockAutomaticPasscode) {
                SmartLockTtLockClient.TtLockPasscodeCommandResult commandResult =
                        createTtLockPeriodPasscode(
                                client,
                                credentials,
                                passcodeTarget.providerLockId(),
                                command,
                                resolveTtLockKeyboardPwdVersion(passcodeTarget)
                        );
                oneTimePasscode = commandResult.passcode();
                record.setPasscodeMasked(SmartLockMaskingUtils.maskPasscode(oneTimePasscode));
                record.setPasscodeHash(passcodeHash(storeId, roomId, oneTimePasscode));
                result = commandResult.taskResult();
            } else {
                result = client.createPasscode(credentials, passcodeTarget.providerLockId(), command);
            }
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
                record.setLastError(passcodeTarget.provider() == SmartLockProvider.TTLOCK
                        ? safeProviderMessage(result.message())
                        : null);
            }
            passcodeRepository.save(record);
            logPasscodeProviderCallResult("createPasscode", record, task, passcodeTarget, result);
            String visiblePasscode = result.status() == SmartLockTaskStatus.SUCCESS ? oneTimePasscode : null;
            return mapper.toPasscodeDto(record, visiblePasscode);
        } catch (RuntimeException ex) {
            boolean outcomeUnknown = providerCallStarted
                    && passcodeTarget.provider() == SmartLockProvider.SWITCHBOT
                    && !(ex instanceof SmartLockSwitchBotClient.ProviderRejectedException);
            if (outcomeUnknown) {
                completeTask(task, new SmartLockProviderClient.ProviderTaskResult(
                        SmartLockTaskStatus.UNKNOWN,
                        task.getProviderTaskId(),
                        null,
                        ApiMessages.get("api.t.ac208e7b9aca")
                ));
            } else {
                failTask(task, ex);
            }
            record.setStatus(outcomeUnknown
                    ? SmartLockPasscodeStatus.UNKNOWN
                    : SmartLockPasscodeStatus.FAILED);
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
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.c3bc0fdd0c58")));
        requirePasscodeWriteEnabled(record.getProvider());
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
        if (!hasText(record.getProviderPasscodeId()) && record.getProvider() == SmartLockProvider.TTLOCK) {
            reconcileTtLockPasscodeRecords(storeId, passcodeTarget, List.of(record));
        }
        if (!hasText(record.getProviderPasscodeId())) {
            logPasscodeDeleteMissingProviderPasscodeId(record, passcodeTarget);
            throw new IllegalArgumentException(ApiMessages.get(MISSING_PROVIDER_PASSCODE_ID_DELETE_MESSAGE_KEY));
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
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.af9c378dbd7e")));
        if (shouldRefreshPendingTask(task)) {
            refreshPendingTask(task);
        }
        return mapper.toTaskDto(task);
    }

    @Transactional
    public Map<String, Object> handleSwitchBotWebhook(String token, Map<String, Object> payload) {
        validateSwitchBotWebhookToken(token);
        JsonNode root = objectMapper.valueToTree(payload != null ? payload : Map.of());
        JsonNode context = root.path("context");
        boolean officialContext = context.isObject();
        String commandId = officialContext
                ? firstJsonText(context, "commandId")
                : findFirstText(root, "commandId", "taskId", "providerTaskId");
        String eventName = officialContext
                ? firstJsonText(context, "eventName")
                : findFirstText(root, "eventName", "eventType", "event", "command");
        SmartLockTaskStatus status = officialContext
                ? resolveSwitchBotWebhookResult(firstJsonText(context, "result"))
                : resolveSwitchBotWebhookStatus(root);
        String commandReference = safeReference(commandId);

        Map<String, Object> result = new LinkedHashMap<>();
        logger.info(
                "switchbot_passcode_webhook_received event={} commandRef={} parseMode={} result={}",
                safeProviderMessage(eventName),
                commandReference,
                officialContext ? "official_context" : "controlled_fallback",
                status
        );

        if (!hasText(commandId)) {
            result.put("processed", false);
            result.put("reason", "missing_command_id");
            logger.warn("switchbot_passcode_webhook_invalid reason=missing_command_id event={}",
                    safeProviderMessage(eventName));
            return result;
        }
        if (!"createKey".equals(eventName) && !"deleteKey".equals(eventName)) {
            result.put("processed", false);
            result.put("reason", "unsupported_event");
            logger.warn("switchbot_passcode_webhook_invalid reason=unsupported_event commandRef={}",
                    commandReference);
            return result;
        }
        if (status == null) {
            result.put("processed", false);
            result.put("reason", "unknown_result");
            logger.warn("switchbot_passcode_webhook_invalid reason=unknown_result commandRef={} event={}",
                    commandReference, eventName);
            return result;
        }

        List<SmartLockTask> tasks = taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                commandId
        );
        if (tasks.isEmpty()) {
            result.put("processed", false);
            result.put("reason", "task_not_found");
            logger.warn("switchbot_passcode_webhook_unmatched reason=task_not_found commandRef={} event={}",
                    commandReference, eventName);
            return result;
        }
        if (tasks.size() > 1) {
            result.put("processed", false);
            result.put("reason", "ambiguous_command_id");
            logger.warn("switchbot_passcode_webhook_unmatched reason=ambiguous_command_id commandRef={} event={}",
                    commandReference, eventName);
            return result;
        }

        SmartLockTask task = tasks.get(0);
        SmartLockTaskType expectedType = "createKey".equals(eventName)
                ? SmartLockTaskType.CREATE_PASSCODE
                : SmartLockTaskType.DELETE_PASSCODE;
        if (task.getTaskType() != expectedType) {
            result.put("processed", false);
            result.put("reason", "task_type_mismatch");
            logger.warn("switchbot_passcode_webhook_unmatched reason=task_type_mismatch commandRef={} event={} taskId={}",
                    commandReference, eventName, task.getId());
            return result;
        }
        String providerPasscodeId = findFirstText(root, "providerPasscodeId", "passcodeId", "keyId");
        String message = fallback(
                findFirstText(root, "message", "resultMessage", "errorMessage", "error"),
                "SwitchBot webhook " + status.name()
        );
        SmartLockProviderClient.ProviderTaskResult providerResult =
                new SmartLockProviderClient.ProviderTaskResult(status, commandId, providerPasscodeId, message);
        applySwitchBotWebhookResult(task, providerResult, commandReference);

        result.put("processed", true);
        result.put("taskId", task.getId());
        result.put("taskStatus", task.getStatus().name());
        if (task.getPasscodeRecord() != null) {
            result.put("passcodeRecordId", task.getPasscodeRecord().getId());
            result.put("passcodeStatus", task.getPasscodeRecord().getStatus().name());
        }
        return result;
    }

    private void applySwitchBotWebhookResult(
            SmartLockTask task,
            SmartLockProviderClient.ProviderTaskResult providerResult,
            String commandReference
    ) {
        SmartLockPasscodeRecord record = task.getPasscodeRecord();
        SmartLockPasscodeStatus current = record != null ? record.getStatus() : null;
        SmartLockPasscodeStatus target = switchBotWebhookPasscodeStatus(task.getTaskType(), providerResult.status());
        boolean conflict = (current == SmartLockPasscodeStatus.ACTIVE && target == SmartLockPasscodeStatus.FAILED)
                || (current == SmartLockPasscodeStatus.FAILED && target == SmartLockPasscodeStatus.ACTIVE)
                || (target == SmartLockPasscodeStatus.UNKNOWN
                    && (current == SmartLockPasscodeStatus.ACTIVE
                        || current == SmartLockPasscodeStatus.FAILED
                        || current == SmartLockPasscodeStatus.DELETED));
        if (conflict) {
            logger.warn(
                    "switchbot_passcode_terminal_conflict recordId={} taskId={} commandRef={} current={} incoming={}",
                    record.getId(), task.getId(), commandReference, current, target
            );
            return;
        }
        if (current != null && current == target && task.getStatus() == providerResult.status()) {
            logger.info(
                    "switchbot_passcode_webhook_noop recordId={} taskId={} commandRef={} status={}",
                    record.getId(), task.getId(), commandReference, target
            );
            return;
        }
        completeTask(task, providerResult);
        applyTaskResultToPasscodeRecord(task, providerResult);
        logger.info(
                "switchbot_passcode_state_updated recordId={} taskId={} commandRef={} from={} to={} taskStatus={}",
                record != null ? record.getId() : null,
                task.getId(),
                commandReference,
                current,
                record != null ? record.getStatus() : null,
                task.getStatus()
        );
    }

    private SmartLockPasscodeStatus switchBotWebhookPasscodeStatus(
            SmartLockTaskType taskType,
            SmartLockTaskStatus taskStatus
    ) {
        if (taskType == SmartLockTaskType.DELETE_PASSCODE) {
            if (taskStatus == SmartLockTaskStatus.SUCCESS) {
                return SmartLockPasscodeStatus.DELETED;
            }
            return taskStatus == SmartLockTaskStatus.FAILED
                    ? SmartLockPasscodeStatus.FAILED
                    : SmartLockPasscodeStatus.UNKNOWN;
        }
        if (taskStatus == SmartLockTaskStatus.SUCCESS) {
            return SmartLockPasscodeStatus.ACTIVE;
        }
        return taskStatus == SmartLockTaskStatus.FAILED
                ? SmartLockPasscodeStatus.FAILED
                : SmartLockPasscodeStatus.UNKNOWN;
    }

    private SmartLockTaskStatus resolveSwitchBotWebhookResult(String result) {
        if (!hasText(result)) {
            return null;
        }
        return switch (result.trim().toLowerCase(Locale.ROOT)) {
            case "success", "succeeded" -> SmartLockTaskStatus.SUCCESS;
            case "failed", "failure" -> SmartLockTaskStatus.FAILED;
            case "timeout", "timed_out", "timedout" -> SmartLockTaskStatus.UNKNOWN;
            default -> null;
        };
    }

    private String firstJsonText(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : SmartLockMaskingUtils.trimToNull(value.asText());
    }

    private String safeReference(String value) {
        if (!hasText(value)) {
            return null;
        }
        String hash = credentialCrypto.sha256Hex(value);
        return hash.substring(0, Math.min(12, hash.length()));
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
            if (result.status() == SmartLockTaskStatus.SUCCESS
                    && commandTarget.provider() == SmartLockProvider.TTLOCK) {
                refreshTtLockStatusAfterLockCommand(commandTarget, task);
            }
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
            throw new IllegalStateException(ApiMessages.get("api.t.274959361d28"), ex);
        }
    }

    private SmartLockCredentialData decryptCredentials(SmartLockIntegration integration) {
        try {
            String json = credentialCrypto.decrypt(integration.getCredentialCiphertext());
            SmartLockCredentialData data = objectMapper.readValue(json, SmartLockCredentialData.class);
            data.setProvider(integration.getProvider());
            return data;
        } catch (Exception ex) {
            throw new IllegalStateException(ApiMessages.get("api.t.c184155aaba6"), ex);
        }
    }

    private SmartLockCredentialData ensureProviderToken(SmartLockIntegration integration) {
        SmartLockCredentialData credentials = decryptCredentials(integration);
        if (credentials.getProvider() == SmartLockProvider.TTLOCK && credentials.shouldRefreshTtLockToken(now())) {
            SmartLockProviderClient client = providerRegistry.getClient(SmartLockProvider.TTLOCK);
            try {
                credentials = client.refreshToken(credentials);
            } catch (RuntimeException ex) {
                logTtLockTokenFailure(integration, "ensureProviderToken", ex);
                throw ex;
            }
            persistCredentials(integration, credentials);
            integrationRepository.save(integration);
        }
        return credentials;
    }

    private SmartLockDevice resolveDevice(Long storeId, SmartLockRequests.CreateBindingRequest request) {
        if (request.getDeviceId() != null) {
            return deviceRepository.findByStoreIdAndId(storeId, request.getDeviceId())
                    .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.0e234935fb51")));
        }
        SmartLockProvider provider = requireProvider(request.getProvider());
        String providerLockId = SmartLockMaskingUtils.trimToNull(request.getProviderLockId());
        if (!hasText(providerLockId)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.37e2b9e6cd0c"));
        }
        return deviceRepository.findByStoreIdAndProviderAndProviderLockId(storeId, provider, providerLockId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.68ea069b0475")));
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
                throw new IllegalArgumentException(ApiMessages.get("api.t.50e763040b84"));
            }
            return new BindingRoleSelection(controlDevice, passcodeDevice);
        }

        SmartLockDevice controlDevice = resolveRoleDevice(
                storeId,
                request,
                request.getControlDeviceId(),
                request.getControlProviderLockId(),
                ApiMessages.get("api.t.f2793eec1ceb")
        );
        SmartLockDevice passcodeDevice = resolveRoleDevice(
                storeId,
                request,
                request.getPasscodeDeviceId(),
                request.getPasscodeProviderLockId(),
                ApiMessages.get("api.t.7ba8def67123")
        );
        if (controlDevice == null && passcodeDevice == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.788917da1d6d"));
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
                    .orElseThrow(() -> new IllegalArgumentException(roleName + ApiMessages.get("api.t.0d864b52e306")));
            if (hasText(providerLockId) && !providerLockId.equals(device.getProviderLockId())) {
                throw new IllegalArgumentException(roleName + ApiMessages.get("api.t.ddb6fb0a9180"));
            }
        } else {
            SmartLockProvider provider = resolveProviderHint(storeId, request);
            device = deviceRepository.findByStoreIdAndProviderAndProviderLockId(storeId, provider, providerLockId)
                    .orElseThrow(() -> new IllegalArgumentException(roleName + ApiMessages.get("api.t.d7326aa71d04")));
        }

        if (request.getProvider() != null && request.getProvider() != device.getProvider()) {
            throw new IllegalArgumentException(roleName + ApiMessages.get("api.t.fbb618ad06f0"));
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
        throw new IllegalArgumentException(ApiMessages.get("api.t.2e4236e0a17f"));
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
                throw new IllegalArgumentException(ApiMessages.get("api.t.4e288611826e"));
            }
        }
        if (request.getIntegrationId() != null) {
            SmartLockIntegration requestedIntegration = requireIntegration(storeId, request.getIntegrationId());
            if (integration == null || !requestedIntegration.getId().equals(integration.getId())) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.f1fc544da457"));
            }
        }
        if (integration == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2091368fecdd"));
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
                throw new IllegalArgumentException(ApiMessages.get("api.t.2d8423627cf0"));
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
                throw new IllegalArgumentException(ApiMessages.get("api.t.03283a2ae1a8"));
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

    private void validateRoomProviderExclusivity(
            Optional<SmartLockRoomBinding> existingBinding,
            SmartLockIntegration integration
    ) {
        if (existingBinding.isEmpty() || integration == null) {
            return;
        }

        SmartLockProvider existingProvider = existingBinding.get().getProvider();
        SmartLockProvider requestedProvider = integration.getProvider();
        if (existingProvider == null || existingProvider == requestedProvider) {
            return;
        }

        throw new IllegalArgumentException(
                ApiMessages.get("api.t.f62abea327bf") + providerDisplayName(existingProvider)
                        + ApiMessages.get("api.t.d5f9a795837d") + providerDisplayName(requestedProvider)
        );
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.b3bc13644d54"));
        }

        if (roleResolver.isSwitchBotAuthenticationPanel(passcodeDevice)) {
            String linkedControlProviderLockId = roleResolver.linkedControlProviderLockId(passcodeDevice);
            if (controlDevice != null) {
                if (!hasText(linkedControlProviderLockId)) {
                    throw new IllegalArgumentException(ApiMessages.get("api.t.fd6442845a4c"));
                }
                if (!linkedControlProviderLockId.equals(controlDevice.getProviderLockId())) {
                    throw new IllegalArgumentException(ApiMessages.get("api.t.350e71307893"));
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.75bacb262488"));
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
                throw new IllegalArgumentException(ApiMessages.get("api.t.b9b419295cc6"));
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.aec3f2a5ff96"));
        }
        String token = SmartLockMaskingUtils.trimToNull(request.getConfirmToken());
        if (!hasText(token)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.f5721be3cc84"));
        }
        String tokenHash = credentialCrypto.sha256Hex(token);
        SmartLockConfirmation confirmation = confirmationRepository.findByStoreIdAndTokenHash(storeId, tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.50d74628b50e")));
        if (!userId.equals(confirmation.getCreatedBy())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d2090a3620ee"));
        }
        if (confirmation.getUsedAt() != null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.c8f46a569336"));
        }
        if (!confirmation.getExpiresAt().isAfter(now())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.87f7c9655591"));
        }
        if (!roomId.equals(confirmation.getRoom().getId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.1ddc5d843218"));
        }
        if (!binding.getId().equals(confirmation.getBinding().getId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.983255ed239f"));
        }
        if (confirmation.getAction() != action) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.c044e75b5315"));
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.f78c3dad32e5"));
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

    private boolean shouldRefreshPendingTask(SmartLockTask task) {
        if (task.getStatus() != SmartLockTaskStatus.PENDING) {
            return false;
        }
        if (hasText(task.getProviderTaskId())) {
            return true;
        }
        return task.getProvider() == SmartLockProvider.TTLOCK && task.getPasscodeRecord() != null;
    }

    private SmartLockTtLockClient.TtLockPasscodeCommandResult createTtLockPeriodPasscode(
            SmartLockProviderClient client,
            SmartLockCredentialData credentials,
            String providerLockId,
            SmartLockProviderClient.PasscodeCommand command,
            String keyboardPwdVersion
    ) {
        if (client instanceof SmartLockTtLockClient ttLockClient) {
            return ttLockClient.createPeriodPasscode(credentials, providerLockId, command, keyboardPwdVersion);
        }
        throw new IllegalStateException(ApiMessages.get("api.t.8fa33190a6dd"));
    }

    private String resolveTtLockKeyboardPwdVersion(RoleTarget target) {
        if (target == null || target.provider() != SmartLockProvider.TTLOCK) {
            return TTLOCK_DEFAULT_KEYBOARD_PWD_VERSION;
        }
        String version = readRawDeviceField(target.device(), "keyboardPwdVersion");
        return fallback(version, TTLOCK_DEFAULT_KEYBOARD_PWD_VERSION);
    }

    private void refreshControlDeviceStatus(RoleTarget target, LocalDateTime statusTime) {
        SmartLockDevice device = target.device();
        SmartLockProviderClient client = providerRegistry.getClient(target.provider());
        SmartLockCredentialData credentials = ensureProviderToken(target.integration());
        if (hasText(target.providerLockId()) && device != null) {
            SmartLockProviderClient.LockStatusSnapshot snapshot = client.getStatus(credentials, target.providerLockId());
            device.setLockStatus(snapshot.lockStatus());
            device.setBattery(snapshot.battery());
            device.setOnline(snapshot.online());
            device.setRawDataJson(snapshot.rawJson());
            device.setLastStatusAt(statusTime);
        } else if (device != null) {
            clearDeviceStatus(device);
        }
        if (device != null) {
            deviceRepository.save(device);
        }
    }

    private void refreshTtLockStatusAfterLockCommand(RoleTarget target, SmartLockTask task) {
        try {
            refreshControlDeviceStatus(target, now());
        } catch (RuntimeException ex) {
            String message = ApiMessages.get(TTLOCK_STATUS_REFRESH_FAILED_PREFIX_KEY) + safeError(ex);
            task.setResultMessage(appendTaskResultMessage(task.getResultMessage(), message));
            taskRepository.save(task);
            logger.warn(
                    "TTLock status refresh after command failed taskId={} providerLockIdSuffix={} errorClass={}",
                    task.getId(),
                    identifierSuffix(target.providerLockId()),
                    ex.getClass().getSimpleName()
            );
        }
    }

    private SmartLockProviderClient.ProviderTaskResult normalizeCreatePasscodeResult(
            SmartLockProvider provider,
            SmartLockProviderClient.ProviderTaskResult result
    ) {
        if (provider == SmartLockProvider.TTLOCK
                && result.status() == SmartLockTaskStatus.SUCCESS
                && !hasText(result.providerPasscodeId())) {
            return new SmartLockProviderClient.ProviderTaskResult(
                    SmartLockTaskStatus.PENDING,
                    result.providerTaskId(),
                    null,
                    ApiMessages.get(TTLOCK_NO_ID_CREATE_PENDING_MESSAGE_KEY)
            );
        }
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
                ApiMessages.get(SWITCHBOT_NO_ID_CREATE_PENDING_MESSAGE_KEY)
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
                ApiMessages.get(LOCAL_NO_REMOTE_PASSCODE_CLEANUP_MESSAGE_KEY)
        );
        logPasscodeDeleteLocalPath("noRemoteIdLocalCleanup", record);
    }

    private void reconcileTtLockPasscodeRecords(
            Long storeId,
            RoleTarget target,
            List<SmartLockPasscodeRecord> records
    ) {
        if (records == null || records.isEmpty()) {
            return;
        }

        SmartLockProviderClient client = providerRegistry.getClient(target.provider());
        SmartLockCredentialData credentials = ensureProviderToken(target.integration());
        List<SmartLockTtLockClient.TtLockPasscodeSnapshot> snapshots =
                loadTtLockPasscodeSnapshots(client, credentials, target.providerLockId());
        Map<String, SmartLockTtLockClient.TtLockPasscodeSnapshot> snapshotsById = new HashMap<>();
        for (SmartLockTtLockClient.TtLockPasscodeSnapshot snapshot : snapshots) {
            if (hasText(snapshot.providerPasscodeId())) {
                snapshotsById.put(snapshot.providerPasscodeId(), snapshot);
            }
        }

        for (SmartLockPasscodeRecord record : records) {
            if (!isTtLockRecordForTarget(record, target)) {
                continue;
            }

            SmartLockTtLockClient.TtLockPasscodeSnapshot snapshot =
                    findTtLockSnapshotForRecord(record, snapshotsById, snapshots);
            if (record.getStatus() == SmartLockPasscodeStatus.DELETE_PENDING) {
                applyTtLockDeletePendingSync(record, snapshot);
                continue;
            }

            if (snapshot != null) {
                applyTtLockSnapshotToRecord(record, snapshot);
                continue;
            }

            if (record.getStatus() == SmartLockPasscodeStatus.PENDING) {
                failTtLockPendingPasscode(record, ApiMessages.get(TTLOCK_PASSCODE_SYNC_NO_MATCH_MESSAGE_KEY));
            }
        }
    }

    private List<SmartLockTtLockClient.TtLockPasscodeSnapshot> loadTtLockPasscodeSnapshots(
            SmartLockProviderClient client,
            SmartLockCredentialData credentials,
            String providerLockId
    ) {
        if (client instanceof SmartLockTtLockClient ttLockClient) {
            return ttLockClient.listKeyboardPasscodes(credentials, providerLockId);
        }

        List<SmartLockTtLockClient.TtLockPasscodeSnapshot> snapshots = new ArrayList<>();
        for (SmartLockProviderClient.ProviderPasscodeSnapshot snapshot : client.listPasscodes(credentials, providerLockId)) {
            snapshots.add(new SmartLockTtLockClient.TtLockPasscodeSnapshot(
                    snapshot.providerPasscodeId(),
                    snapshot.passcodeName(),
                    snapshot.status(),
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return snapshots;
    }

    private boolean isTtLockRecordForTarget(SmartLockPasscodeRecord record, RoleTarget target) {
        if (record == null || record.getProvider() != SmartLockProvider.TTLOCK) {
            return false;
        }
        String recordProviderLockId = firstText(record.getPasscodeProviderLockId(), record.getProviderLockId());
        return hasText(recordProviderLockId) && recordProviderLockId.equals(target.providerLockId());
    }

    private SmartLockTtLockClient.TtLockPasscodeSnapshot findTtLockSnapshotForRecord(
            SmartLockPasscodeRecord record,
            Map<String, SmartLockTtLockClient.TtLockPasscodeSnapshot> snapshotsById,
            List<SmartLockTtLockClient.TtLockPasscodeSnapshot> snapshots
    ) {
        if (hasText(record.getProviderPasscodeId())) {
            return snapshotsById.get(record.getProviderPasscodeId());
        }

        SmartLockTtLockClient.TtLockPasscodeSnapshot match = null;
        for (SmartLockTtLockClient.TtLockPasscodeSnapshot snapshot : snapshots) {
            if (!hasText(snapshot.passcode())) {
                continue;
            }
            Long roomId = record.getRoom() != null ? record.getRoom().getId() : null;
            if (roomId == null) {
                continue;
            }
            String snapshotHash = passcodeHash(record.getStoreId(), roomId, snapshot.passcode());
            if (!snapshotHash.equals(record.getPasscodeHash())) {
                continue;
            }
            if (match != null) {
                return null;
            }
            match = snapshot;
        }
        return match;
    }

    private void applyTtLockSnapshotToRecord(
            SmartLockPasscodeRecord record,
            SmartLockTtLockClient.TtLockPasscodeSnapshot snapshot
    ) {
        SmartLockPasscodeStatus previousStatus = record.getStatus();
        if (hasText(snapshot.providerPasscodeId())) {
            record.setProviderPasscodeId(snapshot.providerPasscodeId());
        }
        if (hasText(snapshot.passcodeName())) {
            record.setPasscodeName(snapshot.passcodeName());
        }
        if (hasText(snapshot.passcode())) {
            Long roomId = record.getRoom() != null ? record.getRoom().getId() : null;
            if (roomId != null) {
                record.setPasscodeMasked(SmartLockMaskingUtils.maskPasscode(snapshot.passcode()));
                record.setPasscodeHash(passcodeHash(record.getStoreId(), roomId, snapshot.passcode()));
            }
        }
        if (snapshot.validFrom() != null) {
            record.setValidFrom(snapshot.validFrom());
        }
        if (snapshot.validUntil() != null) {
            record.setValidUntil(snapshot.validUntil());
        }

        SmartLockPasscodeStatus remoteStatus = mapTtLockRemotePasscodeStatus(snapshot.status());
        record.setStatus(remoteStatus);
        if (remoteStatus == SmartLockPasscodeStatus.DELETED) {
            record.setDeletedAt(now());
            record.setLastError(null);
        } else if (remoteStatus == SmartLockPasscodeStatus.FAILED) {
            record.setLastError(ApiMessages.get("api.t.5d4329bfe719") + fallback(snapshot.status(), "unknown"));
        } else {
            record.setLastError(null);
        }
        passcodeRepository.save(record);

        if (previousStatus == SmartLockPasscodeStatus.PENDING
                && remoteStatus == SmartLockPasscodeStatus.ACTIVE) {
            completePendingPasscodeTasks(
                    record,
                    SmartLockTaskType.CREATE_PASSCODE,
                    SmartLockTaskStatus.SUCCESS,
                    record.getProviderPasscodeId(),
                    ApiMessages.get("api.t.00b41601cf00")
            );
        } else if (previousStatus == SmartLockPasscodeStatus.PENDING
                && remoteStatus == SmartLockPasscodeStatus.FAILED) {
            completePendingPasscodeTasks(
                    record,
                    SmartLockTaskType.CREATE_PASSCODE,
                    SmartLockTaskStatus.FAILED,
                    record.getProviderPasscodeId(),
                    record.getLastError()
            );
        } else if (previousStatus == SmartLockPasscodeStatus.PENDING
                && remoteStatus == SmartLockPasscodeStatus.DELETED) {
            completePendingPasscodeTasks(
                    record,
                    SmartLockTaskType.CREATE_PASSCODE,
                    SmartLockTaskStatus.FAILED,
                    record.getProviderPasscodeId(),
                    ApiMessages.get("api.t.0c0bbcd6b1f0")
            );
        } else if (previousStatus == SmartLockPasscodeStatus.PENDING
                && remoteStatus == SmartLockPasscodeStatus.DELETE_PENDING) {
            completePendingPasscodeTasks(
                    record,
                    SmartLockTaskType.CREATE_PASSCODE,
                    SmartLockTaskStatus.FAILED,
                    record.getProviderPasscodeId(),
                    ApiMessages.get(TTLOCK_DELETE_IN_PROGRESS_MESSAGE_KEY)
            );
        }
    }

    private void applyTtLockDeletePendingSync(
            SmartLockPasscodeRecord record,
            SmartLockTtLockClient.TtLockPasscodeSnapshot snapshot
    ) {
        SmartLockPasscodeStatus remoteStatus = snapshot == null
                ? SmartLockPasscodeStatus.DELETED
                : mapTtLockRemotePasscodeStatus(snapshot.status());
        if (remoteStatus == SmartLockPasscodeStatus.DELETED) {
            record.setStatus(SmartLockPasscodeStatus.DELETED);
            record.setDeletedAt(now());
            record.setLastError(null);
            passcodeRepository.save(record);
            completePendingPasscodeTasks(
                    record,
                    SmartLockTaskType.DELETE_PASSCODE,
                    SmartLockTaskStatus.SUCCESS,
                    record.getProviderPasscodeId(),
                    ApiMessages.get("api.t.c06012895456")
            );
            return;
        }

        if (remoteStatus == SmartLockPasscodeStatus.DELETE_PENDING
                || remoteStatus == SmartLockPasscodeStatus.PENDING) {
            record.setStatus(SmartLockPasscodeStatus.DELETE_PENDING);
            record.setLastError(ApiMessages.get(TTLOCK_DELETE_IN_PROGRESS_MESSAGE_KEY));
            passcodeRepository.save(record);
            return;
        }

        record.setStatus(SmartLockPasscodeStatus.FAILED);
        record.setLastError(ApiMessages.get(TTLOCK_DELETE_STILL_EXISTS_MESSAGE_KEY));
        passcodeRepository.save(record);
        completePendingPasscodeTasks(
                record,
                SmartLockTaskType.DELETE_PASSCODE,
                SmartLockTaskStatus.FAILED,
                record.getProviderPasscodeId(),
                ApiMessages.get(TTLOCK_DELETE_STILL_EXISTS_MESSAGE_KEY)
        );
    }

    private void failTtLockPendingPasscode(SmartLockPasscodeRecord record, String message) {
        record.setStatus(SmartLockPasscodeStatus.FAILED);
        record.setLastError(message);
        passcodeRepository.save(record);
        completePendingPasscodeTasks(
                record,
                SmartLockTaskType.CREATE_PASSCODE,
                SmartLockTaskStatus.FAILED,
                record.getProviderPasscodeId(),
                message
        );
    }

    private SmartLockPasscodeStatus mapTtLockRemotePasscodeStatus(String status) {
        if (!hasText(status)) {
            return SmartLockPasscodeStatus.ACTIVE;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (TTLOCK_PASSCODE_STATUS_ACTIVE.equals(normalized)) {
            return SmartLockPasscodeStatus.ACTIVE;
        }
        if (TTLOCK_PASSCODE_STATUS_INVALID.equals(normalized)
                || TTLOCK_PASSCODE_STATUS_ADD_FAILED.equals(normalized)
                || TTLOCK_PASSCODE_STATUS_DELETE_FAILED.equals(normalized)) {
            return SmartLockPasscodeStatus.FAILED;
        }
        if (TTLOCK_PASSCODE_STATUS_PENDING.equals(normalized)
                || TTLOCK_PASSCODE_STATUS_ADDING.equals(normalized)) {
            return SmartLockPasscodeStatus.PENDING;
        }
        if (TTLOCK_PASSCODE_STATUS_DELETING.equals(normalized)) {
            return SmartLockPasscodeStatus.DELETE_PENDING;
        }
        if (normalized.contains("fail")
                || normalized.contains("error")
                || normalized.contains("invalid")
                || normalized.contains("abnormal")) {
            return SmartLockPasscodeStatus.FAILED;
        }
        if (normalized.contains("deleting")
                || normalized.contains("delete_pending")
                || normalized.contains("delete pending")) {
            return SmartLockPasscodeStatus.DELETE_PENDING;
        }
        if (normalized.contains("delete")
                || normalized.contains("deleted")
                || normalized.contains("remove")
                || normalized.contains("removed")
                || normalized.contains("cancel")) {
            return SmartLockPasscodeStatus.DELETED;
        }
        if (normalized.contains("pending")
                || normalized.contains("processing")
                || normalized.contains("adding")) {
            return SmartLockPasscodeStatus.PENDING;
        }
        if (isNumericStatus(normalized)) {
            return SmartLockPasscodeStatus.FAILED;
        }
        return SmartLockPasscodeStatus.ACTIVE;
    }

    private boolean isNumericStatus(String status) {
        for (int i = 0; i < status.length(); i++) {
            if (!Character.isDigit(status.charAt(i))) {
                return false;
            }
        }
        return !status.isEmpty();
    }

    private void completePendingPasscodeTasks(
            SmartLockPasscodeRecord record,
            SmartLockTaskType taskType,
            SmartLockTaskStatus status,
            String providerPasscodeId,
            String message
    ) {
        List<SmartLockTask> tasks = new ArrayList<>();
        if (hasText(record.getProviderTaskId())) {
            tasks.addAll(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                    record.getProvider(),
                    record.getProviderTaskId()
            ));
        }
        tasks.addAll(taskRepository.findPasscodeTasksWithoutProviderTaskId(
                record.getStoreId(),
                record.getId(),
                taskType,
                SmartLockTaskStatus.PENDING
        ));

        for (SmartLockTask task : tasks) {
            if (!isMatchingPendingPasscodeTask(task, record, taskType)) {
                continue;
            }
            SmartLockProviderClient.ProviderTaskResult result =
                    new SmartLockProviderClient.ProviderTaskResult(
                            status,
                            task.getProviderTaskId(),
                            providerPasscodeId,
                            message
                    );
            completeTask(task, result);
        }
    }

    private boolean isMatchingPendingPasscodeTask(
            SmartLockTask task,
            SmartLockPasscodeRecord record,
            SmartLockTaskType taskType
    ) {
        if (task == null || task.getStatus() != SmartLockTaskStatus.PENDING) {
            return false;
        }
        if (task.getTaskType() != taskType) {
            return false;
        }
        if (!record.getStoreId().equals(task.getStoreId())) {
            return false;
        }
        SmartLockPasscodeRecord taskRecord = task.getPasscodeRecord();
        return taskRecord != null && record.getId().equals(taskRecord.getId());
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
                "smart_lock_passcode_provider_call_requested command={} recordId={} taskId={} provider={} "
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
                "smart_lock_passcode_provider_response command={} recordId={} taskId={} provider={} "
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
                "smart_lock_passcode_terminal_decision command={} recordId={} taskId={} provider={} "
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
                task.getStatus(),
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
            if (task.getProvider() == SmartLockProvider.TTLOCK) {
                reconcileTtLockPasscodeRecords(task.getStoreId(), target, List.of(task.getPasscodeRecord()));
                return;
            }
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
        } else if (result.status() == SmartLockTaskStatus.UNKNOWN) {
            record.setStatus(SmartLockPasscodeStatus.UNKNOWN);
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
        } else if (result.status() == SmartLockTaskStatus.UNKNOWN) {
            record.setStatus(SmartLockPasscodeStatus.UNKNOWN);
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
        if (device.getProvider() == SmartLockProvider.TTLOCK) {
            applyTtLockSyncedStatus(device, snapshot, client, credentials, syncTime);
            return;
        }

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

    private void applyTtLockSyncedStatus(
            SmartLockDevice device,
            SmartLockProviderClient.DeviceSnapshot snapshot,
            SmartLockProviderClient client,
            SmartLockCredentialData credentials,
            LocalDateTime syncTime
    ) {
        device.setBattery(snapshot.battery());
        device.setLockStatus(null);
        device.setOnline(null);
        device.setLastStatusAt(null);
        try {
            SmartLockProviderClient.LockStatusSnapshot status =
                    client.getStatus(credentials, snapshot.providerLockId());
            device.setBattery(status.battery() != null ? status.battery() : snapshot.battery());
            device.setLockStatus(status.lockStatus());
            device.setOnline(status.online());
            device.setLastStatusAt(syncTime);
        } catch (RuntimeException ex) {
            device.setLockStatus(null);
            device.setOnline(null);
            device.setLastStatusAt(null);
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
                throw new IllegalArgumentException(ApiMessages.get("api.t.c516198cd0e1"));
            }
            String providerLockId = firstText(binding.getControlProviderLockId(), controlDevice.getProviderLockId());
            if (!hasText(providerLockId)) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.0b23224b4294"));
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
                throw new IllegalArgumentException(ApiMessages.get("api.t.1297b07142b3"));
            }
        }

        throw new IllegalArgumentException(ApiMessages.get("api.t.51c47107ffb4"));
    }

    private RoleTarget requirePasscodeTarget(SmartLockRoomBinding binding) {
        SmartLockDevice passcodeDevice = binding.getPasscodeDevice();
        if (passcodeDevice != null) {
            if (!roleResolver.supportsPasscode(passcodeDevice)) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.988186454749"));
            }
            String providerLockId = firstText(binding.getPasscodeProviderLockId(), passcodeDevice.getProviderLockId());
            if (!hasText(providerLockId)) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.e5413f2ab4ce"));
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

        throw new IllegalArgumentException(ApiMessages.get("api.t.26f9d785dbb8"));
    }

    private void requirePasscodeWriteEnabled(SmartLockProvider provider) {
        if (provider == SmartLockProvider.SWITCHBOT && !config.isSwitchBotPasscodeWriteEnabled()) {
            throw new IllegalStateException(SWITCHBOT_PASSCODE_WRITE_UNAVAILABLE_REASON_CODE);
        }
    }

    private SmartLockStatusDTO decoratePasscodeWriteCapability(
            SmartLockStatusDTO dto,
            SmartLockProvider provider
    ) {
        boolean enabled = provider != SmartLockProvider.SWITCHBOT
                || config.isSwitchBotPasscodeWriteEnabled();
        dto.setPasscodeWriteEnabled(enabled);
        dto.setReasonCode(enabled ? null : SWITCHBOT_PASSCODE_WRITE_UNAVAILABLE_REASON_CODE);
        return dto;
    }

    private RoleTarget requirePasscodeSnapshotTarget(Long storeId, SmartLockPasscodeRecord record) {
        if (record.getRoom() == null || record.getRoom().getId() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.38a24b117eef"));
        }
        if (!storeId.equals(record.getRoom().getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.6bce1281516f"));
        }
        if (record.getBinding() == null || !storeId.equals(record.getBinding().getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d4aacfe1851d"));
        }
        SmartLockIntegration integration = record.getIntegration();
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.8b92190c076b"));
        }
        if (record.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.5e3ed979ba1b"));
        }
        String providerLockId = firstText(record.getPasscodeProviderLockId(), record.getProviderLockId());
        if (!hasText(providerLockId)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.3f5e34cf3432"));
        }
        SmartLockDevice passcodeDevice = record.getPasscodeDevice();
        if (passcodeDevice != null) {
            validateDeviceConsistency(storeId, passcodeDevice, integration);
            if (!providerLockId.equals(passcodeDevice.getProviderLockId())) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.caf39e4fe8ff"));
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.a03d2710918c"));
        }
        return integrationRepository.findByStoreIdAndId(storeId, integrationId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.2091368fecdd")));
    }

    private Room requireRoom(Long storeId, Long roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.57c08b5c1084"));
        }
        return roomRepository.findByStoreIdAndId(storeId, roomId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.36d380d97cb5")));
    }

    private SmartLockRoomBinding requireBinding(Long storeId, Long bindingId) {
        if (bindingId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2e735271b487"));
        }
        return bindingRepository.findByStoreIdAndIdAndStatus(storeId, bindingId, SmartLockBindingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.7cf1346a8c2f")));
    }

    private SmartLockRoomBinding requireBindingForRoom(Long storeId, Long roomId, Long bindingId) {
        SmartLockRoomBinding binding;
        if (bindingId == null) {
            binding = bindingRepository.findByStoreIdAndRoomIdAndStatus(
                            storeId,
                            roomId,
                            SmartLockBindingStatus.ACTIVE
                    )
                    .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.de683e8dffd4")));
        } else {
            binding = requireBinding(storeId, bindingId);
        }
        if (binding.getRoom() == null || !roomId.equals(binding.getRoom().getId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2d4cdee4df39"));
        }
        return binding;
    }

    private void validateBindingConsistency(Long storeId, SmartLockRoomBinding binding) {
        if (binding == null || !storeId.equals(binding.getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.7cf1346a8c2f"));
        }
        if (binding.getStatus() != SmartLockBindingStatus.ACTIVE) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.c49f8a6500c9"));
        }
        if (binding.getRoom() == null || !storeId.equals(binding.getRoom().getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.41e474287408"));
        }
        SmartLockIntegration integration = binding.getIntegration();
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.72c8b0bfe2a8"));
        }
        if (binding.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.9b9e2d0fb478"));
        }
        if (binding.getControlDevice() != null) {
            validateRoleDeviceConsistency(
                    storeId,
                    binding.getControlDevice(),
                    integration,
                    binding.getControlProviderLockId(),
                    ApiMessages.get("api.t.f2793eec1ceb")
            );
        }
        if (binding.getPasscodeDevice() != null) {
            validateRoleDeviceConsistency(
                    storeId,
                    binding.getPasscodeDevice(),
                    integration,
                    binding.getPasscodeProviderLockId(),
                    ApiMessages.get("api.t.7ba8def67123")
            );
        }
        if (!hasRoleColumns(binding)) {
            validateDeviceConsistency(storeId, binding.getDevice(), integration);
            if (binding.getProvider() != binding.getDevice().getProvider()) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.df6b9d362f73"));
            }
            if (!binding.getProviderLockId().equals(binding.getDevice().getProviderLockId())) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.1728d536ebfa"));
            }
        } else if (binding.getControlDevice() == null && binding.getPasscodeDevice() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.463f34dd13f1"));
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.8c6fef49c175") + roleName + ApiMessages.get("api.t.c5e4faa15bc2"));
        }
    }

    private void ensureBindingCanBeSoftDeleted(Long storeId, SmartLockRoomBinding binding) {
        boolean hasRiskyPasscode = passcodeRepository.existsRiskyStatusForBinding(
                storeId,
                binding.getId(),
                BINDING_DELETE_RISKY_PASSCODE_STATUSES
        );
        if (hasRiskyPasscode) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.0378b4994dc8"));
        }
        boolean hasPendingTask = taskRepository.existsByBindingAndStatus(
                storeId,
                binding.getId(),
                SmartLockTaskStatus.PENDING
        );
        if (hasPendingTask) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.e4e2de05415a"));
        }
        boolean hasPendingConfirmation = confirmationRepository.existsUnfinishedForBinding(
                storeId,
                binding.getId(),
                now()
        );
        if (hasPendingConfirmation) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.cfec0b9dd839"));
        }
    }

    private void validateDeviceConsistency(
            Long storeId,
            SmartLockDevice device,
            SmartLockIntegration integration
    ) {
        if (device == null || !storeId.equals(device.getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.0e234935fb51"));
        }
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2091368fecdd"));
        }
        if (device.getIntegration() == null || !integration.getId().equals(device.getIntegration().getId())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.a1bc42fe2b92"));
        }
        if (device.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.35a89c605a44"));
        }
    }

    private SmartLockProvider requireProvider(SmartLockProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2e4236e0a17f"));
        }
        return provider;
    }

    private SmartLockTaskType requireLockAction(SmartLockTaskType action) {
        if (action == SmartLockTaskType.LOCK || action == SmartLockTaskType.UNLOCK) {
            return action;
        }
        throw new IllegalArgumentException(ApiMessages.get("api.t.bd35b5e40c3b"));
    }

    private String normalizePasscode(String requested) {
        String passcode = SmartLockMaskingUtils.trimToNull(requested);
        if (passcode == null) {
            return String.valueOf(GENERATED_PASSCODE_MIN + SECURE_RANDOM.nextInt(GENERATED_PASSCODE_BOUND));
        }
        if (!passcode.matches("\\d{" + MIN_PASSCODE_LENGTH + "," + MAX_PASSCODE_LENGTH + "}")) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.708e1941711b"));
        }
        return passcode;
    }

    private void validatePasscodeWindow(LocalDateTime validFrom, LocalDateTime validUntil, ZoneId storeZoneId) {
        if (validFrom == null || validUntil == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.f99b4d411e4e"));
        }
        if (!validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.bde6f1a7a9f4"));
        }
        if (!validUntil.isAfter(now(storeZoneId))) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.6a5e98d2c7b7"));
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
            throw new IllegalStateException(ApiMessages.get("api.t.bd8d0047526c"));
        }
        String actualToken = SmartLockMaskingUtils.trimToNull(token);
        if (!hasText(actualToken) || !constantTimeEquals(expectedToken, actualToken)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.71686837197b"));
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
        if (normalized.equals("timeout") || normalized.equals("timed_out") || normalized.equals("timedout")) {
            return SmartLockTaskStatus.UNKNOWN;
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

    private String passcodeHash(Long storeId, Long roomId, String passcode) {
        return credentialCrypto.sha256Hex(storeId + "|" + roomId + "|" + passcode);
    }

    private String pendingTtLockAutoPasscodeHash(
            Long storeId,
            Long userId,
            Long roomId,
            String idempotencyKey
    ) {
        return credentialCrypto.sha256Hex(
                TTLOCK_AUTO_PASSCODE_PENDING_HASH_PREFIX
                        + "|" + storeId
                        + "|" + userId
                        + "|" + roomId
                        + "|" + fallback(SmartLockMaskingUtils.trimToNull(idempotencyKey), "no-idempotency")
        );
    }

    private String appendTaskResultMessage(String existing, String message) {
        if (!hasText(existing)) {
            return message;
        }
        if (!hasText(message)) {
            return existing;
        }
        return existing + "；" + message;
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

    private String providerDisplayName(SmartLockProvider provider) {
        if (provider == SmartLockProvider.SWITCHBOT) {
            return "SwitchBot";
        }
        if (provider == SmartLockProvider.TTLOCK) {
            return "TTLock";
        }
        return provider != null ? provider.name() : ApiMessages.get("api.t.ec4dfef30233");
    }

    private void logTtLockTokenFailure(
            SmartLockIntegration integration,
            String operation,
            RuntimeException ex
    ) {
        if (integration == null || integration.getProvider() != SmartLockProvider.TTLOCK) {
            return;
        }

        String endpoint = null;
        String errcode = null;
        String errmsg = safeError(ex);
        String tokenOperation = operation;
        if (ex instanceof SmartLockTtLockClient.TtLockTokenException tokenException) {
            endpoint = tokenException.getEndpoint();
            errcode = tokenException.getErrcode();
            errmsg = safeProviderMessage(tokenException.getErrmsg());
            tokenOperation = fallback(tokenException.getOperation(), operation);
        }

        logger.warn(
                "TTLock token operation failed provider={} storeId={} integrationId={} operation={} "
                        + "endpoint={} errcode={} errmsg={} errorClass={}",
                SmartLockProvider.TTLOCK,
                integration.getStoreId(),
                integration.getId(),
                tokenOperation,
                endpoint,
                errcode,
                errmsg,
                ex.getClass().getSimpleName()
        );
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
