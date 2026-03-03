package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.dto.OpenRoomBlockoutRequest;
import server.demo.dto.RoomBlockoutSummaryDTO;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.RoomStatusStatisticsDTO;
import server.demo.dto.UpsertRoomBlockoutRequest;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.exception.PermissionDeniedException;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomBlockoutType;
import server.demo.enums.RoomStatus;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.util.StoreContextUtils;

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
public class RoomStatusService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomBlockoutRepository roomBlockoutRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired(required = false)
    private SuAriAutoSyncService suAriAutoSyncService;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
    }

    public RoomStatusCalendarDTO getRoomStatusCalendar(LocalDate startDate, LocalDate endDate) {
        Long storeId = currentStoreId();

        if (!permissionService.isEnforcementEnabled()) {
            return getRoomStatusCalendarForStore(storeId, startDate, endDate);
        }

        Long userId = currentUserId();
        PermissionService.RoomTypeScope scope = permissionService.resolveRoomTypeScope(
                storeId,
                userId,
                PermissionModule.ACCOMMODATION,
                PermissionAction.VIEW_ROOM_STATUS
        );
        if (scope.isEmpty()) {
            throw new PermissionDeniedException("您没有权限查看房态");
        }

        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        if (!scope.isAllRoomTypes()) {
            Set<Long> allowedRoomTypeIds = scope.getRoomTypeIds();
            rooms = rooms.stream()
                    .filter(r -> r != null && r.getRoomType() != null && allowedRoomTypeIds.contains(r.getRoomType().getId()))
                    .toList();
        }

        return buildRoomStatusCalendar(storeId, startDate, endDate, rooms);
    }

    public RoomStatusCalendarDTO getRoomStatusCalendarForStore(Long storeId, LocalDate startDate, LocalDate endDate) {
        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        return buildRoomStatusCalendar(storeId, startDate, endDate, rooms);
    }

    private RoomStatusCalendarDTO buildRoomStatusCalendar(Long storeId, LocalDate startDate, LocalDate endDate, List<Room> rooms) {
        List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .filter(id -> id != null)
                .toList();

        Map<String, RoomBlockout> blockoutByKey = new HashMap<>();
        if (!roomIds.isEmpty() && startDate != null && endDate != null && !endDate.isBefore(startDate)) {
            List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                    storeId,
                    roomIds,
                    startDate,
                    endDate
            );
            for (RoomBlockout b : blockouts) {
                if (b == null || b.getRoom() == null || b.getRoom().getId() == null || b.getBlockDate() == null) {
                    continue;
                }
                blockoutByKey.put(blockoutKey(b.getRoom().getId(), b.getBlockDate()), b);
            }
        }

        RoomStatusCalendarDTO.DateRangeDTO dateRange = new RoomStatusCalendarDTO.DateRangeDTO(startDate, endDate);
        List<RoomStatusCalendarDTO.CalendarRoomDataDTO> roomDataList = new ArrayList<>();

        for (Room room : rooms) {
            List<DailyRoomStatusDTO> dailyStatusList = new ArrayList<>();
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                RoomStatus status = determineRoomStatus(room, currentDate, storeId);
                DailyRoomStatusDTO.ReservationInfoDTO reservationInfo = getReservationInfo(room, currentDate, storeId);

                RoomBlockout blockout = room != null && room.getId() != null
                        ? blockoutByKey.get(blockoutKey(room.getId(), currentDate))
                        : null;

                Boolean closed = blockout != null;
                String closeType = blockout != null && blockout.getBlockType() != null
                        ? blockout.getBlockType().toUiValue()
                        : null;
                String closeRemark = blockout != null ? blockout.getRemark() : null;

                dailyStatusList.add(new DailyRoomStatusDTO(currentDate, status, reservationInfo, closed, closeType, closeRemark));
                currentDate = currentDate.plusDays(1);
            }

            String roomTypeName = room.getRoomType() != null ? room.getRoomType().getName() : "未知房型";
            RoomStatusCalendarDTO.CalendarRoomDataDTO roomData = new RoomStatusCalendarDTO.CalendarRoomDataDTO(
                    room.getId(),
                    room.getRoomNumber(),
                    roomTypeName,
                    dailyStatusList
            );
            roomDataList.add(roomData);
        }

        return new RoomStatusCalendarDTO(dateRange, roomDataList);
    }

    public RoomBlockoutSummaryDTO closeRooms(UpsertRoomBlockoutRequest request) {
        Long storeId = currentStoreId();
        if (request == null) {
            throw new RuntimeException("请求不能为空");
        }
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new RuntimeException("日期范围不合法");
        }
        RoomBlockoutType type = RoomBlockoutType.fromUiValue(request.getType());
        if (type == null) {
            throw new RuntimeException("关房类型不合法: " + request.getType());
        }
        List<Long> roomIds = request.getRoomIds();
        if (roomIds == null || roomIds.isEmpty()) {
            throw new RuntimeException("roomIds 不能为空");
        }

        List<Room> rooms = roomRepository.findByStoreIdAndIdIn(storeId, roomIds);
        if (rooms.size() != roomIds.stream().filter(id -> id != null).collect(Collectors.toSet()).size()) {
            throw new RuntimeException("部分房间不存在或无权限");
        }

        if (permissionService.isEnforcementEnabled()) {
            Long userId = currentUserId();
            PermissionService.RoomTypeScope scope = permissionService.resolveRoomTypeScope(
                    storeId,
                    userId,
                    PermissionModule.ACCOMMODATION,
                    PermissionAction.VIEW_ROOM_STATUS
            );
            if (scope.isEmpty()) {
                throw new PermissionDeniedException("您没有权限操作房态");
            }
            if (!scope.isAllRoomTypes()) {
                Set<Long> allowedRoomTypeIds = scope.getRoomTypeIds();
                boolean anyForbidden = rooms.stream().anyMatch(r -> r == null
                        || r.getRoomType() == null
                        || r.getRoomType().getId() == null
                        || !allowedRoomTypeIds.contains(r.getRoomType().getId()));
                if (anyForbidden) {
                    throw new PermissionDeniedException("您没有权限操作该房型的房态");
                }
            }
        }

        // 不允许对已有阻塞预订的日期关房，避免破坏在住/已确认订单
        List<Long> normalizedRoomIds = rooms.stream().map(Room::getId).filter(id -> id != null).toList();
        Set<ReservationStatus> blocking = Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.REQUESTED);
        List<Reservation> conflicts = reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                storeId,
                normalizedRoomIds,
                startDate,
                endDate,
                blocking
        );
        if (conflicts != null && !conflicts.isEmpty()) {
            String detail = conflicts.stream()
                    .limit(20)
                    .map(r -> "roomId=" + (r.getRoom() != null ? r.getRoom().getId() : "null") +
                            ", " + r.getCheckInDate() + "~" + r.getCheckOutDate() +
                            ", status=" + r.getStatus() +
                            ", order=" + r.getOrderNumber())
                    .collect(Collectors.joining("; "));
            throw new RuntimeException("所选日期范围内存在预订，无法关房。示例: " + detail);
        }

        Map<String, RoomBlockout> existingByKey = new HashMap<>();
        List<RoomBlockout> existing = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                storeId,
                normalizedRoomIds,
                startDate,
                endDate
        );
        for (RoomBlockout b : existing) {
            if (b == null || b.getRoom() == null || b.getRoom().getId() == null || b.getBlockDate() == null) {
                continue;
            }
            existingByKey.put(blockoutKey(b.getRoom().getId(), b.getBlockDate()), b);
        }

        long affected = 0;
        LocalDate d = startDate;
        List<RoomBlockout> toSave = new ArrayList<>();
        while (!d.isAfter(endDate)) {
            for (Room room : rooms) {
                String key = blockoutKey(room.getId(), d);
                RoomBlockout b = existingByKey.get(key);
                if (b == null) {
                    b = new RoomBlockout(storeId, room, d, type, request.getRemark());
                } else {
                    b.setBlockType(type);
                    b.setRemark(request.getRemark());
                }
                toSave.add(b);
                affected++;
            }
            d = d.plusDays(1);
        }
        roomBlockoutRepository.saveAll(toSave);
        if (suAriAutoSyncService != null) {
            Set<Long> roomTypeIds = rooms.stream()
                    .filter(r -> r != null && r.getRoomType() != null && r.getRoomType().getId() != null)
                    .map(r -> r.getRoomType().getId())
                    .collect(Collectors.toSet());
            suAriAutoSyncService.enqueueForStoreScope(
                    storeId,
                    "room_blockout_close",
                    startDate,
                    endDate,
                    roomTypeIds,
                    null,
                    true,
                    false,
                    true,
                    true
            );
        }
        return new RoomBlockoutSummaryDTO(affected);
    }

    public RoomBlockoutSummaryDTO openRooms(OpenRoomBlockoutRequest request) {
        Long storeId = currentStoreId();
        if (request == null) {
            throw new RuntimeException("请求不能为空");
        }
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new RuntimeException("日期范围不合法");
        }
        List<Long> roomIds = request.getRoomIds();
        if (roomIds == null || roomIds.isEmpty()) {
            throw new RuntimeException("roomIds 不能为空");
        }

        // 校验房间属于当前门店
        List<Room> rooms = roomRepository.findByStoreIdAndIdIn(storeId, roomIds);
        if (rooms.size() != roomIds.stream().filter(id -> id != null).collect(Collectors.toSet()).size()) {
            throw new RuntimeException("部分房间不存在或无权限");
        }

        if (permissionService.isEnforcementEnabled()) {
            Long userId = currentUserId();
            PermissionService.RoomTypeScope scope = permissionService.resolveRoomTypeScope(
                    storeId,
                    userId,
                    PermissionModule.ACCOMMODATION,
                    PermissionAction.VIEW_ROOM_STATUS
            );
            if (scope.isEmpty()) {
                throw new PermissionDeniedException("您没有权限操作房态");
            }
            if (!scope.isAllRoomTypes()) {
                Set<Long> allowedRoomTypeIds = scope.getRoomTypeIds();
                boolean anyForbidden = rooms.stream().anyMatch(r -> r == null
                        || r.getRoomType() == null
                        || r.getRoomType().getId() == null
                        || !allowedRoomTypeIds.contains(r.getRoomType().getId()));
                if (anyForbidden) {
                    throw new PermissionDeniedException("您没有权限操作该房型的房态");
                }
            }
        }
        List<Long> normalizedRoomIds = rooms.stream().map(Room::getId).filter(id -> id != null).toList();

        long deleted = roomBlockoutRepository.deleteByStoreIdAndRoom_IdInAndBlockDateBetween(
                storeId,
                normalizedRoomIds,
                startDate,
                endDate
        );
        if (suAriAutoSyncService != null && deleted > 0) {
            Set<Long> roomTypeIds = rooms.stream()
                    .filter(r -> r != null && r.getRoomType() != null && r.getRoomType().getId() != null)
                    .map(r -> r.getRoomType().getId())
                    .collect(Collectors.toSet());
            suAriAutoSyncService.enqueueForStoreScope(
                    storeId,
                    "room_blockout_open",
                    startDate,
                    endDate,
                    roomTypeIds,
                    null,
                    true,
                    false,
                    true,
                    true
            );
        }
        return new RoomBlockoutSummaryDTO(deleted);
    }

    public void updateRoomStatus(Long roomId, LocalDate date, RoomStatus newStatus, String reason) {
        Long storeId = currentStoreId();
        Room room = roomRepository.findByStoreIdAndIdWithRoomType(storeId, roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在或无权限"));

        if (permissionService.isEnforcementEnabled()) {
            Long userId = currentUserId();
            Long roomTypeId = room.getRoomType() != null ? room.getRoomType().getId() : null;

            boolean canEdit = permissionService.hasPermission(
                    storeId,
                    userId,
                    PermissionModule.ACCOMMODATION,
                    PermissionAction.EDIT_ROOM_STATUS
            );
            boolean canViewRoomType = permissionService.hasPermission(
                    storeId,
                    userId,
                    PermissionModule.ACCOMMODATION,
                    PermissionAction.VIEW_ROOM_STATUS,
                    roomTypeId
            );
            if (!canEdit || !canViewRoomType) {
                throw new PermissionDeniedException("您没有权限修改该房型的房态");
            }
        }

        Optional<Reservation> existingReservation =
                reservationRepository.findByStoreIdAndRoomIdAndDate(storeId, room.getId(), date);

        if (existingReservation.isPresent() &&
                (newStatus == RoomStatus.AVAILABLE || newStatus == RoomStatus.MAINTENANCE)) {
            throw new RuntimeException("该日期已有预订，无法修改为该状态");
        }

        if (isDateToday(date)) {
            room.setStatus(newStatus);
            roomRepository.save(room);
            if (suAriAutoSyncService != null) {
                Long rtId = room.getRoomType() != null ? room.getRoomType().getId() : null;
                if (rtId != null) {
                    suAriAutoSyncService.enqueueForStoreScope(
                            storeId,
                            "room_status_update_today",
                            date,
                            date,
                            Set.of(rtId),
                            null,
                            true,
                            false,
                            false,
                            false
                    );
                } else {
                    suAriAutoSyncService.enqueueForStore(storeId, "room_status_update_today");
                }
            }
        }

        // TODO: 后续可在此记录房态变更日志
    }

    public RoomStatusStatisticsDTO getRoomStatusStatistics(LocalDate date) {
        if (permissionService.isEnforcementEnabled()) {
            Long storeId = currentStoreId();
            Long userId = currentUserId();
            PermissionService.RoomTypeScope scope = permissionService.resolveRoomTypeScope(
                    storeId,
                    userId,
                    PermissionModule.ACCOMMODATION,
                    PermissionAction.VIEW_ROOM_STATUS
            );
            if (scope.isEmpty()) {
                throw new PermissionDeniedException("您没有权限查看房态");
            }
        }
        return getRoomStatusStatisticsForStore(currentStoreId(), date);
    }

    public RoomStatusStatisticsDTO getRoomStatusStatisticsForStore(Long storeId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<Room> storeRooms = roomRepository.findByStoreId(storeId);
        List<Long> roomIds = storeRooms.stream().map(Room::getId).collect(Collectors.toList());

        long todayArrivals = reservationRepository.countTodayArrivalsByStoreId(storeId, targetDate);
        long todayDepartures = reservationRepository.countByStoreIdAndCheckOutDate(storeId, targetDate);
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        long todayNewOrders = reservationRepository.countTodayNewOrdersByStoreId(storeId, startOfDay, endOfDay);
        long availableRooms = roomRepository.countAvailableRoomsForDateByStore(storeId, targetDate);
        long unassignedOrders = reservationRepository.countUnassignedOrUnmappedByStoreId(storeId);
        long pendingOrders = reservationRepository.countPendingOrdersByStoreId(storeId);

        return new RoomStatusStatisticsDTO(
                targetDate,
                todayArrivals,
                todayDepartures,
                todayNewOrders,
                availableRooms,
                unassignedOrders,
                pendingOrders
        );
    }

    private RoomStatus determineRoomStatus(Room room, LocalDate date, Long storeId) {
        Optional<Reservation> reservation = reservationRepository.findByStoreIdAndRoomIdAndDate(storeId, room.getId(), date);
        if (reservation.isPresent()) {
            ReservationStatus reservationStatus = reservation.get().getStatus();
            if (reservationStatus == ReservationStatus.CONFIRMED) {
                return RoomStatus.RESERVED;
            }
            if (reservationStatus == ReservationStatus.REQUESTED) {
                return RoomStatus.RESERVED;
            }
            if (reservationStatus == ReservationStatus.CHECKED_IN) {
                return RoomStatus.OCCUPIED;
            }
        }
        if (isDateToday(date)) {
            return room.getStatus();
        }
        return RoomStatus.AVAILABLE;
    }

    private DailyRoomStatusDTO.ReservationInfoDTO getReservationInfo(Room room, LocalDate date, Long storeId) {
        Optional<Reservation> reservation = reservationRepository.findByStoreIdAndRoomIdAndDate(storeId, room.getId(), date);
        if (reservation.isEmpty()) {
            return null;
        }
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

    private boolean isDateToday(LocalDate date) {
        return LocalDate.now().equals(date);
    }

    private static String blockoutKey(Long roomId, LocalDate date) {
        return roomId + "|" + date;
    }
}
