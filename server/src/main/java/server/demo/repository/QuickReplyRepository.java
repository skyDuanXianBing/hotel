package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.QuickReply;

import java.util.List;

@Repository
public interface QuickReplyRepository extends JpaRepository<QuickReply, Long> {
    /**
     * 根据用户ID查找所有快捷回复(已废弃,使用门店级查询)
     */
    @Deprecated
    List<QuickReply> findByUserId(Long userId);

    // 门店级查询方法
    List<QuickReply> findByStoreId(Long storeId);
}
