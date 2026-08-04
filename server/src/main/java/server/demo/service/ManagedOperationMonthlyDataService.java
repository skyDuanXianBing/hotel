package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationMonthlyData;
import server.demo.entity.ManagedOperationMonthlyFee;
import server.demo.entity.ManagedOperationSettings;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.repository.ManagedOperationMonthlyDataRepository;
import server.demo.repository.ManagedOperationMonthlyFeeRepository;
import server.demo.repository.ManagedOperationSettingsRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import server.demo.i18n.ApiMessages;

/**
 * 代运营「本月原始数据 + 费用」的持久化、单据编号建议、已存报表文件的读取。
 */
@Service
public class ManagedOperationMonthlyDataService {
    private final ManagedOperationSettingsRepository settingsRepository;
    private final ManagedOperationMonthlyDataRepository monthlyDataRepository;
    private final ManagedOperationMonthlyFeeRepository feeRepository;
    private final ManagedOperationSheetStorage sheetStorage;

    public ManagedOperationMonthlyDataService(
            ManagedOperationSettingsRepository settingsRepository,
            ManagedOperationMonthlyDataRepository monthlyDataRepository,
            ManagedOperationMonthlyFeeRepository feeRepository,
            ManagedOperationSheetStorage sheetStorage) {
        this.settingsRepository = settingsRepository;
        this.monthlyDataRepository = monthlyDataRepository;
        this.feeRepository = feeRepository;
        this.sheetStorage = sheetStorage;
    }

    @Transactional(readOnly = true)
    public ManagedOperationDtos.MonthlyDataResponse getMonthlyData(Long storeId, Long settingsId, String monthValue) {
        requireSettings(storeId, settingsId);
        YearMonth month = ManagedOperationRunFieldsValidator.requireMonth(monthValue);
        return monthlyDataRepository
                .findByStoreIdAndSettingsIdAndSettlementMonth(storeId, settingsId, month.toString())
                .map(data -> toResponse(storeId, data, true))
                .orElseGet(() -> emptyResponse(month.toString()));
    }

    @Transactional
    public ManagedOperationDtos.MonthlyDataResponse saveMonthlyData(
            Long storeId,
            Long settingsId,
            ManagedOperationDtos.MonthlyDataRequest request,
            MultipartFile airbnbFile,
            MultipartFile bookingFile) {
        if (request == null) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.26f60647de6f"));
        }
        ManagedOperationSettings settings = requireSettings(storeId, settingsId);
        YearMonth month = ManagedOperationRunFieldsValidator.requireMonth(request.settlementMonth());
        ManagedOperationRunFieldsValidator.validateFees(request.fees());
        ManagedOperationRunFieldsValidator.validateDocumentNumbers(request.invoiceNumber(), request.receiptNumber());
        ManagedOperationRunFieldsValidator.validateNote(request.note());

        ManagedOperationMonthlyData data = monthlyDataRepository
                .findByStoreIdAndSettingsIdAndSettlementMonth(storeId, settingsId, month.toString())
                .orElseGet(() -> {
                    ManagedOperationMonthlyData created = new ManagedOperationMonthlyData();
                    created.setStoreId(storeId);
                    created.setSettings(settings);
                    created.setSettlementMonth(month.toString());
                    return created;
                });

        data.setInvoiceNumber(trimTo(request.invoiceNumber(), 100));
        data.setInvoiceDate(request.invoiceDate());
        data.setPaymentDueDate(request.paymentDueDate());
        data.setReceiptNumber(trimTo(request.receiptNumber(), 100));
        data.setReceiptDate(request.receiptDate());
        data.setNote(trimTo(request.note(), 1000));

        swapSheet(storeId, settingsId, "airbnb", airbnbFile, data);
        swapSheet(storeId, settingsId, "booking", bookingFile, data);

        data = monthlyDataRepository.save(data);

