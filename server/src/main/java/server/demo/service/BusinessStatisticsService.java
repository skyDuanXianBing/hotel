package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.*;
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

    @Autowired
    private ReservationRevenueAllocationService revenueAllocationService;

    /**
     * 获取当前门店ID
     */
    private Long getCurrentStoreId() {
        StoreContext context = StoreContextHolder.getContext();
        if (context == null || context.getStoreId() == null) {
            throw new RuntimeException("无法获取当前门店信息");
        }
        return context.getStoreId();
    }

    /**
     * 获取营业汇总统计(门店级)
     * 按消费记录的发生时间（入住日期）统计，不统计已取消的订单
     */
    public BusinessSummaryDTO getBusinessSummary(LocalDate startDate, LocalDate endDate) {
        Long storeId = getCurrentStoreId();
        BusinessSummaryDTO summary = new BusinessSummaryDTO();

        List<Reservation> reservations = findActiveReservations(storeId, startDate, endDate);
        ReservationRevenueAllocationService.AllocationResult allocationResult =
                revenueAllocationService.allocateRevenue(storeId, reservations, startDate, endDate);
        List<ReservationRevenueAllocationService.Allocation> allocations = allocationResult.allocations();

        BigDecimal totalRevenue = allocationResult.totalRevenue();

        summary.setTotalRevenue(totalRevenue);
        summary.setTotalOrders(reservations.size());
        summary.setRevenuePrecision(allocationResult.revenuePrecision());

        int totalRoomNights = allocationResult.totalRoomNights();
        summary.setTotalRoomNights(totalRoomNights);

        if (totalRoomNights > 0) {
            BigDecimal averageRate = totalRevenue.divide(
                    new BigDecimal(totalRoomNights), 2, RoundingMode.HALF_UP);
            summary.setAverageRoomRate(averageRate);
        } else {
            summary.setAverageRoomRate(BigDecimal.ZERO);
        }

        // 计算出租率 - 只统计当前门店的房间
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long totalRoomCount = roomRepository.countByStoreId(storeId);
        long totalAvailableRoomNights = totalDays * totalRoomCount;

        if (totalAvailableRoomNights > 0) {
            BigDecimal occupancyRate = new BigDecimal(totalRoomNights)
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(totalAvailableRoomNights), 2, RoundingMode.HALF_UP);
            summary.setOccupancyRate(occupancyRate);
        } else {
            summary.setOccupancyRate(BigDecimal.ZERO);
        }

        summary.setRevenueByChannel(calculateRevenueByChannel(allocations));
        summary.setRevenueByRoomType(calculateRevenueByRoomType(allocations));
        summary.setRevenueByDate(calculateRevenueByDate(allocations, startDate, endDate));

        return summary;
    }

    /**
     * 判断订单日期是否在统计范围内
     */
    private boolean isDateInRange(LocalDate checkIn, LocalDate checkOut, LocalDate startDate, LocalDate endDate) {
        return checkIn.isBefore(endDate.plusDays(1)) && checkOut.isAfter(startDate);
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
            List<ReservationRevenueAllocationService.Allocation> allocations) {

        Map<String, RevenueAggregation> channelMap = new HashMap<>();
        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            String channelName = getChannelName(allocation.reservation());
            RevenueAggregation aggregation = channelMap.computeIfAbsent(channelName, key -> new RevenueAggregation());
            aggregation.add(allocation);
        }

        List<BusinessSummaryDTO.ChannelRevenue> result = new ArrayList<>();
        for (Map.Entry<String, RevenueAggregation> entry : channelMap.entrySet()) {
            RevenueAggregation aggregation = entry.getValue();
            result.add(new BusinessSummaryDTO.ChannelRevenue(
                    entry.getKey(),
                    aggregation.revenue,
                    aggregation.orderIds.size(),
                    aggregation.roomNights
            ));
        }
        result.sort(Comparator.comparing(BusinessSummaryDTO.ChannelRevenue::getRevenue).reversed());
        return result;
    }

    /**
     * 按房型统计收入
     */
    private List<BusinessSummaryDTO.RoomTypeRevenue> calculateRevenueByRoomType(
            List<ReservationRevenueAllocationService.Allocation> allocations) {

        Map<String, RevenueAggregation> roomTypeMap = new HashMap<>();
        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            String roomTypeName = getRoomTypeName(allocation.reservation());
            RevenueAggregation aggregation = roomTypeMap.computeIfAbsent(roomTypeName, key -> new RevenueAggregation());
            aggregation.add(allocation);
        }

        List<BusinessSummaryDTO.RoomTypeRevenue> result = new ArrayList<>();
        for (Map.Entry<String, RevenueAggregation> entry : roomTypeMap.entrySet()) {
            RevenueAggregation aggregation = entry.getValue();
            result.add(new BusinessSummaryDTO.RoomTypeRevenue(
                    entry.getKey(),
                    aggregation.revenue,
                    aggregation.orderIds.size(),
                    aggregation.roomNights
            ));
        }
        result.sort(Comparator.comparing(BusinessSummaryDTO.RoomTypeRevenue::getRevenue).reversed());
        return result;
    }

    /**
     * 按日期统计收入（每日收入）
     */
    private List<BusinessSummaryDTO.DailyRevenue> calculateRevenueByDate(
            List<ReservationRevenueAllocationService.Allocation> allocations,
            LocalDate startDate,
            LocalDate endDate) {

        Map<LocalDate, RevenueAggregation> dateMap = new HashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dateMap.put(currentDate, new RevenueAggregation());
            currentDate = currentDate.plusDays(1);
        }

        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            RevenueAggregation aggregation = dateMap.get(allocation.date());
            if (aggregation != null) {
                aggregation.add(allocation);
            }
        }

        List<BusinessSummaryDTO.DailyRevenue> result = new ArrayList<>();
        currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dateMap.get(currentDate);
            result.add(new BusinessSummaryDTO.DailyRevenue(
                    currentDate.toString(),
                    aggregation.revenue,
                    aggregation.orderIds.size(),
                    aggregation.roomNights
            ));
            currentDate = currentDate.plusDays(1);
        }
        return result;
    }

    /**
     * 获取每日入住率统计(门店级)
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日入住率列表
     */
    public List<DailyOccupancyDTO> getDailyOccupancy(LocalDate startDate, LocalDate endDate) {
        Long storeId = getCurrentStoreId();
        List<DailyOccupancyDTO> result = new ArrayList<>();
        long totalRoomCount = roomRepository.countByStoreId(storeId);

        // 遍历日期范围内的每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate date = currentDate;

            // 查询当天在住的预订（已入住状态，且当天在入住期间内）- 只查询当前门店的订单
            long occupiedRooms = reservationRepository.findByStoreId(storeId).stream()
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

    /**
     * 获取营业概况详细统计(门店级)
     * 包括房费、押金、退房金、餐食消费等详细数据
     */
    public BusinessOverviewDTO getBusinessOverview(LocalDate startDate, LocalDate endDate) {
        Long storeId = getCurrentStoreId();
        BusinessOverviewDTO overview = new BusinessOverviewDTO();

        List<Reservation> reservations = findActiveReservations(storeId, startDate, endDate);
        ReservationRevenueAllocationService.AllocationResult allocationResult =
                revenueAllocationService.allocateRevenue(storeId, reservations, startDate, endDate);
        List<ReservationRevenueAllocationService.Allocation> allocations = allocationResult.allocations();

        BigDecimal roomFee = allocationResult.totalRevenue();

        BigDecimal deposit = BigDecimal.ZERO; // 押金 - 未来扩展
        BigDecimal checkoutFee = BigDecimal.ZERO; // 退房金 - 未来扩展
        BigDecimal roomServiceFee = BigDecimal.ZERO; // 餐食/客房消费 - 未来扩展

        overview.setRoomFee(roomFee);
        overview.setDeposit(deposit);
        overview.setCheckoutFee(checkoutFee);
        overview.setRoomServiceFee(roomServiceFee);
        overview.setTotalRevenue(roomFee.add(deposit).add(checkoutFee).add(roomServiceFee));
        overview.setRevenuePrecision(allocationResult.revenuePrecision());

        // 计算消费分类分布(饼图数据)
        List<BusinessOverviewDTO.CategoryDistribution> categoryDistribution = new ArrayList<>();
        BigDecimal totalRevenue = overview.getTotalRevenue();

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            categoryDistribution.add(new BusinessOverviewDTO.CategoryDistribution(
                "房费", roomFee,
                roomFee.multiply(new BigDecimal(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
            ));

            if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                categoryDistribution.add(new BusinessOverviewDTO.CategoryDistribution(
                    "押金", deposit,
                    deposit.multiply(new BigDecimal(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                ));
            }

            if (checkoutFee.compareTo(BigDecimal.ZERO) > 0) {
                categoryDistribution.add(new BusinessOverviewDTO.CategoryDistribution(
                    "退房金", checkoutFee,
                    checkoutFee.multiply(new BigDecimal(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                ));
            }

            if (roomServiceFee.compareTo(BigDecimal.ZERO) > 0) {
                categoryDistribution.add(new BusinessOverviewDTO.CategoryDistribution(
                    "餐食/客房消费", roomServiceFee,
                    roomServiceFee.multiply(new BigDecimal(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                ));
            }
        }
        overview.setCategoryDistribution(categoryDistribution);

        // 计算每日消费趋势(柱状图数据)
        List<BusinessOverviewDTO.DailyConsumption> consumptionTrend = new ArrayList<>();
        Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(allocations, startDate, endDate);
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
            BigDecimal dailyRoomFee = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;

            consumptionTrend.add(new BusinessOverviewDTO.DailyConsumption(
                currentDate.toString(),
                dailyRoomFee,
                BigDecimal.ZERO, // 押金
                BigDecimal.ZERO, // 退房金
                BigDecimal.ZERO  // 餐食/客房消费
            ));

            currentDate = currentDate.plusDays(1);
        }
        overview.setConsumptionTrend(consumptionTrend);

        // 计算住宿消费明细表格数据
        List<BusinessOverviewDTO.ConsumptionDetail> consumptionDetails = new ArrayList<>();

        // 房费明细
        List<BusinessOverviewDTO.ConsumptionDetail.DailyAmount> roomFeeDailyAmounts = new ArrayList<>();
        currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
            BigDecimal dailyAmount = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
            roomFeeDailyAmounts.add(new BusinessOverviewDTO.ConsumptionDetail.DailyAmount(
                currentDate.toString(), dailyAmount
            ));
            currentDate = currentDate.plusDays(1);
        }
        consumptionDetails.add(new BusinessOverviewDTO.ConsumptionDetail(
            "房费", roomFee, roomFeeDailyAmounts
        ));

        // 押金明细
        List<BusinessOverviewDTO.ConsumptionDetail.DailyAmount> depositDailyAmounts = new ArrayList<>();
        currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            depositDailyAmounts.add(new BusinessOverviewDTO.ConsumptionDetail.DailyAmount(
                currentDate.toString(), BigDecimal.ZERO
            ));
            currentDate = currentDate.plusDays(1);
        }
        consumptionDetails.add(new BusinessOverviewDTO.ConsumptionDetail(
            "押金", deposit, depositDailyAmounts
        ));

        // 退房金明细
        List<BusinessOverviewDTO.ConsumptionDetail.DailyAmount> checkoutDailyAmounts = new ArrayList<>();
        currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            checkoutDailyAmounts.add(new BusinessOverviewDTO.ConsumptionDetail.DailyAmount(
                currentDate.toString(), BigDecimal.ZERO
            ));
            currentDate = currentDate.plusDays(1);
        }
        consumptionDetails.add(new BusinessOverviewDTO.ConsumptionDetail(
            "退房金", checkoutFee, checkoutDailyAmounts
        ));

        // 餐食/客房消费明细
        List<BusinessOverviewDTO.ConsumptionDetail.DailyAmount> roomServiceDailyAmounts = new ArrayList<>();
        currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            roomServiceDailyAmounts.add(new BusinessOverviewDTO.ConsumptionDetail.DailyAmount(
                currentDate.toString(), BigDecimal.ZERO
            ));
            currentDate = currentDate.plusDays(1);
        }
        consumptionDetails.add(new BusinessOverviewDTO.ConsumptionDetail(
            "餐食/客房消费", roomServiceFee, roomServiceDailyAmounts
        ));

        overview.setConsumptionDetails(consumptionDetails);

        return overview;
    }

    /**
     * 获取流水汇总统计(门店级)
     * 包括支付方式和款项分类两种维度的统计
     */
    public RevenueSummaryDTO getRevenueSummary(LocalDate startDate, LocalDate endDate) {
        Long storeId = getCurrentStoreId();
        RevenueSummaryDTO summary = new RevenueSummaryDTO();

        List<Reservation> reservations = findActiveReservations(storeId, startDate, endDate);
        ReservationRevenueAllocationService.AllocationResult allocationResult =
                revenueAllocationService.allocateRevenue(storeId, reservations, startDate, endDate);
        List<ReservationRevenueAllocationService.Allocation> allocations = allocationResult.allocations();

        BigDecimal totalRevenue = allocationResult.totalRevenue();

        summary.setTotalRevenue(totalRevenue);
        summary.setRevenuePrecision(allocationResult.revenuePrecision());

        Map<String, RevenueAggregation> channelMap = aggregateByChannel(allocations);

        BigDecimal splitAccount = BigDecimal.ZERO; // OTA代收
        BigDecimal actualReceived = BigDecimal.ZERO; // 实收款
        List<RevenueSummaryDTO.PaymentMethodStat> paymentStats = new ArrayList<>();
        List<RevenueSummaryDTO.Distribution> incomeDistribution = new ArrayList<>();

        for (Map.Entry<String, RevenueAggregation> entry : channelMap.entrySet()) {
            String channelName = entry.getKey();
            BigDecimal channelRevenue = entry.getValue().revenue;

            // 简单判断:包含 booking、airbnb、携程、美团 等关键词的视为OTA代收
            if (channelName.toLowerCase().contains("booking") ||
                channelName.toLowerCase().contains("airbnb") ||
                channelName.toLowerCase().contains("携程") ||
                channelName.toLowerCase().contains("美团")) {
                splitAccount = splitAccount.add(channelRevenue);
            } else {
                actualReceived = actualReceived.add(channelRevenue);
            }

            BigDecimal percentage = BigDecimal.ZERO;
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                percentage = channelRevenue.multiply(new BigDecimal(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP);
            }

            paymentStats.add(new RevenueSummaryDTO.PaymentMethodStat(
                    channelName, channelRevenue, percentage
            ));

            incomeDistribution.add(new RevenueSummaryDTO.Distribution(
                    channelName, channelRevenue, percentage
            ));
        }

        summary.setSplitAccount(splitAccount);
        summary.setActualReceived(actualReceived);
        summary.setTotalIncome(totalRevenue);
        summary.setTotalExpense(BigDecimal.ZERO); // 目前系统无支出记录
        summary.setPaymentMethodStats(paymentStats);
        summary.setIncomeDistribution(incomeDistribution);
        summary.setExpenseDistribution(new ArrayList<>()); // 暂无支出

        // 款项分类统计 - 目前只有常规流水
        List<RevenueSummaryDTO.CategoryStat> categoryStats = new ArrayList<>();
        categoryStats.add(new RevenueSummaryDTO.CategoryStat(
                "常规流水", totalRevenue, new BigDecimal(100)
        ));
        summary.setCategoryStats(categoryStats);

        // 每日流水明细
        List<RevenueSummaryDTO.DailyRevenue> dailyRevenues = new ArrayList<>();
        Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(allocations, startDate, endDate);
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
            BigDecimal dailyAmount = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
            int orderCount = aggregation != null ? aggregation.orderIds.size() : 0;

            dailyRevenues.add(new RevenueSummaryDTO.DailyRevenue(
                    currentDate.toString(), dailyAmount, orderCount
            ));
            currentDate = currentDate.plusDays(1);
        }
        summary.setDailyRevenues(dailyRevenues);

        return summary;
    }

    /**
     * 获取渠道汇总统计(门店级)
     */
    public ChannelSummaryDTO getChannelSummary(LocalDate startDate, LocalDate endDate) {
        Long storeId = getCurrentStoreId();
        ChannelSummaryDTO summary = new ChannelSummaryDTO();

        List<Reservation> reservations = findActiveReservations(storeId, startDate, endDate);
        ReservationRevenueAllocationService.AllocationResult allocationResult =
                revenueAllocationService.allocateRevenue(storeId, reservations, startDate, endDate);
        List<ReservationRevenueAllocationService.Allocation> allocations = allocationResult.allocations();

        Map<String, List<ReservationRevenueAllocationService.Allocation>> channelAllocations = new HashMap<>();
        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            String channelName = getChannelName(allocation.reservation());
            channelAllocations.computeIfAbsent(channelName, ignored -> new ArrayList<>()).add(allocation);
        }

        // 计算总消费和总间夜
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalRoomNights = 0;

        List<ChannelSummaryDTO.ChannelDistribution> revenueDistribution = new ArrayList<>();
        List<ChannelSummaryDTO.ChannelDistribution> nightsDistribution = new ArrayList<>();
        List<ChannelSummaryDTO.ChannelDetail> channelDetails = new ArrayList<>();

        for (Map.Entry<String, List<ReservationRevenueAllocationService.Allocation>> entry : channelAllocations.entrySet()) {
            String channelName = entry.getKey();
            List<ReservationRevenueAllocationService.Allocation> channelItems = entry.getValue();

            BigDecimal channelRevenue = BigDecimal.ZERO;
            for (ReservationRevenueAllocationService.Allocation allocation : channelItems) {
                channelRevenue = channelRevenue.add(allocation.amount());
            }
            int channelNights = channelItems.size();

            totalRevenue = totalRevenue.add(channelRevenue);
            totalRoomNights += channelNights;

            // 渠道明细每日数据
            List<ChannelSummaryDTO.ChannelDetail.DailyValue> dailyValues = new ArrayList<>();
            Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(channelItems, startDate, endDate);
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
                BigDecimal dailyRevenue = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
                int dailyNights = aggregation != null ? aggregation.roomNights : 0;

                dailyValues.add(new ChannelSummaryDTO.ChannelDetail.DailyValue(
                        currentDate.toString(), dailyRevenue, dailyNights
                ));
                currentDate = currentDate.plusDays(1);
            }

            channelDetails.add(new ChannelSummaryDTO.ChannelDetail(
                    channelName, channelRevenue, channelNights, dailyValues
            ));
        }

        summary.setTotalRevenue(totalRevenue);
        summary.setTotalRoomNights(totalRoomNights);
        summary.setRevenuePrecision(allocationResult.revenuePrecision());

        // 计算百分比并生成分布数据
        for (ChannelSummaryDTO.ChannelDetail detail : channelDetails) {
            BigDecimal revenuePercentage = BigDecimal.ZERO;
            BigDecimal nightsPercentage = BigDecimal.ZERO;

            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                revenuePercentage = detail.getTotalRevenue().multiply(new BigDecimal(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP);
            }

            if (totalRoomNights > 0) {
                nightsPercentage = new BigDecimal(detail.getTotalRoomNights()).multiply(new BigDecimal(100))
                        .divide(new BigDecimal(totalRoomNights), 2, RoundingMode.HALF_UP);
            }

            revenueDistribution.add(new ChannelSummaryDTO.ChannelDistribution(
                    detail.getChannelName(), detail.getTotalRevenue(), revenuePercentage
            ));

            nightsDistribution.add(new ChannelSummaryDTO.ChannelDistribution(
                    detail.getChannelName(), new BigDecimal(detail.getTotalRoomNights()), nightsPercentage
            ));
        }

        summary.setRevenueDistribution(revenueDistribution);
        summary.setNightsDistribution(nightsDistribution);
        summary.setChannelDetails(channelDetails);

        // 趋势数据 - 按日期统计各渠道
        List<ChannelSummaryDTO.ChannelTrend> revenueTrend = new ArrayList<>();
        List<ChannelSummaryDTO.ChannelTrend> nightsTrend = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate date = currentDate;
            List<ChannelSummaryDTO.ChannelTrend.ChannelValue> revenueValues = new ArrayList<>();
            List<ChannelSummaryDTO.ChannelTrend.ChannelValue> nightsValues = new ArrayList<>();

            for (String channelName : channelAllocations.keySet()) {
                List<ReservationRevenueAllocationService.Allocation> channelItems =
                        channelAllocations.get(channelName);
                Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(channelItems, date, date);
                RevenueAggregation aggregation = dailyRevenueMap.get(date);

                BigDecimal dailyRevenue = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
                long dailyNights = aggregation != null ? aggregation.roomNights : 0;

                revenueValues.add(new ChannelSummaryDTO.ChannelTrend.ChannelValue(
                        channelName, dailyRevenue
                ));

                nightsValues.add(new ChannelSummaryDTO.ChannelTrend.ChannelValue(
                        channelName, new BigDecimal(dailyNights)
                ));
            }

            revenueTrend.add(new ChannelSummaryDTO.ChannelTrend(date.toString(), revenueValues));
            nightsTrend.add(new ChannelSummaryDTO.ChannelTrend(date.toString(), nightsValues));

            currentDate = currentDate.plusDays(1);
        }

        summary.setRevenueTrend(revenueTrend);
        summary.setNightsTrend(nightsTrend);

        return summary;
    }

    /**
     * 获取销售汇总统计(门店级)
     */
    public SalesSummaryDTO getSalesSummary(LocalDate startDate, LocalDate endDate, String keyword, Long channelId) {
        Long storeId = getCurrentStoreId();
        SalesSummaryDTO summary = new SalesSummaryDTO();

        List<Reservation> reservations = findActiveReservations(storeId, startDate, endDate);

        // 应用搜索过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            final String searchKeyword = keyword.trim().toLowerCase();
            reservations = reservations.stream()
                    .filter(r ->
                        (r.getGuestName() != null && r.getGuestName().toLowerCase().contains(searchKeyword)) ||
                        (r.getGuestPhone() != null && r.getGuestPhone().contains(searchKeyword)) ||
                        (r.getId() != null && r.getId().toString().contains(searchKeyword))
                    )
                    .collect(Collectors.toList());
        }

        // 应用渠道过滤
        if (channelId != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getChannel() != null && channelId.equals(r.getChannel().getId()))
                    .collect(Collectors.toList());
        }

        ReservationRevenueAllocationService.AllocationResult allocationResult =
                revenueAllocationService.allocateRevenue(storeId, reservations, startDate, endDate);
        List<ReservationRevenueAllocationService.Allocation> allocations = allocationResult.allocations();
        BigDecimal totalSales = allocationResult.totalRevenue();

        summary.setTotalSales(totalSales);
        summary.setTotalOrders(reservations.size());
        summary.setRevenuePrecision(allocationResult.revenuePrecision());

        // 每日销售额趋势
        List<SalesSummaryDTO.DailySales> dailySalesTrend = new ArrayList<>();
        Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(allocations, startDate, endDate);
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
            BigDecimal dailySales = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
            int orderCount = aggregation != null ? aggregation.orderIds.size() : 0;

            dailySalesTrend.add(new SalesSummaryDTO.DailySales(
                    currentDate.toString(), dailySales, orderCount
            ));
            currentDate = currentDate.plusDays(1);
        }
        summary.setDailySalesTrend(dailySalesTrend);

        // 销售订单明细
        List<SalesSummaryDTO.SalesOrderDetail> orderDetails = reservations.stream()
                .map(r -> {
                    SalesSummaryDTO.SalesOrderDetail detail = new SalesSummaryDTO.SalesOrderDetail();
                    detail.setId(r.getId());
                    detail.setOrderNumber(r.getId() != null ? r.getId().toString() : "");
                    detail.setChannelNumber(r.getChannelOrderNumber() != null ? r.getChannelOrderNumber() : "");
                    detail.setCreatedAt(r.getCreatedAt());
                    detail.setGuestName(r.getGuestName() != null ? r.getGuestName() : "系统");
                    detail.setChannelName(r.getChannel() != null ? r.getChannel().getName() : "");
                    detail.setCustomerName(r.getGuestName() != null ? r.getGuestName() : "");
                    detail.setPhone(r.getGuestPhone() != null ? r.getGuestPhone() : "");
                    detail.setAmount(r.getTotalAmount());
                    detail.setRoomTypeName(r.getRoom() != null && r.getRoom().getRoomType() != null ?
                            r.getRoom().getRoomType().getName() : "");
                    detail.setCheckInDate(r.getCheckInDate() != null ? r.getCheckInDate().toString() : "");
                    detail.setCheckOutDate(r.getCheckOutDate() != null ? r.getCheckOutDate().toString() : "");
                    return detail;
                })
                .collect(Collectors.toList());

        summary.setOrderDetails(orderDetails);

        return summary;
    }

    /**
     * 获取经营指标统计(门店级)
     * 包含 ADR、Occ、RevPAR 等核心指标,以及趋势和明细数据
     */
    public OperationalMetricsDTO getOperationalMetrics(LocalDate startDate, LocalDate endDate) {
        Long storeId = getCurrentStoreId();

        List<Reservation> reservations = findActiveReservations(storeId, startDate, endDate);
        ReservationRevenueAllocationService.AllocationResult allocationResult =
                revenueAllocationService.allocateRevenue(storeId, reservations, startDate, endDate);
        List<ReservationRevenueAllocationService.Allocation> allocations = allocationResult.allocations();

        BigDecimal totalRoomFee = allocationResult.totalRevenue();

        int totalSoldRoomNights = allocationResult.totalRoomNights();

        // 计算可售房间总数
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long totalRooms = roomRepository.countByStoreId(storeId);
        int totalAvailableRoomNights = (int) (days * totalRooms);

        // 计算 ADR (平均房价) = 总房费 / 已售间夜数
        BigDecimal adr = BigDecimal.ZERO;
        if (totalSoldRoomNights > 0) {
            adr = totalRoomFee.divide(new BigDecimal(totalSoldRoomNights), 2, RoundingMode.HALF_UP);
        }

        // 计算 Occ (入住率) = (已售间夜数 / 可售房间总数) * 100%
        BigDecimal occ = BigDecimal.ZERO;
        if (totalAvailableRoomNights > 0) {
            occ = new BigDecimal(totalSoldRoomNights)
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(totalAvailableRoomNights), 2, RoundingMode.HALF_UP);
        }

        // 计算 RevPAR (每房收益) = 总房费 / 可售房间总数  或  ADR * Occ
        BigDecimal revpar = BigDecimal.ZERO;
        if (totalAvailableRoomNights > 0) {
            revpar = totalRoomFee.divide(new BigDecimal(totalAvailableRoomNights), 2, RoundingMode.HALF_UP);
        }

        // 创建基础DTO
        OperationalMetricsDTO dto = new OperationalMetricsDTO(
                totalRoomFee,
                adr,
                occ,
                revpar,
                totalSoldRoomNights,
                totalAvailableRoomNights,
                (int) totalRooms,
                (int) days
        );
        dto.setRevenuePrecision(allocationResult.revenuePrecision());

        // 1. 计算每日趋势数据
        List<DailyMetricsDTO> dailyTrends = calculateDailyTrends(
                allocations, startDate, endDate, totalRooms);
        dto.setDailyTrends(dailyTrends);

        // 2. 计算房费明细
        List<RoomDetailDTO> roomFeeDetails = calculateRoomFeeDetails(
                allocations, startDate, endDate);
        dto.setRoomFeeDetails(roomFeeDetails);

        // 3. 计算间夜明细
        List<RoomDetailDTO> roomNightsDetails = calculateRoomNightsDetails(
                reservations, startDate, endDate);
        dto.setRoomNightsDetails(roomNightsDetails);

        // 4. 计算入住率明细
        List<RoomDetailDTO> occupancyDetails = calculateOccupancyDetails(
                reservations, startDate, endDate, totalRooms, days);
        dto.setOccupancyDetails(occupancyDetails);

        // 5. 计算RevPAR明细
        List<RoomDetailDTO> revparDetails = calculateRevparDetails(
                allocations, totalRooms, days);
        dto.setRevparDetails(revparDetails);

        return dto;
    }

    /**
     * 计算每日趋势数据
     */
    private List<DailyMetricsDTO> calculateDailyTrends(
            List<ReservationRevenueAllocationService.Allocation> allocations,
            LocalDate startDate,
            LocalDate endDate,
            long totalRooms) {

        List<DailyMetricsDTO> dailyTrends = new ArrayList<>();
        Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(allocations, startDate, endDate);
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
            BigDecimal dailyRoomFee = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
            int dailyRoomNights = aggregation != null ? aggregation.roomNights : 0;

            // 当天ADR
            BigDecimal dailyAdr = BigDecimal.ZERO;
            if (dailyRoomNights > 0) {
                dailyAdr = dailyRoomFee.divide(new BigDecimal(dailyRoomNights), 2, RoundingMode.HALF_UP);
            }

            // 当天入住率
            BigDecimal dailyOcc = BigDecimal.ZERO;
            if (totalRooms > 0) {
                dailyOcc = new BigDecimal(dailyRoomNights)
                        .multiply(new BigDecimal(100))
                        .divide(new BigDecimal(totalRooms), 2, RoundingMode.HALF_UP);
            }

            // 当天RevPAR
            BigDecimal dailyRevpar = BigDecimal.ZERO;
            if (totalRooms > 0) {
                dailyRevpar = dailyRoomFee.divide(new BigDecimal(totalRooms), 2, RoundingMode.HALF_UP);
            }

            dailyTrends.add(new DailyMetricsDTO(
                    currentDate.toString(),
                    dailyRoomFee,
                    dailyAdr,
                    dailyRevpar,
                    dailyRoomNights,
                    dailyOcc
            ));

            currentDate = currentDate.plusDays(1);
        }

        return dailyTrends;
    }

    /**
     * 计算房费明细(按房型和房间分组)
     */
    private List<RoomDetailDTO> calculateRoomFeeDetails(
            List<ReservationRevenueAllocationService.Allocation> allocations,
            LocalDate startDate,
            LocalDate endDate) {

        List<RoomDetailDTO> details = new ArrayList<>();

        // 按房型和房间号分组
        Map<String, Map<String, RevenueAggregation>> roomTypeMap = new HashMap<>();

        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            Reservation r = allocation.reservation();
            String roomType = getRoomTypeName(r);
            String roomNumber = getRoomNumber(r);

            roomTypeMap.computeIfAbsent(roomType, k -> new HashMap<>())
                    .computeIfAbsent(roomNumber, k -> new RevenueAggregation())
                    .add(allocation);
        }

        // 计算每个房间的费用
        for (Map.Entry<String, Map<String, RevenueAggregation>> typeEntry : roomTypeMap.entrySet()) {
            String roomType = typeEntry.getKey();
            BigDecimal roomTypeTotal = BigDecimal.ZERO;
            BigDecimal roomTypeLastDay = BigDecimal.ZERO;

            for (Map.Entry<String, RevenueAggregation> roomEntry : typeEntry.getValue().entrySet()) {
                String roomNumber = roomEntry.getKey();
                RevenueAggregation roomAggregation = roomEntry.getValue();
                BigDecimal total = roomAggregation.revenue;
                BigDecimal lastDayAmount = BigDecimal.ZERO;
                for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
                    if (!allocation.date().equals(endDate)) {
                        continue;
                    }
                    if (!roomType.equals(getRoomTypeName(allocation.reservation()))) {
                        continue;
                    }
                    if (!roomNumber.equals(getRoomNumber(allocation.reservation()))) {
                        continue;
                    }
                    lastDayAmount = lastDayAmount.add(allocation.amount());
                }

                details.add(new RoomDetailDTO(roomType, roomNumber, total, lastDayAmount));

                roomTypeTotal = roomTypeTotal.add(total);
                roomTypeLastDay = roomTypeLastDay.add(lastDayAmount);
            }

            // 添加房型小计
            details.add(new RoomDetailDTO(roomType, "小计", roomTypeTotal, roomTypeLastDay));
        }

        return details;
    }

    /**
     * 计算间夜明细(按房型和房间分组)
     */
    private List<RoomDetailDTO> calculateRoomNightsDetails(
            List<Reservation> reservations, LocalDate startDate, LocalDate endDate) {

        List<RoomDetailDTO> details = new ArrayList<>();

        // 按房型和房间号分组
        Map<String, Map<String, List<Reservation>>> roomTypeMap = new HashMap<>();

        for (Reservation r : reservations) {
            String roomType = r.getRoom() != null && r.getRoom().getRoomType() != null
                    ? r.getRoom().getRoomType().getName() : "未知房型";
            String roomNumber = r.getRoom() != null && r.getRoom().getRoomNumber() != null
                    ? r.getRoom().getRoomNumber() : "未排房";

            roomTypeMap.computeIfAbsent(roomType, k -> new HashMap<>())
                    .computeIfAbsent(roomNumber, k -> new ArrayList<>())
                    .add(r);
        }

        // 计算每个房间的间夜数
        for (Map.Entry<String, Map<String, List<Reservation>>> typeEntry : roomTypeMap.entrySet()) {
            String roomType = typeEntry.getKey();
            int roomTypeTotal = 0;
            int roomTypeLastDay = 0;

            for (Map.Entry<String, List<Reservation>> roomEntry : typeEntry.getValue().entrySet()) {
                String roomNumber = roomEntry.getKey();
                List<Reservation> roomReservations = roomEntry.getValue();

                // 计算总间夜数
                int total = roomReservations.stream()
                        .mapToInt(r -> calculateRoomNights(r, startDate, endDate))
                        .sum();

                // 计算最后一天的间夜数(0或1)
                int lastDayNights = (int) roomReservations.stream()
                        .filter(r -> isReservationActiveOnDate(r, endDate))
                        .count();

                details.add(new RoomDetailDTO(
                        roomType,
                        roomNumber,
                        new BigDecimal(total),
                        new BigDecimal(lastDayNights)
                ));

                roomTypeTotal += total;
                roomTypeLastDay += lastDayNights;
            }

            // 添加房型小计
            details.add(new RoomDetailDTO(
                    roomType,
                    "小计",
                    new BigDecimal(roomTypeTotal),
                    new BigDecimal(roomTypeLastDay)
            ));
        }

        return details;
    }

    /**
     * 计算入住率明细(按房型分组)
     */
    private List<RoomDetailDTO> calculateOccupancyDetails(
            List<Reservation> reservations, LocalDate startDate, LocalDate endDate,
            long totalRooms, long days) {

        List<RoomDetailDTO> details = new ArrayList<>();

        // 按房型分组
        Map<String, List<Reservation>> roomTypeMap = reservations.stream()
                .collect(Collectors.groupingBy(r ->
                        r.getRoom() != null && r.getRoom().getRoomType() != null
                                ? r.getRoom().getRoomType().getName() : "未知房型"
                ));

        for (Map.Entry<String, List<Reservation>> entry : roomTypeMap.entrySet()) {
            String roomType = entry.getKey();
            List<Reservation> typeReservations = entry.getValue();

            // 计算该房型的总间夜数
            int totalNights = typeReservations.stream()
                    .mapToInt(r -> calculateRoomNights(r, startDate, endDate))
                    .sum();

            // 获取该房型的房间总数(简化处理,使用已售的不同房间数)
            long roomCount = typeReservations.stream()
                    .filter(r -> r.getRoom() != null)
                    .map(r -> r.getRoom().getId())
                    .distinct()
                    .count();

            if (roomCount == 0) {
                roomCount = 1; // 避免除零
            }

            long availableNights = roomCount * days;

            // 计算入住率
            BigDecimal occupancy = BigDecimal.ZERO;
            if (availableNights > 0) {
                occupancy = new BigDecimal(totalNights)
                        .multiply(new BigDecimal(100))
                        .divide(new BigDecimal(availableNights), 2, RoundingMode.HALF_UP);
            }

            // 计算最后一天的入住率
            int lastDayNights = (int) typeReservations.stream()
                    .filter(r -> isReservationActiveOnDate(r, endDate))
                    .count();

            BigDecimal lastDayOccupancy = BigDecimal.ZERO;
            if (roomCount > 0) {
                lastDayOccupancy = new BigDecimal(lastDayNights)
                        .multiply(new BigDecimal(100))
                        .divide(new BigDecimal(roomCount), 2, RoundingMode.HALF_UP);
            }

            details.add(new RoomDetailDTO(roomType, roomType, occupancy, lastDayOccupancy));
        }

        return details;
    }

    /**
     * 计算RevPAR明细(按房型分组)
     */
    private List<RoomDetailDTO> calculateRevparDetails(
            List<ReservationRevenueAllocationService.Allocation> allocations,
            long totalRooms, long days) {

        List<RoomDetailDTO> details = new ArrayList<>();

        // 按房型分组
        Map<String, RevenueAggregation> roomTypeMap = new HashMap<>();
        Map<String, Set<Long>> roomIdsByType = new HashMap<>();
        Map<String, BigDecimal> lastDayFeeByType = new HashMap<>();

        LocalDate lastDate = null;
        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            if (lastDate == null || allocation.date().isAfter(lastDate)) {
                lastDate = allocation.date();
            }
        }

        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            String roomType = getRoomTypeName(allocation.reservation());
            roomTypeMap.computeIfAbsent(roomType, ignored -> new RevenueAggregation()).add(allocation);
            Long roomId = allocation.reservation().getRoom() != null ? allocation.reservation().getRoom().getId() : null;
            if (roomId != null) {
                roomIdsByType.computeIfAbsent(roomType, ignored -> new LinkedHashSet<>()).add(roomId);
            }
            if (lastDate != null && allocation.date().equals(lastDate)) {
                BigDecimal current = lastDayFeeByType.getOrDefault(roomType, BigDecimal.ZERO);
                lastDayFeeByType.put(roomType, current.add(allocation.amount()));
            }
        }

        for (Map.Entry<String, RevenueAggregation> entry : roomTypeMap.entrySet()) {
            String roomType = entry.getKey();
            RevenueAggregation aggregation = entry.getValue();
            BigDecimal totalFee = aggregation.revenue;

            // 获取该房型的房间总数
            long roomCount = roomIdsByType.getOrDefault(roomType, Set.of()).size();

            if (roomCount == 0) {
                roomCount = 1; // 避免除零
            }

            long availableNights = roomCount * days;

            // 计算RevPAR = 总房费 / 可售房间总数
            BigDecimal revpar = BigDecimal.ZERO;
            if (availableNights > 0) {
                revpar = totalFee.divide(new BigDecimal(availableNights), 2, RoundingMode.HALF_UP);
            }

            BigDecimal lastDayFee = lastDayFeeByType.getOrDefault(roomType, BigDecimal.ZERO);

            BigDecimal lastDayRevpar = BigDecimal.ZERO;
            if (roomCount > 0) {
                lastDayRevpar = lastDayFee.divide(new BigDecimal(roomCount), 2, RoundingMode.HALF_UP);
            }

            details.add(new RoomDetailDTO(roomType, roomType, revpar, lastDayRevpar));
        }

        return details;
    }

    private List<Reservation> findActiveReservations(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<Reservation> result = new ArrayList<>();
        List<Reservation> reservations = reservationRepository.findByStoreId(storeId);
        for (Reservation reservation : reservations) {
            if (reservation == null || reservation.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }
            if (reservation.getCheckInDate() == null || reservation.getCheckOutDate() == null) {
                continue;
            }
            if (isDateInRange(reservation.getCheckInDate(), reservation.getCheckOutDate(), startDate, endDate)) {
                result.add(reservation);
            }
        }
        return result;
    }

    private Map<String, RevenueAggregation> aggregateByChannel(
            List<ReservationRevenueAllocationService.Allocation> allocations) {
        Map<String, RevenueAggregation> result = new HashMap<>();
        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            String channelName = getChannelName(allocation.reservation());
            result.computeIfAbsent(channelName, ignored -> new RevenueAggregation()).add(allocation);
        }
        return result;
    }

    private Map<LocalDate, RevenueAggregation> aggregateByDate(
            List<ReservationRevenueAllocationService.Allocation> allocations,
            LocalDate startDate,
            LocalDate endDate) {
        Map<LocalDate, RevenueAggregation> result = new HashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            result.put(currentDate, new RevenueAggregation());
            currentDate = currentDate.plusDays(1);
        }

        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            RevenueAggregation aggregation = result.get(allocation.date());
            if (aggregation != null) {
                aggregation.add(allocation);
            }
        }
        return result;
    }

    private String getChannelName(Reservation reservation) {
        if (reservation != null && reservation.getChannel() != null && reservation.getChannel().getName() != null) {
            return reservation.getChannel().getName();
        }
        return "未知渠道";
    }

    private String getRoomTypeName(Reservation reservation) {
        if (reservation != null
                && reservation.getRoom() != null
                && reservation.getRoom().getRoomType() != null
                && reservation.getRoom().getRoomType().getName() != null) {
            return reservation.getRoom().getRoomType().getName();
        }
        return "未知房型";
    }

    private String getRoomNumber(Reservation reservation) {
        if (reservation != null && reservation.getRoom() != null && reservation.getRoom().getRoomNumber() != null) {
            return reservation.getRoom().getRoomNumber();
        }
        return "未排房";
    }

    private static class RevenueAggregation {
        private BigDecimal revenue = BigDecimal.ZERO;
        private int roomNights = 0;
        private final Set<Long> orderIds = new LinkedHashSet<>();

        void add(ReservationRevenueAllocationService.Allocation allocation) {
            if (allocation == null) {
                return;
            }
            revenue = revenue.add(allocation.amount() != null ? allocation.amount() : BigDecimal.ZERO);
            roomNights++;
            if (allocation.reservation() != null && allocation.reservation().getId() != null) {
                orderIds.add(allocation.reservation().getId());
            }
        }
    }

    /**
     * 判断订单在指定日期是否在住
     */
    private boolean isReservationActiveOnDate(Reservation r, LocalDate date) {
        return !r.getCheckInDate().isAfter(date) && r.getCheckOutDate().isAfter(date);
    }

    /**
     * 计算订单在指定日期的日均费用
     */
    private BigDecimal calculateDailyAmount(Reservation r, LocalDate date) {
        if (!isReservationActiveOnDate(r, date)) {
            return BigDecimal.ZERO;
        }

        // 计算总天数
        long totalDays = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
        if (totalDays <= 0) {
            totalDays = 1;
        }

        // 按天均摊费用
        return r.getTotalAmount().divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP);
    }
}
