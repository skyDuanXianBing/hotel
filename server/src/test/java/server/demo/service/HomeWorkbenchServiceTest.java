package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CleaningTaskDTO;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.home.HomeWorkbenchItemDTO;
import server.demo.dto.home.HomeWorkbenchResponse;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

class HomeWorkbenchServiceTest {
    private static final long USER_ID = 3L;
    private static final long STORE_ID = 8L;

    private CleaningTaskService cleaningTaskService;
    private RegistrationAdminService registrationAdminService;
    private ReservationRepository reservationRepository;
    private SuMessagingService suMessagingService;
    private StoreRepository storeRepository;
    private HomeWorkbenchService service;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));

        cleaningTaskService = Mockito.mock(CleaningTaskService.class);
        registrationAdminService = Mockito.mock(RegistrationAdminService.class);
        reservationRepository = Mockito.mock(ReservationRepository.class);
        suMessagingService = Mockito.mock(SuMessagingService.class);
        storeRepository = Mockito.mock(StoreRepository.class);

        service = new HomeWorkbenchService(
                cleaningTaskService,
                registrationAdminService,
                reservationRepository,
                suMessagingService,
                storeRepository,
                Clock.fixed(Instant.parse("2026-06-12T00:00:00Z"), ZoneOffset.UTC)
        );

        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.empty());
        when(cleaningTaskService.getTasksWithFilters(
                anyLong(),
                any(),
                any(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any()
        )).thenReturn(new PageImpl<>(List.of()));
        when(registrationAdminService.list(
                eq(RegistrationFormStatus.SUBMITTED),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(List.of());
        when(suMessagingService.listThreadPage(
                eq(STORE_ID),
                eq(0),
                anyInt(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull()
        )).thenReturn(new SuMessagingThreadPageResponse(List.of(), 0, 50, 0, 0, false));
        when(reservationRepository.findUnassignedOrUnmappedWithDetailsByStoreId(eq(STORE_ID), any()))
                .thenReturn(List.of());
        when(reservationRepository.findPendingOrdersWithDetailsByStoreId(STORE_ID))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getWorkbench_deduplicatesOrderCandidatesAndKeepsUnassignedPriority() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        Reservation reservation = buildReservation(10L, "RSV-10", businessDate);

        when(reservationRepository.findUnassignedOrUnmappedWithDetailsByStoreId(STORE_ID, businessDate))
                .thenReturn(List.of(reservation));
        when(reservationRepository.findPendingOrdersWithDetailsByStoreId(STORE_ID))
                .thenReturn(List.of(reservation));

        HomeWorkbenchResponse response = service.getWorkbench(businessDate, 50);

        assertEquals(1, response.getItems().size());
        HomeWorkbenchItemDTO item = response.getItems().get(0);
        assertEquals("order", item.getType());
        assertEquals("UNASSIGNED", item.getSourceStatus());
        assertEquals("high", item.getPriority());
        assertEquals("pending", item.getStatusGroup());
        assertEquals("/order", item.getTarget().getPath());
        assertEquals("unassigned", item.getTarget().getQuery().get("type"));
        assertEquals("assign_room", item.getActions().get(1).getCode());
        assertEquals(1L, response.getTypeSummaries().get(2).getCount());
    }

    @Test
    void getWorkbench_usesUnreadThreadTotalForMessageSummary() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        when(reservationRepository.findUnassignedOrUnmappedWithDetailsByStoreId(STORE_ID, businessDate))
                .thenReturn(List.of());
        when(reservationRepository.findPendingOrdersWithDetailsByStoreId(STORE_ID))
                .thenReturn(List.of());

        SuMessagingThreadDTO thread = new SuMessagingThreadDTO();
        thread.setId(31L);
        thread.setGuestName("Yamada");
        thread.setChannelName("Booking.com");
        thread.setBookingId("BK-31");
        thread.setLastMessage("Arrival time?");
        thread.setLastActivity(OffsetDateTime.parse("2026-06-12T01:00:00Z"));
        thread.setUnreadCount(3L);
        when(suMessagingService.listThreadPage(
                eq(STORE_ID),
                eq(0),
                anyInt(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull()
        )).thenReturn(new SuMessagingThreadPageResponse(List.of(thread), 0, 5, 7, 2, true));

        HomeWorkbenchResponse response = service.getWorkbench(businessDate, 5);

        assertEquals(1, response.getItems().size());
        assertEquals("message", response.getItems().get(0).getType());
        assertEquals(3L, response.getItems().get(0).getUnreadCount());
        assertEquals("pending", response.getItems().get(0).getStatusGroup());
        assertEquals("31", response.getItems().get(0).getTarget().getQuery().get("suThreadId"));
        assertFalse(response.getItems().get(0).getTarget().getQuery().containsKey("threadId"));
        assertEquals(7L, response.getTypeSummaries().get(3).getCount());
        assertEquals("pending", response.getStatusSummaries().get(0).getStatusGroup());
        assertEquals(1L, response.getStatusSummaries().get(0).getCount());
    }

    @Test
    void getWorkbench_withMessageTypeReturnsMessageItemsOutsideGlobalLimit() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        Reservation reservation = buildReservation(10L, "RSV-10", businessDate);

        when(reservationRepository.findUnassignedOrUnmappedWithDetailsByStoreId(STORE_ID, businessDate))
                .thenReturn(List.of(reservation));
        when(reservationRepository.findPendingOrdersWithDetailsByStoreId(STORE_ID))
                .thenReturn(List.of());

        SuMessagingThreadDTO thread = buildMessageThread(31L, "Yamada", "BK-31");
        when(suMessagingService.listThreadPage(
                eq(STORE_ID),
                eq(0),
                anyInt(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull()
        )).thenReturn(new SuMessagingThreadPageResponse(List.of(thread), 0, 1, 16, 16, true));

        HomeWorkbenchResponse allResponse = service.getWorkbench(businessDate, 1);
        assertEquals(1, allResponse.getItems().size());
        assertEquals("order", allResponse.getItems().get(0).getType());
        assertEquals(16L, allResponse.getTypeSummaries().get(3).getCount());

        HomeWorkbenchResponse messageResponse = service.getWorkbench(businessDate, 1, "message");
        assertEquals(1, messageResponse.getItems().size());
        assertEquals("message", messageResponse.getItems().get(0).getType());
        assertEquals("31", messageResponse.getItems().get(0).getTarget().getQuery().get("suThreadId"));
        assertEquals(16L, messageResponse.getTypeSummaries().get(3).getCount());
        assertEquals("pending", messageResponse.getStatusSummaries().get(0).getStatusGroup());
        assertEquals(1L, messageResponse.getStatusSummaries().get(0).getCount());
    }

    @Test
    void getWorkbench_outputsFrontendActionContractForCleaningAssignment() {
        LocalDate businessDate = LocalDate.of(2026, 6, 12);
        CleaningTaskDTO task = new CleaningTaskDTO();
        task.setId(51L);
        task.setTaskDate(businessDate);
        task.setTaskType("checkout");
        task.setStatus("pending");
        task.setRoomNumber("201");
        task.setRoomType("Double");

        when(cleaningTaskService.getTasksWithFilters(
                anyLong(),
                eq(businessDate),
                eq(businessDate),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any()
        )).thenReturn(new PageImpl<>(List.of(task)));

        HomeWorkbenchResponse response = service.getWorkbench(businessDate, 50);

        HomeWorkbenchItemDTO item = response.getItems().get(0);
        assertEquals("cleaning", item.getType());
        assertEquals("pending", item.getStatusGroup());
        assertEquals("assign_cleaner", item.getActions().get(1).getCode());
        assertEquals("primary", item.getActions().get(1).getType());

        JsonNode actionJson = new ObjectMapper().valueToTree(item.getActions().get(1));
        assertTrue(actionJson.has("code"));
        assertTrue(actionJson.has("type"));
        assertFalse(actionJson.has("key"));
        assertFalse(actionJson.has("style"));
    }

    private static Reservation buildReservation(Long id, String orderNumber, LocalDate checkInDate) {
        Channel channel = new Channel();
        channel.setId(2L);
        channel.setName("Booking.com");

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStoreId(STORE_ID);
        reservation.setOrderNumber(orderNumber);
        reservation.setGuestName("Guest " + id);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkInDate.plusDays(1));
        reservation.setChannel(channel);
        return reservation;
    }

    private static SuMessagingThreadDTO buildMessageThread(Long id, String guestName, String bookingId) {
        SuMessagingThreadDTO thread = new SuMessagingThreadDTO();
        thread.setId(id);
        thread.setGuestName(guestName);
        thread.setChannelName("Booking.com");
        thread.setBookingId(bookingId);
        thread.setLastMessage("Arrival time?");
        thread.setLastActivity(OffsetDateTime.parse("2026-06-12T01:00:00Z"));
        thread.setUnreadCount(3L);
        return thread;
    }
}
