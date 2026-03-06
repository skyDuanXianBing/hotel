package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservations_store_order",
                columnNames = {"store_id", "order_number"}
        )
)
public class Reservation implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    @NotNull(message = "渠道不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @NotBlank(message = "客人姓名不能为空")
    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Column(name = "guest_id_card", length = 50)
    private String guestIdCard;

    @NotNull(message = "入住日期不能为空")
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @NotNull(message = "退房日期不能为空")
    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "actual_check_in")
    private LocalDateTime actualCheckIn;

    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    @NotNull(message = "成人数量不能为空")
    @Min(value = 1, message = "成人数量必须大于0")
    private Integer adults;

    private Integer children = 0;

    @NotNull(message = "总金额不能为空")
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "group_order_no", length = 50)
    private String groupOrderNo;

    @Column(name = "channel_order_number", length = 100)
    private String channelOrderNumber;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "commission", precision = 10, scale = 2)
    private BigDecimal commission;

    @Column(name = "other_fees", precision = 10, scale = 2)
    private BigDecimal otherFees;

    @Column(name = "price_plan", length = 100)
    private String pricePlan;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    @Column(name = "su_hotel_id", length = 50)
    private String suHotelId;

    @Column(name = "su_reservation_id", length = 100)
    private String suReservationId;

    @Column(name = "reservation_notif_id", length = 100)
    private String reservationNotifId;

    @Column(name = "room_reservation_id", length = 100)
    private String roomReservationId;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    /**
     * OTA/IT Provider 回传的 rooms[].id（我们推送给 Su 的 roomid），格式通常为 {roomTypeId}-{roomNumber}
     * 用于未排房时也能按房型统计占用/回传 PriceLabs booked_units。
     */
    @Column(name = "ota_room_id", length = 100)
    private String otaRoomId;

    /**
     * 由 otaRoomId 解析出的房型ID（roomTypeId）。
     */
    @Column(name = "ota_room_type_id")
    private Long otaRoomTypeId;

    /**
     * 由 otaRoomId 解析出的房间号（roomNumber）。
     */
    @Column(name = "ota_room_number", length = 50)
    private String otaRoomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateOrderNumber() {
        return "RSV" + System.currentTimeMillis();
    }

    // Constructors
    public Reservation() {}

    public Reservation(Room room, Channel channel, String guestName, 
                      LocalDate checkInDate, LocalDate checkOutDate, 
                      Integer adults, BigDecimal totalAmount) {
        this.room = room;
        this.channel = channel;
        this.guestName = guestName;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.adults = adults;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
        if (room != null) {
            this.storeId = room.getStoreId();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getGuestIdCard() {
        return guestIdCard;
    }

    public void setGuestIdCard(String guestIdCard) {
        this.guestIdCard = guestIdCard;
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

    public LocalDateTime getActualCheckIn() {
        return actualCheckIn;
    }

    public void setActualCheckIn(LocalDateTime actualCheckIn) {
        this.actualCheckIn = actualCheckIn;
    }

    public String getSuHotelId() {
        return suHotelId;
    }

    public void setSuHotelId(String suHotelId) {
        this.suHotelId = suHotelId;
    }

    public String getSuReservationId() {
        return suReservationId;
    }

    public void setSuReservationId(String suReservationId) {
        this.suReservationId = suReservationId;
    }

    public String getReservationNotifId() {
        return reservationNotifId;
    }

    public void setReservationNotifId(String reservationNotifId) {
        this.reservationNotifId = reservationNotifId;
    }

    public String getRoomReservationId() {
        return roomReservationId;
    }

    public void setRoomReservationId(String roomReservationId) {
        this.roomReservationId = roomReservationId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public LocalDateTime getActualCheckOut() {
        return actualCheckOut;
    }

    public void setActualCheckOut(LocalDateTime actualCheckOut) {
        this.actualCheckOut = actualCheckOut;
    }

    public Integer getAdults() {
        return adults;
    }

    public void setAdults(Integer adults) {
        this.adults = adults;
    }

    public Integer getChildren() {
        return children;
    }

    public void setChildren(Integer children) {
        this.children = children;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getGroupOrderNo() {
        return groupOrderNo;
    }

    public void setGroupOrderNo(String groupOrderNo) {
        this.groupOrderNo = groupOrderNo;
    }

    public String getChannelOrderNumber() {
        return channelOrderNumber;
    }

    public void setChannelOrderNumber(String channelOrderNumber) {
        this.channelOrderNumber = channelOrderNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public BigDecimal getOtherFees() {
        return otherFees;
    }

    public void setOtherFees(BigDecimal otherFees) {
        this.otherFees = otherFees;
    }

    public String getPricePlan() {
        return pricePlan;
    }

    public void setPricePlan(String pricePlan) {
        this.pricePlan = pricePlan;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getOtaRoomId() {
        return otaRoomId;
    }

    public void setOtaRoomId(String otaRoomId) {
        this.otaRoomId = otaRoomId;
    }

    public Long getOtaRoomTypeId() {
        return otaRoomTypeId;
    }

    public void setOtaRoomTypeId(Long otaRoomTypeId) {
        this.otaRoomTypeId = otaRoomTypeId;
    }

    public String getOtaRoomNumber() {
        return otaRoomNumber;
    }

    public void setOtaRoomNumber(String otaRoomNumber) {
        this.otaRoomNumber = otaRoomNumber;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
