package server.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.BulkPriceChangeRequest;
import server.demo.dto.RoomPriceDTO;
import server.demo.dto.RoomPriceManagementDTO;
import server.demo.dto.UpdatePriceByPlanRequest;
import server.demo.dto.UpdateRoomPriceRequest;
import server.demo.entity.ChannelPrice;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.enums.ReservationStatus;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.service.PriceLabsCalendarSyncDebouncer;
import server.demo.service.PriceChangeHistoryService;
import server.demo.service.RoomPriceService;
import server.demo.util.StoreContextUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomPriceServiceImpl implements RoomPriceService {
    private static final Set<ReservationStatus> ROOM_PRICE_OCCUPANCY_STATUSES = Set.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.REQUESTED
    );

    @Autowired
    private RoomPriceRepository roomPriceRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private PricePlanRepository pricePlanRepository;

    @Autowired
    private RoomTypePricePlanRepository roomTypePricePlanRepository;

    @Autowired
    private PriceChangeHistoryService priceChangeHistoryService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ChannelPriceRepository channelPriceRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomBlockoutRepository roomBlockoutRepository;

    @Autowired(required = false)
    private PriceLabsCalendarSyncDebouncer priceLabsCalendarSyncDebouncer;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    @Override
    public List<RoomPriceDTO> getRoomPricesByDateRange(LocalDate startDate, LocalDate endDate) {
        Long storeId = currentStoreId();
        List<RoomPrice> roomPrices = roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(storeId, startDate, endDate);
        return roomPrices.stream()
                .map(RoomPriceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomPriceDTO> getRoomPricesByRoomTypeAndDateRange(Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        Long storeId = currentStoreId();
        if (roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId).isEmpty()) {
            return List.of();
        }
        List<RoomPrice> roomPrices = roomPriceRepository.findByStoreIdAndRoomTypeIdAndPriceDateBetween(storeId, roomTypeId, startDate, endDate);
        return roomPrices.stream()
                .map(RoomPriceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomPriceDTO> getRoomPricesByRoomTypesAndDateRange(List<Long> roomTypeIds, LocalDate startDate, LocalDate endDate) {
        Long storeId = currentStoreId();
        if (roomTypeIds == null || roomTypeIds.isEmpty()) {
            return List.of();
        }
        List<RoomPrice> roomPrices = roomPriceRepository.findByStoreIdAndRoomTypeIdsAndPriceDateBetweenWithRoomTypeAndPricePlan(
                storeId,
                roomTypeIds,
                startDate,
                endDate
        );
        return roomPrices.stream()
                .map(RoomPriceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RoomPriceDTO> getRoomPrice(Long roomTypeId, LocalDate date) {
        Long storeId = currentStoreId();
        Optional<RoomType> roomTypeOpt = roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId);
        if (roomTypeOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<RoomPrice> roomPrice = roomPriceRepository.findByRoomTypeAndPriceDate(roomTypeOpt.get(), date);
        return roomPrice.map(RoomPriceDTO::new);
    }

    @Override
    public BigDecimal getEffectivePrice(Long roomTypeId, LocalDate date) {
        Long storeId = currentStoreId();
        Optional<RoomType> roomTypeOpt = roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId);
        if (roomTypeOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return getEffectivePrice(roomTypeOpt.get(), date);
    }

    @Override
    public BigDecimal getEffectivePrice(RoomType roomType, LocalDate date) {
        // 1. 首先查找是否有特定日期的价格设置
        Optional<RoomPrice> specificPrice = roomPriceRepository.findByRoomTypeAndPriceDate(roomType, date);
        if (specificPrice.isPresent()) {
            return specificPrice.get().getPrice();
        }

        // 2. 如果没有特定日期价格，则使用房型的默认价格逻辑
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        boolean isWeekend = dayOfWeek >= 6; // Saturday=6, Sunday=7

        if (isWeekend && roomType.getWeekendPrice() != null) {
            return roomType.getWeekendPrice();
        } else if (!isWeekend && roomType.getWeekdayPrice() != null) {
            return roomType.getWeekdayPrice();
        } else if (roomType.getDefaultPrice() != null) {
            return roomType.getDefaultPrice();
        }

        // 3. 如果都没有设置，返回0
        return BigDecimal.ZERO;
    }

    @Override
    public List<RoomPriceDTO> updateRoomPrice(UpdateRoomPriceRequest request) {
        Long storeId = currentStoreId();
        // 验证房型存在
        Optional<RoomType> roomTypeOpt = roomTypeRepository.findByStoreIdAndId(storeId, request.getRoomTypeId());
        if (roomTypeOpt.isEmpty()) {
            throw new IllegalArgumentException("房型不存在: " + request.getRoomTypeId());
        }

        RoomType roomType = roomTypeOpt.get();
        List<RoomPriceDTO> updatedPrices = new ArrayList<>();

        // 遍历日期范围，为每一天创建或更新价格
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            // 查找是否已存在该日期的价格
            Optional<RoomPrice> existingPrice = roomPriceRepository.findByRoomTypeAndPriceDate(roomType, currentDate);

            RoomPrice roomPrice;
            if (existingPrice.isPresent()) {
                // 更新现有价格
                roomPrice = existingPrice.get();
                roomPrice.setPrice(request.getPrice());
            } else {
                // 创建新价格记录
                roomPrice = new RoomPrice(roomType, currentDate, request.getPrice());
            }
            applyManualPriceOverride(roomPrice, request.getEndDate());

            // 设置假期标记
            if (request.getIsHoliday() != null) {
                roomPrice.setIsHoliday(request.getIsHoliday());
            }

            // 设置备注
            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                roomPrice.setNotes(request.getNotes());
            }

            // 保存价格
            roomPrice = roomPriceRepository.save(roomPrice);
            updatedPrices.add(new RoomPriceDTO(roomPrice));

            // 移动到下一天
            currentDate = currentDate.plusDays(1);
        }

        return updatedPrices;
    }

    @Override
    public boolean deleteRoomPrice(Long roomTypeId, LocalDate date) {
        Long storeId = currentStoreId();
        Optional<RoomType> roomTypeOpt = roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId);
        if (roomTypeOpt.isEmpty()) {
            return false;
        }

        Optional<RoomPrice> roomPrice = roomPriceRepository.findByRoomTypeAndPriceDate(roomTypeOpt.get(), date);
        if (roomPrice.isPresent()) {
            roomPriceRepository.delete(roomPrice.get());
            return true;
        }
        return false;
    }

    @Override
    public int deleteRoomPriceRange(Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        Long storeId = currentStoreId();
        if (roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId).isEmpty()) {
            return 0;
        }
        List<RoomPrice> pricesToDelete = roomPriceRepository.findByStoreIdAndRoomTypeIdAndPriceDateBetween(
                storeId,
                roomTypeId,
                startDate,
                endDate
        );
        int count = pricesToDelete.size();
        roomPriceRepository.deleteAll(pricesToDelete);
        return count;
    }

    @Override
    public List<RoomPriceDTO> batchUpdateRoomPrices(List<UpdateRoomPriceRequest> requests) {
        List<RoomPriceDTO> allUpdatedPrices = new ArrayList<>();

        for (UpdateRoomPriceRequest request : requests) {
            try {
                List<RoomPriceDTO> updatedPrices = updateRoomPrice(request);
                allUpdatedPrices.addAll(updatedPrices);
            } catch (Exception e) {
                // 记录错误但继续处理其他请求
                System.err.println("Failed to update room price for roomTypeId " +
                    request.getRoomTypeId() + ": " + e.getMessage());
            }
        }

        return allUpdatedPrices;
    }

    @Override
    public List<RoomPriceDTO> bulkPriceChange(BulkPriceChangeRequest request) {
        // 验证请求数据
        request.validate();

        Long storeId = currentStoreId();
        List<RoomPriceDTO> updatedPrices = new ArrayList<>();

        // 获取所有要更新的房型
        List<Long> roomTypeIds = request.getRoomTypeIds();
        if (roomTypeIds == null || roomTypeIds.isEmpty()) {
            throw new IllegalArgumentException("roomTypeIds 不能为空");
        }
        List<RoomType> roomTypes = roomTypeRepository.findAllById(roomTypeIds).stream()
                .filter(rt -> rt != null && storeId.equals(rt.getStoreId()))
                .toList();
        if (roomTypes.isEmpty()) {
            throw new IllegalArgumentException("未找到指定的房型");
        }
        if (roomTypes.size() != roomTypeIds.stream().filter(id -> id != null).distinct().count()) {
            throw new IllegalArgumentException("部分房型不存在或无权限");
        }

        // 遍历每个房型
        for (RoomType roomType : roomTypes) {
            // 遍历每个日期范围
            for (BulkPriceChangeRequest.DateRangeDTO dateRange : request.getDateRanges()) {
                LocalDate currentDate = dateRange.getStartDate();

                // 遍历日期范围内的每一天
                while (!currentDate.isAfter(dateRange.getEndDate())) {
                    // 检查该日期是否符合星期几筛选条件
                    if (shouldApplyPriceForDate(currentDate, request.getWeekdays())) {
                        // 确定该日期使用的价格（平日价或周末价）
                        BigDecimal priceToApply = determinePriceForDate(
                            currentDate,
                            request.getWeekendDifferentiation(),
                            request.getWeekdayPrice(),
                            request.getWeekendPrice()
                        );

                        // 查找是否已存在该日期的价格
                        Optional<RoomPrice> existingPrice = roomPriceRepository
                            .findByRoomTypeAndPriceDate(roomType, currentDate);

                        RoomPrice roomPrice;
                        if (existingPrice.isPresent()) {
                            // 更新现有价格
                            roomPrice = existingPrice.get();
                            roomPrice.setPrice(priceToApply);
                        } else {
                            // 创建新价格记录
                            roomPrice = new RoomPrice(roomType, currentDate, priceToApply);
                        }
                        applyManualPriceOverride(roomPrice, dateRange.getEndDate());

                        // 设置备注
                        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                            roomPrice.setNotes(request.getNotes());
                        }

                        // 保存价格
                        roomPrice = roomPriceRepository.save(roomPrice);
                        updatedPrices.add(new RoomPriceDTO(roomPrice));
                    }

                    // 移动到下一天
                    currentDate = currentDate.plusDays(1);
                }
            }
        }

        return updatedPrices;
    }

    /**
     * 判断某个日期是否符合星期几筛选条件
     * @param date 要检查的日期
     * @param weekdays 允许的星期几集合（1=周一, ..., 6=周六, 0=周日）
     * @return 如果weekdays为空或包含该日期的星期几，返回true
     */
    private boolean shouldApplyPriceForDate(LocalDate date, Set<Integer> weekdays) {
        // 如果没有指定星期几筛选，则所有日期都适用
        if (weekdays == null || weekdays.isEmpty() || weekdays.size() == 7) {
            return true;
        }

        // 获取日期的星期几（1=Monday, 7=Sunday）
        int dayOfWeek = date.getDayOfWeek().getValue();

        // 转换为前端的格式（1=周一, ..., 6=周六, 0=周日）
        int weekdayValue = (dayOfWeek == 7) ? 0 : dayOfWeek;

        return weekdays.contains(weekdayValue);
    }

    /**
     * 根据日期和设置确定使用的价格
     * @param date 日期
     * @param weekendDifferentiation 是否区分平日周末
     * @param weekdayPrice 平日价
     * @param weekendPrice 周末价
     * @return 该日期应使用的价格
     */
    private BigDecimal determinePriceForDate(
        LocalDate date,
        Boolean weekendDifferentiation,
        BigDecimal weekdayPrice,
        BigDecimal weekendPrice
    ) {
        // 如果不区分平日周末，直接使用平日价（即统一价格）
        if (!weekendDifferentiation) {
            return weekdayPrice;
        }

        // 判断是否为周末（周五和周六）
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY);

        return isWeekend ? weekendPrice : weekdayPrice;
    }

    @Override
    public List<RoomPriceManagementDTO> getRoomPriceManagementData(LocalDate startDate, LocalDate endDate, Long roomTypeId) {
        Long storeId = currentStoreId();
        // 先查询所有房型(或特定房型)
        List<RoomType> roomTypes;
        if (roomTypeId != null) {
            roomTypes = roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId)
                    .map(List::of)
                    .orElse(new ArrayList<>());
        } else {
            roomTypes = roomTypeRepository.findByStoreIdOrderByName(storeId);
        }

        // 查询所有房型价格计划关联
        List<RoomTypePricePlan> roomTypePricePlans;
        roomTypePricePlans = roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(storeId);
        if (roomTypeId != null) {
            roomTypePricePlans = roomTypePricePlans.stream()
                    .filter(rtp -> rtp != null && rtp.getRoomType() != null && roomTypeId.equals(rtp.getRoomType().getId()))
                    .toList();
        }

        // 调试: 输出查询到的房型和价格计划数量
        System.out.println("===== 查询到的房型数量: " + roomTypes.size() + " =====");
        System.out.println("===== 查询到的价格计划数量: " + roomTypePricePlans.size() + " =====");
        for (RoomTypePricePlan plan : roomTypePricePlans) {
            System.out.println("价格计划: 房型=" + plan.getRoomType().getName() +
                             ", 计划=" + plan.getPricePlan().getName());
        }

        // 查询日期范围内的所有特定日期价格覆盖记录（门店级）
        List<RoomPrice> specificPrices = roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                storeId,
                startDate,
                endDate
        );
        List<ChannelPrice> priceLabsChannelPrices = channelPriceRepository.findByStoreIdAndDateRange(storeId, startDate, endDate);
        Map<String, ChannelPrice> priceLabsPriceMap = buildPriceLabsPriceMap(priceLabsChannelPrices);

        // 查询日期范围内的所有预订记录，用于计算可用房间数（门店级）
        List<Reservation> reservations = reservationRepository.findByStoreIdAndDateRange(storeId, startDate, endDate);
        Set<Long> targetRoomTypeIds = roomTypes.stream()
                .map(RoomType::getId)
                .collect(Collectors.toSet());
        List<Room> roomsInStore = roomRepository.findByStoreIdWithRoomType(storeId).stream()
                .filter(room -> room != null
                        && room.getId() != null
                        && room.getRoomType() != null
                        && room.getRoomType().getId() != null
                        && targetRoomTypeIds.contains(room.getRoomType().getId()))
                .toList();
        List<Long> targetRoomIds = roomsInStore.stream()
                .map(Room::getId)
                .toList();
        Map<String, Integer> blockedRoomsByTypeDate = new HashMap<>();
        if (!targetRoomIds.isEmpty()) {
            List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                    storeId,
                    targetRoomIds,
                    startDate,
                    endDate
            );
            for (RoomBlockout blockout : blockouts) {
                if (blockout == null || blockout.getBlockDate() == null || blockout.getRoom() == null
                        || blockout.getRoom().getRoomType() == null || blockout.getRoom().getRoomType().getId() == null) {
                    continue;
                }
                Long blockedRoomTypeId = blockout.getRoom().getRoomType().getId();
                String key = blockedRoomTypeId + "|" + blockout.getBlockDate();
                blockedRoomsByTypeDate.merge(key, 1, Integer::sum);
            }
        }

        // 临时调试: 输出查询到的预订信息
        System.out.println("===== 房价管理查询预订 =====");
        System.out.println("查询日期范围: " + startDate + " 到 " + endDate);
        System.out.println("查询到的预订总数: " + reservations.size());
        for (Reservation r : reservations) {
            System.out.println("预订ID=" + r.getId() +
                             ", 房间=" + (r.getRoom() != null ? r.getRoom().getRoomNumber() : "null") +
                             ", 房型=" + (r.getRoom() != null && r.getRoom().getRoomType() != null ? r.getRoom().getRoomType().getName() : "null") +
                             ", 状态=" + r.getStatus() +
                             ", 入住=" + r.getCheckInDate() +
                             ", 退房=" + r.getCheckOutDate());
        }
        System.out.println("============================\n");

        // 为每个房型和日期范围生成价格记录
        List<RoomPriceManagementDTO> result = new ArrayList<>();

        // 遍历所有房型
        for (RoomType roomType : roomTypes) {
            // 获取该房型的价格计划列表
            List<RoomTypePricePlan> plansForRoomType = roomTypePricePlans.stream()
                    .filter(p -> p.getRoomType().getId().equals(roomType.getId()))
                    .collect(java.util.stream.Collectors.toList());

            // 如果该房型没有价格计划,创建一个虚拟的价格计划记录
            if (plansForRoomType.isEmpty()) {
                // 为没有价格计划的房型生成数据,只显示可用房间数
                LocalDate currentDate = startDate;
                while (!currentDate.isAfter(endDate)) {
                    RoomPriceManagementDTO dto = new RoomPriceManagementDTO();
                    dto.setRoomTypeId(roomType.getId());
                    dto.setRoomTypeName(roomType.getName());
                    dto.setRoomTypeCode(roomType.getCode());
                    dto.setPricePlanId(null);
                    dto.setPricePlanName(null);
                    dto.setPriceDate(currentDate);

                    // 动态计算可用房间数
                    Integer availableRooms = calculateAvailableRooms(
                            roomType,
                            currentDate,
                            null,
                            reservations,
                            blockedRoomsByTypeDate
                    );
                    dto.setAvailableRooms(availableRooms);

                    // 没有价格计划时,价格为null或默认价格
                    dto.setPrice(roomType.getDefaultPrice() != null ? roomType.getDefaultPrice() : BigDecimal.ZERO);
                    dto.setPriceSource(RoomPrice.PRICE_SOURCE_SYSTEM);
                    dto.setManualOverride(false);
                    dto.setManualOverrideUntil(null);

                    // 判断是否为周末
                    dto.setIsWeekend(currentDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                                     currentDate.getDayOfWeek() == DayOfWeek.SUNDAY);

                    result.add(dto);
                    currentDate = currentDate.plusDays(1);
                }
                continue; // 跳过后面的价格计划循环
            }

            // 如果该房型有价格计划,为每个价格计划生成数据
            for (RoomTypePricePlan plan : plansForRoomType) {
                LocalDate currentDate = startDate;

                while (!currentDate.isAfter(endDate)) {
                    RoomPriceManagementDTO dto = new RoomPriceManagementDTO();
                    dto.setRoomTypeId(plan.getRoomType().getId());
                    dto.setRoomTypeName(plan.getRoomType().getName());
                    dto.setRoomTypeCode(plan.getRoomType().getCode());
                    dto.setPricePlanId(plan.getPricePlan().getId());
                    dto.setPricePlanName(plan.getPricePlan().getName());
                    dto.setPriceDate(currentDate);

                // 首先检查是否有特定日期的价格覆盖
                final LocalDate checkDate = currentDate;
                Optional<RoomPrice> specificPrice = specificPrices.stream()
                        .filter(sp -> sp.getRoomType().getId().equals(plan.getRoomType().getId())
                                && sp.getPricePlan() != null
                                && sp.getPricePlan().getId().equals(plan.getPricePlan().getId())
                                && sp.getPriceDate().equals(checkDate))
                        .findFirst();
                ChannelPrice priceLabsPrice = priceLabsPriceMap.get(
                    buildPriceLabsKey(plan.getRoomType().getId(), plan.getPricePlan().getId(), checkDate)
                );

                BigDecimal price;
                Integer availableRooms;

                if (specificPrice.isPresent()) {
                    // 使用特定日期的覆盖价格
                    RoomPrice roomPrice = specificPrice.get();
                    price = roomPrice.getPrice();
                    availableRooms = calculateAvailableRooms(
                            plan.getRoomType(),
                            checkDate,
                            roomPrice.getAvailableRooms(),
                            reservations,
                            blockedRoomsByTypeDate
                    );

                    // 设置minStay和maxStay
                    dto.setMinStay(roomPrice.getMinStay());
                    dto.setMaxStay(roomPrice.getMaxStay());
                    dto.setCloseRoom(roomPrice.getCloseRoom());
                    dto.setCta(roomPrice.getCta());
                    dto.setCtd(roomPrice.getCtd());
                    dto.setPriceSource(roomPrice.getPriceSource());
                    dto.setManualOverride(roomPrice.getManualOverride());
                    dto.setManualOverrideUntil(roomPrice.getManualOverrideUntil());
                    dto.setNotes(roomPrice.getNotes());
                    applyPriceLabsMetadata(dto, priceLabsPrice);
                } else {
                    // 使用周价格
                    price = getPriceForDayOfWeek(currentDate.getDayOfWeek(), plan);
                    // 动态计算可用房间数
                    availableRooms = calculateAvailableRooms(
                            plan.getRoomType(),
                            checkDate,
                            null,
                            reservations,
                            blockedRoomsByTypeDate
                    );
                    if (!checkDate.isBefore(LocalDate.of(2025, 11, 11)) && !checkDate.isAfter(LocalDate.of(2025, 11, 13))) {
                        System.out.println("  动态计算availableRooms(无specificPrice): " + availableRooms);
                    }

                    // 没有特定价格记录时,minStay和maxStay为null
                    dto.setMinStay(null);
                    dto.setMaxStay(null);
                    dto.setCloseRoom(null);
                    dto.setCta(null);
                    dto.setCtd(null);
                    dto.setPriceSource(RoomPrice.PRICE_SOURCE_SYSTEM);
                    dto.setManualOverride(false);
                    dto.setManualOverrideUntil(null);
                    applyPriceLabsMetadata(dto, priceLabsPrice);
                }

                dto.setPrice(price);
                dto.setAvailableRooms(availableRooms);

                // 判断是否为周末
                dto.setIsWeekend(currentDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                                 currentDate.getDayOfWeek() == DayOfWeek.SUNDAY);

                // 调试: 输出每个DTO的详细信息（仅11-11到11-13）
                if (!checkDate.isBefore(LocalDate.of(2025, 11, 11)) && !checkDate.isAfter(LocalDate.of(2025, 11, 13))) {
                    System.out.println("生成DTO: 房型=" + plan.getRoomType().getName() +
                                     ", 日期=" + checkDate +
                                     ", 价格=" + price +
                                     ", 可用房间=" + availableRooms);
                }

                result.add(dto);
                currentDate = currentDate.plusDays(1);
            }
            } // 结束for (RoomTypePricePlan plan : plansForRoomType)
        } // 结束for (RoomType roomType : roomTypes)

        return result;
    }

    /**
     * 计算指定日期指定房型的可用房间数
     * @param roomType 房型
     * @param date 日期
     * @param reservations 预订记录列表
     * @param blockedRoomsByTypeDate 房型在指定日期被关房的数量映射
     * @return 可用房间数 = 总房间数 - 已预订/已入住的房间数
     */
    private Integer calculateAvailableRooms(
            RoomType roomType,
            LocalDate date,
            Integer baseAvailableRoomsOverride,
            List<Reservation> reservations,
            Map<String, Integer> blockedRoomsByTypeDate
    ) {
        Integer baseAvailableRooms = resolveBaseAvailableRooms(roomType, baseAvailableRoomsOverride);
        if (baseAvailableRooms <= 0) {
            return 0;
        }

        long occupiedCount = reservations.stream()
                .filter(r -> {
                    boolean isOccupiedStatus = ROOM_PRICE_OCCUPANCY_STATUSES.contains(r.getStatus());
                    boolean isInDateRange = !date.isBefore(r.getCheckInDate()) && date.isBefore(r.getCheckOutDate());
                    boolean isSameRoomType = r.getRoom() != null
                            && r.getRoom().getRoomType() != null
                            && r.getRoom().getRoomType().getId().equals(roomType.getId());

                    return isOccupiedStatus && isInDateRange && isSameRoomType;
                })
                .count();

        int blockedCount = blockedRoomsByTypeDate.getOrDefault(roomType.getId() + "|" + date, 0);
        return Math.max(baseAvailableRooms - (int) occupiedCount - blockedCount, 0);
    }

    private int resolveBaseAvailableRooms(RoomType roomType, Integer baseAvailableRoomsOverride) {
        Integer baseAvailableRooms = baseAvailableRoomsOverride != null
                ? baseAvailableRoomsOverride
                : roomType.getTotalRooms();
        return Math.max(baseAvailableRooms != null ? baseAvailableRooms : 0, 0);
    }

    /**
     * 根据星期几获取对应的价格
     */
    private BigDecimal getPriceForDayOfWeek(DayOfWeek dayOfWeek, RoomTypePricePlan plan) {
        BigDecimal price = switch (dayOfWeek) {
            case MONDAY -> plan.getMondayPrice();
            case TUESDAY -> plan.getTuesdayPrice();
            case WEDNESDAY -> plan.getWednesdayPrice();
            case THURSDAY -> plan.getThursdayPrice();
            case FRIDAY -> plan.getFridayPrice();
            case SATURDAY -> plan.getSaturdayPrice();
            case SUNDAY -> plan.getSundayPrice();
        };
        // 如果价格为null,返回0
        return price != null ? price : BigDecimal.ZERO;
    }

    @Override
    public List<RoomPriceManagementDTO> updatePriceByPlan(UpdatePriceByPlanRequest request, String operator) {
        Long storeId = currentStoreId();
        // 验证房型和价格计划是否存在
        RoomType roomType = roomTypeRepository.findByStoreIdAndId(storeId, request.getRoomTypeId())
                .orElseThrow(() -> new IllegalArgumentException("房型不存在"));

        PricePlan pricePlan = pricePlanRepository.findByStoreIdAndId(storeId, request.getPricePlanId())
                .orElseThrow(() -> new IllegalArgumentException("价格计划不存在"));

        // 查找房型价格计划关联
        RoomTypePricePlan roomTypePricePlan = roomTypePricePlanRepository
                .findByRoomTypeIdAndPricePlanId(request.getRoomTypeId(), request.getPricePlanId())
                .orElseThrow(() -> new IllegalArgumentException("房型与价格计划未关联"));
        if (roomTypePricePlan.getStoreId() == null || !storeId.equals(roomTypePricePlan.getStoreId())) {
            throw new IllegalArgumentException("房型与价格计划不属于当前门店");
        }

        // 记录旧价格用于历史记录
        BigDecimal firstPreviousValue = null;

        // 判断是更新周价格还是特定日期价格
        boolean isWeekdayUpdate = request.getWeekdays() != null && !request.getWeekdays().isEmpty();
        boolean applyWeekdaysInRange = Boolean.TRUE.equals(request.getApplyWeekdaysInRange());
        boolean isMultiDayRange = request.getStartDate() != null
                && request.getEndDate() != null
                && !request.getStartDate().isEqual(request.getEndDate());
        // 强制约束：跨天范围必须按日期逐天更新，避免误走“按周模板”分支导致界面看起来未生效。
        if (isMultiDayRange) {
            applyWeekdaysInRange = true;
        }

        java.util.Set<Integer> weekdayFilter = null;
        if (applyWeekdaysInRange && isWeekdayUpdate) {
            List<Integer> weekdays = request.getWeekdays();
            if (weekdays != null && !weekdays.isEmpty() && !weekdays.contains(0)) {
                weekdayFilter = new java.util.HashSet<>();
                for (Integer w : weekdays) {
                    if (w != null) {
                        weekdayFilter.add(w);
                    }
                }
            }
        }

        if (request.getPrice() != null) {
            if (isWeekdayUpdate && !applyWeekdaysInRange) {
                // 场景1: 指定了星期几 - 更新room_type_price_plans表中对应的周价格字段
                List<Integer> weekdaysToUpdate = request.getWeekdays();

                // 如果包含0(全部),则更新所有星期
                if (weekdaysToUpdate.contains(0)) {
                    weekdaysToUpdate = List.of(1, 2, 3, 4, 5, 6, 7);
                }

                // 记录第一个要修改的星期几的旧价格
                if (!weekdaysToUpdate.isEmpty()) {
                    DayOfWeek firstDay = convertWeekdayValueToDayOfWeek(weekdaysToUpdate.get(0));
                    firstPreviousValue = getPriceForDayOfWeek(firstDay, roomTypePricePlan);
                }

                // 更新对应星期几的价格
                for (Integer weekday : weekdaysToUpdate) {
                    updatePriceForWeekday(roomTypePricePlan, weekday, request.getPrice());
                }

                // 保存更新
                roomTypePricePlanRepository.save(roomTypePricePlan);
            } else {
                // 场景2: 未指定星期几 - 为特定日期范围创建覆盖价格记录到room_prices表
                LocalDate currentDate = request.getStartDate();

                while (!currentDate.isAfter(request.getEndDate())) {
                    if (applyWeekdaysInRange && weekdayFilter != null
                            && !weekdayFilter.contains(currentDate.getDayOfWeek().getValue())) {
                        currentDate = currentDate.plusDays(1);
                        continue;
                    }
                    Optional<RoomPrice> existingPrice = roomPriceRepository
                            .findByRoomTypeIdAndPricePlanIdAndPriceDate(
                                    request.getRoomTypeId(),
                                    request.getPricePlanId(),
                                    currentDate
                            );

                    RoomPrice roomPrice;
                    if (existingPrice.isPresent()) {
                        roomPrice = existingPrice.get();
                        if (firstPreviousValue == null) {
                            firstPreviousValue = roomPrice.getPrice();
                        }
                        roomPrice.setPrice(request.getPrice());
                    } else {
                        // 记录旧价格(从room_type_price_plans获取)
                        if (firstPreviousValue == null) {
                            firstPreviousValue = getPriceForDayOfWeek(currentDate.getDayOfWeek(), roomTypePricePlan);
                        }
                        roomPrice = new RoomPrice(roomType, pricePlan, currentDate, request.getPrice());
                    }
                    applyManualPriceOverride(roomPrice, request.getEndDate());

                    if (request.getAvailableRooms() != null) {
                        roomPrice.setAvailableRooms(request.getAvailableRooms());
                    }
                    if (request.getMinStay() != null) {
                        roomPrice.setMinStay(request.getMinStay());
                    }
                    if (request.getMaxStay() != null) {
                        roomPrice.setMaxStay(request.getMaxStay());
                    }
                    if (request.getCloseRoom() != null) {
                        roomPrice.setCloseRoom(request.getCloseRoom());
                    }
                    if (request.getCta() != null) {
                        roomPrice.setCta(request.getCta());
                    }
                    if (request.getCtd() != null) {
                        roomPrice.setCtd(request.getCtd());
                    }
                    if (request.getNotes() != null) {
                        roomPrice.setNotes(request.getNotes());
                    }

                    roomPriceRepository.save(roomPrice);
                    currentDate = currentDate.plusDays(1);
                }
            }
        } else {
            // 场景3: 只更新其他字段(如最小天数、最大天数等),不更新价格
            LocalDate currentDate = request.getStartDate();

            while (!currentDate.isAfter(request.getEndDate())) {
                if (applyWeekdaysInRange && weekdayFilter != null
                        && !weekdayFilter.contains(currentDate.getDayOfWeek().getValue())) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }
                Optional<RoomPrice> existingPrice = roomPriceRepository
                        .findByRoomTypeIdAndPricePlanIdAndPriceDate(
                                request.getRoomTypeId(),
                                request.getPricePlanId(),
                                currentDate
                        );

                if (existingPrice.isPresent()) {
                    // 只更新已存在记录的非价格字段
                    RoomPrice roomPrice = existingPrice.get();

                    if (request.getAvailableRooms() != null) {
                        roomPrice.setAvailableRooms(request.getAvailableRooms());
                    }
                    if (request.getMinStay() != null) {
                        roomPrice.setMinStay(request.getMinStay());
                    }
                    if (request.getMaxStay() != null) {
                        roomPrice.setMaxStay(request.getMaxStay());
                    }
                    if (request.getCloseRoom() != null) {
                        roomPrice.setCloseRoom(request.getCloseRoom());
                    }
                    if (request.getCta() != null) {
                        roomPrice.setCta(request.getCta());
                    }
                    if (request.getCtd() != null) {
                        roomPrice.setCtd(request.getCtd());
                    }
                    if (request.getNotes() != null) {
                        roomPrice.setNotes(request.getNotes());
                    }

                    roomPriceRepository.save(roomPrice);
                } else {
                    // 如果没有现有记录,需要创建新记录,使用周价格作为默认值
                    BigDecimal defaultPrice = getPriceForDayOfWeek(currentDate.getDayOfWeek(), roomTypePricePlan);
                    RoomPrice roomPrice = new RoomPrice(roomType, pricePlan, currentDate, defaultPrice);

                    if (request.getAvailableRooms() != null) {
                        roomPrice.setAvailableRooms(request.getAvailableRooms());
                    }
                    if (request.getMinStay() != null) {
                        roomPrice.setMinStay(request.getMinStay());
                    }
                    if (request.getMaxStay() != null) {
                        roomPrice.setMaxStay(request.getMaxStay());
                    }
                    if (request.getCloseRoom() != null) {
                        roomPrice.setCloseRoom(request.getCloseRoom());
                    }
                    if (request.getCta() != null) {
                        roomPrice.setCta(request.getCta());
                    }
                    if (request.getCtd() != null) {
                        roomPrice.setCtd(request.getCtd());
                    }
                    if (request.getNotes() != null) {
                        roomPrice.setNotes(request.getNotes());
                    }

                    roomPriceRepository.save(roomPrice);
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        // 只有当价格被修改时才创建价格变更历史记录
        if (request.getPrice() != null) {
            String applyWeekdays = isWeekdayUpdate ? formatWeekdays(request.getWeekdays()) : "特定日期";
            priceChangeHistoryService.createPriceChangeHistory(
                    request.getRoomTypeId(),
                    request.getPricePlanId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    applyWeekdays,
                    request.getPrice(),
                    firstPreviousValue,
                    operator
            );
        }

        // 返回更新后的价格数据
        // PriceLabs：当可售量发生变化（关房/封锁）时，合并并延迟推送一次 /calendar，避免批量操作导致请求风暴。
        boolean shouldPushCalendar = request.getAvailableRooms() != null
                || request.getMinStay() != null
                || request.getMaxStay() != null
                || request.getCloseRoom() != null
                || request.getCta() != null
                || request.getCtd() != null;
        if (priceLabsCalendarSyncDebouncer != null && shouldPushCalendar) {
            try {
                priceLabsCalendarSyncDebouncer.requestSyncAfterCommit(
                        roomType.getStoreId(),
                        roomType.getId(),
                        request.getStartDate(),
                        request.getEndDate()
                );
            } catch (Exception ignored) {
                // 不影响 PMS 主流程
            }
        }

        return getRoomPriceManagementData(request.getStartDate(), request.getEndDate(), request.getRoomTypeId());
    }

    /**
     * 根据星期几值更新价格
     */
    private void applyManualPriceOverride(RoomPrice roomPrice, LocalDate manualOverrideUntil) {
        if (roomPrice == null) {
            return;
        }
        roomPrice.setPriceSource(RoomPrice.PRICE_SOURCE_MANUAL);
        roomPrice.setManualOverride(Boolean.TRUE);
        roomPrice.setManualOverrideUntil(manualOverrideUntil);
    }

    private void updatePriceForWeekday(RoomTypePricePlan plan, Integer weekday, BigDecimal price) {
        switch (weekday) {
            case 1 -> plan.setMondayPrice(price);
            case 2 -> plan.setTuesdayPrice(price);
            case 3 -> plan.setWednesdayPrice(price);
            case 4 -> plan.setThursdayPrice(price);
            case 5 -> plan.setFridayPrice(price);
            case 6 -> plan.setSaturdayPrice(price);
            case 7 -> plan.setSundayPrice(price);
        }
    }

    /**
     * 将weekday值(1-7)转换为DayOfWeek
     */
    private DayOfWeek convertWeekdayValueToDayOfWeek(Integer weekday) {
        return switch (weekday) {
            case 1 -> DayOfWeek.MONDAY;
            case 2 -> DayOfWeek.TUESDAY;
            case 3 -> DayOfWeek.WEDNESDAY;
            case 4 -> DayOfWeek.THURSDAY;
            case 5 -> DayOfWeek.FRIDAY;
            case 6 -> DayOfWeek.SATURDAY;
            case 7 -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("无效的星期几值: " + weekday);
        };
    }

    @Override
    public Optional<RoomPriceManagementDTO> getRoomPriceByPlan(Long roomTypeId, Long pricePlanId, LocalDate date) {
        Long storeId = currentStoreId();
        if (roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId).isEmpty()) {
            return Optional.empty();
        }
        if (pricePlanRepository.findByStoreIdAndId(storeId, pricePlanId).isEmpty()) {
            return Optional.empty();
        }
        Optional<RoomPrice> roomPrice = roomPriceRepository
                .findByRoomTypeIdAndPricePlanIdAndPriceDate(roomTypeId, pricePlanId, date);

        return roomPrice.map(this::convertToManagementDTO);
    }

    /**
     * 将RoomPrice转换为RoomPriceManagementDTO
     */
    private RoomPriceManagementDTO convertToManagementDTO(RoomPrice roomPrice) {
        RoomPriceManagementDTO dto = new RoomPriceManagementDTO();
        dto.setId(roomPrice.getId());
        dto.setRoomTypeId(roomPrice.getRoomType().getId());
        dto.setRoomTypeName(roomPrice.getRoomType().getName());
        dto.setRoomTypeCode(roomPrice.getRoomType().getCode());

        if (roomPrice.getPricePlan() != null) {
            dto.setPricePlanId(roomPrice.getPricePlan().getId());
            dto.setPricePlanName(roomPrice.getPricePlan().getName());
        }

        dto.setPriceDate(roomPrice.getPriceDate());
        dto.setPrice(roomPrice.getPrice());

        Long storeId = currentStoreId();
        List<Reservation> reservations = reservationRepository.findByStoreIdAndRoomTypeIdOverlappingDateRangeWithRoomType(
                storeId,
                roomPrice.getRoomType().getId(),
                roomPrice.getPriceDate(),
                roomPrice.getPriceDate()
        );
        List<Room> rooms = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomPrice.getRoomType().getId());
        Map<String, Integer> blockedRoomsByTypeDate = new HashMap<>();
        List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .filter(id -> id != null)
                .toList();
        if (!roomIds.isEmpty()) {
            List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                    storeId,
                    roomIds,
                    roomPrice.getPriceDate(),
                    roomPrice.getPriceDate()
            );
            for (RoomBlockout blockout : blockouts) {
                if (blockout == null || blockout.getRoom() == null || blockout.getRoom().getRoomType() == null
                        || blockout.getRoom().getRoomType().getId() == null || blockout.getBlockDate() == null) {
                    continue;
                }
                String key = blockout.getRoom().getRoomType().getId() + "|" + blockout.getBlockDate();
                blockedRoomsByTypeDate.merge(key, 1, Integer::sum);
            }
        }
        Integer availableRooms = calculateAvailableRooms(
                roomPrice.getRoomType(),
                roomPrice.getPriceDate(),
                roomPrice.getAvailableRooms(),
                reservations,
                blockedRoomsByTypeDate
        );
        dto.setAvailableRooms(availableRooms);

        dto.setMinStay(roomPrice.getMinStay());
        dto.setMaxStay(roomPrice.getMaxStay());
        dto.setCloseRoom(roomPrice.getCloseRoom());
        dto.setCta(roomPrice.getCta());
        dto.setCtd(roomPrice.getCtd());
        dto.setPriceSource(roomPrice.getPriceSource());
        dto.setManualOverride(roomPrice.getManualOverride());
        dto.setManualOverrideUntil(roomPrice.getManualOverrideUntil());
        dto.setIsWeekend(roomPrice.getIsWeekend());
        dto.setIsHoliday(roomPrice.getIsHoliday());
        dto.setNotes(roomPrice.getNotes());

        if (roomPrice.getPricePlan() != null && roomPrice.getPricePlan().getId() != null) {
            List<ChannelPrice> priceLabsCandidates = channelPriceRepository
                    .findPriceLabsCandidatesByStoreAndRoomTypeAndPricePlanAndDate(
                            storeId,
                            roomPrice.getRoomType().getId(),
                            roomPrice.getPricePlan().getId(),
                            roomPrice.getPriceDate()
                    );
            if (!priceLabsCandidates.isEmpty()) {
                applyPriceLabsMetadata(dto, priceLabsCandidates.get(0));
            }
        }

        return dto;
    }

    private Map<String, ChannelPrice> buildPriceLabsPriceMap(List<ChannelPrice> channelPrices) {
        Map<String, ChannelPrice> result = new HashMap<>();
        if (channelPrices == null || channelPrices.isEmpty()) {
            return result;
        }

        for (ChannelPrice cp : channelPrices) {
            if (cp == null
                    || cp.getRoomType() == null
                    || cp.getRoomType().getId() == null
                    || cp.getPricePlan() == null
                    || cp.getPricePlan().getId() == null
                    || cp.getPriceDate() == null) {
                continue;
            }
            if (cp.getBasePrice() == null && cp.getPriceLabsUpdatedAt() == null) {
                continue;
            }

            String key = buildPriceLabsKey(cp.getRoomType().getId(), cp.getPricePlan().getId(), cp.getPriceDate());
            ChannelPrice existing = result.get(key);
            if (existing == null || isLater(cp.getPriceLabsUpdatedAt(), existing.getPriceLabsUpdatedAt())) {
                result.put(key, cp);
            }
        }

        return result;
    }

    private static boolean isLater(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return false;
        }
        if (right == null) {
            return true;
        }
        return left.isAfter(right);
    }

    private static String buildPriceLabsKey(Long roomTypeId, Long pricePlanId, LocalDate date) {
        return roomTypeId + "_" + pricePlanId + "_" + date;
    }

    private void applyPriceLabsMetadata(RoomPriceManagementDTO dto, ChannelPrice priceLabsPrice) {
        if (dto == null || priceLabsPrice == null) {
            return;
        }

        dto.setPriceLabsBasePrice(priceLabsPrice.getBasePrice());
        dto.setPriceLabsUpdatedAt(priceLabsPrice.getPriceLabsUpdatedAt());

        if (!Boolean.TRUE.equals(dto.getManualOverride())
                && priceLabsPrice.getBasePrice() != null
                && (dto.getPriceSource() == null || RoomPrice.PRICE_SOURCE_SYSTEM.equals(dto.getPriceSource()))) {
            dto.setPriceSource(RoomPrice.PRICE_SOURCE_PRICELABS);
        }
    }

    /**
     * 检查日期是否符合指定的星期几
     */
    private boolean shouldApplyPriceForWeekday(LocalDate date, List<Integer> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) {
            return true;
        }

        int dayOfWeek = date.getDayOfWeek().getValue();
        int weekdayValue = (dayOfWeek == 7) ? 0 : dayOfWeek;

        return weekdays.contains(weekdayValue);
    }

    /**
     * 格式化星期几列表为字符串
     */
    private String formatWeekdays(List<Integer> weekdays) {
        if (weekdays == null || weekdays.isEmpty() || weekdays.contains(0)) {
            return "全部";
        }

        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekdays.stream()
                .sorted()
                .map(day -> dayNames[day])
                .collect(Collectors.joining(","));
    }
}
