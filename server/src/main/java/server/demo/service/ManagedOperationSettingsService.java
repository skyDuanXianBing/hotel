package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationMonthlyData;
import server.demo.entity.ManagedOperationRoom;
import server.demo.entity.ManagedOperationSettings;
import server.demo.entity.Room;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.repository.ManagedOperationMonthlyDataRepository;
import server.demo.repository.ManagedOperationMonthlyFeeRepository;
import server.demo.repository.ManagedOperationRoomRepository;
import server.demo.repository.ManagedOperationSettingsRepository;
import server.demo.repository.RoomRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import server.demo.i18n.ApiMessages;
@Service
public class ManagedOperationSettingsService {
    public static final int MIN_ISSUE_DAY = 1;
    public static final int MAX_ISSUE_DAY = 28;
    public static final int DEFAULT_INVOICE_ISSUE_DAY = 9;
    public static final int DEFAULT_RECEIPT_ISSUE_DAY = 10;

    private final ManagedOperationSettingsRepository settingsRepository;
    private final ManagedOperationRoomRepository managedRoomRepository;
    private final ManagedOperationMonthlyDataRepository monthlyDataRepository;
    private final ManagedOperationMonthlyFeeRepository monthlyFeeRepository;
    private final RoomRepository roomRepository;
    private final ManagedOperationPrivateStampStorage stampStorage;
    private final ManagedOperationSheetStorage sheetStorage;

    public ManagedOperationSettingsService(
            ManagedOperationSettingsRepository settingsRepository,
            ManagedOperationRoomRepository managedRoomRepository,
            ManagedOperationMonthlyDataRepository monthlyDataRepository,
            ManagedOperationMonthlyFeeRepository monthlyFeeRepository,
            RoomRepository roomRepository,
            ManagedOperationPrivateStampStorage stampStorage,
            ManagedOperationSheetStorage sheetStorage) {
        this.settingsRepository = settingsRepository;
        this.managedRoomRepository = managedRoomRepository;
        this.monthlyDataRepository = monthlyDataRepository;
        this.monthlyFeeRepository = monthlyFeeRepository;
        this.roomRepository = roomRepository;
        this.stampStorage = stampStorage;
        this.sheetStorage = sheetStorage;
    }

