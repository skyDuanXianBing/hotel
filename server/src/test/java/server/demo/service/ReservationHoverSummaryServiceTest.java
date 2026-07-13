package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ReservationHoverSummaryResponseDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.PaymentRepository;
import server.demo.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationHoverSummaryServiceTest {

    private static final Long STORE_ID = 26L;
    private static final Long USER_ID = 7L;

    private ReservationService service;
    private ReservationRepository reservationRepository;
    private PaymentRepository paymentRepository;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "member"));
        service = new ReservationService();
        reservationRepository = mock(ReservationRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        permissionService = mock(PermissionService.class);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "paymentRepository", paymentRepository);
        ReflectionTestUtils.setField(service, "permissionService", permissionService);
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void enforcementDisabled_shouldDedupePreserveOrderAndAggregatePaymentsOnce() {
        Reservation first = reservation(101L, 11L, "111", LocalDateTime.of(2026, 7, 12, 10, 0));
        Reservation second = reservation(102L, 12L, "222", LocalDateTime.of(2026, 7, 12, 11, 0));
        when(permissionService.isEnforcementEnabled()).thenReturn(false);
        when(reservationRepository.findAssignedByStoreIdAndIdInWithRoomType(
                STORE_ID, List.of(102L, 101L))).thenReturn(List.of(first, second));
        PaymentRepository.ReservationPaymentTotal total = paymentTotal(101L, new BigDecimal("35.50"));
        when(paymentRepository.sumAmountsByStoreIdAndReservationIds(
                STORE_ID, List.of(102L, 101L))).thenReturn(List.of(total));

        ReservationHoverSummaryResponseDTO response = service.getHoverSummaries(List.of(102L, 101L, 102L));

        assertTrue(response.getCapabilities().isGuestPhone());
        assertTrue(response.getCapabilities().isFinancial());
        assertEquals(List.of(102L, 101L), response.getItems().stream()
                .map(ReservationHoverSummaryResponseDTO.Item::getReservationId).toList());
        assertEquals(BigDecimal.ZERO, response.getItems().get(0).getPaidAmount());
        assertEquals(new BigDecimal("35.50"), response.getItems().get(1).getPaidAmount());
        assertEquals("222", response.getItems().get(0).getPhone());

        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);
        verify(reservationRepository).findAssignedByStoreIdAndIdInWithRoomType(eq(STORE_ID), ids.capture());
        assertEquals(List.of(102L, 101L), ids.getValue());
        verify(paymentRepository).sumAmountsByStoreIdAndReservationIds(STORE_ID, List.of(102L, 101L));
    }

    @Test
    void onlyGuestPhoneCapability_shouldFilterRoomTypeAndSkipPaymentQuery() {
        Reservation visible = reservation(101L, 11L, "111", LocalDateTime.of(2026, 7, 12, 10, 0));
        Reservation hidden = reservation(102L, 12L, "222", LocalDateTime.of(2026, 7, 12, 11, 0));
        when(permissionService.isEnforcementEnabled()).thenReturn(true);
        when(permissionService.hasPermission(
                STORE_ID, USER_ID, PermissionModule.ORDER, PermissionAction.VIEW_ORDERS)).thenReturn(true);
        when(permissionService.hasPermission(
                STORE_ID, USER_ID, PermissionModule.SENSITIVE, PermissionAction.VIEW_FINANCIAL_DATA)).thenReturn(false);
        when(permissionService.resolveRoomTypeScope(
                STORE_ID, USER_ID, PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_STATUS))
                .thenReturn(PermissionService.RoomTypeScope.of(Set.of(11L)));
        when(reservationRepository.findAssignedByStoreIdAndIdInWithRoomType(
                STORE_ID, List.of(101L, 102L, 999L))).thenReturn(List.of(visible, hidden));

        ReservationHoverSummaryResponseDTO response = service.getHoverSummaries(List.of(101L, 102L, 999L));

        assertTrue(response.getCapabilities().isGuestPhone());
        assertFalse(response.getCapabilities().isFinancial());
        assertEquals(1, response.getItems().size());
        assertEquals(101L, response.getItems().get(0).getReservationId());
        assertEquals("111", response.getItems().get(0).getPhone());
        assertNull(response.getItems().get(0).getPaidAmount());
        verify(paymentRepository, never()).sumAmountsByStoreIdAndReservationIds(any(), any());
    }

    @Test
    void onlyFinancialCapability_shouldOmitPhoneAndReturnPaymentTotal() {
        Reservation reservation = reservation(101L, 11L, "111", LocalDateTime.of(2026, 7, 12, 10, 0));
        when(permissionService.isEnforcementEnabled()).thenReturn(true);
        when(permissionService.hasPermission(
                STORE_ID, USER_ID, PermissionModule.ORDER, PermissionAction.VIEW_ORDERS)).thenReturn(false);
        when(permissionService.hasPermission(
                STORE_ID, USER_ID, PermissionModule.SENSITIVE, PermissionAction.VIEW_FINANCIAL_DATA)).thenReturn(true);
        when(permissionService.resolveRoomTypeScope(
                STORE_ID, USER_ID, PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_STATUS))
                .thenReturn(PermissionService.RoomTypeScope.all());
        when(reservationRepository.findAssignedByStoreIdAndIdInWithRoomType(
                STORE_ID, List.of(101L))).thenReturn(List.of(reservation));
        PaymentRepository.ReservationPaymentTotal total = paymentTotal(101L, new BigDecimal("-10.00"));
        when(paymentRepository.sumAmountsByStoreIdAndReservationIds(STORE_ID, List.of(101L)))
                .thenReturn(List.of(total));

        ReservationHoverSummaryResponseDTO response = service.getHoverSummaries(List.of(101L));

        assertFalse(response.getCapabilities().isGuestPhone());
        assertTrue(response.getCapabilities().isFinancial());
        assertNull(response.getItems().get(0).getPhone());
        assertEquals(new BigDecimal("-10.00"), response.getItems().get(0).getPaidAmount());
    }

    @Test
    void noSensitiveCapabilities_shouldOmitBothFieldsAndSkipPaymentQuery() {
        Reservation reservation = reservation(101L, 11L, "111", LocalDateTime.of(2026, 7, 12, 10, 0));
        when(permissionService.isEnforcementEnabled()).thenReturn(true);
        when(permissionService.resolveRoomTypeScope(
                STORE_ID, USER_ID, PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_STATUS))
                .thenReturn(PermissionService.RoomTypeScope.all());
        when(reservationRepository.findAssignedByStoreIdAndIdInWithRoomType(
                STORE_ID, List.of(101L))).thenReturn(List.of(reservation));

        ReservationHoverSummaryResponseDTO response = service.getHoverSummaries(List.of(101L));

        assertFalse(response.getCapabilities().isGuestPhone());
        assertFalse(response.getCapabilities().isFinancial());
        assertNull(response.getItems().get(0).getPhone());
        assertNull(response.getItems().get(0).getPaidAmount());
        verify(paymentRepository, never()).sumAmountsByStoreIdAndReservationIds(any(), any());
    }

    private Reservation reservation(Long id, Long roomTypeId, String phone, LocalDateTime updatedAt) {
        RoomType roomType = new RoomType();
        roomType.setId(roomTypeId);
        Room room = new Room();
        room.setRoomType(roomType);
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStoreId(STORE_ID);
        reservation.setRoom(room);
        reservation.setGuestPhone(phone);
        reservation.setUpdatedAt(updatedAt);
        return reservation;
    }

    private PaymentRepository.ReservationPaymentTotal paymentTotal(Long reservationId, BigDecimal paidAmount) {
        PaymentRepository.ReservationPaymentTotal total = mock(PaymentRepository.ReservationPaymentTotal.class);
        when(total.getReservationId()).thenReturn(reservationId);
        when(total.getPaidAmount()).thenReturn(paidAmount);
        return total;
    }
}
