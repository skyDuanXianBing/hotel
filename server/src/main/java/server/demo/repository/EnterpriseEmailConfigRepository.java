package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.EnterpriseEmailConfig;

import java.util.Optional;

/**
 * 企业邮箱配置Repository
 */
@Repository
public interface EnterpriseEmailConfigRepository extends JpaRepository<EnterpriseEmailConfig, Long> {

    /**
     * 根据门店ID查找启用的邮箱配置
     */
    Optional<EnterpriseEmailConfig> findByStoreIdAndEnabledTrue(Long storeId);

    /**
     * 根据门店ID查找邮箱配置
     */
    Optional<EnterpriseEmailConfig> findByStoreId(Long storeId);
}
