package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Reservation;
import server.demo.entity.User;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.UserRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.enums.RoomStatus;
import server.demo.dto.RoomTypeWithRoomsDTO;
import server.demo.util.StoreContextUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomTypeService {

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
    private server.demo.repository.RoomPriceRepository roomPriceRepository;

    @Autowired
    private server.demo.repository.PriceChangeHistoryRepository priceChangeHistoryRepository;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
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

        if (roomTypeRepository.existsByStoreIdAndCode(storeId, roomType.getCode())) {
            throw new RuntimeException("房型代码已存在");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        roomType.setStoreId(storeId);
        roomType.setUser(user);
        return roomTypeRepository.save(roomType);
    }

    public RoomType createRoomTypeWithRooms(RoomType roomType, List<String> roomNumbers) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        if (roomTypeRepository.existsByStoreIdAndCode(storeId, roomType.getCode())) {
            throw new RuntimeException("房型代码已存在");
        }

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

        return savedRoomType;
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

        existingRoomType.setName(roomType.getName());
        existingRoomType.setCode(roomType.getCode());
        existingRoomType.setTotalRooms(roomType.getTotalRooms());
        existingRoomType.setDescription(roomType.getDescription());
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
