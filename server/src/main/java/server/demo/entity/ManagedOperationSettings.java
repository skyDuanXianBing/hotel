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
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "managed_operation_settings")
public class ManagedOperationSettings implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false, unique = true)
    private Long storeId;

    @Column(name = "property_name", nullable = false, length = 200)
    private String propertyName = "";

    @Column(name = "management_fee_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal managementFeeRate = new BigDecimal("0.10");

    @Column(name = "tax_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal taxRate = new BigDecimal("0.10");

    @Column(name = "cleaning_fee_gross", nullable = false, precision = 14, scale = 2)
    private BigDecimal cleaningFeeGross = new BigDecimal("8000");

    @Column(name = "registration_fee_net", nullable = false, precision = 14, scale = 2)
    private BigDecimal registrationFeeNet = new BigDecimal("2000");

    @Column(name = "owner_company_name", nullable = false, length = 200)
    private String ownerCompanyName = "";
    @Column(name = "owner_contact_name", nullable = false, length = 100)
    private String ownerContactName = "";
    @Column(name = "owner_postal_code", nullable = false, length = 30)
    private String ownerPostalCode = "";
    @Column(name = "owner_address", nullable = false, length = 500)
    private String ownerAddress = "";
    @Column(name = "issuer_company_name", nullable = false, length = 200)
    private String issuerCompanyName = "";
    @Column(name = "issuer_postal_code", nullable = false, length = 30)
    private String issuerPostalCode = "";
    @Column(name = "issuer_address", nullable = false, length = 500)
    private String issuerAddress = "";
    @Column(name = "issuer_registration_number", nullable = false, length = 100)
    private String issuerRegistrationNumber = "";
    @Column(name = "issuer_phone", nullable = false, length = 50)
    private String issuerPhone = "";
    @Column(name = "issuer_email", nullable = false, length = 200)
    private String issuerEmail = "";
    @Column(name = "bank_name", nullable = false, length = 200)
    private String bankName = "";
    @Column(name = "bank_branch", nullable = false, length = 200)
    private String bankBranch = "";
    @Column(name = "bank_account_type", nullable = false, length = 50)
    private String bankAccountType = "";
    @Column(name = "bank_account_number", nullable = false, length = 100)
    private String bankAccountNumber = "";
    @Column(name = "bank_account_holder", nullable = false, length = 200)
    private String bankAccountHolder = "";
    @Column(name = "stamp_storage_key", length = 500)
    private String stampStorageKey;

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
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public BigDecimal getManagementFeeRate() { return managementFeeRate; }
    public void setManagementFeeRate(BigDecimal managementFeeRate) { this.managementFeeRate = managementFeeRate; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getCleaningFeeGross() { return cleaningFeeGross; }
    public void setCleaningFeeGross(BigDecimal cleaningFeeGross) { this.cleaningFeeGross = cleaningFeeGross; }
    public BigDecimal getRegistrationFeeNet() { return registrationFeeNet; }
    public void setRegistrationFeeNet(BigDecimal registrationFeeNet) { this.registrationFeeNet = registrationFeeNet; }
    public String getOwnerCompanyName() { return ownerCompanyName; }
    public void setOwnerCompanyName(String value) { ownerCompanyName = value; }
    public String getOwnerContactName() { return ownerContactName; }
    public void setOwnerContactName(String value) { ownerContactName = value; }
    public String getOwnerPostalCode() { return ownerPostalCode; }
    public void setOwnerPostalCode(String value) { ownerPostalCode = value; }
    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String value) { ownerAddress = value; }
    public String getIssuerCompanyName() { return issuerCompanyName; }
    public void setIssuerCompanyName(String value) { issuerCompanyName = value; }
    public String getIssuerPostalCode() { return issuerPostalCode; }
    public void setIssuerPostalCode(String value) { issuerPostalCode = value; }
    public String getIssuerAddress() { return issuerAddress; }
    public void setIssuerAddress(String value) { issuerAddress = value; }
    public String getIssuerRegistrationNumber() { return issuerRegistrationNumber; }
    public void setIssuerRegistrationNumber(String value) { issuerRegistrationNumber = value; }
    public String getIssuerPhone() { return issuerPhone; }
    public void setIssuerPhone(String value) { issuerPhone = value; }
    public String getIssuerEmail() { return issuerEmail; }
    public void setIssuerEmail(String value) { issuerEmail = value; }
    public String getBankName() { return bankName; }
    public void setBankName(String value) { bankName = value; }
    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String value) { bankBranch = value; }
    public String getBankAccountType() { return bankAccountType; }
    public void setBankAccountType(String value) { bankAccountType = value; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String value) { bankAccountNumber = value; }
    public String getBankAccountHolder() { return bankAccountHolder; }
    public void setBankAccountHolder(String value) { bankAccountHolder = value; }
    public String getStampStorageKey() { return stampStorageKey; }
    public void setStampStorageKey(String stampStorageKey) { this.stampStorageKey = stampStorageKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
