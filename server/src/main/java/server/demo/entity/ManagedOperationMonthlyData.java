package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "managed_operation_monthly_data")
public class ManagedOperationMonthlyData implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "settings_id", nullable = false)
    private ManagedOperationSettings settings;

    @Column(name = "settlement_month", nullable = false, length = 7)
    private String settlementMonth;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber = "";
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;
    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;
    @Column(name = "receipt_number", nullable = false, length = 100)
    private String receiptNumber = "";
    @Column(name = "receipt_date")
    private LocalDate receiptDate;
    @Column(name = "note", nullable = false, length = 1000)
    private String note = "";

    @Column(name = "airbnb_file_key", length = 500)
    private String airbnbFileKey;
    @Column(name = "airbnb_file_name", nullable = false, length = 255)
    private String airbnbFileName = "";
    @Column(name = "booking_file_key", length = 500)
    private String bookingFileKey;
    @Column(name = "booking_file_name", nullable = false, length = 255)
    private String bookingFileName = "";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @Override public Long getStoreId() { return storeId; }
    @Override public void setStoreId(Long storeId) { this.storeId = storeId; }
    public ManagedOperationSettings getSettings() { return settings; }
    public void setSettings(ManagedOperationSettings settings) { this.settings = settings; }
    public String getSettlementMonth() { return settlementMonth; }
    public void setSettlementMonth(String settlementMonth) { this.settlementMonth = settlementMonth; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getAirbnbFileKey() { return airbnbFileKey; }
    public void setAirbnbFileKey(String airbnbFileKey) { this.airbnbFileKey = airbnbFileKey; }
    public String getAirbnbFileName() { return airbnbFileName; }
    public void setAirbnbFileName(String airbnbFileName) { this.airbnbFileName = airbnbFileName; }
    public String getBookingFileKey() { return bookingFileKey; }
    public void setBookingFileKey(String bookingFileKey) { this.bookingFileKey = bookingFileKey; }
    public String getBookingFileName() { return bookingFileName; }
    public void setBookingFileName(String bookingFileName) { this.bookingFileName = bookingFileName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
