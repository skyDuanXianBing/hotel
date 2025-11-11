package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.ConsumptionItem;

import java.util.List;

@Repository
public interface ConsumptionItemRepository extends JpaRepository<ConsumptionItem, Long> {

    /**
     * 根据用户ID查找所有消费项
     */
    List<ConsumptionItem> findByUserId(Long userId);

    /**
     * 根据用户ID和分类查找消费项
     */
    List<ConsumptionItem> findByUserIdAndCategory(Long userId, String category);

    /**
     * 根据用户ID和启用状态查找消费项
     */
    List<ConsumptionItem> findByUserIdAndEnabled(Long userId, Boolean enabled);
}
