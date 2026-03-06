package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.time.LocalDateTime;

/**
 * 企业邮箱配置实体
 * 存储门店的企业邮箱配置信息,用于虚拟邮箱的邮件收发
 */
@Entity
@Table(name = "enterprise_email_configs")
@EntityListeners(StoreScopedEntityListener.class)
public class EnterpriseEmailConfig implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 门店ID(门店级架构)
     */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 企业主邮箱
     */
    @Column(name = "parent_email", nullable = false, length = 255)
    private String parentEmail;

    /**
     * 子邮箱前缀模板
     * 例如: "order-{orderNumber}"
     */
    @Column(name = "email_prefix", nullable = false, length = 100)
    private String emailPrefix;

    /**
     * 邮箱域名
     * 例如: "myhotel.the-host.jp"
     */
    @Column(name = "email_domain", nullable = false, length = 100)
    private String emailDomain;

    /**
     * SMTP服务器地址
     */
    @Column(name = "smtp_host", length = 100)
    private String smtpHost;

    /**
     * SMTP端口
     */
    @Column(name = "smtp_port")
    private Integer smtpPort;

    /**
     * IMAP服务器地址
     */
    @Column(name = "imap_host", length = 100)
    private String imapHost;

    /**
     * IMAP端口
     */
    @Column(name = "imap_port")
    private Integer imapPort;

    /**
     * 邮箱账号
     */
    @Column(length = 255)
    private String username;

    /**
     * 邮箱密码(应使用加密存储)
     */
    @Column(length = 255)
    private String password;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public EnterpriseEmailConfig() {}

    public EnterpriseEmailConfig(String parentEmail, String emailPrefix, String emailDomain) {
        this.parentEmail = parentEmail;
        this.emailPrefix = emailPrefix;
        this.emailDomain = emailDomain;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }

    public String getEmailPrefix() {
        return emailPrefix;
    }

    public void setEmailPrefix(String emailPrefix) {
        this.emailPrefix = emailPrefix;
    }

    public String getEmailDomain() {
        return emailDomain;
    }

    public void setEmailDomain(String emailDomain) {
        this.emailDomain = emailDomain;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getImapHost() {
        return imapHost;
    }

    public void setImapHost(String imapHost) {
        this.imapHost = imapHost;
    }

    public Integer getImapPort() {
        return imapPort;
    }

    public void setImapPort(Integer imapPort) {
        this.imapPort = imapPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
