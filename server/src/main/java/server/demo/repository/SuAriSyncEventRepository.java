package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.SuAriSyncEvent;
import server.demo.enums.SuAriSyncEventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuAriSyncEventRepository extends JpaRepository<SuAriSyncEvent, Long> {

    Optional<SuAriSyncEvent> findTopByStoreIdAndHotelIdAndStatusOrderByIdDesc(
            Long storeId,
            String hotelId,
            SuAriSyncEventStatus status
    );

    @Query("SELECT e FROM SuAriSyncEvent e WHERE e.status = :status " +
            "AND (e.notBeforeAt IS NULL OR e.notBeforeAt <= :now) " +
            "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
            "ORDER BY e.id ASC")
    List<SuAriSyncEvent> findDueEvents(
            @Param("status") SuAriSyncEventStatus status,
            @Param("now") LocalDateTime now
    );
}

