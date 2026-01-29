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
import server.demo.dto.CreateReservationRequest;
import server.demo.dto.PagedReservationResponse;
import server.demo.dto.ReservationDTO;
import server.demo.dto.ReservationStatistics;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.User;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;
import server.demo.util.StoreContextUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AutoMessageTriggerService autoMessageTriggerService;

    @Autowired
    private PriceLabsReservationSyncService priceLabsReservationSyncService;

    @Autowired
    private CleaningTaskAutoService cleaningTaskAutoService;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
    }

    /**
     * 创建预订
     */
    public ReservationDTO createReservation(CreateReservationRequest request) {
        Long storeId = currentStoreId();
        Long userId = currentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Room room = roomRepository.findByStoreIdAndId(storeId, request.getRoomId())
                .orElseThrow(() -> new RuntimeException("房间不存在或无权限"));

        // 验证渠道是否存在
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        // 检查房间在指定日期是否有冲突(仅检查当前用户的预订)
        List<Reservation> conflictReservations = reservationRepository.findByStoreIdAndRoomIdAndDateRange(
                storeId, request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

        System.out.println("===== 预订冲突检查 =====");
        System.out.println("用户ID: " + userId + ", 房间ID: " + request.getRoomId());
        System.out.println("入住日期: " + request.getCheckInDate() + ", 退房日期: " + request.getCheckOutDate());
        System.out.println("查询到的冲突预订数量: " + conflictReservations.size());
        for (Reservation r : conflictReservations) {
            System.out.println("  冲突预订: ID=" + r.getId() +
                             ", 状态=" + r.getStatus() +
                             ", 入住=" + r.getCheckInDate() +
                             ", 退房=" + r.getCheckOutDate());
        }
        System.out.println("========================\n");

        if (!conflictReservations.isEmpty()) {
            throw new RuntimeException("房间在指定日期已有预订，请选择其他日期或房间");
        }

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
        reservation.setNotes(request.getNotes());

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
        return convertToDTO(savedReservation);
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
        scheduleAutoMessageDispatchAfterCommit(reservation.getStoreId());
        schedulePriceLabsReservationSyncAfterCommit(reservation.getStoreId(), savedReservation.getId());
        return convertToDTO(savedReservation);
    }

    /**
     * 取消预订
     */
    public ReservationDTO cancelReservation(Long reservationId) {
        Reservation reservation = loadReservationInStore(reservationId);

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new RuntimeException("已退房订单无法取消");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setActualCheckOut(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);
        cleaningTaskAutoService.syncTaskForReservation(savedReservation);
        schedulePriceLabsReservationSyncAfterCommit(reservation.getStoreId(), savedReservation.getId());
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
     * 根据客人信息搜索预订
     */
    public List<ReservationDTO> searchReservationsByGuestInfo(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        Long storeId = currentStoreId();
        List<Reservation> reservations = reservationRepository.findByStoreIdAndGuestNameContainingIgnoreCase(storeId, keyword);
        if (reservations.isEmpty()) {
            reservations = reservationRepository.findByStoreIdAndGuestPhone(storeId, keyword);
        }

        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取今日入住的预订
     */
    public List<ReservationDTO> getTodayCheckIns() {
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findByStoreIdAndCheckInDateBetween(currentStoreId(), today, today);
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取今日退房的预订
     */
    public List<ReservationDTO> getTodayCheckOuts() {
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findByStoreIdAndCheckOutDateBetween(currentStoreId(), today, today);
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
        Room room = roomRepository.findByStoreIdAndId(storeId, request.getRoomId())
                .orElseThrow(() -> new RuntimeException("房间不存在或无权限"));
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        if (!existingReservation.getRoom().getId().equals(request.getRoomId()) ||
                !existingReservation.getCheckInDate().equals(request.getCheckInDate()) ||
                !existingReservation.getCheckOutDate().equals(request.getCheckOutDate())) {

            List<Reservation> conflictReservations = reservationRepository.findByStoreIdAndRoomIdAndDateRange(
                    storeId, request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

            conflictReservations = conflictReservations.stream()
                    .filter(r -> !r.getId().equals(reservationId))
                    .collect(Collectors.toList());

            if (!conflictReservations.isEmpty()) {
                throw new RuntimeException("房间在指定日期已有预订，请选择其他日期或房间");
            }
        }

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
        existingReservation.setStoreId(storeId);
        existingReservation.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在")));

        Reservation savedReservation = reservationRepository.save(existingReservation);
        cleaningTaskAutoService.syncTaskForReservation(savedReservation);
        schedulePriceLabsReservationSyncAfterCommit(storeId, savedReservation.getId());
        return convertToDTO(savedReservation);
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
            
            // 日期范围过滤
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("checkInDate"), startDate, endDate));
            }
            
            // 订单类型过滤
            if (orderType != null && !orderType.trim().isEmpty()) {
                LocalDate today = LocalDate.now();
                switch (orderType) {
                    case "today-checkin":
                        predicates.add(criteriaBuilder.equal(root.get("checkInDate"), today));
                        break;
                    case "today-checkout":
                        predicates.add(criteriaBuilder.equal(root.get("checkOutDate"), today));
                        break;
                    case "today-new":
                        predicates.add(criteriaBuilder.between(
                            root.get("createdAt"), 
                            today.atStartOfDay(), 
                            today.plusDays(1).atStartOfDay()
                        ));
                        break;
                    case "unassigned":
                        // 未排房：没有分配具体房间
                        predicates.add(criteriaBuilder.isNull(root.get("room")));
                        break;
                    case "pending":
                        predicates.add(criteriaBuilder.equal(root.get("status"), ReservationStatus.CONFIRMED));
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
        LocalDate today = LocalDate.now();
        long todayCheckinCount = reservationRepository.countTodayArrivalsByStoreId(storeId, today);
        long todayCheckoutCount = reservationRepository.countByStoreIdAndCheckOutDate(storeId, today);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long todayNewCount = reservationRepository.countTodayNewOrdersByStoreId(storeId, startOfDay, endOfDay);
        long unassignedCount = reservationRepository.countByStoreIdAndRoomIsNull(storeId);
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
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findByStoreIdAndCreatedAtBetween(
                currentStoreId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取未排房预订
     */
    public List<ReservationDTO> getUnassignedReservations() {
        List<Reservation> reservations = reservationRepository.findByStoreIdAndRoomIsNull(currentStoreId());
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
        dto.setCheckInDate(reservation.getCheckInDate());
        dto.setCheckOutDate(reservation.getCheckOutDate());
        dto.setStatus(reservation.getStatus().name());
        dto.setNotes(reservation.getNotes());
        
        // 设置原始订单金额
        dto.setTotalAmount(reservation.getTotalAmount());
        
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setUpdatedAt(reservation.getUpdatedAt());
        return dto;
    }

    /**
     * 根据统计类型获取对应的订单列表
     */
    public List<ReservationDTO> getReservationsByType(String type) {
        Long storeId = currentStoreId();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<Reservation> reservations;
        switch (type) {
            case "today-arrivals":
                reservations = reservationRepository.findTodayArrivalsByStoreId(storeId, today);
                break;
            case "today-departures":
                reservations = reservationRepository.findTodayDeparturesByStoreId(storeId, today);
                break;
            case "today-new":
                reservations = reservationRepository.findTodayNewOrdersByStoreId(storeId, startOfDay, endOfDay);
                break;
            case "unassigned":
                reservations = reservationRepository.findByStoreIdAndRoomIsNull(storeId);
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
}
