package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    // ========== 新增：按用户ID查询的方法 ==========

    // 按用户ID查询所有预订
    List<Reservation> findByUserId(Long userId);

    // 按用户ID和订单号查询
    Optional<Reservation> findByUserIdAndOrderNumber(Long userId, String orderNumber);

    // 按用户ID和客人姓名查询
    List<Reservation> findByUserIdAndGuestNameContainingIgnoreCase(Long userId, String guestName);

    // 按用户ID和客人电话查询
    List<Reservation> findByUserIdAndGuestPhone(Long userId, String guestPhone);

    // 按用户ID和状态查询
    List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

    // 按用户ID和房间ID查询
    List<Reservation> findByUserIdAndRoomId(Long userId, Long roomId);

    // 按用户ID查询指定日期范围的入住预订
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.checkInDate >= :startDate AND r.checkInDate <= :endDate")
    List<Reservation> findByUserIdAndCheckInDateBetween(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    // 按用户ID查询指定日期范围的退房预订
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.checkOutDate >= :startDate AND r.checkOutDate <= :endDate")
    List<Reservation> findByUserIdAndCheckOutDateBetween(@Param("userId") Long userId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    // 按用户ID查询活跃预订（指定日期范围）
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND " +
           "(r.checkInDate <= :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findActiveReservationsByUserIdBetweenDates(@Param("userId") Long userId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    // 按用户ID和房间ID查询日期范围内的预订
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.room.id = :roomId AND " +
           "(r.checkInDate < :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findByUserIdAndRoomIdAndDateRange(@Param("userId") Long userId,
                                                         @Param("roomId") Long roomId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    // 按用户ID、房间ID和日期查询
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.room.id = :roomId AND " +
           "r.checkInDate <= :date AND r.checkOutDate > :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    Optional<Reservation> findByUserIdAndRoomIdAndDate(@Param("userId") Long userId,
                                                        @Param("roomId") Long roomId,
                                                        @Param("date") LocalDate date);

    // 按用户ID统计今日预抵
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    long countTodayArrivalsByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 按用户ID统计今日预离
    long countByUserIdAndCheckOutDate(Long userId, LocalDate checkOutDate);

    // 按用户ID统计今日新办
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND " +
           "((r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay))")
    long countTodayNewOrdersByUserId(@Param("userId") Long userId,
                                      @Param("startOfDay") LocalDateTime startOfDay,
                                      @Param("endOfDay") LocalDateTime endOfDay);

    // 按用户ID统计未排房
    long countByUserIdAndRoomIsNull(Long userId);

    // 按用户ID统计待处理
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL")
    long countPendingOrdersByUserId(@Param("userId") Long userId);

    // 按用户ID查询今日新增预订
    List<Reservation> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // 按用户ID查询未排房
    List<Reservation> findByUserIdAndRoomIsNull(Long userId);

    // 按用户ID查询今日预抵
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN') ORDER BY r.createdAt DESC")
    List<Reservation> findTodayArrivalsByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 按用户ID查询今日预离
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.checkOutDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') ORDER BY r.createdAt DESC")
    List<Reservation> findTodayDeparturesByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 按用户ID查询今日新办
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND " +
           "((r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay)) " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findTodayNewOrdersByUserId(@Param("userId") Long userId,
                                                  @Param("startOfDay") LocalDateTime startOfDay,
                                                  @Param("endOfDay") LocalDateTime endOfDay);

    // 按用户ID查询待处理
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findPendingOrdersByUserId(@Param("userId") Long userId);

    // 按用户ID查询日期范围内的所有预订（用于远期房情表）
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND " +
           "(r.checkInDate <= :endDate AND r.checkOutDate > :startDate)")
    List<Reservation> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    // ========== 旧方法，保留用于兼容 ==========
    
    Optional<Reservation> findByOrderNumber(String orderNumber);
    
    List<Reservation> findByGuestNameContainingIgnoreCase(String guestName);
    
    List<Reservation> findByGuestPhone(String guestPhone);
    
    List<Reservation> findByStatus(ReservationStatus status);
    
    List<Reservation> findByRoom(Room room);
    
    List<Reservation> findByRoomId(Long roomId);
    
    @Query("SELECT r FROM Reservation r WHERE r.checkInDate <= :date AND r.checkOutDate > :date")
    List<Reservation> findByDate(@Param("date") LocalDate date);
    
    @Query("SELECT r FROM Reservation r WHERE r.checkInDate >= :startDate AND r.checkInDate <= :endDate")
    List<Reservation> findByCheckInDateBetween(@Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    @Query("SELECT r FROM Reservation r WHERE r.checkOutDate >= :startDate AND r.checkOutDate <= :endDate")
    List<Reservation> findByCheckOutDateBetween(@Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate);
    
    @Query("SELECT r FROM Reservation r WHERE " +
           "(r.checkInDate <= :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findActiveReservationsBetweenDates(@Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate);
    
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND " +
           "(r.checkInDate <= :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findByRoomIdAndDateRange(@Param("roomId") Long roomId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
    
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND " +
           "r.checkInDate <= :date AND r.checkOutDate > :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    Optional<Reservation> findByRoomIdAndDate(@Param("roomId") Long roomId, @Param("date") LocalDate date);
    
    boolean existsByOrderNumber(String orderNumber);
    
    // 今日预抵统计：今天入住的订单
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    long countTodayArrivals(@Param("date") LocalDate date);
    
    // 今日预离统计：今天退房的订单
    long countByCheckOutDate(LocalDate checkOutDate);
    
    // 今日新办统计：今天创建的订单和今天实际入住的订单
    @Query("SELECT COUNT(r) FROM Reservation r WHERE " +
           "(r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay)")
    long countTodayNewOrders(@Param("startOfDay") LocalDateTime startOfDay, 
                           @Param("endOfDay") LocalDateTime endOfDay);
    
    // 未排房统计
    long countByRoomIsNull();
    
    // 待处理统计
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL")
    long countPendingOrders();
    
    // 新增查询方法
    List<Reservation> findByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    List<Reservation> findByRoomIsNull();
    
    // 根据类型查询订单的方法
    
    // 今日预抵：今天入住的订单
    @Query("SELECT r FROM Reservation r WHERE r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN') ORDER BY r.createdAt DESC")
    List<Reservation> findTodayArrivals(@Param("date") LocalDate date);
    
    // 今日预离：今天退房的订单
    @Query("SELECT r FROM Reservation r WHERE r.checkOutDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') ORDER BY r.createdAt DESC")
    List<Reservation> findTodayDepartures(@Param("date") LocalDate date);
    
    // 今日新办：今天创建的订单和今天实际入住的订单
    @Query("SELECT r FROM Reservation r WHERE " +
           "(r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay) " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findTodayNewOrders(@Param("startOfDay") LocalDateTime startOfDay, 
                                        @Param("endOfDay") LocalDateTime endOfDay);
    
    // 待处理：已确认但未入住的订单
    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findPendingOrders();
    
    // 房情表专用查询方法
    
    // 根据房型和日期查询预订（用于预抵/预离统计）
    @Query("SELECT r FROM Reservation r WHERE r.room.roomType.id = :roomTypeId " +
           "AND (:type = 'ARRIVAL' AND r.checkInDate = :date OR :type = 'DEPARTURE' AND r.checkOutDate = :date) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findByRoomTypeAndDate(@Param("roomTypeId") Long roomTypeId, 
                                           @Param("date") LocalDate date,
                                           @Param("type") String type);
    
    // 查询在住但不含预离的预订
    @Query("SELECT r FROM Reservation r WHERE r.room.roomType.id = :roomTypeId " +
           "AND r.checkInDate <= :date AND r.checkOutDate > :date " +
           "AND r.status = 'CHECKED_IN'")
    List<Reservation> findOccupiedWithoutDeparture(@Param("roomTypeId") Long roomTypeId, 
                                                   @Param("date") LocalDate date);
    
    // 查询当天取消的预订
    @Query("SELECT r FROM Reservation r WHERE r.room.roomType.id = :roomTypeId " +
           "AND r.status = 'CANCELLED' " +
           "AND r.updatedAt >= :startOfDay AND r.updatedAt < :endOfDay")
    List<Reservation> findCancelledByRoomTypeAndDate(@Param("roomTypeId") Long roomTypeId,
                                                    @Param("startOfDay") LocalDateTime startOfDay,
                                                    @Param("endOfDay") LocalDateTime endOfDay);
    
    // 查询日期范围内的所有预订（用于远期房情表）
    @Query("SELECT r FROM Reservation r WHERE " +
           "(r.checkInDate <= :endDate AND r.checkOutDate > :startDate)")
    List<Reservation> findByDateRange(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    // ========== 按房间ID列表查询的统计方法（用于用户数据隔离） ==========

    // 按房间ID列表统计今日预抵
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    long countTodayArrivalsForUser(@Param("date") LocalDate date, @Param("roomIds") List<Long> roomIds);

    // 按房间ID列表统计今日预离
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.checkOutDate = :date")
    long countByCheckOutDateForUser(@Param("date") LocalDate date, @Param("roomIds") List<Long> roomIds);

    // 按房间ID列表统计今日新办
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND " +
           "((r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay))")
    long countTodayNewOrdersForUser(@Param("startOfDay") LocalDateTime startOfDay,
                                    @Param("endOfDay") LocalDateTime endOfDay,
                                    @Param("roomIds") List<Long> roomIds);

    // 按房间ID列表统计未排房
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.room IS NULL")
    long countByRoomIsNullForUser(@Param("roomIds") List<Long> roomIds);

    // 按房间ID列表统计待处理
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL")
    long countPendingOrdersForUser(@Param("roomIds") List<Long> roomIds);
}