    @Transactional(readOnly = true)
    public List<ManagedOperationDtos.PropertySummary> listProperties(Long storeId) {
        Map<Long, Integer> roomCounts = managedRoomRepository.countByStoreIdGroupBySettings(storeId)
                .stream().collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()));
        return settingsRepository.findByStoreIdOrderByIdAsc(storeId).stream()
                .map(settings -> new ManagedOperationDtos.PropertySummary(
                        settings.getId(),
                        settings.getPropertyName(),
                        roomCounts.getOrDefault(settings.getId(), 0),
                        settings.getStampStorageKey() != null && !settings.getStampStorageKey().isBlank(),
                        settings.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public ManagedOperationDtos.SettingsResponse createProperty(Long storeId, ManagedOperationDtos.CreatePropertyRequest request) {
        String name = request == null ? "" : text(request.propertyName(), 200);
        requireText(name, ApiMessages.get("api.t.0d86438422da"));
        if (settingsRepository.existsByStoreIdAndPropertyName(storeId, name)) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.3b4bd940ec55"));
        }
        ManagedOperationSettings settings = defaultSettings(storeId);
        settings.setPropertyName(name);
        settings = settingsRepository.save(settings);
        return new ManagedOperationDtos.SettingsResponse(toDto(settings, List.of()), availableRooms(storeId), true);
    }

    @Transactional(readOnly = true)
    public ManagedOperationDtos.SettingsResponse getSettings(Long storeId, Long settingsId) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId, settingsId);
        List<Long> selected = managedRoomRepository
                .findByStoreIdAndSettingsIdWithRoom(storeId, settings.getId())
                .stream().map(link -> link.getRoom().getId()).toList();
        return new ManagedOperationDtos.SettingsResponse(toDto(settings, selected), availableRooms(storeId), true);
    }

    @Transactional
    public ManagedOperationDtos.SettingsResponse saveSettings(
            Long storeId, Long settingsId, ManagedOperationDtos.SettingsRequest request) {
        if (request == null) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.26f60647de6f"));
        }
        String propertyName = text(request.propertyName(), 200);
        requireText(propertyName, ApiMessages.get("api.t.0d86438422da"));
        requireText(request.ownerCompanyName(), ApiMessages.get("api.t.1e81fa8e9c37"));
        requireText(request.issuerCompanyName(), ApiMessages.get("api.t.ad9f84bcbe1a"));
        validateRate(request.managementFeeRate(), ApiMessages.get("api.t.b457f2525d4d"));
        validateRate(request.taxRate(), ApiMessages.get("api.t.49afb4e7bdf8"));
        validateMoney(request.cleaningFeeGross(), ApiMessages.get("api.t.ca2708ecfc3b"));
        validateMoney(request.registrationFeeNet(), ApiMessages.get("api.t.1fda0430b730"));
        validateIssueDay(request.invoiceIssueDay());
        validateIssueDay(request.receiptIssueDay());

        ManagedOperationSettings settings = requirePersistedSettings(storeId, settingsId);
        if (!settings.getPropertyName().equals(propertyName)
                && settingsRepository.existsByStoreIdAndPropertyName(storeId, propertyName)) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.3b4bd940ec55"));
        }

        List<Long> roomIds = request.selectedRoomIds() == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(request.selectedRoomIds()));
        if (roomIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.865927e11455"));
        }
        List<Room> rooms = roomIds.isEmpty() ? List.of() : roomRepository.findByStoreIdAndIdIn(storeId, roomIds);
        if (rooms.size() != roomIds.size()) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.a3269b668450"));
        }

        apply(settings, request);
        settings = settingsRepository.save(settings);

        managedRoomRepository.deleteByStoreIdAndSettingsId(storeId, settings.getId());
        managedRoomRepository.flush();
        for (Room room : rooms) {
            ManagedOperationRoom link = new ManagedOperationRoom();
            link.setStoreId(storeId);
            link.setSettings(settings);
            link.setRoom(room);
            managedRoomRepository.save(link);
        }
        return new ManagedOperationDtos.SettingsResponse(toDto(settings, roomIds), availableRooms(storeId), true);
    }

    @Transactional
    public ManagedOperationDtos.SettingsResponse updateIssueDay(
            Long storeId, Long settingsId, ManagedOperationDtos.IssueDayRequest request) {
        Integer invoiceDay = request == null ? null : request.invoiceIssueDay();
        Integer receiptDay = request == null ? null : request.receiptIssueDay();
        validateIssueDay(invoiceDay);
        validateIssueDay(receiptDay);
        ManagedOperationSettings settings = requirePersistedSettings(storeId, settingsId);
        settings.setInvoiceIssueDay(invoiceDay);
        settings.setReceiptIssueDay(receiptDay);
        settings = settingsRepository.save(settings);
        List<Long> selected = managedRoomRepository
                .findByStoreIdAndSettingsIdWithRoom(storeId, settings.getId())
                .stream().map(link -> link.getRoom().getId()).toList();
        return new ManagedOperationDtos.SettingsResponse(toDto(settings, selected), availableRooms(storeId), true);
    }

    @Transactional
    public void deleteProperty(Long storeId, Long settingsId) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId, settingsId);
        List<String> fileKeys = new ArrayList<>();
        if (settings.getStampStorageKey() != null && !settings.getStampStorageKey().isBlank()) {
            fileKeys.add(settings.getStampStorageKey());
        }
        List<ManagedOperationMonthlyData> monthlyRows =
                monthlyDataRepository.findByStoreIdAndSettingsId(storeId, settingsId);
        for (ManagedOperationMonthlyData monthly : monthlyRows) {
            if (monthly.getAirbnbFileKey() != null && !monthly.getAirbnbFileKey().isBlank()) {
                fileKeys.add(monthly.getAirbnbFileKey());
            }
            if (monthly.getBookingFileKey() != null && !monthly.getBookingFileKey().isBlank()) {
                fileKeys.add(monthly.getBookingFileKey());
            }
            monthlyFeeRepository.deleteByStoreIdAndMonthlyDataId(storeId, monthly.getId());
        }
        // 显式删除子表，兼容外键缺少 ON DELETE CASCADE 的环境
        monthlyDataRepository.deleteByStoreIdAndSettingsId(storeId, settingsId);
        managedRoomRepository.deleteByStoreIdAndSettingsId(storeId, settingsId);
        settingsRepository.delete(settings);
        registerFileCleanupAfterCommit(storeId, fileKeys);
    }

    @Transactional
    public ManagedOperationDtos.StampResponse uploadStamp(Long storeId, Long settingsId, MultipartFile file) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId, settingsId);
        validateSnapshotSettings(settings);
        String oldKey = settings.getStampStorageKey();
        String newKey = stampStorage.store(storeId, file);
        if (newKey == null || !newKey.startsWith(storeId + "/")) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.03597afc7bf4"));
        }
        try {
            registerStampSwapSynchronization(storeId, oldKey, newKey);
            settings.setStampStorageKey(newKey);
            settingsRepository.save(settings);
            return new ManagedOperationDtos.StampResponse(true);
        } catch (RuntimeException | Error ex) {
            if (!Objects.equals(oldKey, newKey)) {
                stampStorage.deleteQuietly(storeId, newKey);
            }
            throw ex;
        }
    }

    private void registerStampSwapSynchronization(Long storeId, String oldKey, String newKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!Objects.equals(oldKey, newKey)) {
                    stampStorage.deleteQuietly(storeId, oldKey);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED
                        && !Objects.equals(oldKey, newKey)) {
                    stampStorage.deleteQuietly(storeId, newKey);
                }
            }
        });
    }

    private void registerFileCleanupAfterCommit(Long storeId, List<String> keys) {
        if (keys.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        List<String> immutableKeys = List.copyOf(keys);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String key : immutableKeys) {
                    stampStorage.deleteQuietly(storeId, key);
                    sheetStorage.deleteQuietly(storeId, key);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public ManagedOperationPrivateStampStorage.StoredStamp loadStamp(Long storeId, Long settingsId) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId, settingsId);
        return stampStorage.load(storeId, settings.getStampStorageKey());
    }

    @Transactional(readOnly = true)
    public SettingsSnapshot requireSnapshot(Long storeId, Long settingsId) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId, settingsId);
        validateSnapshotSettings(settings);
        List<Room> rooms = managedRoomRepository.findByStoreIdAndSettingsIdWithRoom(storeId, settings.getId())
                .stream().map(ManagedOperationRoom::getRoom).toList();
        if (rooms.isEmpty()) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.1c11f3ccce67"));
        }
        return new SettingsSnapshot(settings, rooms);
    }

    static void validateSnapshotSettings(ManagedOperationSettings settings) {
        requireText(settings.getPropertyName(), ApiMessages.get("api.t.0d86438422da"));
        requireText(settings.getOwnerCompanyName(), ApiMessages.get("api.t.1e81fa8e9c37"));
        requireText(settings.getIssuerCompanyName(), ApiMessages.get("api.t.ad9f84bcbe1a"));
        validateRate(settings.getManagementFeeRate(), ApiMessages.get("api.t.b457f2525d4d"));
        validateRate(settings.getTaxRate(), ApiMessages.get("api.t.49afb4e7bdf8"));
        validateMoney(settings.getCleaningFeeGross(), ApiMessages.get("api.t.ca2708ecfc3b"));
        validateMoney(settings.getRegistrationFeeNet(), ApiMessages.get("api.t.1fda0430b730"));
    }

    private ManagedOperationSettings requirePersistedSettings(Long storeId, Long settingsId) {
        if (settingsId == null || settingsId <= 0) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.c6b7d11d8389"));
        }
        return settingsRepository.findByStoreIdAndId(storeId, settingsId)
                .orElseThrow(() -> new ManagedOperationValidationException(ApiMessages.get("api.t.c6b7d11d8389")));
    }

    private List<ManagedOperationDtos.RoomOption> availableRooms(Long storeId) {
        return roomRepository.findByStoreIdWithRoomType(storeId).stream()
                .map(room -> new ManagedOperationDtos.RoomOption(
                        room.getId(), room.getRoomNumber(), room.getRoomTypeName()))
                .toList();
    }

    private static ManagedOperationDtos.Settings toDto(ManagedOperationSettings s, List<Long> selected) {
        return new ManagedOperationDtos.Settings(
                s.getId(), s.getPropertyName(), selected, s.getManagementFeeRate(), s.getTaxRate(),
                s.getCleaningFeeGross(), s.getRegistrationFeeNet(), s.getInvoiceIssueDay(), s.getReceiptIssueDay(),
                s.getOwnerCompanyName(), s.getOwnerContactName(), s.getOwnerPostalCode(), s.getOwnerAddress(),
                s.getIssuerCompanyName(), s.getIssuerPostalCode(), s.getIssuerAddress(),
                s.getIssuerRegistrationNumber(), s.getIssuerPhone(), s.getIssuerEmail(),
                s.getBankName(), s.getBankBranch(), s.getBankAccountType(), s.getBankAccountNumber(),
                s.getBankAccountHolder(), s.getStampStorageKey() != null && !s.getStampStorageKey().isBlank());
    }

    private static ManagedOperationSettings defaultSettings(Long storeId) {
        ManagedOperationSettings settings = new ManagedOperationSettings();
        settings.setStoreId(storeId);
        settings.setInvoiceIssueDay(DEFAULT_INVOICE_ISSUE_DAY);
        settings.setReceiptIssueDay(DEFAULT_RECEIPT_ISSUE_DAY);
        return settings;
    }

    private static void apply(ManagedOperationSettings s, ManagedOperationDtos.SettingsRequest r) {
        s.setPropertyName(text(r.propertyName(), 200));
        s.setManagementFeeRate(r.managementFeeRate());
        s.setTaxRate(r.taxRate());
        s.setCleaningFeeGross(r.cleaningFeeGross());
        s.setRegistrationFeeNet(r.registrationFeeNet());
        s.setInvoiceIssueDay(r.invoiceIssueDay());
        s.setReceiptIssueDay(r.receiptIssueDay());
        s.setOwnerCompanyName(text(r.ownerCompanyName(), 200));
        s.setOwnerContactName(text(r.ownerContactName(), 100));
        s.setOwnerPostalCode(text(r.ownerPostalCode(), 30));
        s.setOwnerAddress(text(r.ownerAddress(), 500));
        s.setIssuerCompanyName(text(r.issuerCompanyName(), 200));
        s.setIssuerPostalCode(text(r.issuerPostalCode(), 30));
        s.setIssuerAddress(text(r.issuerAddress(), 500));
        s.setIssuerRegistrationNumber(text(r.issuerRegistrationNumber(), 100));
        s.setIssuerPhone(text(r.issuerPhone(), 50));
        s.setIssuerEmail(text(r.issuerEmail(), 200));
        s.setBankName(text(r.bankName(), 200));
        s.setBankBranch(text(r.bankBranch(), 200));
        s.setBankAccountType(text(r.bankAccountType(), 50));
        s.setBankAccountNumber(text(r.bankAccountNumber(), 100));
        s.setBankAccountHolder(text(r.bankAccountHolder(), 200));
    }

    private static String text(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.24c862ea003b"));
        }
        return normalized;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ManagedOperationValidationException(field + ApiMessages.get("api.t.44fa2a2c698d"));
        }
    }

    private static void validateRate(BigDecimal rate, String field) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new ManagedOperationValidationException(field + ApiMessages.get("api.t.b1d5d5a3a6dc"));
        }
    }

    private static void validateMoney(BigDecimal amount, String field) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(new BigDecimal("1000000000")) > 0) {
            throw new ManagedOperationValidationException(field + ApiMessages.get("api.t.b8be414d90f8"));
        }
        ManagedOperationMoneyRules.requireWholeYen(amount, field);
    }

    static void validateIssueDay(Integer day) {
        if (day == null || day < MIN_ISSUE_DAY || day > MAX_ISSUE_DAY) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.a5cb079905bc"));
        }
    }

    public record SettingsSnapshot(ManagedOperationSettings settings, List<Room> rooms) {}
}
