package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.enums.RoomStatus;
import server.demo.dto.RoomTypeWithRoomsDTO;
import server.demo.util.StoreContextUtils;
import server.demo.util.SuHotelIdUtil;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomTypeService {

    private static final Logger logger = LoggerFactory.getLogger(RoomTypeService.class);

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
    private SuContentSyncService suContentSyncService;

    @Autowired
    private SuAriAutoSyncService suAriAutoSyncService;

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
            String msg = "Su hotelId 未配置，无法同步房型到 SU（stores.su_hotel_id）。";
            if (suRoomTypeAutoSyncStrict) {
                throw new RuntimeException(msg);
            }
            logger.warn("[SuRoomTypeUpsert] skip: {} storeId={}", msg, storeId);
            return;
        }

        try {
            // SU Content 的 Quantity 需要按“房型的物理房间数”配置。
            // 这里以 PMS 内该房型下实际房间记录数为准（只增不减，避免触发 SU 不允许减少数量的问题）。
            if (roomType != null && roomType.getId() != null) {
                int actualRooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomType.getId()).size();
                int currentTotal = roomType.getTotalRooms() != null ? roomType.getTotalRooms() : 0;
                if (actualRooms > currentTotal) {
                    roomType.setTotalRooms(actualRooms);
                    roomType = roomTypeRepository.save(roomType);
                }
            }

            suContentSyncService.upsertSingleRoomTypeStrict(storeId, hotelId, roomType);

            // 为了让 SU 沙盒 UI（Price & Availability）能立刻看到新 roomid，创建/编辑房型后预热库存（roomstosell）。
            if (suRoomTypeAutoSyncStrict && roomType != null && roomType.getId() != null) {
                java.time.LocalDate start = java.time.LocalDate.now();
                int d = Math.max(1, suAriWarmupDays);
                java.time.LocalDate end = start.plusDays(d - 1L);
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
            throw new RuntimeException("房型创建失败：同一门店下已存在同名房型，请更换名称后重试");
        }

        String uniqueCode = ensureUniqueRoomTypeCode(storeId, roomType.getCode());
        roomType.setCode(uniqueCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        roomType.setStoreId(storeId);
        roomType.setUser(user);
        RoomType saved = roomTypeRepository.save(roomType);
        syncRoomTypeToSuIfEnabled(saved);
        return saved;
    }

    public RoomType createRoomTypeWithRooms(RoomType roomType, List<String> roomNumbers) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        if (roomTypeRepository.existsByStoreIdAndName(storeId, roomType.getName())) {
            throw new RuntimeException("房型创建失败：同一门店下已存在同名房型，请更换名称后重试");
        }

        String uniqueCode = ensureUniqueRoomTypeCode(storeId, roomType.getCode());
        roomType.setCode(uniqueCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (roomNumbers != null && !roomNumbers.isEmpty()) {
            for (String roomNumber : roomNumbers) {
                if (roomRepository.existsByStoreIdAndRoomNumber(storeId, roomNumber)) {
                    throw new RuntimeException("房间号 " + roomNumber + " 已存在");
                }
            }
        }

        roomType.setStoreId(storeId);
        roomType.setUser(user);
        RoomType savedRoomType = roomTypeRepository.save(roomType);

        if (roomNumbers != null && !roomNumbers.isEmpty()) {
            for (String roomNumber : roomNumbers) {
                Room room = new Room(roomNumber, savedRoomType, null);
                room.setStatus(RoomStatus.AVAILABLE);
                room.setUserId(userId);
                room.setStoreId(storeId);
                roomRepository.save(room);
            }
        }

        syncRoomTypeToSuIfEnabled(savedRoomType);
        return savedRoomType;
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
    public RoomType updateRoomType(Long id, RoomType roomType) {
        Long storeId = currentStoreId();
        RoomType existingRoomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("房型不存在"));

        if (!storeId.equals(existingRoomType.getStoreId())) {
            throw new RuntimeException("无权限修改此房型");
        }

        if (!existingRoomType.getCode().equals(roomType.getCode()) &&
            roomTypeRepository.existsByStoreIdAndCode(storeId, roomType.getCode())) {
            throw new RuntimeException("房型代码已存在");
        }

        if (!java.util.Objects.equals(existingRoomType.getName(), roomType.getName())
                && roomTypeRepository.existsByStoreIdAndName(storeId, roomType.getName())) {
            throw new RuntimeException("房型更新失败：同一门店下已存在同名房型，请更换名称后重试");
        }

        existingRoomType.setName(roomType.getName());
        existingRoomType.setCode(roomType.getCode());

        if (suRoomTypeAutoSyncEnabled && suRoomTypeAutoSyncStrict) {
            Integer oldTotal = existingRoomType.getTotalRooms();
            Integer newTotal = roomType.getTotalRooms();
            if (oldTotal != null && newTotal != null && newTotal < oldTotal) {
                throw new RuntimeException("SU 房型数量（Room Count/Quantity）不允许减少。若需减少请联系 SU Support。当前=" + oldTotal + "，请求=" + newTotal);
            }
        }

        existingRoomType.setTotalRooms(roomType.getTotalRooms());
        existingRoomType.setMaxGuests(roomType.getMaxGuests());
        existingRoomType.setDescription(roomType.getDescription());
        existingRoomType.setCheckInGuideLink(roomType.getCheckInGuideLink());
        existingRoomType.setDefaultPrice(roomType.getDefaultPrice());
        existingRoomType.setWeekdayPrice(roomType.getWeekdayPrice());
        existingRoomType.setWeekendPrice(roomType.getWeekendPrice());
        existingRoomType.setMondayPrice(roomType.getMondayPrice());
        existingRoomType.setTuesdayPrice(roomType.getTuesdayPrice());
        existingRoomType.setWednesdayPrice(roomType.getWednesdayPrice());
        existingRoomType.setThursdayPrice(roomType.getThursdayPrice());
        existingRoomType.setFridayPrice(roomType.getFridayPrice());
        existingRoomType.setSaturdayPrice(roomType.getSaturdayPrice());
        existingRoomType.setSundayPrice(roomType.getSundayPrice());

        return roomTypeRepository.save(existingRoomType);
    }

    public RoomType updateRoomTypeWithRooms(Long id, RoomType roomType, List<String> roomNumbers) {
        Long storeId = currentStoreId();
        RoomType savedRoomType = updateRoomType(id, roomType);

        if (roomNumbers != null) {
            List<Room> existingRooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, id);

            List<String> existingRoomNumbers = existingRooms.stream()
                .map(Room::getRoomNumber)
                .collect(Collectors.toList());

            List<Room> roomsToDelete = existingRooms.stream()
                .filter(room -> !roomNumbers.contains(room.getRoomNumber()))
                .collect(java.util.stream.Collectors.toList());

            roomRepository.deleteAll(roomsToDelete);

            List<String> roomNumbersToAdd = roomNumbers.stream()
                .filter(roomNumber -> !existingRoomNumbers.contains(roomNumber))
                .collect(Collectors.toList());

            for (String roomNumber : roomNumbersToAdd) {
                if (roomRepository.existsByStoreIdAndRoomNumber(storeId, roomNumber)) {
                    throw new RuntimeException("房间号 " + roomNumber + " 已存在");
                }
            }

            Long userId = currentUserId();
            for (String roomNumber : roomNumbersToAdd) {
                Room room = new Room(roomNumber, savedRoomType, null);
                room.setStatus(RoomStatus.AVAILABLE);
                room.setUserId(userId);
                room.setStoreId(storeId);
                roomRepository.save(room);
            }
        }

        syncRoomTypeToSuIfEnabled(savedRoomType);
        return savedRoomType;
    }

    public void deleteRoomType(Long id) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        RoomType roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("房型不存在"));

        if (!storeId.equals(roomType.getStoreId())) {
            throw new RuntimeException("无权限删除此房型");
        }

        List<Room> rooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, id);

        for (Room room : rooms) {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate futureDate = today.plusYears(10);

            List<Reservation> activeReservations = reservationRepository.findByStoreIdAndRoomIdAndDateRange(
                currentStoreId(),
                room.getId(),
                today,
                futureDate
            );

            if (!activeReservations.isEmpty()) {
                throw new RuntimeException("该房型下的房间还有活跃预订记录，无法删除。请先处理或取消相关订单后再删除。");
            }
        }

        priceChangeHistoryRepository.deleteByRoomTypeId(id);
        roomPriceRepository.deleteByRoomTypeId(id);
        roomTypePricePlanRepository.deleteByRoomTypeId(id);
        roomRepository.deleteAll(rooms);
        roomTypeRepository.deleteById(id);
    }

}
