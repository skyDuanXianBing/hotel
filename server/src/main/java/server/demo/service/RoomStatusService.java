package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.RoomStatusStatisticsDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoomStatusService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    /**
     * 获取房态日历数据 - 按用户过滤
     *
     * @param userId 用户ID,用于数据隔离
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 房态日历数据
     */
    public RoomStatusCalendarDTO getRoomStatusCalendar(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            // 只查询当前用户的房间
            System.out.println("===== 获取房态日历 =====");
            System.out.println("当前用户ID: " + userId);
            List<Room> rooms = roomRepository.findByUserIdWithRoomType(userId);
            System.out.println("查询到的用户房间数: " + rooms.size());

            // 如果当前用户没有房间，检查是否有未分配的房间（userId为null或0）
            // 这是一个兼容处理，用于支持从旧系统升级
            if (rooms.isEmpty()) {
                System.out.println("当前用户没有房间，检查是否有未分配的房间...");

                // 查找userId为null的房间
                List<Room> unassignedRooms = roomRepository.findByUserId(null);
                System.out.println("发现userId为null的房间数: " + (unassignedRooms != null ? unassignedRooms.size() : 0));

                // 查找userId为0的房间
                List<Room> zeroUserIdRooms = roomRepository.findByUserId(0L);
                System.out.println("发现userId为0的房间数: " + (zeroUserIdRooms != null ? zeroUserIdRooms.size() : 0));

                // 合并两个列表
                List<Room> allUnassignedRooms = new ArrayList<>();
                if (unassignedRooms != null) {
                    allUnassignedRooms.addAll(unassignedRooms);
                }
                if (zeroUserIdRooms != null) {
                    allUnassignedRooms.addAll(zeroUserIdRooms);
                }

                // 如果存在未分配的房间，将它们分配给当前用户
                if (!allUnassignedRooms.isEmpty()) {
                    System.out.println("将 " + allUnassignedRooms.size() + " 个未分配的房间分配给用户 " + userId);
                    for (Room room : allUnassignedRooms) {
                        room.setUserId(userId);
                    }
                    roomRepository.saveAll(allUnassignedRooms);

                    // 重新查询带RoomType的房间数据
                    rooms = roomRepository.findByUserIdWithRoomType(userId);
                    System.out.println("迁移后重新查询,获得房间数: " + rooms.size());
                }
            }
            
            RoomStatusCalendarDTO.DateRangeDTO dateRange = 
                new RoomStatusCalendarDTO.DateRangeDTO(startDate, endDate);
            
            List<RoomStatusCalendarDTO.CalendarRoomDataDTO> roomDataList = new ArrayList<>();
            
            for (Room room : rooms) {
                try {
                    List<DailyRoomStatusDTO> dailyStatusList = new ArrayList<>();
                    
                    LocalDate currentDate = startDate;
                    while (!currentDate.isAfter(endDate)) {
                        RoomStatus status = determineRoomStatus(room, currentDate);
                        DailyRoomStatusDTO.ReservationInfoDTO reservationInfo = 
                            getReservationInfo(room, currentDate);
                        
                        DailyRoomStatusDTO dailyStatus = new DailyRoomStatusDTO(
                            currentDate, status, reservationInfo);
                        dailyStatusList.add(dailyStatus);
                        
                        currentDate = currentDate.plusDays(1);
                    }
                    
                    // 确保roomType不为null
                    String roomTypeName = room.getRoomType() != null ? 
                        room.getRoomType().getName() : "未知房型";
                    
                    RoomStatusCalendarDTO.CalendarRoomDataDTO roomData = 
                        new RoomStatusCalendarDTO.CalendarRoomDataDTO(
                            room.getId(),
                            room.getRoomNumber(),
                            roomTypeName,
                            dailyStatusList
                        );
                    roomDataList.add(roomData);
                } catch (Exception e) {
                    // 记录单个房间处理失败，但不影响整体响应
                    System.err.println("处理房间 " + room.getRoomNumber() + " 时出错: " + e.getMessage());
                }
            }

            System.out.println("===== 房态日历数据处理完成 =====");
            System.out.println("最终返回的房间数据数量: " + roomDataList.size());

            return new RoomStatusCalendarDTO(dateRange, roomDataList);
        } catch (Exception e) {
            System.err.println("获取房态日历数据失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取房态日历数据失败: " + e.getMessage(), e);
        }
    }

    public void updateRoomStatus(Long roomId, LocalDate date, RoomStatus newStatus, String reason) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("房间不存在"));
        
        // 检查是否有冲突的预订
        Optional<Reservation> existingReservation = 
            reservationRepository.findByRoomIdAndDate(roomId, date);
        
        if (existingReservation.isPresent() && 
            (newStatus == RoomStatus.AVAILABLE || newStatus == RoomStatus.MAINTENANCE)) {
            throw new RuntimeException("该日期已有预订，无法修改为该状态");
        }
        
        // 更新房间基础状态（如果需要的话）
        if (isDateToday(date)) {
            room.setStatus(newStatus);
            roomRepository.save(room);
        }
        
        // 这里可以记录房态修改日志
        // roomStatusLogService.logStatusChange(roomId, date, room.getStatus(), newStatus, reason);
    }

    private RoomStatus determineRoomStatus(Room room, LocalDate date) {
        // 首先检查是否有预订
        Optional<Reservation> reservation = reservationRepository.findByRoomIdAndDate(room.getId(), date);
        
        if (reservation.isPresent()) {
            ReservationStatus reservationStatus = reservation.get().getStatus();
            switch (reservationStatus) {
                case CONFIRMED:
                    return RoomStatus.RESERVED;
                case CHECKED_IN:
                    return RoomStatus.OCCUPIED;
                default:
                    break;
            }
        }
        
        // 如果是今天，返回房间的实际状态
        if (isDateToday(date)) {
            return room.getStatus();
        }
        
        // 其他日期默认为可用
        return RoomStatus.AVAILABLE;
    }

    private DailyRoomStatusDTO.ReservationInfoDTO getReservationInfo(Room room, LocalDate date) {
        try {
            Optional<Reservation> reservation = reservationRepository.findByRoomIdAndDate(room.getId(), date);
            
            if (reservation.isPresent()) {
                Reservation r = reservation.get();
                String channelName = r.getChannel() != null ? r.getChannel().getName() : "未知渠道";
                
                return new DailyRoomStatusDTO.ReservationInfoDTO(
                    r.getId(),
                    r.getGuestName(),
                    channelName,
                    r.getCheckInDate(),
                    r.getCheckOutDate(),
                    r.getOrderNumber()
                );
            }
        } catch (Exception e) {
            System.err.println("获取预订信息失败，房间ID: " + room.getId() + ", 日期: " + date + ", 错误: " + e.getMessage());
        }
        
        return null;
    }

    private boolean isDateToday(LocalDate date) {
        return date.equals(LocalDate.now());
    }

    public RoomStatusStatisticsDTO getRoomStatusStatistics(Long userId, LocalDate date) {
        // 获取用户的房间列表
        List<Room> userRooms = roomRepository.findByUserId(userId);
        List<Long> roomIds = userRooms.stream().map(Room::getId).toList();

        // 今日预抵：今天预订的和到期的预订
        long todayArrivals = reservationRepository.countTodayArrivalsForUser(date, roomIds);

        // 今日预离：今天退房的订单
        long todayDepartures = reservationRepository.countByCheckOutDateForUser(date, roomIds);

        // 今日新办：今天办理的补办、入住、预办
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        long todayNewOrders = reservationRepository.countTodayNewOrdersForUser(startOfDay, endOfDay, roomIds);

        // 今日可售：未被预订且非停用的房间
        long availableRooms = roomRepository.countAvailableRoomsForDateAndUser(date, userId);

        // 未排房：没有分配房间的订单(用户的订单)
        long unassignedOrders = reservationRepository.countByRoomIsNullForUser(roomIds);

        // 待处理：已确认但未入住的订单(用户的订单)
        long pendingOrders = reservationRepository.countPendingOrdersForUser(roomIds);

        return new RoomStatusStatisticsDTO(
            date,
            todayArrivals,
            todayDepartures,
            todayNewOrders,
            availableRooms,
            unassignedOrders,
            pendingOrders
        );
    }
}