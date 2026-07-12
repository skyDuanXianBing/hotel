package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.constants.ChannelPriceOtaSyncState;
import server.demo.entity.AutoMessage;
import server.demo.entity.Channel;
import server.demo.entity.ChannelPrice;
import server.demo.entity.Cleaner;
import server.demo.entity.OtaIntegration;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.CleanerRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;
import server.demo.service.ChannelE2ETestSupportService.TestSupportAccessException;
import server.demo.service.ChannelE2ETestSupportService.LocalCleanerFixture;
import server.demo.util.JwtUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChannelE2ETestSupportServiceTest {

    private static final String VALID_KEY = "local-test-key";

    @Test
    void validateTestSupportAccess_defaultDisabledRejectsAccess() {
        ChannelE2ETestSupportService service = newService(false, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(VALID_KEY)
        );

        assertEquals(403, exception.getStatusCode());
        assertEquals("本地渠道E2E test-support 未启用", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_enabledButEmptyKeyRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, " ");

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(VALID_KEY)
        );

        assertEquals(403, exception.getStatusCode());
        assertEquals("本地渠道E2E test-support key 未配置", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_missingHeaderRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(null)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("缺少 X-Test-Support-Key", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_blankHeaderRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess(" ")
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("缺少 X-Test-Support-Key", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_wrongKeyRejectsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        TestSupportAccessException exception = assertThrows(
                TestSupportAccessException.class,
                () -> service.validateTestSupportAccess("wrong-key")
        );

        assertEquals(403, exception.getStatusCode());
        assertEquals("X-Test-Support-Key 不匹配", exception.getMessage());
    }

    @Test
    void validateTestSupportAccess_correctKeyAllowsAccess() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        assertDoesNotThrow(() -> service.validateTestSupportAccess(" " + VALID_KEY + " "));
    }

    @Test
    void localSetupStoreTimezoneDefaultsToTokyo() {
        assertEquals(
                "Asia/Tokyo",
                ReflectionTestUtils.getField(ChannelE2ETestSupportService.class, "LOCAL_SETUP_TIME_ZONE")
        );
    }

    @Test
    void ensureLocalSetupStore_reusedStoreNormalizesTimezoneToTokyo() {
        StoreRepository storeRepository = mock(StoreRepository.class);
        Store existingStore = new Store();
        existingStore.setId(41L);
        existingStore.setTimezone("Asia/Shanghai");
        existingStore.setSuHotelId("LOCALE2EHOTEL");
        when(storeRepository.findAllBySuHotelIdOrderByIdAsc("LOCALE2EHOTEL")).thenReturn(List.of(existingStore));
        when(storeRepository.save(existingStore)).thenReturn(existingStore);
        ChannelE2ETestSupportService service = newService(true, VALID_KEY, storeRepository);

        User user = new User();
        user.setId(7L);
        user.setEmail("channel-e2e-local@thehosthub.test");
        List<String> created = new ArrayList<>();
        List<String> reused = new ArrayList<>();

        Store result = ReflectionTestUtils.invokeMethod(
                service,
                "ensureLocalSetupStore",
                user,
                created,
                reused
        );

        assertEquals(existingStore, result);
        assertEquals("Asia/Tokyo", existingStore.getTimezone());
        assertTrue(created.isEmpty());
        assertTrue(reused.contains("storeTimezone:Asia/Tokyo"));
        assertTrue(reused.contains("store:41"));
        verify(storeRepository).save(existingStore);
    }

    @Test
    void hasLocalSetupAutoMessageTemplate_requiresEnabledBookingConfirmMarkerAndBookingChannel() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);
        Channel bookingChannel = new Channel();
        bookingChannel.setId(19L);
        bookingChannel.setCode("BOOKING");
        AutoMessage template = new AutoMessage();
        template.setEnabled(true);
        template.setAction("BOOKING_CONFIRM");
        template.setSendTiming("IMMEDIATELY");
        template.setChannels("[19]");
        template.setMessage("LOCAL_E2E_AUTO_BOOKING_CONFIRM hello");

        Boolean matched = ReflectionTestUtils.invokeMethod(
                service,
                "hasLocalSetupAutoMessageTemplate",
                template,
                bookingChannel
        );

        assertTrue(Boolean.TRUE.equals(matched));

        template.setChannels("[244]");
        Boolean mismatched = ReflectionTestUtils.invokeMethod(
                service,
                "hasLocalSetupAutoMessageTemplate",
                template,
                bookingChannel
        );
        assertFalse(Boolean.TRUE.equals(mismatched));
    }

    @Test
    void hasSupportedOtaIntegration_requiresSupportedEnabledAndConnectedIntegration() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        Boolean supported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("BOOKING", true, true))
        );

        assertTrue(Boolean.TRUE.equals(supported));
    }

    @Test
    void hasSupportedOtaIntegration_rejectsDisconnectedSupportedIntegration() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        Boolean supported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("BOOKING", true, false))
        );

        assertFalse(Boolean.TRUE.equals(supported));
    }

    @Test
    void hasSupportedOtaIntegration_rejectsDisabledAndUnsupportedIntegrations() {
        ChannelE2ETestSupportService service = newService(true, VALID_KEY);

        Boolean disabledSupported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("AIRBNB", false, true))
        );
        Boolean unsupported = ReflectionTestUtils.invokeMethod(
                service,
                "hasSupportedOtaIntegration",
                List.of(buildIntegration("EXPEDIA", true, true))
        );

        assertFalse(Boolean.TRUE.equals(disabledSupported));
        assertFalse(Boolean.TRUE.equals(unsupported));
    }

    @Test
    void ensureLocalSetupRoomTypePricePlan_createsBoundWeeklyPrices() {
        RoomTypePricePlanRepository repository = mock(RoomTypePricePlanRepository.class);
        ChannelE2ETestSupportService service = newService(true, VALID_KEY, null, repository, null);
        RoomType roomType = buildRoomType(4L, 11L, "Local E2E Standard Room", "E2ELOCAL");
        PricePlan pricePlan = buildPricePlan(21L);
        List<String> created = new ArrayList<>();
        List<String> reused = new ArrayList<>();

        when(repository.findByRoomTypeIdAndPricePlanId(11L, 21L)).thenReturn(Optional.empty());
        when(repository.save(any(RoomTypePricePlan.class))).thenAnswer(invocation -> {
            RoomTypePricePlan binding = invocation.getArgument(0);
            binding.setId(31L);
            return binding;
        });

        ReflectionTestUtils.invokeMethod(
                service,
                "ensureLocalSetupRoomTypePricePlan",
                4L,
                roomType,
                pricePlan,
                created,
                reused
        );

        ArgumentCaptor<RoomTypePricePlan> captor = ArgumentCaptor.forClass(RoomTypePricePlan.class);
        verify(repository).save(captor.capture());
        RoomTypePricePlan saved = captor.getValue();
        assertEquals(4L, saved.getStoreId());
        assertEquals(11L, saved.getRoomType().getId());
        assertEquals(21L, saved.getPricePlan().getId());
        assertEquals(new BigDecimal("120.00"), saved.getMondayPrice());
        assertEquals(new BigDecimal("120.00"), saved.getSundayPrice());
        assertEquals(2, saved.getIncludedGuests());
        assertTrue(created.contains("roomTypePricePlan:31"));
        assertTrue(reused.isEmpty());
    }

    @Test
    void ensureLocalSetupChannelPrices_createsReadOnlyChannelReferences() {
        ChannelPriceRepository repository = mock(ChannelPriceRepository.class);
        ChannelE2ETestSupportService service = newService(true, VALID_KEY, null, null, repository);
        RoomType roomType = buildRoomType(4L, 11L, "Local E2E Standard Room", "E2ELOCAL");
        PricePlan pricePlan = buildPricePlan(21L);
        Channel booking = buildChannel(41L, "BOOKING", "Booking.com");
        List<String> created = new ArrayList<>();
        List<String> reused = new ArrayList<>();

        when(repository.findByStoreIdAndRoomTypeIdAndPricePlanIdAndChannelIdAndPriceDate(
                any(),
                any(),
                any(),
                any(),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(repository.save(any(ChannelPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.invokeMethod(
                service,
                "ensureLocalSetupChannelPrices",
                4L,
                roomType,
                pricePlan,
                List.of(booking),
                created,
                reused
        );

        ArgumentCaptor<ChannelPrice> captor = ArgumentCaptor.forClass(ChannelPrice.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        ChannelPrice saved = captor.getAllValues().get(0);
        assertEquals(4L, saved.getStoreId());
        assertEquals(11L, saved.getRoomType().getId());
        assertEquals(21L, saved.getPricePlan().getId());
        assertEquals(41L, saved.getChannel().getId());
        assertEquals(new BigDecimal("120.00"), saved.getBasePrice());
        assertEquals(new BigDecimal("132.00"), saved.getChannelPrice());
        assertEquals(ChannelPriceOtaSyncState.NOT_REQUIRED, saved.getOtaSyncState());
        assertEquals(Boolean.TRUE, saved.getIsSyncedToOta());
        assertTrue(created.stream().anyMatch(item -> item.startsWith("channelPrices:")));
        assertTrue(reused.isEmpty());
    }

    @Test
    void ensureLocalSetupCleanerFixture_reusesOnlySafeReservedIdentityIdempotently() {
        UserRepository userRepository = mock(UserRepository.class);
        StoreUserRepository storeUserRepository = mock(StoreUserRepository.class);
        CleanerRepository cleanerRepository = mock(CleanerRepository.class);
        CleanerIdentityService identityService = mock(CleanerIdentityService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ChannelE2ETestSupportService service = newCleanerFixtureService(
                userRepository, storeUserRepository, cleanerRepository, identityService, jwtUtil);

        Store store = new Store(); store.setId(41L);
        User user = new User(); user.setId(21L); user.setEmail("local-e2e-cleaner@thehosthub.test");
        user.setUsername("local_e2e_cleaner"); user.setIsActive(true);
        StoreUser membership = new StoreUser(store, user, "member"); membership.setId(31L); membership.setIsActive(true);
        Cleaner cleaner = new Cleaner(); cleaner.setId(51L); cleaner.setStoreId(41L); cleaner.setUserId(21L);
        cleaner.setEmail("local-e2e-cleaner@thehosthub.test"); cleaner.setName("Local E2E Cleaner");
        cleaner.setPassword("encoded"); cleaner.setIsActive(true);

        when(userRepository.findByEmail("local-e2e-cleaner@thehosthub.test")).thenReturn(Optional.of(user));
        when(storeUserRepository.findByUserId(21L)).thenReturn(List.of(membership));
        when(storeUserRepository.findByStoreIdAndUserId(41L, 21L)).thenReturn(Optional.of(membership));
        when(cleanerRepository.findByStoreIdAndEmailIgnoreCase(41L, "local-e2e-cleaner@thehosthub.test"))
                .thenReturn(List.of(cleaner));
        when(cleanerRepository.findByUserIdAndStoreId(21L, 41L)).thenReturn(List.of(cleaner));
        when(cleanerRepository.findByEmail("local-e2e-cleaner@thehosthub.test")).thenReturn(Optional.of(cleaner));
        when(identityService.ensureCleanerIdentity(cleaner)).thenReturn(cleaner);
        when(jwtUtil.generateToken(21L, "local-e2e-cleaner@thehosthub.test")).thenReturn("cleaner-token");

        List<String> created = new ArrayList<>(); List<String> reused = new ArrayList<>();
        LocalCleanerFixture first = ReflectionTestUtils.invokeMethod(
                service, "ensureLocalSetupCleanerFixture", store, created, reused);
        LocalCleanerFixture second = ReflectionTestUtils.invokeMethod(
                service, "ensureLocalSetupCleanerFixture", store, created, reused);

        assertEquals(51L, first.cleanerId());
        assertEquals(first, second);
        assertTrue(created.isEmpty());
        verify(userRepository, never()).save(any(User.class));
        verify(storeUserRepository, never()).save(any(StoreUser.class));
        verify(cleanerRepository, never()).save(any(Cleaner.class));
    }

    @Test
    void ensureLocalSetupCleanerFixture_rejectsDisabledReservedUserWithoutReviving() {
        UserRepository userRepository = mock(UserRepository.class);
        StoreUserRepository storeUserRepository = mock(StoreUserRepository.class);
        CleanerRepository cleanerRepository = mock(CleanerRepository.class);
        CleanerIdentityService identityService = mock(CleanerIdentityService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        ChannelE2ETestSupportService service = newCleanerFixtureService(
                userRepository, storeUserRepository, cleanerRepository, identityService, jwtUtil);
        Store store = new Store(); store.setId(41L);
        User user = new User(); user.setId(21L); user.setEmail("local-e2e-cleaner@thehosthub.test");
        user.setUsername("local_e2e_cleaner"); user.setIsActive(false);
        when(userRepository.findByEmail("local-e2e-cleaner@thehosthub.test")).thenReturn(Optional.of(user));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(
                        service, "ensureLocalSetupCleanerFixture", store, new ArrayList<>(), new ArrayList<>()));

        assertTrue(exception.getMessage().contains("拒绝自动复活"));
        assertFalse(Boolean.TRUE.equals(user.getIsActive()));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(identityService, jwtUtil);
    }

    private ChannelE2ETestSupportService newService(boolean localE2EEnabled, String testSupportKey) {
        return newService(localE2EEnabled, testSupportKey, null, null, null);
    }

    private ChannelE2ETestSupportService newService(
            boolean localE2EEnabled,
            String testSupportKey,
            StoreRepository storeRepository
    ) {
        return newService(localE2EEnabled, testSupportKey, storeRepository, null, null);
    }

    private ChannelE2ETestSupportService newService(
            boolean localE2EEnabled,
            String testSupportKey,
            StoreRepository storeRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            ChannelPriceRepository channelPriceRepository
    ) {
        ChannelE2ETestSupportService service = new ChannelE2ETestSupportService(
                null,
                storeRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                roomTypePricePlanRepository,
                channelPriceRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(service, "localE2EEnabled", localE2EEnabled);
        ReflectionTestUtils.setField(service, "testSupportKey", testSupportKey);
        return service;
    }

    private ChannelE2ETestSupportService newCleanerFixtureService(
            UserRepository userRepository,
            StoreUserRepository storeUserRepository,
            CleanerRepository cleanerRepository,
            CleanerIdentityService identityService,
            JwtUtil jwtUtil
    ) {
        return new ChannelE2ETestSupportService(
                userRepository,
                null,
                storeUserRepository,
                cleanerRepository,
                identityService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                jwtUtil
        );
    }

    private RoomType buildRoomType(Long storeId, Long id, String name, String code) {
        RoomType roomType = new RoomType();
        roomType.setStoreId(storeId);
        roomType.setId(id);
        roomType.setName(name);
        roomType.setCode(code);
        return roomType;
    }

    private PricePlan buildPricePlan(Long id) {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(id);
        pricePlan.setName("Local E2E Standard Rate");
        return pricePlan;
    }

    private Channel buildChannel(Long id, String code, String name) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setCode(code);
        channel.setName(name);
        return channel;
    }

    private OtaIntegration buildIntegration(String code, boolean enabled, boolean connected) {
        OtaIntegration integration = new OtaIntegration();
        integration.setCode(code);
        integration.setEnabled(enabled);
        integration.setIsConnected(connected);
        return integration;
    }
}
