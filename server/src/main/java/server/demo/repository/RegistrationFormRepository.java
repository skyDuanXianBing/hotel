package server.demo.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationForm;
import server.demo.enums.RegistrationFormStatus;

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
            WHERE f.storeId = :storeId
              AND (:status IS NULL OR f.status = :status)
              AND (:channelId IS NULL OR c.id = :channelId)
              AND (:checkInDate IS NULL OR r.checkInDate = :checkInDate)
              AND (:checkOutDate IS NULL OR r.checkOutDate = :checkOutDate)
            ORDER BY f.updatedAt DESC
            """)
    List<RegistrationForm> searchForAdminList(
            @Param("storeId") Long storeId,
            @Param("status") RegistrationFormStatus status,
            @Param("channelId") Long channelId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );
}
