package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.*;
import server.demo.entity.Room;
import server.demo.entity.Reservation;
import server.demo.enums.ChannelType;
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

    private static final int DEFAULT_SALES_PAGE = 1;
    private static final int DEFAULT_SALES_PAGE_SIZE = 50;
    private static final int MAX_SALES_PAGE_SIZE = 500;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRevenueAllocationService revenueAllocationService;

    @Autowired
    private StatisticsFinancialAggregationService financialAggregationService;

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
        StatisticsFinancialAggregationService.FinancialAggregation financial =
                financialAggregationService.aggregate(storeId, startDate, endDate);

        BigDecimal roomFee = allocationResult.totalRevenue();
        BigDecimal deposit = financial.getDeposit();
        BigDecimal checkoutFee = BigDecimal.ZERO;
        BigDecimal roomServiceFee = financial.getRoomServiceFee();
        BigDecimal notesIncome = financial.getNotesIncome();
        BigDecimal notesExpense = financial.getNotesExpense().add(financial.getPaymentRefund());
        BigDecimal totalIncome = roomFee.add(deposit).add(checkoutFee).add(roomServiceFee).add(notesIncome);

        overview.setRoomFee(roomFee);
        overview.setDeposit(deposit);
        overview.setCheckoutFee(checkoutFee);
        overview.setRoomServiceFee(roomServiceFee);
        overview.setNotesIncome(notesIncome);
        overview.setNotesExpense(notesExpense);
        overview.setTotalRevenue(totalIncome);
        overview.setNetRevenue(totalIncome.subtract(notesExpense));
        overview.setRevenuePrecision(allocationResult.revenuePrecision());
        overview.setSourceMetadata(buildSourceMetadata());
        overview.setDataGaps(buildDataGaps());

        // 计算消费分类分布(饼图数据)
        List<BusinessOverviewDTO.CategoryDistribution> categoryDistribution = new ArrayList<>();
        addOverviewDistribution(categoryDistribution, "房费", roomFee, totalIncome);
        addOverviewDistribution(categoryDistribution, "押金", deposit, totalIncome);
        addOverviewDistribution(categoryDistribution, "退房金", checkoutFee, totalIncome);
        addOverviewDistribution(categoryDistribution, "餐食/客房消费", roomServiceFee, totalIncome);
        addOverviewDistribution(categoryDistribution, "记一笔收入", notesIncome, totalIncome);
        overview.setCategoryDistribution(categoryDistribution);

        // 计算每日消费趋势(柱状图数据)
        List<BusinessOverviewDTO.DailyConsumption> consumptionTrend = new ArrayList<>();
        Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(allocations, startDate, endDate);
        Map<LocalDate, BigDecimal> roomFeeByDate = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> depositByDate = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> checkoutFeeByDate = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> roomServiceFeeByDate = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> notesIncomeByDate = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> notesExpenseByDate = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
            BigDecimal dailyRoomFee = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
            StatisticsFinancialAggregationService.DailyFinancialAmount dailyFinancial =
                    financial.getDailyAmountMap().get(currentDate);
            BigDecimal dailyDeposit = dailyFinancial != null ? dailyFinancial.getDeposit() : BigDecimal.ZERO;
            BigDecimal dailyRoomServiceFee =
                    dailyFinancial != null ? dailyFinancial.getRoomServiceFee() : BigDecimal.ZERO;
            BigDecimal dailyNotesIncome =
                    dailyFinancial != null ? dailyFinancial.getNotesIncome() : BigDecimal.ZERO;
            BigDecimal dailyNotesExpense = BigDecimal.ZERO;
            if (dailyFinancial != null) {
                dailyNotesExpense = dailyFinancial.getNotesExpense().add(dailyFinancial.getPaymentRefund());
            }

            BusinessOverviewDTO.DailyConsumption dailyConsumption = new BusinessOverviewDTO.DailyConsumption(
                currentDate.toString(),
                dailyRoomFee,
                dailyDeposit,
                BigDecimal.ZERO,
                dailyRoomServiceFee
            );
            dailyConsumption.setNotesIncome(dailyNotesIncome);
            dailyConsumption.setNotesExpense(dailyNotesExpense);
            consumptionTrend.add(dailyConsumption);

            roomFeeByDate.put(currentDate, dailyRoomFee);
            depositByDate.put(currentDate, dailyDeposit);
            checkoutFeeByDate.put(currentDate, BigDecimal.ZERO);
            roomServiceFeeByDate.put(currentDate, dailyRoomServiceFee);
            notesIncomeByDate.put(currentDate, dailyNotesIncome);
            notesExpenseByDate.put(currentDate, dailyNotesExpense);

            currentDate = currentDate.plusDays(1);
        }
        overview.setConsumptionTrend(consumptionTrend);

        // 计算住宿消费明细表格数据
        List<BusinessOverviewDTO.ConsumptionDetail> consumptionDetails = new ArrayList<>();
        consumptionDetails.add(buildOverviewDetail(
                "房费",
                roomFee,
                roomFeeByDate,
                startDate,
                endDate,
                "RESERVATION_DAILY_PRICE_OR_RESIDUAL_AVERAGE",
                false
        ));
        consumptionDetails.add(buildOverviewDetail(
                "押金",
                deposit,
                depositByDate,
                startDate,
                endDate,
                StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT,
                false
        ));
        consumptionDetails.add(buildOverviewDetail(
                "退房金",
                checkoutFee,
                checkoutFeeByDate,
                startDate,
                endDate,
                "UNSUPPORTED",
                true
        ));
        consumptionDetails.add(buildOverviewDetail(
                "餐食/客房消费",
                roomServiceFee,
                roomServiceFeeByDate,
                startDate,
                endDate,
                StatisticsFinancialAggregationService.SOURCE_CONSUMPTION_ABS_AMOUNT,
                false
        ));
        consumptionDetails.add(buildOverviewDetail(
                "记一笔收入",
                notesIncome,
                notesIncomeByDate,
                startDate,
                endDate,
                StatisticsFinancialAggregationService.SOURCE_NOTE_TYPE,
                false
        ));
        consumptionDetails.add(buildOverviewDetail(
                "记一笔支出",
                notesExpense,
                notesExpenseByDate,
                startDate,
                endDate,
                StatisticsFinancialAggregationService.SOURCE_NOTE_TYPE,
                false
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
        StatisticsFinancialAggregationService.FinancialAggregation financial =
                financialAggregationService.aggregate(storeId, startDate, endDate);

        BigDecimal roomFee = allocationResult.totalRevenue();
        BigDecimal splitAccount = calculateSplitAccount(allocations);
        BigDecimal actualReceived = financial.getActualReceived();
        BigDecimal deposit = financial.getDeposit();
        BigDecimal roomServiceFee = financial.getRoomServiceFee();
        BigDecimal notesIncome = financial.getNotesIncome();
        BigDecimal paymentRefund = financial.getPaymentRefund();
        BigDecimal notesExpense = financial.getNotesExpense();
        BigDecimal totalIncome = roomFee.add(deposit).add(roomServiceFee).add(notesIncome);
        BigDecimal totalExpense = paymentRefund.add(notesExpense);
        BigDecimal netIncome = totalIncome.subtract(totalExpense);

        summary.setTotalRevenue(totalIncome);
        summary.setRevenuePrecision(allocationResult.revenuePrecision());
        summary.setSplitAccount(splitAccount);
        summary.setActualReceived(actualReceived);
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setNetIncome(netIncome);
        summary.setRoomFee(roomFee);
        summary.setDeposit(deposit);
        summary.setRoomServiceFee(roomServiceFee);
        summary.setNotesIncome(notesIncome);
        summary.setNotesExpense(notesExpense);
        summary.setPaymentRefund(paymentRefund);
        summary.setSourceMetadata(buildSourceMetadata());
        summary.setDataGaps(buildDataGaps());

        List<RevenueSummaryDTO.PaymentMethodStat> paymentStats = buildPaymentMethodStats(financial);
        summary.setPaymentMethodStats(paymentStats);

        List<RevenueSummaryDTO.Distribution> incomeDistribution = new ArrayList<>();
        addDistribution(incomeDistribution, "税后房费", roomFee, totalIncome);
        addDistribution(incomeDistribution, "押金", deposit, totalIncome);
        addDistribution(incomeDistribution, "客房消费", roomServiceFee, totalIncome);
        addDistribution(incomeDistribution, "记一笔收入", notesIncome, totalIncome);
        summary.setIncomeDistribution(incomeDistribution);

        List<RevenueSummaryDTO.Distribution> expenseDistribution = new ArrayList<>();
        addDistribution(expenseDistribution, "退款", paymentRefund, totalExpense);
        addDistribution(expenseDistribution, "记一笔支出", notesExpense, totalExpense);
        summary.setExpenseDistribution(expenseDistribution);

        List<RevenueSummaryDTO.CategoryStat> categoryStats = new ArrayList<>();
        BigDecimal categoryDenominator = totalIncome.add(totalExpense);
        addCategoryStat(categoryStats, "税后房费", roomFee, categoryDenominator,
                "RESERVATION_DAILY_PRICE_OR_RESIDUAL_AVERAGE", allocationResult.totalRoomNights());
        addCategoryStat(categoryStats, "押金", deposit, categoryDenominator,
                StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT, 0);
        addCategoryStat(categoryStats, "客房消费", roomServiceFee, categoryDenominator,
                StatisticsFinancialAggregationService.SOURCE_CONSUMPTION_ABS_AMOUNT, 0);
        addCategoryStat(categoryStats, "记一笔收入", notesIncome, categoryDenominator,
                StatisticsFinancialAggregationService.SOURCE_NOTE_TYPE, 0);
        addCategoryStat(categoryStats, "退款", paymentRefund, categoryDenominator,
                StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT, 0);
        addCategoryStat(categoryStats, "记一笔支出", notesExpense, categoryDenominator,
                StatisticsFinancialAggregationService.SOURCE_NOTE_TYPE, 0);
        summary.setCategoryStats(categoryStats);

        // 每日流水明细
        List<RevenueSummaryDTO.DailyRevenue> dailyRevenues = new ArrayList<>();
        Map<LocalDate, RevenueAggregation> dailyRevenueMap = aggregateByDate(allocations, startDate, endDate);
        Map<LocalDate, BigDecimal> splitAccountByDate = aggregateSplitAccountByDate(allocations, startDate, endDate);
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            RevenueAggregation aggregation = dailyRevenueMap.get(currentDate);
            BigDecimal dailyRoomFee = aggregation != null ? aggregation.revenue : BigDecimal.ZERO;
            int orderCount = aggregation != null ? aggregation.orderIds.size() : 0;
            StatisticsFinancialAggregationService.DailyFinancialAmount dailyFinancial =
                    financial.getDailyAmountMap().get(currentDate);
            BigDecimal dailyDeposit = BigDecimal.ZERO;
            BigDecimal dailyRoomServiceFee = BigDecimal.ZERO;
            BigDecimal dailyNotesIncome = BigDecimal.ZERO;
            BigDecimal dailyPaymentRefund = BigDecimal.ZERO;
            BigDecimal dailyNotesExpense = BigDecimal.ZERO;
            BigDecimal dailyActualReceived = BigDecimal.ZERO;
            int transactionCount = 0;
            if (dailyFinancial != null) {
                dailyDeposit = dailyFinancial.getDeposit();
                dailyRoomServiceFee = dailyFinancial.getRoomServiceFee();
                dailyNotesIncome = dailyFinancial.getNotesIncome();
                dailyPaymentRefund = dailyFinancial.getPaymentRefund();
                dailyNotesExpense = dailyFinancial.getNotesExpense();
                dailyActualReceived = dailyFinancial.getActualReceived();
                transactionCount = dailyFinancial.getTransactionCount();
            }
            BigDecimal dailyTotalIncome = dailyRoomFee
                    .add(dailyDeposit)
                    .add(dailyRoomServiceFee)
                    .add(dailyNotesIncome);
            BigDecimal dailyTotalExpense = dailyPaymentRefund.add(dailyNotesExpense);
            BigDecimal dailyNetIncome = dailyTotalIncome.subtract(dailyTotalExpense);

            RevenueSummaryDTO.DailyRevenue dailyRevenue = new RevenueSummaryDTO.DailyRevenue(
                    currentDate.toString(),
                    dailyTotalIncome,
                    orderCount
            );
            dailyRevenue.setRoomFee(dailyRoomFee);
            dailyRevenue.setSplitAccount(splitAccountByDate.getOrDefault(currentDate, BigDecimal.ZERO));
            dailyRevenue.setActualReceived(dailyActualReceived);
            dailyRevenue.setDeposit(dailyDeposit);
            dailyRevenue.setRoomServiceFee(dailyRoomServiceFee);
            dailyRevenue.setNotesIncome(dailyNotesIncome);
            dailyRevenue.setNotesExpense(dailyNotesExpense);
            dailyRevenue.setPaymentRefund(dailyPaymentRefund);
            dailyRevenue.setTotalIncome(dailyTotalIncome);
            dailyRevenue.setTotalExpense(dailyTotalExpense);
            dailyRevenue.setNetIncome(dailyNetIncome);
            dailyRevenue.setTransactionCount(transactionCount);
            dailyRevenues.add(dailyRevenue);
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
        return getSalesSummary(
                startDate,
                endDate,
                keyword,
                channelId,
                null,
                DEFAULT_SALES_PAGE,
                DEFAULT_SALES_PAGE_SIZE
        );
    }

    public SalesSummaryDTO getSalesSummary(
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Long channelId,
            String customer,
            Integer page,
            Integer pageSize
    ) {
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
                        (r.getOrderNumber() != null && r.getOrderNumber().toLowerCase().contains(searchKeyword)) ||
                        (r.getChannelOrderNumber() != null && r.getChannelOrderNumber().toLowerCase().contains(searchKeyword)) ||
                        (r.getId() != null && r.getId().toString().contains(searchKeyword))
                    )
                    .collect(Collectors.toList());
        }

        if (customer != null && !customer.trim().isEmpty()) {
            final String customerKeyword = customer.trim().toLowerCase();
            reservations = reservations.stream()
                    .filter(r ->
                        (r.getGuestName() != null && r.getGuestName().toLowerCase().contains(customerKeyword)) ||
                        (r.getGuestPhone() != null && r.getGuestPhone().contains(customerKeyword))
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
        int resolvedPage = sanitizePage(page);
        int resolvedPageSize = sanitizePageSize(pageSize);
        summary.setPage(resolvedPage);
        summary.setPageSize(resolvedPageSize);
        summary.setTotalRecords(reservations.size());
        summary.setTotalPages(calculateTotalPages(reservations.size(), resolvedPageSize));

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

        Map<Long, OrderAllocationAggregation> orderAllocationMap = new HashMap<>();
        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            if (allocation.reservation() == null || allocation.reservation().getId() == null) {
                continue;
            }
            OrderAllocationAggregation orderAggregation = orderAllocationMap.computeIfAbsent(
                    allocation.reservation().getId(),
                    ignored -> new OrderAllocationAggregation()
            );
            orderAggregation.add(allocation);
        }

        reservations.sort((left, right) -> {
            if (left.getCreatedAt() == null && right.getCreatedAt() == null) {
                return 0;
            }
            if (left.getCreatedAt() == null) {
                return 1;
            }
            if (right.getCreatedAt() == null) {
                return -1;
            }
            return right.getCreatedAt().compareTo(left.getCreatedAt());
        });

        int fromIndex = (resolvedPage - 1) * resolvedPageSize;
        int toIndex = Math.min(fromIndex + resolvedPageSize, reservations.size());
        List<Reservation> pagedReservations = new ArrayList<>();
        if (fromIndex < reservations.size()) {
            pagedReservations = reservations.subList(fromIndex, toIndex);
        }

        // 销售订单明细
        List<SalesSummaryDTO.SalesOrderDetail> orderDetails = pagedReservations.stream()
                .map(r -> {
                    OrderAllocationAggregation orderAggregation = orderAllocationMap.get(r.getId());
                    BigDecimal allocatedAmount = BigDecimal.ZERO;
                    int allocatedRoomNights = 0;
                    if (orderAggregation != null) {
                        allocatedAmount = orderAggregation.amount;
                        allocatedRoomNights = orderAggregation.roomNights;
                    }

                    SalesSummaryDTO.SalesOrderDetail detail = new SalesSummaryDTO.SalesOrderDetail();
                    detail.setId(r.getId());
                    detail.setOrderNumber(r.getOrderNumber() != null ? r.getOrderNumber() : "");
                    detail.setChannelNumber(r.getChannelOrderNumber() != null ? r.getChannelOrderNumber() : "");
                    detail.setCreatedAt(r.getCreatedAt());
                    detail.setGuestName(r.getGuestName() != null ? r.getGuestName() : "系统");
                    detail.setChannelName(r.getChannel() != null ? r.getChannel().getName() : "");
                    detail.setCustomerName(r.getGuestName() != null ? r.getGuestName() : "");
                    detail.setPhone(r.getGuestPhone() != null ? r.getGuestPhone() : "");
                    detail.setAmount(allocatedAmount);
                    detail.setAllocatedAmount(allocatedAmount);
                    detail.setTotalAmount(r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO);
                    detail.setAllocatedRoomNights(allocatedRoomNights);
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
        Map<String, Integer> roomCountByType = getRoomCountByType(storeId);

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
                reservations, startDate, endDate, roomCountByType, days);
        dto.setOccupancyDetails(occupancyDetails);

        // 5. 计算RevPAR明细
        List<RoomDetailDTO> revparDetails = calculateRevparDetails(
                allocations, roomCountByType, days, endDate);
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
            Map<String, Integer> roomCountByType, long days) {

        List<RoomDetailDTO> details = new ArrayList<>();

        // 按房型分组
        Map<String, List<Reservation>> roomTypeMap = new HashMap<>();
        for (Reservation reservation : reservations) {
            String roomType = getRoomTypeName(reservation);
            roomTypeMap.computeIfAbsent(roomType, ignored -> new ArrayList<>()).add(reservation);
        }
        for (String roomType : roomCountByType.keySet()) {
            roomTypeMap.computeIfAbsent(roomType, ignored -> new ArrayList<>());
        }

        for (Map.Entry<String, List<Reservation>> entry : roomTypeMap.entrySet()) {
            String roomType = entry.getKey();
            List<Reservation> typeReservations = entry.getValue();

            // 计算该房型的总间夜数
            int totalNights = typeReservations.stream()
                    .mapToInt(r -> calculateRoomNights(r, startDate, endDate))
                    .sum();

            long roomCount = roomCountByType.getOrDefault(roomType, 0);
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
            Map<String, Integer> roomCountByType,
            long days,
            LocalDate endDate) {

        List<RoomDetailDTO> details = new ArrayList<>();

        // 按房型分组
        Map<String, RevenueAggregation> roomTypeMap = new HashMap<>();
        Map<String, BigDecimal> lastDayFeeByType = new HashMap<>();

        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            String roomType = getRoomTypeName(allocation.reservation());
            roomTypeMap.computeIfAbsent(roomType, ignored -> new RevenueAggregation()).add(allocation);
            if (allocation.date().equals(endDate)) {
                BigDecimal current = lastDayFeeByType.getOrDefault(roomType, BigDecimal.ZERO);
                lastDayFeeByType.put(roomType, current.add(allocation.amount()));
            }
        }
        for (String roomType : roomCountByType.keySet()) {
            roomTypeMap.computeIfAbsent(roomType, ignored -> new RevenueAggregation());
        }

        for (Map.Entry<String, RevenueAggregation> entry : roomTypeMap.entrySet()) {
            String roomType = entry.getKey();
            RevenueAggregation aggregation = entry.getValue();
            BigDecimal totalFee = aggregation.revenue;

            long roomCount = roomCountByType.getOrDefault(roomType, 0);
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

    private void addOverviewDistribution(
            List<BusinessOverviewDTO.CategoryDistribution> target,
            String category,
            BigDecimal amount,
            BigDecimal total
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        target.add(new BusinessOverviewDTO.CategoryDistribution(
                category,
                amount,
                calculatePercentage(amount, total)
        ));
    }

    private BusinessOverviewDTO.ConsumptionDetail buildOverviewDetail(
            String category,
            BigDecimal total,
            Map<LocalDate, BigDecimal> amountsByDate,
            LocalDate startDate,
            LocalDate endDate,
            String sourceType,
            boolean unsupported
    ) {
        List<BusinessOverviewDTO.ConsumptionDetail.DailyAmount> dailyAmounts = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            BigDecimal amount = amountsByDate.getOrDefault(currentDate, BigDecimal.ZERO);
            dailyAmounts.add(new BusinessOverviewDTO.ConsumptionDetail.DailyAmount(
                    currentDate.toString(),
                    amount
            ));
            currentDate = currentDate.plusDays(1);
        }

        BusinessOverviewDTO.ConsumptionDetail detail = new BusinessOverviewDTO.ConsumptionDetail(
                category,
                total,
                dailyAmounts
        );
        detail.setSourceType(sourceType);
        detail.setUnsupported(unsupported);
        return detail;
    }

    private List<StatisticsSourceMetadataDTO> buildSourceMetadata() {
        List<StatisticsSourceMetadataDTO> metadata = new ArrayList<>();
        metadata.add(new StatisticsSourceMetadataDTO(
                "roomFee",
                "RESERVATION_DAILY_PRICE_OR_RESIDUAL_AVERAGE",
                ReservationRevenueAllocationService.DATE_BASIS_STAY_DATE,
                ReservationRevenueAllocationService.TAX_MODE_PRICE_AFTER_TAX,
                "优先 ReservationDailyPrice.priceAfterTax；缺失每日价时使用整单残差均摊"
        ));
        metadata.add(new StatisticsSourceMetadataDTO(
                "deposit",
                StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT,
                "PAYMENT_DATE",
                "ABS_AMOUNT",
                "Payment.type 为自由文本，服务端按 deposit/押金等关键词保守归一化"
        ));
        metadata.add(new StatisticsSourceMetadataDTO(
                "actualReceived",
                StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT,
                "PAYMENT_DATE",
                "ABS_AMOUNT",
                "Payment.type 非押金/退款时按实收款处理；OTA 代收支付方式不计入直接实收"
        ));
        metadata.add(new StatisticsSourceMetadataDTO(
                "roomServiceFee",
                StatisticsFinancialAggregationService.SOURCE_CONSUMPTION_ABS_AMOUNT,
                "CONSUMPTION_DATE",
                "ABS_AMOUNT",
                "Consumption.amount 当前以负数保存，统计展示取绝对值"
        ));
        metadata.add(new StatisticsSourceMetadataDTO(
                "notesIncomeExpense",
                StatisticsFinancialAggregationService.SOURCE_NOTE_TYPE,
                "NOTE_DATETIME",
                "ABS_AMOUNT",
                "Note.type=income 计收入，type=expense 计支出"
        ));
        return metadata;
    }

    private List<StatisticsDataGapDTO> buildDataGaps() {
        List<StatisticsDataGapDTO> dataGaps = new ArrayList<>();
        dataGaps.add(new StatisticsDataGapDTO(
                "checkoutFee",
                "当前没有稳定独立的退房金字段或业务模型，Payment.type 自由文本不足以形成正式口径",
                "需要新增退房金业务模型或稳定交易分类",
                true
        ));
        return dataGaps;
    }

    private List<RevenueSummaryDTO.PaymentMethodStat> buildPaymentMethodStats(
            StatisticsFinancialAggregationService.FinancialAggregation financial
    ) {
        List<StatisticsFinancialAggregationService.MethodAmount> methodAmounts =
                financial.getIncomeByPaymentMethod();
        BigDecimal total = BigDecimal.ZERO;
        for (StatisticsFinancialAggregationService.MethodAmount methodAmount : methodAmounts) {
            total = total.add(methodAmount.getAmount());
        }

        List<RevenueSummaryDTO.PaymentMethodStat> stats = new ArrayList<>();
        for (StatisticsFinancialAggregationService.MethodAmount methodAmount : methodAmounts) {
            RevenueSummaryDTO.PaymentMethodStat stat = new RevenueSummaryDTO.PaymentMethodStat(
                    methodAmount.getName(),
                    methodAmount.getAmount(),
                    calculatePercentage(methodAmount.getAmount(), total)
            );
            stat.setNormalizedType(methodAmount.getNormalizedType());
            stat.setSourceType(methodAmount.getSourceType());
            stat.setTransactionCount(methodAmount.getTransactionCount());
            stats.add(stat);
        }
        stats.sort(Comparator.comparing(RevenueSummaryDTO.PaymentMethodStat::getAmount).reversed());
        return stats;
    }

    private void addDistribution(
            List<RevenueSummaryDTO.Distribution> target,
            String name,
            BigDecimal value,
            BigDecimal total
    ) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        target.add(new RevenueSummaryDTO.Distribution(name, value, calculatePercentage(value, total)));
    }

    private void addCategoryStat(
            List<RevenueSummaryDTO.CategoryStat> target,
            String category,
            BigDecimal amount,
            BigDecimal total,
            String sourceType,
            int transactionCount
    ) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        RevenueSummaryDTO.CategoryStat stat = new RevenueSummaryDTO.CategoryStat(
                category,
                safeAmount,
                calculatePercentage(safeAmount, total)
        );
        stat.setSourceType(sourceType);
        stat.setTransactionCount(transactionCount);
        target.add(stat);
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (amount == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSplitAccount(List<ReservationRevenueAllocationService.Allocation> allocations) {
        BigDecimal total = BigDecimal.ZERO;
        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            if (isSplitAccountReservation(allocation.reservation())) {
                total = total.add(allocation.amount());
            }
        }
        return total;
    }

    private Map<LocalDate, BigDecimal> aggregateSplitAccountByDate(
            List<ReservationRevenueAllocationService.Allocation> allocations,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            result.put(currentDate, BigDecimal.ZERO);
            currentDate = currentDate.plusDays(1);
        }

        for (ReservationRevenueAllocationService.Allocation allocation : allocations) {
            if (!isSplitAccountReservation(allocation.reservation())) {
                continue;
            }
            BigDecimal currentAmount = result.getOrDefault(allocation.date(), BigDecimal.ZERO);
            result.put(allocation.date(), currentAmount.add(allocation.amount()));
        }
        return result;
    }

    private boolean isSplitAccountReservation(Reservation reservation) {
        if (reservation == null || reservation.getChannel() == null) {
            return false;
        }
        if (ChannelType.OTA.equals(reservation.getChannel().getType())) {
            return true;
        }
        String channelName = getChannelName(reservation).toLowerCase();
        return channelName.contains("booking")
                || channelName.contains("airbnb")
                || channelName.contains("agoda")
                || channelName.contains("expedia")
                || channelName.contains("ctrip")
                || channelName.contains("携程")
                || channelName.contains("美团");
    }

    private Map<String, Integer> getRoomCountByType(Long storeId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        for (Room room : rooms) {
            if (room == null || room.getRoomType() == null || room.getRoomType().getName() == null) {
                continue;
            }
            String roomTypeName = room.getRoomType().getName();
            int currentCount = result.getOrDefault(roomTypeName, 0);
            result.put(roomTypeName, currentCount + 1);
        }
        return result;
    }

    private int sanitizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_SALES_PAGE;
        }
        return page;
    }

    private int sanitizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_SALES_PAGE_SIZE;
        }
        if (pageSize > MAX_SALES_PAGE_SIZE) {
            return MAX_SALES_PAGE_SIZE;
        }
        return pageSize;
    }

    private int calculateTotalPages(int totalRecords, int pageSize) {
        if (totalRecords <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalRecords / (double) pageSize);
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

    private static class OrderAllocationAggregation {
        private BigDecimal amount = BigDecimal.ZERO;
        private int roomNights = 0;

        void add(ReservationRevenueAllocationService.Allocation allocation) {
            if (allocation == null) {
                return;
            }
            amount = amount.add(allocation.amount() != null ? allocation.amount() : BigDecimal.ZERO);
            roomNights++;
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
