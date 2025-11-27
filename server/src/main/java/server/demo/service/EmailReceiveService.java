package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.demo.entity.VirtualMailbox;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 邮件接收服务
 * 负责通过IMAP轮询接收邮件
 *
 * 注意: 此服务需要配置IMAP服务器信息
 * 在application.yml中配置:
 * mail:
 *   imap:
 *     host: imap.example.com
 *     port: 993
 *     username: your-email@example.com
 *     password: your-password
 */
@Service
public class EmailReceiveService {

    private static final Logger logger = LoggerFactory.getLogger(EmailReceiveService.class);

    @Autowired
    private VirtualMailboxService virtualMailboxService;

    @Autowired
    private EmailMessageService emailMessageService;

    @Value("${mail.imap.host:#{null}}")
    private String imapHost;

    @Value("${mail.imap.port:993}")
    private int imapPort;

    @Value("${mail.imap.username:#{null}}")
    private String imapUsername;

    @Value("${mail.imap.password:#{null}}")
    private String imapPassword;

    private boolean isConfigured = false;

    /**
     * 定时轮询邮件
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    public void pollEmails() {
        // 检查配置
        if (!checkConfiguration()) {
            if (!isConfigured) {
                logger.warn("IMAP邮件接收未配置,跳过轮询。请在application.yml中配置mail.imap.*");
                isConfigured = false;
            }
            return;
        }

        isConfigured = true;

        try {
            logger.debug("开始轮询邮件...");
            receiveEmails();
        } catch (Exception e) {
            logger.error("邮件轮询失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查IMAP配置是否完整
     */
    private boolean checkConfiguration() {
        return imapHost != null && imapUsername != null && imapPassword != null;
    }

    /**
     * 接收邮件的主要逻辑
     *
     * TODO: 实现IMAP邮件接收
     * 步骤:
     * 1. 连接到IMAP服务器
     * 2. 打开收件箱
     * 3. 搜索未读邮件
     * 4. 解析邮件内容
     * 5. 匹配虚拟邮箱
     * 6. 保存到数据库
     * 7. 标记为已读
     */
    private void receiveEmails() {
        logger.info("IMAP邮件接收功能当前为占位实现,需要完整的IMAP客户端集成");

        // 基础实现框架:
        // 1. 使用Jakarta Mail API连接IMAP
        // 2. 获取未读邮件
        // 3. 解析邮件
        // 4. 保存到数据库

        /*
        示例代码结构:

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(imapHost, imapUsername, imapPassword);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        // 搜索未读邮件
        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        for (Message message : messages) {
            String from = message.getFrom()[0].toString();
            String to = message.getRecipients(Message.RecipientType.TO)[0].toString();
            String subject = message.getSubject();
            String content = extractTextContent(message);

            // 匹配虚拟邮箱
            Optional<VirtualMailbox> mailbox = virtualMailboxService.getMailboxByEmailAddress(to);
            if (mailbox.isPresent()) {
                // 保存到数据库
                emailMessageService.saveReceivedEmail(
                    mailbox.get().getId(),
                    message.getHeader("Message-ID")[0],
                    from,
                    extractName(from),
                    subject,
                    content,
                    extractHtmlContent(message),
                    LocalDateTime.now()
                );

                // 标记为已读
                message.setFlag(Flags.Flag.SEEN, true);
            }
        }

        inbox.close(false);
        store.close();
        */
    }

    /**
     * 解析邮件收件人,匹配虚拟邮箱
     *
     * @param toAddress 收件人地址
     * @return 虚拟邮箱
     */
    private Optional<VirtualMailbox> matchVirtualMailbox(String toAddress) {
        // 提取邮箱地址(去除显示名称)
        String email = extractEmailAddress(toAddress);
        return virtualMailboxService.getMailboxByEmailAddress(email);
    }

    /**
     * 从邮箱地址字符串中提取纯邮箱
     * 例如: "Name <email@example.com>" -> "email@example.com"
     *
     * @param address 邮箱地址字符串
     * @return 纯邮箱地址
     */
    private String extractEmailAddress(String address) {
        if (address.contains("<") && address.contains(">")) {
            int start = address.indexOf('<');
            int end = address.indexOf('>');
            return address.substring(start + 1, end);
        }
        return address.trim();
    }

    /**
     * 从邮箱地址字符串中提取显示名称
     *
     * @param address 邮箱地址字符串
     * @return 显示名称
     */
    private String extractName(String address) {
        if (address.contains("<")) {
            return address.substring(0, address.indexOf('<')).trim();
        }
        return null;
    }
}
