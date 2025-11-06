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

    public RoomStatusCalendarDTO getRoomStatusCalendar(LocalDate startDate, LocalDate endDate) {
        try {
            List<Room> rooms = roomRepository.findAllWithRoomType();
            
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

    public RoomStatusStatisticsDTO getRoomStatusStatistics(LocalDate date) {
        // 今日预抵：今天预订的和到期的预订
        long todayArrivals = reservationRepository.countTodayArrivals(date);
        
        // 今日预离：今天退房的订单
        long todayDepartures = reservationRepository.countByCheckOutDate(date);
        
        // 今日新办：今天办理的补办、入住、预办
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        long todayNewOrders = reservationRepository.countTodayNewOrders(startOfDay, endOfDay);
        
        // 今日可售：未被预订且非停用的房间
        long availableRooms = roomRepository.countAvailableRoomsForDate(date);
        
        // 未排房：没有分配房间的订单
        long unassignedOrders = reservationRepository.countByRoomIsNull();
        
        // 待处理：已确认但未入住的订单
        long pendingOrders = reservationRepository.countPendingOrders();
        
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