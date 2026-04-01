package server.demo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.SuWebhookEventStatus;

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

    List<SuReservationWebhookEvent> findByStatusOrderByUpdatedAtDesc(SuWebhookEventStatus status, Pageable pageable);
}

