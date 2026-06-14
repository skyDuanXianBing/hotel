package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.dto.RoomTableDataDTO;
import server.demo.dto.RoomTableMonthlyResponse;
import server.demo.dto.RoomTableStatisticsDTO;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class RoomTableService {
    private static final int MAX_MONTHLY_DAYS = 62;
    private static final String DISPLAY_STATUS_FULL = "FULL";
    private static final String DISPLAY_STATUS_AVAILABLE = "AVAILABLE";
    private static final String DISPLAY_STATUS_AVAILABLE_MANY = "AVAILABLE_MANY";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String UNKNOWN_ROOM_TYPE_NAME = "未知房型";
    private static final String BLOCKED_REASON_RESERVATION = "RESERVATION";
    private static final String BLOCKED_REASON_BLOCKOUT = "BLOCKOUT";
    private static final String BLOCKED_REASON_STATIC_STATUS = "STATIC_STATUS";
    private static final String BLOCKED_REASON_CLOSE_ROOM = "CLOSE_ROOM";
    private static final String BLOCKED_REASON_INVENTORY_LIMIT = "INVENTORY_LIMIT";
    private static final String BLOCKED_REASON_UNASSIGNED_RESERVATION = "UNASSIGNED_RESERVATION";
    private static final Set<ReservationStatus> MONTHLY_OCCUPANCY_STATUSES = Set.of(
            ReservationStatus.REQUESTED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN
    );

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomPriceRepository roomPriceRepository;

    @Autowired
    private RoomBlockoutRepository roomBlockoutRepository;

    @Autowired
    private StoreRepository storeRepository;

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

    @Transactional(readOnly = true)
    public RoomTableMonthlyResponse getMonthlyRoomTable(LocalDate startDate, LocalDate endDate, Long roomTypeId) {
        validateMonthlyDateRange(startDate, endDate);

        Long storeId = StoreContextUtils.requireStoreId();
        List<RoomType> roomTypes = findMonthlyRoomTypes(storeId, roomTypeId);
        List<LocalDate> dates = buildMonthlyDates(startDate, endDate);
        if (roomTypes.isEmpty()) {
            return new RoomTableMonthlyResponse(startDate, endDate, List.of(), List.of());
        }

        Set<Long> roomTypeIds = new HashSet<>();
        for (RoomType roomType : roomTypes) {
            if (roomType != null && roomType.getId() != null) {
                roomTypeIds.add(roomType.getId());
            }
        }

        List<Room> rooms = findMonthlyRooms(storeId, roomTypeIds);
        List<Long> roomIds = new ArrayList<>();
        for (Room room : rooms) {
            if (room != null && room.getId() != null) {
                roomIds.add(room.getId());
            }
        }

        Map<Long, List<Room>> roomsByType = buildRoomsByType(rooms);
        Map<String, Reservation> reservationByRoomDate = buildAssignedReservationLookup(
                storeId,
                roomIds,
                startDate,
                endDate
        );
        Map<String, RoomBlockout> blockoutByRoomDate = buildBlockoutLookup(
                storeId,
                roomIds,
                startDate,
                endDate
        );
        Map<String, Integer> unassignedByTypeDate = buildUnassignedOccupancyLookup(
                storeId,
                roomTypeIds,
                startDate,
                endDate
        );
        Map<String, PriceRestriction> restrictionByTypeDate = buildPriceRestrictionLookup(
                storeId,
                roomTypeIds,
                startDate,
                endDate
        );

        Map<String, RoomTypeDateCalculation> calculationByTypeDate = new HashMap<>();
        List<RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO> summaries = new ArrayList<>();
        for (RoomType roomType : roomTypes) {
            if (roomType == null || roomType.getId() == null) {
                continue;
            }
            List<Room> roomsOfType = roomsByType.getOrDefault(roomType.getId(), List.of());
            for (LocalDate date : dates) {
                String typeDateKey = roomTypeDateKey(roomType.getId(), date);
                PriceRestriction restriction = restrictionByTypeDate.getOrDefault(typeDateKey, new PriceRestriction());
                int unassignedRooms = unassignedByTypeDate.getOrDefault(typeDateKey, 0);
                RoomTypeDateCalculation calculation = calculateRoomTypeDate(
                        roomType,
                        roomsOfType,
                        date,
                        reservationByRoomDate,
                        blockoutByRoomDate,
                        restriction,
                        unassignedRooms
                );
                calculationByTypeDate.put(typeDateKey, calculation);
                summaries.add(toMonthlySummary(calculation));
            }
        }

        Map<String, Integer> remainingSlotsByTypeDate = new HashMap<>();
        for (Map.Entry<String, RoomTypeDateCalculation> entry : calculationByTypeDate.entrySet()) {
            remainingSlotsByTypeDate.put(entry.getKey(), entry.getValue().effectiveAvailableRooms);
        }

        List<RoomTableMonthlyResponse.MonthlyRoomDataDTO> roomData = new ArrayList<>();
        for (Room room : rooms) {
            RoomType roomType = room.getRoomType();
            Long currentRoomTypeId = roomType != null ? roomType.getId() : null;
            String roomTypeName = roomType != null ? roomType.getName() : UNKNOWN_ROOM_TYPE_NAME;
            List<RoomTableMonthlyResponse.MonthlyDailyStatusDTO> dailyStatuses = new ArrayList<>();
            for (LocalDate date : dates) {
                RoomTypeDateCalculation calculation = calculationByTypeDate.get(
                        roomTypeDateKey(currentRoomTypeId, date)
                );
                dailyStatuses.add(toMonthlyDailyStatus(
                        room,
                        date,
                        calculation,
                        reservationByRoomDate,
                        blockoutByRoomDate,
                        remainingSlotsByTypeDate
                ));
            }
            roomData.add(new RoomTableMonthlyResponse.MonthlyRoomDataDTO(
                    room.getId(),
                    room.getRoomNumber(),
                    currentRoomTypeId,
                    roomTypeName,
                    dailyStatuses
            ));
        }

        return new RoomTableMonthlyResponse(startDate, endDate, roomData, summaries);
    }

    private void validateMonthlyDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_MONTHLY_DAYS) {
            throw new IllegalArgumentException("月度房情查询范围不能超过 " + MAX_MONTHLY_DAYS + " 天");
        }
    }

    private List<RoomType> findMonthlyRoomTypes(Long storeId, Long roomTypeId) {
        if (roomTypeId != null) {
            return roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId)
                    .map(List::of)
                    .orElse(List.of());
        }
        return roomTypeRepository.findByStoreIdOrderByName(storeId);
    }

    private List<LocalDate> buildMonthlyDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return dates;
    }

    private List<Room> findMonthlyRooms(Long storeId, Set<Long> roomTypeIds) {
        List<Room> allRooms = roomRepository.findByStoreIdWithRoomType(storeId);
        List<Room> rooms = new ArrayList<>();
        for (Room room : allRooms) {
            if (room == null || room.getId() == null || room.getRoomType() == null) {
                continue;
            }
            Long roomTypeId = room.getRoomType().getId();
            if (roomTypeId == null || !roomTypeIds.contains(roomTypeId)) {
                continue;
            }
            rooms.add(room);
        }
        return rooms;
    }

    private Map<Long, List<Room>> buildRoomsByType(List<Room> rooms) {
        Map<Long, List<Room>> roomsByType = new HashMap<>();
        for (Room room : rooms) {
            if (room == null || room.getRoomType() == null || room.getRoomType().getId() == null) {
                continue;
            }
            Long roomTypeId = room.getRoomType().getId();
            roomsByType.computeIfAbsent(roomTypeId, ignored -> new ArrayList<>()).add(room);
        }
        return roomsByType;
    }

    private Map<String, Reservation> buildAssignedReservationLookup(
            Long storeId,
            List<Long> roomIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<String, Reservation> lookup = new HashMap<>();
        if (roomIds.isEmpty()) {
            return lookup;
        }

        List<Reservation> reservations = reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatusesWithChannel(
                storeId,
                roomIds,
                startDate,
                endDate,
                MONTHLY_OCCUPANCY_STATUSES
        );
        for (Reservation reservation : reservations) {
            if (reservation == null || reservation.getRoom() == null || reservation.getRoom().getId() == null) {
                continue;
            }
            if (reservation.getCheckInDate() == null || reservation.getCheckOutDate() == null) {
                continue;
            }
            LocalDate currentDate = laterDate(reservation.getCheckInDate(), startDate);
            LocalDate lastOccupiedDate = earlierDate(reservation.getCheckOutDate().minusDays(1), endDate);
            while (!currentDate.isAfter(lastOccupiedDate)) {
                String key = roomDateKey(reservation.getRoom().getId(), currentDate);
                Reservation existing = lookup.get(key);
                if (shouldReplacePrimaryReservation(existing, reservation)) {
                    lookup.put(key, reservation);
                }
                currentDate = currentDate.plusDays(1);
            }
        }
        return lookup;
    }

    private Map<String, RoomBlockout> buildBlockoutLookup(
            Long storeId,
            List<Long> roomIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<String, RoomBlockout> lookup = new HashMap<>();
        if (roomIds.isEmpty()) {
            return lookup;
        }

        List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                storeId,
                roomIds,
                startDate,
                endDate
        );
        for (RoomBlockout blockout : blockouts) {
            if (blockout == null || blockout.getRoom() == null || blockout.getRoom().getId() == null) {
                continue;
            }
            if (blockout.getBlockDate() == null) {
                continue;
            }
            lookup.put(roomDateKey(blockout.getRoom().getId(), blockout.getBlockDate()), blockout);
        }
        return lookup;
    }

    private Map<String, Integer> buildUnassignedOccupancyLookup(
            Long storeId,
            Set<Long> roomTypeIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<String, Integer> lookup = new HashMap<>();
        List<ReservationRepository.MonthlyReservationOccupancyRow> rows = reservationRepository
                .findMonthlyOccupancyRowsByStoreIdAndDateRangeAndStatuses(
                        storeId,
                        startDate,
                        endDate.plusDays(1),
                        MONTHLY_OCCUPANCY_STATUSES
                );
        for (ReservationRepository.MonthlyReservationOccupancyRow row : rows) {
            if (row == null || row.getRoomId() != null || row.getOtaRoomTypeId() == null) {
                continue;
            }
            if (!roomTypeIds.contains(row.getOtaRoomTypeId())) {
                continue;
            }
            if (row.getCheckInDate() == null || row.getCheckOutDate() == null) {
                continue;
            }
            addRoomTypeDateCounts(
                    lookup,
                    row.getOtaRoomTypeId(),
                    row.getCheckInDate(),
                    row.getCheckOutDate(),
                    startDate,
                    endDate
            );
        }
        return lookup;
    }

    private Map<String, PriceRestriction> buildPriceRestrictionLookup(
            Long storeId,
            Set<Long> roomTypeIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<String, PriceRestriction> lookup = new HashMap<>();
        List<RoomPrice> roomPrices = roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                storeId,
                startDate,
                endDate
        );
        for (RoomPrice roomPrice : roomPrices) {
            if (roomPrice == null || roomPrice.getRoomType() == null || roomPrice.getRoomType().getId() == null) {
                continue;
            }
            if (roomPrice.getPriceDate() == null) {
                continue;
            }
            Long roomTypeId = roomPrice.getRoomType().getId();
            if (!roomTypeIds.contains(roomTypeId)) {
                continue;
            }

            String key = roomTypeDateKey(roomTypeId, roomPrice.getPriceDate());
            PriceRestriction restriction = lookup.computeIfAbsent(key, ignored -> new PriceRestriction());
            restriction.merge(roomPrice);
        }
        return lookup;
    }

    private RoomTypeDateCalculation calculateRoomTypeDate(
            RoomType roomType,
            List<Room> rooms,
            LocalDate date,
            Map<String, Reservation> reservationByRoomDate,
            Map<String, RoomBlockout> blockoutByRoomDate,
            PriceRestriction restriction,
            int unassignedRooms
    ) {
        RoomTypeDateCalculation calculation = new RoomTypeDateCalculation();
        calculation.roomTypeId = roomType.getId();
        calculation.roomTypeName = roomType.getName();
        calculation.date = date;
        calculation.totalRooms = rooms.size();
        calculation.unassignedOccupiedRooms = Math.max(unassignedRooms, 0);
        calculation.inventoryLimit = restriction.inventoryLimit;
        calculation.closeRoom = restriction.closeRoom;
        calculation.cta = restriction.cta;
        calculation.ctd = restriction.ctd;

        for (Room room : rooms) {
            String key = roomDateKey(room.getId(), date);
            if (reservationByRoomDate.containsKey(key)) {
                calculation.assignedOccupiedRooms++;
                continue;
            }
            if (blockoutByRoomDate.containsKey(key)) {
                calculation.blockoutRooms++;
                continue;
            }
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                calculation.staticUnavailableRooms++;
                continue;
            }
            calculation.physicalSellableRooms++;
        }

        int available = calculation.physicalSellableRooms;
        if (restriction.inventoryLimit != null && restriction.inventoryLimit < available) {
            available = restriction.inventoryLimit;
        }
        if (restriction.closeRoom) {
            available = 0;
        }
        available = available - calculation.unassignedOccupiedRooms;
        calculation.effectiveAvailableRooms = Math.max(available, 0);
        return calculation;
    }

    private RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO toMonthlySummary(
            RoomTypeDateCalculation calculation
    ) {
        RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO summary =
                new RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO();
        summary.setRoomTypeId(calculation.roomTypeId);
        summary.setRoomTypeName(calculation.roomTypeName);
        summary.setDate(calculation.date);
        summary.setTotalRooms(calculation.totalRooms);
        summary.setPhysicalSellableRooms(calculation.physicalSellableRooms);
        summary.setAssignedOccupiedRooms(calculation.assignedOccupiedRooms);
        summary.setBlockoutRooms(calculation.blockoutRooms);
        summary.setStaticUnavailableRooms(calculation.staticUnavailableRooms);
        summary.setUnassignedOccupiedRooms(calculation.unassignedOccupiedRooms);
        summary.setInventoryLimit(calculation.inventoryLimit);
        summary.setEffectiveAvailableRooms(calculation.effectiveAvailableRooms);
        summary.setCloseRoom(calculation.closeRoom);
        summary.setCta(calculation.cta);
        summary.setCtd(calculation.ctd);
        return summary;
    }

    private RoomTableMonthlyResponse.MonthlyDailyStatusDTO toMonthlyDailyStatus(
            Room room,
            LocalDate date,
            RoomTypeDateCalculation calculation,
            Map<String, Reservation> reservationByRoomDate,
            Map<String, RoomBlockout> blockoutByRoomDate,
            Map<String, Integer> remainingSlotsByTypeDate
    ) {
        RoomTableMonthlyResponse.MonthlyDailyStatusDTO status =
                new RoomTableMonthlyResponse.MonthlyDailyStatusDTO();
        status.setDate(date);
        status.setDisplayStatus(DISPLAY_STATUS_FULL);
        status.setSellable(false);

        if (calculation != null) {
            status.setRoomTypeAvailableRooms(calculation.effectiveAvailableRooms);
            status.setCloseRoom(calculation.closeRoom);
            status.setCta(calculation.cta);
            status.setCtd(calculation.ctd);
        }

        String roomDateKey = roomDateKey(room.getId(), date);
        Reservation reservation = reservationByRoomDate.get(roomDateKey);
        if (reservation != null) {
            status.setStatus(statusForReservation(reservation));
            status.setReservation(toReservationInfo(reservation));
            status.setBlockedReason(BLOCKED_REASON_RESERVATION);
            return status;
        }

        RoomBlockout blockout = blockoutByRoomDate.get(roomDateKey);
        if (blockout != null) {
            status.setStatus(STATUS_BLOCKED);
            status.setClosed(true);
            if (blockout.getBlockType() != null) {
                status.setCloseType(blockout.getBlockType().toUiValue());
            }
            status.setCloseRemark(blockout.getRemark());
            status.setBlockedReason(BLOCKED_REASON_BLOCKOUT);
            return status;
        }

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            if (room.getStatus() != null) {
                status.setStatus(room.getStatus().name());
            } else {
                status.setStatus(STATUS_UNKNOWN);
            }
            status.setBlockedReason(BLOCKED_REASON_STATIC_STATUS);
            return status;
        }

        status.setStatus(RoomStatus.AVAILABLE.name());
        if (calculation == null) {
            status.setBlockedReason(BLOCKED_REASON_INVENTORY_LIMIT);
            return status;
        }

        String typeDateKey = roomTypeDateKey(calculation.roomTypeId, date);
        int remainingSlots = remainingSlotsByTypeDate.getOrDefault(typeDateKey, 0);
        if (remainingSlots > 0) {
            status.setSellable(true);
            status.setDisplayStatus(displayStatusForAvailable(calculation.effectiveAvailableRooms));
            remainingSlotsByTypeDate.put(typeDateKey, remainingSlots - 1);
            return status;
        }

        status.setBlockedReason(resolveInventoryBlockedReason(calculation));
        return status;
    }

    private String displayStatusForAvailable(int effectiveAvailableRooms) {
        if (effectiveAvailableRooms >= 2) {
            return DISPLAY_STATUS_AVAILABLE_MANY;
        }
        return DISPLAY_STATUS_AVAILABLE;
    }

    private String resolveInventoryBlockedReason(RoomTypeDateCalculation calculation) {
        if (calculation.closeRoom) {
            return BLOCKED_REASON_CLOSE_ROOM;
        }
        if (calculation.inventoryLimit != null && calculation.inventoryLimit < calculation.physicalSellableRooms) {
            return BLOCKED_REASON_INVENTORY_LIMIT;
        }
        if (calculation.unassignedOccupiedRooms > 0) {
            return BLOCKED_REASON_UNASSIGNED_RESERVATION;
        }
        return BLOCKED_REASON_INVENTORY_LIMIT;
    }

    private String statusForReservation(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            return RoomStatus.OCCUPIED.name();
        }
        return RoomStatus.RESERVED.name();
    }

    private DailyRoomStatusDTO.ReservationInfoDTO toReservationInfo(Reservation reservation) {
        String channelName = "";
        if (reservation.getChannel() != null && reservation.getChannel().getName() != null) {
            channelName = reservation.getChannel().getName();
        }
        DailyRoomStatusDTO.ReservationInfoDTO info = new DailyRoomStatusDTO.ReservationInfoDTO(
                reservation.getId(),
                reservation.getGuestName(),
                channelName,
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getOrderNumber()
        );
        if (reservation.getStatus() != null) {
            info.setStatus(reservation.getStatus().name());
        }
        info.setTotalAmount(reservation.getTotalAmount());
        info.setGroupOrderNo(reservation.getGroupOrderNo());
        info.setNotes(reservation.getNotes());
        info.setSpecialRequests(reservation.getSpecialRequests());
        return info;
    }

    private void addRoomTypeDateCounts(
            Map<String, Integer> lookup,
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate currentDate = laterDate(checkInDate, startDate);
        LocalDate lastOccupiedDate = earlierDate(checkOutDate.minusDays(1), endDate);
        while (!currentDate.isAfter(lastOccupiedDate)) {
            String key = roomTypeDateKey(roomTypeId, currentDate);
            lookup.put(key, lookup.getOrDefault(key, 0) + 1);
            currentDate = currentDate.plusDays(1);
        }
    }

    private boolean shouldReplacePrimaryReservation(Reservation existing, Reservation candidate) {
        if (existing == null) {
            return true;
        }

        int existingPriority = reservationStatusPriority(existing.getStatus());
        int candidatePriority = reservationStatusPriority(candidate.getStatus());
        if (candidatePriority != existingPriority) {
            return candidatePriority > existingPriority;
        }

        LocalDateTime existingCreatedAt = existing.getCreatedAt();
        LocalDateTime candidateCreatedAt = candidate.getCreatedAt();
        if (candidateCreatedAt != null && existingCreatedAt != null
                && !candidateCreatedAt.equals(existingCreatedAt)) {
            return candidateCreatedAt.isAfter(existingCreatedAt);
        }
        if (candidateCreatedAt != null && existingCreatedAt == null) {
            return true;
        }
        if (candidate.getId() == null || existing.getId() == null) {
            return false;
        }
        return candidate.getId() > existing.getId();
    }

    private int reservationStatusPriority(ReservationStatus status) {
        if (status == ReservationStatus.CHECKED_IN) {
            return 3;
        }
        if (status == ReservationStatus.CONFIRMED) {
            return 2;
        }
        if (status == ReservationStatus.REQUESTED) {
            return 1;
        }
        return 0;
    }

    private LocalDate laterDate(LocalDate first, LocalDate second) {
        if (first.isAfter(second)) {
            return first;
        }
        return second;
    }

    private LocalDate earlierDate(LocalDate first, LocalDate second) {
        if (first.isBefore(second)) {
            return first;
        }
        return second;
    }

    private String roomDateKey(Long roomId, LocalDate date) {
        return roomId + "|" + date;
    }

    private String roomTypeDateKey(Long roomTypeId, LocalDate date) {
        return roomTypeId + "|" + date;
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
            BusinessDayWindow targetWindow = storeDayWindow(storeId, date);
            List<Reservation> todayCancelledReservations = reservationRepository.findCancelledByStoreAndRoomTypeAndDate(
                storeId, roomType.getId(), targetWindow.start(), targetWindow.end());
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

    private BusinessDayWindow storeDayWindow(Long storeId, LocalDate businessDate) {
        ZoneId zoneId = resolveStoreZoneId(storeId);
        LocalDateTime start = StoreTimeZoneUtil.toReservationTimestampStorageLocalDateTime(
                businessDate,
                LocalTime.MIDNIGHT,
                zoneId
        );
        LocalDateTime end = StoreTimeZoneUtil.toReservationTimestampStorageLocalDateTime(
                businessDate.plusDays(1),
                LocalTime.MIDNIGHT,
                zoneId
        );
        return new BusinessDayWindow(start, end);
    }

    private ZoneId resolveStoreZoneId(Long storeId) {
        Store store = storeId == null || storeRepository == null ? null : storeRepository.findById(storeId).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
    }

    private static class PriceRestriction {
        private Integer inventoryLimit;
        private boolean closeRoom;
        private boolean cta;
        private boolean ctd;

        private void merge(RoomPrice roomPrice) {
            Integer availableRooms = roomPrice.getAvailableRooms();
            if (availableRooms != null && availableRooms >= 0) {
                if (inventoryLimit == null || availableRooms < inventoryLimit) {
                    inventoryLimit = availableRooms;
                }
            }
            if (Boolean.TRUE.equals(roomPrice.getCloseRoom())) {
                closeRoom = true;
            }
            if (Boolean.TRUE.equals(roomPrice.getCta())) {
                cta = true;
            }
            if (Boolean.TRUE.equals(roomPrice.getCtd())) {
                ctd = true;
            }
        }
    }

    private static class RoomTypeDateCalculation {
        private Long roomTypeId;
        private String roomTypeName;
        private LocalDate date;
        private int totalRooms;
        private int physicalSellableRooms;
        private int assignedOccupiedRooms;
        private int blockoutRooms;
        private int staticUnavailableRooms;
        private int unassignedOccupiedRooms;
        private Integer inventoryLimit;
        private int effectiveAvailableRooms;
        private boolean closeRoom;
        private boolean cta;
        private boolean ctd;
    }

    private static class BusinessDayWindow {
        private final LocalDateTime start;
        private final LocalDateTime end;

        private BusinessDayWindow(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        private LocalDateTime start() {
            return start;
        }

        private LocalDateTime end() {
            return end;
        }
    }
}
