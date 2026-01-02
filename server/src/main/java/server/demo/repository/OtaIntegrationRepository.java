package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.OtaIntegration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * OTA直连配置数据访问层
 */
@Repository
public interface OtaIntegrationRepository extends JpaRepository<OtaIntegration, Long> {

    /**
     * 根据门店ID查询所有OTA直连配置
     */
    List<OtaIntegration> findByStoreId(Long storeId);

    /**
     * 根据门店ID和启用状态查询
     */
    List<OtaIntegration> findByStoreIdAndEnabled(Long storeId, Boolean enabled);

    /**
     * 根据门店ID和渠道代码查询
     */
    Optional<OtaIntegration> findByStoreIdAndCode(Long storeId, String code);

    /**
     * 根据门店ID和渠道名称查询
     */
    Optional<OtaIntegration> findByStoreIdAndName(Long storeId, String name);

    /**
     * 检查门店是否存在指定渠道代码的配置
     */
    boolean existsByStoreIdAndCode(Long storeId, String code);

    @Query("select distinct o.storeId from OtaIntegration o " +
           "where o.enabled = true and o.isConnected = true and o.code in :codes")
    List<Long> findDistinctConnectedStoreIdsByCodes(@Param("codes") List<String> codes);
}
