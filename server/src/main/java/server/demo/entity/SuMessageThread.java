package server.demo.entity;

import jakarta.persistence.*;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;

/**
 * Su Messaging 会话（thread）。
 * <p>
 * 说明：Su 推送的消息会带 threadid / bookingid / guestid 等字段；我们按 store + channel + threadKey 做唯一会话。
 */
@Entity
@Table(
        name = "su_message_threads",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_su_msg_thread_store_channel_key", columnNames = {"store_id", "channel_id", "thread_key"})
        },
        indexes = {
                @Index(name = "idx_su_msg_thread_store_last", columnList = "store_id,last_activity"),
                @Index(name = "idx_su_msg_thread_store_channel", columnList = "store_id,channel_id"),
                @Index(name = "idx_su_msg_thread_open", columnList = "store_id,closed,id"),
                @Index(
                        name = "idx_su_msg_thread_kb_due",
                        columnList = "knowledge_pending,knowledge_extract_after,knowledge_extracting_until,store_id,id"
                ),
                @Index(
                        name = "idx_su_msg_thread_store_kb_state",
                        columnList = "store_id,knowledge_pending,knowledge_extracted_at"
                )
        }
)
public class SuMessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * Su property ID（文档字段：hotelid）
     */
    @Column(name = "su_hotel_id", nullable = false, length = 50)
    private String suHotelId;

    /**
     * 渠道 ID（244=Airbnb，19=Booking.com）
     */
    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    /**
     * 用于会话去重的 Key。
     * Airbnb：threadid；Booking：bookingid（缺失时回退 threadid）。
     */
    @Column(name = "thread_key", nullable = false, length = 255)
    private String threadKey;

    @Column(name = "thread_id", length = 255)
    private String threadId;

    @Column(name = "booking_id", length = 255)
    private String bookingId;

    @Column(name = "guest_id", length = 255)
    private String guestId;

    @Column(name = "booking_flag", length = 10)
    private String bookingFlag;

    /**
     * Channel hotel/listing ID（文档字段：listingid）
     */
    @Column(name = "listing_id", length = 255)
    private String listingId;

    @Column(name = "guest_name", length = 120)
    private String guestName;

    @Column(name = "listing_name", length = 255)
    private String listingName;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity = UtcTimeUtil.nowLocalDateTime();

    @Column(name = "closed", nullable = false)
    private Boolean closed = false;

    @Column(name = "knowledge_pending", nullable = false)
    private Boolean knowledgePending = false;

    @Column(name = "knowledge_extract_after")
    private LocalDateTime knowledgeExtractAfter;

    @Column(name = "knowledge_extracting_until")
    private LocalDateTime knowledgeExtractingUntil;

    @Column(name = "knowledge_extracting_owner", length = 120)
    private String knowledgeExtractingOwner;

    @Column(name = "knowledge_extracted_at")
    private LocalDateTime knowledgeExtractedAt;

    @Column(name = "knowledge_extracted_message_id")
    private Long knowledgeExtractedMessageId;

    @Column(name = "knowledge_dirty_message_id")
    private Long knowledgeDirtyMessageId;

    @Column(name = "knowledge_error", length = 500)
    private String knowledgeError;

    @Column(name = "knowledge_attempt_count", nullable = false)
    private Integer knowledgeAttemptCount = 0;

    @Column(name = "knowledge_extractor_version", length = 40)
    private String knowledgeExtractorVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = UtcTimeUtil.nowLocalDateTime();
        updatedAt = UtcTimeUtil.nowLocalDateTime();
        if (lastActivity == null) {
            lastActivity = UtcTimeUtil.nowLocalDateTime();
        }
        if (closed == null) {
            closed = false;
        }
        if (knowledgePending == null) {
            knowledgePending = false;
        }
        if (knowledgeAttemptCount == null) {
            knowledgeAttemptCount = 0;
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getSuHotelId() {
        return suHotelId;
    }

    public void setSuHotelId(String suHotelId) {
        this.suHotelId = suHotelId;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getThreadKey() {
        return threadKey;
    }

    public void setThreadKey(String threadKey) {
        this.threadKey = threadKey;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getBookingFlag() {
        return bookingFlag;
    }

    public void setBookingFlag(String bookingFlag) {
        this.bookingFlag = bookingFlag;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getListingName() {
        return listingName;
    }

    public void setListingName(String listingName) {
        this.listingName = listingName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public Boolean getKnowledgePending() {
        return knowledgePending;
    }

    public void setKnowledgePending(Boolean knowledgePending) {
        this.knowledgePending = knowledgePending;
    }

    public LocalDateTime getKnowledgeExtractAfter() {
        return knowledgeExtractAfter;
    }

    public void setKnowledgeExtractAfter(LocalDateTime knowledgeExtractAfter) {
        this.knowledgeExtractAfter = knowledgeExtractAfter;
    }

    public LocalDateTime getKnowledgeExtractingUntil() {
        return knowledgeExtractingUntil;
    }

    public void setKnowledgeExtractingUntil(LocalDateTime knowledgeExtractingUntil) {
        this.knowledgeExtractingUntil = knowledgeExtractingUntil;
    }

    public String getKnowledgeExtractingOwner() {
        return knowledgeExtractingOwner;
    }

    public void setKnowledgeExtractingOwner(String knowledgeExtractingOwner) {
        this.knowledgeExtractingOwner = knowledgeExtractingOwner;
    }

    public LocalDateTime getKnowledgeExtractedAt() {
        return knowledgeExtractedAt;
    }

    public void setKnowledgeExtractedAt(LocalDateTime knowledgeExtractedAt) {
        this.knowledgeExtractedAt = knowledgeExtractedAt;
    }

    public Long getKnowledgeExtractedMessageId() {
        return knowledgeExtractedMessageId;
    }

    public void setKnowledgeExtractedMessageId(Long knowledgeExtractedMessageId) {
        this.knowledgeExtractedMessageId = knowledgeExtractedMessageId;
    }

    public Long getKnowledgeDirtyMessageId() {
        return knowledgeDirtyMessageId;
    }

    public void setKnowledgeDirtyMessageId(Long knowledgeDirtyMessageId) {
        this.knowledgeDirtyMessageId = knowledgeDirtyMessageId;
    }

    public String getKnowledgeError() {
        return knowledgeError;
    }

    public void setKnowledgeError(String knowledgeError) {
        this.knowledgeError = knowledgeError;
    }

    public Integer getKnowledgeAttemptCount() {
        return knowledgeAttemptCount;
    }

    public void setKnowledgeAttemptCount(Integer knowledgeAttemptCount) {
        this.knowledgeAttemptCount = knowledgeAttemptCount;
    }

    public String getKnowledgeExtractorVersion() {
        return knowledgeExtractorVersion;
    }

    public void setKnowledgeExtractorVersion(String knowledgeExtractorVersion) {
        this.knowledgeExtractorVersion = knowledgeExtractorVersion;
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
