package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.ConsumptionCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionCategoryRepository extends JpaRepository<ConsumptionCategory, Long> {

    /**
     * 根据门店ID查找所有分类（门店级架构）
     */
    List<ConsumptionCategory> findByStoreId(Long storeId);

    /**
     * 根据门店ID和分类名称查找分类（门店级架构）
     */
    Optional<ConsumptionCategory> findByStoreIdAndName(Long storeId, String name);

    /**
     * @deprecated 已废弃，使用门店级架构
     */
    @Deprecated
    List<ConsumptionCategory> findByUserId(Long userId);

    /**
     * @deprecated 已废弃，使用门店级架构
     */
    @Deprecated
    Optional<ConsumptionCategory> findByUserIdAndName(Long userId, String name);
}
