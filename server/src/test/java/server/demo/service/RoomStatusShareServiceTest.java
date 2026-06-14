package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.dto.RoomStatusShareRequest;
import server.demo.dto.RoomStatusSharePublicResponse;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.entity.Room;
import server.demo.entity.RoomStatusShare;
import server.demo.enums.RoomStatus;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomStatusShareRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomStatusShareServiceTest {

    private final RoomStatusShareService service = new RoomStatusShareService();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createShare_shouldPersistCurrentStoreIdAndUserId() {
        RoomStatusShareRepository shareRepository = mock(RoomStatusShareRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        ReflectionTestUtils.setField(service, "shareRepository", shareRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");

        StoreContextHolder.setContext(new StoreContext(7L, 26L, "owner"));
        RoomStatusShareRequest request = new RoomStatusShareRequest();
        request.setShareTitle("BA share");
        request.setViewRoomStatus(true);
        request.setQueryMethod(true);
        request.setViewType("normal");
        request.setQueryMode("enabled");
        request.setAssociatedRooms(List.of(101L, 102L));

        when(shareRepository.existsByStoreIdAndShareTitle(26L, "BA share")).thenReturn(false);
        when(shareRepository.existsByShareToken(any(String.class))).thenReturn(false);
        when(shareRepository.save(any(RoomStatusShare.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepository.findByStoreIdAndIdIn(eq(26L), any()))
                .thenReturn(List.of(room(101L), room(102L)));

        RoomStatusShare created = service.createShare(request);

        assertEquals(26L, created.getStoreId());
        assertEquals(7L, created.getUserId());
        assertEquals("101,102", created.getAssociatedRoomIds());
    }

    @Test
    void getPublicShareByToken_shouldReturnPublicDtoWithoutInternalMetadata() {
        RoomStatusShareRepository shareRepository = mock(RoomStatusShareRepository.class);
        ReflectionTestUtils.setField(service, "shareRepository", shareRepository);

        RoomStatusShare share = publicShare();
        when(shareRepository.findByShareToken("share-token")).thenReturn(Optional.of(share));

        RoomStatusSharePublicResponse response = service.getPublicShareByToken("share-token");
        JsonNode responseJson = objectMapper.valueToTree(response);

        assertEquals("BA public share", responseJson.get("shareTitle").asText());
        assertEquals("blurred", responseJson.get("viewType").asText());
        assertEquals("arrivals,available", responseJson.get("filterItems").asText());
        assertEquals("guestName,channel", responseJson.get("orderItems").asText());
        assertEquals(7, responseJson.size());
        assertFalse(responseJson.has("id"));
        assertFalse(responseJson.has("userId"));
        assertFalse(responseJson.has("storeId"));
        assertFalse(responseJson.has("associatedRoomIds"));
        assertFalse(responseJson.has("shareToken"));
        assertFalse(responseJson.has("shareLink"));
    }

    @Test
    void filterRoomStatusByShare_shouldDropReservationSensitiveFieldsForNormalPublicPayload() {
        RoomStatusShare share = new RoomStatusShare();
        share.setViewType("normal");

        RoomStatusCalendarDTO filtered = service.filterRoomStatusByShare(share, buildCalendar());

        JsonNode dailyStatusJson = objectMapper.valueToTree(filtered).at("/rooms/0/dailyStatus/0");
        JsonNode reservationJson = dailyStatusJson.get("reservation");

        assertEquals(1L, filtered.getRooms().get(0).getRoomId());
        assertEquals("GuestA", reservationJson.get("guestName").asText());
        assertEquals("BOOKING", reservationJson.get("channel").asText());
        assertEquals("2025-01-01", reservationJson.get("checkIn").asText());
        assertEquals("2025-01-02", reservationJson.get("checkOut").asText());
        assertEquals("REQUESTED", reservationJson.get("status").asText());

        assertFalse(reservationJson.has("id"));
        assertFalse(reservationJson.has("orderNumber"));
        assertFalse(reservationJson.has("channelOrderNumber"));
        assertFalse(reservationJson.has("groupOrderNo"));
        assertFalse(reservationJson.has("notes"));
        assertFalse(reservationJson.has("remark"));
        assertFalse(reservationJson.has("specialRequests"));
        assertFalse(reservationJson.has("totalAmount"));
        assertFalse(dailyStatusJson.has("closeRemark"));
    }

    @Test
    void filterRoomStatusByShare_shouldBlurGuestNameWhenViewTypeIsBlurred() {
        RoomStatusShare share = new RoomStatusShare();
        share.setViewType("blurred");

        RoomStatusCalendarDTO filtered = service.filterRoomStatusByShare(share, buildCalendar());

        JsonNode reservationJson = objectMapper.valueToTree(filtered).at("/rooms/0/dailyStatus/0/reservation");

        assertEquals("G**", reservationJson.get("guestName").asText());
        assertFalse(reservationJson.has("id"));
        assertFalse(reservationJson.has("orderNumber"));
        assertFalse(reservationJson.has("groupOrderNo"));
        assertFalse(reservationJson.has("notes"));
        assertFalse(reservationJson.has("specialRequests"));
        assertFalse(reservationJson.has("totalAmount"));
    }

    private RoomStatusCalendarDTO buildCalendar() {
        DailyRoomStatusDTO.ReservationInfoDTO reservation =
                new DailyRoomStatusDTO.ReservationInfoDTO(
                        99L,
                        "GuestA",
                        "BOOKING",
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 2),
                        "ORD-1"
                );
        reservation.setStatus("REQUESTED");
        reservation.setTotalAmount(new BigDecimal("99.00"));
        reservation.setGroupOrderNo("GROUP-1");
        reservation.setNotes("private-note");
        reservation.setSpecialRequests("late arrival");

        DailyRoomStatusDTO dailyStatus = new DailyRoomStatusDTO(
                LocalDate.of(2025, 1, 1),
                RoomStatus.RESERVED,
                reservation,
                Boolean.FALSE,
                "",
                "internal close remark"
        );

        RoomStatusCalendarDTO.CalendarRoomDataDTO roomData =
                new RoomStatusCalendarDTO.CalendarRoomDataDTO(
                        10L,
                        "101",
                        "RT1",
                        List.of(dailyStatus)
                );

        return new RoomStatusCalendarDTO(
                new RoomStatusCalendarDTO.DateRangeDTO(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 1)
                ),
                List.of(roomData)
        );
    }

    private Room room(Long id) {
        Room room = new Room();
        room.setId(id);
        return room;
    }

    private RoomStatusShare publicShare() {
        RoomStatusShare share = new RoomStatusShare();
        share.setId(42L);
        share.setUserId(7L);
        share.setStoreId(26L);
        share.setShareTitle("BA public share");
        share.setShareToken("internal-token");
        share.setShareLink("https://example.test/share/internal-token");
        share.setViewRoomStatus(true);
        share.setQueryMethod(true);
        share.setViewType("blurred");
        share.setQueryMode("enabled");
        share.setFilterItems("arrivals,available");
        share.setOrderItems("guestName,channel");
        share.setAssociatedRoomIds("101,102");
        return share;
    }
}
