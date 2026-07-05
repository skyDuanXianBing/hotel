package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.Consumption;
import server.demo.entity.Note;
import server.demo.entity.Payment;
import server.demo.repository.ConsumptionRepository;
import server.demo.repository.NoteRepository;
import server.demo.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatisticsFinancialAggregationServiceTest {

    @Test
    void aggregate_shouldUseStoreScopedPaymentConsumptionAndNoteAmounts() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        ConsumptionRepository consumptionRepository = mock(ConsumptionRepository.class);
        NoteRepository noteRepository = mock(NoteRepository.class);
        StatisticsFinancialAggregationService service = new StatisticsFinancialAggregationService(
                paymentRepository,
                consumptionRepository,
                noteRepository
        );

        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 2);
        Payment receipt = buildPayment("payment", "wechat", "120.00", startDate);
        Payment deposit = buildPayment("收押金", "wechat", "50.00", startDate);
        Payment refund = buildPayment("refund", "cash", "20.00", endDate);
        Consumption consumption = buildConsumption("餐饮", "-35.00", startDate);
        Note incomeNote = buildNote("income", "souvenir", "alipay", "40.00",
                LocalDateTime.of(2026, 2, 1, 10, 0));
        Note expenseNote = buildNote("expense", "maintenance", "cash", "10.00",
                LocalDateTime.of(2026, 2, 2, 11, 0));

        when(paymentRepository.findActiveReservationPaymentsByStoreIdAndDateRange(26L, startDate, endDate))
                .thenReturn(List.of(receipt, deposit, refund));
        when(consumptionRepository.findActiveReservationConsumptionsByStoreIdAndDateRange(26L, startDate, endDate))
                .thenReturn(List.of(consumption));
        when(noteRepository.findByStoreIdAndDateRange(
                26L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(incomeNote, expenseNote));

        StatisticsFinancialAggregationService.FinancialAggregation result =
                service.aggregate(26L, startDate, endDate);

        assertEquals(new BigDecimal("120.00"), result.getActualReceived());
        assertEquals(new BigDecimal("50.00"), result.getDeposit());
        assertEquals(new BigDecimal("20.00"), result.getPaymentRefund());
        assertEquals(new BigDecimal("35.00"), result.getRoomServiceFee());
        assertEquals(new BigDecimal("40.00"), result.getNotesIncome());
        assertEquals(new BigDecimal("10.00"), result.getNotesExpense());

        StatisticsFinancialAggregationService.DailyFinancialAmount firstDay =
                result.getDailyAmountMap().get(startDate);
        assertEquals(new BigDecimal("120.00"), firstDay.getActualReceived());
        assertEquals(new BigDecimal("50.00"), firstDay.getDeposit());
        assertEquals(new BigDecimal("35.00"), firstDay.getRoomServiceFee());
        assertEquals(new BigDecimal("40.00"), firstDay.getNotesIncome());
        assertEquals(4, firstDay.getTransactionCount());

        StatisticsFinancialAggregationService.DailyFinancialAmount secondDay =
                result.getDailyAmountMap().get(endDate);
        assertEquals(new BigDecimal("20.00"), secondDay.getPaymentRefund());
        assertEquals(new BigDecimal("10.00"), secondDay.getNotesExpense());
        assertEquals(2, secondDay.getTransactionCount());
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
