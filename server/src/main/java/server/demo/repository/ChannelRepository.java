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

    // 按用户ID查询所有渠道
    List<Channel> findByUserId(Long userId);

    // 按用户ID和代码查询
    Optional<Channel> findByUserIdAndCode(Long userId, String code);

    // 按用户ID和类型查询
    List<Channel> findByUserIdAndType(Long userId, ChannelType type);

    // 按用户ID查询激活的渠道
    List<Channel> findByUserIdAndIsActiveTrue(Long userId);

    // 按用户ID和名称模糊查询
    List<Channel> findByUserIdAndNameContainingIgnoreCase(Long userId, String name);

    // 按用户ID查询激活的渠道并排序
    @Query("SELECT c FROM Channel c WHERE c.user.id = :userId AND c.isActive = true ORDER BY c.type, c.name")
    List<Channel> findActiveChannelsByUserIdOrderByTypeAndName(@Param("userId") Long userId);

    // 检查用户的渠道代码是否存在
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