        feeRepository.deleteByStoreIdAndMonthlyDataId(storeId, data.getId());
        feeRepository.flush();
        List<ManagedOperationDtos.FeeInput> fees = request.fees() == null ? List.of() : request.fees();
        int sort = 0;
        for (ManagedOperationDtos.FeeInput fee : fees) {
            ManagedOperationMonthlyFee entity = new ManagedOperationMonthlyFee();
            entity.setStoreId(storeId);
            entity.setMonthlyData(data);
            entity.setFeeType(fee.feeType());
            entity.setDescription(fee.description().strip());
            entity.setAmountGross(fee.amountGross());
            entity.setSortOrder(sort++);
            feeRepository.save(entity);
        }
        return toResponse(storeId, data, true);
    }

    @Transactional(readOnly = true)
    public ManagedOperationDtos.DocumentNumberSuggestion suggestDocumentNumbers(
            Long storeId, Long settingsId, String monthValue) {
        ManagedOperationSettings settings = requireSettings(storeId, settingsId);
        YearMonth month = ManagedOperationRunFieldsValidator.requireMonth(monthValue);
        int invoiceDay = settings.getInvoiceIssueDay() == null
                ? ManagedOperationSettingsService.DEFAULT_INVOICE_ISSUE_DAY
                : settings.getInvoiceIssueDay();
        int receiptDay = settings.getReceiptIssueDay() == null
                ? ManagedOperationSettingsService.DEFAULT_RECEIPT_ISSUE_DAY
                : settings.getReceiptIssueDay();
        String invoicePrefix = String.format("%04d%02d%02d", month.getYear(), month.getMonthValue(), invoiceDay);
        String receiptPrefix = String.format("%04d%02d%02d", month.getYear(), month.getMonthValue(), receiptDay);
        int maxSeq = 0;
        for (ManagedOperationMonthlyData row
                : monthlyDataRepository.findByStoreIdAndSettingsId(storeId, settingsId)) {
            maxSeq = Math.max(maxSeq, sequenceOf(invoicePrefix, row.getInvoiceNumber()));
            maxSeq = Math.max(maxSeq, sequenceOf(invoicePrefix, row.getReceiptNumber()));
            maxSeq = Math.max(maxSeq, sequenceOf(receiptPrefix, row.getInvoiceNumber()));
            maxSeq = Math.max(maxSeq, sequenceOf(receiptPrefix, row.getReceiptNumber()));
        }
        // 请款书与收据共用同一序号流水：下一个可用序号给请款书，再下一个给收据
        String invoice = invoicePrefix + padSequence(maxSeq + 1);
        String receipt = receiptPrefix + padSequence(maxSeq + 2);
        return new ManagedOperationDtos.DocumentNumberSuggestion(
                invoice, receipt, month.atDay(invoiceDay), month.atDay(receiptDay), invoiceDay, receiptDay);
    }

    /**
     * 预览/导出时解析报表来源：优先使用本次新上传的文件，缺失时回退到本月已保存的文件。
     */
    @Transactional(readOnly = true)
    public ResolvedSheets resolveSheets(
            Long storeId, Long settingsId, String monthValue,
            MultipartFile airbnbFile, MultipartFile bookingFile) {
        ManagedOperationRunFieldsValidator.requireMonth(monthValue);
        MultipartFile airbnb = usable(airbnbFile) ? airbnbFile : null;
        MultipartFile booking = usable(bookingFile) ? bookingFile : null;
        if (airbnb == null || booking == null) {
            ManagedOperationMonthlyData data = monthlyDataRepository
                    .findByStoreIdAndSettingsIdAndSettlementMonth(storeId, settingsId, monthValue)
                    .orElse(null);
            if (data != null) {
                if (airbnb == null && data.getAirbnbFileKey() != null && !data.getAirbnbFileKey().isBlank()) {
                    airbnb = new StoredSheetMultipartFile("airbnbFile", data.getAirbnbFileName(),
                            sheetStorage.load(storeId, data.getAirbnbFileKey()).bytes());
                }
                if (booking == null && data.getBookingFileKey() != null && !data.getBookingFileKey().isBlank()) {
                    booking = new StoredSheetMultipartFile("bookingFile", data.getBookingFileName(),
                            sheetStorage.load(storeId, data.getBookingFileKey()).bytes());
                }
            }
        }
        if (airbnb == null || booking == null) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.a38454742c8d"));
        }
        return new ResolvedSheets(airbnb, booking);
    }

    private void swapSheet(
            Long storeId, Long settingsId, String kind, MultipartFile file, ManagedOperationMonthlyData data) {
        if (!usable(file)) return;
        String oldKey = "airbnb".equals(kind) ? data.getAirbnbFileKey() : data.getBookingFileKey();
        String newKey = sheetStorage.store(storeId, settingsId, kind, file);
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if ("airbnb".equals(kind)) {
            data.setAirbnbFileKey(newKey);
            data.setAirbnbFileName(trimTo(originalName, 255));
        } else {
            data.setBookingFileKey(newKey);
            data.setBookingFileName(trimTo(originalName, 255));
        }
        registerOldSheetCleanup(storeId, oldKey, newKey);
    }

    private void registerOldSheetCleanup(Long storeId, String oldKey, String newKey) {
        if (oldKey == null || oldKey.isBlank() || Objects.equals(oldKey, newKey)
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sheetStorage.deleteQuietly(storeId, oldKey);
            }
        });
    }

    private ManagedOperationDtos.MonthlyDataResponse toResponse(
            Long storeId, ManagedOperationMonthlyData data, boolean persisted) {
        List<ManagedOperationDtos.FeeInput> fees = feeRepository
                .findByStoreIdAndMonthlyDataIdOrderBySortOrderAscIdAsc(storeId, data.getId())
                .stream()
                .map(fee -> new ManagedOperationDtos.FeeInput(fee.getFeeType(), fee.getDescription(), fee.getAmountGross()))
                .toList();
        return new ManagedOperationDtos.MonthlyDataResponse(
                data.getSettlementMonth(), fees,
                data.getInvoiceNumber(), data.getInvoiceDate(), data.getPaymentDueDate(),
                data.getReceiptNumber(), data.getReceiptDate(), data.getNote(),
                data.getAirbnbFileName(), data.getBookingFileName(), persisted);
    }

    private static ManagedOperationDtos.MonthlyDataResponse emptyResponse(String month) {
        return new ManagedOperationDtos.MonthlyDataResponse(
                month, List.of(), "", null, null, "", null, "", "", "", false);
    }

    private ManagedOperationSettings requireSettings(Long storeId, Long settingsId) {
        if (settingsId == null || settingsId <= 0) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.c6b7d11d8389"));
        }
        return settingsRepository.findByStoreIdAndId(storeId, settingsId)
                .orElseThrow(() -> new ManagedOperationValidationException(ApiMessages.get("api.t.c6b7d11d8389")));
    }

    private static boolean usable(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static String trimTo(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.strip();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private static int sequenceOf(String prefix, String number) {
        if (number == null || !number.startsWith(prefix)) return 0;
        String suffix = number.substring(prefix.length());
        if (suffix.length() < 3 || !suffix.chars().allMatch(Character::isDigit)) return 0;
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String padSequence(int sequence) {
        return sequence < 1000 ? String.format("%03d", sequence) : String.valueOf(sequence);
    }

    public record ResolvedSheets(MultipartFile airbnbFile, MultipartFile bookingFile) {}
}
