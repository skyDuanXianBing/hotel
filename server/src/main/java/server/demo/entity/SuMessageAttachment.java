package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(
        name = "su_message_attachments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_su_msg_attachment_external",
                columnNames = {"store_id", "message_id", "external_attachment_id"}
        ),
        indexes = {
                @Index(name = "idx_su_msg_attachment_store_thread", columnList = "store_id,thread_id"),
                @Index(name = "idx_su_msg_attachment_message", columnList = "message_id")
        }
)
public class SuMessageAttachment implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private SuMessageThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private SuMessage message;

    @Column(name = "external_attachment_id", nullable = false, length = 255)
    private String externalAttachmentId;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public Long getStoreId() { return storeId; }

    @Override
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public SuMessageThread getThread() { return thread; }
    public void setThread(SuMessageThread thread) { this.thread = thread; }

    public SuMessage getMessage() { return message; }
    public void setMessage(SuMessage message) { this.message = message; }

    public String getExternalAttachmentId() { return externalAttachmentId; }
    public void setExternalAttachmentId(String externalAttachmentId) { this.externalAttachmentId = externalAttachmentId; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
