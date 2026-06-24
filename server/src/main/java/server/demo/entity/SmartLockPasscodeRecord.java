package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import server.demo.enums.SmartLockPasscodeStatus;
import server.demo.enums.SmartLockProvider;

import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "smart_lock_passcode_records")
public class SmartLockPasscodeRecord implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binding_id", nullable = false)
    private SmartLockRoomBinding binding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_id", nullable = false)
    private SmartLockIntegration integration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SmartLockProvider provider;

    @Column(name = "provider_lock_id", nullable = false, length = 120)
    private String providerLockId;

    @Column(name = "passcode_role", nullable = false, length = 30)
    private String passcodeRole = "PASSCODE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passcode_device_id")
    private SmartLockDevice passcodeDevice;

    @Column(name = "passcode_provider_lock_id", length = 120)
    private String passcodeProviderLockId;

    @Column(name = "provider_passcode_id", length = 120)
    private String providerPasscodeId;

    @Column(name = "provider_task_id", length = 120)
    private String providerTaskId;

    @Column(name = "passcode_name", nullable = false, length = 120)
    private String passcodeName;

    @Column(name = "passcode_masked", nullable = false, length = 40)
    private String passcodeMasked;

    @Column(name = "passcode_hash", nullable = false, length = 128)
    private String passcodeHash;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SmartLockPasscodeStatus status = SmartLockPasscodeStatus.PENDING;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public SmartLockRoomBinding getBinding() {
        return binding;
    }

    public void setBinding(SmartLockRoomBinding binding) {
        this.binding = binding;
    }

    public SmartLockIntegration getIntegration() {
        return integration;
    }

    public void setIntegration(SmartLockIntegration integration) {
        this.integration = integration;
    }

    public SmartLockProvider getProvider() {
        return provider;
    }

    public void setProvider(SmartLockProvider provider) {
        this.provider = provider;
    }

    public String getProviderLockId() {
        return providerLockId;
    }

    public void setProviderLockId(String providerLockId) {
        this.providerLockId = providerLockId;
    }

    public String getPasscodeRole() {
        return passcodeRole;
    }

    public void setPasscodeRole(String passcodeRole) {
        this.passcodeRole = passcodeRole;
    }

    public SmartLockDevice getPasscodeDevice() {
        return passcodeDevice;
    }

    public void setPasscodeDevice(SmartLockDevice passcodeDevice) {
        this.passcodeDevice = passcodeDevice;
    }

    public String getPasscodeProviderLockId() {
        return passcodeProviderLockId;
    }

    public void setPasscodeProviderLockId(String passcodeProviderLockId) {
        this.passcodeProviderLockId = passcodeProviderLockId;
    }

    public String getProviderPasscodeId() {
        return providerPasscodeId;
    }

    public void setProviderPasscodeId(String providerPasscodeId) {
        this.providerPasscodeId = providerPasscodeId;
    }

    public String getProviderTaskId() {
        return providerTaskId;
    }

    public void setProviderTaskId(String providerTaskId) {
        this.providerTaskId = providerTaskId;
    }

    public String getPasscodeName() {
        return passcodeName;
    }

    public void setPasscodeName(String passcodeName) {
        this.passcodeName = passcodeName;
    }

    public String getPasscodeMasked() {
        return passcodeMasked;
    }

    public void setPasscodeMasked(String passcodeMasked) {
        this.passcodeMasked = passcodeMasked;
    }

    public String getPasscodeHash() {
        return passcodeHash;
    }

    public void setPasscodeHash(String passcodeHash) {
        this.passcodeHash = passcodeHash;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public SmartLockPasscodeStatus getStatus() {
        return status;
    }

    public void setStatus(SmartLockPasscodeStatus status) {
        this.status = status;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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
