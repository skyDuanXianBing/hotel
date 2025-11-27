package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.entity.EmailMessage;
import server.demo.entity.VirtualMailbox;
import server.demo.enums.EmailSenderType;
import server.demo.repository.EmailMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件消息服务
 * 负责邮件消息的存储、查询和管理
 */
@Service
public class EmailMessageService {

    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Autowired
    private VirtualMailboxService virtualMailboxService;

    @Autowired
    private EmailService emailService;

    /**
     * 发送邮件消息
     *
     * @param mailboxId 虚拟邮箱ID
     * @param content 消息内容
     * @param senderType 发送者类型
     * @param senderName 发送者姓名
     * @return 保存的邮件消息
     */
    @Transactional
    public EmailMessage sendEmailMessage(Long mailboxId, String content,
                                        EmailSenderType senderType, String senderName) {
        // 获取虚拟邮箱
        VirtualMailbox mailbox = virtualMailboxService.getMailboxById(mailboxId)
                .orElseThrow(() -> new RuntimeException("虚拟邮箱不存在: " + mailboxId));

        // 构建邮件消息
        EmailMessage message = new EmailMessage();
        message.setMailboxId(mailboxId);
        message.setStoreId(mailbox.getStoreId());
        message.setSenderEmail(mailbox.getEmailAddress());
        message.setSenderName(senderName);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());

        // 获取收件人邮箱(从订单获取客人邮箱)
        String recipientEmail = getRecipientEmailForMailbox(mailboxId);
        message.setRecipientEmail(recipientEmail);

        // 发送邮件
        try {
            emailService.sendEmail(
                mailbox.getEmailAddress(),
                recipientEmail,
                "订单沟通 - " + mailbox.getDisplayName(),
                content,
                null
            );
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }

        // 保存到数据库
        return emailMessageRepository.save(message);
    }

    /**
     * 获取虚拟邮箱的所有消息
     *
     * @param mailboxId 虚拟邮箱ID
     * @return 消息列表
     */
    public List<EmailMessage> getMessagesByMailbox(Long mailboxId) {
        return emailMessageRepository.findByMailboxIdOrderBySentAtAsc(mailboxId);
    }

    /**
     * 轮询新消息(指定时间之后的消息)
     *
     * @param mailboxId 虚拟邮箱ID
     * @param since 起始时间
     * @return 新消息列表
     */
    public List<EmailMessage> pollNewMessages(Long mailboxId, LocalDateTime since) {
        return emailMessageRepository.findByMailboxIdAndSentAtAfterOrderBySentAtAsc(mailboxId, since);
    }

    /**
     * 保存接收到的邮件消息
     *
     * @param mailboxId 虚拟邮箱ID
     * @param messageId 邮件Message-ID
     * @param senderEmail 发件人邮箱
     * @param senderName 发件人姓名
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param htmlContent HTML内容
     * @param receivedAt 接收时间
     * @return 保存的邮件消息
     */
    @Transactional
    public EmailMessage saveReceivedEmail(Long mailboxId, String messageId,
                                         String senderEmail, String senderName,
                                         String subject, String content,
                                         String htmlContent, LocalDateTime receivedAt) {
        // 检查是否已存在(去重)
        if (messageId != null && emailMessageRepository.existsByMessageId(messageId)) {
            return emailMessageRepository.findByMessageId(messageId).orElse(null);
        }

        // 获取虚拟邮箱
        VirtualMailbox mailbox = virtualMailboxService.getMailboxById(mailboxId)
                .orElseThrow(() -> new RuntimeException("虚拟邮箱不存在: " + mailboxId));

        // 创建邮件消息
        EmailMessage message = new EmailMessage();
        message.setMailboxId(mailboxId);
        message.setStoreId(mailbox.getStoreId());
        message.setMessageId(messageId);
        message.setSenderEmail(senderEmail);
        message.setSenderName(senderName);
        message.setSenderType(EmailSenderType.GUEST); // 外部邮件视为客人发送
        message.setRecipientEmail(mailbox.getEmailAddress());
        message.setSubject(subject);
        message.setContent(content);
        message.setHtmlContent(htmlContent);
        message.setSentAt(receivedAt != null ? receivedAt : LocalDateTime.now());
        message.setReceivedAt(LocalDateTime.now());

        return emailMessageRepository.save(message);
    }

    /**
     * 标记消息为已读
     *
     * @param messageId 消息ID
     */
    @Transactional
    public void markAsRead(Long messageId) {
        EmailMessage message = emailMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在: " + messageId));

        message.setIsRead(true);
        emailMessageRepository.save(message);
    }

    /**
     * 统计未读消息数量
     *
     * @param mailboxId 虚拟邮箱ID
     * @return 未读消息数量
     */
    public long countUnreadMessages(Long mailboxId) {
        return emailMessageRepository.countByMailboxIdAndIsReadFalse(mailboxId);
    }

    /**
     * 获取虚拟邮箱的收件人邮箱
     * (从关联的订单获取客人邮箱)
     *
     * @param mailboxId 虚拟邮箱ID
     * @return 收件人邮箱
     */
    private String getRecipientEmailForMailbox(Long mailboxId) {
        // TODO: 实现从订单获取客人邮箱的逻辑
        // 这里需要关联Reservation实体获取guest_email字段
        // 暂时返回一个占位符,后续需要在Reservation实体中添加guestEmail字段
        return "guest@example.com";
    }
}
