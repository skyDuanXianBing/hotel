package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.RoomTypeDeleteBlockInfo;
import server.demo.dto.RoomTypeWithRoomsDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.exception.RoomTypeDeleteBlockedException;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.util.StoreContextUtils;
import server.demo.util.SuHotelIdUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomTypeService {

    private static final Logger logger = LoggerFactory.getLogger(RoomTypeService.class);

    public record RoomInput(String roomNumber, String smartlockPasscode) {}

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomTypePricePlanRepository roomTypePricePlanRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ChannelPriceRepository channelPriceRepository;

    @Autowired
    private PriceLabsConnectionRepository priceLabsConnectionRepository;

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private RoomBlockoutRepository roomBlockoutRepository;

    @Autowired
    private SuContentSyncService suContentSyncService;

    @Autowired
    private SuAriAutoSyncService suAriAutoSyncService;

    @Autowired
    private SuImageSyncService suImageSyncService;

    @Value("${su.content.autosync.roomtype.enabled:true}")
    private boolean suRoomTypeAutoSyncEnabled;

    @Value("${su.content.autosync.roomtype.strict:true}")
    private boolean suRoomTypeAutoSyncStrict;

    @Value("${su.ari.autosync.days:365}")
    private int suAriWarmupDays;

    @Autowired
    private server.demo.repository.RoomPriceRepository roomPriceRepository;

    @Autowired
    private server.demo.repository.PriceChangeHistoryRepository priceChangeHistoryRepository;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
    }

    private String resolveOrInitSuHotelId(Long storeId) {
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return null;
        }
        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId != null) {
            return hotelId;
        }
        String generated = SuHotelIdUtil.buildDefault(storeId);
        store.setSuHotelId(generated);
        storeRepository.save(store);
        return generated;
    }

    private void syncRoomTypeToSuIfEnabled(RoomType roomType) {
        if (!suRoomTypeAutoSyncEnabled) {
            return;
        }
        Long storeId = currentStoreId();
        String hotelId = resolveOrInitSuHotelId(storeId);
        if (hotelId == null || hotelId.isBlank()) {
            String msg = "Su hotelId is missing; cannot sync room type to Su (stores.su_hotel_id)";
            if (suRoomTypeAutoSyncStrict) {
                throw new RuntimeException(msg);
            }
            logger.warn("[SuRoomTypeUpsert] skip: {} storeId={}", msg, storeId);
            return;
        }

        try {
            if (roomType != null && roomType.getId() != null) {
                int actualRooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomType.getId()).size();
                int currentTotal = roomType.getTotalRooms() != null ? roomType.getTotalRooms() : 0;
                if (actualRooms > currentTotal) {
                    roomType.setTotalRooms(actualRooms);
                    roomType = roomTypeRepository.save(roomType);
                }
            }

            suContentSyncService.upsertSingleRoomTypeStrict(storeId, hotelId, roomType);
            suImageSyncService.syncRoomTypeImagesStrict(storeId, roomType);

            if (suRoomTypeAutoSyncStrict && roomType != null && roomType.getId() != null) {
                LocalDate start = LocalDate.now();
                int days = Math.max(1, suAriWarmupDays);
                LocalDate end = start.plusDays(days - 1L);
                suAriAutoSyncService.enqueueForStoreScope(
                        storeId,
                        "room-type-upsert",
                        start,
                        end,
                        java.util.Set.of(roomType.getId()),
                        null,
                        true,
                        false,
                        false,
                        false
                );
            }
        } catch (RuntimeException e) {
            if (suRoomTypeAutoSyncStrict) {
                throw e;
            }
            logger.warn("[SuRoomTypeUpsert] best-effort failed. storeId={}, hotelId={}, roomTypeId={}, err={}",
                    storeId,
                    hotelId,
                    roomType != null ? roomType.getId() : null,
                    e.getMessage());
        }
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findByStoreIdOrderByName(currentStoreId());
    }

    public List<RoomTypeWithRoomsDTO> getAllRoomTypesWithRooms() {
        Long storeId = currentStoreId();
        List<RoomType> roomTypes = roomTypeRepository.findByStoreIdOrderByName(storeId);
        return roomTypes.stream()
                .map(roomType -> {
                    List<Room> rooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomType.getId());
                    List<RoomTypeWithRoomsDTO.RoomInfoDTO> roomInfos = rooms.stream()
                            .map(RoomTypeWithRoomsDTO.RoomInfoDTO::new)
                            .collect(Collectors.toList());
                    return new RoomTypeWithRoomsDTO(roomType, roomInfos);
                })
                .collect(Collectors.toList());
    }

    public Optional<RoomType> getRoomTypeById(Long id) {
        Long storeId = currentStoreId();
        return roomTypeRepository.findById(id)
                .filter(roomType -> storeId.equals(roomType.getStoreId()));
    }

    public Optional<RoomType> getRoomTypeByCode(String code) {
        return roomTypeRepository.findByStoreIdAndCode(currentStoreId(), code);
    }

    public RoomType createRoomType(RoomType roomType) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        if (roomTypeRepository.existsByStoreIdAndName(storeId, roomType.getName())) {
            throw new RuntimeException("Duplicate room type name exists in current store");
        }

        String uniqueCode = ensureUniqueRoomTypeCode(storeId, roomType.getCode());
        roomType.setCode(uniqueCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        roomType.setStoreId(storeId);
        roomType.setUser(user);
        applyOptionalRoomTypeFields(roomType, roomType);
        RoomType saved = roomTypeRepository.save(roomType);
        syncRoomTypeToSuIfEnabled(saved);
        return saved;
    }

    public RoomType createRoomTypeWithRooms(RoomType roomType, List<String> roomNumbers) {
        return createRoomTypeWithRoomInputs(roomType, toRoomInputs(roomNumbers));
    }

    public RoomType createRoomTypeWithRoomInputs(RoomType roomType, List<RoomInput> rooms) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        if (roomTypeRepository.existsByStoreIdAndName(storeId, roomType.getName())) {
            throw new RuntimeException("Duplicate room type name exists in current store");
        }

        String uniqueCode = ensureUniqueRoomTypeCode(storeId, roomType.getCode());
        roomType.setCode(uniqueCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, String> passcodeByRoomNumber = normalizeRoomInputs(rooms);
        for (String roomNumber : passcodeByRoomNumber.keySet()) {
            if (roomRepository.existsByStoreIdAndRoomNumber(storeId, roomNumber)) {
                throw new RuntimeException("Room number " + roomNumber + " already exists");
            }
        }

        roomType.setStoreId(storeId);
        roomType.setUser(user);
        applyOptionalRoomTypeFields(roomType, roomType);
        RoomType savedRoomType = roomTypeRepository.save(roomType);

        for (Map.Entry<String, String> entry : passcodeByRoomNumber.entrySet()) {
            Room room = new Room(entry.getKey(), savedRoomType, null);
            room.setSmartlockPasscode(entry.getValue());
            room.setStatus(RoomStatus.AVAILABLE);
            room.setUserId(userId);
            room.setStoreId(storeId);
            roomRepository.save(room);
        }

        syncRoomTypeToSuIfEnabled(savedRoomType);
        return savedRoomType;
    }

    private static List<RoomInput> toRoomInputs(List<String> roomNumbers) {
        if (roomNumbers == null || roomNumbers.isEmpty()) {
            return List.of();
        }
        return roomNumbers.stream()
                .map(roomNumber -> new RoomInput(roomNumber, null))
                .toList();
    }

    private static String normalizeOptionalPasscode(String rawPasscode) {
        if (rawPasscode == null) {
            return null;
        }
        String trimmed = rawPasscode.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, String> normalizeRoomInputs(List<RoomInput> rooms) {
        Map<String, String> passcodeByRoomNumber = new java.util.LinkedHashMap<>();
        if (rooms == null) {
            return passcodeByRoomNumber;
        }

        for (RoomInput input : rooms) {
            if (input == null) {
                continue;
            }
            String roomNumber = input.roomNumber() != null ? input.roomNumber().trim() : "";
            if (roomNumber.isEmpty()) {
                continue;
            }
            if (passcodeByRoomNumber.containsKey(roomNumber)) {
                throw new RuntimeException("Duplicate room number " + roomNumber);
            }
            passcodeByRoomNumber.put(roomNumber, normalizeOptionalPasscode(input.smartlockPasscode()));
        }

        return passcodeByRoomNumber;
    }

    private String ensureUniqueRoomTypeCode(Long storeId, String rawCode) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new RuntimeException("room type code is required");
        }

        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        final int maxLen = 20;
        if (normalized.length() > maxLen) {
            normalized = normalized.substring(0, maxLen);
        }

        if (!roomTypeRepository.existsByStoreIdAndCode(storeId, normalized)) {
            return normalized;
        }

        String base = normalized;
        for (int i = 1; i <= 999; i++) {
            String suffix = String.valueOf(i);
            int maxBaseLen = maxLen - suffix.length();
            String trimmedBase = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            String candidate = trimmedBase + suffix;
            if (!roomTypeRepository.existsByStoreIdAndCode(storeId, candidate)) {
                return candidate;
            }
        }

        throw new RuntimeException("room type code already exists");
    }

    private static String normalizeRoomTypeCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new RuntimeException("room type code is required");
        }
        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        final int maxLen = 20;
        if (normalized.length() > maxLen) {
            normalized = normalized.substring(0, maxLen);
        }
        return normalized;
    }

    private static String normalizeRoomTypeName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new RuntimeException("room type name is required");
        }
        return rawName.trim();
    }

    private static String normalizeOptionalSuRoomType(String rawSuRoomType) {
        if (rawSuRoomType == null) {
            return null;
        }
        String normalized = rawSuRoomType.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeOptionalSizeMeasurementUnit(String rawUnit) {
        if (rawUnit == null) {
            return null;
        }
        String normalized = rawUnit.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (!"sqm".equals(normalized) && !"sqft".equals(normalized)) {
            throw new RuntimeException("size measurement unit must be sqm or sqft");
        }
        return normalized;
    }

    private static void validateRoomTypeCapacity(Integer maxGuests, Integer maxChildOccupancy) {
        if (maxGuests == null || maxGuests < 1) {
            throw new RuntimeException("max guests must be greater than 0");
        }
        if (maxChildOccupancy == null || maxChildOccupancy < 0) {
            throw new RuntimeException("max child occupancy must be greater than or equal to 0");
        }
        if (maxChildOccupancy > maxGuests) {
            throw new RuntimeException("max child occupancy cannot be greater than max guests");
        }
    }

    private static void applyOptionalRoomTypeFields(RoomType target, RoomType source) {
        target.setDescription(source.getDescription());
        target.setCheckInGuideLink(source.getCheckInGuideLink());
        target.setRoomTypeAddress(source.getRoomTypeAddress());
        target.setNearbyStation(source.getNearbyStation());
        target.setDefaultPrice(source.getDefaultPrice());
        target.setWeekdayPrice(source.getWeekdayPrice());
        target.setWeekendPrice(source.getWeekendPrice());
        target.setMondayPrice(source.getMondayPrice());
        target.setTuesdayPrice(source.getTuesdayPrice());
        target.setWednesdayPrice(source.getWednesdayPrice());
        target.setThursdayPrice(source.getThursdayPrice());
        target.setFridayPrice(source.getFridayPrice());
        target.setSaturdayPrice(source.getSaturdayPrice());
        target.setSundayPrice(source.getSundayPrice());

        target.setMaxChildOccupancy(source.getMaxChildOccupancy() == null ? 0 : source.getMaxChildOccupancy());
        target.setSuRoomType(normalizeOptionalSuRoomType(source.getSuRoomType()));

        BigDecimal sizeMeasurement = source.getSizeMeasurement();
        target.setSizeMeasurement(sizeMeasurement);
        target.setSizeMeasurementUnit(
                sizeMeasurement == null ? null : normalizeOptionalSizeMeasurementUnit(source.getSizeMeasurementUnit())
        );

        validateRoomTypeCapacity(target.getMaxGuests(), target.getMaxChildOccupancy());

        if (source.getFacilities() != null) {
            target.setFacilities(source.getFacilities());
        }
        if (source.getDesktopPhotoUrls() != null) {
            target.setDesktopPhotoUrls(source.getDesktopPhotoUrls());
        }
        if (source.getMobilePhotoUrls() != null) {
            target.setMobilePhotoUrls(source.getMobilePhotoUrls());
        }
        if (source.getLocalizedContent() != null) {
            target.setLocalizedContent(source.getLocalizedContent());
        }
    }

    public RoomType updateRoomType(Long id, RoomType roomType) {
        Long storeId = currentStoreId();
        RoomType existingRoomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room type not found"));

        if (!storeId.equals(existingRoomType.getStoreId())) {
            throw new RuntimeException("Permission denied for current store");
        }

        String normalizedExistingCode = normalizeRoomTypeCode(existingRoomType.getCode());
        String normalizedCode = normalizeRoomTypeCode(roomType.getCode());
        if (!normalizedExistingCode.equals(normalizedCode)
                && roomTypeRepository.existsByStoreIdAndCodeAndIdNot(storeId, normalizedCode, id)) {
            throw new RuntimeException("Room type code already exists");
        }

        String normalizedExistingName = normalizeRoomTypeName(existingRoomType.getName());
        String normalizedName = normalizeRoomTypeName(roomType.getName());
        if (!normalizedExistingName.equalsIgnoreCase(normalizedName)
                && roomTypeRepository.existsByStoreIdAndNameAndIdNot(storeId, normalizedName, id)) {
            throw new RuntimeException("Duplicate room type name exists in current store");
        }

        existingRoomType.setName(normalizedName);
        existingRoomType.setCode(normalizedCode);

        if (suRoomTypeAutoSyncEnabled && suRoomTypeAutoSyncStrict) {
            Integer oldTotal = existingRoomType.getTotalRooms();
            Integer newTotal = roomType.getTotalRooms();
            if (oldTotal != null && newTotal != null && newTotal < oldTotal) {
                throw new RuntimeException(
                        "SU strict mode does not allow reducing total rooms. old="
                                + oldTotal
                                + ", new="
                                + newTotal
                );
            }
        }

        existingRoomType.setTotalRooms(roomType.getTotalRooms());
        existingRoomType.setMaxGuests(roomType.getMaxGuests());
        applyOptionalRoomTypeFields(existingRoomType, roomType);

        return roomTypeRepository.save(existingRoomType);
    }

    public RoomType updateRoomTypeWithRooms(Long id, RoomType roomType, List<String> roomNumbers) {
        return updateRoomTypeWithRoomsInternal(id, roomType, toRoomInputs(roomNumbers), false);
    }

    public RoomType updateRoomTypeWithRoomInputs(Long id, RoomType roomType, List<RoomInput> rooms) {
        return updateRoomTypeWithRoomsInternal(id, roomType, rooms, true);
    }

    private RoomType updateRoomTypeWithRoomsInternal(Long id, RoomType roomType, List<RoomInput> rooms, boolean applyPasscodes) {
        Long storeId = currentStoreId();
        RoomType savedRoomType = updateRoomType(id, roomType);

        Map<String, String> passcodeByRoomNumber = normalizeRoomInputs(rooms);
        if (!passcodeByRoomNumber.isEmpty()) {
            List<Room> existingRooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, id);

            List<String> existingRoomNumbers = existingRooms.stream()
                    .map(Room::getRoomNumber)
                    .collect(Collectors.toList());

            List<Room> roomsToDelete = existingRooms.stream()
                    .filter(room -> !passcodeByRoomNumber.containsKey(room.getRoomNumber()))
                    .collect(java.util.stream.Collectors.toList());

            List<String> roomNumbersToAdd = passcodeByRoomNumber.keySet().stream()
                    .filter(roomNumber -> !existingRoomNumbers.contains(roomNumber))
                    .collect(Collectors.toList());

            // Prefer in-place rename to keep room_id stable and avoid breaking FK references.
            int renameCount = Math.min(roomsToDelete.size(), roomNumbersToAdd.size());
            for (int i = 0; i < renameCount; i++) {
                Room roomToRename = roomsToDelete.get(i);
                String newRoomNumber = roomNumbersToAdd.get(i);
                roomToRename.setRoomNumber(newRoomNumber);
                if (applyPasscodes) {
                    roomToRename.setSmartlockPasscode(passcodeByRoomNumber.get(newRoomNumber));
                }
                roomRepository.save(roomToRename);
            }
            if (renameCount > 0) {
                roomsToDelete = new ArrayList<>(roomsToDelete.subList(renameCount, roomsToDelete.size()));
                roomNumbersToAdd = new ArrayList<>(roomNumbersToAdd.subList(renameCount, roomNumbersToAdd.size()));
            }

            for (String roomNumber : roomNumbersToAdd) {
                if (roomRepository.existsByStoreIdAndRoomNumber(storeId, roomNumber)) {
                    throw new RuntimeException("Room number " + roomNumber + " already exists");
                }
            }

            Long userId = currentUserId();
            for (String roomNumber : roomNumbersToAdd) {
                Room room = new Room(roomNumber, savedRoomType, null);
                if (applyPasscodes) {
                    room.setSmartlockPasscode(passcodeByRoomNumber.get(roomNumber));
                }
                room.setStatus(RoomStatus.AVAILABLE);
                room.setUserId(userId);
                room.setStoreId(storeId);
                roomRepository.save(room);
            }

            if (!roomsToDelete.isEmpty()) {
                Set<Long> roomIdsToDelete = roomsToDelete.stream()
                        .map(Room::getId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
                if (!roomIdsToDelete.isEmpty()) {
                    reservationRepository.clearRoomBindingByStoreIdAndRoomIds(storeId, new ArrayList<>(roomIdsToDelete));
                    cleaningTaskRepository.deleteByRoomIdIn(roomIdsToDelete);
                    roomBlockoutRepository.deleteByStoreIdAndRoom_IdIn(storeId, new ArrayList<>(roomIdsToDelete));
                }
                roomRepository.deleteAll(roomsToDelete);
            }

            if (applyPasscodes) {
                for (Room existingRoom : existingRooms) {
                    String roomNumber = existingRoom.getRoomNumber();
                    if (!passcodeByRoomNumber.containsKey(roomNumber)) {
                        continue;
                    }
                    String desired = passcodeByRoomNumber.get(roomNumber);
                    String current = existingRoom.getSmartlockPasscode();
                    if (desired == null && current == null) {
                        continue;
                    }
                    if (desired != null && desired.equals(current)) {
                        continue;
                    }
                    existingRoom.setSmartlockPasscode(desired);
                    roomRepository.save(existingRoom);
                }
            }
        }

        syncRoomTypeToSuIfEnabled(savedRoomType);
        return savedRoomType;
    }

    public void deleteRoomType(Long id) {
        Long storeId = currentStoreId();

        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room type not found"));

        if (!storeId.equals(roomType.getStoreId())) {
            throw new RuntimeException("Permission denied for current store");
        }

        List<Room> rooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, id);
        List<Long> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());

        final int sampleLimit = 10;
        List<RoomTypeDeleteBlockInfo.BlockingReservationSummary> samples = new ArrayList<>();
        LocalDate todayForScan = LocalDate.now();
        LocalDate futureDateForScan = todayForScan.plusYears(10);

        List<Reservation> blockingReservations = roomIds.isEmpty()
                ? List.of()
                : reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                        storeId,
                        roomIds,
                        todayForScan,
                        futureDateForScan,
                        Set.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN)
                );

        long blockingTotal = blockingReservations.size();
        if (blockingTotal > 0) {
            for (Reservation reservation : blockingReservations) {
                if (samples.size() >= sampleLimit) {
                    break;
                }
                Room reservationRoom = reservation.getRoom();
                samples.add(new RoomTypeDeleteBlockInfo.BlockingReservationSummary(
                        reservation.getOrderNumber(),
                        reservation.getStatus(),
                        reservationRoom != null ? reservationRoom.getRoomNumber() : null,
                        reservation.getCheckInDate(),
                        reservation.getCheckOutDate()
                ));
            }
        }
        if (blockingTotal > 0) {
            throw new RoomTypeDeleteBlockedException(
                    "该房型仍有未来占用订单，无法删除。请先对相关订单办理退房或取消后重试。",
                    new RoomTypeDeleteBlockInfo(blockingTotal, samples)
            );
        }

        if (!roomIds.isEmpty()) {
            reservationRepository.clearRoomBindingByStoreIdAndRoomIds(storeId, roomIds);
            cleaningTaskRepository.deleteByRoomIdIn(new HashSet<>(roomIds));
            roomBlockoutRepository.deleteByStoreIdAndRoom_IdIn(storeId, roomIds);
        }
        channelPriceRepository.deleteByStoreIdAndRoomTypeId(storeId, id);
        priceLabsConnectionRepository.deleteByStoreIdAndRoomTypeId(storeId, id);

        deleteRoomTypeFromSuIfEnabled(roomType);

        priceChangeHistoryRepository.deleteByRoomTypeId(id);
        roomPriceRepository.deleteByRoomTypeId(id);
        roomTypePricePlanRepository.deleteByRoomTypeId(id);
        roomRepository.deleteAll(rooms);
        roomTypeRepository.deleteById(id);
    }

    private void deleteRoomTypeFromSuIfEnabled(RoomType roomType) {
        if (!suRoomTypeAutoSyncEnabled) {
            return;
        }
        Long storeId = currentStoreId();
        String hotelId = resolveOrInitSuHotelId(storeId);
        if (hotelId == null || hotelId.isBlank()) {
            String msg = "Su hotelId is missing; cannot delete room type in Su (stores.su_hotel_id)";
            if (suRoomTypeAutoSyncStrict) {
                throw new RuntimeException(msg);
            }
            logger.warn("[SuRoomTypeDelete] skip: {} storeId={}", msg, storeId);
            return;
        }

        try {
            suContentSyncService.deleteSingleRoomTypeStrict(storeId, hotelId, String.valueOf(roomType.getId()));
        } catch (RuntimeException e) {
            if (suRoomTypeAutoSyncStrict) {
                throw e;
            }
            logger.warn("[SuRoomTypeDelete] best-effort failed. storeId={}, hotelId={}, roomTypeId={}, err={}",
                    storeId,
                    hotelId,
                    roomType != null ? roomType.getId() : null,
                    e.getMessage());
        }
    }
}
