package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.PriceLabsIntegration;

import java.util.Optional;

/**
 * PriceLabs 集成配置 Repository
 */
@Repository
public interface PriceLabsIntegrationRepository extends JpaRepository<PriceLabsIntegration, Long> {

    /**
     * 根据门店ID查找集成配置
     */
    Optional<PriceLabsIntegration> findByStoreId(Long storeId);

    /**
     * 检查门店是否存在集成配置
     */
    boolean existsByStoreId(Long storeId);

    /**
     * 根据门店ID删除集成配置
     */
    void deleteByStoreId(Long storeId);
}
