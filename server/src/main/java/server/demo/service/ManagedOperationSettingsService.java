package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationRoom;
import server.demo.entity.ManagedOperationSettings;
import server.demo.entity.Room;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.repository.ManagedOperationRoomRepository;
import server.demo.repository.ManagedOperationSettingsRepository;
import server.demo.repository.RoomRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ManagedOperationSettingsService {
    private final ManagedOperationSettingsRepository settingsRepository;
    private final ManagedOperationRoomRepository managedRoomRepository;
    private final RoomRepository roomRepository;
    private final ManagedOperationPrivateStampStorage stampStorage;

    public ManagedOperationSettingsService(
            ManagedOperationSettingsRepository settingsRepository,
            ManagedOperationRoomRepository managedRoomRepository,
            RoomRepository roomRepository,
            ManagedOperationPrivateStampStorage stampStorage) {
        this.settingsRepository = settingsRepository;
        this.managedRoomRepository = managedRoomRepository;
        this.roomRepository = roomRepository;
        this.stampStorage = stampStorage;
    }

    @Transactional(readOnly = true)
    public ManagedOperationDtos.SettingsResponse getSettings(Long storeId) {
        Optional<ManagedOperationSettings> persistedSettings = settingsRepository.findByStoreId(storeId);
        ManagedOperationSettings settings = persistedSettings.orElseGet(() -> defaultSettings(storeId));
        List<Long> selected = settings.getId() == null ? List.of()
                : managedRoomRepository.findByStoreIdAndSettingsIdWithRoom(storeId, settings.getId())
                .stream().map(link -> link.getRoom().getId()).toList();
        return new ManagedOperationDtos.SettingsResponse(
                toDto(settings, selected), availableRooms(storeId), persistedSettings.isPresent());
    }

    @Transactional
    public ManagedOperationDtos.SettingsResponse saveSettings(Long storeId, ManagedOperationDtos.SettingsRequest request) {
        if (request == null) {
            throw new ManagedOperationValidationException("配置不能为空");
        }
        requireText(request.propertyName(), "物业/方案名称");
        requireText(request.ownerCompanyName(), "房东公司");
        requireText(request.issuerCompanyName(), "开票方公司");
        validateRate(request.managementFeeRate(), "管理费率");
        validateRate(request.taxRate(), "消费税率");
        validateMoney(request.cleaningFeeGross(), "含税清洁费");
        validateMoney(request.registrationFeeNet(), "名簿制作费");

        List<Long> roomIds = request.selectedRoomIds() == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(request.selectedRoomIds()));
        if (roomIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ManagedOperationValidationException("房间选择包含非法值");
        }
        List<Room> rooms = roomIds.isEmpty() ? List.of() : roomRepository.findByStoreIdAndIdIn(storeId, roomIds);
        if (rooms.size() != roomIds.size()) {
            throw new ManagedOperationValidationException("部分房间不存在或不属于当前门店");
        }

        ManagedOperationSettings settings = settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> defaultSettings(storeId));
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
    public ManagedOperationDtos.StampResponse uploadStamp(Long storeId, MultipartFile file) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId);
        validateSnapshotSettings(settings);
        String oldKey = settings.getStampStorageKey();
        String newKey = stampStorage.store(storeId, file);
        if (newKey == null || !newKey.startsWith(storeId + "/")) {
            throw new ManagedOperationValidationException("印章存储路径与当前门店不一致");
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

    @Transactional(readOnly = true)
    public ManagedOperationPrivateStampStorage.StoredStamp loadStamp(Long storeId) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId);
        return stampStorage.load(storeId, settings.getStampStorageKey());
    }

    @Transactional(readOnly = true)
    public SettingsSnapshot requireSnapshot(Long storeId) {
        ManagedOperationSettings settings = requirePersistedSettings(storeId);
        validateSnapshotSettings(settings);
        List<Room> rooms = managedRoomRepository.findByStoreIdAndSettingsIdWithRoom(storeId, settings.getId())
                .stream().map(ManagedOperationRoom::getRoom).toList();
        if (rooms.isEmpty()) {
            throw new ManagedOperationValidationException("请先选择至少一个代运营房间");
        }
        return new SettingsSnapshot(settings, rooms);
    }

    static void validateSnapshotSettings(ManagedOperationSettings settings) {
        requireText(settings.getPropertyName(), "物业/方案名称");
        requireText(settings.getOwnerCompanyName(), "房东公司");
        requireText(settings.getIssuerCompanyName(), "开票方公司");
        validateRate(settings.getManagementFeeRate(), "管理费率");
        validateRate(settings.getTaxRate(), "消费税率");
        validateMoney(settings.getCleaningFeeGross(), "含税清洁费");
        validateMoney(settings.getRegistrationFeeNet(), "名簿制作费");
    }

    private ManagedOperationSettings requirePersistedSettings(Long storeId) {
        return settingsRepository.findByStoreId(storeId)
                .orElseThrow(() -> new ManagedOperationValidationException("请先保存代运营结算配置"));
    }

    private List<ManagedOperationDtos.RoomOption> availableRooms(Long storeId) {
        return roomRepository.findByStoreIdWithRoomType(storeId).stream()
                .map(room -> new ManagedOperationDtos.RoomOption(
                        room.getId(), room.getRoomNumber(), room.getRoomTypeName()))
                .toList();
    }

    private static ManagedOperationDtos.Settings toDto(ManagedOperationSettings s, List<Long> selected) {
        return new ManagedOperationDtos.Settings(
                s.getPropertyName(), selected, s.getManagementFeeRate(), s.getTaxRate(),
                s.getCleaningFeeGross(), s.getRegistrationFeeNet(), s.getOwnerCompanyName(),
                s.getOwnerContactName(), s.getOwnerPostalCode(), s.getOwnerAddress(),
                s.getIssuerCompanyName(), s.getIssuerPostalCode(), s.getIssuerAddress(),
                s.getIssuerRegistrationNumber(), s.getIssuerPhone(), s.getIssuerEmail(),
                s.getBankName(), s.getBankBranch(), s.getBankAccountType(), s.getBankAccountNumber(),
                s.getBankAccountHolder(), s.getStampStorageKey() != null && !s.getStampStorageKey().isBlank());
    }

    private static ManagedOperationSettings defaultSettings(Long storeId) {
        ManagedOperationSettings settings = new ManagedOperationSettings();
        settings.setStoreId(storeId);
        return settings;
    }

    private static void apply(ManagedOperationSettings s, ManagedOperationDtos.SettingsRequest r) {
        s.setPropertyName(text(r.propertyName(), 200));
        s.setManagementFeeRate(r.managementFeeRate());
        s.setTaxRate(r.taxRate());
        s.setCleaningFeeGross(r.cleaningFeeGross());
        s.setRegistrationFeeNet(r.registrationFeeNet());
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
            throw new ManagedOperationValidationException("配置文字长度超过限制");
        }
        return normalized;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ManagedOperationValidationException(field + "不能为空");
        }
    }

    private static void validateRate(BigDecimal rate, String field) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new ManagedOperationValidationException(field + "必须在 0 到 1 之间");
        }
    }

    private static void validateMoney(BigDecimal amount, String field) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(new BigDecimal("1000000000")) > 0) {
            throw new ManagedOperationValidationException(field + "金额不合法");
        }
        ManagedOperationMoneyRules.requireWholeYen(amount, field);
    }

    public record SettingsSnapshot(ManagedOperationSettings settings, List<Room> rooms) {}
}
