package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.dto.*;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.ReservationRepository;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreContextUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FutureRoomTableService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final String[] WEEKDAYS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    /**
     * 获取远期房情表数据
     * @param startDate 开始日期
     * @param days 天数
     * @return 远期房情表数据
     */
    public FutureRoomTableResponse getFutureRoomTableData(LocalDate startDate, int days) {
        LocalDate endDate = startDate.plusDays(days - 1);
        Long storeId = currentStoreId();
        
        // 获取所有房型
        List<RoomType> roomTypes = roomTypeRepository.findByStoreId(storeId);
        
        // 获取日期范围内的所有预订
        List<Reservation> reservations = reservationRepository.findByStoreIdAndDateRange(storeId, startDate, endDate);
        
        // 为每个房型生成数据
        List<FutureRoomTypeData> roomTypeDataList = new ArrayList<>();
        FutureRoomTypeData totalData = null;
        
        // 初始化合计数据
        List<FutureDateRoomData> totalDates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String dayOfWeek = WEEKDAYS[currentDate.getDayOfWeek().getValue() % 7];
            
            totalDates.add(new FutureDateRoomData(dateStr, dayOfWeek, 0, 0, 0, 0.0, 0.0, 0.0));
        }
        
        int totalRoomsCount = 0;
        
        // 处理每个房型
        for (RoomType roomType : roomTypes) {
            List<Room> roomsOfType = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomType.getId());
            int roomCount = roomsOfType.size();
            totalRoomsCount += roomCount;
            
            List<FutureDateRoomData> dates = new ArrayList<>();
            
            // 为每一天生成数据
            for (int i = 0; i < days; i++) {
                LocalDate currentDate = startDate.plusDays(i);
                String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String dayOfWeek = WEEKDAYS[currentDate.getDayOfWeek().getValue() % 7];
                
                // 计算当日房型的房间状态
                int occupiedCount = calculateOccupiedRooms(roomsOfType, currentDate, reservations);
                int unavailableCount = calculateUnavailableRooms(roomsOfType, currentDate);
                int availableCount = roomCount - occupiedCount - unavailableCount;
                
                // 计算比例
                double availableRate = roomCount > 0 ? (availableCount * 100.0 / roomCount) : 0.0;
                double occupiedRate = roomCount > 0 ? (occupiedCount * 100.0 / roomCount) : 0.0;
                double unavailableRate = roomCount > 0 ? (unavailableCount * 100.0 / roomCount) : 0.0;
                
                FutureDateRoomData dateData = new FutureDateRoomData(
                    dateStr, dayOfWeek, availableCount, occupiedCount, unavailableCount,
                    Math.round(availableRate * 10) / 10.0,
                    Math.round(occupiedRate * 10) / 10.0,
                    Math.round(unavailableRate * 10) / 10.0
                );
                
                dates.add(dateData);
                
                // 累加到合计数据
                FutureDateRoomData totalDate = totalDates.get(i);
                totalDate.setAvailable(totalDate.getAvailable() + availableCount);
                totalDate.setOccupied(totalDate.getOccupied() + occupiedCount);
                totalDate.setUnavailable(totalDate.getUnavailable() + unavailableCount);
            }
            
            roomTypeDataList.add(new FutureRoomTypeData(roomType.getName(), roomCount, dates));
        }
        
        // 计算合计数据的比例
        for (FutureDateRoomData totalDate : totalDates) {
            if (totalRoomsCount > 0) {
                totalDate.setAvailableRate(Math.round(totalDate.getAvailable() * 100.0 / totalRoomsCount * 10) / 10.0);
                totalDate.setOccupiedRate(Math.round(totalDate.getOccupied() * 100.0 / totalRoomsCount * 10) / 10.0);
                totalDate.setUnavailableRate(Math.round(totalDate.getUnavailable() * 100.0 / totalRoomsCount * 10) / 10.0);
            }
        }
        
        totalData = new FutureRoomTypeData("合计", totalRoomsCount, totalDates);
        
        // 生成底部统计数据
        List<FutureRoomStatistics> statistics = generateStatistics(startDate, days, roomTypes, totalDates);
        
        return new FutureRoomTableResponse(
            startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            roomTypeDataList,
            totalData,
            statistics
        );
    }
    
    /**
     * 计算占用房间数
     * 参考房价管理的calculateAvailableRooms逻辑
     */
    private int calculateOccupiedRooms(List<Room> rooms, LocalDate date, List<Reservation> allReservations) {
        // 获取房间ID集合,用于快速查找
        Set<Long> roomIds = rooms.stream()
            .map(Room::getId)
            .collect(Collectors.toSet());

        // 直接从预订中筛选符合条件的房间数
        long occupiedCount = allReservations.stream()
            .filter(r -> {
                // 检查预订状态：已确认、已入住状态都算占用
                boolean isOccupiedStatus = r.getStatus() == ReservationStatus.CONFIRMED ||
                                         r.getStatus() == ReservationStatus.CHECKED_IN;

                // 检查日期是否在预订范围内: checkInDate <= date < checkOutDate
                boolean isInDateRange = !date.isBefore(r.getCheckInDate()) &&
                                      date.isBefore(r.getCheckOutDate());

                // 检查是否属于当前房型的房间
                boolean isTargetRoom = r.getRoom() != null &&
                                      roomIds.contains(r.getRoom().getId());

                return isOccupiedStatus && isInDateRange && isTargetRoom;
            })
            .count();

        return (int) occupiedCount;
    }
    
    /**
     * 计算不可售房间数（维修、停用等）
     */
    private int calculateUnavailableRooms(List<Room> rooms, LocalDate date) {
        // 简化处理，实际应该根据房间状态表或维修记录
        return 0;
    }
    
    /**
     * 生成底部统计数据
     */
    private List<FutureRoomStatistics> generateStatistics(LocalDate startDate, int days, 
                                                         List<RoomType> roomTypes, 
                                                         List<FutureDateRoomData> totalDates) {
        List<FutureRoomStatistics> statistics = new ArrayList<>();
        
        for (int i = 0; i < days; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 获取当日合计数据
            FutureDateRoomData dayData = totalDates.get(i);
            int effectiveRooms = dayData.getAvailable() + dayData.getOccupied(); // 有效客房数 = 可售 + 已占用
            int occupiedRooms = dayData.getOccupied();
            
            // 计算入住率（预期）
            double expectedOccupancyRate = effectiveRooms > 0 ? 
                Math.round((occupiedRooms * 100.0 / effectiveRooms) * 10) / 10.0 : 0.0;
            
            // 计算收入相关数据
            double expectedRoomRevenue = calculateExpectedRoomRevenue(roomTypes, currentDate, occupiedRooms);
            double expectedTotalRoomFee = calculateExpectedTotalRoomFee(roomTypes, currentDate, effectiveRooms);
            double averageRoomRevenue = effectiveRooms > 0 ? 
                Math.round((expectedRoomRevenue / effectiveRooms) * 100) / 100.0 : 0.0;
            
            statistics.add(new FutureRoomStatistics(
                dateStr,
                effectiveRooms,
                expectedOccupancyRate,
                expectedRoomRevenue,
                expectedTotalRoomFee,
                averageRoomRevenue
            ));
        }
        
        return statistics;
    }
    
    /**
     * 计算预期客房收入（已预订房间的收入）
     */
    private double calculateExpectedRoomRevenue(List<RoomType> roomTypes, LocalDate date, int totalOccupiedRooms) {
        // 获取当前门店ID
        server.demo.context.StoreContext storeContext = server.demo.context.StoreContextHolder.getContext();
        if (storeContext == null || storeContext.getStoreId() == null) {
            throw new RuntimeException("无法获取当前门店信息");
        }
        Long currentStoreId = storeContext.getStoreId();

        double totalRevenue = 0.0;

        for (RoomType roomType : roomTypes) {
            // 获取该房型在指定日期的占用房间数
            List<Room> roomsOfType = roomRepository.findByStoreIdAndRoomTypeId(currentStoreId, roomType.getId());
            List<Reservation> reservations = reservationRepository.findByStoreIdAndDateRange(currentStoreId, date, date.plusDays(1));
            
            int occupiedCount = (int) roomsOfType.stream()
                .filter(room -> {
                    return reservations.stream()
                        .anyMatch(reservation -> 
                            reservation.getRoom() != null &&
                            reservation.getRoom().getId().equals(room.getId()) &&
                            !date.isBefore(reservation.getCheckInDate()) &&
                            date.isBefore(reservation.getCheckOutDate()) &&
                            (reservation.getStatus() == ReservationStatus.CONFIRMED ||
                             reservation.getStatus() == ReservationStatus.CHECKED_IN)
                        );
                })
                .count();
                
            // 根据日期类型获取房价
            double roomPrice = getRoomPriceForDate(roomType, date);
            totalRevenue += occupiedCount * roomPrice;
        }
        
        return Math.round(totalRevenue * 100) / 100.0;
    }
    
    /**
     * 计算预期总房费（所有可售房间的潜在收入）
     */
    private double calculateExpectedTotalRoomFee(List<RoomType> roomTypes, LocalDate date, int totalEffectiveRooms) {
        Long storeId = currentStoreId();
        double totalFee = 0.0;

        for (RoomType roomType : roomTypes) {
            List<Room> roomsOfType = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomType.getId());
            int effectiveRoomsOfType = roomsOfType.size(); // 简化处理，假设所有房间都可售
            
            double roomPrice = getRoomPriceForDate(roomType, date);
            totalFee += effectiveRoomsOfType * roomPrice;
        }
        
        return Math.round(totalFee * 100) / 100.0;
    }
    
    /**
     * 根据日期获取房间价格（周末/工作日）
     */
    private double getRoomPriceForDate(RoomType roomType, LocalDate date) {
        // 判断是否为周末（周六、周日）
        boolean isWeekend = date.getDayOfWeek().getValue() >= 6;
        
        BigDecimal price;
        if (isWeekend && roomType.getWeekendPrice() != null) {
            price = roomType.getWeekendPrice();
        } else if (!isWeekend && roomType.getWeekdayPrice() != null) {
            price = roomType.getWeekdayPrice();
        } else if (roomType.getDefaultPrice() != null) {
            price = roomType.getDefaultPrice();
        } else {
            price = BigDecimal.ZERO; // 如果没有设置价格，默认为0
        }
        
        return price.doubleValue();
    }
}
