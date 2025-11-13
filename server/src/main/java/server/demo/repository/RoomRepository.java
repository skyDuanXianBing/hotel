package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.RoomStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    Optional<Room> findByRoomNumber(String roomNumber);
    
    List<Room> findByRoomType(RoomType roomType);
    
    List<Room> findByRoomTypeId(Long roomTypeId);
    
    List<Room> findByStatus(RoomStatus status);
    
    List<Room> findByRoomTypeAndStatus(RoomType roomType, RoomStatus status);
    
    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId AND r.status = :status")
    List<Room> findByRoomTypeIdAndStatus(@Param("roomTypeId") Long roomTypeId, @Param("status") RoomStatus status);
    
    @Query("SELECT r FROM Room r JOIN FETCH r.roomType ORDER BY r.roomType.name, r.roomNumber")
    List<Room> findAllWithRoomType();

    /**
     * 按用户ID和房型查询房间(用于数据隔离)
     */
    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.userId = :userId ORDER BY r.roomType.name, r.roomNumber")
    List<Room> findByUserIdWithRoomType(@Param("userId") Long userId);

    /**
     * 按用户ID查询房间列表
     */
    List<Room> findByUserId(Long userId);

    /**
     * 按用户ID统计房间数量
     */
    long countByUserId(Long userId);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.roomType.id = :roomTypeId AND r.status = :status")
    long countByRoomTypeIdAndStatus(@Param("roomTypeId") Long roomTypeId, @Param("status") RoomStatus status);
    
    // 今日可售房间统计：未被预订且非停用的房间
    @Query("SELECT COUNT(r) FROM Room r WHERE r.status NOT IN ('OUT_OF_ORDER', 'MAINTENANCE') " +
           "AND NOT EXISTS (SELECT res FROM Reservation res WHERE res.room = r " +
           "AND res.checkInDate <= :date AND res.checkOutDate > :date " +
           "AND res.status IN ('CONFIRMED', 'CHECKED_IN'))")
    long countAvailableRoomsForDate(@Param("date") java.time.LocalDate date);
    
    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.id = :roomId")
    Optional<Room> findByIdWithRoomType(@Param("roomId") Long roomId);
    
    boolean existsByRoomNumber(String roomNumber);

    /**
     * 按用户ID和日期统计可售房间数(用于数据隔离)
     * 未被预订且非停用的房间
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.userId = :userId AND r.status NOT IN ('OUT_OF_ORDER', 'MAINTENANCE') " +
           "AND NOT EXISTS (SELECT res FROM Reservation res WHERE res.room = r " +
           "AND res.checkInDate <= :date AND res.checkOutDate > :date " +
           "AND res.status IN ('CONFIRMED', 'CHECKED_IN'))")
    long countAvailableRoomsForDateAndUser(@Param("date") java.time.LocalDate date, @Param("userId") Long userId);
}