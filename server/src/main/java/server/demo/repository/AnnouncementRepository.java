package server.demo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.Announcement;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("""
            SELECT a
            FROM Announcement a
            WHERE a.active = true
              AND (a.locale IS NULL OR a.locale = '' OR a.locale = :locale)
              AND (
                    (a.scope = :globalScope AND a.storeId IS NULL)
                    OR (a.scope = :storeScope AND a.storeId = :storeId)
                  )
              AND (a.startsAt IS NULL OR a.startsAt <= :now)
              AND (a.endsAt IS NULL OR a.endsAt >= :now)
            ORDER BY
              CASE WHEN a.scope = :storeScope THEN 0 ELSE 1 END,
              a.sortOrder ASC,
              a.startsAt DESC,
              a.id DESC
            """)
    List<Announcement> findHomeAnnouncements(
            @Param("storeId") Long storeId,
            @Param("locale") String locale,
            @Param("now") LocalDateTime now,
            @Param("globalScope") String globalScope,
            @Param("storeScope") String storeScope,
            Pageable pageable
    );
}
