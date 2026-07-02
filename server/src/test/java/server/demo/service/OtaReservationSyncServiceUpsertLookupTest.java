package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.enums.ChannelType;
import server.demo.repository.ChannelRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.UserRepository;
import server.demo.util.SuReservationParser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtaReservationSyncServiceUpsertLookupTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void resolveReservationTargetForUpsert_shouldUseUniqueChannelFallbackWhenRoomReservationIdChanges() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        Reservation existing = new Reservation();
        existing.setOrderNumber("SU26-6588792273_W39FVCQYSN-OLD");

        when(repository.findByStoreIdAndOrderNumber(7L, "SU26-6588792273_W39FVCQYSN-NEW"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(7L, "6588792273_W39FVCQYSN", "1775048736013"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndChannelIdAndExternalBookingKey(7L, 19L, "6588792273"))
                .thenReturn(List.of());
        when(repository.findByStoreIdAndChannelOrderNumber(7L, "6588792273"))
                .thenReturn(List.of(existing));

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                7L,
                19L,
                "SU26-6588792273_W39FVCQYSN-NEW",
                "6588792273_W39FVCQYSN",
                "1775048736013",
                "6588792273",
                "6588792273",
                false
        );

        assertSame(existing, result.reservation());
        assertEquals("CHANNEL_ORDER_UNIQUE", result.matchStrategy());
        assertEquals("SU26-6588792273_W39FVCQYSN-OLD", result.resolvedOrderNumber());
    }

    @Test
    void resolveReservationTargetForUpsert_shouldPreferSuRoomKeyBeforeChannelFallback() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        Reservation existing = new Reservation();
        existing.setOrderNumber("SU26-EXISTING");

        when(repository.findByStoreIdAndOrderNumber(8L, "SU26-NEW"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(8L, "R-100", "ROOM-2"))
                .thenReturn(Optional.of(existing));

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                8L,
                19L,
                "SU26-NEW",
                "R-100",
                "ROOM-2",
                "BOOKING-200",
                "BOOKING-200",
                false
        );

        assertSame(existing, result.reservation());
        assertEquals("SU_ROOM_KEY", result.matchStrategy());
        assertEquals("SU26-EXISTING", result.resolvedOrderNumber());
        verify(repository, never()).findByStoreIdAndChannelIdAndExternalBookingKey(8L, 19L, "BOOKING-200");
        verify(repository, never()).findByStoreIdAndChannelOrderNumber(8L, "BOOKING-200");
    }

    @Test
    void resolveReservationTargetForUpsert_shouldInsertWhenChannelOrderMapsToMultipleRows() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        when(repository.findByStoreIdAndOrderNumber(9L, "SU26-NEW"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(9L, "R-200", "ROOM-3"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndChannelIdAndExternalBookingKey(9L, 19L, "BOOKING-300"))
                .thenReturn(List.of());
        when(repository.findByStoreIdAndChannelOrderNumber(9L, "BOOKING-300"))
                .thenReturn(List.of(new Reservation(), new Reservation()));

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                9L,
                19L,
                "SU26-NEW",
                "R-200",
                "ROOM-3",
                "BOOKING-300",
                "BOOKING-300",
                false
        );

        assertEquals("INSERT_NEW", result.matchStrategy());
        assertEquals("SU26-NEW", result.resolvedOrderNumber());
    }

    @Test
    void resolveReservationTargetForUpsert_shouldInsertNewForMultiRoomReservationBeforeOrderLevelFallback() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        when(repository.findByStoreIdAndOrderNumber(10L, "SU26-BOOKING-ROOM-2"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(10L, "6253752979_W39FVCQYSN", "1779165101753"))
                .thenReturn(Optional.empty());

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                10L,
                19L,
                "SU26-BOOKING-ROOM-2",
                "6253752979_W39FVCQYSN",
                "1779165101753",
                "6253752979",
                "6253752979",
                true
        );

        assertEquals("INSERT_NEW", result.matchStrategy());
        assertEquals("SU26-BOOKING-ROOM-2", result.resolvedOrderNumber());
        verify(repository, never()).findByStoreIdAndChannelIdAndExternalBookingKey(10L, 19L, "6253752979");
        verify(repository, never()).findByStoreIdAndChannelOrderNumber(10L, "6253752979");
    }


    @Test
    void mergeChannelOrderNumber_keepsExistingWhenIncomingBlank() {
        assertEquals(
                "5003249282",
                OtaReservationSyncService.mergeChannelOrderNumber("BOOKING", "5003249282", "   ", null)
        );
    }

    @Test
    void mergeChannelOrderNumber_prefersIncomingWhenProvided() {
        assertEquals(
                "5842688289",
                OtaReservationSyncService.mergeChannelOrderNumber("BOOKING", "5003249282", "5842688289", null)
        );
    }

    @Test
    void resolveCanonicalChannelBookingId_normalizesBookingFormattedOrderNumber() {
        assertEquals(
                "5003249282",
                OtaReservationSyncService.resolveCanonicalChannelBookingId(
                        "BOOKING",
                        null,
                        "SU26-5003249282_W39FVCQYSN-1774939615039",
                        null
                )
        );
    }

    @Test
    void resolveExternalBookingKey_shouldUseSuReservationPrefixForAirbnbWhenChannelBookingMissing() {
        assertEquals(
                "HMKMSRREFW",
                OtaReservationSyncService.resolveExternalBookingKey(
                        "AIRBNB",
                        null,
                        "HMKMSRREFW_W39FVCQYSN",
                        "SU26-HMKMSRREFW_W39FVCQYSN-1776072590757"
                )
        );
    }

    @Test
    void resolveExternalBookingKey_shouldUseBookingIdFromOrderNumberFallback() {
        assertEquals(
                "5526740549",
                OtaReservationSyncService.resolveExternalBookingKey(
                        "BOOKING",
                        null,
                        null,
                        "SU26-5526740549_W39FVCQYSN-1775048516093"
                )
        );
    }

    @Test
    void resolveReservationTimestamps_shouldKeepSuDateTimePrecision() throws Exception {
        JsonNode reservation = OBJECT_MAPPER.readTree("""
                {
                  "booked_at": "2026-01-30 14:35:11",
                  "modified_at": "2026-01-31 08:09:10"
                }
                """);

        assertEquals(
                LocalDateTime.of(2026, 1, 30, 14, 35, 11),
                OtaReservationSyncService.resolveBookingDateTimestamp(reservation)
        );
        assertEquals(
                LocalDateTime.of(2026, 1, 31, 8, 9, 10),
                OtaReservationSyncService.resolveModifiedAtTimestamp(reservation)
        );
    }

    @Test
    void resolveReservationTimestamps_shouldKeepDateOnlyCompatibility() throws Exception {
        JsonNode reservation = OBJECT_MAPPER.readTree("""
                {
                  "booked_at": "2026-01-30",
                  "modified_at": "0000-00-00"
                }
                """);

        assertEquals(
                LocalDateTime.of(2026, 1, 30, 0, 0),
                OtaReservationSyncService.resolveBookingDateTimestamp(reservation)
        );
        assertNull(OtaReservationSyncService.resolveModifiedAtTimestamp(reservation));
    }

    @Test
    void recordRegistrationLinkInboxIfAbsent_shouldDelegateForExistingReservationUpdate() {
        ReservationRepository repository = mock(ReservationRepository.class);
        RegistrationLinkInboxService registrationLinkInboxService = mock(RegistrationLinkInboxService.class);
        OtaReservationSyncService service = createService(repository, registrationLinkInboxService);

        Reservation reservation = new Reservation();
        reservation.setId(99L);
        reservation.setChannelOrderNumber("BOOK-UPDATE");

        service.recordRegistrationLinkInboxIfAbsent(26L, reservation, 2);

        verify(registrationLinkInboxService).recordIfAbsent(26L, reservation, 2);
    }

    @Test
    void upsertReservationsFromWebhook_shouldCallDailyPriceSyncWithParsedRoomPrices() throws Exception {
        StoreRepository storeRepository = mock(StoreRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PricePlanRepository pricePlanRepository = mock(PricePlanRepository.class);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        OtaReservationRoomAssignmentService roomAssignmentService = mock(OtaReservationRoomAssignmentService.class);
        AutoMessageTriggerService autoMessageTriggerService = mock(AutoMessageTriggerService.class);
        CleaningTaskAutoService cleaningTaskAutoService = mock(CleaningTaskAutoService.class);
        ReservationDailyPriceSyncService dailyPriceSyncService = mock(ReservationDailyPriceSyncService.class);
        OrderNotificationDispatchService orderNotificationDispatchService =
                mock(OrderNotificationDispatchService.class);

        Store store = new Store();
        store.setId(26L);
        store.setUserId(100L);
        store.setName("Store 26");
        store.setSuHotelId("HOTEL26");
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));

        User user = new User();
        user.setId(100L);
        user.setUsername("owner");
        user.setEmail("owner@example.test");
        user.setPassword("secret");
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));

        Channel channel = new Channel("Booking", "BOOKING", ChannelType.OTA);
        channel.setId(19L);
        channel.setStoreId(26L);
        when(channelRepository.findByStoreIdAndCode(26L, "BOOKING")).thenReturn(Optional.of(channel));

        when(reservationRepository.findByStoreIdAndOrderNumber(eq(26L), anyString()))
                .thenReturn(Optional.empty());
        when(reservationRepository.findByStoreIdAndSuReservationIdAndRoomReservationId(
                26L,
                "5003249282_W39FVCQYSN",
                "1775048736013"
        )).thenReturn(Optional.empty());
        when(reservationRepository.findByStoreIdAndChannelIdAndExternalBookingKey(26L, 19L, "5003249282"))
                .thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndChannelOrderNumber(26L, "5003249282"))
                .thenReturn(List.of());
        when(reservationRepository.save(org.mockito.ArgumentMatchers.any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation saved = invocation.getArgument(0);
                    saved.setId(77L);
                    return saved;
                });
        when(threadRepository.findByStoreIdAndChannelIdAndThreadKey(26L, 19, "5003249282"))
                .thenReturn(Optional.empty());
        when(threadRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OtaReservationSyncService service = new OtaReservationSyncService(
                null,
                storeRepository,
                userRepository,
                pricePlanRepository,
                channelRepository,
                reservationRepository,
                threadRepository,
                roomAssignmentService,
                new NoopTransactionManager(),
                null,
                autoMessageTriggerService,
                cleaningTaskAutoService,
                null,
                null,
                null,
                dailyPriceSyncService,
                orderNotificationDispatchService
        );

        JsonNode reservationNode = OBJECT_MAPPER.readTree("""
                {
                  "reservation_notif_id": "notif-1",
                  "id": "5003249282_W39FVCQYSN",
                  "channel_booking_id": "5003249282",
                  "status": "new",
                  "currencycode": "JPY",
                  "affiliation": { "OTA_Code": "19" },
                  "customer": { "first_name": "Daily", "last_name": "Guest" },
                  "rooms": [
                    {
                      "roomreservation_id": "1775048736013",
                      "arrival_date": "2026-02-01",
                      "departure_date": "2026-02-03",
                      "numberofadults": "2",
                      "numberofchildren": "0",
                      "totalprice": "220.00",
                      "price": [
                        {
                          "date": "2026-02-01",
                          "tax": "10.00",
                          "pricebeforetax": "100.00",
                          "priceaftertax": "110.00"
                        },
                        {
                          "date": "2026-02-02",
                          "tax": "12.00",
                          "pricebeforetax": "108.00",
                          "priceaftertax": "120.00"
                        }
                      ]
                    }
                  ]
                }
                """);

        OtaReservationSyncService.UpsertOnlyResult result =
                service.upsertReservationsFromWebhook(26L, List.of(reservationNode));

        assertEquals(1, result.processedRoomStays());
        assertEquals(1, result.createdCount());
        assertEquals(0, result.failedCount());

        ArgumentCaptor<Reservation> savedReservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        ArgumentCaptor<JsonNode> reservationNodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<JsonNode> roomStayCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(dailyPriceSyncService).syncDailyPrices(
                eq(26L),
                eq("HOTEL26"),
                savedReservationCaptor.capture(),
                reservationNodeCaptor.capture(),
                roomStayCaptor.capture()
        );

        Reservation savedReservation = savedReservationCaptor.getValue();
        assertEquals(77L, savedReservation.getId());
        assertEquals("5003249282_W39FVCQYSN", savedReservation.getSuReservationId());
        assertEquals("1775048736013", savedReservation.getRoomReservationId());
        assertEquals(LocalDate.of(2026, 2, 1), savedReservation.getCheckInDate());
        assertEquals(LocalDate.of(2026, 2, 3), savedReservation.getCheckOutDate());
        assertSame(reservationNode, reservationNodeCaptor.getValue());

        List<SuReservationParser.DailyRoomPrice> parsedPrices = SuReservationParser.extractDailyPrices(
                reservationNodeCaptor.getValue(),
                roomStayCaptor.getValue(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        assertEquals(2, parsedPrices.size());
        assertEquals(LocalDate.of(2026, 2, 1), parsedPrices.get(0).priceDate());
        assertEquals(new BigDecimal("110.00"), parsedPrices.get(0).priceAfterTax());
        assertEquals(LocalDate.of(2026, 2, 2), parsedPrices.get(1).priceDate());
        assertEquals(new BigDecimal("120.00"), parsedPrices.get(1).priceAfterTax());
    }

    private static OtaReservationSyncService createService(ReservationRepository reservationRepository) {
        return createService(reservationRepository, null);
    }

    private static OtaReservationSyncService createService(
            ReservationRepository reservationRepository,
            RegistrationLinkInboxService registrationLinkInboxService
    ) {
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
                transactionManager,
                null,
                null,
                null,
                null,
                registrationLinkInboxService,
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
