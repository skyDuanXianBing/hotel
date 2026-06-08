package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.dto.SuMessagingMessagePageResponse;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.SuMessagingUnreadSummaryDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.ReservationStatus;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.ReservationRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingServiceTest {

    @Test
    void listThreadPage_shouldReturnClientFieldsAndClampSize() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuMessagingService service = newService(threadRepository, messageRepository, reservationRepository);

        SuMessageThread thread = newThread(1L, SuMessagingService.CHANNEL_AIRBNB, "T1", "B1", "B");
        when(threadRepository.findPageByStoreIdAndFilters(
                eq(26L),
                eq(SuMessagingService.CHANNEL_AIRBNB),
                isNull(),
                isNull(),
                isNull(),
                eq(SuMessagingSenderType.GUEST),
                eq(PageRequest.of(0, 100))
        )).thenReturn(new PageImpl<>(List.of(thread), PageRequest.of(0, 100), 101));
        when(messageRepository.countByThread_IdAndSenderTypeAndIsReadFalse(1L, SuMessagingSenderType.GUEST))
                .thenReturn(2L);

        SuMessagingThreadPageResponse result = service.listThreadPage(
                26L,
                -3,
                200,
                "AIRBNB",
                null,
                null,
                null,
                null,
                null
        );

        assertEquals(1, result.getItems().size());
        assertEquals(0, result.getPage());
        assertEquals(100, result.getSize());
        assertEquals(101, result.getTotalElements());
        assertTrue(result.isHasNext());
        assertEquals("UNMATCHED_ORDER", result.getItems().get(0).getOrderKind());

        SuMessagingThreadPageResponse contractProbe = new SuMessagingThreadPageResponse(
                List.of(new SuMessagingThreadDTO()),
                result.getPage(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isHasNext()
        );
        JsonNode json = new ObjectMapper().valueToTree(contractProbe);
        assertTrue(json.has("items"));
        assertTrue(json.has("page"));
        assertTrue(json.has("size"));
        assertTrue(json.has("hasNext"));
        assertFalse(json.has("content"));
        assertFalse(json.has("currentPage"));
        assertFalse(json.has("pageSize"));
        assertFalse(json.has("last"));
    }

    @Test
    void listThreadPage_shouldFilterAirbnbInquiryAndExcludeBookingFromInquiry() {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuMessagingService service = newService(threadRepository, messageRepository, reservationRepository);

        SuMessageThread airbnbInquiry = newThread(1L, SuMessagingService.CHANNEL_AIRBNB, "T1", "T1", "T");
        SuMessageThread airbnbBooking = newThread(2L, SuMessagingService.CHANNEL_AIRBNB, "T2", "B2", "B");
        SuMessageThread bookingThread = newThread(3L, SuMessagingService.CHANNEL_BOOKING, "BK1", "BK1", "T");

        when(threadRepository.findByStoreIdAndFilters(
                eq(26L),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(SuMessagingSenderType.GUEST)
        )).thenReturn(List.of(airbnbInquiry, airbnbBooking, bookingThread));
        when(messageRepository.countByThread_IdAndSenderTypeAndIsReadFalse(any(), eq(SuMessagingSenderType.GUEST)))
                .thenReturn(0L);

        SuMessagingThreadPageResponse result = service.listThreadPage(
                26L,
                0,
                20,
                null,
                "INQUIRY",
                null,
                null,
                null,
                null
        );

        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals("INQUIRY", result.getItems().get(0).getOrderKind());
    }

    @Test
    void listThreadPage_shouldClassifyAirbnbReservationStatuses() {
        Reservation requestedReservation = new Reservation();
        requestedReservation.setStatus(ReservationStatus.REQUESTED);
        Reservation confirmedReservation = new Reservation();
        confirmedReservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation checkedInReservation = new Reservation();
        checkedInReservation.setStatus(ReservationStatus.CHECKED_IN);
        Reservation cancelledReservation = new Reservation();
        cancelledReservation.setStatus(ReservationStatus.CANCELLED);
        Reservation noShowReservation = new Reservation();
        noShowReservation.setStatus(ReservationStatus.NO_SHOW);
        Reservation checkedOutReservation = new Reservation();
        checkedOutReservation.setStatus(ReservationStatus.CHECKED_OUT);

        SuMessageThread airbnbThread = newThread(1L, SuMessagingService.CHANNEL_AIRBNB, "T1", "B1", "B");
        SuMessageThread bookingThread = newThread(2L, SuMessagingService.CHANNEL_BOOKING, "BK1", "BK1", "T");

        assertEquals("REQUESTED", SuMessagingService.resolveOrderKind(airbnbThread, requestedReservation));
        assertEquals("CONFIRMED", SuMessagingService.resolveOrderKind(airbnbThread, confirmedReservation));
        assertEquals("CONFIRMED", SuMessagingService.resolveOrderKind(airbnbThread, checkedInReservation));
        assertEquals("CANCELLED", SuMessagingService.resolveOrderKind(airbnbThread, cancelledReservation));
        assertEquals("CANCELLED", SuMessagingService.resolveOrderKind(airbnbThread, noShowReservation));
        assertEquals("COMPLETED", SuMessagingService.resolveOrderKind(airbnbThread, checkedOutReservation));
        assertEquals("UNMATCHED_ORDER", SuMessagingService.resolveOrderKind(bookingThread, null));
    }

    @Test
    void getThreadMessagePage_shouldReturnItemsNextCursorAndMarkReadByDefault() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuMessagingService service = newService(threadRepository, messageRepository, reservationRepository);
        SuMessageThread thread = newThread(10L, SuMessagingService.CHANNEL_AIRBNB, "T1", "B1", "B");

        SuMessage newest = newMessage(103L, thread, "Newest");
        SuMessage middle = newMessage(102L, thread, "Middle");
        SuMessage oldest = newMessage(101L, thread, "Oldest");
        SuMessage overflow = newMessage(100L, thread, "Overflow");

        when(threadRepository.findByStoreIdAndId(26L, 10L)).thenReturn(Optional.of(thread));
        when(messageRepository.findMessagePageByCursorDesc(
                eq(26L),
                eq(10L),
                isNull(),
                isNull(),
                eq(PageRequest.of(0, 4))
        )).thenReturn(List.of(newest, middle, oldest, overflow));
        when(messageRepository.existsByStoreIdAndThread_IdAndIdLessThan(26L, 10L, 101L)).thenReturn(true);
        when(messageRepository.existsByStoreIdAndThread_IdAndIdGreaterThan(26L, 10L, 103L)).thenReturn(false);

        SuMessagingMessagePageResponse result = service.getThreadMessagePage(
                26L,
                10L,
                3,
                null,
                null,
                null
        );

        assertEquals(3, result.getItems().size());
        assertEquals(101L, result.getItems().get(0).getId());
        assertEquals(103L, result.getItems().get(2).getId());
        assertTrue(result.isHasMoreBefore());
        assertEquals(101L, result.getNextBeforeMessageId());
        assertFalse(result.isHasMoreAfter());
        verify(messageRepository).markThreadMessagesAsRead(10L, SuMessagingSenderType.GUEST);

        SuMessagingMessagePageResponse contractProbe = new SuMessagingMessagePageResponse(
                List.of(),
                result.getLimit(),
                result.isHasMoreBefore(),
                result.getNextBeforeMessageId(),
                result.isHasMoreAfter()
        );
        JsonNode json = new ObjectMapper().valueToTree(contractProbe);
        assertTrue(json.has("items"));
        assertTrue(json.has("nextBeforeMessageId"));
        assertFalse(json.has("content"));
        assertFalse(json.has("beforeMessageId"));
    }

    @Test
    void getThreadMessagePage_shouldClampLimitAndRespectMarkReadFalse() {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuMessagingService service = newService(threadRepository, messageRepository, reservationRepository);
        SuMessageThread thread = newThread(10L, SuMessagingService.CHANNEL_AIRBNB, "T1", "B1", "B");

        when(threadRepository.findByStoreIdAndId(26L, 10L)).thenReturn(Optional.of(thread));
        when(messageRepository.findMessagePageByCursorDesc(
                eq(26L),
                eq(10L),
                eq(99L),
                isNull(),
                eq(PageRequest.of(0, 101))
        )).thenReturn(List.of());

        SuMessagingMessagePageResponse result = service.getThreadMessagePage(
                26L,
                10L,
                300,
                99L,
                null,
                false
        );

        assertEquals(100, result.getLimit());
        assertTrue(result.getItems().isEmpty());
        Mockito.verify(messageRepository, Mockito.never())
                .markThreadMessagesAsRead(any(), eq(SuMessagingSenderType.GUEST));
    }

    @Test
    void getUnreadSummary_shouldReturnTotalUnreadClientField() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuMessagingService service = newService(threadRepository, messageRepository, reservationRepository);

        SuMessageRepository.ChannelUnreadSummaryRow channelRow = Mockito.mock(SuMessageRepository.ChannelUnreadSummaryRow.class);
        when(channelRow.getChannelId()).thenReturn(SuMessagingService.CHANNEL_AIRBNB);
        when(channelRow.getUnreadMessageCount()).thenReturn(7L);
        when(channelRow.getUnreadThreadCount()).thenReturn(2L);
        when(messageRepository.countUnreadMessagesByStoreId(26L, SuMessagingSenderType.GUEST)).thenReturn(9L);
        when(messageRepository.countUnreadThreadsByStoreId(26L, SuMessagingSenderType.GUEST)).thenReturn(3L);
        when(messageRepository.summarizeUnreadByChannel(26L, SuMessagingSenderType.GUEST))
                .thenReturn(List.of(channelRow));

        SuMessagingUnreadSummaryDTO result = service.getUnreadSummary(26L);

        assertEquals(9L, result.getTotalUnread());
        assertEquals(3L, result.getUnreadThreadCount());
        assertEquals(1, result.getByChannel().size());
        assertEquals(7L, result.getByChannel().get("AIRBNB"));

        JsonNode json = new ObjectMapper().valueToTree(result);
        assertTrue(json.has("totalUnread"));
        assertTrue(json.has("byChannel"));
        assertFalse(json.has("unreadMessageCount"));
        assertFalse(json.has("channels"));
    }

    @Test
    void listThreads_shouldResolveReservationByExternalBookingKeyFirst() {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                reservationBookingKeyResolver,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        SuMessageThread thread = new SuMessageThread();
        thread.setId(1L);
        thread.setStoreId(10L);
        thread.setChannelId(244);
        thread.setBookingId("HMKMSRREFW");
        thread.setThreadId("2500524048");
        thread.setLastActivity(LocalDateTime.now());

        Reservation reservation = new Reservation();
        reservation.setId(11L);
        reservation.setCheckInDate(LocalDate.of(2026, 4, 18));
        reservation.setCheckOutDate(LocalDate.of(2026, 4, 22));
        RoomType roomType = new RoomType();
        roomType.setName("Deluxe Twin");
        Room room = new Room();
        room.setRoomType(roomType);
        reservation.setRoom(room);

        when(threadRepository.findByStoreIdOrderByLastActivityDesc(10L)).thenReturn(List.of(thread));
        when(reservationRepository.findByStoreIdAndExternalBookingKeyWithRoomType(10L, "HMKMSRREFW"))
                .thenReturn(List.of(reservation));
        when(messageRepository.countByThread_IdAndSenderTypeAndIsReadFalse(1L, SuMessagingSenderType.GUEST))
                .thenReturn(0L);

        List<SuMessagingThreadDTO> result = service.listThreads(10L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getReservationId());
        assertEquals(LocalDate.of(2026, 4, 18), result.get(0).getCheckInDate());
        assertEquals(LocalDate.of(2026, 4, 22), result.get(0).getCheckOutDate());
        assertEquals("Deluxe Twin", result.get(0).getRoomTypeName());
        verify(reservationRepository).findByStoreIdAndExternalBookingKeyWithRoomType(10L, "HMKMSRREFW");
    }

    @Test
    void listThreads_shouldResolveReservationFromOrderNumberLikeValue() {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                reservationBookingKeyResolver,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        SuMessageThread thread = new SuMessageThread();
        thread.setId(2L);
        thread.setStoreId(26L);
        thread.setChannelId(244);
        thread.setBookingId("HM855QW52K");
        thread.setThreadId("2395583851");
        thread.setThreadKey("2395583851");
        thread.setLastActivity(LocalDateTime.now());

        Reservation reservation = new Reservation();
        reservation.setId(144L);
        reservation.setOrderNumber("SU26-HM855QW52K_W39FVCQYSN-1775039201599");
        reservation.setCheckInDate(LocalDate.of(2026, 4, 29));
        reservation.setCheckOutDate(LocalDate.of(2026, 5, 3));

        when(threadRepository.findByStoreIdOrderByLastActivityDesc(26L)).thenReturn(List.of(thread));
        when(reservationRepository.findByStoreIdAndExternalBookingKeyWithRoomType(26L, "HM855QW52K")).thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndChannelOrderNumberWithRoomType(26L, "HM855QW52K")).thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndOrderNumberWithRoomType(26L, "HM855QW52K")).thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndOrderNumberContainingWithRoomType(26L, "HM855QW52K"))
                .thenReturn(List.of(reservation));
        when(messageRepository.countByThread_IdAndSenderTypeAndIsReadFalse(2L, SuMessagingSenderType.GUEST))
                .thenReturn(0L);

        List<SuMessagingThreadDTO> result = service.listThreads(26L);

        assertEquals(1, result.size());
        assertEquals(144L, result.get(0).getReservationId());
        assertEquals(LocalDate.of(2026, 4, 29), result.get(0).getCheckInDate());
        assertEquals(LocalDate.of(2026, 5, 3), result.get(0).getCheckOutDate());
        verify(reservationRepository).findByStoreIdAndOrderNumberContainingWithRoomType(26L, "HM855QW52K");
    }

    @Test
    void markThreadAsRead_shouldClearGuestUnreadForStoreThread() {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                reservationBookingKeyResolver,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(10L);

        when(threadRepository.findByStoreIdAndId(10L, 5L)).thenReturn(Optional.of(thread));

        service.markThreadAsRead(10L, 5L);

        verify(messageRepository).markThreadMessagesAsRead(5L, SuMessagingSenderType.GUEST);
    }

    @Test
    void handleInboundMessage_shouldUpsertThreadSaveMessageAndBroadcast() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                reservationBookingKeyResolver,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        String raw = """
                {
                  "message": "Hello",
                  "guestid": "G1",
                  "bookingid": "B1",
                  "listingid": "L1",
                  "bookingflag": "B",
                  "messageid": "M1",
                  "channel_id": "244",
                  "threadid": "T1",
                  "hotelid": "STORE10",
                  "booking_details": {
                    "listing_name": "My Listing"
                  },
                  "user_details": {
                    "users": [{"first_name": "Alice"}]
                  }
                }
                """;
        JsonNode root = objectMapper.readTree(raw);

        when(messageRepository.findByStoreIdAndExternalMessageId(10L, "M1")).thenReturn(Optional.empty());
        when(threadRepository.findByStoreIdAndChannelIdAndThreadKey(10L, 244, "T1")).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(inv -> {
            SuMessageThread savedThread = inv.getArgument(0);
            savedThread.setId(99L);
            return savedThread;
        });
        when(messageRepository.save(any())).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(100L);
            return message;
        });

        service.handleInboundMessage(10L, "STORE10", root, raw);

        ArgumentCaptor<SuMessageThread> threadCaptor = ArgumentCaptor.forClass(SuMessageThread.class);
        verify(threadRepository).save(threadCaptor.capture());
        SuMessageThread savedThread = threadCaptor.getValue();
        assertEquals(10L, savedThread.getStoreId());
        assertEquals("STORE10", savedThread.getSuHotelId());
        assertEquals(244, savedThread.getChannelId());
        assertEquals("T1", savedThread.getThreadKey());
        assertEquals("T1", savedThread.getThreadId());
        assertEquals("B1", savedThread.getBookingId());
        assertEquals("G1", savedThread.getGuestId());
        assertEquals("L1", savedThread.getListingId());
        assertEquals("My Listing", savedThread.getListingName());
        assertEquals("Alice", savedThread.getGuestName());
        assertEquals("Hello", savedThread.getLastMessage());

        ArgumentCaptor<SuMessage> msgCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        SuMessage savedMsg = msgCaptor.getValue();
        assertEquals(10L, savedMsg.getStoreId());
        assertEquals("M1", savedMsg.getExternalMessageId());
        assertEquals(SuMessagingSenderType.GUEST, savedMsg.getSenderType());
        assertEquals("Hello", savedMsg.getContent());
        assertFalse(savedMsg.getIsRead());
        assertNotNull(savedMsg.getThread());

        verify(realtimeGateway).broadcastMessageCreated(eq(10L), eq(99L), any());
    }

    @Test
    void handleInboundMessage_shouldNormalizeBookingThreadIdentity() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                reservationBookingKeyResolver,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        String raw = """
                {
                  "message": "Hello",
                  "bookingid": "SU26-6022279490_W39FVCQYSN-1775048446300",
                  "threadid": "SU26-6022279490_W39FVCQYSN-1775048446300",
                  "listingid": "14844797",
                  "messageid": "M2",
                  "channel_id": "19",
                  "hotelid": "STORE10"
                }
                """;
        JsonNode root = objectMapper.readTree(raw);

        when(messageRepository.findByStoreIdAndExternalMessageId(10L, "M2")).thenReturn(Optional.empty());
        when(threadRepository.findByStoreIdAndChannelIdAndThreadKey(10L, 19, "6022279490")).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(inv -> {
            SuMessageThread savedThread = inv.getArgument(0);
            savedThread.setId(199L);
            return savedThread;
        });
        when(messageRepository.save(any())).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(200L);
            return message;
        });

        service.handleInboundMessage(10L, "STORE10", root, raw);

        ArgumentCaptor<SuMessageThread> threadCaptor = ArgumentCaptor.forClass(SuMessageThread.class);
        verify(threadRepository).save(threadCaptor.capture());
        SuMessageThread savedThread = threadCaptor.getValue();
        assertEquals("6022279490", savedThread.getThreadKey());
        assertEquals("6022279490", savedThread.getBookingId());
        assertEquals("SU26-6022279490_W39FVCQYSN-1775048446300", savedThread.getThreadId());
    }

    @Test
    void sendMessage_shouldCallSuMessagingABPersistStaffMessageAndBroadcast() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                reservationBookingKeyResolver,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(10L);
        thread.setSuHotelId("STORE10");
        thread.setChannelId(244);
        thread.setThreadId("T1");
        thread.setGuestId("G1");
        thread.setBookingId("B1");
        thread.setListingId("L1");
        thread.setClosed(false);

        when(threadRepository.findByStoreIdAndId(10L, 5L)).thenReturn(Optional.of(thread));

        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postMessagingAB(anyString(), any())).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);
        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) inv.getArgument(0);
            return fn.apply("token");
        });
        when(messageRepository.save(any())).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(101L);
            return message;
        });
        when(threadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuMessagingSendRequest request = new SuMessagingSendRequest();
        request.setContent("Hi");
        request.setSenderName("Front Desk");

        service.sendMessage(10L, 5L, request);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postMessagingAB(eq("token"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("STORE10", payload.get("hotelid"));
        assertEquals("244", payload.get("channelid"));
        assertEquals("L1", payload.get("listingid"));
        assertEquals("T1", payload.get("threadid"));
        assertEquals("G1", payload.get("guestid"));
        assertEquals("Hi", payload.get("message"));

        ArgumentCaptor<SuMessage> msgCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        SuMessage saved = msgCaptor.getValue();
        assertEquals(SuMessagingSenderType.STAFF, saved.getSenderType());
        assertEquals("Front Desk", saved.getSenderName());
        assertEquals(Boolean.TRUE, saved.getIsRead());

        verify(realtimeGateway).broadcastMessageCreated(eq(10L), eq(5L), any());
    }

    private static SuMessagingService newService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            ReservationRepository reservationRepository
    ) {
        ReservationBookingKeyResolver reservationBookingKeyResolver =
                new ReservationBookingKeyResolver(reservationRepository);
        return new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                reservationBookingKeyResolver,
                Mockito.mock(SuApiClient.class),
                Mockito.mock(SuAccessTokenService.class),
                new ObjectMapper(),
                Mockito.mock(SuMessagingRealtimeGateway.class)
        );
    }

    private static SuMessageThread newThread(
            Long id,
            Integer channelId,
            String threadId,
            String bookingId,
            String bookingFlag
    ) {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(id);
        thread.setStoreId(26L);
        thread.setSuHotelId("STORE26");
        thread.setChannelId(channelId);
        thread.setThreadKey(threadId);
        thread.setThreadId(threadId);
        thread.setBookingId(bookingId);
        thread.setBookingFlag(bookingFlag);
        thread.setGuestName("Guest " + id);
        thread.setListingId("L" + id);
        thread.setLastMessage("Last " + id);
        thread.setLastActivity(LocalDateTime.of(2026, 6, 1, 12, 0).plusMinutes(id));
        thread.setClosed(false);
        return thread;
    }

    private static SuMessage newMessage(Long id, SuMessageThread thread, String content) {
        SuMessage message = new SuMessage();
        message.setId(id);
        message.setStoreId(thread.getStoreId());
        message.setThread(thread);
        message.setSenderType(SuMessagingSenderType.GUEST);
        message.setSenderName("Guest");
        message.setContent(content);
        message.setDeliveryStatus("SENT");
        message.setSentAt(LocalDateTime.of(2026, 6, 1, 12, 0).plusMinutes(id));
        message.setIsRead(false);
        return message;
    }
}
