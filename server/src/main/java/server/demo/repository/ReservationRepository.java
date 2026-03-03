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
import java.util.Set;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    interface ReservationOccupancyRow {
        LocalDate getCheckInDate();

        LocalDate getCheckOutDate();

        Long getOtaRoomTypeId();

        Long getAssignedRoomTypeId();
    }

    // ===== store-scoped APIs =====
    List<Reservation> findByStoreId(Long storeId);

    @Query("SELECT DISTINCT r FROM Reservation r " +
            "LEFT JOIN FETCH r.room room " +
            "LEFT JOIN FETCH room.roomType " +
            "WHERE r.storeId = :storeId " +
            "AND r.checkOutDate >= :startDate " +
            "AND r.checkInDate <= :endDate")
    List<Reservation> findByStoreIdOverlappingDateRangeWithRoomType(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Reservation r " +
            "LEFT JOIN FETCH r.room room " +
            "LEFT JOIN FETCH room.roomType " +
            "WHERE r.storeId = :storeId " +
            "AND r.checkOutDate >= :startDate " +
            "AND r.checkInDate <= :endDate " +
            "AND (room.roomType.id = :roomTypeId OR r.otaRoomTypeId = :roomTypeId)")
    List<Reservation> findByStoreIdAndRoomTypeIdOverlappingDateRangeWithRoomType(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Reservation r " +
            "LEFT JOIN FETCH r.room room " +
            "LEFT JOIN FETCH room.roomType " +
            "WHERE r.storeId = :storeId AND r.id = :id")
    Optional<Reservation> findByStoreIdAndIdWithRoomType(
            @Param("storeId") Long storeId,
            @Param("id") Long id
    );

    Optional<Reservation> findByStoreIdAndOrderNumber(Long storeId, String orderNumber);

    List<Reservation> findByStoreIdAndChannelOrderNumber(Long storeId, String channelOrderNumber);

    List<Reservation> findByStoreIdAndGuestNameContainingIgnoreCase(Long storeId, String guestName);

    List<Reservation> findByStoreIdAndGuestPhone(Long storeId, String guestPhone);

    List<Reservation> findByStoreIdAndStatus(Long storeId, ReservationStatus status);

    List<Reservation> findByStoreIdAndRoomId(Long storeId, Long roomId);

    List<Reservation> findByStoreIdAndActualCheckInBetween(Long storeId, LocalDateTime start, LocalDateTime end);

    List<Reservation> findByStoreIdAndActualCheckOutBetween(Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.checkInDate >= :startDate AND r.checkInDate <= :endDate")
    List<Reservation> findByStoreIdAndCheckInDateBetween(@Param("storeId") Long storeId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    List<Reservation> findByStoreIdAndCheckInDateBetweenAndStatusIn(Long storeId,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    Set<ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.checkOutDate >= :startDate AND r.checkOutDate <= :endDate")
    List<Reservation> findByStoreIdAndCheckOutDateBetween(@Param("storeId") Long storeId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND (r.checkInDate <= :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findActiveReservationsByStoreIdBetweenDates(@Param("storeId") Long storeId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT r.checkInDate as checkInDate, r.checkOutDate as checkOutDate, r.otaRoomTypeId as otaRoomTypeId, r.room.roomType.id as assignedRoomTypeId " +
            "FROM Reservation r " +
            "WHERE r.storeId = :storeId " +
            "AND r.status IN :statuses " +
            "AND r.checkInDate < :endExclusive " +
            "AND r.checkOutDate > :startDate")
    List<ReservationOccupancyRow> findOccupancyRowsByStoreIdAndDateRangeAndStatuses(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endExclusive") LocalDate endExclusive,
            @Param("statuses") Set<ReservationStatus> statuses
    );

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.room.id = :roomId AND " +
           "(r.checkInDate < :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'REQUESTED')")
    List<Reservation> findByStoreIdAndRoomIdAndDateRange(@Param("storeId") Long storeId,
                                                         @Param("roomId") Long roomId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.room.id IN :roomIds AND " +
            "(r.checkInDate <= :endDate AND r.checkOutDate > :startDate) AND r.status IN :statuses")
    List<Reservation> findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
            @Param("storeId") Long storeId,
            @Param("roomIds") List<Long> roomIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Set<ReservationStatus> statuses
    );

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.room.id = :roomId AND " +
           "r.checkInDate <= :date AND r.checkOutDate > :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'REQUESTED')")
    Optional<Reservation> findByStoreIdAndRoomIdAndDate(@Param("storeId") Long storeId,
                                                        @Param("roomId") Long roomId,
                                                        @Param("date") LocalDate date);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.storeId = :storeId AND r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    long countTodayArrivalsByStoreId(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    long countByStoreIdAndCheckOutDate(Long storeId, LocalDate checkOutDate);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.storeId = :storeId AND " +
           "((r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay))")
    long countTodayNewOrdersByStoreId(@Param("storeId") Long storeId,
                                      @Param("startOfDay") LocalDateTime startOfDay,
                                      @Param("endOfDay") LocalDateTime endOfDay);

    long countByStoreIdAndRoomIsNull(Long storeId);

    /**
     * 未排房/未映射：
     * - 统一以 room IS NULL 判定（只要已排到具体房间，就不再计入该列表/统计）。
     * - 未映射场景本质上也会导致无法排房，因此仍会落在 room IS NULL 集合中。
     */
    @Query("""
            SELECT COUNT(r)
            FROM Reservation r
            WHERE r.storeId = :storeId
              AND r.room IS NULL
            """)
    long countUnassignedOrUnmappedByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.storeId = :storeId AND r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL")
    long countPendingOrdersByStoreId(@Param("storeId") Long storeId);

    List<Reservation> findByStoreIdAndCreatedAtBetween(Long storeId, LocalDateTime start, LocalDateTime end);

    List<Reservation> findByStoreIdAndRoomIsNull(Long storeId);

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.storeId = :storeId
              AND r.room IS NULL
            ORDER BY r.createdAt DESC
            """)
    List<Reservation> findUnassignedOrUnmappedByStoreId(@Param("storeId") Long storeId);

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.storeId = :storeId
              AND r.room IS NOT NULL
            ORDER BY r.createdAt DESC
            """)
    List<Reservation> findAssignedByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN') ORDER BY r.createdAt DESC")
    List<Reservation> findTodayArrivalsByStoreId(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.checkOutDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') ORDER BY r.createdAt DESC")
    List<Reservation> findTodayDeparturesByStoreId(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND " +
           "((r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay)) " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findTodayNewOrdersByStoreId(@Param("storeId") Long storeId,
                                                  @Param("startOfDay") LocalDateTime startOfDay,
                                                  @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findPendingOrdersByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND (r.checkInDate <= :endDate AND r.checkOutDate > :startDate)")
    List<Reservation> findByStoreIdAndDateRange(@Param("storeId") Long storeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.room.roomType.id = :roomTypeId " +
           "AND ((:type = 'ARRIVAL' AND r.checkInDate = :date AND r.status = 'CONFIRMED') OR " +
           "(:type = 'DEPARTURE' AND r.checkOutDate = :date AND r.status = 'CHECKED_IN'))")
    List<Reservation> findByStoreIdAndRoomTypeAndDate(@Param("storeId") Long storeId,
                                                      @Param("roomTypeId") Long roomTypeId,
                                                      @Param("date") LocalDate date,
                                                      @Param("type") String type);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.room.roomType.id = :roomTypeId " +
           "AND r.checkInDate <= :date AND r.checkOutDate > :date " +
           "AND r.status = 'CHECKED_IN'")
    List<Reservation> findOccupiedWithoutDepartureByStore(@Param("storeId") Long storeId,
                                                          @Param("roomTypeId") Long roomTypeId,
                                                          @Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.storeId = :storeId AND r.room.roomType.id = :roomTypeId " +
           "AND r.status = 'CANCELLED' AND r.updatedAt >= :startOfDay AND r.updatedAt < :endOfDay")
    List<Reservation> findCancelledByStoreAndRoomTypeAndDate(@Param("storeId") Long storeId,
                                                             @Param("roomTypeId") Long roomTypeId,
                                                             @Param("startOfDay") LocalDateTime startOfDay,
                                                             @Param("endOfDay") LocalDateTime endOfDay);

    // ===== legacy user-scoped APIs (to be removed once迁移完成) =====
    @Deprecated
    List<Reservation> findByUserId(Long userId);

    @Deprecated
    Optional<Reservation> findByUserIdAndOrderNumber(Long userId, String orderNumber);

    @Deprecated
    List<Reservation> findByUserIdAndGuestNameContainingIgnoreCase(Long userId, String guestName);

    @Deprecated
    List<Reservation> findByUserIdAndGuestPhone(Long userId, String guestPhone);

    @Deprecated
    List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

    @Deprecated
    List<Reservation> findByUserIdAndRoomId(Long userId, Long roomId);

    @Deprecated
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.checkInDate >= :startDate AND r.checkInDate <= :endDate")
    List<Reservation> findByUserIdAndCheckInDateBetween(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Deprecated
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.checkOutDate >= :startDate AND r.checkOutDate <= :endDate")
    List<Reservation> findByUserIdAndCheckOutDateBetween(@Param("userId") Long userId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Deprecated
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND (r.checkInDate <= :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findActiveReservationsByUserIdBetweenDates(@Param("userId") Long userId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    @Deprecated
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.room.id = :roomId AND (r.checkInDate < :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findByUserIdAndRoomIdAndDateRange(@Param("userId") Long userId,
                                                         @Param("roomId") Long roomId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Deprecated
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.room.id = :roomId AND r.checkInDate <= :date AND r.checkOutDate > :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    Optional<Reservation> findByUserIdAndRoomIdAndDate(@Param("userId") Long userId,
                                                        @Param("roomId") Long roomId,
                                                        @Param("date") LocalDate date);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    long countTodayArrivalsByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Deprecated
    long countByUserIdAndCheckOutDate(Long userId, LocalDate checkOutDate);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND " +
           "((r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay))")
    long countTodayNewOrdersByUserId(@Param("userId") Long userId,
                                      @Param("startOfDay") LocalDateTime startOfDay,
                                      @Param("endOfDay") LocalDateTime endOfDay);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    long countTodayArrivalsForUser(@Param("date") LocalDate date, @Param("roomIds") List<Long> roomIds);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.checkOutDate = :date")
    long countByCheckOutDateForUser(@Param("date") LocalDate date, @Param("roomIds") List<Long> roomIds);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND " +
           "((r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay))")
    long countTodayNewOrdersForUser(@Param("startOfDay") LocalDateTime startOfDay,
                                    @Param("endOfDay") LocalDateTime endOfDay,
                                    @Param("roomIds") List<Long> roomIds);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.room IS NULL")
    long countByRoomIsNullForUser(@Param("roomIds") List<Long> roomIds);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.room.id IN :roomIds AND r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL")
    long countPendingOrdersForUser(@Param("roomIds") List<Long> roomIds);

    // ===== legacy generic APIs =====
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

    @Query("SELECT r FROM Reservation r WHERE (r.checkInDate <= :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    List<Reservation> findActiveReservationsBetweenDates(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND (r.checkInDate <= :endDate AND r.checkOutDate > :startDate) " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'REQUESTED')")
    List<Reservation> findByRoomIdAndDateRange(@Param("roomId") Long roomId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND r.checkInDate <= :date AND r.checkOutDate > :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN', 'REQUESTED')")
    Optional<Reservation> findByRoomIdAndDate(@Param("roomId") Long roomId, @Param("date") LocalDate date);

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.checkInDate = :date " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN')")
    long countTodayArrivals(@Param("date") LocalDate date);

    long countByCheckOutDate(LocalDate checkOutDate);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE (r.createdAt >= :startOfDay AND r.createdAt < :endOfDay) OR " +
           "(r.actualCheckIn >= :startOfDay AND r.actualCheckIn < :endOfDay)")
    long countTodayNewOrders(@Param("startOfDay") LocalDateTime startOfDay,
                             @Param("endOfDay") LocalDateTime endOfDay);

    long countByRoomIsNull();

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'CONFIRMED' AND r.actualCheckIn IS NULL")
    long countPendingOrders();

    /**
     * 根据入住日期和状态查找订单(用于自动化消息触发)
     */
    List<Reservation> findByCheckInDateAndStatus(LocalDate checkInDate, ReservationStatus status);

    /**
     * 根据退房日期和状态查找订单(用于自动化消息触发)
     */
    List<Reservation> findByCheckOutDateAndStatus(LocalDate checkOutDate, ReservationStatus status);
}
