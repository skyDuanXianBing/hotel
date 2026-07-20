package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.ReviewActionStatus;
import server.demo.enums.ReviewActionType;
import server.demo.enums.ReviewAssociationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "channel_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_reviews_external",
                columnNames = {"store_id", "su_channel_id", "channel_review_id", "review_type"}
        ),
        indexes = {
                @Index(name = "idx_channel_reviews_store_received", columnList = "store_id,received_at,id"),
                @Index(name = "idx_channel_reviews_store_reservation", columnList = "store_id,reservation_id"),
                @Index(name = "idx_channel_reviews_store_association", columnList = "store_id,association_status"),
                @Index(name = "idx_channel_reviews_store_action", columnList = "store_id,last_action_status")
        }
)
@EntityListeners(StoreScopedEntityListener.class)
public class ChannelReview implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "association_status", nullable = false, length = 20)
    private ReviewAssociationStatus associationStatus = ReviewAssociationStatus.UNLINKED;

    @Column(name = "association_reason", length = 500)
    private String associationReason;

    @Column(name = "pms_channel_code", nullable = false, length = 30)
    private String pmsChannelCode;

    @Column(name = "su_channel_id", nullable = false)
    private Integer suChannelId;

    @Column(name = "hotel_id", nullable = false, length = 50)
    private String hotelId;

    @Column(name = "channel_property_id", nullable = false, length = 150)
    private String channelPropertyId;

    @Column(name = "listing_id", length = 255)
    private String listingId;

    @Column(name = "channel_review_id", nullable = false, length = 255)
    private String channelReviewId;

    @Column(name = "channel_booking_id", length = 255)
    private String channelBookingId;

    @Column(name = "review_type", nullable = false, length = 50)
    private String reviewType;

    @Column(name = "review_status", length = 50)
    private String reviewStatus;

    @Column(name = "can_reply", nullable = false)
    private Boolean canReply = false;

    @Column(name = "can_review_guest", nullable = false)
    private Boolean canReviewGuest = false;

    @Column(name = "guest_name", length = 200)
    private String guestName;

    @Column(name = "property_name", length = 255)
    private String propertyName;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "review_title", length = 500)
    private String reviewTitle;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "negative_review_text", columnDefinition = "TEXT")
    private String negativeReviewText;

    @Column(name = "private_feedback", columnDefinition = "TEXT")
    private String privateFeedback;

    @Column(name = "overall_score", precision = 6, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "category_ratings_json", columnDefinition = "LONGTEXT")
    private String categoryRatingsJson;

    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "reply_at")
    private LocalDateTime replyAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_action_type", length = 30)
    private ReviewActionType lastActionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_action_status", length = 30)
    private ReviewActionStatus lastActionStatus;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Column(name = "source_event_hash", length = 64)
    private String sourceEventHash;

    @Column(name = "su_ruid", length = 100)
    private String suRuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (associationStatus == null) {
            associationStatus = ReviewAssociationStatus.UNLINKED;
        }
        if (canReply == null) {
            canReply = false;
        }
        if (canReviewGuest == null) {
            canReviewGuest = false;
        }
        if (lastSyncedAt == null) {
            lastSyncedAt = now;
        }
        createdAt = now;
        updatedAt = now;
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

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public ReviewAssociationStatus getAssociationStatus() {
        return associationStatus;
    }

    public void setAssociationStatus(ReviewAssociationStatus associationStatus) {
        this.associationStatus = associationStatus;
    }

    public String getAssociationReason() {
        return associationReason;
    }

    public void setAssociationReason(String associationReason) {
        this.associationReason = associationReason;
    }

    public String getPmsChannelCode() {
        return pmsChannelCode;
    }

    public void setPmsChannelCode(String pmsChannelCode) {
        this.pmsChannelCode = pmsChannelCode;
    }

    public Integer getSuChannelId() {
        return suChannelId;
    }

    public void setSuChannelId(Integer suChannelId) {
        this.suChannelId = suChannelId;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getChannelPropertyId() {
        return channelPropertyId;
    }

    public void setChannelPropertyId(String channelPropertyId) {
        this.channelPropertyId = channelPropertyId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getChannelReviewId() {
        return channelReviewId;
    }

    public void setChannelReviewId(String channelReviewId) {
        this.channelReviewId = channelReviewId;
    }

    public String getChannelBookingId() {
        return channelBookingId;
    }

    public void setChannelBookingId(String channelBookingId) {
        this.channelBookingId = channelBookingId;
    }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Boolean getCanReply() {
        return canReply;
    }

    public void setCanReply(Boolean canReply) {
        this.canReply = canReply;
    }

    public Boolean getCanReviewGuest() {
        return canReviewGuest;
    }

    public void setCanReviewGuest(Boolean canReviewGuest) {
        this.canReviewGuest = canReviewGuest;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public String getReviewTitle() {
        return reviewTitle;
    }

    public void setReviewTitle(String reviewTitle) {
        this.reviewTitle = reviewTitle;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getNegativeReviewText() {
        return negativeReviewText;
    }

    public void setNegativeReviewText(String negativeReviewText) {
        this.negativeReviewText = negativeReviewText;
    }

    public String getPrivateFeedback() {
        return privateFeedback;
    }

    public void setPrivateFeedback(String privateFeedback) {
        this.privateFeedback = privateFeedback;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public String getCategoryRatingsJson() {
        return categoryRatingsJson;
    }

    public void setCategoryRatingsJson(String categoryRatingsJson) {
        this.categoryRatingsJson = categoryRatingsJson;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public LocalDateTime getReplyAt() {
        return replyAt;
    }

    public void setReplyAt(LocalDateTime replyAt) {
        this.replyAt = replyAt;
    }

    public ReviewActionType getLastActionType() {
        return lastActionType;
    }

    public void setLastActionType(ReviewActionType lastActionType) {
        this.lastActionType = lastActionType;
    }

    public ReviewActionStatus getLastActionStatus() {
        return lastActionStatus;
    }

    public void setLastActionStatus(ReviewActionStatus lastActionStatus) {
        this.lastActionStatus = lastActionStatus;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getSourceEventHash() {
        return sourceEventHash;
    }

    public void setSourceEventHash(String sourceEventHash) {
        this.sourceEventHash = sourceEventHash;
    }

    public String getSuRuid() {
        return suRuid;
    }

    public void setSuRuid(String suRuid) {
        this.suRuid = suRuid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
