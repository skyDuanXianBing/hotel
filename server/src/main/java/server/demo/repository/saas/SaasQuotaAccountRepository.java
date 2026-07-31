package server.demo.repository.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.saas.SaasQuotaAccount;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SaasQuotaAccountRepository extends JpaRepository<SaasQuotaAccount, Long> {

    Optional<SaasQuotaAccount> findByStoreIdAndFeatureCode(Long storeId, String featureCode);

    /**
     * 并发安全预扣：数据库原子条件 UPDATE，仅当 used+delta 不超过总额（或总额为 NULL=不限）才生效。
     * 返回受影响行数，0 = 额度不足。避免先读后写的竞态，也无需长事务持行锁。
     */
    @Modifying
    @Query("UPDATE SaasQuotaAccount q SET q.usedQuota = q.usedQuota + :delta, q.version = q.version + 1 "
            + "WHERE q.storeId = :storeId AND q.featureCode = :featureCode "
            + "AND (q.totalQuota IS NULL OR q.usedQuota + :delta <= q.totalQuota)")
    int deductIfAvailable(@Param("storeId") Long storeId,
                          @Param("featureCode") String featureCode,
                          @Param("delta") long delta);

    /**
     * 失败返还：回补 used，下限为 0（CASE 分支仅在 0 &lt; used &lt; delta 的过度返还时作最终钳制）。
     * used &gt; 0 守卫（审查 G6/G2 修复）：重复返还、used=0 的无效返还、以及 used 为负
     * （人工补偿未消耗）的返还均不命中行——不写流水、version 不自增，补偿额度不被吞。
     */
    @Modifying
    @Query("UPDATE SaasQuotaAccount q "
            + "SET q.usedQuota = CASE WHEN q.usedQuota >= :delta THEN q.usedQuota - :delta ELSE 0 END, "
            + "q.version = q.version + 1 "
            + "WHERE q.storeId = :storeId AND q.featureCode = :featureCode "
            + "AND q.usedQuota > 0")
    int refund(@Param("storeId") Long storeId,
               @Param("featureCode") String featureCode,
               @Param("delta") long delta);

    /**
     * 人工调整剩余额度（管理端）：delta &gt; 0 增加剩余（used 减少，允许变负，
     * 补偿后剩余额度可超过套餐总额）；delta &lt; 0 扣减剩余（used 增加，可超总额=处罚语义）。
     * 单语句原子更新，无先读后写竞态。
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SaasQuotaAccount q "
            + "SET q.usedQuota = q.usedQuota - :delta, "
            + "q.version = q.version + 1 "
            + "WHERE q.id = :id")
    int adjustUsedByRemainingDelta(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 管理端概览：某 feature 全平台已用配额总量。
     * 人工补偿贷记可使单行 used_quota 为负（剩余额度多于套餐总额，属正常状态），
     * 展示口径按行夹回 0 再求和，避免总量出现负数。
     */
    @Query("SELECT COALESCE(SUM(GREATEST(q.usedQuota, 0)), 0) FROM SaasQuotaAccount q WHERE q.featureCode = :featureCode")
    Long sumUsedQuotaByFeatureCode(@Param("featureCode") String featureCode);

    /**
     * 惰性周期滚动：仅当数据库中窗口仍过期才重置（并发下只有一个事务生效）。
     * clearAutomatically 清除可能过期的 L1 缓存实体，保证后续读取到新窗口。
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SaasQuotaAccount q SET q.usedQuota = 0, q.periodStart = :periodStart, q.periodEnd = :periodEnd, "
            + "q.version = q.version + 1 "
            + "WHERE q.id = :id AND q.periodEnd <= :now")
    int resetPeriodIfExpired(@Param("id") Long id,
                             @Param("now") LocalDateTime now,
                             @Param("periodStart") LocalDateTime periodStart,
                             @Param("periodEnd") LocalDateTime periodEnd);
}
