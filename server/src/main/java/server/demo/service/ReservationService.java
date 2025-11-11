package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * 创建预订
     */
    public ReservationDTO createReservation(Long userId, CreateReservationRequest request) {
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 验证房间是否存在
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        // 验证渠道是否存在
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        // 检查房间在指定日期是否有冲突(仅检查当前用户的预订)
        List<Reservation> conflictReservations = reservationRepository.findByUserIdAndRoomIdAndDateRange(
                userId, request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

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
        return convertToDTO(savedReservation);
    }

    /**
     * 办理入住
     */
    public ReservationDTO checkIn(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        // 验证数据归属
        if (!reservation.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限操作此预订");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("预订状态不正确，无法办理入住");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setActualCheckIn(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);
        return convertToDTO(savedReservation);
    }

    /**
     * 办理退房
     */
    public ReservationDTO checkOut(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        // 验证数据归属
        if (!reservation.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限操作此预订");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("预订状态不正确，无法办理退房");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setActualCheckOut(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);
        return convertToDTO(savedReservation);
    }

    /**
     * 取消预订
     */
    public ReservationDTO cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        // 验证数据归属
        if (!reservation.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限操作此预订");
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            throw new RuntimeException("已入住的预订无法取消");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        Reservation savedReservation = reservationRepository.save(reservation);
        return convertToDTO(savedReservation);
    }

    /**
     * 根据ID获取预订
     */
    public Optional<ReservationDTO> getReservationById(Long userId, Long id) {
        return reservationRepository.findById(id)
                .filter(reservation -> reservation.getUser().getId().equals(userId))
                .map(this::convertToDTO);
    }

    /**
     * 根据订单号获取预订
     */
    public Optional<ReservationDTO> getReservationByOrderNumber(Long userId, String orderNumber) {
        return reservationRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .map(this::convertToDTO);
    }

    /**
     * 获取指定日期范围内的预订
     */
    public List<ReservationDTO> getReservationsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = reservationRepository.findActiveReservationsByUserIdBetweenDates(userId, startDate, endDate);
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定房间的预订
     */
    public List<ReservationDTO> getReservationsByRoomId(Long userId, Long roomId) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndRoomId(userId, roomId);
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据客人信息搜索预订
     */
    public List<ReservationDTO> searchReservationsByGuestInfo(Long userId, String keyword) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndGuestNameContainingIgnoreCase(userId, keyword);
        if (reservations.isEmpty() && keyword.matches("\\d+")) {
            // 如果按姓名搜索无结果且关键词是数字，尝试按手机号搜索
            reservations = reservationRepository.findByUserIdAndGuestPhone(userId, keyword);
        }
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取今日入住的预订
     */
    public List<ReservationDTO> getTodayCheckIns(Long userId) {
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findByUserIdAndCheckInDateBetween(userId, today, today);
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取今日退房的预订
     */
    public List<ReservationDTO> getTodayCheckOuts(Long userId) {
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findByUserIdAndCheckOutDateBetween(userId, today, today);
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定房间在指定日期的预订
     */
    public Optional<ReservationDTO> getReservationByRoomAndDate(Long userId, Long roomId, LocalDate date) {
        return reservationRepository.findByUserIdAndRoomIdAndDate(userId, roomId, date)
                .map(this::convertToDTO);
    }

    /**
     * 更新预订
     */
    public ReservationDTO updateReservation(Long userId, Long reservationId, CreateReservationRequest request) {
        // 查找现有预订
        Reservation existingReservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        // 验证数据归属
        if (!existingReservation.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限操作此预订");
        }

        // 验证房间是否存在
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        // 验证渠道是否存在
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        // 如果房间或日期有变化，检查是否有冲突(仅检查当前用户的预订)
        if (!existingReservation.getRoom().getId().equals(request.getRoomId()) ||
            !existingReservation.getCheckInDate().equals(request.getCheckInDate()) ||
            !existingReservation.getCheckOutDate().equals(request.getCheckOutDate())) {

            List<Reservation> conflictReservations = reservationRepository.findByUserIdAndRoomIdAndDateRange(
                    userId, request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

            // 排除当前正在更新的预订
            conflictReservations = conflictReservations.stream()
                    .filter(r -> !r.getId().equals(reservationId))
                    .collect(Collectors.toList());

            if (!conflictReservations.isEmpty()) {
                throw new RuntimeException("房间在指定日期已有预订，请选择其他日期或房间");
            }
        }

        // 更新预订信息
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

        Reservation savedReservation = reservationRepository.save(existingReservation);
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
            Long userId, int page, int size, String searchKeyword, String channel,
            String roomType, String checkinType, String status,
            String paymentStatus, String isPackage, LocalDate startDate,
            LocalDate endDate, String orderType) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Reservation> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 添加用户ID过滤条件
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            
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
    public ReservationStatistics getReservationStatistics(Long userId) {
        LocalDate today = LocalDate.now();

        // 使用带用户ID的统计方法
        long todayCheckinCount = reservationRepository.countTodayArrivalsByUserId(userId, today);
        long todayCheckoutCount = reservationRepository.countByUserIdAndCheckOutDate(userId, today);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long todayNewCount = reservationRepository.countTodayNewOrdersByUserId(userId, startOfDay, endOfDay);
        long unassignedCount = reservationRepository.countByUserIdAndRoomIsNull(userId);
        long pendingCount = reservationRepository.countPendingOrdersByUserId(userId);

        // 计算该用户的总预订数
        Specification<Reservation> spec = (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("user").get("id"), userId);
        long totalReservations = reservationRepository.count(spec);

        return new ReservationStatistics(
            todayCheckinCount, todayCheckoutCount, todayNewCount,
            unassignedCount, pendingCount, totalReservations
        );
    }

    /**
     * 获取今日新增预订
     */
    public List<ReservationDTO> getTodayNewReservations(Long userId) {
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findByUserIdAndCreatedAtBetween(
            userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay()
        );
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取未排房预订
     */
    public List<ReservationDTO> getUnassignedReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndRoomIsNull(userId);
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取待处理预订
     */
    public List<ReservationDTO> getPendingReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndStatus(userId, ReservationStatus.CONFIRMED);
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
    public List<ReservationDTO> getReservationsByType(Long userId, String type) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<Reservation> reservations;

        switch (type) {
            case "today-arrivals":
                // 今日预抵：今天入住的订单
                reservations = reservationRepository.findTodayArrivalsByUserId(userId, today);
                break;
            case "today-departures":
                // 今日预离：今天退房的订单
                reservations = reservationRepository.findTodayDeparturesByUserId(userId, today);
                break;
            case "today-new":
                // 今日新办：今天创建的订单和今天实际入住的订单
                reservations = reservationRepository.findTodayNewOrdersByUserId(userId, startOfDay, endOfDay);
                break;
            case "unassigned":
                // 未排房：没有分配房间的订单
                reservations = reservationRepository.findByUserIdAndRoomIsNull(userId);
                break;
            case "pending":
                // 待处理：已确认但未入住的订单
                reservations = reservationRepository.findPendingOrdersByUserId(userId);
                break;
            default:
                // 默认返回该用户的所有订单
                Specification<Reservation> spec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("user").get("id"), userId);
                reservations = reservationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
                break;
        }

        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}