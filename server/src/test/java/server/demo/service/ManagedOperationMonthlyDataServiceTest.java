package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationMonthlyData;
import server.demo.entity.ManagedOperationMonthlyFee;
import server.demo.entity.ManagedOperationSettings;
import server.demo.enums.ManagedOperationFeeType;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.repository.ManagedOperationMonthlyDataRepository;
import server.demo.repository.ManagedOperationMonthlyFeeRepository;
import server.demo.repository.ManagedOperationSettingsRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedOperationMonthlyDataServiceTest {
    private final ManagedOperationSettingsRepository settingsRepository =
            mock(ManagedOperationSettingsRepository.class);
    private final ManagedOperationMonthlyDataRepository monthlyDataRepository =
            mock(ManagedOperationMonthlyDataRepository.class);
    private final ManagedOperationMonthlyFeeRepository feeRepository =
            mock(ManagedOperationMonthlyFeeRepository.class);
    private final ManagedOperationSheetStorage sheetStorage = mock(ManagedOperationSheetStorage.class);
    private final ManagedOperationMonthlyDataService service = new ManagedOperationMonthlyDataService(
            settingsRepository, monthlyDataRepository, feeRepository, sheetStorage);

    @Test
    void suggestDocumentNumbers_shouldStartAtOneForFreshMonth() {
        stubSettings(9, 10);
        when(monthlyDataRepository.findByStoreIdAndSettingsId(1L, 9L)).thenReturn(List.of());

        ManagedOperationDtos.DocumentNumberSuggestion suggestion =
                service.suggestDocumentNumbers(1L, 9L, "2026-08");

        assertEquals("20260809001", suggestion.invoiceNumber());
        assertEquals("20260810002", suggestion.receiptNumber());
        assertEquals(LocalDate.of(2026, 8, 9), suggestion.invoiceDate());
        assertEquals(LocalDate.of(2026, 8, 10), suggestion.receiptDate());
        assertEquals(9, suggestion.invoiceIssueDay());
        assertEquals(10, suggestion.receiptIssueDay());
    }

    @Test
    void suggestDocumentNumbers_shouldUseConfiguredIssueDays() {
        stubSettings(25, 26);
        when(monthlyDataRepository.findByStoreIdAndSettingsId(1L, 9L)).thenReturn(List.of());

        ManagedOperationDtos.DocumentNumberSuggestion suggestion =
                service.suggestDocumentNumbers(1L, 9L, "2026-08");

        assertEquals("20260825001", suggestion.invoiceNumber());
        assertEquals("20260826002", suggestion.receiptNumber());
        assertEquals(LocalDate.of(2026, 8, 25), suggestion.invoiceDate());
        assertEquals(LocalDate.of(2026, 8, 26), suggestion.receiptDate());
    }

    @Test
    void suggestDocumentNumbers_shouldContinueSharedSequenceAcrossInvoiceAndReceipt() {
        stubSettings(9, 10);
        ManagedOperationMonthlyData sameMonth = monthly("2026-08", "20260809001", "20260810002");
        ManagedOperationMonthlyData otherMonth = monthly("2026-07", "20260709007", "20260710008");
        when(monthlyDataRepository.findByStoreIdAndSettingsId(1L, 9L))
                .thenReturn(List.of(sameMonth, otherMonth));

        ManagedOperationDtos.DocumentNumberSuggestion suggestion =
                service.suggestDocumentNumbers(1L, 9L, "2026-08");

        assertEquals("20260809003", suggestion.invoiceNumber());
        assertEquals("20260810004", suggestion.receiptNumber());
    }

    @Test
    void suggestDocumentNumbers_shouldIgnoreCustomNumbersAndOverflowToFourDigits() {
        stubSettings(9, 10);
        ManagedOperationMonthlyData custom = monthly("2026-08", "INV-CUSTOM-1", "");
        ManagedOperationMonthlyData overflow = monthly("2026-08", "20260809999", "");
        when(monthlyDataRepository.findByStoreIdAndSettingsId(1L, 9L))
                .thenReturn(List.of(custom, overflow));

        ManagedOperationDtos.DocumentNumberSuggestion suggestion =
                service.suggestDocumentNumbers(1L, 9L, "2026-08");

        assertEquals("202608091000", suggestion.invoiceNumber());
        assertEquals("202608101001", suggestion.receiptNumber());
    }

    @Test
    void suggestDocumentNumbers_shouldRejectInvalidMonthAndForeignSettings() {
        when(settingsRepository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.empty());
        assertThrows(ManagedOperationValidationException.class,
                () -> service.suggestDocumentNumbers(1L, 9L, "2026-08"));

        stubSettings(9, 10);
        assertThrows(ManagedOperationValidationException.class,
                () -> service.suggestDocumentNumbers(1L, 9L, "2026-13"));
        assertThrows(ManagedOperationValidationException.class,
                () -> service.suggestDocumentNumbers(1L, 9L, " "));
    }

    @Test
    void getMonthlyData_shouldReturnEmptyResponseWhenNothingPersisted() {
        stubSettings(9, 10);
        when(monthlyDataRepository.findByStoreIdAndSettingsIdAndSettlementMonth(1L, 9L, "2026-08"))
                .thenReturn(Optional.empty());

        ManagedOperationDtos.MonthlyDataResponse response = service.getMonthlyData(1L, 9L, "2026-08");

        assertFalse(response.persisted());
        assertEquals("2026-08", response.settlementMonth());
        assertTrue(response.fees().isEmpty());
        assertEquals("", response.airbnbFileName());
    }

    @Test
    void saveMonthlyData_shouldPersistFieldsFeesAndFiles() {
        stubSettings(9, 10);
        when(monthlyDataRepository.findByStoreIdAndSettingsIdAndSettlementMonth(1L, 9L, "2026-08"))
                .thenReturn(Optional.empty());
        when(monthlyDataRepository.save(any(ManagedOperationMonthlyData.class))).thenAnswer(invocation -> {
            ManagedOperationMonthlyData saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(feeRepository.findByStoreIdAndMonthlyDataIdOrderBySortOrderAscIdAsc(anyLong(), anyLong()))
                .thenReturn(List.of());
        MockMultipartFile airbnb = new MockMultipartFile(
                "airbnbFile", "airbnb-202608.csv", "text/csv", "a,b".getBytes());
        when(sheetStorage.store(anyLong(), anyLong(), anyString(), any(MultipartFile.class)))
                .thenReturn("1/9/airbnb-uuid.csv");

        ManagedOperationDtos.MonthlyDataRequest request = new ManagedOperationDtos.MonthlyDataRequest(
                "2026-08",
                List.of(
                        new ManagedOperationDtos.FeeInput(ManagedOperationFeeType.DEDUCTION, "修理費", new BigDecimal("550")),
                        new ManagedOperationDtos.FeeInput(ManagedOperationFeeType.CREDIT, "祝い金", new BigDecimal("1100"))),
                "20260809001", LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 31),
                "20260809002", LocalDate.of(2026, 8, 9), "備考");

        ManagedOperationDtos.MonthlyDataResponse response =
                service.saveMonthlyData(1L, 9L, request, airbnb, null);

        assertTrue(response.persisted());
        assertEquals("20260809001", response.invoiceNumber());
        assertEquals("airbnb-202608.csv", response.airbnbFileName());
        verify(feeRepository, times(1)).deleteByStoreIdAndMonthlyDataId(1L, 5L);
        verify(feeRepository, times(2)).save(any(ManagedOperationMonthlyFee.class));
        verify(sheetStorage, times(1)).store(1L, 9L, "airbnb", airbnb);
    }

    @Test
    void saveMonthlyData_shouldRejectInvalidFeeAndMonth() {
        stubSettings(9, 10);
        ManagedOperationDtos.MonthlyDataRequest badFee = new ManagedOperationDtos.MonthlyDataRequest(
                "2026-08",
                List.of(new ManagedOperationDtos.FeeInput(ManagedOperationFeeType.DEDUCTION, "  ", new BigDecimal("100"))),
                "", null, null, "", null, "");
        assertThrows(ManagedOperationValidationException.class,
                () -> service.saveMonthlyData(1L, 9L, badFee, null, null));

        ManagedOperationDtos.MonthlyDataRequest badMonth = new ManagedOperationDtos.MonthlyDataRequest(
                "2026/08", List.of(), "", null, null, "", null, "");
        assertThrows(ManagedOperationValidationException.class,
                () -> service.saveMonthlyData(1L, 9L, badMonth, null, null));

        verify(monthlyDataRepository, never()).save(any(ManagedOperationMonthlyData.class));
    }

    @Test
    void resolveSheets_shouldPreferNewlyUploadedFiles() {
        stubSettings(9, 10);
        MockMultipartFile airbnb = new MockMultipartFile("airbnbFile", "a.csv", "text/csv", "a".getBytes());
        MockMultipartFile booking = new MockMultipartFile("bookingFile", "b.xlsx", "", "b".getBytes());

        ManagedOperationMonthlyDataService.ResolvedSheets sheets =
                service.resolveSheets(1L, 9L, "2026-08", airbnb, booking);

        assertEquals("a.csv", sheets.airbnbFile().getOriginalFilename());
        assertEquals("b.xlsx", sheets.bookingFile().getOriginalFilename());
        verify(sheetStorage, never()).load(anyLong(), anyString());
    }

    @Test
    void resolveSheets_shouldFallbackToStoredFiles() {
        stubSettings(9, 10);
        ManagedOperationMonthlyData data = monthly("2026-08", "", "");
        data.setAirbnbFileKey("1/9/airbnb-x.csv");
        data.setAirbnbFileName("saved-a.csv");
        data.setBookingFileKey("1/9/booking-x.csv");
        data.setBookingFileName("saved-b.csv");
        when(monthlyDataRepository.findByStoreIdAndSettingsIdAndSettlementMonth(1L, 9L, "2026-08"))
                .thenReturn(Optional.of(data));
        when(sheetStorage.load(1L, "1/9/airbnb-x.csv"))
                .thenReturn(new ManagedOperationSheetStorage.StoredSheet("a".getBytes()));
        when(sheetStorage.load(1L, "1/9/booking-x.csv"))
                .thenReturn(new ManagedOperationSheetStorage.StoredSheet("b".getBytes()));

        ManagedOperationMonthlyDataService.ResolvedSheets sheets =
                service.resolveSheets(1L, 9L, "2026-08", null, null);

        assertEquals("saved-a.csv", sheets.airbnbFile().getOriginalFilename());
        assertEquals("saved-b.csv", sheets.bookingFile().getOriginalFilename());
    }

    @Test
    void resolveSheets_shouldRejectWhenNoFilesAvailable() {
        stubSettings(9, 10);
        when(monthlyDataRepository.findByStoreIdAndSettingsIdAndSettlementMonth(1L, 9L, "2026-08"))
                .thenReturn(Optional.empty());

        assertThrows(ManagedOperationValidationException.class,
                () -> service.resolveSheets(1L, 9L, "2026-08", null, null));
    }

    private void stubSettings(int invoiceIssueDay, int receiptIssueDay) {
        ManagedOperationSettings settings = new ManagedOperationSettings();
        settings.setId(9L);
        settings.setStoreId(1L);
        settings.setInvoiceIssueDay(invoiceIssueDay);
        settings.setReceiptIssueDay(receiptIssueDay);
        when(settingsRepository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(settings));
    }

    private static ManagedOperationMonthlyData monthly(String month, String invoice, String receipt) {
        ManagedOperationMonthlyData data = new ManagedOperationMonthlyData();
        data.setStoreId(1L);
        data.setSettlementMonth(month);
        data.setInvoiceNumber(invoice);
        data.setReceiptNumber(receipt);
        return data;
    }
}
