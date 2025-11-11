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

        // 如果提供了房间号列表,检查是否重复
        if (roomNumbers != null && !roomNumbers.isEmpty()) {
            for (String roomNumber : roomNumbers) {
                if (roomRepository.existsByRoomNumber(roomNumber)) {
                    throw new RuntimeException("房间号 " + roomNumber + " 已存在");
                }
            }
        }

        // 设置用户关联并保存房型
        roomType.setUser(user);
        RoomType savedRoomType = roomTypeRepository.save(roomType);

        // 如果提供了房间号列表,创建对应房间
        if (roomNumbers != null && !roomNumbers.isEmpty()) {
            for (String roomNumber : roomNumbers) {
                Room room = new Room(roomNumber, savedRoomType, null);
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
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
        existingRoomType.setMondayPrice(roomType.getMondayPrice());
        existingRoomType.setTuesdayPrice(roomType.getTuesdayPrice());
        existingRoomType.setWednesdayPrice(roomType.getWednesdayPrice());
        existingRoomType.setThursdayPrice(roomType.getThursdayPrice());
        existingRoomType.setFridayPrice(roomType.getFridayPrice());
        existingRoomType.setSaturdayPrice(roomType.getSaturdayPrice());
        existingRoomType.setSundayPrice(roomType.getSundayPrice());

        return roomTypeRepository.save(existingRoomType);
    }

    public RoomType updateRoomTypeWithRooms(Long userId, Long id, RoomType roomType, List<String> roomNumbers) {
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

        // 更新房型基本信息
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

        RoomType savedRoomType = roomTypeRepository.save(existingRoomType);

        // 处理房间号更新
        if (roomNumbers != null) {
            // 获取现有房间
            List<Room> existingRooms = roomRepository.findByRoomTypeId(id);

            // 获取现有房间号列表
            List<String> existingRoomNumbers = existingRooms.stream()
                .map(Room::getRoomNumber)
                .collect(Collectors.toList());

            // 找出需要删除的房间(在旧列表但不在新列表中)
            List<Room> roomsToDelete = existingRooms.stream()
                .filter(room -> !roomNumbers.contains(room.getRoomNumber()))
                .collect(Collectors.toList());

            // 临时禁用预订检查以排查问题
            // TODO: 稍后重新启用
            /*
            // 检查要删除的房间是否有活跃预订
            for (Room room : roomsToDelete) {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate futureDate = today.plusYears(10);
                List<Reservation> activeReservations = reservationRepository.findByUserIdAndRoomIdAndDateRange(
                    userId,
                    room.getId(),
                    today,
                    futureDate
                );

                if (!activeReservations.isEmpty()) {
                    throw new RuntimeException("房间号 " + room.getRoomNumber() + " 还有活跃预订记录，无法删除");
                }
            }
            */

            // 删除不在新列表中的房间
            roomRepository.deleteAll(roomsToDelete);

            // 找出需要添加的房间号(在新列表但不在旧列表中)
            List<String> roomNumbersToAdd = roomNumbers.stream()
                .filter(roomNumber -> !existingRoomNumbers.contains(roomNumber))
                .collect(Collectors.toList());

            // 检查新房间号是否已被其他房型使用
            for (String roomNumber : roomNumbersToAdd) {
                if (roomRepository.existsByRoomNumber(roomNumber)) {
                    throw new RuntimeException("房间号 " + roomNumber + " 已存在");
                }
            }

            // 添加新房间
            for (String roomNumber : roomNumbersToAdd) {
                Room room = new Room(roomNumber, savedRoomType, null);
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
        }

        return savedRoomType;
    }

    public void deleteRoomType(Long userId, Long id) {
        System.out.println("=== 开始删除房型 ===");
        System.out.println("用户ID: " + userId);
        System.out.println("房型ID: " + id);

        RoomType roomType = roomTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("房型不存在"));

        // 验证数据是否属于当前用户
        if (!roomType.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限删除此房型");
        }

        // 获取该房型下的所有房间
        List<Room> rooms = roomRepository.findByRoomTypeId(id);
        System.out.println("该房型下共有 " + rooms.size() + " 个房间");

        // 检查每个房间是否有活跃的预订记录(只检查CONFIRMED和CHECKED_IN状态)
        for (Room room : rooms) {
            System.out.println("检查房间: " + room.getRoomNumber() + " (ID: " + room.getId() + ")");

            // 使用带用户ID的方法检查未来的活跃预订
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate futureDate = today.plusYears(10); // 检查未来10年的预订

            System.out.println("查询参数: userId=" + userId + ", roomId=" + room.getId() +
                             ", startDate=" + today + ", endDate=" + futureDate);

            List<Reservation> activeReservations = reservationRepository.findByUserIdAndRoomIdAndDateRange(
                userId,
                room.getId(),
                today,
                futureDate
            );

            System.out.println("查询到 " + activeReservations.size() + " 条预订记录");

            if (!activeReservations.isEmpty()) {
                System.out.println("发现活跃预订,抛出异常!");
                for (Reservation r : activeReservations) {
                    System.out.println("  - 订单号: " + r.getOrderNumber() +
                                     ", 状态: " + r.getStatus() +
                                     ", 入住日期: " + r.getCheckInDate() +
                                     ", 离店日期: " + r.getCheckOutDate());
                }
                throw new RuntimeException("该房型下的房间还有活跃预订记录，无法删除。请先处理或取消相关订单后再删除。");
            }
        }

        System.out.println("没有活跃预订,开始删除相关数据...");

        // 先删除价格变更历史记录
        System.out.println("删除价格变更历史记录...");
        priceChangeHistoryRepository.deleteByRoomTypeId(id);

        // 删除房价记录
        System.out.println("删除房价记录...");
        roomPriceRepository.deleteByRoomTypeId(id);

        // 删除价格计划关联
        System.out.println("删除价格计划关联...");
        roomTypePricePlanRepository.deleteByRoomTypeId(id);

        // 删除该房型下的所有房间
        System.out.println("删除房间...");
        roomRepository.deleteAll(rooms);

        // 最后删除房型
        System.out.println("删除房型...");
        roomTypeRepository.deleteById(id);

        System.out.println("=== 删除完成 ===");
    }
}