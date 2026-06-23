package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.demo.config.SmartLockConfig;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.SmartLockDeviceDTO;
import server.demo.dto.SmartLockIntegrationDTO;
import server.demo.dto.SmartLockPasscodeDTO;
import server.demo.dto.SmartLockRequests;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartLockServiceTest {
    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 26L;
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-21T10:00:00Z"), ZoneOffset.UTC);

    private SmartLockIntegrationRepository integrationRepository;
    private SmartLockDeviceRepository deviceRepository;
    private SmartLockRoomBindingRepository bindingRepository;
    private SmartLockConfirmationRepository confirmationRepository;
    private SmartLockPasscodeRecordRepository passcodeRepository;
    private SmartLockTaskRepository taskRepository;
    private RoomRepository roomRepository;
    private FakeProviderClient fakeProviderClient;
    private SmartLockService service;
    private ObjectMapper objectMapper;
    private SmartLockCredentialCrypto credentialCrypto;

    @BeforeEach
    void setUp() {
        integrationRepository = mock(SmartLockIntegrationRepository.class);
        deviceRepository = mock(SmartLockDeviceRepository.class);
        bindingRepository = mock(SmartLockRoomBindingRepository.class);
        confirmationRepository = mock(SmartLockConfirmationRepository.class);
        passcodeRepository = mock(SmartLockPasscodeRecordRepository.class);
        taskRepository = mock(SmartLockTaskRepository.class);
        roomRepository = mock(RoomRepository.class);
        fakeProviderClient = new FakeProviderClient();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        credentialCrypto = new SmartLockCredentialCrypto("test-smart-lock-key");

        SmartLockConfig config = mock(SmartLockConfig.class);
        when(config.getConfirmationTtlSeconds()).thenReturn(300L);
        when(config.getSwitchBotWebhookToken()).thenReturn("webhook-token");

        service = new SmartLockService(
                integrationRepository,
                deviceRepository,
                bindingRepository,
                confirmationRepository,
                passcodeRepository,
                taskRepository,
                roomRepository,
                new SmartLockProviderClientRegistry(List.of(fakeProviderClient)),
                credentialCrypto,
                new SmartLockMapper(),
                config,
                objectMapper,
                FIXED_CLOCK
        );

        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "owner"));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createBinding_shouldRejectRoomOutsideCurrentStore() {
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(99L);
        request.setDeviceId(10L);

        when(roomRepository.findByStoreIdAndId(STORE_ID, 99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.createBinding(request));
        verify(bindingRepository, never()).save(any(SmartLockRoomBinding.class));
    }

    @Test
    void createBinding_shouldRejectProviderLockIdOutsideCurrentStore() {
        Room room = room(1L, STORE_ID);
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(room.getId());
        request.setProvider(SmartLockProvider.SWITCHBOT);
        request.setProviderLockId("other-store-lock");

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(deviceRepository.findByStoreIdAndProviderAndProviderLockId(
                STORE_ID,
                SmartLockProvider.SWITCHBOT,
                "other-store-lock"
        )).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.createBinding(request));
        verify(bindingRepository, never()).save(any(SmartLockRoomBinding.class));
    }

    @Test
    void refreshToken_shouldRejectIntegrationOutsideCurrentStore() {
        when(integrationRepository.findByStoreIdAndId(STORE_ID, 77L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.refreshToken(77L));
        verify(integrationRepository, never()).save(any(SmartLockIntegration.class));
    }

    @Test
    void saveIntegration_shouldMaskCredentialsAndPersistEncryptedPayload() throws Exception {
        SmartLockRequests.UpsertIntegrationRequest request = new SmartLockRequests.UpsertIntegrationRequest();
        request.setProvider(SmartLockProvider.SWITCHBOT);
        request.setName("Front desk SwitchBot");
        request.setSwitchBotToken("switch-token-123456");
        request.setSwitchBotSecret("switch-secret-abcdef");

        when(integrationRepository.findByStoreIdAndProvider(STORE_ID, SmartLockProvider.SWITCHBOT))
                .thenReturn(Optional.empty());
        when(integrationRepository.save(any(SmartLockIntegration.class))).thenAnswer(invocation -> {
            SmartLockIntegration integration = invocation.getArgument(0);
            integration.setId(1L);
            return integration;
        });

        SmartLockIntegrationDTO dto = service.saveIntegration(request);
        String dtoJson = objectMapper.writeValueAsString(dto);

        assertEquals(Boolean.TRUE, dto.getCredentialsConfigured());
        assertFalse(dtoJson.contains("switch-token-123456"));
        assertFalse(dtoJson.contains("switch-secret-abcdef"));
        verify(integrationRepository).save(any(SmartLockIntegration.class));
    }

    @Test
    void testIntegration_shouldRedactProviderFailureFromMessageAndLastError() throws Exception {
        SmartLockIntegration integration = switchBotIntegration();
        fakeProviderClient.testConnectionFailure = new RuntimeException(
                "provider rejected token=tok_live_123 secret=sec_live_456 "
                        + "passcode=123456 password=plain_password"
        );

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        when(integrationRepository.save(any(SmartLockIntegration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockTestResultDTO result = service.testIntegration(integration.getId());

        assertFalse(result.getSuccess());
        assertFalse(result.getMessage().contains("tok_live_123"));
        assertFalse(result.getMessage().contains("sec_live_456"));
        assertFalse(result.getMessage().contains("123456"));
        assertFalse(result.getMessage().contains("plain_password"));
        assertFalse(integration.getLastError().contains("tok_live_123"));
        assertFalse(integration.getLastError().contains("sec_live_456"));
        assertFalse(integration.getLastError().contains("123456"));
        assertFalse(integration.getLastError().contains("plain_password"));
        assertTrue(result.getMessage().contains("[REDACTED]"));
        assertTrue(integration.getLastError().contains("[REDACTED]"));
    }

    @Test
    void syncDevices_shouldEnrichSmartLockAndKeypadFromStatusTargets() throws Exception {
        SmartLockIntegration integration = switchBotIntegration();
        fakeProviderClient.deviceSnapshots = List.of(
                new SmartLockProviderClient.DeviceSnapshot(
                        "smart-lock-1",
                        "Smart Door Lock",
                        "Smart Lock Pro Wifi",
                        null,
                        null,
                        null,
                        null,
                        "{}"
                ),
                new SmartLockProviderClient.DeviceSnapshot(
                        "keypad-1",
                        "Keypad Touch",
                        "Keypad Touch",
                        "lock-1",
                        null,
                        null,
                        true,
                        "{}"
                ),
                new SmartLockProviderClient.DeviceSnapshot(
                        "keypad-2",
                        "Keypad Without Lock",
                        "Keypad",
                        null,
                        null,
                        null,
                        true,
                        "{}"
                )
        );
        fakeProviderClient.statusSnapshots.put(
                "smart-lock-1",
                new SmartLockProviderClient.LockStatusSnapshot("locked", 88, true, "{}")
        );
        fakeProviderClient.statusSnapshots.put(
                "lock-1",
                new SmartLockProviderClient.LockStatusSnapshot("unlocked", 72, false, "{}")
        );

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        when(deviceRepository.findByStoreIdAndProviderAndProviderLockId(
                eq(STORE_ID),
                eq(SmartLockProvider.SWITCHBOT),
                anyString()
        )).thenReturn(Optional.empty());
        stubDeviceSaveWithIds();

        List<SmartLockDeviceDTO> devices = service.syncDevices(integration.getId());

        SmartLockDeviceDTO smartLock = findDevice(devices, "smart-lock-1");
        assertEquals("locked", smartLock.getLockStatus());
        assertEquals(88, smartLock.getBattery());
        assertEquals(Boolean.TRUE, smartLock.getOnline());
        assertEquals("DEVICE", smartLock.getStatusSource());
        assertEquals("smart-lock-1", smartLock.getStatusSourceDeviceId());

        SmartLockDeviceDTO keypad = findDevice(devices, "keypad-1");
        assertEquals("unlocked", keypad.getLockStatus());
        assertEquals(72, keypad.getBattery());
        assertEquals(Boolean.FALSE, keypad.getOnline());
        assertEquals("BOUND_LOCK", keypad.getStatusSource());
        assertEquals("lock-1", keypad.getStatusSourceDeviceId());

        SmartLockDeviceDTO keypadWithoutLock = findDevice(devices, "keypad-2");
        assertEquals(null, keypadWithoutLock.getLockStatus());
        assertEquals(null, keypadWithoutLock.getBattery());
        assertEquals(null, keypadWithoutLock.getOnline());
        assertEquals("UNAVAILABLE", keypadWithoutLock.getStatusSource());
        assertEquals(null, keypadWithoutLock.getStatusSourceDeviceId());
        assertEquals(List.of("smart-lock-1", "lock-1"), fakeProviderClient.statusRequests);
    }

    @Test
    void syncDevices_shouldKeepSwitchBotDeviceWhenStatusApiFails() throws Exception {
        SmartLockIntegration integration = switchBotIntegration();
        fakeProviderClient.deviceSnapshots = List.of(
                new SmartLockProviderClient.DeviceSnapshot(
                        "keypad-1",
                        "Keypad Touch",
                        "Keypad Touch",
                        "lock-1",
                        null,
                        null,
                        true,
                        "{}"
                )
        );
        fakeProviderClient.failedStatusDeviceIds.add("lock-1");

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        when(deviceRepository.findByStoreIdAndProviderAndProviderLockId(
                eq(STORE_ID),
                eq(SmartLockProvider.SWITCHBOT),
                anyString()
        )).thenReturn(Optional.empty());
        stubDeviceSaveWithIds();

        List<SmartLockDeviceDTO> devices = service.syncDevices(integration.getId());

        SmartLockDeviceDTO keypad = findDevice(devices, "keypad-1");
        assertEquals(null, keypad.getLockStatus());
        assertEquals(null, keypad.getBattery());
        assertEquals(null, keypad.getOnline());
        assertEquals(null, keypad.getLastStatusAt());
        assertEquals("BOUND_LOCK", keypad.getStatusSource());
        assertEquals("lock-1", keypad.getStatusSourceDeviceId());
        assertEquals(List.of("lock-1"), fakeProviderClient.statusRequests);
        verify(deviceRepository).save(any(SmartLockDevice.class));
        verify(integrationRepository).save(integration);
    }

    @Test
    void unlock_shouldRejectConfirmationForDifferentAction() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        SmartLockConfirmation confirmation = new SmartLockConfirmation();
        confirmation.setId(55L);
        confirmation.setStoreId(STORE_ID);
        confirmation.setRoom(room);
        confirmation.setBinding(binding);
        confirmation.setAction(SmartLockTaskType.LOCK);
        confirmation.setCreatedBy(USER_ID);
        confirmation.setExpiresAt(LocalDateTime.now(FIXED_CLOCK).plusMinutes(5));

        SmartLockRequests.LockOperationRequest request = new SmartLockRequests.LockOperationRequest();
        request.setConfirm(true);
        request.setConfirmToken("confirm-token");
        request.setBindingId(binding.getId());

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndIdAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(confirmationRepository.findByStoreIdAndTokenHash(eq(STORE_ID), anyString()))
                .thenReturn(Optional.of(confirmation));

        assertThrows(IllegalArgumentException.class, () -> service.unlock(room.getId(), request));
        verify(taskRepository, never()).save(any(SmartLockTask.class));
    }

    @Test
    void unlock_shouldReturnExistingTaskForDuplicateIdempotencyKeyWithoutProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        SmartLockTask existingTask = new SmartLockTask();
        existingTask.setId(200L);
        existingTask.setStoreId(STORE_ID);
        existingTask.setTaskType(SmartLockTaskType.UNLOCK);
        existingTask.setProvider(SmartLockProvider.SWITCHBOT);
        existingTask.setRoom(room);
        existingTask.setBinding(binding);
        existingTask.setStatus(SmartLockTaskStatus.SUCCESS);
        existingTask.setIdempotencyKey("dup-key");

        SmartLockRequests.LockOperationRequest request = new SmartLockRequests.LockOperationRequest();
        request.setConfirm(false);
        request.setBindingId(binding.getId());
        request.setIdempotencyKey("dup-key");

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndIdAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(taskRepository.findByStoreIdAndIdempotencyKey(STORE_ID, "dup-key"))
                .thenReturn(Optional.of(existingTask));

        SmartLockTaskDTO result = service.unlock(room.getId(), request);

        assertEquals(200L, result.getId());
        assertEquals(SmartLockTaskStatus.SUCCESS, result.getStatus());
        assertEquals(0, fakeProviderClient.unlockCalls);
        verify(confirmationRepository, never()).findByStoreIdAndTokenHash(eq(STORE_ID), anyString());
    }

    @Test
    void unlock_shouldUseBoundLockDeviceIdWhenSwitchBotKeypadIsBound() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        setBindingDevice(
                binding,
                "keypad-1",
                "Keypad Touch",
                "lock-1",
                "{\"lockDeviceId\":\"lock-1\",\"hubDeviceId\":\"hub-1\"}"
        );
        SmartLockRequests.LockOperationRequest request = lockOperationRequest(binding, "keypad-unlock");
        stubSuccessfulLockOperation(room, binding, SmartLockTaskType.UNLOCK, request.getIdempotencyKey());

        SmartLockTaskDTO result = service.unlock(room.getId(), request);

        assertEquals(SmartLockTaskStatus.SUCCESS, result.getStatus());
        assertEquals(List.of("lock-1"), fakeProviderClient.unlockRequests);
        assertEquals(1, fakeProviderClient.unlockCalls);
    }

    @Test
    void lock_shouldUseOwnDeviceIdWhenSwitchBotLockIsBound() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Lock");
        binding.getDevice().setAuxiliaryDeviceId("lock-from-keypad");
        binding.getDevice().setRawDataJson("{\"deviceId\":\"lock-1\",\"hubDeviceId\":\"hub-1\"}");
        SmartLockRequests.LockOperationRequest request = lockOperationRequest(binding, "lock-self");
        stubSuccessfulLockOperation(room, binding, SmartLockTaskType.LOCK, request.getIdempotencyKey());

        SmartLockTaskDTO result = service.lock(room.getId(), request);

        assertEquals(SmartLockTaskStatus.SUCCESS, result.getStatus());
        assertEquals(List.of("lock-1"), fakeProviderClient.lockRequests);
    }

    @Test
    void unlock_shouldRejectSwitchBotKeypadWithoutLockDeviceIdBeforeProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        setBindingDevice(binding, "keypad-1", "Keypad Vision", "hub-1", "{\"hubDeviceId\":\"hub-1\"}");
        SmartLockRequests.LockOperationRequest request = lockOperationRequest(binding, "keypad-missing-lock");

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndIdAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(taskRepository.findByStoreIdAndIdempotencyKey(STORE_ID, request.getIdempotencyKey()))
                .thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.unlock(room.getId(), request)
        );

        assertTrue(error.getMessage().contains("lockDeviceId"));
        assertEquals(List.of(), fakeProviderClient.unlockRequests);
        verify(confirmationRepository, never()).findByStoreIdAndTokenHash(eq(STORE_ID), anyString());
        verify(taskRepository, never()).save(any(SmartLockTask.class));
    }

    @Test
    void deletePasscode_shouldRejectRecordOutsideCurrentStore() {
        when(passcodeRepository.findByStoreIdAndId(STORE_ID, 301L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deletePasscode(301L));
        verify(taskRepository, never()).save(any(SmartLockTask.class));
        assertEquals(0, fakeProviderClient.deletePasscodeCalls);
    }

    @Test
    void deleteBinding_shouldRejectWhenRiskyPasscodeExists() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);

        when(bindingRepository.findByStoreIdAndIdAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.existsRiskyStatusForBinding(
                eq(STORE_ID),
                eq(binding.getId()),
                any()
        )).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteBinding(binding.getId())
        );

        assertTrue(error.getMessage().contains("密码"));
        verify(bindingRepository, never()).save(any(SmartLockRoomBinding.class));
    }

    @Test
    void deleteBinding_shouldSoftDeleteWhenNoLifecycleRisk() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);

        when(bindingRepository.findByStoreIdAndIdAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.existsRiskyStatusForBinding(eq(STORE_ID), eq(binding.getId()), any()))
                .thenReturn(false);
        when(taskRepository.existsByBindingAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockTaskStatus.PENDING
        )).thenReturn(false);
        when(confirmationRepository.existsUnfinishedForBinding(eq(STORE_ID), eq(binding.getId()), any()))
                .thenReturn(false);
        when(bindingRepository.save(any(SmartLockRoomBinding.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteBinding(binding.getId());

        assertEquals(SmartLockBindingStatus.DELETED, binding.getStatus());
        verify(bindingRepository).save(binding);
    }

    @Test
    void deletePasscode_shouldRejectDirtyRecordSnapshotBeforeProviderDelete() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.ACTIVE);
        record.setProviderPasscodeId("provider-passcode-1");
        record.setProviderLockId("stale-lock");

        when(passcodeRepository.findByStoreIdAndId(STORE_ID, record.getId())).thenReturn(Optional.of(record));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletePasscode(record.getId())
        );

        assertTrue(error.getMessage().contains("拒绝删除远程密码"));
        verify(taskRepository, never()).save(any(SmartLockTask.class));
        assertEquals(0, fakeProviderClient.deletePasscodeCalls);
    }

    @Test
    void handleSwitchBotWebhook_shouldActivateCreatePasscodeOnSuccess() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, "cmd-create-1");
        Map<String, Object> payload = Map.of(
                "eventName", "createKey",
                "data", Map.of(
                        "commandId", "cmd-create-1",
                        "result", "success",
                        "keyId", "key-101"
                )
        );

        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                "cmd-create-1"
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.handleSwitchBotWebhook("webhook-token", payload);

        assertEquals(Boolean.TRUE, result.get("processed"));
        assertEquals(SmartLockTaskStatus.SUCCESS, task.getStatus());
        assertEquals(SmartLockPasscodeStatus.ACTIVE, record.getStatus());
        assertEquals("key-101", record.getProviderPasscodeId());
        assertEquals("cmd-create-1", record.getProviderTaskId());
    }

    @Test
    void handleSwitchBotWebhook_shouldFailDeletePasscodeOnFailure() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.DELETE_PENDING);
        record.setProviderPasscodeId("provider-passcode-1");
        SmartLockTask task = task(401L, SmartLockTaskType.DELETE_PASSCODE, binding, record, "cmd-delete-1");
        Map<String, Object> payload = Map.of(
                "eventName", "deleteKey",
                "body", Map.of(
                        "commandId", "cmd-delete-1",
                        "result", "failed",
                        "message", "provider rejected command"
                )
        );

        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                "cmd-delete-1"
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.handleSwitchBotWebhook("webhook-token", payload);

        assertEquals(Boolean.TRUE, result.get("processed"));
        assertEquals(SmartLockTaskStatus.FAILED, task.getStatus());
        assertEquals(SmartLockPasscodeStatus.FAILED, record.getStatus());
        assertEquals("provider rejected command", record.getLastError());
    }

    @Test
    void handleSwitchBotWebhook_shouldRedactSensitiveProviderMessage() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.DELETE_PENDING);
        record.setProviderPasscodeId("provider-passcode-1");
        SmartLockTask task = task(401L, SmartLockTaskType.DELETE_PASSCODE, binding, record, "cmd-delete-sensitive");
        Map<String, Object> payload = Map.of(
                "eventName", "deleteKey",
                "body", Map.of(
                        "commandId", "cmd-delete-sensitive",
                        "result", "failed",
                        "message", "failed token=tok_live_123 clientSecret=client_secret_456 passcode=123456"
                )
        );

        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                "cmd-delete-sensitive"
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.handleSwitchBotWebhook("webhook-token", payload);

        assertEquals(Boolean.TRUE, result.get("processed"));
        assertFalse(task.getResultMessage().contains("tok_live_123"));
        assertFalse(task.getResultMessage().contains("client_secret_456"));
        assertFalse(task.getResultMessage().contains("123456"));
        assertFalse(record.getLastError().contains("tok_live_123"));
        assertFalse(record.getLastError().contains("client_secret_456"));
        assertFalse(record.getLastError().contains("123456"));
        assertTrue(task.getResultMessage().contains("[REDACTED]"));
        assertTrue(record.getLastError().contains("[REDACTED]"));
    }

    @Test
    void smartLockConfig_shouldNotFallbackToJwtSecretForEncryptionKey() {
        SmartLockConfig config = new SmartLockConfig();
        ReflectionTestUtils.setField(config, "encryptionKey", "");

        assertEquals("local-smart-lock-dev-encryption-key-change-me", config.getEncryptionKey());
        assertFalse(config.getEncryptionKey().contains("jwt"));

        ReflectionTestUtils.setField(config, "encryptionKey", "explicit-smart-lock-key");
        assertEquals("explicit-smart-lock-key", config.getEncryptionKey());
    }

    @Test
    void createPasscode_shouldRejectMissingValidityWindowBeforeProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockRequests.CreatePasscodeRequest request = new SmartLockRequests.CreatePasscodeRequest();
        request.setPasscode("123456");

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));

        assertThrows(IllegalArgumentException.class, () -> service.createPasscode(room.getId(), request));
        verify(passcodeRepository, never()).save(any(SmartLockPasscodeRecord.class));
        verify(taskRepository, never()).save(any(SmartLockTask.class));
        assertEquals(0, fakeProviderClient.createPasscodeCalls);
    }

    @Test
    void createPasscode_shouldRejectPlainSwitchBotLockDeviceBeforeProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Lock");
        SmartLockRequests.CreatePasscodeRequest request = passcodeRequest();

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));

        assertThrows(IllegalArgumentException.class, () -> service.createPasscode(room.getId(), request));
        verify(passcodeRepository, never()).save(any(SmartLockPasscodeRecord.class));
        verify(taskRepository, never()).save(any(SmartLockTask.class));
        assertEquals(0, fakeProviderClient.createPasscodeCalls);
    }

    @Test
    void createPasscode_shouldKeepPendingAndSuppressOneTimePasscodeForProviderPending() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        binding.getIntegration().setCredentialCiphertext(encryptedSwitchBotCredentials());
        fakeProviderClient.createPasscodeStatus = SmartLockTaskStatus.PENDING;
        SmartLockRequests.CreatePasscodeRequest request = passcodeRequest();
        request.setIdempotencyKey("create-key-1");

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(taskRepository.findByStoreIdAndIdempotencyKey(any(), anyString())).thenReturn(Optional.empty());
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> {
            SmartLockPasscodeRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(301L);
            }
            return record;
        });
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> {
            SmartLockTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(401L);
            }
            return task;
        });

        SmartLockPasscodeDTO result = service.createPasscode(room.getId(), request);

        assertEquals(SmartLockPasscodeStatus.PENDING, result.getStatus());
        assertEquals("provider-task-1", result.getProviderTaskId());
        assertEquals("provider-passcode-1", result.getProviderPasscodeId());
        assertEquals(null, result.getOneTimePasscode());
        assertEquals(1, fakeProviderClient.createPasscodeCalls);
    }

    @Test
    void getTask_shouldRejectTaskOutsideCurrentStore() {
        when(taskRepository.findByStoreIdAndId(STORE_ID, 902L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getTask(902L));
        assertEquals(0, fakeProviderClient.queryTaskCalls);
    }

    @Test
    void getTask_shouldUpdatePasscodeRecordWhenProviderTaskCompletes() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getIntegration().setCredentialCiphertext(encryptedSwitchBotCredentials());
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        record.setProviderPasscodeId("provider-passcode-1");
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, "provider-task-1");

        when(taskRepository.findByStoreIdAndId(STORE_ID, task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockTaskDTO result = service.getTask(task.getId());

        assertEquals(SmartLockTaskStatus.SUCCESS, result.getStatus());
        assertEquals(SmartLockPasscodeStatus.ACTIVE, record.getStatus());
        assertEquals(1, fakeProviderClient.queryTaskCalls);
    }

    private Room room(Long id, Long storeId) {
        Room room = new Room();
        room.setId(id);
        room.setStoreId(storeId);
        room.setRoomNumber("101");
        return room;
    }

    private SmartLockRoomBinding binding(Long id, Room room) {
        SmartLockIntegration integration = new SmartLockIntegration();
        integration.setId(10L);
        integration.setStoreId(STORE_ID);
        integration.setProvider(SmartLockProvider.SWITCHBOT);
        integration.setConnectionStatus(SmartLockIntegrationStatus.CONNECTED);

        SmartLockDevice device = new SmartLockDevice();
        device.setId(20L);
        device.setStoreId(STORE_ID);
        device.setIntegration(integration);
        device.setProvider(SmartLockProvider.SWITCHBOT);
        device.setProviderLockId("lock-1");
        device.setLockName("Room 101");

        SmartLockRoomBinding binding = new SmartLockRoomBinding();
        binding.setId(id);
        binding.setStoreId(STORE_ID);
        binding.setRoom(room);
        binding.setIntegration(integration);
        binding.setDevice(device);
        binding.setProvider(SmartLockProvider.SWITCHBOT);
        binding.setProviderLockId("lock-1");
        binding.setCreatedBy(USER_ID);
        binding.setStatus(SmartLockBindingStatus.ACTIVE);
        return binding;
    }

    private SmartLockPasscodeRecord passcodeRecord(
            Long id,
            SmartLockRoomBinding binding,
            SmartLockPasscodeStatus status
    ) {
        SmartLockPasscodeRecord record = new SmartLockPasscodeRecord();
        record.setId(id);
        record.setStoreId(STORE_ID);
        record.setRoom(binding.getRoom());
        record.setBinding(binding);
        record.setIntegration(binding.getIntegration());
        record.setProvider(binding.getProvider());
        record.setProviderLockId(binding.getProviderLockId());
        record.setPasscodeName("Guest");
        record.setPasscodeMasked("******");
        record.setPasscodeHash("hash");
        record.setValidFrom(LocalDateTime.now(FIXED_CLOCK).plusHours(1));
        record.setValidUntil(LocalDateTime.now(FIXED_CLOCK).plusHours(5));
        record.setStatus(status);
        record.setCreatedBy(USER_ID);
        return record;
    }

    private SmartLockTask task(
            Long id,
            SmartLockTaskType taskType,
            SmartLockRoomBinding binding,
            SmartLockPasscodeRecord record,
            String providerTaskId
    ) {
        SmartLockTask task = new SmartLockTask();
        task.setId(id);
        task.setStoreId(STORE_ID);
        task.setTaskType(taskType);
        task.setProvider(binding.getProvider());
        task.setRoom(binding.getRoom());
        task.setBinding(binding);
        task.setPasscodeRecord(record);
        task.setProviderTaskId(providerTaskId);
        task.setStatus(SmartLockTaskStatus.PENDING);
        task.setCreatedBy(USER_ID);
        return task;
    }

    private SmartLockRequests.CreatePasscodeRequest passcodeRequest() {
        SmartLockRequests.CreatePasscodeRequest request = new SmartLockRequests.CreatePasscodeRequest();
        request.setPasscodeName("Guest");
        request.setPasscode("123456");
        request.setValidFrom(LocalDateTime.now(FIXED_CLOCK).plusHours(1));
        request.setValidUntil(LocalDateTime.now(FIXED_CLOCK).plusHours(5));
        return request;
    }

    private SmartLockRequests.LockOperationRequest lockOperationRequest(
            SmartLockRoomBinding binding,
            String idempotencyKey
    ) {
        SmartLockRequests.LockOperationRequest request = new SmartLockRequests.LockOperationRequest();
        request.setConfirm(true);
        request.setConfirmToken("confirm-token");
        request.setBindingId(binding.getId());
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }

    private void setBindingDevice(
            SmartLockRoomBinding binding,
            String providerLockId,
            String deviceType,
            String auxiliaryDeviceId,
            String rawDataJson
    ) {
        binding.setProviderLockId(providerLockId);
        binding.getDevice().setProviderLockId(providerLockId);
        binding.getDevice().setDeviceType(deviceType);
        binding.getDevice().setAuxiliaryDeviceId(auxiliaryDeviceId);
        binding.getDevice().setRawDataJson(rawDataJson);
    }

    private void stubSuccessfulLockOperation(
            Room room,
            SmartLockRoomBinding binding,
            SmartLockTaskType action,
            String idempotencyKey
    ) throws Exception {
        binding.getIntegration().setCredentialCiphertext(encryptedSwitchBotCredentials());
        SmartLockConfirmation confirmation = new SmartLockConfirmation();
        confirmation.setId(55L);
        confirmation.setStoreId(STORE_ID);
        confirmation.setRoom(room);
        confirmation.setBinding(binding);
        confirmation.setAction(action);
        confirmation.setCreatedBy(USER_ID);
        confirmation.setExpiresAt(LocalDateTime.now(FIXED_CLOCK).plusMinutes(5));

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndIdAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(taskRepository.findByStoreIdAndIdempotencyKey(STORE_ID, idempotencyKey))
                .thenReturn(Optional.empty());
        when(confirmationRepository.findByStoreIdAndTokenHash(eq(STORE_ID), anyString()))
                .thenReturn(Optional.of(confirmation));
        when(confirmationRepository.save(any(SmartLockConfirmation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTaskSaveWithIds();
    }

    private String encryptedSwitchBotCredentials() throws Exception {
        SmartLockCredentialData credentials = new SmartLockCredentialData();
        credentials.setProvider(SmartLockProvider.SWITCHBOT);
        credentials.setSwitchBotToken("switch-token");
        credentials.setSwitchBotSecret("switch-secret");
        return credentialCrypto.encrypt(objectMapper.writeValueAsString(credentials));
    }

    private SmartLockIntegration switchBotIntegration() throws Exception {
        SmartLockIntegration integration = new SmartLockIntegration();
        integration.setId(10L);
        integration.setStoreId(STORE_ID);
        integration.setProvider(SmartLockProvider.SWITCHBOT);
        integration.setConnectionStatus(SmartLockIntegrationStatus.CONNECTED);
        integration.setCredentialCiphertext(encryptedSwitchBotCredentials());
        return integration;
    }

    private void stubDeviceSaveWithIds() {
        long[] nextId = {20L};
        when(deviceRepository.save(any(SmartLockDevice.class))).thenAnswer(invocation -> {
            SmartLockDevice device = invocation.getArgument(0);
            if (device.getId() == null) {
                device.setId(nextId[0]);
                nextId[0] += 1;
            }
            return device;
        });
    }

    private void stubTaskSaveWithIds() {
        long[] nextId = {401L};
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> {
            SmartLockTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(nextId[0]);
                nextId[0] += 1;
            }
            return task;
        });
    }

    private SmartLockDeviceDTO findDevice(List<SmartLockDeviceDTO> devices, String providerLockId) {
        for (SmartLockDeviceDTO device : devices) {
            if (providerLockId.equals(device.getProviderLockId())) {
                return device;
            }
        }
        throw new AssertionError("Device not found: " + providerLockId);
    }

    private static final class FakeProviderClient implements SmartLockProviderClient {
        private int unlockCalls;
        private int createPasscodeCalls;
        private int deletePasscodeCalls;
        private int queryTaskCalls;
        private RuntimeException testConnectionFailure;
        private final List<String> unlockRequests = new ArrayList<>();
        private final List<String> lockRequests = new ArrayList<>();
        private SmartLockTaskStatus createPasscodeStatus = SmartLockTaskStatus.SUCCESS;
        private List<DeviceSnapshot> deviceSnapshots = List.of();
        private final List<String> statusRequests = new ArrayList<>();
        private final Map<String, LockStatusSnapshot> statusSnapshots = new HashMap<>();
        private final Set<String> failedStatusDeviceIds = new HashSet<>();

        @Override
        public SmartLockProvider getProvider() {
            return SmartLockProvider.SWITCHBOT;
        }

        @Override
        public void testConnection(SmartLockCredentialData credentials) {
            if (testConnectionFailure != null) {
                throw testConnectionFailure;
            }
        }

        @Override
        public SmartLockCredentialData refreshToken(SmartLockCredentialData credentials) {
            return credentials;
        }

        @Override
        public List<DeviceSnapshot> listDevices(SmartLockCredentialData credentials) {
            return deviceSnapshots;
        }

        @Override
        public LockStatusSnapshot getStatus(SmartLockCredentialData credentials, String providerLockId) {
            statusRequests.add(providerLockId);
            if (failedStatusDeviceIds.contains(providerLockId)) {
                throw new RuntimeException("SwitchBot status unavailable");
            }
            LockStatusSnapshot snapshot = statusSnapshots.get(providerLockId);
            if (snapshot != null) {
                return snapshot;
            }
            return new LockStatusSnapshot("LOCKED", 90, true, "{}");
        }

        @Override
        public ProviderTaskResult unlock(SmartLockCredentialData credentials, String providerLockId) {
            unlockCalls++;
            unlockRequests.add(providerLockId);
            return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, "task-1", null, "ok");
        }

        @Override
        public ProviderTaskResult lock(SmartLockCredentialData credentials, String providerLockId) {
            lockRequests.add(providerLockId);
            return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, "task-2", null, "ok");
        }

        @Override
        public ProviderTaskResult createPasscode(
                SmartLockCredentialData credentials,
                String providerLockId,
                PasscodeCommand command
        ) {
            createPasscodeCalls++;
            return new ProviderTaskResult(
                    createPasscodeStatus,
                    "provider-task-1",
                    "provider-passcode-1",
                    "ok"
            );
        }

        @Override
        public ProviderTaskResult deletePasscode(
                SmartLockCredentialData credentials,
                String providerLockId,
                String providerPasscodeId
        ) {
            deletePasscodeCalls++;
            return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, "task-4", providerPasscodeId, "ok");
        }

        @Override
        public ProviderTaskResult queryTask(SmartLockCredentialData credentials, String providerTaskId) {
            queryTaskCalls++;
            return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, providerTaskId, null, "ok");
        }
    }
}
