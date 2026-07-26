package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.IndependentSitePublication;
import server.demo.enums.IndependentSitePublicationType;

import java.util.List;

@Repository
public interface IndependentSitePublicationRepository extends JpaRepository<IndependentSitePublication, Long> {

    List<IndependentSitePublication> findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc(
            Long storeId,
            Long siteId
    );

    List<IndependentSitePublication> findByStoreIdAndSiteIdAndTargetTypeAndEnabledTrueOrderByDisplayOrderAscIdAsc(
            Long storeId,
            Long siteId,
            IndependentSitePublicationType targetType
    );

    /**
     * 发布物重同步前的整站清除。
     * 必须用 JPQL 批量删除（立即执行）：派生删除会延迟到事务 flush，而 Hibernate 先插后删，
     * 会导致重新插入相同 (site_id, target_type, target_id) 时撞唯一键。
     */
    @Modifying
    @Query("DELETE FROM IndependentSitePublication p WHERE p.storeId = :storeId AND p.site.id = :siteId")
    int deleteByStoreIdAndSiteIdInBulk(@Param("storeId") Long storeId, @Param("siteId") Long siteId);

    long countByStoreIdAndSiteId(Long storeId, Long siteId);
}
