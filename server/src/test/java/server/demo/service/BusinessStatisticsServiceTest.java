package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.BusinessOverviewDTO;
import server.demo.dto.BusinessSummaryDTO;
import server.demo.dto.RevenuePrecisionDTO;
import server.demo.dto.RevenueSummaryDTO;
import server.demo.entity.Channel;
import server.demo.entity.Consumption;
import server.demo.entity.Note;
import server.demo.entity.Payment;
import server.demo.entity.Reservation;
import server.demo.enums.ChannelType;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ConsumptionRepository;
import server.demo.repository.NoteRepository;
import server.demo.repository.PaymentRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void getBusinessSummary_shouldUseAccommodationRevenueStatusesBeforeRevenueAllocation() {
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
        Reservation checkedOutReservation = buildReservation(102L, ReservationStatus.CHECKED_OUT);
        Reservation requestedReservation = buildReservation(103L, ReservationStatus.REQUESTED);
        Reservation noShowReservation = buildReservation(104L, ReservationStatus.NO_SHOW);
        Reservation cancelledReservation = buildReservation(105L, ReservationStatus.CANCELLED);
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        when(reservationRepository.findByStoreId(26L))
                .thenReturn(List.of(
                        activeReservation,
                        checkedOutReservation,
                        requestedReservation,
                        noShowReservation,
                        cancelledReservation
                ));
        when(roomRepository.countByStoreId(26L)).thenReturn(1L);
        when(revenueAllocationService.allocateRevenue(eq(26L), anyList(), eq(startDate), eq(endDate)))
                .thenReturn(new ReservationRevenueAllocationService.AllocationResult(
                        List.of(
                                new ReservationRevenueAllocationService.Allocation(
                                        activeReservation,
                                        startDate,
                                        new BigDecimal("100.00"),
                                        false
                                ),
                                new ReservationRevenueAllocationService.Allocation(
                                        checkedOutReservation,
                                        startDate,
                                        new BigDecimal("90.00"),
                                        false
                                )
                        ),
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

        assertEquals(List.of(activeReservation, checkedOutReservation), reservationsCaptor.getValue());
        assertEquals(2, summary.getTotalOrders());
        assertEquals(new BigDecimal("190.00"), summary.getTotalRevenue());
    }

    @Test
    void getRevenueSummary_shouldUseRealFinancialAggregatesAndNotChannelAsPaymentMethod() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        ReservationRevenueAllocationService revenueAllocationService =
                mock(ReservationRevenueAllocationService.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        ConsumptionRepository consumptionRepository = mock(ConsumptionRepository.class);
        NoteRepository noteRepository = mock(NoteRepository.class);
        StatisticsFinancialAggregationService financialAggregationService =
                new StatisticsFinancialAggregationService(paymentRepository, consumptionRepository, noteRepository);
        BusinessStatisticsService service = new BusinessStatisticsService();
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "revenueAllocationService", revenueAllocationService);
        ReflectionTestUtils.setField(service, "financialAggregationService", financialAggregationService);

        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 1);
        Reservation otaReservation = buildReservation(201L, ReservationStatus.CONFIRMED);
        Channel otaChannel = new Channel();
        otaChannel.setName("Booking");
        otaChannel.setType(ChannelType.OTA);
        otaReservation.setChannel(otaChannel);
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        when(reservationRepository.findByStoreId(26L)).thenReturn(List.of(otaReservation));
        when(revenueAllocationService.allocateRevenue(eq(26L), anyList(), eq(startDate), eq(endDate)))
                .thenReturn(new ReservationRevenueAllocationService.AllocationResult(
                        List.of(new ReservationRevenueAllocationService.Allocation(
                                otaReservation,
                                startDate,
                                new BigDecimal("100.00"),
                                true
                        )),
                        buildPrecision()
                ));
        when(paymentRepository.findActiveReservationPaymentsByStoreIdAndDateRange(26L, startDate, endDate))
                .thenReturn(List.of(
                        buildPayment("payment", "wechat", "80.00", startDate),
                        buildPayment("deposit", "wechat", "30.00", startDate),
                        buildPayment("refund", "cash", "20.00", startDate)
                ));
        when(consumptionRepository.findActiveReservationConsumptionsByStoreIdAndDateRange(26L, startDate, endDate))
                .thenReturn(List.of(buildConsumption("餐饮", "-12.00", startDate)));
        when(noteRepository.findByStoreIdAndDateRange(
                26L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(
                buildNote("income", "souvenir", "alipay", "7.00",
                        LocalDateTime.of(2026, 2, 1, 10, 0)),
                buildNote("expense", "maintenance", "cash", "2.00",
                        LocalDateTime.of(2026, 2, 1, 11, 0))
        ));

        RevenueSummaryDTO summary = service.getRevenueSummary(startDate, endDate);

        assertEquals(new BigDecimal("112.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("112.00"), summary.getTotalRevenue());
        assertEquals(new BigDecimal("22.00"), summary.getTotalExpense());
        assertEquals(new BigDecimal("90.00"), summary.getNetIncome());
        assertEquals(new BigDecimal("100.00"), summary.getRoomFee());
        assertEquals(new BigDecimal("100.00"), summary.getSplitAccount());
        assertEquals(new BigDecimal("80.00"), summary.getActualReceived());
        assertEquals(new BigDecimal("30.00"), summary.getDeposit());
        assertEquals(new BigDecimal("12.00"), summary.getRoomServiceFee());
        assertEquals(new BigDecimal("7.00"), summary.getNotesIncome());
        assertEquals(new BigDecimal("2.00"), summary.getNotesExpense());
        assertEquals(new BigDecimal("20.00"), summary.getPaymentRefund());
        assertFalse(summary.getPaymentMethodStats().stream()
                .anyMatch(stat -> "Booking".equals(stat.getPaymentMethod())));
        for (RevenueSummaryDTO.PaymentMethodStat stat : summary.getPaymentMethodStats()) {
            assertEquals(null, stat.getSourceType());
            assertEquals(null, stat.getNormalizedType());
        }
        assertEquals(0, summary.getSourceMetadata().size());
        assertEquals(0, summary.getDataGaps().size());
        assertEquals(new BigDecimal("112.00"), summary.getDailyRevenues().get(0).getTotalIncome());
        assertEquals(new BigDecimal("2.00"), summary.getDailyRevenues().get(0).getNotesExpense());
        assertEquals(new BigDecimal("20.00"), summary.getDailyRevenues().get(0).getPaymentRefund());
        assertEquals(new BigDecimal("22.00"), summary.getDailyRevenues().get(0).getTotalExpense());
        assertEquals(new BigDecimal("90.00"), summary.getDailyRevenues().get(0).getNetIncome());
    }

    @Test
    void getBusinessOverview_shouldUseAccommodationRevenueAndHideCheckoutGapDetails() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        ReservationRevenueAllocationService revenueAllocationService =
                mock(ReservationRevenueAllocationService.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        ConsumptionRepository consumptionRepository = mock(ConsumptionRepository.class);
        NoteRepository noteRepository = mock(NoteRepository.class);
        StatisticsFinancialAggregationService financialAggregationService =
                new StatisticsFinancialAggregationService(paymentRepository, consumptionRepository, noteRepository);
        BusinessStatisticsService service = new BusinessStatisticsService();
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "revenueAllocationService", revenueAllocationService);
        ReflectionTestUtils.setField(service, "financialAggregationService", financialAggregationService);

        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 1);
        Reservation reservation = buildReservation(301L, ReservationStatus.CONFIRMED);
        StoreContextHolder.setContext(new StoreContext(1L, 26L, "OWNER"));

        when(reservationRepository.findByStoreId(26L)).thenReturn(List.of(reservation));
        when(revenueAllocationService.allocateRevenue(eq(26L), anyList(), eq(startDate), eq(endDate)))
                .thenReturn(new ReservationRevenueAllocationService.AllocationResult(
                        List.of(new ReservationRevenueAllocationService.Allocation(
                                reservation,
                                startDate,
                                new BigDecimal("100.00"),
                                false
                        )),
                        buildPrecision()
                ));
        when(paymentRepository.findActiveReservationPaymentsByStoreIdAndDateRange(26L, startDate, endDate))
                .thenReturn(List.of(
                        buildPayment("deposit", "wechat", "30.00", startDate),
                        buildPayment("refund", "cash", "20.00", startDate)
                ));
        when(consumptionRepository.findActiveReservationConsumptionsByStoreIdAndDateRange(26L, startDate, endDate))
                .thenReturn(List.of(buildConsumption("餐饮", "-12.00", startDate)));
        when(noteRepository.findByStoreIdAndDateRange(
                26L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(
                buildNote("income", "souvenir", "alipay", "7.00",
                        LocalDateTime.of(2026, 2, 1, 10, 0)),
                buildNote("expense", "maintenance", "cash", "2.00",
                        LocalDateTime.of(2026, 2, 1, 11, 0))
        ));

        BusinessOverviewDTO overview = service.getBusinessOverview(startDate, endDate);

        assertEquals(new BigDecimal("112.00"), overview.getTotalRevenue());
        assertEquals(new BigDecimal("90.00"), overview.getNetRevenue());
        assertEquals(new BigDecimal("0"), overview.getCheckoutFee());
        assertEquals(0, overview.getSourceMetadata().size());
        assertEquals(0, overview.getDataGaps().size());
        assertFalse(overview.getCategoryDistribution().stream()
                .anyMatch(item -> "押金".equals(item.getCategory()) || "记一笔收入".equals(item.getCategory())));
        assertFalse(overview.getConsumptionDetails().stream()
                .anyMatch(item -> "退房金".equals(item.getCategory())));
        assertFalse(overview.getConsumptionDetails().stream()
                .anyMatch(item -> item.getSourceType() != null || Boolean.TRUE.equals(item.getUnsupported())));
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

    private Payment buildPayment(String type, String paymentMethod, String amount, LocalDate date) {
        Payment payment = new Payment();
        payment.setType(type);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(new BigDecimal(amount));
        payment.setDate(date);
        return payment;
    }

    private Consumption buildConsumption(String item, String amount, LocalDate date) {
        Consumption consumption = new Consumption();
        consumption.setItem(item);
        consumption.setAmount(new BigDecimal(amount));
        consumption.setDate(date);
        return consumption;
    }

    private Note buildNote(
            String type,
            String category,
            String paymentMethod,
            String amount,
            LocalDateTime datetime
    ) {
        Note note = new Note();
        note.setType(type);
        note.setCategory(category);
        note.setPaymentMethod(paymentMethod);
        note.setAmount(new BigDecimal(amount));
        note.setDatetime(datetime);
        return note;
    }
}
