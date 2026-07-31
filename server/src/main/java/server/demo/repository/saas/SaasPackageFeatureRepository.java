package server.demo.repository.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.saas.SaasPackageFeature;

import java.util.List;

public interface SaasPackageFeatureRepository extends JpaRepository<SaasPackageFeature, Long> {

    List<SaasPackageFeature> findByPackageId(Long packageId);

    /** 功能字典 type 修改杠杆守卫（审查 E5）：该 feature 是否已被任何套餐权益引用。 */
    boolean existsByFeatureCode(String featureCode);

    /**
     * 整体替换权益时的批量删除：单条 DELETE 立即执行，不进入 Hibernate ActionQueue。
     * 若用 deleteAll(entities) 逐实体删除，ActionQueue 按 insert-before-delete 排序，
     * 同事务内新旧行 (package_id, feature_code) 重叠时先插后删会撞 uk_saas_package_feature。
     * flushAutomatically 先刷出上下文中的待写变更，保证本删除先于后续 INSERT 落库；
     * clearAutomatically 清掉上下文中可能残留的旧权益实体，避免后续读取到已删行。
     *
     * @return 实际删除行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SaasPackageFeature f WHERE f.packageId = :packageId")
    int deleteByPackageId(@Param("packageId") Long packageId);
}
