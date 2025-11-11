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
     * 根据用户ID查找自动化消息列表
     */
    List<AutoMessage> findByUserId(Long userId);

    /**
     * 根据用户ID和启用状态查找自动化消息列表
     */
    List<AutoMessage> findByUserIdAndEnabled(Long userId, Boolean enabled);
}
