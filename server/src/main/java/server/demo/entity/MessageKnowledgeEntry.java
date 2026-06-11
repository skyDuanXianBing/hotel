package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(
        name = "message_knowledge_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_msg_kb_store_source_hash",
                        columnNames = {
                                "store_id",
                                "source_guest_message_id",
                                "source_staff_message_id",
                                "normalized_hash"
                        }
                )
        }
)
public class MessageKnowledgeEntry implements StoreScopedEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String REDACTION_STATUS_REDACTED = "REDACTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_number", length = 80)
    private String roomNumber;

    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "room_type_name", length = 120)
    private String roomTypeName;

    @Column(name = "channel_id")
    private Integer channelId;

    @Column(name = "booking_key", length = 255)
    private String bookingKey;

    @Column(name = "source_guest_message_id", nullable = false)
    private Long sourceGuestMessageId;

    @Column(name = "source_staff_message_id", nullable = false)
    private Long sourceStaffMessageId;

    @Column(name = "source_timestamp")
    private LocalDateTime sourceTimestamp;

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "normalized_text", columnDefinition = "TEXT", nullable = false)
    private String normalizedText;

    @Column(name = "normalized_hash", nullable = false, length = 64)
    private String normalizedHash;

    @Column(name = "language", length = 20)
    private String language;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "pii_redaction_status", nullable = false, length = 20)
    private String piiRedactionStatus = REDACTION_STATUS_REDACTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        if (status == null) {
            status = STATUS_ACTIVE;
        }
        if (piiRedactionStatus == null) {
            piiRedactionStatus = REDACTION_STATUS_REDACTED;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (indexedAt == null) {
            indexedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UtcTimeUtil.nowLocalDateTime();
    }

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

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getBookingKey() {
        return bookingKey;
    }

    public void setBookingKey(String bookingKey) {
        this.bookingKey = bookingKey;
    }

    public Long getSourceGuestMessageId() {
        return sourceGuestMessageId;
    }

    public void setSourceGuestMessageId(Long sourceGuestMessageId) {
        this.sourceGuestMessageId = sourceGuestMessageId;
    }

    public Long getSourceStaffMessageId() {
        return sourceStaffMessageId;
    }

    public void setSourceStaffMessageId(Long sourceStaffMessageId) {
        this.sourceStaffMessageId = sourceStaffMessageId;
    }

    public LocalDateTime getSourceTimestamp() {
        return sourceTimestamp;
    }

    public void setSourceTimestamp(LocalDateTime sourceTimestamp) {
        this.sourceTimestamp = sourceTimestamp;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public void setNormalizedText(String normalizedText) {
        this.normalizedText = normalizedText;
    }

    public String getNormalizedHash() {
        return normalizedHash;
    }

    public void setNormalizedHash(String normalizedHash) {
        this.normalizedHash = normalizedHash;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPiiRedactionStatus() {
        return piiRedactionStatus;
    }

    public void setPiiRedactionStatus(String piiRedactionStatus) {
        this.piiRedactionStatus = piiRedactionStatus;
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

    public LocalDateTime getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
    }
}
