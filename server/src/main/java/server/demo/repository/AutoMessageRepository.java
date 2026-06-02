package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.AutoMessage;

import java.util.List;

/**
 * 自动化消息 Repository
 */
@Repository
public interface AutoMessageRepository extends JpaRepository<AutoMessage, Long> {

    /**
     * 根据用户ID查找自动化消息列表(已废弃,使用门店级查询)
     */
    @Deprecated
    List<AutoMessage> findByUserId(Long userId);

    /**
     * 根据用户ID和启用状态查找自动化消息列表(已废弃,使用门店级查询)
     */
    @Deprecated
    List<AutoMessage> findByUserIdAndEnabled(Long userId, Boolean enabled);

    // 门店级查询方法
    List<AutoMessage> findByStoreId(Long storeId);

    List<AutoMessage> findByStoreIdAndEnabled(Long storeId, Boolean enabled);

    /**
     * 根据门店ID查找启用的自动化消息
     */
    List<AutoMessage> findByStoreIdAndEnabledTrue(Long storeId);

    List<AutoMessage> findByStoreIdAndActionAndSendTimingOrderByIdAsc(
            Long storeId,
            String action,
            String sendTiming
    );
}
