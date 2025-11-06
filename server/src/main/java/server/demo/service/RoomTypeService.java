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
import server.demo.enums.RoomStatus;
import server.demo.dto.RoomTypeWithRoomsDTO;

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

    public List<RoomType> getAllRoomTypes(Long userId) {
        return roomTypeRepository.findByUserIdOrderByName(userId);
    }

    public List<RoomTypeWithRoomsDTO> getAllRoomTypesWithRooms(Long userId) {
        List<RoomType> roomTypes = roomTypeRepository.findByUserIdOrderByName(userId);
        return roomTypes.stream()
            .map(roomType -> {
                List<Room> rooms = roomRepository.findByRoomTypeId(roomType.getId());
                List<RoomTypeWithRoomsDTO.RoomInfoDTO> roomInfos = rooms.stream()
                    .map(RoomTypeWithRoomsDTO.RoomInfoDTO::new)
                    .collect(Collectors.toList());
                return new RoomTypeWithRoomsDTO(roomType, roomInfos);
            })
            .collect(Collectors.toList());
    }

    public Optional<RoomType> getRoomTypeById(Long userId, Long id) {
        return roomTypeRepository.findById(id)
                .filter(roomType -> roomType.getUser().getId().equals(userId));
    }

    public Optional<RoomType> getRoomTypeByCode(Long userId, String code) {
        return roomTypeRepository.findByUserIdAndCode(userId, code);
    }

    public RoomType createRoomType(Long userId, RoomType roomType) {
        // 检查房型代码是否已存在(用户级别)
        if (roomTypeRepository.existsByUserIdAndCode(userId, roomType.getCode())) {
            throw new RuntimeException("房型代码已存在");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        roomType.setUser(user);
        return roomTypeRepository.save(roomType);
    }

    public RoomType createRoomTypeWithRooms(Long userId, RoomType roomType, List<String> roomNumbers) {
        // 检查房型代码是否已存在(用户级别)
        if (roomTypeRepository.existsByUserIdAndCode(userId, roomType.getCode())) {
            throw new RuntimeException("房型代码已存在");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查房间号是否重复
        for (String roomNumber : roomNumbers) {
            if (roomRepository.existsByRoomNumber(roomNumber)) {
                throw new RuntimeException("房间号 " + roomNumber + " 已存在");
            }
        }

        // 设置用户关联并保存房型
        roomType.setUser(user);
        RoomType savedRoomType = roomTypeRepository.save(roomType);

        // 创建对应房间
        for (String roomNumber : roomNumbers) {
            Room room = new Room(roomNumber, savedRoomType, null);
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        return savedRoomType;
    }

    public RoomType updateRoomType(Long userId, Long id, RoomType roomType) {
        RoomType existingRoomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("房型不存在"));

        // 验证数据是否属于当前用户
        if (!existingRoomType.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此房型");
        }

        // 检查代码是否被其他房型使用(用户级别)
        if (!existingRoomType.getCode().equals(roomType.getCode()) &&
            roomTypeRepository.existsByUserIdAndCode(userId, roomType.getCode())) {
            throw new RuntimeException("房型代码已存在");
        }

        existingRoomType.setName(roomType.getName());
        existingRoomType.setCode(roomType.getCode());
        existingRoomType.setTotalRooms(roomType.getTotalRooms());
        existingRoomType.setDescription(roomType.getDescription());
        existingRoomType.setDefaultPrice(roomType.getDefaultPrice());
        existingRoomType.setWeekdayPrice(roomType.getWeekdayPrice());
        existingRoomType.setWeekendPrice(roomType.getWeekendPrice());

        return roomTypeRepository.save(existingRoomType);
    }

    public void deleteRoomType(Long userId, Long id) {
        RoomType roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("房型不存在"));

        // 验证数据是否属于当前用户
        if (!roomType.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限删除此房型");
        }

        // 获取该房型下的所有房间
        List<Room> rooms = roomRepository.findByRoomTypeId(id);

        // 检查每个房间是否有预订记录
        for (Room room : rooms) {
            List<Reservation> reservations = reservationRepository.findByRoomId(room.getId());
            if (!reservations.isEmpty()) {
                throw new RuntimeException("该房型下的房间还有预订记录，无法删除。请先处理或取消相关订单后再删除。");
            }
        }

        // 删除该房型下的所有房间
        roomRepository.deleteAll(rooms);

        // 删除房型
        roomTypeRepository.deleteById(id);
    }
}