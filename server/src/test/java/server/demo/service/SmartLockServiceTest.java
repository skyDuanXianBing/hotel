package server.demo.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import server.demo.config.SmartLockConfig;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.SmartLockBindingDTO;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import static org.mockito.ArgumentMatchers.argThat;
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
    private StoreRepository storeRepository;
    private FakeProviderClient fakeProviderClient;
    private FakeProviderClient fakeTtLockClient;
    private SmartLockService service;
    private SmartLockPasscodeReconciliationService reconciliationService;
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
        storeRepository = mock(StoreRepository.class);
        fakeProviderClient = new FakeProviderClient(SmartLockProvider.SWITCHBOT);
        fakeTtLockClient = new FakeProviderClient(SmartLockProvider.TTLOCK);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        credentialCrypto = new SmartLockCredentialCrypto("test-smart-lock-key");
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("UTC")));

        SmartLockConfig config = mock(SmartLockConfig.class);
        when(config.getConfirmationTtlSeconds()).thenReturn(300L);
        when(config.getSwitchBotWebhookToken()).thenReturn("webhook-token");
        SmartLockProviderClientRegistry providerRegistry =
                new SmartLockProviderClientRegistry(List.of(fakeProviderClient, fakeTtLockClient));
        reconciliationService = new SmartLockPasscodeReconciliationService(
                passcodeRepository,
                taskRepository,
                providerRegistry,
                credentialCrypto,
                objectMapper,
                storeRepository,
                FIXED_CLOCK
        );

        service = new SmartLockService(
                integrationRepository,
                deviceRepository,
                bindingRepository,
                confirmationRepository,
                passcodeRepository,
                taskRepository,
                roomRepository,
                storeRepository,
                providerRegistry,
                credentialCrypto,
                new SmartLockMapper(),
                config,
                objectMapper,
                reconciliationService,
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
    void createBinding_shouldSaveSwitchBotControlAndPasscodeRoles() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-1", "Lock", null, "{}");
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-1",
                "Keypad Touch",
                "lock-1",
                "{\"lockDeviceId\":\"lock-1\"}"
        );
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(room.getId());
        request.setControlDeviceId(lock.getId());
        request.setPasscodeDeviceId(keypad.getId());

        stubCreateBindingDevices(room, lock, keypad);
        stubNoRoleConflicts(SmartLockProvider.SWITCHBOT);
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.empty());
        when(bindingRepository.save(any(SmartLockRoomBinding.class))).thenAnswer(invocation -> {
            SmartLockRoomBinding binding = invocation.getArgument(0);
            binding.setId(100L);
            return binding;
        });

        SmartLockBindingDTO result = service.createBinding(request);

        assertEquals(lock.getId(), result.getControlDeviceId());
        assertEquals("lock-1", result.getControlProviderLockId());
        assertEquals(keypad.getId(), result.getPasscodeDeviceId());
        assertEquals("keypad-1", result.getPasscodeProviderLockId());
        assertEquals(lock.getId(), result.getDeviceId());
        assertEquals("lock-1", result.getProviderLockId());
    }

    @Test
    void createBinding_shouldRejectSwitchBotKeypadAsControlRole() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-1",
                "Keypad Touch",
                "lock-1",
                "{\"lockDeviceId\":\"lock-1\"}"
        );
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(room.getId());
        request.setControlDeviceId(keypad.getId());

        stubCreateBindingDevices(room, keypad);
        stubNoRoleConflicts(SmartLockProvider.SWITCHBOT);
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createBinding(request)
        );

        assertTrue(error.getMessage().contains("控制设备"));
        verify(bindingRepository, never()).save(any(SmartLockRoomBinding.class));
    }

    @Test
    void createBinding_shouldRejectSwitchBotLockAsPasscodeRole() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-1", "Lock", null, "{}");
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(room.getId());
        request.setPasscodeDeviceId(lock.getId());

        stubCreateBindingDevices(room, lock);
        stubNoRoleConflicts(SmartLockProvider.SWITCHBOT);
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createBinding(request)
        );

        assertTrue(error.getMessage().contains("密码设备"));
        verify(bindingRepository, never()).save(any(SmartLockRoomBinding.class));
    }

    @Test
    void createBinding_shouldRejectSwitchBotKeypadLinkedToDifferentControlLock() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-2", "Lock", null, "{}");
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-1",
                "Keypad Touch",
                "lock-1",
                "{\"lockDeviceId\":\"lock-1\"}"
        );
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(room.getId());
        request.setControlDeviceId(lock.getId());
        request.setPasscodeDeviceId(keypad.getId());

        stubCreateBindingDevices(room, lock, keypad);
        stubNoRoleConflicts(SmartLockProvider.SWITCHBOT);
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createBinding(request)
        );

        assertTrue(error.getMessage().contains("关联的控制门锁"));
        verify(bindingRepository, never()).save(any(SmartLockRoomBinding.class));
    }

    @Test
    void createBinding_shouldAllowTtLockSameDeviceForBothRoles() {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = ttLockIntegration();
        SmartLockDevice lock = device(
                20L,
                integration,
                "101",
                "TTLock",
                null,
                "{\"hasGateway\":1,\"keyboardPwdVersion\":4}"
        );
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(room.getId());
        request.setControlDeviceId(lock.getId());
        request.setPasscodeDeviceId(lock.getId());

        stubCreateBindingDevices(room, lock);
        stubNoRoleConflicts(SmartLockProvider.TTLOCK);
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.empty());
        when(bindingRepository.save(any(SmartLockRoomBinding.class))).thenAnswer(invocation -> {
            SmartLockRoomBinding binding = invocation.getArgument(0);
            binding.setId(100L);
            return binding;
        });

        SmartLockBindingDTO result = service.createBinding(request);

        assertEquals(lock.getId(), result.getControlDeviceId());
        assertEquals(lock.getId(), result.getPasscodeDeviceId());
        assertEquals("101", result.getControlProviderLockId());
        assertEquals("101", result.getPasscodeProviderLockId());
    }

    @Test
    void createBinding_shouldRejectCrossProviderBindingForSameRoom() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding existingSwitchBotBinding = binding(100L, room);
        SmartLockIntegration ttLockIntegration = ttLockIntegration();
        SmartLockDevice ttLock = device(
                30L,
                ttLockIntegration,
                "101",
                "TTLock",
                null,
                "{\"hasGateway\":1,\"keyboardPwdVersion\":4}"
        );
        SmartLockRequests.CreateBindingRequest request = new SmartLockRequests.CreateBindingRequest();
        request.setRoomId(room.getId());
        request.setControlDeviceId(ttLock.getId());
        request.setPasscodeDeviceId(ttLock.getId());

        stubCreateBindingDevices(room, ttLock);
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(existingSwitchBotBinding));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createBinding(request)
        );

        assertTrue(error.getMessage().contains("SwitchBot"));
        assertTrue(error.getMessage().contains("TTLock"));
        verify(bindingRepository, never()).save(any(SmartLockRoomBinding.class));
    }

    @Test
    void refreshToken_shouldRejectIntegrationOutsideCurrentStore() {
        when(integrationRepository.findByStoreIdAndId(STORE_ID, 77L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.refreshToken(77L));
        verify(integrationRepository, never()).save(any(SmartLockIntegration.class));
    }

    @Test
    void testIntegration_ttLockShouldPersistRefreshedTokenBeforeConnectionTest() throws Exception {
        SmartLockIntegration integration = ttLockIntegration();
        integration.setCredentialCiphertext(encryptedTtLockCredentials(
                "access-old",
                "refresh-old",
                LocalDateTime.now(FIXED_CLOCK).minusMinutes(1)
        ));
        fakeTtLockClient.refreshTokenResult = ttLockCredentials(
                "access-new",
                "refresh-new",
                LocalDateTime.now(FIXED_CLOCK).plusHours(2)
        );

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        when(integrationRepository.save(any(SmartLockIntegration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockTestResultDTO result = service.testIntegration(integration.getId());
        SmartLockCredentialData savedCredentials = decryptCredentials(integration);

        assertTrue(result.getSuccess());
        assertEquals(1, fakeTtLockClient.refreshTokenCalls);
        assertEquals(1, fakeTtLockClient.testConnectionCalls);
        assertEquals("access-new", fakeTtLockClient.lastTestConnectionCredentials.getTtLockAccessToken());
        assertEquals("refresh-new", fakeTtLockClient.lastTestConnectionCredentials.getTtLockRefreshToken());
        assertEquals("access-new", savedCredentials.getTtLockAccessToken());
        assertEquals("refresh-new", savedCredentials.getTtLockRefreshToken());
        assertEquals(fakeTtLockClient.refreshTokenResult.getTtLockTokenExpiresAt(), integration.getTokenExpiresAt());
    }

    @Test
    void updateIntegration_ttLockCoreCredentialChangeShouldClearStoredTokens() throws Exception {
        SmartLockIntegration integration = ttLockIntegration();
        integration.setCredentialCiphertext(encryptedTtLockCredentials(
                "access-old",
                "refresh-old",
                LocalDateTime.now(FIXED_CLOCK).plusHours(2)
        ));
        integration.setTokenExpiresAt(LocalDateTime.now(FIXED_CLOCK).plusHours(2));
        SmartLockRequests.UpsertIntegrationRequest request = new SmartLockRequests.UpsertIntegrationRequest();
        request.setTtLockClientId("client-new");

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        when(integrationRepository.save(any(SmartLockIntegration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateIntegration(integration.getId(), request);
        SmartLockCredentialData savedCredentials = decryptCredentials(integration);

        assertEquals("client-new", savedCredentials.getTtLockClientId());
        assertEquals(null, savedCredentials.getTtLockAccessToken());
        assertEquals(null, savedCredentials.getTtLockRefreshToken());
        assertEquals(null, savedCredentials.getTtLockTokenExpiresAt());
        assertEquals(null, integration.getTokenExpiresAt());
    }

    @Test
    void refreshToken_ttLockFailureShouldLogContextAndExposeProviderMessage() throws Exception {
        SmartLockIntegration integration = ttLockIntegration();
        integration.setCredentialCiphertext(encryptedTtLockCredentials(
                "access-old",
                "refresh-old",
                LocalDateTime.now(FIXED_CLOCK).minusMinutes(1)
        ));
        fakeTtLockClient.refreshTokenFailure = new SmartLockTtLockClient.TtLockTokenException(
                "refresh_token",
                "/oauth2/token",
                "10011",
                "invalid refresh_token"
        );

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        ListAppender<ILoggingEvent> logAppender = attachServiceLogAppender();

        RuntimeException error;
        try {
            error = assertThrows(RuntimeException.class, () -> service.refreshToken(integration.getId()));
        } finally {
            detachServiceLogAppender(logAppender);
        }

        assertTrue(error.getMessage().contains("invalid refresh_token"));
        assertTrue(hasLogMessage(logAppender, "provider=TTLOCK storeId=26 integrationId=11"));
        assertTrue(hasLogMessage(logAppender, "operation=refresh_token endpoint=/oauth2/token"));
        assertTrue(hasLogMessage(logAppender, "errcode=10011 errmsg=invalid refresh_token"));
        assertFalse(hasLogMessage(logAppender, "refresh-old"));
        assertFalse(hasLogMessage(logAppender, "secret-1"));
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
    void syncDevices_shouldRefreshTtLockStatusAndExposeSingleDeviceCapabilities() throws Exception {
        SmartLockIntegration integration = ttLockIntegration();
        integration.setCredentialCiphertext(encryptedTtLockCredentials(
                "access-1",
                "refresh-1",
                LocalDateTime.now(FIXED_CLOCK).plusHours(2)
        ));
        fakeTtLockClient.deviceSnapshots = List.of(
                new SmartLockProviderClient.DeviceSnapshot(
                        "101",
                        "Room 101",
                        "TTLock",
                        null,
                        88,
                        null,
                        null,
                        "{\"lockId\":101,\"hasGateway\":1,\"keyboardPwdVersion\":4}"
                )
        );
        fakeTtLockClient.statusSnapshots.put(
                "101",
                new SmartLockProviderClient.LockStatusSnapshot("locked", 77, null, "{}")
        );

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        when(deviceRepository.findByStoreIdAndProviderAndProviderLockId(
                eq(STORE_ID),
                eq(SmartLockProvider.TTLOCK),
                anyString()
        )).thenReturn(Optional.empty());
        stubDeviceSaveWithIds();

        List<SmartLockDeviceDTO> devices = service.syncDevices(integration.getId());

        SmartLockDeviceDTO ttLock = findDevice(devices, "101");
        assertEquals("locked", ttLock.getLockStatus());
        assertEquals(77, ttLock.getBattery());
        assertEquals(Boolean.TRUE, ttLock.getSupportsControl());
        assertEquals(Boolean.TRUE, ttLock.getSupportsPasscode());
        assertEquals("DEVICE", ttLock.getStatusSource());
        assertEquals("101", ttLock.getStatusSourceDeviceId());
        assertEquals(LocalDateTime.now(FIXED_CLOCK), ttLock.getLastStatusAt());
        assertEquals(List.of("101"), fakeTtLockClient.statusRequests);
    }

    @Test
    void syncDevices_shouldKeepTtLockDevicePendingWhenStatusApiFails() throws Exception {
        SmartLockIntegration integration = ttLockIntegration();
        integration.setCredentialCiphertext(encryptedTtLockCredentials(
                "access-1",
                "refresh-1",
                LocalDateTime.now(FIXED_CLOCK).plusHours(2)
        ));
        fakeTtLockClient.deviceSnapshots = List.of(
                new SmartLockProviderClient.DeviceSnapshot(
                        "101",
                        "Room 101",
                        "TTLock",
                        null,
                        88,
                        null,
                        null,
                        "{\"lockId\":101,\"hasGateway\":1,\"keyboardPwdVersion\":4}"
                )
        );
        fakeTtLockClient.failedStatusDeviceIds.add("101");

        when(integrationRepository.findByStoreIdAndId(STORE_ID, integration.getId()))
                .thenReturn(Optional.of(integration));
        when(deviceRepository.findByStoreIdAndProviderAndProviderLockId(
                eq(STORE_ID),
                eq(SmartLockProvider.TTLOCK),
                anyString()
        )).thenReturn(Optional.empty());
        stubDeviceSaveWithIds();

        List<SmartLockDeviceDTO> devices = service.syncDevices(integration.getId());

        SmartLockDeviceDTO ttLock = findDevice(devices, "101");
        assertEquals(null, ttLock.getLockStatus());
        assertEquals(88, ttLock.getBattery());
        assertEquals(null, ttLock.getOnline());
        assertEquals(null, ttLock.getLastStatusAt());
        assertEquals("DEVICE", ttLock.getStatusSource());
        assertEquals("101", ttLock.getStatusSourceDeviceId());
        assertEquals(List.of("101"), fakeTtLockClient.statusRequests);
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
    void roomOperations_shouldRouteControlAndPasscodeToSeparateSwitchBotRoleDevices() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-A", "Lock", null, "{}");
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-A",
                "Keypad Touch",
                "lock-A",
                "{\"lockDeviceId\":\"lock-A\"}"
        );
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, lock, keypad);
        SmartLockRequests.LockOperationRequest lockRequest = lockOperationRequest(binding, "dual-unlock");
        stubSuccessfulLockOperation(room, binding, SmartLockTaskType.UNLOCK, lockRequest.getIdempotencyKey());

        SmartLockTaskDTO lockResult = service.unlock(room.getId(), lockRequest);

        assertEquals(SmartLockTaskStatus.SUCCESS, lockResult.getStatus());
        assertEquals(List.of("lock-A"), fakeProviderClient.unlockRequests);

        SmartLockRequests.CreatePasscodeRequest passcodeRequest = passcodeRequest();
        passcodeRequest.setIdempotencyKey("create-for-keypad-a");
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(taskRepository.findByStoreIdAndIdempotencyKey(STORE_ID, passcodeRequest.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> {
            SmartLockPasscodeRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(301L);
            }
            return record;
        });
        stubTaskSaveWithIds();

        SmartLockPasscodeDTO passcodeResult = service.createPasscode(room.getId(), passcodeRequest);

        assertEquals(SmartLockPasscodeStatus.ACTIVE, passcodeResult.getStatus());
        assertEquals(List.of("keypad-A"), fakeProviderClient.createPasscodeRequests);
        assertEquals(List.of("lock-A"), fakeProviderClient.unlockRequests);
    }

    @Test
    void unlock_shouldRefreshTtLockStatusAfterSuccessfulCommand() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = ttLockDualRoleBinding(100L, room);
        SmartLockRequests.LockOperationRequest request = lockOperationRequest(binding, "ttlock-unlock-refresh");
        SmartLockConfirmation confirmation = confirmation(room, binding, SmartLockTaskType.UNLOCK);
        fakeTtLockClient.statusSnapshots.put(
                "101",
                new SmartLockProviderClient.LockStatusSnapshot("unlocked", 66, null, "{\"state\":1}")
        );

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndIdAndStatus(
                STORE_ID,
                binding.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(taskRepository.findByStoreIdAndIdempotencyKey(STORE_ID, request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(confirmationRepository.findByStoreIdAndTokenHash(eq(STORE_ID), anyString()))
                .thenReturn(Optional.of(confirmation));
        when(confirmationRepository.save(any(SmartLockConfirmation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceRepository.save(any(SmartLockDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTaskSaveWithIds();

        SmartLockTaskDTO result = service.unlock(room.getId(), request);

        assertEquals(SmartLockTaskStatus.SUCCESS, result.getStatus());
        assertEquals(List.of("101"), fakeTtLockClient.unlockRequests);
        assertEquals(List.of("101"), fakeTtLockClient.statusRequests);
        assertEquals("unlocked", binding.getControlDevice().getLockStatus());
        assertEquals(66, binding.getControlDevice().getBattery());
        assertEquals(LocalDateTime.now(FIXED_CLOCK), binding.getControlDevice().getLastStatusAt());
    }

    @Test
    void createPasscode_shouldUseTtLockPasscodeRoleLockId() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = ttLockDualRoleBinding(100L, room);
        SmartLockRequests.CreatePasscodeRequest request = passcodeRequest();
        request.setIdempotencyKey("ttlock-create-manual");
        fakeTtLockClient.createProviderTaskId = null;
        fakeTtLockClient.createProviderPasscodeId = "pwd-101";

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(taskRepository.findByStoreIdAndIdempotencyKey(STORE_ID, request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> {
            SmartLockPasscodeRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(301L);
            }
            return record;
        });
        stubTaskSaveWithIds();

        SmartLockPasscodeDTO result = service.createPasscode(room.getId(), request);

        assertEquals(SmartLockPasscodeStatus.ACTIVE, result.getStatus());
        assertEquals("pwd-101", result.getProviderPasscodeId());
        assertEquals("123456", result.getOneTimePasscode());
        assertEquals(List.of("101"), fakeTtLockClient.createPasscodeRequests);
    }

    @Test
    void listPasscodes_shouldReconcileTtLockPendingRecordFromRemoteList() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = ttLockDualRoleBinding(100L, room);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        record.setProviderPasscodeId("pwd-101");
        record.setProviderTaskId("task-create-ttlock");
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, "task-create-ttlock");
        fakeTtLockClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("pwd-101", "Guest", "normal")
        );

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(STORE_ID, room.getId()))
                .thenReturn(List.of(record));
        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.TTLOCK,
                "task-create-ttlock"
        )).thenReturn(List.of(task));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<SmartLockPasscodeDTO> result = service.listPasscodes(room.getId());

        assertEquals(1, result.size());
        assertEquals(SmartLockPasscodeStatus.ACTIVE, result.get(0).getStatus());
        assertEquals(SmartLockTaskStatus.SUCCESS, task.getStatus());
        assertEquals(List.of("101"), fakeTtLockClient.listPasscodeRequests);
    }

    @Test
    void listPasscodes_shouldMapTtLockNumericRemoteStatuses() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = ttLockDualRoleBinding(100L, room);
        SmartLockPasscodeRecord invalidRecord = passcodeRecord(301L, binding, SmartLockPasscodeStatus.ACTIVE);
        SmartLockPasscodeRecord pendingRecord = passcodeRecord(302L, binding, SmartLockPasscodeStatus.ACTIVE);
        SmartLockPasscodeRecord addingRecord = passcodeRecord(303L, binding, SmartLockPasscodeStatus.ACTIVE);
        SmartLockPasscodeRecord addFailedRecord = passcodeRecord(304L, binding, SmartLockPasscodeStatus.ACTIVE);
        SmartLockPasscodeRecord deletingRecord = passcodeRecord(305L, binding, SmartLockPasscodeStatus.ACTIVE);
        SmartLockPasscodeRecord deleteFailedRecord = passcodeRecord(306L, binding, SmartLockPasscodeStatus.ACTIVE);
        invalidRecord.setProviderPasscodeId("pwd-invalid");
        pendingRecord.setProviderPasscodeId("pwd-pending");
        addingRecord.setProviderPasscodeId("pwd-adding");
        addFailedRecord.setProviderPasscodeId("pwd-add-failed");
        deletingRecord.setProviderPasscodeId("pwd-deleting");
        deleteFailedRecord.setProviderPasscodeId("pwd-delete-failed");
        fakeTtLockClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("pwd-invalid", "Invalid", "2"),
                new SmartLockProviderClient.ProviderPasscodeSnapshot("pwd-pending", "Pending", "3"),
                new SmartLockProviderClient.ProviderPasscodeSnapshot("pwd-adding", "Adding", "4"),
                new SmartLockProviderClient.ProviderPasscodeSnapshot("pwd-add-failed", "Add failed", "5"),
                new SmartLockProviderClient.ProviderPasscodeSnapshot("pwd-deleting", "Deleting", "8"),
                new SmartLockProviderClient.ProviderPasscodeSnapshot("pwd-delete-failed", "Delete failed", "9")
        );

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(STORE_ID, room.getId()))
                .thenReturn(List.of(
                        invalidRecord,
                        pendingRecord,
                        addingRecord,
                        addFailedRecord,
                        deletingRecord,
                        deleteFailedRecord
                ));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<SmartLockPasscodeDTO> result = service.listPasscodes(room.getId());
        Map<String, SmartLockPasscodeStatus> statusByPasscodeId = new HashMap<>();
        for (SmartLockPasscodeDTO passcode : result) {
            statusByPasscodeId.put(passcode.getProviderPasscodeId(), passcode.getStatus());
        }

        assertEquals(SmartLockPasscodeStatus.FAILED, statusByPasscodeId.get("pwd-invalid"));
        assertEquals(SmartLockPasscodeStatus.PENDING, statusByPasscodeId.get("pwd-pending"));
        assertEquals(SmartLockPasscodeStatus.PENDING, statusByPasscodeId.get("pwd-adding"));
        assertEquals(SmartLockPasscodeStatus.FAILED, statusByPasscodeId.get("pwd-add-failed"));
        assertEquals(SmartLockPasscodeStatus.DELETE_PENDING, statusByPasscodeId.get("pwd-deleting"));
        assertEquals(SmartLockPasscodeStatus.FAILED, statusByPasscodeId.get("pwd-delete-failed"));
        assertEquals(List.of("101"), fakeTtLockClient.listPasscodeRequests);
    }

    @Test
    void listPasscodes_shouldMarkTtLockDeletePendingDeletedWhenRemoteIdDisappears() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = ttLockDualRoleBinding(100L, room);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.DELETE_PENDING);
        record.setProviderPasscodeId("pwd-delete");
        record.setProviderTaskId("task-delete-ttlock");
        SmartLockTask task = task(401L, SmartLockTaskType.DELETE_PASSCODE, binding, record, "task-delete-ttlock");
        fakeTtLockClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("other-pwd", "Other", "normal")
        );

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(STORE_ID, room.getId()))
                .thenReturn(List.of(record));
        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.TTLOCK,
                "task-delete-ttlock"
        )).thenReturn(List.of(task));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<SmartLockPasscodeDTO> result = service.listPasscodes(room.getId());

        assertEquals(0, result.size());
        assertEquals(SmartLockPasscodeStatus.DELETED, record.getStatus());
        assertTrue(record.getDeletedAt() != null);
        assertEquals(SmartLockTaskStatus.SUCCESS, task.getStatus());
        assertEquals(List.of("101"), fakeTtLockClient.listPasscodeRequests);
    }

    @Test
    void getTask_shouldCloseTtLockPendingPasscodeWhenRemoteListHasNoMatch() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = ttLockDualRoleBinding(100L, room);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        record.setProviderTaskId("task-create-ttlock");
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, "task-create-ttlock");
        fakeTtLockClient.passcodeSnapshots = List.of();

        when(taskRepository.findByStoreIdAndId(STORE_ID, task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.TTLOCK,
                "task-create-ttlock"
        )).thenReturn(List.of(task));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockTaskDTO result = service.getTask(task.getId());

        assertEquals(SmartLockTaskStatus.FAILED, result.getStatus());
        assertEquals(SmartLockPasscodeStatus.FAILED, record.getStatus());
        assertTrue(record.getLastError().contains("未找到"));
        assertEquals(List.of("101"), fakeTtLockClient.listPasscodeRequests);
    }

    @Test
    void deletePasscode_shouldUseTtLockDeleteAndLandDeletedSynchronously() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = ttLockDualRoleBinding(100L, room);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.ACTIVE);
        record.setProviderPasscodeId("pwd-delete");

        when(passcodeRepository.findByStoreIdAndId(STORE_ID, record.getId())).thenReturn(Optional.of(record));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTaskSaveWithIds();

        SmartLockPasscodeDTO result = service.deletePasscode(record.getId());

        assertEquals(SmartLockPasscodeStatus.DELETED, result.getStatus());
        assertEquals(SmartLockPasscodeStatus.DELETED, record.getStatus());
        assertTrue(record.getDeletedAt() != null);
        assertEquals(List.of("101"), fakeTtLockClient.deletePasscodeRequests);
    }

    @Test
    void unlock_shouldRejectPasscodeOnlyBindingBeforeProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = new SmartLockIntegration();
        integration.setId(10L);
        integration.setStoreId(STORE_ID);
        integration.setProvider(SmartLockProvider.SWITCHBOT);
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-A",
                "Keypad Touch",
                "lock-A",
                "{\"lockDeviceId\":\"lock-A\"}"
        );
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, null, keypad);
        SmartLockRequests.LockOperationRequest request = lockOperationRequest(binding, "passcode-only-unlock");

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

        assertTrue(error.getMessage().contains("控制设备"));
        assertEquals(List.of(), fakeProviderClient.unlockRequests);
        verify(taskRepository, never()).save(any(SmartLockTask.class));
    }

    @Test
    void createPasscode_shouldRejectControlOnlyBindingBeforeProviderCall() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-A", "Lock", null, "{}");
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, lock, null);

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createPasscode(room.getId(), passcodeRequest())
        );

        assertTrue(error.getMessage().contains("密码设备"));
        verify(passcodeRepository, never()).save(any(SmartLockPasscodeRecord.class));
        assertEquals(0, fakeProviderClient.createPasscodeCalls);
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
    void deletePasscode_shouldUsePasscodeDeviceSnapshotWhenBindingChanged() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-1", "Lock", null, "{}");
        SmartLockDevice oldKeypad = device(
                21L,
                integration,
                "keypad-old",
                "Keypad Touch",
                "lock-1",
                "{\"lockDeviceId\":\"lock-1\"}"
        );
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, lock, oldKeypad);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.ACTIVE);
        record.setProviderPasscodeId("provider-passcode-1");

        when(passcodeRepository.findByStoreIdAndId(STORE_ID, record.getId())).thenReturn(Optional.of(record));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTaskSaveWithIds();

        SmartLockPasscodeDTO result = service.deletePasscode(record.getId());

        assertEquals(SmartLockPasscodeStatus.DELETED, result.getStatus());
        assertEquals(List.of("keypad-old"), fakeProviderClient.deletePasscodeRequests);
        verify(bindingRepository, never()).findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        );
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
    void createPasscode_shouldRejectExpiredValidityWindowBeforeProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockRequests.CreatePasscodeRequest request = passcodeRequest();
        request.setValidFrom(LocalDateTime.now(FIXED_CLOCK).minusDays(2));
        request.setValidUntil(LocalDateTime.now(FIXED_CLOCK).minusDays(1));

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createPasscode(room.getId(), request)
        );

        assertTrue(error.getMessage().contains("有效期"));
        verify(passcodeRepository, never()).save(any(SmartLockPasscodeRecord.class));
        verify(taskRepository, never()).save(any(SmartLockTask.class));
        assertEquals(0, fakeProviderClient.createPasscodeCalls);
    }

    @Test
    void createPasscode_shouldUseStoreTimezoneForExpiredValidityWindowBeforeProviderCall() {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockRequests.CreatePasscodeRequest request = passcodeRequest();
        request.setValidFrom(LocalDateTime.of(2026, 6, 21, 17, 0));
        request.setValidUntil(LocalDateTime.of(2026, 6, 21, 18, 30));

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createPasscode(room.getId(), request)
        );

        assertTrue(error.getMessage().contains("有效期"));
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
    void createPasscode_shouldPersistCommandIdOnlySwitchBotPendingResult() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        binding.getIntegration().setCredentialCiphertext(encryptedSwitchBotCredentials());
        fakeProviderClient.createPasscodeStatus = SmartLockTaskStatus.PENDING;
        fakeProviderClient.createProviderTaskId = "cmd-create-1";
        fakeProviderClient.createProviderPasscodeId = null;
        SmartLockRequests.CreatePasscodeRequest request = passcodeRequest();
        request.setIdempotencyKey("create-command-only");

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
        stubTaskSaveWithIds();

        SmartLockPasscodeDTO result = service.createPasscode(room.getId(), request);

        assertEquals(SmartLockPasscodeStatus.PENDING, result.getStatus());
        assertEquals("cmd-create-1", result.getProviderTaskId());
        assertEquals(null, result.getProviderPasscodeId());
        assertEquals(null, result.getOneTimePasscode());
        assertEquals(1, fakeProviderClient.createPasscodeCalls);
    }

    @Test
    void createPasscode_shouldKeepSwitchBotSuccessWithoutTraceableIdsPendingForKeyListSync() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        binding.getIntegration().setCredentialCiphertext(encryptedSwitchBotCredentials());
        fakeProviderClient.createPasscodeStatus = SmartLockTaskStatus.PENDING;
        fakeProviderClient.createProviderTaskId = null;
        fakeProviderClient.createProviderPasscodeId = null;
        SmartLockRequests.CreatePasscodeRequest request = passcodeRequest();
        request.setIdempotencyKey("create-no-trace");

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
        List<SmartLockTask> savedTasks = new ArrayList<>();
        long[] nextTaskId = {401L};
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> {
            SmartLockTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(nextTaskId[0]);
                nextTaskId[0] += 1;
            }
            savedTasks.add(task);
            return task;
        });

        SmartLockPasscodeDTO result = service.createPasscode(room.getId(), request);

        assertEquals(SmartLockPasscodeStatus.PENDING, result.getStatus());
        assertEquals(null, result.getLastError());
        assertEquals(null, result.getProviderTaskId());
        assertEquals(null, result.getProviderPasscodeId());
        assertTrue(savedTasks.stream().anyMatch(task ->
                task.getResultMessage() != null && task.getResultMessage().contains("keyList")
        ));
        assertEquals(1, fakeProviderClient.createPasscodeCalls);
    }

    @Test
    void listPasscodes_shouldReconcileSwitchBotPendingRecordFromKeyList() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-A", "Lock", null, "{}");
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-A",
                "Keypad Touch",
                "lock-A",
                "{\"lockDeviceId\":\"lock-A\"}"
        );
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, lock, keypad);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        record.setProviderTaskId("cmd-create-1");
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, "cmd-create-1");
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-101", "Guest", "normal")
        );

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(STORE_ID, room.getId()))
                .thenReturn(List.of(record));
        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                "cmd-create-1"
        )).thenReturn(List.of(task));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<SmartLockPasscodeDTO> result = service.listPasscodes(room.getId());

        assertEquals(1, result.size());
        assertEquals(SmartLockPasscodeStatus.ACTIVE, result.get(0).getStatus());
        assertEquals("key-101", result.get(0).getProviderPasscodeId());
        assertEquals(SmartLockTaskStatus.SUCCESS, task.getStatus());
        assertEquals(List.of("keypad-A"), fakeProviderClient.listPasscodeRequests);
    }

    @Test
    void listPasscodes_shouldReconcileSwitchBotPendingNoTraceRecordFromKeyList() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-A", "Lock", null, "{}");
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-A",
                "Keypad Touch",
                "lock-A",
                "{\"lockDeviceId\":\"lock-A\"}"
        );
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, lock, keypad);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, null);
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-101", "Guest", "normal")
        );

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(STORE_ID, room.getId()))
                .thenReturn(List.of(record));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findPasscodeTasksWithoutProviderTaskId(
                STORE_ID,
                record.getId(),
                SmartLockTaskType.CREATE_PASSCODE,
                SmartLockTaskStatus.PENDING
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<SmartLockPasscodeDTO> result = service.listPasscodes(room.getId());

        assertEquals(1, result.size());
        assertEquals(SmartLockPasscodeStatus.ACTIVE, result.get(0).getStatus());
        assertEquals("key-101", result.get(0).getProviderPasscodeId());
        assertEquals(SmartLockTaskStatus.SUCCESS, task.getStatus());
        assertEquals("key-101", task.getPasscodeRecord().getProviderPasscodeId());
        assertEquals(List.of("keypad-A"), fakeProviderClient.listPasscodeRequests);
        verify(taskRepository, never()).findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                eq(SmartLockProvider.SWITCHBOT),
                anyString()
        );
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldActivatePendingRecordWithoutCreateCall() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-A", "Lock", null, "{}");
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-A",
                "Keypad Touch",
                "lock-A",
                "{\"lockDeviceId\":\"lock-A\"}"
        );
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, lock, keypad);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        record.setCreatedAt(LocalDateTime.now(FIXED_CLOCK).minusMinutes(1));
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, null);
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-101", "Guest", "normal")
        );

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findPasscodeTasksWithoutProviderTaskId(
                STORE_ID,
                record.getId(),
                SmartLockTaskType.CREATE_PASSCODE,
                SmartLockTaskStatus.PENDING
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockPasscodeReconciliationService.ReconciliationSummary summary =
                reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(1, summary.candidates());
        assertEquals(1, summary.changed());
        assertEquals(SmartLockPasscodeStatus.ACTIVE, record.getStatus());
        assertEquals("key-101", record.getProviderPasscodeId());
        assertEquals(SmartLockTaskStatus.SUCCESS, task.getStatus());
        assertEquals(0, fakeProviderClient.createPasscodeCalls);
        assertEquals(List.of("keypad-A"), fakeProviderClient.listPasscodeRequests);
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldKeepPendingWhenNoKeyListMatch() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-201", "Other Guest", "normal")
        );

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockPasscodeReconciliationService.ReconciliationSummary summary =
                reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(1, summary.candidates());
        assertEquals(SmartLockPasscodeStatus.PENDING, record.getStatus());
        assertEquals(null, record.getProviderPasscodeId());
        assertTrue(record.getLastError().contains("尚未发现"));
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldKeepPendingWhenKeyListHasMultipleNameMatches() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-101", "Guest", "normal"),
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-102", "Guest", "normal")
        );

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(SmartLockPasscodeStatus.PENDING, record.getStatus());
        assertEquals(null, record.getProviderPasscodeId());
        assertTrue(record.getLastError().contains("多条同名"));
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldKeepPendingWhenMatchedKeyHasNoId() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot(null, "Guest", "normal")
        );

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(SmartLockPasscodeStatus.PENDING, record.getStatus());
        assertEquals(null, record.getProviderPasscodeId());
        assertTrue(record.getLastError().contains("缺少远端 ID"));
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldFailTimedOutPendingCreateWithReason() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        record.setCreatedAt(LocalDateTime.now(FIXED_CLOCK).minusMinutes(10));
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, record.getBinding(), record, null);
        fakeProviderClient.passcodeSnapshots = List.of();

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findPasscodeTasksWithoutProviderTaskId(
                STORE_ID,
                record.getId(),
                SmartLockTaskType.CREATE_PASSCODE,
                SmartLockTaskStatus.PENDING
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockPasscodeReconciliationService.ReconciliationSummary summary =
                reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(1, summary.timedOut());
        assertEquals(1, summary.failed());
        assertEquals(SmartLockPasscodeStatus.FAILED, record.getStatus());
        assertTrue(record.getLastError().contains("超过后台对账超时"));
        assertEquals(SmartLockTaskStatus.FAILED, task.getStatus());
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldUseTokyoStoreZoneForCreatedAtAge() throws Exception {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        LocalDateTime storeNow = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), ZoneId.of("Asia/Tokyo"));
        record.setCreatedAt(storeNow.minusMinutes(10));
        record.setUpdatedAt(storeNow);
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, record.getBinding(), record, null);
        fakeProviderClient.passcodeSnapshots = List.of();

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findPasscodeTasksWithoutProviderTaskId(
                STORE_ID,
                record.getId(),
                SmartLockTaskType.CREATE_PASSCODE,
                SmartLockTaskStatus.PENDING
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ListAppender<ILoggingEvent> logAppender = attachReconciliationLogAppender();

        SmartLockPasscodeReconciliationService.ReconciliationSummary summary;
        try {
            summary = reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);
        } finally {
            detachReconciliationLogAppender(logAppender);
        }

        assertEquals(1, summary.timedOut());
        assertEquals(1, summary.failed());
        assertEquals(SmartLockPasscodeStatus.FAILED, record.getStatus());
        assertEquals(SmartLockTaskStatus.FAILED, task.getStatus());
        assertTrue(hasReconciliationLog(
                logAppender,
                "ageSeconds=600 ageSource=record_created_at ageZone=Asia/Tokyo"
        ));
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldUseShanghaiStoreZoneForCreatedAtAge() throws Exception {
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Shanghai")));
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        LocalDateTime storeNow = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), ZoneId.of("Asia/Shanghai"));
        record.setCreatedAt(storeNow.minusMinutes(4));
        record.setUpdatedAt(storeNow.plusMinutes(30));
        fakeProviderClient.passcodeSnapshots = List.of();

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ListAppender<ILoggingEvent> logAppender = attachReconciliationLogAppender();

        SmartLockPasscodeReconciliationService.ReconciliationSummary summary;
        try {
            summary = reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);
        } finally {
            detachReconciliationLogAppender(logAppender);
        }

        assertEquals(0, summary.timedOut());
        assertEquals(0, summary.failed());
        assertEquals(SmartLockPasscodeStatus.PENDING, record.getStatus());
        assertTrue(record.getLastError().contains("尚未发现"));
        assertTrue(hasReconciliationLog(
                logAppender,
                "ageSeconds=240 ageSource=record_created_at ageZone=Asia/Shanghai"
        ));
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldUseCreateTaskCreatedAtWhenRecordCreatedAtMissing() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        record.setCreatedAt(null);
        record.setUpdatedAt(LocalDateTime.now(FIXED_CLOCK));
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, record.getBinding(), record, null);
        task.setCreatedAt(LocalDateTime.now(FIXED_CLOCK).minusMinutes(10));
        task.setUpdatedAt(LocalDateTime.now(FIXED_CLOCK));
        fakeProviderClient.passcodeSnapshots = List.of();

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findPasscodeTasksWithoutProviderTaskId(
                STORE_ID,
                record.getId(),
                SmartLockTaskType.CREATE_PASSCODE,
                SmartLockTaskStatus.PENDING
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockPasscodeReconciliationService.ReconciliationSummary summary =
                reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(1, summary.timedOut());
        assertEquals(1, summary.failed());
        assertEquals(SmartLockPasscodeStatus.FAILED, record.getStatus());
        assertTrue(record.getLastError().contains("超过后台对账超时"));
        assertEquals(SmartLockTaskStatus.FAILED, task.getStatus());
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldMarkDeletePendingDeletedWhenRemoteIdDisappears() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        record.setStatus(SmartLockPasscodeStatus.DELETE_PENDING);
        record.setProviderPasscodeId("key-101");
        record.setProviderTaskId("cmd-delete-1");
        SmartLockTask task = task(
                401L,
                SmartLockTaskType.DELETE_PASSCODE,
                record.getBinding(),
                record,
                "cmd-delete-1"
        );
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-202", "Other Guest", "normal")
        );

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                "cmd-delete-1"
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(SmartLockPasscodeStatus.DELETED, record.getStatus());
        assertTrue(record.getDeletedAt() != null);
        assertEquals(SmartLockTaskStatus.SUCCESS, task.getStatus());
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldKeepDeletePendingWhenRemoteIdStillExists() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        record.setStatus(SmartLockPasscodeStatus.DELETE_PENDING);
        record.setProviderPasscodeId("key-101");
        fakeProviderClient.passcodeSnapshots = List.of(
                new SmartLockProviderClient.ProviderPasscodeSnapshot("key-101", "Guest", "normal")
        );

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(SmartLockPasscodeStatus.DELETE_PENDING, record.getStatus());
        assertTrue(record.getLastError().contains("仍显示远端密码存在"));
    }

    @Test
    void reconcilePendingSwitchBotPasscodesForCurrentStore_shouldWriteReasonWhenKeyListIsUnreadable() throws Exception {
        SmartLockPasscodeRecord record = switchBotPendingRecordForReconciliation();
        fakeProviderClient.passcodeKeyListReadable = false;

        stubSwitchBotReconciliationCandidates(record);
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5);

        assertEquals(SmartLockPasscodeStatus.PENDING, record.getStatus());
        assertTrue(record.getLastError().contains("未返回可读取的 keyList"));
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

    @Test
    void deletePasscode_shouldRejectMissingProviderPasscodeIdWhenProviderTaskIdExists() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        record.setProviderTaskId("cmd-create-1");

        when(passcodeRepository.findByStoreIdAndId(STORE_ID, record.getId())).thenReturn(Optional.of(record));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletePasscode(record.getId())
        );

        assertTrue(error.getMessage().contains("供应商密码 ID"));
        assertEquals(0, fakeProviderClient.deletePasscodeCalls);
        verify(taskRepository, never()).save(any(SmartLockTask.class));
    }

    @Test
    void deletePasscode_shouldLocalCleanupPendingNoRemoteIdsWithoutProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        SmartLockTask task = task(401L, SmartLockTaskType.CREATE_PASSCODE, binding, record, null);

        when(passcodeRepository.findByStoreIdAndId(STORE_ID, record.getId())).thenReturn(Optional.of(record));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.findPasscodeTasksWithoutProviderTaskId(
                STORE_ID,
                record.getId(),
                SmartLockTaskType.CREATE_PASSCODE,
                SmartLockTaskStatus.PENDING
        )).thenReturn(List.of(task));
        when(taskRepository.save(any(SmartLockTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockPasscodeDTO result = service.deletePasscode(record.getId());

        assertEquals(SmartLockPasscodeStatus.DELETED, result.getStatus());
        assertEquals(SmartLockPasscodeStatus.DELETED, record.getStatus());
        assertEquals(null, record.getLastError());
        assertTrue(record.getDeletedAt() != null);
        assertEquals(SmartLockTaskStatus.FAILED, task.getStatus());
        assertTrue(task.getResultMessage().contains("本地密码记录已清理"));
        assertEquals(0, fakeProviderClient.deletePasscodeCalls);
        verify(taskRepository, never()).save(argThat(taskArg ->
                taskArg != null && taskArg.getTaskType() == SmartLockTaskType.DELETE_PASSCODE
        ));
    }

    @Test
    void deletePasscode_shouldLocalCleanupFailedNoRemoteIdsWithoutProviderCall() {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.FAILED);

        when(passcodeRepository.findByStoreIdAndId(STORE_ID, record.getId())).thenReturn(Optional.of(record));
        when(passcodeRepository.save(any(SmartLockPasscodeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SmartLockPasscodeDTO result = service.deletePasscode(record.getId());

        assertEquals(SmartLockPasscodeStatus.DELETED, result.getStatus());
        assertEquals(SmartLockPasscodeStatus.DELETED, record.getStatus());
        assertTrue(record.getDeletedAt() != null);
        assertEquals(0, fakeProviderClient.deletePasscodeCalls);
        verify(taskRepository, never()).save(argThat(taskArg ->
                taskArg != null && taskArg.getTaskType() == SmartLockTaskType.DELETE_PASSCODE
        ));
    }

    @Test
    void listPasscodes_shouldHideDeletedRecordsAfterLocalCleanup() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockRoomBinding binding = binding(100L, room);
        binding.getDevice().setDeviceType("Keypad Touch");
        SmartLockPasscodeRecord deletedRecord = passcodeRecord(301L, binding, SmartLockPasscodeStatus.DELETED);
        SmartLockPasscodeRecord activeRecord = passcodeRecord(302L, binding, SmartLockPasscodeStatus.ACTIVE);
        activeRecord.setProviderPasscodeId("key-active");

        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        when(bindingRepository.findByStoreIdAndRoomIdAndStatus(
                STORE_ID,
                room.getId(),
                SmartLockBindingStatus.ACTIVE
        )).thenReturn(Optional.of(binding));
        when(passcodeRepository.findByStoreIdAndRoomIdOrderByCreatedAtDesc(STORE_ID, room.getId()))
                .thenReturn(List.of(deletedRecord, activeRecord));

        List<SmartLockPasscodeDTO> result = service.listPasscodes(room.getId());

        assertEquals(1, result.size());
        assertEquals(activeRecord.getId(), result.get(0).getId());
    }


    private Room room(Long id, Long storeId) {
        Room room = new Room();
        room.setId(id);
        room.setStoreId(storeId);
        room.setRoomNumber("101");
        return room;
    }

    private Store store(String timezone) {
        Store store = new Store();
        store.setId(STORE_ID);
        store.setUserId(USER_ID);
        store.setName("Test Store");
        store.setTimezone(timezone);
        return store;
    }

    private SmartLockDevice device(
            Long id,
            SmartLockIntegration integration,
            String providerLockId,
            String deviceType,
            String auxiliaryDeviceId,
            String rawDataJson
    ) {
        SmartLockDevice device = new SmartLockDevice();
        device.setId(id);
        device.setStoreId(STORE_ID);
        device.setIntegration(integration);
        device.setProvider(integration.getProvider());
        device.setProviderLockId(providerLockId);
        device.setLockName(providerLockId);
        device.setDeviceType(deviceType);
        device.setAuxiliaryDeviceId(auxiliaryDeviceId);
        device.setRawDataJson(rawDataJson);
        return device;
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

    private SmartLockRoomBinding dualRoleBinding(
            Long id,
            Room room,
            SmartLockDevice controlDevice,
            SmartLockDevice passcodeDevice
    ) {
        SmartLockDevice legacyDevice = controlDevice != null ? controlDevice : passcodeDevice;
        SmartLockIntegration integration = legacyDevice.getIntegration();

        SmartLockRoomBinding binding = new SmartLockRoomBinding();
        binding.setId(id);
        binding.setStoreId(STORE_ID);
        binding.setRoom(room);
        binding.setIntegration(integration);
        binding.setDevice(legacyDevice);
        binding.setControlDevice(controlDevice);
        binding.setControlProviderLockId(controlDevice != null ? controlDevice.getProviderLockId() : null);
        binding.setPasscodeDevice(passcodeDevice);
        binding.setPasscodeProviderLockId(passcodeDevice != null ? passcodeDevice.getProviderLockId() : null);
        binding.setProvider(integration.getProvider());
        binding.setProviderLockId(legacyDevice.getProviderLockId());
        binding.setCreatedBy(USER_ID);
        binding.setStatus(SmartLockBindingStatus.ACTIVE);
        return binding;
    }

    private SmartLockRoomBinding ttLockDualRoleBinding(Long id, Room room) throws Exception {
        SmartLockIntegration integration = ttLockIntegration();
        integration.setCredentialCiphertext(encryptedTtLockCredentials(
                "access-1",
                "refresh-1",
                LocalDateTime.now(FIXED_CLOCK).plusHours(2)
        ));
        SmartLockDevice lock = device(
                20L,
                integration,
                "101",
                "TTLock",
                null,
                "{\"lockId\":101,\"hasGateway\":1,\"keyboardPwdVersion\":4}"
        );
        return dualRoleBinding(id, room, lock, lock);
    }

    private SmartLockConfirmation confirmation(
            Room room,
            SmartLockRoomBinding binding,
            SmartLockTaskType action
    ) {
        SmartLockConfirmation confirmation = new SmartLockConfirmation();
        confirmation.setId(55L);
        confirmation.setStoreId(STORE_ID);
        confirmation.setRoom(room);
        confirmation.setBinding(binding);
        confirmation.setAction(action);
        confirmation.setCreatedBy(USER_ID);
        confirmation.setExpiresAt(LocalDateTime.now(FIXED_CLOCK).plusMinutes(5));
        return confirmation;
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
        SmartLockDevice passcodeDevice = binding.getPasscodeDevice() != null
                ? binding.getPasscodeDevice()
                : binding.getDevice();
        String passcodeProviderLockId = binding.getPasscodeProviderLockId() != null
                ? binding.getPasscodeProviderLockId()
                : binding.getProviderLockId();
        record.setProviderLockId(passcodeProviderLockId);
        record.setPasscodeRole("PASSCODE");
        record.setPasscodeDevice(passcodeDevice);
        record.setPasscodeProviderLockId(passcodeProviderLockId);
        record.setPasscodeName("Guest");
        record.setPasscodeMasked("******");
        record.setPasscodeHash("hash");
        record.setValidFrom(LocalDateTime.now(FIXED_CLOCK).plusHours(1));
        record.setValidUntil(LocalDateTime.now(FIXED_CLOCK).plusHours(5));
        record.setStatus(status);
        record.setCreatedBy(USER_ID);
        return record;
    }

    private SmartLockPasscodeRecord switchBotPendingRecordForReconciliation() throws Exception {
        Room room = room(1L, STORE_ID);
        SmartLockIntegration integration = switchBotIntegration();
        SmartLockDevice lock = device(20L, integration, "lock-A", "Lock", null, "{}");
        SmartLockDevice keypad = device(
                21L,
                integration,
                "keypad-A",
                "Keypad Touch",
                "lock-A",
                "{\"lockDeviceId\":\"lock-A\"}"
        );
        SmartLockRoomBinding binding = dualRoleBinding(100L, room, lock, keypad);
        SmartLockPasscodeRecord record = passcodeRecord(301L, binding, SmartLockPasscodeStatus.PENDING);
        record.setCreatedAt(LocalDateTime.now(FIXED_CLOCK).minusMinutes(1));
        return record;
    }

    private void stubSwitchBotReconciliationCandidates(SmartLockPasscodeRecord... records) {
        when(passcodeRepository.findSwitchBotReconciliationCandidates(
                eq(STORE_ID),
                eq(SmartLockProvider.SWITCHBOT),
                eq(SmartLockPasscodeStatus.PENDING),
                eq(SmartLockPasscodeStatus.DELETE_PENDING),
                any()
        )).thenReturn(List.of(records));
    }

    private ListAppender<ILoggingEvent> attachReconciliationLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(SmartLockPasscodeReconciliationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachReconciliationLogAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(SmartLockPasscodeReconciliationService.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private boolean hasReconciliationLog(
            ListAppender<ILoggingEvent> appender,
            String expectedFragment
    ) {
        for (ILoggingEvent event : appender.list) {
            if (event.getFormattedMessage().contains(expectedFragment)) {
                return true;
            }
        }
        return false;
    }

    private ListAppender<ILoggingEvent> attachServiceLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(SmartLockService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachServiceLogAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(SmartLockService.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private boolean hasLogMessage(
            ListAppender<ILoggingEvent> appender,
            String expectedFragment
    ) {
        for (ILoggingEvent event : appender.list) {
            if (event.getFormattedMessage().contains(expectedFragment)) {
                return true;
            }
        }
        return false;
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

    private void stubCreateBindingDevices(Room room, SmartLockDevice... devices) {
        when(roomRepository.findByStoreIdAndId(STORE_ID, room.getId())).thenReturn(Optional.of(room));
        for (SmartLockDevice device : devices) {
            when(deviceRepository.findByStoreIdAndId(STORE_ID, device.getId())).thenReturn(Optional.of(device));
        }
    }

    private void stubNoRoleConflicts(SmartLockProvider provider) {
        when(bindingRepository.findActiveByAnyRoleProviderLockId(
                eq(STORE_ID),
                eq(provider),
                anyString(),
                eq(SmartLockBindingStatus.ACTIVE),
                any()
        )).thenReturn(List.of());
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

    private SmartLockCredentialData ttLockCredentials(
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        SmartLockCredentialData credentials = new SmartLockCredentialData();
        credentials.setProvider(SmartLockProvider.TTLOCK);
        credentials.setTtLockClientId("client-1");
        credentials.setTtLockClientSecret("secret-1");
        credentials.setTtLockUsername("owner@example.com");
        credentials.setTtLockPasswordMd5("5f4dcc3b5aa765d61d8327deb882cf99");
        credentials.setTtLockAccessToken(accessToken);
        credentials.setTtLockRefreshToken(refreshToken);
        credentials.setTtLockTokenExpiresAt(tokenExpiresAt);
        return credentials;
    }

    private String encryptedTtLockCredentials(
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) throws Exception {
        return credentialCrypto.encrypt(objectMapper.writeValueAsString(
                ttLockCredentials(accessToken, refreshToken, tokenExpiresAt)
        ));
    }

    private SmartLockCredentialData decryptCredentials(SmartLockIntegration integration) throws Exception {
        String json = credentialCrypto.decrypt(integration.getCredentialCiphertext());
        return objectMapper.readValue(json, SmartLockCredentialData.class);
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

    private SmartLockIntegration ttLockIntegration() {
        SmartLockIntegration integration = new SmartLockIntegration();
        integration.setId(11L);
        integration.setStoreId(STORE_ID);
        integration.setProvider(SmartLockProvider.TTLOCK);
        integration.setConnectionStatus(SmartLockIntegrationStatus.CONNECTED);
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
        private final SmartLockProvider provider;
        private int refreshTokenCalls;
        private int testConnectionCalls;
        private int unlockCalls;
        private int createPasscodeCalls;
        private int deletePasscodeCalls;
        private int queryTaskCalls;
        private RuntimeException testConnectionFailure;
        private RuntimeException refreshTokenFailure;
        private SmartLockCredentialData refreshTokenResult;
        private SmartLockCredentialData lastTestConnectionCredentials;
        private SmartLockCredentialData lastRefreshCredentials;
        private final List<String> unlockRequests = new ArrayList<>();
        private final List<String> lockRequests = new ArrayList<>();
        private final List<String> createPasscodeRequests = new ArrayList<>();
        private final List<String> deletePasscodeRequests = new ArrayList<>();
        private final List<String> listPasscodeRequests = new ArrayList<>();
        private SmartLockTaskStatus createPasscodeStatus = SmartLockTaskStatus.SUCCESS;
        private String createProviderTaskId = "provider-task-1";
        private String createProviderPasscodeId = "provider-passcode-1";
        private List<DeviceSnapshot> deviceSnapshots = List.of();
        private List<ProviderPasscodeSnapshot> passcodeSnapshots = List.of();
        private boolean passcodeDeviceFound = true;
        private boolean passcodeKeyListReadable = true;
        private final List<String> statusRequests = new ArrayList<>();
        private final Map<String, LockStatusSnapshot> statusSnapshots = new HashMap<>();
        private final Set<String> failedStatusDeviceIds = new HashSet<>();

        private FakeProviderClient(SmartLockProvider provider) {
            this.provider = provider;
        }

        @Override
        public SmartLockProvider getProvider() {
            return provider;
        }

        @Override
        public void testConnection(SmartLockCredentialData credentials) {
            testConnectionCalls++;
            lastTestConnectionCredentials = credentials.copy();
            if (testConnectionFailure != null) {
                throw testConnectionFailure;
            }
        }

        @Override
        public SmartLockCredentialData refreshToken(SmartLockCredentialData credentials) {
            refreshTokenCalls++;
            lastRefreshCredentials = credentials.copy();
            if (refreshTokenFailure != null) {
                throw refreshTokenFailure;
            }
            if (refreshTokenResult != null) {
                return refreshTokenResult.copy();
            }
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
            createPasscodeRequests.add(providerLockId);
            return new ProviderTaskResult(
                    createPasscodeStatus,
                    createProviderTaskId,
                    createProviderPasscodeId,
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
            deletePasscodeRequests.add(providerLockId);
            return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, "task-4", providerPasscodeId, "ok");
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
            listPasscodeRequests.add(providerLockId);
            return new ProviderPasscodeListSnapshot(
                    passcodeDeviceFound,
                    passcodeKeyListReadable,
                    "Keypad Touch",
                    true,
                    "lock-A",
                    "hub-A",
                    passcodeSnapshots
            );
        }

        @Override
        public ProviderTaskResult queryTask(SmartLockCredentialData credentials, String providerTaskId) {
            queryTaskCalls++;
            return new ProviderTaskResult(SmartLockTaskStatus.SUCCESS, providerTaskId, null, "ok");
        }
    }
}
