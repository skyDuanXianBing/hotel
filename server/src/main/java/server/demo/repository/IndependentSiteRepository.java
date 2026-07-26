package server.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.IndependentSite;

import server.demo.entity.IndependentSite;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndependentSiteRepository extends JpaRepository<IndependentSite, Long> {

    @Query("""
            SELECT site
            FROM IndependentSite site
            JOIN FETCH site.channel channel
            LEFT JOIN FETCH channel.defaultPricePlan
            WHERE site.storeId = :storeId
            """)
    Optional<IndependentSite> findByStoreIdWithChannel(@Param("storeId") Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT site
            FROM IndependentSite site
            JOIN FETCH site.channel channel
            LEFT JOIN FETCH channel.defaultPricePlan
            WHERE site.storeId = :storeId
            """)
    Optional<IndependentSite> findByStoreIdWithChannelForUpdate(@Param("storeId") Long storeId);

    @Query("""
            SELECT site
            FROM IndependentSite site
            JOIN FETCH site.channel channel
            LEFT JOIN FETCH channel.defaultPricePlan
            WHERE site.slug = :slug
              AND site.enabled = true
            """)
    Optional<IndependentSite> findEnabledBySlugWithChannel(@Param("slug") String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT site
            FROM IndependentSite site
            JOIN FETCH site.channel channel
            LEFT JOIN FETCH channel.defaultPricePlan
            WHERE site.slug = :slug
              AND site.enabled = true
            """)
    Optional<IndependentSite> findEnabledBySlugForUpdate(@Param("slug") String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT site
            FROM IndependentSite site
            JOIN FETCH site.channel channel
            LEFT JOIN FETCH channel.defaultPricePlan
            WHERE site.storeId = :storeId
              AND site.slug = :slug
              AND site.enabled = true
            """)
    Optional<IndependentSite> findEnabledByStoreIdAndSlugForUpdate(
            @Param("storeId") Long storeId,
            @Param("slug") String slug
    );

    Optional<IndependentSite> findByStoreIdAndId(Long storeId, Long id);

    List<IndependentSite> findByStoreIdOrderByCreatedAtAscIdAsc(Long storeId);

    @Query("""
            SELECT site
            FROM IndependentSite site
            JOIN FETCH site.channel channel
            LEFT JOIN FETCH channel.defaultPricePlan
            WHERE site.storeId = :storeId
              AND site.id = :id
            """)
    Optional<IndependentSite> findByStoreIdAndIdWithChannel(
            @Param("storeId") Long storeId,
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT site
            FROM IndependentSite site
            JOIN FETCH site.channel channel
            LEFT JOIN FETCH channel.defaultPricePlan
            WHERE site.storeId = :storeId
              AND site.id = :id
            """)
    Optional<IndependentSite> findByStoreIdAndIdWithChannelForUpdate(
            @Param("storeId") Long storeId,
            @Param("id") Long id
    );

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySlug(String slug);
}
