package server.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.RoomStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByStoreIdAndId(Long storeId, Long id);

    Optional<Room> findByStoreIdAndRoomNumber(Long storeId, String roomNumber);

    List<Room> findAllByStoreIdAndRoomNumberOrderByIdAsc(Long storeId, String roomNumber);

    List<Room> findByStoreId(Long storeId);

    List<Room> findByStoreIdAndIdIn(Long storeId, Collection<Long> ids);

    @Query("SELECT r FROM Room r WHERE r.storeId = :storeId AND r.roomType.id = :roomTypeId")
    List<Room> findByStoreIdAndRoomTypeId(@Param("storeId") Long storeId,
                                          @Param("roomTypeId") Long roomTypeId);

    List<Room> findByStoreIdAndStatus(Long storeId, RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.storeId = :storeId AND r.roomType.id = :roomTypeId AND r.status = :status")
    List<Room> findByStoreIdAndRoomTypeIdAndStatus(@Param("storeId") Long storeId,
                                                   @Param("roomTypeId") Long roomTypeId,
                                                   @Param("status") RoomStatus status);

    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.storeId = :storeId ORDER BY r.roomType.name, r.roomNumber")
    List<Room> findByStoreIdWithRoomType(@Param("storeId") Long storeId);

    long countByStoreId(Long storeId);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.storeId = :storeId AND r.roomType.id = :roomTypeId AND r.status = :status")
    long countByStoreIdAndRoomTypeIdAndStatus(@Param("storeId") Long storeId,
                                              @Param("roomTypeId") Long roomTypeId,
                                              @Param("status") RoomStatus status);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.storeId = :storeId AND r.status NOT IN ('OUT_OF_ORDER', 'MAINTENANCE') " +
            "AND NOT EXISTS (SELECT res FROM Reservation res WHERE res.room = r " +
            "AND res.checkInDate <= :date AND res.checkOutDate > :date " +
            "AND res.status IN ('CONFIRMED', 'CHECKED_IN', 'REQUESTED'))")
    long countAvailableRoomsForDateByStore(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.storeId = :storeId AND r.id = :roomId")
    Optional<Room> findByStoreIdAndIdWithRoomType(@Param("storeId") Long storeId, @Param("roomId") Long roomId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT r FROM Room r WHERE r.storeId = :storeId AND r.id = :roomId")
        Optional<Room> findByStoreIdAndIdForUpdate(@Param("storeId") Long storeId, @Param("roomId") Long roomId);

    boolean existsByStoreIdAndRoomNumber(Long storeId, String roomNumber);

    // ===== 以下为兼容旧逻辑的接口，后续会逐步淘汰 =====

    @Deprecated
    Optional<Room> findByRoomNumber(String roomNumber);

    @Deprecated
    List<Room> findByRoomType(RoomType roomType);

    @Deprecated
    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId")
    List<Room> findByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    @Deprecated
    List<Room> findByStatus(RoomStatus status);

    @Deprecated
    List<Room> findByRoomTypeAndStatus(RoomType roomType, RoomStatus status);

    @Deprecated
    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId AND r.status = :status")
    List<Room> findByRoomTypeIdAndStatus(@Param("roomTypeId") Long roomTypeId, @Param("status") RoomStatus status);

    @Deprecated
    @Query("SELECT r FROM Room r JOIN FETCH r.roomType ORDER BY r.roomType.name, r.roomNumber")
    List<Room> findAllWithRoomType();

    @Deprecated
    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.userId = :userId ORDER BY r.roomType.name, r.roomNumber")
    List<Room> findByUserIdWithRoomType(@Param("userId") Long userId);

    @Deprecated
    List<Room> findByUserId(Long userId);

    @Deprecated
    long countByUserId(Long userId);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Room r WHERE r.roomType.id = :roomTypeId AND r.status = :status")
    long countByRoomTypeIdAndStatus(@Param("roomTypeId") Long roomTypeId, @Param("status") RoomStatus status);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Room r WHERE r.status NOT IN ('OUT_OF_ORDER', 'MAINTENANCE') " +
            "AND NOT EXISTS (SELECT res FROM Reservation res WHERE res.room = r " +
            "AND res.checkInDate <= :date AND res.checkOutDate > :date " +
            "AND res.status IN ('CONFIRMED', 'CHECKED_IN', 'REQUESTED'))")
    long countAvailableRoomsForDate(@Param("date") LocalDate date);

    @Deprecated
    @Query("SELECT r FROM Room r JOIN FETCH r.roomType WHERE r.id = :roomId")
    Optional<Room> findByIdWithRoomType(@Param("roomId") Long roomId);

    @Deprecated
    boolean existsByRoomNumber(String roomNumber);

    @Deprecated
    @Query("SELECT COUNT(r) FROM Room r WHERE r.userId = :userId AND r.status NOT IN ('OUT_OF_ORDER', 'MAINTENANCE') " +
            "AND NOT EXISTS (SELECT res FROM Reservation res WHERE res.room = r " +
            "AND res.checkInDate <= :date AND res.checkOutDate > :date " +
            "AND res.status IN ('CONFIRMED', 'CHECKED_IN'))")
    long countAvailableRoomsForDateAndUser(@Param("date") LocalDate date, @Param("userId") Long userId);
}
