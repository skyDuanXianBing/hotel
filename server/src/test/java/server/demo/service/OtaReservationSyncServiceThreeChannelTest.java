package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.entity.SuMessageThread;
import server.demo.entity.User;
import server.demo.enums.ChannelType;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ChannelRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.UserRepository;
import server.demo.util.SuChannelCatalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 三渠道（Expedia 9 / Trip.com 339 / Agoda 189）订单同步聚焦测试：
 * 渠道闸门、code→Su id 映射、幂等键稳定性与 webhook 全流程落库。
 */
class OtaReservationSyncServiceThreeChannelTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Long STORE_ID = 26L;

    private static final String TRIPCOM_PAYLOAD = """
            {
              "reservation_notif_id": "NOTIF_ID_7",
              "id": "TC-8899001122_HOTEL_ID",
              "channel_booking_id": "TC-8899001122",
              "hotel_id": "HOTEL_ID",
              "status": "new",
              "booked_at": "2026-06-01",
              "modified_at": "2026-06-01",
              "currencycode": "JPY",
              "paymenttype": "Hotel Collect",
              "commissionamount": "2900.00",
              "affiliation": { "pos": "Trip.com", "source": "trip.com", "OTA_Code": "339" },
              "customer": {
                "first_name": "Mei",
                "last_name": "Chen",
                "telephone": "+86-138-0000-0339",
                "countrycode": "CN",
                "guest_lang": "zh",
                "remarks": "Trip.com guest"
              },
              "rooms": [
                {
                  "id": "30",
                  "roomreservation_id": "177251687436100006",
                  "arrival_date": "2026-06-10",
                  "departure_date": "2026-06-12",
                  "roomstaystatus": "new",
                  "guest_name": "Mei Chen",
                  "numberofadults": "2",
                  "numberofchildren": "0",
                  "totalprice": "26336",
                  "specialrequest": "Non-smoking room",
                  "channel_room_id": "30"
                }
              ]
            }
            """;

    private static final String EXPEDIA_PAYLOAD = """
            {
              "reservation_notif_id": "NOTIF_ID_6",
              "id": "712345678901_HOTEL_ID",
              "channel_booking_id": "712345678901",
              "hotel_id": "HOTEL_ID",
              "status": "new",
              "booked_at": "2026-06-01",
              "modified_at": "2026-06-01",
              "currencycode": "JPY",
              "paymenttype": "Hotel Collect",
              "commissionamount": "2900.00",
              "affiliation": { "pos": "Expedia", "source": "expedia", "OTA_Code": "9" },
              "customer": {
                "first_name": "Emma",
                "last_name": "Brown",
                "telephone": "+1-415-555-0132",
                "countrycode": "US",
                "guest_lang": "en",
                "remarks": "Expedia VIP guest"
              },
              "rooms": [
                {
                  "id": "30",
                  "roomreservation_id": "177251687436100005",
                  "arrival_date": "2026-06-10",
                  "departure_date": "2026-06-12",
                  "roomstaystatus": "new",
                  "guest_name": "Emma Brown",
                  "numberofadults": "2",
                  "numberofchildren": "0",
                  "totalprice": "26336",
                  "specialrequest": "Non-smoking room",
                  "channel_room_id": "30"
                }
              ]
            }
            """;

    @Test
    void getSupportedChannelCodes_shouldContainAllFiveChannels() {
        OtaReservationSyncService service = createService(null);
        assertEquals(SuChannelCatalog.allCodes(), service.getSupportedChannelCodes());
        assertTrue(service.getSupportedChannelCodes().containsAll(
                List.of("BOOKING", "AIRBNB", "EXPEDIA", "TRIP", "AGODA")
        ));
    }

    @Test
    void toSuChannelId_shouldResolveAllFiveChannelsViaCatalog() {
        assertEquals(Integer.valueOf(19), OtaReservationSyncService.toSuChannelId("BOOKING"));
        assertEquals(Integer.valueOf(19), OtaReservationSyncService.toSuChannelId("booking.com"));
        assertEquals(Integer.valueOf(244), OtaReservationSyncService.toSuChannelId("AIRBNB"));
        assertEquals(Integer.valueOf(9), OtaReservationSyncService.toSuChannelId("EXPEDIA"));
        assertEquals(Integer.valueOf(339), OtaReservationSyncService.toSuChannelId("TRIP"));
        assertEquals(Integer.valueOf(339), OtaReservationSyncService.toSuChannelId("CTRIP"));
        assertEquals(Integer.valueOf(189), OtaReservationSyncService.toSuChannelId("AGODA"));
        assertNull(OtaReservationSyncService.toSuChannelId("UNKNOWN"));
        assertNull(OtaReservationSyncService.toSuChannelId("  "));
        assertNull(OtaReservationSyncService.toSuChannelId(null));
    }

    @Test
    void resolveCanonicalChannelBookingId_shouldPassThroughThreeChannelIdsWithoutBookingNormalization() {
        // Trip.com TC- 前缀原样保留
        assertEquals(
                "TC-8899001122",
                OtaReservationSyncService.resolveCanonicalChannelBookingId("TRIP", null, "TC-8899001122", null)
        );
        // Agoda AG- 前缀原样保留
        assertEquals(
                "AG-5566778899",
                OtaReservationSyncService.resolveCanonicalChannelBookingId("AGODA", null, "AG-5566778899", null)
        );
        // Expedia 纯数字单号原样保留
        assertEquals(
                "712345678901",
                OtaReservationSyncService.resolveCanonicalChannelBookingId("EXPEDIA", null, "712345678901", null)
        );
        // Booking 专用归一化不得泄漏到三渠道：Booking 形态订单号对 TRIP 保持原值
        assertEquals(
                "SU26-5003249282_W39FVCQYSN-1774939615039",
                OtaReservationSyncService.resolveCanonicalChannelBookingId(
                        "TRIP", null, "SU26-5003249282_W39FVCQYSN-1774939615039", null)
        );
        assertEquals(
                "5003249282_W39FVCQYSN",
                OtaReservationSyncService.resolveCanonicalChannelBookingId("EXPEDIA", null, "5003249282_W39FVCQYSN", null)
        );
    }

    @Test
    void resolveExternalBookingKey_shouldBeStableForThreeChannelIdShapes() {
        // TC-/AG- 前缀与纯数字单号：以 channel_booking_id 为幂等键，重放稳定
        String tripKey = OtaReservationSyncService.resolveExternalBookingKey(
                "TRIP", "TC-8899001122", "TC-8899001122_HOTEL_ID", "SU26-TC-8899001122_HOTEL_ID-177251687436100006");
        assertEquals("TC-8899001122", tripKey);
        assertEquals(tripKey, OtaReservationSyncService.resolveExternalBookingKey(
                "TRIP", "TC-8899001122", "TC-8899001122_HOTEL_ID", "SU26-TC-8899001122_HOTEL_ID-177251687436100006"));

        assertEquals("AG-5566778899", OtaReservationSyncService.resolveExternalBookingKey(
                "AGODA", "AG-5566778899", "AG-5566778899_HOTEL_ID", null));
        assertEquals("712345678901", OtaReservationSyncService.resolveExternalBookingKey(
                "EXPEDIA", "712345678901", "712345678901_HOTEL_ID", null));

        // channel_booking_id 缺失时回退 suReservationId 下划线前缀（与 AIRBNB 同款通用路径）
        assertEquals("TC-8899001122", OtaReservationSyncService.resolveExternalBookingKey(
                "TRIP", null, "TC-8899001122_HOTEL_ID", null));
        assertEquals("AG-5566778899", OtaReservationSyncService.resolveExternalBookingKey(
                "AGODA", "  ", "AG-5566778899_HOTEL_ID", null));

        // 单号与 Su reservation id 均缺失时才为 null（保持既有语义）
        assertNull(OtaReservationSyncService.resolveExternalBookingKey("TRIP", null, null, null));
    }

    @Test
    void mergeChannelOrderNumber_threeChannelsPreferIncomingAndKeepExistingOnBlank() {
        assertEquals("TC-8899001122",
                OtaReservationSyncService.mergeChannelOrderNumber("TRIP", "TC-OLD", "TC-8899001122", null));
        assertEquals("TC-OLD",
                OtaReservationSyncService.mergeChannelOrderNumber("TRIP", "TC-OLD", "   ", null));
        assertEquals("AG-5566778899",
                OtaReservationSyncService.mergeChannelOrderNumber("AGODA", null, "AG-5566778899", null));
        assertEquals("712345678901",
                OtaReservationSyncService.mergeChannelOrderNumber("EXPEDIA", null, "712345678901", null));
    }

    @Test
    void resolveThreadBookingId_threeChannelsUseGenericChannelOrderNumberPath() {
        Reservation reservation = new Reservation();
        reservation.setChannelOrderNumber("TC-8899001122");
        reservation.setOrderNumber("SU26-TC-8899001122_HOTEL_ID-177251687436100006");

        assertEquals("TC-8899001122",
                OtaReservationSyncService.resolveThreadBookingId("TRIP", reservation, null));

        Reservation noChannelOrder = new Reservation();
        noChannelOrder.setOrderNumber("SU26-AG-5566778899_HOTEL_ID-177251687436100007");
        assertEquals("SU26-AG-5566778899_HOTEL_ID-177251687436100007",
                OtaReservationSyncService.resolveThreadBookingId("AGODA", noChannelOrder, null));
    }

    @Test
    void resolveReservationTargetForUpsert_shouldMatchAgodaExternalBookingKeyUnique() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        Reservation existing = new Reservation();
        existing.setOrderNumber("SU26-AG-5566778899_HOTEL_ID-177251687436100007");

        when(repository.findByStoreIdAndOrderNumber(STORE_ID, "SU26-AG-5566778899_HOTEL_ID-177251687436100008"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(
                STORE_ID, "AG-5566778899_HOTEL_ID", "177251687436100008"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndChannelIdAndExternalBookingKey(STORE_ID, 189L, "AG-5566778899"))
                .thenReturn(List.of(existing));

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                STORE_ID,
                189L,
                "SU26-AG-5566778899_HOTEL_ID-177251687436100008",
                "AG-5566778899_HOTEL_ID",
                "177251687436100008",
                "AG-5566778899",
                "AG-5566778899",
                false
        );

        assertSame(existing, result.reservation());
        assertEquals("EXTERNAL_BOOKING_KEY_UNIQUE", result.matchStrategy());
        assertEquals("SU26-AG-5566778899_HOTEL_ID-177251687436100007", result.resolvedOrderNumber());
    }

    @Test
    void upsertReservationsFromWebhook_tripcomPayload_createsReservationAndReplayIsIdempotent() throws Exception {
        StoreRepository storeRepository = mock(StoreRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        RoomTypeInventoryLockService inventoryLockService = mock(RoomTypeInventoryLockService.class);

        stubStoreAndUser(storeRepository, userRepository);

        Channel channel = new Channel("Trip.com", "TRIP", ChannelType.OTA);
        channel.setId(339L);
        channel.setStoreId(STORE_ID);
        when(channelRepository.findByStoreIdAndCode(STORE_ID, "TRIP")).thenReturn(Optional.of(channel));

        when(reservationRepository.findByStoreIdAndOrderNumber(eq(STORE_ID), anyString()))
                .thenReturn(Optional.empty());
        when(inventoryLockService.lockRoomTypes(STORE_ID, Set.of(30L))).thenReturn(Set.of(30L));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });

        OtaReservationSyncService service = new OtaReservationSyncService(
                null,
                storeRepository,
                userRepository,
                mock(PricePlanRepository.class),
                channelRepository,
                reservationRepository,
                threadRepository,
                mock(OtaReservationRoomAssignmentService.class),
                inventoryLockService,
                new NoopTransactionManager(),
                null,
                mock(AutoMessageTriggerService.class),
                mock(CleaningTaskAutoService.class),
                null,
                null,
                null,
                null,
                mock(OrderNotificationDispatchService.class),
                null
        );

        JsonNode reservationNode = OBJECT_MAPPER.readTree(TRIPCOM_PAYLOAD);
        String expectedOrderNumber = "SU26-TC-8899001122_HOTEL_ID-177251687436100006";

        OtaReservationSyncService.UpsertOnlyResult first =
                service.upsertReservationsFromWebhook(STORE_ID, List.of(reservationNode));

        assertEquals(1, first.processedRoomStays());
        assertEquals(1, first.createdCount());
        assertEquals(0, first.updatedCount());
        assertEquals(0, first.failedCount());
        assertEquals(Set.of("NOTIF_ID_7"), first.processedNotifIds());

        ArgumentCaptor<Reservation> firstSaveCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(firstSaveCaptor.capture());
        Reservation saved = firstSaveCaptor.getValue();
        assertEquals(expectedOrderNumber, saved.getOrderNumber());
        assertEquals("TC-8899001122", saved.getChannelOrderNumber());
        assertEquals("TC-8899001122", saved.getExternalBookingKey());
        assertEquals("TC-8899001122_HOTEL_ID", saved.getSuReservationId());
        assertEquals("177251687436100006", saved.getRoomReservationId());
        assertEquals(channel.getId(), saved.getChannel().getId());
        assertEquals("Mei Chen", saved.getGuestName());
        assertEquals("+8613800000339", saved.getGuestPhone());
        assertEquals("CN", saved.getGuestCountry());
        assertEquals("zh", saved.getGuestLanguage());
        assertEquals(LocalDate.of(2026, 6, 10), saved.getCheckInDate());
        assertEquals(LocalDate.of(2026, 6, 12), saved.getCheckOutDate());
        assertEquals(new BigDecimal("26336"), saved.getTotalAmount());
        assertEquals("JPY", saved.getCurrencyCode());
        assertEquals(new BigDecimal("2900.00"), saved.getCommission());
        assertEquals("Hotel Collect", saved.getPaymentMethod());
        assertEquals(ReservationStatus.CONFIRMED, saved.getStatus());
        assertEquals("30", saved.getOtaRoomId());
        assertEquals(30L, saved.getOtaRoomTypeId());

        // B9 改查目录后：TRIP/AGODA 仍不得创建消息线程（Su 官方不支持消息；EXPEDIA 自 P4 起建线程）
        verify(threadRepository, never()).findByStoreIdAndChannelIdAndThreadKey(any(), any(), anyString());
        verify(threadRepository, never()).save(any());

        // 幂等重放：同一载荷第二次到达，按 ORDER_NUMBER 命中既有记录，更新而非新建
        Reservation existing = saved;
        when(reservationRepository.findByStoreIdAndOrderNumber(STORE_ID, expectedOrderNumber))
                .thenReturn(Optional.of(existing));

        OtaReservationSyncService.UpsertOnlyResult replay =
                service.upsertReservationsFromWebhook(STORE_ID, List.of(reservationNode));

        assertEquals(1, replay.processedRoomStays());
        assertEquals(0, replay.createdCount());
        assertEquals(1, replay.updatedCount());
        assertEquals(0, replay.failedCount());

        ArgumentCaptor<Reservation> allSaves = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository, org.mockito.Mockito.times(2)).save(allSaves.capture());
        Reservation replayed = allSaves.getAllValues().get(1);
        assertSame(existing, replayed);
        assertEquals(77L, replayed.getId());
        assertEquals(expectedOrderNumber, replayed.getOrderNumber());
        assertEquals("TC-8899001122", replayed.getChannelOrderNumber());
        assertEquals("TC-8899001122", replayed.getExternalBookingKey());
    }

    @Test
    void upsertReservationsFromWebhook_expediaNumericPayload_createsReservation() throws Exception {
        StoreRepository storeRepository = mock(StoreRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        RoomTypeInventoryLockService inventoryLockService = mock(RoomTypeInventoryLockService.class);

        stubStoreAndUser(storeRepository, userRepository);

        Channel channel = new Channel("Expedia", "EXPEDIA", ChannelType.OTA);
        channel.setId(9L);
        channel.setStoreId(STORE_ID);
        when(channelRepository.findByStoreIdAndCode(STORE_ID, "EXPEDIA")).thenReturn(Optional.of(channel));

        when(reservationRepository.findByStoreIdAndOrderNumber(eq(STORE_ID), anyString()))
                .thenReturn(Optional.empty());
        when(inventoryLockService.lockRoomTypes(STORE_ID, Set.of(30L))).thenReturn(Set.of(30L));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(78L);
            return saved;
        });

        OtaReservationSyncService service = new OtaReservationSyncService(
                null,
                storeRepository,
                userRepository,
                mock(PricePlanRepository.class),
                channelRepository,
                reservationRepository,
                threadRepository,
                mock(OtaReservationRoomAssignmentService.class),
                inventoryLockService,
                new NoopTransactionManager(),
                null,
                mock(AutoMessageTriggerService.class),
                mock(CleaningTaskAutoService.class),
                null,
                null,
                null,
                null,
                mock(OrderNotificationDispatchService.class),
                null
        );

        JsonNode reservationNode = OBJECT_MAPPER.readTree(EXPEDIA_PAYLOAD);

        OtaReservationSyncService.UpsertOnlyResult result =
                service.upsertReservationsFromWebhook(STORE_ID, List.of(reservationNode));

        assertEquals(1, result.processedRoomStays());
        assertEquals(1, result.createdCount());
        assertEquals(0, result.failedCount());
        assertEquals(Set.of("NOTIF_ID_6"), result.processedNotifIds());

        ArgumentCaptor<Reservation> saveCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(saveCaptor.capture());
        Reservation saved = saveCaptor.getValue();
        assertEquals("SU26-712345678901_HOTEL_ID-177251687436100005", saved.getOrderNumber());
        assertEquals("712345678901", saved.getChannelOrderNumber());
        assertEquals("712345678901", saved.getExternalBookingKey());
        assertEquals("712345678901_HOTEL_ID", saved.getSuReservationId());
        assertEquals(channel.getId(), saved.getChannel().getId());
        assertEquals("Emma Brown", saved.getGuestName());
        assertEquals(ReservationStatus.CONFIRMED, saved.getStatus());

        // P4：EXPEDIA 为 Su 官方消息渠道，订单落库创建以 bookingid 为会话键的消息线程
        // （TRIP/AGODA 官方不支持消息，仍不建线程，见 tripcom 用例的 never 断言）
        ArgumentCaptor<SuMessageThread> threadCaptor = ArgumentCaptor.forClass(SuMessageThread.class);
        verify(threadRepository).save(threadCaptor.capture());
        SuMessageThread thread = threadCaptor.getValue();
        assertEquals(Integer.valueOf(9), thread.getChannelId());
        assertEquals("712345678901", thread.getThreadKey());
        assertEquals("712345678901", thread.getBookingId());
    }

    private static void stubStoreAndUser(StoreRepository storeRepository, UserRepository userRepository) {
        Store store = new Store();
        store.setId(STORE_ID);
        store.setUserId(100L);
        store.setName("Store 26");
        store.setSuHotelId("HOTEL_ID");
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));

        User user = new User();
        user.setId(100L);
        user.setUsername("owner");
        user.setEmail("owner@example.test");
        user.setPassword("secret");
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
    }

    private static OtaReservationSyncService createService(ReservationRepository reservationRepository) {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        return new OtaReservationSyncService(
                null,
                null,
                null,
                null,
                null,
                reservationRepository,
                null,
                null,
                null,
                transactionManager,
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
    }

    private static final class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }
    }
}
