package server.demo.entity;

import jakarta.persistence.*;

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
                @Index(name = "idx_su_msg_thread_store_channel", columnList = "store_id,channel_id")
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
    private LocalDateTime lastActivity = LocalDateTime.now();

    @Column(name = "closed", nullable = false)
    private Boolean closed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (lastActivity == null) {
            lastActivity = LocalDateTime.now();
        }
        if (closed == null) {
            closed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

