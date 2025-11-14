package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.RoomTableDataDTO;
import server.demo.dto.RoomTableStatisticsDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RoomTableService {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    /**
     * 获取房情表统计数据
     * @param date 统计日期
     * @return 房情表数据
     */
    public RoomTableDataDTO getRoomTableStatistics(LocalDate date) {
        try {
            // 获取当前门店ID
            server.demo.context.StoreContext storeContext = server.demo.context.StoreContextHolder.getContext();
            if (storeContext == null || storeContext.getStoreId() == null) {
                throw new RuntimeException("无法获取当前门店信息");
            }
            Long currentStoreId = storeContext.getStoreId();

            // 获取当前门店的所有房型
            List<RoomType> roomTypes = roomTypeRepository.findByStoreId(currentStoreId);

            List<RoomTableStatisticsDTO> statisticsList = new ArrayList<>();
            RoomTableStatisticsDTO totalStatistics = new RoomTableStatisticsDTO("合计", 0);

            for (RoomType roomType : roomTypes) {
                RoomTableStatisticsDTO statistics = calculateRoomTypeStatistics(roomType, date, currentStoreId);
                statisticsList.add(statistics);

                // 累加到合计数据
                addToTotal(totalStatistics, statistics);
            }

            // 计算合计的派生字段
            totalStatistics.calculateAvailableForSale();
            totalStatistics.calculateAvailableRooms();
            totalStatistics.calculateOccupiedRooms();
            totalStatistics.calculateExpectedOccupancyRate();

            return new RoomTableDataDTO(date, statisticsList, totalStatistics);
        } catch (Exception e) {
            System.err.println("获取房情表数据失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取房情表数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算单个房型的统计数据
     */
    private RoomTableStatisticsDTO calculateRoomTypeStatistics(RoomType roomType, LocalDate date, Long storeId) {
        try {
            System.out.println("处理房型: " + roomType.getName() + ", ID: " + roomType.getId());

            // 使用实际房间数而不是totalRooms字段
            List<Room> rooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomType.getId());
            int actualRoomCount = rooms.size();

            System.out.println("房型 " + roomType.getName() + " 的实际房间数: " + actualRoomCount);

            RoomTableStatisticsDTO statistics = new RoomTableStatisticsDTO(roomType.getName(), actualRoomCount);

            // 统计各种状态的房间数量
            int maintenanceRooms = 0;
            int outOfOrderRooms = 0;
            int cleanRooms = 0;
            int dirtyRooms = 0;

            for (Room room : rooms) {
                switch (room.getStatus()) {
                    case MAINTENANCE:
                        maintenanceRooms++;
                        break;
                    case OUT_OF_ORDER:
                        outOfOrderRooms++;
                        break;
                    case AVAILABLE:
                        cleanRooms++;
                        break;
                    default:
                        dirtyRooms++;
                        break;
                }
            }

            statistics.setMaintenanceRooms(maintenanceRooms);
            statistics.setOutOfOrderRooms(outOfOrderRooms);
            statistics.setCleanRooms(cleanRooms);
            statistics.setDirtyRooms(dirtyRooms);

            // 统计预订相关数据
            calculateReservationStatistics(statistics, roomType, date, storeId);

            // 计算派生字段
            statistics.calculateAvailableForSale();
            statistics.calculateAvailableRooms();
            statistics.calculateOccupiedRooms();
            statistics.calculateExpectedOccupancyRate();

            return statistics;
        } catch (Exception e) {
            System.err.println("计算房型 " + roomType.getName() + " 统计数据失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("计算房型统计数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算预订相关统计数据
     */
    private void calculateReservationStatistics(RoomTableStatisticsDTO statistics, RoomType roomType, LocalDate date, Long storeId) {
        try {
            System.out.println("计算房型 " + roomType.getName() + " 的预订统计数据, 日期: " + date);

            // 预抵：当天入住的预订
            List<Reservation> todayArrivals = reservationRepository.findByStoreIdAndRoomTypeAndDate(storeId, roomType.getId(), date, "ARRIVAL");
            statistics.setScheduledArrival(todayArrivals.size());
            System.out.println("预抵: " + todayArrivals.size());

            // 预离：当天退房的预订
            List<Reservation> todayDepartures = reservationRepository.findByStoreIdAndRoomTypeAndDate(storeId, roomType.getId(), date, "DEPARTURE");
            statistics.setScheduledDeparture(todayDepartures.size());
            System.out.println("预离: " + todayDepartures.size());

            // 在住（不含预离）：当前在住但不包括今天退房的
            List<Reservation> occupiedWithoutDeparture = reservationRepository.findOccupiedWithoutDepartureByStore(storeId, roomType.getId(), date);
            statistics.setOccupiedWithoutDeparture(occupiedWithoutDeparture.size());
            System.out.println("在住(不含预离): " + occupiedWithoutDeparture.size());

            // 当日取消房：当天取消的预订数量
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            List<Reservation> todayCancelledReservations = reservationRepository.findCancelledByStoreAndRoomTypeAndDate(
                storeId, roomType.getId(), startOfDay, endOfDay);
            statistics.setDailyCancelledRooms(todayCancelledReservations.size());
            System.out.println("当日取消: " + todayCancelledReservations.size());
        } catch (Exception e) {
            System.err.println("计算预订统计数据失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("计算预订统计数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 累加统计数据到合计
     */
    private void addToTotal(RoomTableStatisticsDTO total, RoomTableStatisticsDTO statistics) {
        total.setTotalRooms(total.getTotalRooms() + (statistics.getTotalRooms() != null ? statistics.getTotalRooms() : 0));
        total.setScheduledArrival(total.getScheduledArrival() + (statistics.getScheduledArrival() != null ? statistics.getScheduledArrival() : 0));
        total.setScheduledDeparture(total.getScheduledDeparture() + (statistics.getScheduledDeparture() != null ? statistics.getScheduledDeparture() : 0));
        total.setOccupiedWithoutDeparture(total.getOccupiedWithoutDeparture() + (statistics.getOccupiedWithoutDeparture() != null ? statistics.getOccupiedWithoutDeparture() : 0));
        total.setReservedRooms(total.getReservedRooms() + (statistics.getReservedRooms() != null ? statistics.getReservedRooms() : 0));
        total.setMaintenanceRooms(total.getMaintenanceRooms() + (statistics.getMaintenanceRooms() != null ? statistics.getMaintenanceRooms() : 0));
        total.setOutOfOrderRooms(total.getOutOfOrderRooms() + (statistics.getOutOfOrderRooms() != null ? statistics.getOutOfOrderRooms() : 0));
        total.setLinkedClosedRooms(total.getLinkedClosedRooms() + (statistics.getLinkedClosedRooms() != null ? statistics.getLinkedClosedRooms() : 0));
        total.setCleanRooms(total.getCleanRooms() + (statistics.getCleanRooms() != null ? statistics.getCleanRooms() : 0));
        total.setDirtyRooms(total.getDirtyRooms() + (statistics.getDirtyRooms() != null ? statistics.getDirtyRooms() : 0));
        total.setDailyCancelledRooms(total.getDailyCancelledRooms() + (statistics.getDailyCancelledRooms() != null ? statistics.getDailyCancelledRooms() : 0));
    }
}