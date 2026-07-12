package server.demo.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationForm;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
            WHERE f.storeId = :storeId
              AND f.status = server.demo.enums.RegistrationFormStatus.APPROVED
              AND f.approvedAt >= :approvedSince
              AND (r.status IS NULL OR r.status <> server.demo.enums.ReservationStatus.CANCELLED)
            ORDER BY f.approvedAt DESC, f.id DESC
            """)
    List<RegistrationForm> findRecentApprovedForHome(
            @Param("storeId") Long storeId,
            @Param("approvedSince") LocalDateTime approvedSince
    );

    @Query("""
            SELECT f FROM RegistrationForm f
            JOIN FETCH f.reservation r
            JOIN FETCH r.channel c
            LEFT JOIN FETCH r.room room
            WHERE f.storeId = :storeId
              AND (:status IS NULL OR f.status = :status)
              AND (:channelId IS NULL OR c.id = :channelId)
              AND (:reservationStatus IS NULL OR r.status = :reservationStatus)
              AND (:includeCancelledArchive = true
                    OR r.status IS NULL
                    OR r.status <> server.demo.enums.ReservationStatus.CANCELLED)
              AND (:roomNumberFilterEnabled = false OR room.roomNumber IN :roomNumbers OR r.otaRoomNumber IN :roomNumbers)
              AND (:roomGroupId IS NULL OR EXISTS (
                    SELECT rgm.id FROM RoomGroupMember rgm
                    WHERE rgm.storeId = :storeId
                      AND rgm.groupId = :roomGroupId
                      AND rgm.roomId = room.id
              ))
              AND (:checkInStartDate IS NULL OR r.checkInDate >= :checkInStartDate)
              AND (:checkInEndDate IS NULL OR r.checkInDate <= :checkInEndDate)
              AND (:checkOutStartDate IS NULL OR r.checkOutDate >= :checkOutStartDate)
              AND (:checkOutEndDate IS NULL OR r.checkOutDate <= :checkOutEndDate)
            ORDER BY f.updatedAt DESC
            """)
    List<RegistrationForm> searchForAdminList(
            @Param("storeId") Long storeId,
            @Param("status") RegistrationFormStatus status,
            @Param("channelId") Long channelId,
            @Param("reservationStatus") ReservationStatus reservationStatus,
            @Param("includeCancelledArchive") boolean includeCancelledArchive,
            @Param("roomNumberFilterEnabled") boolean roomNumberFilterEnabled,
            @Param("roomNumbers") List<String> roomNumbers,
            @Param("roomGroupId") Long roomGroupId,
            @Param("checkInStartDate") LocalDate checkInStartDate,
            @Param("checkInEndDate") LocalDate checkInEndDate,
            @Param("checkOutStartDate") LocalDate checkOutStartDate,
            @Param("checkOutEndDate") LocalDate checkOutEndDate
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE RegistrationForm f
            SET f.status = server.demo.enums.RegistrationFormStatus.APPROVED,
                f.approvedAt = :reviewedAt,
                f.reviewNote = :note
            WHERE f.id = :formId
              AND f.storeId = :storeId
              AND f.status = server.demo.enums.RegistrationFormStatus.SUBMITTED
            """)
    int approveSubmitted(
            @Param("storeId") Long storeId,
            @Param("formId") Long formId,
            @Param("note") String note,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE RegistrationForm f
            SET f.status = server.demo.enums.RegistrationFormStatus.REJECTED,
                f.rejectedAt = :reviewedAt,
                f.reviewNote = :note
            WHERE f.id = :formId
              AND f.storeId = :storeId
              AND f.status = server.demo.enums.RegistrationFormStatus.SUBMITTED
            """)
    int rejectSubmitted(
            @Param("storeId") Long storeId,
            @Param("formId") Long formId,
            @Param("note") String note,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );
}
