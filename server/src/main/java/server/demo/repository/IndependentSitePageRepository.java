package server.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.IndependentSitePage;
import server.demo.enums.IndependentSitePageType;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndependentSitePageRepository extends JpaRepository<IndependentSitePage, Long> {

    List<IndependentSitePage> findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc(Long storeId, Long siteId);

    Optional<IndependentSitePage> findByStoreIdAndSiteIdAndId(Long storeId, Long siteId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT page
            FROM IndependentSitePage page
            WHERE page.storeId = :storeId
              AND page.site.id = :siteId
              AND page.id = :id
            """)
    Optional<IndependentSitePage> findByStoreIdAndSiteIdAndIdForUpdate(
            @Param("storeId") Long storeId,
            @Param("siteId") Long siteId,
            @Param("id") Long id
    );

    Optional<IndependentSitePage> findByStoreIdAndSiteIdAndPath(Long storeId, Long siteId, String path);

    List<IndependentSitePage> findByStoreIdAndSiteIdAndType(
            Long storeId,
            Long siteId,
            IndependentSitePageType type
    );

    Optional<IndependentSitePage> findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue(
            Long siteId,
            IndependentSitePageType type
    );

    List<IndependentSitePage> findBySiteIdAndPublishedAtIsNotNullAndEnabledTrueOrderBySortOrderAscIdAsc(
            Long siteId
    );

    List<IndependentSitePage> findByStoreIdAndSiteIdAndPublishedAtIsNotNullAndEnabledTrueOrderBySortOrderAscIdAsc(
            Long storeId,
            Long siteId
    );

    long countByStoreIdAndSiteId(Long storeId, Long siteId);
}
