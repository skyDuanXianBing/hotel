package server.demo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.enums.SuWebhookEventType;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SuReservationWebhookEventRepository extends JpaRepository<SuReservationWebhookEvent, Long> {

    Optional<SuReservationWebhookEvent> findByHotelIdAndReservationNotifId(String hotelId, String reservationNotifId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM SuReservationWebhookEvent e
            WHERE e.status IN :statuses
              AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
            ORDER BY COALESCE(e.nextRetryAt, e.createdAt) ASC
            """)
    List<SuReservationWebhookEvent> findDueEvents(
            @Param("statuses") List<SuWebhookEventStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    List<SuReservationWebhookEvent> findTop200ByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("""
            SELECT e
            FROM SuReservationWebhookEvent e
            WHERE e.storeId = :storeId
              AND (:hotelId IS NULL OR e.hotelId = :hotelId)
              AND (:reservationNotifId IS NULL OR e.reservationNotifId = :reservationNotifId)
              AND (:status IS NULL OR e.status = :status)
              AND (:eventType IS NULL OR e.eventType = :eventType)
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<SuReservationWebhookEvent> findRecentByStoreIdAndFilters(
            @Param("storeId") Long storeId,
            @Param("hotelId") String hotelId,
            @Param("reservationNotifId") String reservationNotifId,
            @Param("status") SuWebhookEventStatus status,
            @Param("eventType") SuWebhookEventType eventType,
            Pageable pageable
    );

    List<SuReservationWebhookEvent> findByStatusOrderByUpdatedAtDesc(SuWebhookEventStatus status, Pageable pageable);
}
