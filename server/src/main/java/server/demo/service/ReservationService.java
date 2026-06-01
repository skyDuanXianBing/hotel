package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import jakarta.persistence.criteria.Predicate;
import server.demo.dto.AssignableRoomDTO;
import server.demo.dto.AssignableRoomTypeDTO;
import server.demo.dto.AssignableRoomsResponse;
import server.demo.dto.BatchCreateReservationRequest;
import server.demo.dto.BatchCreateReservationResponse;
import server.demo.dto.CreateReservationRequest;
import server.demo.dto.OperationLogDetailDTO;
import server.demo.dto.PagedReservationResponse;
import server.demo.dto.ReservationChannelInfoDTO;
import server.demo.dto.ReservationDTO;
import server.demo.dto.ReservationStatistics;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.enums.RoomStatus;
import server.demo.enums.OperationType;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ChannelRepository;
import server.demo.repository.OrderBoxRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import server.demo.util.ReservationSettlementRules;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final String SU_ARI_SOURCE_RESERVATION_CREATE = "reservation_create";
    private static final String SU_ARI_SOURCE_RESERVATION_CHECK_IN = "reservation_check_in";
    private static final String SU_ARI_SOURCE_RESERVATION_CHECK_OUT = "reservation_check_out";
    private static final String SU_ARI_SOURCE_RESERVATION_CANCEL = "reservation_cancel";
    private static final String SU_ARI_SOURCE_RESERVATION_UPDATE = "reservation_update";
    private static final LocalTime UNASSIGNED_CHECK_OUT_CUTOFF_TIME = LocalTime.NOON;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderBoxRepository orderBoxRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private AutoMessageTriggerService autoMessageTriggerService;

    @Autowired
    private PriceLabsReservationSyncService priceLabsReservationSyncService;

    @Autowired(required = false)
    private PriceLabsCalendarSyncDebouncer priceLabsCalendarSyncDebouncer;

    @Autowired
    private CleaningTaskAutoService cleaningTaskAutoService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private OrderNotificationDispatchService orderNotificationDispatchService;

    @Autowired(required = false)
    private SuAriAutoSyncService suAriAutoSyncService;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
    }

    private LocalDate storeToday(Long storeId) {
        ZoneId zoneId = resolveStoreZoneId(storeId);
        return LocalDate.now(effectiveClock().withZone(zoneId));
    }

    private LocalTime storeLocalTime(Long storeId) {
        ZoneId zoneId = resolveStoreZoneId(storeId);
        return LocalTime.now(effectiveClock().withZone(zoneId));
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

    private Clock effectiveClock() {
        return clock != null ? clock : Clock.systemUTC();
    }

    /**
     * 创建预订
     */
    public ReservationDTO createReservation(CreateReservationRequest request) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Room room = loadRoomForAssignmentWithLock(storeId, request.getRoomId());

        // 验证渠道是否存在
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        assertRoomAvailableForDateRange(
                storeId,
                room.getId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                null
        );

        // 创建预订
        Reservation reservation = new Reservation();
        reservation.setStoreId(storeId);
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setChannel(channel);
        reservation.setGuestName(request.getGuestName());
        reservation.setGuestPhone(request.getGuestPhone());
        reservation.setGuestIdCard(request.getGuestIdCard());
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setAdults(request.getAdults());
        reservation.setChildren(request.getChildren());
        reservation.setTotalAmount(request.getTotalAmount());
        reservation.setChannelOrderNumber(request.getChannelOrderNumber());
        reservation.setGroupOrderNo(normalizeGroupOrderNo(request.getGroupOrderNo()));
        reservation.setNotes(request.getNotes());
        reservation.setPaymentMethod(request.getPaymentMethod());
        reservation.setCommission(request.getCommission());
        reservation.setOtherFees(request.getOtherFees());
        reservation.setPricePlan(request.getPricePlan());
        reservation.setSpecialRequests(request.getSpecialRequests());
        reservation.setBookingDate(request.getBookingDate());

        // 如果是直接入住，设置为已入住状态并记录入住时间
        System.out.println("=== 调试信息 ===");
        System.out.println("directCheckIn值: " + request.getDirectCheckIn());
        System.out.println("是否为true: " + Boolean.TRUE.equals(request.getDirectCheckIn()));

        if (Boolean.TRUE.equals(request.getDirectCheckIn())) {
            System.out.println("设置为已入住状态");
            reservation.setStatus(ReservationStatus.CHECKED_IN);
            reservation.setActualCheckIn(LocalDateTime.now());
        } else {
            System.out.println("设置为预订状态");
            reservation.setStatus(ReservationStatus.CONFIRMED);
        }

        Reservation savedReservation = reservationRepository.save(reservation);
        cleaningTaskAutoService.syncTaskForReservation(savedReservation);
        scheduleAutoMessageDispatchAfterCommit(storeId);
        schedulePriceLabsReservationSyncAfterCommit(storeId, savedReservation.getId());
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                storeId,
                resolveRoomTypeId(savedReservation.getRoom(), savedReservation.getOtaRoomTypeId()),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        scheduleSuAvailabilitySyncForReservationAfterCommit(
                storeId,
                SU_ARI_SOURCE_RESERVATION_CREATE,
                savedReservation.getRoom(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        logCreateReservation(savedReservation);
        orderNotificationDispatchService.notifyOrderCreated(storeId, savedReservation, userId);
        return convertToDTO(savedReservation);
    }

    /**
     * 批量创建预订（单次请求创建多条预订，并用 groupOrderNo 关联）。
     */
    public BatchCreateReservationResponse createBatchReservations(BatchCreateReservationRequest request) {
        if (request == null || request.getReservations() == null || request.getReservations().isEmpty()) {
            throw new RuntimeException("批量预订数据不能为空");
        }

        String groupOrderNo = normalizeGroupOrderNo(request.getGroupOrderNo());
        if (groupOrderNo == null) {
            groupOrderNo = generateGroupOrderNo();
        }

        List<ReservationDTO> created = new ArrayList<>();
        for (CreateReservationRequest item : request.getReservations()) {
            if (item == null) {
                throw new RuntimeException("批量预订明细不能为空");
            }
            item.setGroupOrderNo(groupOrderNo);
            created.add(createReservation(item));
        }

        return new BatchCreateReservationResponse(groupOrderNo, created.size(), created);
    }

    /**
     * 办理入住
     */
    public ReservationDTO checkIn(Long reservationId) {
        Reservation reservation = loadReservationInStore(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("预订状态不正确，无法办理入住");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setActualCheckIn(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);
        scheduleAutoMessageDispatchAfterCommit(reservation.getStoreId());
        schedulePriceLabsReservationSyncAfterCommit(reservation.getStoreId(), savedReservation.getId());
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                reservation.getStoreId(),
                resolveRoomTypeId(savedReservation.getRoom(), savedReservation.getOtaRoomTypeId()),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        scheduleSuAvailabilitySyncForReservationAfterCommit(
                reservation.getStoreId(),
                SU_ARI_SOURCE_RESERVATION_CHECK_IN,
                savedReservation.getRoom(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        operationLogService.logOperation(
                savedReservation.getId(),
                OperationType.ORDER,
                "办理入住",
                null,
                null,
                List.of(new OperationLogDetailDTO("房间", formatRoomDisplay(savedReservation)))
        );
        return convertToDTO(savedReservation);
    }

    /**
     * 办理退房
     */
    public ReservationDTO checkOut(Long reservationId) {
        Reservation reservation = loadReservationInStore(reservationId);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("预订状态不正确，无法办理退房");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setActualCheckOut(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);
        cleaningTaskAutoService.ensureCheckoutTaskForReservation(savedReservation);
        scheduleAutoMessageDispatchAfterCommit(reservation.getStoreId());
        schedulePriceLabsReservationSyncAfterCommit(reservation.getStoreId(), savedReservation.getId());
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                reservation.getStoreId(),
                resolveRoomTypeId(savedReservation.getRoom(), savedReservation.getOtaRoomTypeId()),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        scheduleSuAvailabilitySyncForReservationAfterCommit(
                reservation.getStoreId(),
                SU_ARI_SOURCE_RESERVATION_CHECK_OUT,
                savedReservation.getRoom(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        operationLogService.logOperation(
                savedReservation.getId(),
                OperationType.ORDER,
                "办理退房",
                null,
                null,
                List.of(new OperationLogDetailDTO("房间", formatRoomDisplay(savedReservation)))
        );
        return convertToDTO(savedReservation);
    }

    /**
     * 取消预订
     */
    public ReservationDTO cancelReservation(Long reservationId) {
        Long userId = currentUserId();
        Reservation reservation = loadReservationInStore(reservationId);

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new RuntimeException("已退房订单无法取消");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setActualCheckOut(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);
        cleaningTaskAutoService.syncTaskForReservation(savedReservation);
        schedulePriceLabsReservationSyncAfterCommit(reservation.getStoreId(), savedReservation.getId());
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                reservation.getStoreId(),
                resolveRoomTypeId(savedReservation.getRoom(), savedReservation.getOtaRoomTypeId()),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        scheduleSuAvailabilitySyncForReservationAfterCommit(
                reservation.getStoreId(),
                SU_ARI_SOURCE_RESERVATION_CANCEL,
                savedReservation.getRoom(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        operationLogService.logOperation(
                savedReservation.getId(),
                OperationType.ORDER,
                "取消订单",
                null,
                null,
                List.of(
                        new OperationLogDetailDTO("订单号", savedReservation.getOrderNumber()),
                        new OperationLogDetailDTO("状态", savedReservation.getStatus().name())
                )
        );
        orderNotificationDispatchService.notifyOrderCancelled(reservation.getStoreId(), savedReservation, userId);
        return convertToDTO(savedReservation);
    }

    /**
     * 更新结账状态（手动结账/取消手动结账）。
     */
    public ReservationDTO updateSettlementStatus(Long reservationId, boolean settled) {
        Reservation reservation = loadReservationInStore(reservationId);
        reservation.setSettled(settled);
        Reservation savedReservation = reservationRepository.save(reservation);
        return convertToDTO(savedReservation);
    }

    private void scheduleAutoMessageDispatchAfterCommit(Long storeId) {
        if (storeId == null || autoMessageTriggerService == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            autoMessageTriggerService.dispatchStoreOnce(storeId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                autoMessageTriggerService.dispatchStoreOnce(storeId);
            }
        });
    }

    private void schedulePriceLabsReservationSyncAfterCommit(Long storeId, Long reservationId) {
        if (storeId == null || reservationId == null || priceLabsReservationSyncService == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                priceLabsReservationSyncService.pushReservationById(storeId, reservationId);
            } catch (Exception e) {
                logger.warn("[PriceLabsReservations] push reservation failed after commit (no tx). storeId={}, reservationId={}, error={}",
                        storeId, reservationId, e.getMessage());
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    priceLabsReservationSyncService.pushReservationById(storeId, reservationId);
                } catch (Exception e) {
                    logger.warn("[PriceLabsReservations] push reservation failed after commit. storeId={}, reservationId={}, error={}",
                            storeId, reservationId, e.getMessage());
                }
            }
        });
    }

    /**
     * PriceLabs 要求：预订/预留变化时，除了 /reservations，还需要同步 /calendar 的 booked_units/available_units/blocked_units。
     * 这里按“住宿晚数（nights only）”触发日历同步：checkIn ~ (checkOut-1)。
     */
    private void schedulePriceLabsCalendarSyncForReservationAfterCommit(
            Long storeId,
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (priceLabsCalendarSyncDebouncer == null || storeId == null || roomTypeId == null) {
            return;
        }
        LocalDate endInclusive = resolveNightEndInclusive(checkInDate, checkOutDate);
        if (checkInDate == null || endInclusive == null) {
            return;
        }
        priceLabsCalendarSyncDebouncer.requestSyncAfterCommit(storeId, roomTypeId, checkInDate, endInclusive);
    }

    private static LocalDate resolveNightEndInclusive(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return null;
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            return null;
        }
        return checkOutDate.minusDays(1);
    }

    private static Long resolveRoomTypeId(Room room, Long fallbackRoomTypeId) {
        if (room != null) {
            try {
                if (room.getRoomType() != null && room.getRoomType().getId() != null) {
                    return room.getRoomType().getId();
                }
            } catch (Exception ignored) {
                // ignore lazy init issues; fallback below
            }
        }
        return fallbackRoomTypeId;
    }

    private void scheduleSuAvailabilitySyncForReservationAfterCommit(
            Long storeId,
            String source,
            Room room,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        Long roomTypeId = resolveRoomTypeId(storeId, room);
        SuAriAutoSyncService.DateRange range = toInventoryDateRange(checkInDate, checkOutDate);
        if (range == null) {
            return;
        }
        Set<Long> roomTypeIds = roomTypeId != null ? Set.of(roomTypeId) : null;
        logger.info(
                "[SuAriTrace][Reservation] prepare enqueue. storeId={}, source={}, roomId={}, roomTypeId={}, range={}",
                storeId,
                source,
                room != null ? room.getId() : null,
                roomTypeId,
                range.from() + "~" + range.to()
        );
        scheduleSuAvailabilitySyncAfterCommit(
                storeId,
                source,
                List.of(range),
                roomTypeIds
        );
    }

    private void scheduleSuAvailabilitySyncForReservationChangeAfterCommit(
            Long storeId,
            String source,
            Room oldRoom,
            LocalDate oldCheckInDate,
            LocalDate oldCheckOutDate,
            Room newRoom,
            LocalDate newCheckInDate,
            LocalDate newCheckOutDate
    ) {
        Set<Long> roomTypeIds = new LinkedHashSet<>();
        List<SuAriAutoSyncService.DateRange> ranges = new ArrayList<>();

        Long oldRoomTypeId = resolveRoomTypeId(storeId, oldRoom);
        SuAriAutoSyncService.DateRange oldRange = toInventoryDateRange(oldCheckInDate, oldCheckOutDate);
        if (oldRoomTypeId != null && oldRange != null) {
            roomTypeIds.add(oldRoomTypeId);
            ranges.add(oldRange);
        }

        Long newRoomTypeId = resolveRoomTypeId(storeId, newRoom);
        SuAriAutoSyncService.DateRange newRange = toInventoryDateRange(newCheckInDate, newCheckOutDate);
        if (newRoomTypeId != null && newRange != null) {
            roomTypeIds.add(newRoomTypeId);
            ranges.add(newRange);
        }

        if (roomTypeIds.isEmpty()) {
            if (oldRange != null) {
                ranges.add(oldRange);
            }
            if (newRange != null) {
                ranges.add(newRange);
            }
        }

        if (ranges.isEmpty()) {
            return;
        }

        logger.info(
                "[SuAriTrace][Reservation] prepare enqueue(change). storeId={}, source={}, oldRoomId={}, newRoomId={}, roomTypeScope={}, ranges={}",
                storeId,
                source,
                oldRoom != null ? oldRoom.getId() : null,
                newRoom != null ? newRoom.getId() : null,
                roomTypeIds.isEmpty() ? "ALL" : roomTypeIds,
                formatSuAriRanges(ranges)
        );
        scheduleSuAvailabilitySyncAfterCommit(storeId, source, ranges, roomTypeIds.isEmpty() ? null : roomTypeIds);
    }

    private void scheduleSuAvailabilitySyncAfterCommit(
            Long storeId,
            String source,
            List<SuAriAutoSyncService.DateRange> ranges,
            Set<Long> roomTypeIds
    ) {
        if (storeId == null || suAriAutoSyncService == null || ranges == null || ranges.isEmpty()) {
            return;
        }

        Runnable enqueue = () -> {
            try {
                suAriAutoSyncService.enqueueForStoreDateRanges(
                        storeId,
                        source,
                        ranges,
                        roomTypeIds,
                        null,
                        true,
                        false,
                        false,
                        false
                );
                logger.info(
                        "[SuAriAutoSync] queued from reservation. storeId={}, source={}, rangesCount={}, ranges={}, roomTypeScope={}",
                        storeId,
                        source,
                        ranges.size(),
                        formatSuAriRanges(ranges),
                        roomTypeIds == null || roomTypeIds.isEmpty() ? "ALL" : roomTypeIds
                );
            } catch (Exception e) {
                logger.warn(
                        "[SuAriAutoSync] enqueue availability failed. storeId={}, source={}, roomTypes={}, ranges={}, error={}",
                        storeId,
                        source,
                        roomTypeIds,
                        ranges,
                        e.getMessage()
                );
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enqueue.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enqueue.run();
            }
        });
    }

    private String formatSuAriRanges(List<SuAriAutoSyncService.DateRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return "[]";
        }
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (SuAriAutoSyncService.DateRange range : ranges) {
            if (range == null || range.from() == null || range.to() == null) {
                continue;
            }
            joiner.add(range.from() + "~" + range.to());
        }
        return joiner.toString();
    }

    private SuAriAutoSyncService.DateRange toInventoryDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return null;
        }
        LocalDate endDate = checkOutDate.minusDays(1);
        if (endDate.isBefore(checkInDate)) {
            endDate = checkInDate;
        }
        return new SuAriAutoSyncService.DateRange(checkInDate, endDate);
    }

    private Long resolveRoomTypeId(Long storeId, Room room) {
        if (room == null) {
            return null;
        }
        if (room.getRoomType() != null && room.getRoomType().getId() != null) {
            return room.getRoomType().getId();
        }
        if (storeId == null || room.getId() == null) {
            return null;
        }
        return roomRepository.findByStoreIdAndIdWithRoomType(storeId, room.getId())
                .map(Room::getRoomType)
                .map(rt -> rt != null ? rt.getId() : null)
                .orElse(null);
    }

    /**
     * 根据ID获取预订
     */
    public Optional<ReservationDTO> getReservationById(Long id) {
        Long storeId = currentStoreId();
        return reservationRepository.findById(id)
                .filter(reservation -> storeId.equals(reservation.getStoreId()))
                .map(this::convertToDTO);
    }

    /**
     * 根据订单号获取预订
     */
    public Optional<ReservationDTO> getReservationByOrderNumber(String orderNumber) {
        return reservationRepository.findByStoreIdAndOrderNumber(currentStoreId(), orderNumber)
                .map(this::convertToDTO);
    }

    /**
     * 获取指定日期范围内的预订
     */
    public List<ReservationDTO> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = reservationRepository.findActiveReservationsByStoreIdBetweenDates(currentStoreId(), startDate, endDate);
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定房间的预订
     */
    public List<ReservationDTO> getReservationsByRoomId(Long roomId) {
        List<Reservation> reservations = reservationRepository.findByStoreIdAndRoomId(currentStoreId(), roomId);
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按门店范围搜索预订（客人、手机号、订单号、渠道订单号、房号）
     */
    public List<ReservationDTO> searchReservationsByGuestInfo(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        Long storeId = currentStoreId();
        List<Reservation> reservations = reservationRepository.searchByStoreIdAndKeyword(storeId, normalizedKeyword);

        return reservations.stream()
                .limit(50)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取今日入住的预订
     */
    public List<ReservationDTO> getTodayCheckIns() {
        Long storeId = currentStoreId();
        LocalDate today = storeToday(storeId);
        List<Reservation> reservations = reservationRepository.findByStoreIdAndCheckInDateBetween(storeId, today, today);
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取今日退房的预订
     */
    public List<ReservationDTO> getTodayCheckOuts() {
        Long storeId = currentStoreId();
        LocalDate today = storeToday(storeId);
        List<Reservation> reservations = reservationRepository.findByStoreIdAndCheckOutDateBetween(storeId, today, today);
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定房间在指定日期的预订
     */
    public Optional<ReservationDTO> getReservationByRoomAndDate(Long roomId, LocalDate date) {
        return reservationRepository.findByStoreIdAndRoomIdAndDate(currentStoreId(), roomId, date)
                .map(this::convertToDTO);
    }

    /**
     * 更新预订
     */
    public ReservationDTO updateReservation(Long reservationId, CreateReservationRequest request) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        Reservation existingReservation = loadReservationInStore(reservationId);
        Room room = loadRoomForAssignmentWithLock(storeId, request.getRoomId());
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        Long currentRoomId = existingReservation.getRoom() != null ? existingReservation.getRoom().getId() : null;

        if (!Objects.equals(currentRoomId, request.getRoomId()) ||
                !existingReservation.getCheckInDate().equals(request.getCheckInDate()) ||
                !existingReservation.getCheckOutDate().equals(request.getCheckOutDate())) {
            assertRoomAvailableForDateRange(
                    storeId,
                    room.getId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    reservationId
            );
        }

        Room oldRoom = existingReservation.getRoom();
        Channel oldChannel = existingReservation.getChannel();
        LocalDate oldCheckIn = existingReservation.getCheckInDate();
        LocalDate oldCheckOut = existingReservation.getCheckOutDate();
        String oldGuestName = existingReservation.getGuestName();
        String oldGuestPhone = existingReservation.getGuestPhone();
        BigDecimal oldTotalAmount = existingReservation.getTotalAmount();

        existingReservation.setRoom(room);
        existingReservation.setChannel(channel);
        existingReservation.setGuestName(request.getGuestName());
        existingReservation.setGuestPhone(request.getGuestPhone());
        existingReservation.setGuestIdCard(request.getGuestIdCard());
        existingReservation.setCheckInDate(request.getCheckInDate());
        existingReservation.setCheckOutDate(request.getCheckOutDate());
        existingReservation.setAdults(request.getAdults());
        existingReservation.setChildren(request.getChildren());
        existingReservation.setTotalAmount(request.getTotalAmount());
        existingReservation.setChannelOrderNumber(request.getChannelOrderNumber());
        existingReservation.setNotes(request.getNotes());
        existingReservation.setPaymentMethod(request.getPaymentMethod());
        existingReservation.setCommission(request.getCommission());
        existingReservation.setOtherFees(request.getOtherFees());
        existingReservation.setPricePlan(request.getPricePlan());
        existingReservation.setSpecialRequests(request.getSpecialRequests());
        existingReservation.setBookingDate(request.getBookingDate());
        existingReservation.setStoreId(storeId);
        existingReservation.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在")));

        Reservation savedReservation = reservationRepository.save(existingReservation);
        cleaningTaskAutoService.syncTaskForReservation(savedReservation);
        schedulePriceLabsReservationSyncAfterCommit(storeId, savedReservation.getId());
        // dates/room type might change; sync both old and new ranges to keep booked_units consistent
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                storeId,
                resolveRoomTypeId(oldRoom, existingReservation.getOtaRoomTypeId()),
                oldCheckIn,
                oldCheckOut
        );
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                storeId,
                resolveRoomTypeId(savedReservation.getRoom(), savedReservation.getOtaRoomTypeId()),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        scheduleSuAvailabilitySyncForReservationChangeAfterCommit(
                storeId,
                SU_ARI_SOURCE_RESERVATION_UPDATE,
                oldRoom,
                oldCheckIn,
                oldCheckOut,
                savedReservation.getRoom(),
                savedReservation.getCheckInDate(),
                savedReservation.getCheckOutDate()
        );
        logUpdateReservation(savedReservation, oldRoom, room, oldChannel, channel, oldCheckIn, oldCheckOut, oldGuestName, oldGuestPhone, oldTotalAmount);
        orderNotificationDispatchService.notifyOrderUpdated(storeId, savedReservation, userId);
        return convertToDTO(savedReservation);
    }

    /**
     * 获取某订单在其入住-退房日期范围内可分配的房型/房间列表。
     *
     * 规则：
     * 1) 仅返回当前门店的房间
     * 2) 过滤掉 OUT_OF_ORDER / MAINTENANCE 的房间
     * 3) 过滤掉在该日期范围内已有有效订单占用的房间（CONFIRMED/CHECKED_IN/REQUESTED）
     *
     * @param reservationId 订单ID
     * @param roomTypeId 可选，指定后返回该房型下可用房间列表；未指定时仅返回可用房型列表
     */
    @Transactional(readOnly = true)
    public AssignableRoomsResponse getAssignableRooms(Long reservationId, Long roomTypeId) {
        Reservation reservation = loadReservationInStore(reservationId);
        Long storeId = currentStoreId();

        LocalDate checkIn = reservation.getCheckInDate();
        LocalDate checkOut = reservation.getCheckOutDate();
        if (checkIn == null || checkOut == null) {
            throw new RuntimeException("订单缺少入住/退房日期，无法排房");
        }

        List<Room> allRooms = roomRepository.findByStoreIdWithRoomType(storeId);
        List<Room> candidateRooms = allRooms.stream()
                .filter(r -> r.getStatus() != RoomStatus.OUT_OF_ORDER && r.getStatus() != RoomStatus.MAINTENANCE)
                .collect(Collectors.toList());

        List<Long> candidateRoomIds = candidateRooms.stream()
                .map(Room::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<ReservationStatus> occupyStatuses = Set.of(
                ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN,
                ReservationStatus.REQUESTED
        );

        Set<Long> conflictRoomIds = candidateRoomIds.isEmpty()
                ? Set.of()
                : reservationRepository
                .findByStoreIdAndRoomIdInAndDateRangeAndStatuses(storeId, candidateRoomIds, checkIn, checkOut, occupyStatuses)
                .stream()
                .filter(r -> !Objects.equals(r.getId(), reservationId))
                .map(r -> r.getRoom() == null ? null : r.getRoom().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Room> availableRooms = candidateRooms.stream()
                .filter(r -> r.getId() != null && !conflictRoomIds.contains(r.getId()))
                .collect(Collectors.toList());

        Map<Long, RoomType> roomTypeById = new HashMap<>();
        Map<Long, Long> availableCountByRoomTypeId = new HashMap<>();
        for (Room room : availableRooms) {
            RoomType rt = room.getRoomType();
            if (rt == null || rt.getId() == null) {
                continue;
            }
            roomTypeById.putIfAbsent(rt.getId(), rt);
            availableCountByRoomTypeId.put(rt.getId(), availableCountByRoomTypeId.getOrDefault(rt.getId(), 0L) + 1);
        }

        List<AssignableRoomTypeDTO> roomTypes = availableCountByRoomTypeId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    RoomType rt = roomTypeById.get(e.getKey());
                    return new AssignableRoomTypeDTO(rt.getId(), rt.getName(), rt.getCode(), e.getValue());
                })
                .collect(Collectors.toList());

        List<AssignableRoomDTO> rooms = List.of();
        if (roomTypeId != null) {
            rooms = availableRooms.stream()
                    .filter(r -> r.getRoomType() != null && Objects.equals(r.getRoomType().getId(), roomTypeId))
                    .sorted((a, b) -> {
                        String an = a.getRoomNumber() == null ? "" : a.getRoomNumber();
                        String bn = b.getRoomNumber() == null ? "" : b.getRoomNumber();
                        return an.compareTo(bn);
                    })
                    .map(r -> new AssignableRoomDTO(
                            r.getId(),
                            r.getRoomNumber(),
                            r.getRoomType() == null ? null : r.getRoomType().getId(),
                            r.getRoomType() == null ? null : r.getRoomType().getName()
                    ))
                    .collect(Collectors.toList());
        }

        return new AssignableRoomsResponse(
                reservationId,
                checkIn.toString(),
                checkOut.toString(),
                roomTypes,
                rooms
        );
    }

    /**
     * 为订单分配具体房间（会按订单日期范围校验冲突）。
     */
    public ReservationDTO assignRoom(Long reservationId, Long roomId) {
        Long storeId = currentStoreId();

        Reservation reservation = loadReservationInStore(reservationId);
        LocalDate checkIn = reservation.getCheckInDate();
        LocalDate checkOut = reservation.getCheckOutDate();
        if (checkIn == null || checkOut == null) {
            throw new RuntimeException("订单缺少入住/退房日期，无法排房");
        }
        ReservationStatus reservationStatus = reservation.getStatus();
        boolean assignableStatus = reservationStatus == ReservationStatus.CONFIRMED
                || reservationStatus == ReservationStatus.REQUESTED
                || reservationStatus == ReservationStatus.CHECKED_IN;
        if (!assignableStatus) {
            throw new RuntimeException("当前订单状态不支持排房（仅已确认/待确认/已入住可排房）");
        }

        Room room = roomRepository.findByStoreIdAndIdWithRoomType(storeId, roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在或无权限"));
        if (room.getStatus() == RoomStatus.OUT_OF_ORDER || room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new RuntimeException("该房间当前不可用");
        }

        List<Reservation> conflicts = reservationRepository.findByStoreIdAndRoomIdAndDateRange(storeId, roomId, checkIn, checkOut)
                .stream()
                .filter(r -> !Objects.equals(r.getId(), reservationId))
                .collect(Collectors.toList());
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("该房间在订单日期范围内已被占用，请选择其他房间");
        }

        Room oldRoom = reservation.getRoom();
        Channel oldChannel = reservation.getChannel();
        LocalDate oldCheckIn = reservation.getCheckInDate();
        LocalDate oldCheckOut = reservation.getCheckOutDate();
        String oldGuestName = reservation.getGuestName();
        String oldGuestPhone = reservation.getGuestPhone();
        BigDecimal oldTotalAmount = reservation.getTotalAmount();

        reservation.setRoom(room);
        Reservation saved = reservationRepository.save(reservation);
        // 手动排房成功后自动移出订单盒子，保持列表状态一致
        orderBoxRepository.deleteByReservationId(reservationId);
        cleaningTaskAutoService.syncTaskForReservation(saved);
        schedulePriceLabsReservationSyncAfterCommit(storeId, saved.getId());
        // assignment can shift occupancy between room types; sync old and new ranges
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                storeId,
                resolveRoomTypeId(oldRoom, reservation.getOtaRoomTypeId()),
                oldCheckIn,
                oldCheckOut
        );
        schedulePriceLabsCalendarSyncForReservationAfterCommit(
                storeId,
                resolveRoomTypeId(saved.getRoom(), saved.getOtaRoomTypeId()),
                saved.getCheckInDate(),
                saved.getCheckOutDate()
        );
        scheduleSuAvailabilitySyncForReservationChangeAfterCommit(
                storeId,
                SU_ARI_SOURCE_RESERVATION_UPDATE,
                oldRoom,
                oldCheckIn,
                oldCheckOut,
                saved.getRoom(),
                saved.getCheckInDate(),
                saved.getCheckOutDate()
        );
        logUpdateReservation(saved, oldRoom, saved.getRoom(), oldChannel, saved.getChannel(), oldCheckIn, oldCheckOut, oldGuestName, oldGuestPhone, oldTotalAmount);
        return convertToDTO(saved);
    }

    /**
     * 获取房型的当前价格（基于日期判断平日/周末价格）
     */
    public BigDecimal getCurrentRoomPrice(Long roomTypeId, LocalDate date) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("房型不存在"));
        
        // 判断是否为周末（周六、周日）
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        
        // 优先使用周末价/平日价，其次使用默认价格
        if (isWeekend && roomType.getWeekendPrice() != null) {
            return roomType.getWeekendPrice();
        } else if (!isWeekend && roomType.getWeekdayPrice() != null) {
            return roomType.getWeekdayPrice();
        } else if (roomType.getDefaultPrice() != null) {
            return roomType.getDefaultPrice();
        } else {
            // 如果没有设置任何价格，返回默认价格100
            return new BigDecimal("100.00");
        }
    }

    /**
     * 带筛选条件的分页查询预订
     */
    public PagedReservationResponse getReservationsWithFilters(
            int page, int size, String searchKeyword, String channel,
            String roomType, String checkinType, String status,
            String paymentStatus, String isPackage, LocalDate startDate,
            LocalDate endDate, String orderType) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long storeId = currentStoreId();
        LocalDate today = storeToday(storeId);
        BusinessDayWindow todayWindow = storeDayWindow(storeId, today);
        LocalDate unassignedMinCheckOutDate = resolveUnassignedMinCheckOutDate(storeId);

        Specification<Reservation> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("storeId"), storeId));
            
            // 搜索关键词过滤
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = "%" + searchKeyword.toLowerCase() + "%";
                Predicate orderNumberPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("orderNumber")), keyword);
                Predicate channelOrderPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("channelOrderNumber")), keyword);
                Predicate guestNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("guestName")), keyword);
                Predicate phonePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("guestPhone")), keyword);
                Predicate roomNumberPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("room").get("roomNumber")), keyword);
                
                predicates.add(criteriaBuilder.or(
                    orderNumberPredicate, channelOrderPredicate, 
                    guestNamePredicate, phonePredicate, roomNumberPredicate
                ));
            }
            
            // 渠道过滤
            if (channel != null && !channel.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    root.get("channel").get("name"), getChannelDisplayName(channel)
                ));
            }
            
            // 房型过滤
            if (roomType != null && !roomType.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    root.get("room").get("roomType").get("name"), roomType
                ));
            }
            
            // 状态过滤
            if (status != null && !status.trim().isEmpty()) {
                ReservationStatus reservationStatus = getReservationStatusFromValue(status);
                if (reservationStatus != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), reservationStatus));
                }
            }

            // 结账状态过滤
            if (paymentStatus != null && !paymentStatus.trim().isEmpty()) {
                Predicate manuallySettledPredicate = criteriaBuilder.isTrue(root.get("settled"));
                Predicate suReservationPredicate = criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("suReservationId")),
                        criteriaBuilder.notEqual(root.get("suReservationId"), "")
                );
                Predicate checkedInOrOutPredicate = root.get("status").in(
                        ReservationStatus.CHECKED_IN,
                        ReservationStatus.CHECKED_OUT
                );
                Predicate fullyPaidPredicate = criteriaBuilder.and(
                        criteriaBuilder.greaterThan(root.get("totalAmount"), BigDecimal.ZERO),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("paidAmount"), root.get("totalAmount"))
                );
                Predicate paidPredicate = criteriaBuilder.or(
                        manuallySettledPredicate,
                        suReservationPredicate,
                        checkedInOrOutPredicate,
                        fullyPaidPredicate
                );

                if ("paid".equalsIgnoreCase(paymentStatus)) {
                    predicates.add(paidPredicate);
                } else if ("unpaid".equalsIgnoreCase(paymentStatus)) {
                    predicates.add(criteriaBuilder.not(paidPredicate));
                }
            }
            
            // 日期范围过滤
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("checkInDate"), startDate, endDate));
            }
            
            // 订单类型过滤
            if (orderType != null && !orderType.trim().isEmpty()) {
                switch (orderType) {
                    case "today-checkin":
                        predicates.add(criteriaBuilder.equal(root.get("checkInDate"), today));
                        break;
                    case "today-checkout":
                        predicates.add(criteriaBuilder.equal(root.get("checkOutDate"), today));
                        break;
                    case "today-new":
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), todayWindow.start()));
                        predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), todayWindow.end()));
                        break;
                    case "unassigned":
                        // 未排房/未映射统一按“未分配具体房间”处理
                        predicates.add(criteriaBuilder.isNull(root.get("room")));
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("checkOutDate"), unassignedMinCheckOutDate));
                        break;
                    case "assigned":
                        predicates.add(criteriaBuilder.isNotNull(root.get("room")));
                        break;
                    case "pending":
                        predicates.add(criteriaBuilder.equal(root.get("status"), ReservationStatus.CONFIRMED));
                        predicates.add(criteriaBuilder.isNull(root.get("actualCheckIn")));
                        break;
                    case "deleted-rooms":
                        predicates.add(criteriaBuilder.isNull(root.get("room")));
                        break;
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<Reservation> reservationPage = reservationRepository.findAll(spec, pageable);
        
        List<ReservationDTO> reservationDTOs = reservationPage.getContent()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return new PagedReservationResponse(
            reservationDTOs,
            reservationPage.getNumber(),
            reservationPage.getSize(),
            reservationPage.getTotalElements(),
            reservationPage.getTotalPages(),
            reservationPage.isFirst(),
            reservationPage.isLast()
        );
    }

    /**
     * 获取预订统计信息
     */
    public ReservationStatistics getReservationStatistics() {
        Long storeId = currentStoreId();
        LocalDate today = storeToday(storeId);
        LocalDate unassignedMinCheckOutDate = resolveUnassignedMinCheckOutDate(storeId);
        long todayCheckinCount = reservationRepository.countTodayArrivalsByStoreId(storeId, today);
        long todayCheckoutCount = reservationRepository.countByStoreIdAndCheckOutDate(storeId, today);
        BusinessDayWindow todayWindow = storeDayWindow(storeId, today);
        long todayNewCount = reservationRepository.countTodayNewOrdersByStoreId(storeId, todayWindow.start(), todayWindow.end());
        long unassignedCount = reservationRepository.countUnassignedOrUnmappedByStoreId(storeId, unassignedMinCheckOutDate);
        long pendingCount = reservationRepository.countPendingOrdersByStoreId(storeId);

        Specification<Reservation> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("storeId"), storeId);
        long totalReservations = reservationRepository.count(spec);

        return new ReservationStatistics(
            todayCheckinCount, todayCheckoutCount, todayNewCount,
            unassignedCount, pendingCount, totalReservations
        );
    }

    /**
     * 获取今日新增预订
     */
    public List<ReservationDTO> getTodayNewReservations() {
        Long storeId = currentStoreId();
        LocalDate today = storeToday(storeId);
        BusinessDayWindow todayWindow = storeDayWindow(storeId, today);
        List<Reservation> reservations = reservationRepository.findByStoreIdAndCreatedAtBetween(
                storeId, todayWindow.start(), todayWindow.end());
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取未排房预订
     */
    public List<ReservationDTO> getUnassignedReservations() {
        Long storeId = currentStoreId();
        LocalDate unassignedMinCheckOutDate = resolveUnassignedMinCheckOutDate(storeId);
        List<Reservation> reservations = reservationRepository.findUnassignedOrUnmappedByStoreId(
            storeId,
            unassignedMinCheckOutDate
        );
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private LocalDate resolveUnassignedMinCheckOutDate(Long storeId) {
        LocalDate today = storeToday(storeId);
        LocalTime now = storeLocalTime(storeId);
        return now.isBefore(UNASSIGNED_CHECK_OUT_CUTOFF_TIME) ? today : today.plusDays(1);
    }

    /**
     * 获取待处理预订
     */
    public List<ReservationDTO> getPendingReservations() {
        List<Reservation> reservations = reservationRepository.findByStoreIdAndStatus(currentStoreId(), ReservationStatus.CONFIRMED);
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据值获取预订状态枚举
     */
    private ReservationStatus getReservationStatusFromValue(String value) {
        switch (value) {
            case "checked-in":
                return ReservationStatus.CHECKED_IN;
            case "not-checked-in":
                return ReservationStatus.CONFIRMED;
            case "checked-out":
                return ReservationStatus.CHECKED_OUT;
            default:
                return null;
        }
    }

    private Room loadRoomForAssignmentWithLock(Long storeId, Long roomId) {
        return roomRepository.findByStoreIdAndIdForUpdate(storeId, roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在或无权限"));
    }

    private void assertRoomAvailableForDateRange(
            Long storeId,
            Long roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            Long excludeReservationId
    ) {
        List<Reservation> conflicts = reservationRepository.findByStoreIdAndRoomIdAndDateRange(
                storeId,
                roomId,
                checkIn,
                checkOut
        ).stream()
                .filter(r -> !Objects.equals(r.getId(), excludeReservationId))
                .toList();

        if (!conflicts.isEmpty()) {
            throw new RuntimeException("房间在指定日期已有预订，请选择其他日期或房间");
        }
    }

    private Reservation loadReservationInStore(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));
        if (!currentStoreId().equals(reservation.getStoreId())) {
            throw new RuntimeException("无权操作该预订");
        }
        return reservation;
    }

    /**
     * 获取渠道显示名称
     */
    private String getChannelDisplayName(String value) {
        switch (value) {
            case "direct":
                return "直营客";
            case "meituan":
                return "美团民宿";
            case "tujia":
                return "途家";
            default:
                return value;
        }
    }

    /**
     * 转换实体为DTO
     */
    private ReservationDTO convertToDTO(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId());
        dto.setOrderNumber(reservation.getOrderNumber());
        dto.setGroupOrderNo(reservation.getGroupOrderNo());
        dto.setGuestName(reservation.getGuestName());
        dto.setPhone(reservation.getGuestPhone());
        
        if (reservation.getRoom() != null) {
            dto.setRoomId(reservation.getRoom().getId());
            dto.setRoomNumber(reservation.getRoom().getRoomNumber());
            dto.setRoomTypeName(reservation.getRoom().getRoomType().getName());
            
            // 计算当前房价（基于入住日期）
            BigDecimal currentPrice = getCurrentRoomPrice(
                    reservation.getRoom().getRoomType().getId(), 
                    reservation.getCheckInDate()
            );
            dto.setCurrentRoomPrice(currentPrice);
        }
        
        dto.setChannelId(reservation.getChannel().getId());
        dto.setChannelName(reservation.getChannel().getName());
        dto.setChannelOrderNumber(reservation.getChannelOrderNumber());
        dto.setCheckInDate(reservation.getCheckInDate());
        dto.setCheckOutDate(reservation.getCheckOutDate());
        dto.setStatus(reservation.getStatus().name());
        dto.setAdults(reservation.getAdults());
        dto.setChildren(reservation.getChildren());
        dto.setPaymentMethod(reservation.getPaymentMethod());
        dto.setCommission(reservation.getCommission());
        boolean settled = ReservationSettlementRules.isSettled(
                reservation.getSettled(),
                reservation.getSuReservationId(),
                reservation.getStatus(),
                reservation.getPaidAmount(),
                reservation.getTotalAmount()
        );
        dto.setSettled(settled);
        dto.setPaidAmount(ReservationSettlementRules.resolveDisplayPaidAmount(
                reservation.getSettled(),
                reservation.getSuReservationId(),
                reservation.getStatus(),
                reservation.getPaidAmount(),
                reservation.getTotalAmount()
        ));
        dto.setPricePlan(reservation.getPricePlan());
        String createdBy = null;
        if (reservation.getUser() != null) {
            if (reservation.getUser().getName() != null && !reservation.getUser().getName().isBlank()) {
                createdBy = reservation.getUser().getName();
            } else if (reservation.getUser().getNickname() != null && !reservation.getUser().getNickname().isBlank()) {
                createdBy = reservation.getUser().getNickname();
            } else {
                createdBy = reservation.getUser().getUsername();
            }
        }
        dto.setCreatedBy(createdBy);
        dto.setNotes(reservation.getNotes());
        
        dto.setReservationNotifId(reservation.getReservationNotifId());
        dto.setSuReservationId(reservation.getSuReservationId());
        dto.setOtaRoomId(reservation.getOtaRoomId());
        dto.setOtaRoomTypeId(reservation.getOtaRoomTypeId());
        
        // 设置原始订单金额
        dto.setTotalAmount(reservation.getTotalAmount());
        
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setUpdatedAt(reservation.getUpdatedAt());
        dto.setReservationTimestampStorageZone(StoreTimeZoneUtil.getReservationTimestampStorageZoneId().getId());
        return dto;
    }

    private String normalizeGroupOrderNo(String groupOrderNo) {
        if (groupOrderNo == null) {
            return null;
        }
        String normalized = groupOrderNo.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateGroupOrderNo() {
        return "GRP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * 根据统计类型获取对应的订单列表
     */
    public List<ReservationDTO> getReservationsByType(String type) {
        Long storeId = currentStoreId();
        LocalDate today = storeToday(storeId);
        BusinessDayWindow todayWindow = storeDayWindow(storeId, today);

        List<Reservation> reservations;
        switch (type) {
            case "today-arrivals":
                reservations = reservationRepository.findTodayArrivalsByStoreId(storeId, today);
                break;
            case "today-departures":
                reservations = reservationRepository.findTodayDeparturesByStoreId(storeId, today);
                break;
            case "today-new":
                reservations = reservationRepository.findTodayNewOrdersByStoreId(storeId, todayWindow.start(), todayWindow.end());
                break;
            case "unassigned":
                reservations = reservationRepository.findUnassignedOrUnmappedByStoreId(storeId, resolveUnassignedMinCheckOutDate(storeId));
                break;
            case "assigned":
                reservations = reservationRepository.findAssignedByStoreId(storeId);
                break;
            case "pending":
                reservations = reservationRepository.findPendingOrdersByStoreId(storeId);
                break;
            default:
                Specification<Reservation> spec = (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("storeId"), storeId);
                reservations = reservationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
        }

        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ReservationChannelInfoDTO getChannelInfo(Long reservationId) {
        Reservation reservation = loadReservationInStore(reservationId);

        ReservationChannelInfoDTO dto = new ReservationChannelInfoDTO();
        dto.setChannelName(reservation.getChannel() == null ? null : reservation.getChannel().getName());
        dto.setChannelOrderNumber(reservation.getChannelOrderNumber());
        dto.setPaymentMethod(reservation.getPaymentMethod());
        dto.setStatus(reservation.getStatus() == null ? null : reservation.getStatus().name());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setCommission(reservation.getCommission());
        dto.setOtherFees(reservation.getOtherFees());
        dto.setRoomType(reservation.getRoom() == null ? null : reservation.getRoom().getRoomType().getName());
        dto.setGuestName(reservation.getGuestName());
        dto.setAdults(reservation.getAdults());
        dto.setChildren(reservation.getChildren());
        dto.setCheckInDate(reservation.getCheckInDate() == null ? null : reservation.getCheckInDate().toString());
        dto.setCheckOutDate(reservation.getCheckOutDate() == null ? null : reservation.getCheckOutDate().toString());
        if (reservation.getCheckInDate() != null && reservation.getCheckOutDate() != null) {
            dto.setNights(ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate()));
        }
        dto.setPricePlan(reservation.getPricePlan());
        dto.setSpecialRequests(reservation.getSpecialRequests());

        LocalDateTime bookingDate = reservation.getBookingDate() != null ? reservation.getBookingDate() : reservation.getCreatedAt();
        if (bookingDate != null) {
            dto.setBookingDate(bookingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return dto;
    }

    private void logCreateReservation(Reservation reservation) {
        List<OperationLogDetailDTO> details = new ArrayList<>();
        details.add(new OperationLogDetailDTO("联系人", reservation.getGuestName()));
        if (reservation.getGuestPhone() != null && !reservation.getGuestPhone().isBlank()) {
            details.add(new OperationLogDetailDTO("手机号", reservation.getGuestPhone()));
        }
        if (reservation.getChannel() != null) {
            details.add(new OperationLogDetailDTO("渠道", reservation.getChannel().getName()));
        }
        if (reservation.getChannelOrderNumber() != null && !reservation.getChannelOrderNumber().isBlank()) {
            details.add(new OperationLogDetailDTO("渠道订单号", reservation.getChannelOrderNumber()));
        }
        details.add(new OperationLogDetailDTO("房间", formatRoomDisplay(reservation)));
        details.add(new OperationLogDetailDTO("入住", reservation.getCheckInDate() == null ? "-" : reservation.getCheckInDate().toString()));
        details.add(new OperationLogDetailDTO("离店", reservation.getCheckOutDate() == null ? "-" : reservation.getCheckOutDate().toString()));
        details.add(new OperationLogDetailDTO("成人", String.valueOf(reservation.getAdults() == null ? 0 : reservation.getAdults())));
        details.add(new OperationLogDetailDTO("儿童", String.valueOf(reservation.getChildren() == null ? 0 : reservation.getChildren())));
        details.add(new OperationLogDetailDTO("订单金额", reservation.getTotalAmount() == null ? "0.00" : reservation.getTotalAmount().toPlainString()));

        operationLogService.logOperation(
                reservation.getId(),
                OperationType.ORDER,
                "新增预订订单",
                null,
                null,
                details
        );
    }

    private void logUpdateReservation(
            Reservation reservation,
            Room oldRoom,
            Room newRoom,
            Channel oldChannel,
            Channel newChannel,
            LocalDate oldCheckIn,
            LocalDate oldCheckOut,
            String oldGuestName,
            String oldGuestPhone,
            BigDecimal oldTotalAmount
    ) {
        boolean roomChanged = !Objects.equals(oldRoom == null ? null : oldRoom.getId(), newRoom == null ? null : newRoom.getId());

        if (roomChanged) {
            operationLogService.logOperation(
                    reservation.getId(),
                    OperationType.ORDER,
                    "分配房间",
                    null,
                    null,
                    List.of(
                            new OperationLogDetailDTO("原房间", formatRoomDisplay(oldRoom)),
                            new OperationLogDetailDTO("新房间", formatRoomDisplay(newRoom))
                    )
            );
        }

        List<OperationLogDetailDTO> details = new ArrayList<>();
        if (!Objects.equals(oldGuestName, reservation.getGuestName())) {
            details.add(new OperationLogDetailDTO("联系人", (oldGuestName == null ? "-" : oldGuestName) + " → " + (reservation.getGuestName() == null ? "-" : reservation.getGuestName())));
        }
        if (!Objects.equals(oldGuestPhone, reservation.getGuestPhone())) {
            details.add(new OperationLogDetailDTO("手机号", (oldGuestPhone == null ? "-" : oldGuestPhone) + " → " + (reservation.getGuestPhone() == null ? "-" : reservation.getGuestPhone())));
        }
        if (!Objects.equals(oldChannel == null ? null : oldChannel.getId(), newChannel == null ? null : newChannel.getId())) {
            details.add(new OperationLogDetailDTO("渠道", (oldChannel == null ? "-" : oldChannel.getName()) + " → " + (newChannel == null ? "-" : newChannel.getName())));
        }
        if (!Objects.equals(oldCheckIn, reservation.getCheckInDate())) {
            details.add(new OperationLogDetailDTO("入住日期", (oldCheckIn == null ? "-" : oldCheckIn.toString()) + " → " + (reservation.getCheckInDate() == null ? "-" : reservation.getCheckInDate().toString())));
        }
        if (!Objects.equals(oldCheckOut, reservation.getCheckOutDate())) {
            details.add(new OperationLogDetailDTO("离店日期", (oldCheckOut == null ? "-" : oldCheckOut.toString()) + " → " + (reservation.getCheckOutDate() == null ? "-" : reservation.getCheckOutDate().toString())));
        }
        if (!Objects.equals(oldTotalAmount, reservation.getTotalAmount())) {
            details.add(new OperationLogDetailDTO("订单金额", (oldTotalAmount == null ? "0.00" : oldTotalAmount.toPlainString()) + " → " + (reservation.getTotalAmount() == null ? "0.00" : reservation.getTotalAmount().toPlainString())));
        }

        if (!details.isEmpty() || !roomChanged) {
            operationLogService.logOperation(
                    reservation.getId(),
                    OperationType.ORDER,
                    "修改订单",
                    null,
                    null,
                    details
            );
        }
    }

    private String formatRoomDisplay(Reservation reservation) {
        if (reservation == null || reservation.getRoom() == null) {
            return "-";
        }
        return formatRoomDisplay(reservation.getRoom());
    }

    private String formatRoomDisplay(Room room) {
        if (room == null) {
            return "-";
        }
        String roomTypeName = room.getRoomType() == null ? "" : room.getRoomType().getName();
        String roomNumber = room.getRoomNumber() == null ? "" : room.getRoomNumber();
        if (!roomTypeName.isBlank() && !roomNumber.isBlank()) {
            return roomTypeName + "-" + roomNumber;
        }
        return !roomNumber.isBlank() ? roomNumber : roomTypeName;
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
