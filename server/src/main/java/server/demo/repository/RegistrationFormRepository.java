package server.demo.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationForm;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationFormRepository extends JpaRepository<RegistrationForm, Long> {
    Optional<RegistrationForm> findByStoreIdAndOrderNumber(Long storeId, String orderNumber);

    Optional<RegistrationForm> findByStoreIdAndReservation_Id(Long storeId, Long reservationId);

    List<RegistrationForm> findByStoreIdAndStatusOrderByUpdatedAtDesc(Long storeId, RegistrationFormStatus status);

    List<RegistrationForm> findByStoreIdOrderByUpdatedAtDesc(Long storeId);

    @Query("""
            SELECT f FROM RegistrationForm f
            JOIN FETCH f.reservation r
            JOIN FETCH r.channel c
            LEFT JOIN FETCH r.room room
            LEFT JOIN FETCH room.roomType
            WHERE f.storeId = :storeId
              AND (:status IS NULL OR f.status = :status)
              AND (:channelId IS NULL OR c.id = :channelId)
              AND (:reservationStatus IS NULL OR r.status = :reservationStatus)
              AND (:checkInDate IS NULL OR r.checkInDate >= :checkInDate)
              AND (:checkOutDate IS NULL OR r.checkOutDate <= :checkOutDate)
              AND (
                    :roomNumber IS NULL
                    OR LOWER(room.roomNumber) LIKE LOWER(CONCAT('%', :roomNumber, '%'))
                    OR LOWER(r.otaRoomNumber) LIKE LOWER(CONCAT('%', :roomNumber, '%'))
                  )
              AND (
                    :roomGroupId IS NULL
                    OR EXISTS (
                        SELECT 1 FROM RoomGroupMember m
                        WHERE m.storeId = :storeId
                          AND m.groupId = :roomGroupId
                          AND m.roomId = room.id
                    )
                  )
            ORDER BY f.updatedAt DESC
            """)
    List<RegistrationForm> searchForAdminList(
            @Param("storeId") Long storeId,
            @Param("status") RegistrationFormStatus status,
            @Param("channelId") Long channelId,
            @Param("reservationStatus") ReservationStatus reservationStatus,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomNumber") String roomNumber,
            @Param("roomGroupId") Long roomGroupId
    );
}
