package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.BusinessSummaryDTO;
import server.demo.dto.RevenuePrecisionDTO;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessStatisticsServiceTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getBusinessSummary_shouldExcludeCancelledReservationsBeforeRevenueAllocation() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        ReservationRevenueAllocationService revenueAllocationService =
                mock(ReservationRevenueAllocationService.class);
        BusinessStatisticsService service = new BusinessStatisticsService();
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "revenueAllocationService", revenueAllocationService);

        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 1);
        Reservation activeReservation = buildReservation(101L, ReservationStatus.CONFIRMED);
        Reservation cancelledReservation = buildReservation(102L, ReservationStatus.CANCELLED);
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        when(reservationRepository.findByStoreId(26L))
                .thenReturn(List.of(activeReservation, cancelledReservation));
        when(roomRepository.countByStoreId(26L)).thenReturn(1L);
        when(revenueAllocationService.allocateRevenue(eq(26L), anyList(), eq(startDate), eq(endDate)))
                .thenReturn(new ReservationRevenueAllocationService.AllocationResult(
                        List.of(new ReservationRevenueAllocationService.Allocation(
                                activeReservation,
                                startDate,
                                new BigDecimal("100.00"),
                                false
                        )),
                        buildPrecision()
                ));

        BusinessSummaryDTO summary = service.getBusinessSummary(startDate, endDate);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Reservation>> reservationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(revenueAllocationService).allocateRevenue(
                eq(26L),
                reservationsCaptor.capture(),
                eq(startDate),
                eq(endDate)
        );

        assertEquals(List.of(activeReservation), reservationsCaptor.getValue());
        assertEquals(1, summary.getTotalOrders());
        assertEquals(new BigDecimal("100.00"), summary.getTotalRevenue());
    }

    private Reservation buildReservation(Long id, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStoreId(26L);
        reservation.setStatus(status);
        reservation.setCheckInDate(LocalDate.of(2026, 2, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 2, 2));
        reservation.setTotalAmount(new BigDecimal("100.00"));
        reservation.setCurrencyCode("JPY");
        return reservation;
    }

    private RevenuePrecisionDTO buildPrecision() {
        RevenuePrecisionDTO precision = new RevenuePrecisionDTO();
        precision.setPriceBasis(ReservationRevenueAllocationService.PRICE_BASIS_AVERAGED_TOTAL_AMOUNT);
        precision.setDateBasis(ReservationRevenueAllocationService.DATE_BASIS_STAY_DATE);
        precision.setTaxMode(ReservationRevenueAllocationService.TAX_MODE_PRICE_AFTER_TAX);
        precision.setCurrencyCode("JPY");
        precision.setExactRoomNights(0);
        precision.setAveragedRoomNights(1);
        precision.setTotalRoomNights(1);
        precision.setCoverageRate(BigDecimal.ZERO);
        return precision;
    }
}
