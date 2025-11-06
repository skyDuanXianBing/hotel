package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.dto.BusinessSummaryDTO;
import server.demo.dto.DailyOccupancyDTO;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusinessStatisticsService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * 获取营业汇总统计
     * 按消费记录的发生时间（入住日期）统计，不统计已取消的订单
     */
    public BusinessSummaryDTO getBusinessSummary(LocalDate startDate, LocalDate endDate) {
        BusinessSummaryDTO summary = new BusinessSummaryDTO();

        // 获取时间范围内的有效订单（不包括已取消的）
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> isDateInRange(r.getCheckInDate(), r.getCheckOutDate(), startDate, endDate))
                .collect(Collectors.toList());

        // 计算总营业收入和订单数
        BigDecimal totalRevenue = reservations.stream()
                .map(Reservation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.setTotalRevenue(totalRevenue);
        summary.setTotalOrders(reservations.size());

        // 计算总间夜数
        int totalRoomNights = reservations.stream()
                .mapToInt(r -> calculateRoomNights(r, startDate, endDate))
                .sum();
        summary.setTotalRoomNights(totalRoomNights);

        // 计算平均房价 = 总收入 / 总间夜数
        if (totalRoomNights > 0) {
            BigDecimal averageRate = totalRevenue.divide(
                    new BigDecimal(totalRoomNights), 2, RoundingMode.HALF_UP);
            summary.setAverageRoomRate(averageRate);
        } else {
            summary.setAverageRoomRate(BigDecimal.ZERO);
        }

        // 计算出租率
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long totalRoomCount = roomRepository.count();
        long totalAvailableRoomNights = totalDays * totalRoomCount;

        if (totalAvailableRoomNights > 0) {
            BigDecimal occupancyRate = new BigDecimal(totalRoomNights)
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(totalAvailableRoomNights), 2, RoundingMode.HALF_UP);
            summary.setOccupancyRate(occupancyRate);
        } else {
            summary.setOccupancyRate(BigDecimal.ZERO);
        }

        // 按渠道统计
        summary.setRevenueByChannel(calculateRevenueByChannel(reservations, startDate, endDate));

        // 按房型统计
        summary.setRevenueByRoomType(calculateRevenueByRoomType(reservations, startDate, endDate));

        // 按日期统计
        summary.setRevenueByDate(calculateRevenueByDate(reservations, startDate, endDate));

        return summary;
    }

    /**
     * 判断订单日期是否在统计范围内
     */
    private boolean isDateInRange(LocalDate checkIn, LocalDate checkOut, LocalDate startDate, LocalDate endDate) {
        // 订单的入住或退房日期在统计范围内即计入
        return !(checkOut.isBefore(startDate) || checkIn.isAfter(endDate));
    }

    /**
     * 计算订单在统计范围内的间夜数
     */
    private int calculateRoomNights(Reservation reservation, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveCheckIn = reservation.getCheckInDate().isBefore(startDate)
                ? startDate : reservation.getCheckInDate();
        LocalDate effectiveCheckOut = reservation.getCheckOutDate().isAfter(endDate)
                ? endDate.plusDays(1) : reservation.getCheckOutDate();

        long nights = ChronoUnit.DAYS.between(effectiveCheckIn, effectiveCheckOut);
        return nights > 0 ? (int) nights : 0;
    }

    /**
     * 按渠道统计收入
     */
    private List<BusinessSummaryDTO.ChannelRevenue> calculateRevenueByChannel(
            List<Reservation> reservations, LocalDate startDate, LocalDate endDate) {

        Map<String, List<Reservation>> channelMap = reservations.stream()
                .collect(Collectors.groupingBy(r -> r.getChannel().getName()));

        return channelMap.entrySet().stream()
                .map(entry -> {
                    String channelName = entry.getKey();
                    List<Reservation> channelReservations = entry.getValue();

                    BigDecimal revenue = channelReservations.stream()
                            .map(Reservation::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int roomNights = channelReservations.stream()
                            .mapToInt(r -> calculateRoomNights(r, startDate, endDate))
                            .sum();

                    return new BusinessSummaryDTO.ChannelRevenue(
                            channelName, revenue, channelReservations.size(), roomNights);
                })
                .sorted(Comparator.comparing(BusinessSummaryDTO.ChannelRevenue::getRevenue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 按房型统计收入
     */
    private List<BusinessSummaryDTO.RoomTypeRevenue> calculateRevenueByRoomType(
            List<Reservation> reservations, LocalDate startDate, LocalDate endDate) {

        Map<String, List<Reservation>> roomTypeMap = reservations.stream()
                .collect(Collectors.groupingBy(r -> r.getRoom().getRoomType().getName()));

        return roomTypeMap.entrySet().stream()
                .map(entry -> {
                    String roomTypeName = entry.getKey();
                    List<Reservation> roomTypeReservations = entry.getValue();

                    BigDecimal revenue = roomTypeReservations.stream()
                            .map(Reservation::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int roomNights = roomTypeReservations.stream()
                            .mapToInt(r -> calculateRoomNights(r, startDate, endDate))
                            .sum();

                    return new BusinessSummaryDTO.RoomTypeRevenue(
                            roomTypeName, revenue, roomTypeReservations.size(), roomNights);
                })
                .sorted(Comparator.comparing(BusinessSummaryDTO.RoomTypeRevenue::getRevenue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 按日期统计收入（每日收入）
     */
    private List<BusinessSummaryDTO.DailyRevenue> calculateRevenueByDate(
            List<Reservation> reservations, LocalDate startDate, LocalDate endDate) {

        Map<LocalDate, List<Reservation>> dateMap = new HashMap<>();

        // 初始化每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dateMap.put(currentDate, new ArrayList<>());
            currentDate = currentDate.plusDays(1);
        }

        // 将订单分配到每一天（按入住日期）
        for (Reservation reservation : reservations) {
            LocalDate checkInDate = reservation.getCheckInDate();
            if (!checkInDate.isBefore(startDate) && !checkInDate.isAfter(endDate)) {
                dateMap.get(checkInDate).add(reservation);
            }
        }

        return dateMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Reservation> dailyReservations = entry.getValue();

                    BigDecimal revenue = dailyReservations.stream()
                            .map(Reservation::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int roomNights = dailyReservations.stream()
                            .mapToInt(r -> (int) ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate()))
                            .sum();

                    return new BusinessSummaryDTO.DailyRevenue(
                            date.toString(), revenue, dailyReservations.size(), roomNights);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取每日入住率统计
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日入住率列表
     */
    public List<DailyOccupancyDTO> getDailyOccupancy(LocalDate startDate, LocalDate endDate) {
        List<DailyOccupancyDTO> result = new ArrayList<>();
        long totalRoomCount = roomRepository.count();

        // 遍历日期范围内的每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate date = currentDate;

            // 查询当天在住的预订（已入住状态，且当天在入住期间内）
            long occupiedRooms = reservationRepository.findAll().stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN)
                    .filter(r -> !r.getCheckInDate().isAfter(date) && r.getCheckOutDate().isAfter(date))
                    .count();

            // 计算入住率
            BigDecimal occupancyRate = BigDecimal.ZERO;
            if (totalRoomCount > 0) {
                occupancyRate = new BigDecimal(occupiedRooms)
                        .multiply(new BigDecimal(100))
                        .divide(new BigDecimal(totalRoomCount), 2, RoundingMode.HALF_UP);
            }

            DailyOccupancyDTO dto = new DailyOccupancyDTO(
                    date.toString(),
                    occupancyRate,
                    (int) occupiedRooms,
                    (int) totalRoomCount
            );

            result.add(dto);
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }
}