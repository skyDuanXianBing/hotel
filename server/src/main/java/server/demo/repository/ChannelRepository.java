package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Channel;
import server.demo.enums.ChannelType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    // 按门店ID查询所有渠道（门店级架构）
    List<Channel> findByStoreId(Long storeId);

    // 按门店ID和代码查询
    Optional<Channel> findByStoreIdAndCode(Long storeId, String code);

    // 按门店ID和类型查询
    List<Channel> findByStoreIdAndType(Long storeId, ChannelType type);

    // 按门店ID查询激活的渠道
    List<Channel> findByStoreIdAndIsActiveTrue(Long storeId);

    // 按门店ID和名称模糊查询
    List<Channel> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name);

    // 按门店ID查询激活的渠道并排序
    @Query("SELECT c FROM Channel c WHERE c.storeId = :storeId AND c.isActive = true ORDER BY c.type, c.name")
    List<Channel> findActiveChannelsByStoreIdOrderByTypeAndName(@Param("storeId") Long storeId);

    // 检查门店的渠道代码是否存在
    boolean existsByStoreIdAndCode(Long storeId, String code);

    @Query("SELECT DISTINCT c.storeId FROM Channel c " +
            "WHERE c.storeId IS NOT NULL " +
            "AND c.enabled = true " +
            "AND c.autoSyncPrice = true " +
            "AND c.code IN :codes")
    List<Long> findDistinctStoreIdsByEnabledAutoSyncAndCodes(@Param("codes") List<String> codes);

    /**
     * @deprecated 已废弃，使用findByStoreId替代
     */
    @Deprecated
    List<Channel> findByUserId(Long userId);

    /**
     * @deprecated 已废弃，使用findByStoreIdAndCode替代
     */
    @Deprecated
    Optional<Channel> findByUserIdAndCode(Long userId, String code);

    /**
     * @deprecated 已废弃，使用findByStoreIdAndType替代
     */
    @Deprecated
    List<Channel> findByUserIdAndType(Long userId, ChannelType type);

    /**
     * @deprecated 已废弃，使用findByStoreIdAndIsActiveTrue替代
     */
    @Deprecated
    List<Channel> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * @deprecated 已废弃，使用findByStoreIdAndNameContainingIgnoreCase替代
     */
    @Deprecated
    List<Channel> findByUserIdAndNameContainingIgnoreCase(Long userId, String name);

    /**
     * @deprecated 已废弃，使用findActiveChannelsByStoreIdOrderByTypeAndName替代
     */
    @Deprecated
    @Query("SELECT c FROM Channel c WHERE c.user.id = :userId AND c.isActive = true ORDER BY c.type, c.name")
    List<Channel> findActiveChannelsByUserIdOrderByTypeAndName(@Param("userId") Long userId);

    /**
     * @deprecated 已废弃，使用existsByStoreIdAndCode替代
     */
    @Deprecated
    boolean existsByUserIdAndCode(Long userId, String code);

    // 以下为旧方法，保留用于数据迁移兼容
    Optional<Channel> findByCode(String code);
    List<Channel> findByType(ChannelType type);
    List<Channel> findByIsActiveTrue();
    List<Channel> findByNameContainingIgnoreCase(String name);
    @Query("SELECT c FROM Channel c WHERE c.isActive = true ORDER BY c.type, c.name")
    List<Channel> findActiveChannelsOrderByTypeAndName();
    boolean existsByCode(String code);
}
