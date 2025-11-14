package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.ConsumptionItem;

import java.util.List;

@Repository
public interface ConsumptionItemRepository extends JpaRepository<ConsumptionItem, Long> {

    /**
     * 根据门店ID查找所有消费项（门店级架构）
     */
    List<ConsumptionItem> findByStoreId(Long storeId);

    /**
     * 根据门店ID和分类查找消费项
     */
    List<ConsumptionItem> findByStoreIdAndCategory(Long storeId, String category);

    /**
     * 根据门店ID和启用状态查找消费项
     */
    List<ConsumptionItem> findByStoreIdAndEnabled(Long storeId, Boolean enabled);

    /**
     * @deprecated 已废弃，使用findByStoreId替代
     */
    @Deprecated
    List<ConsumptionItem> findByUserId(Long userId);

    /**
     * @deprecated 已废弃，使用findByStoreIdAndCategory替代
     */
    @Deprecated
    List<ConsumptionItem> findByUserIdAndCategory(Long userId, String category);

    /**
     * @deprecated 已废弃，使用findByStoreIdAndEnabled替代
     */
    @Deprecated
    List<ConsumptionItem> findByUserIdAndEnabled(Long userId, Boolean enabled);
}
