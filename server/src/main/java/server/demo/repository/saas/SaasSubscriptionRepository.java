package server.demo.repository.saas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasSubscriptionStatus;

import java.util.List;
import java.util.Optional;

public interface SaasSubscriptionRepository extends JpaRepository<SaasSubscription, Long> {

    List<SaasSubscription> findByStoreIdAndStatus(Long storeId, SaasSubscriptionStatus status);

    Optional<SaasSubscription> findFirstByStoreIdAndStatusOrderByEndTimeDesc(Long storeId, SaasSubscriptionStatus status);

    /** 幂等重放兜底：门店最新一条订阅（不限状态，用于当前无 ACTIVE 时返回最近订阅视图）。 */
    Optional<SaasSubscription> findFirstByStoreIdOrderByIdDesc(Long storeId);

    /** 管理端订阅列表：全量分页（按 id 倒序）。 */
    Page<SaasSubscription> findAllByOrderByIdDesc(Pageable pageable);

    /** 管理端订阅列表：按门店筛选分页。 */
    Page<SaasSubscription> findByStoreIdOrderByIdDesc(Long storeId, Pageable pageable);

    long countByStatus(SaasSubscriptionStatus status);

    /**
     * 管理端概览：各套餐的 ACTIVE 订阅数。返回 [packageName, count] 行。
     */
    @Query("SELECT s.packageName, COUNT(s) FROM SaasSubscription s "
            + "WHERE s.status = :status GROUP BY s.packageName ORDER BY COUNT(s) DESC")
    List<Object[]> countGroupByPackageName(@Param("status") SaasSubscriptionStatus status);
}
