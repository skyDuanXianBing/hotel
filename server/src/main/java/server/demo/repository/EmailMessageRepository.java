package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.EmailMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件消息Repository
 */
@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {

    /**
     * 根据虚拟邮箱ID查找所有消息,按发送时间排序
     */
    List<EmailMessage> findByMailboxIdOrderBySentAtAsc(Long mailboxId);

    /**
     * 根据虚拟邮箱ID和时间范围查找消息
     */
    List<EmailMessage> findByMailboxIdAndSentAtAfterOrderBySentAtAsc(Long mailboxId, LocalDateTime since);

    /**
     * 根据Message-ID查找消息(用于去重)
     */
    Optional<EmailMessage> findByMessageId(String messageId);

    /**
     * 检查Message-ID是否已存在
     */
    boolean existsByMessageId(String messageId);

    /**
     * 统计虚拟邮箱的未读消息数量
     */
    long countByMailboxIdAndIsReadFalse(Long mailboxId);

    /**
     * 根据门店ID查找所有消息
     */
    List<EmailMessage> findByStoreId(Long storeId);
}
