package server.demo.dto;

import server.demo.enums.SmartLockPasscodeStatus;
import server.demo.enums.SmartLockProvider;

import java.time.LocalDateTime;

public class SmartLockPasscodeDTO {
    private Long id;
    private Long roomId;
    private Long bindingId;
    private SmartLockProvider provider;
    private String providerLockId;
    private String providerPasscodeId;
    private String providerTaskId;
    private String passcodeName;
    private String passcodeMasked;
    private String oneTimePasscode;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private SmartLockPasscodeStatus status;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getBindingId() {
        return bindingId;
    }

    public void setBindingId(Long bindingId) {
        this.bindingId = bindingId;
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

    public String getOneTimePasscode() {
        return oneTimePasscode;
    }

    public void setOneTimePasscode(String oneTimePasscode) {
        this.oneTimePasscode = oneTimePasscode;
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
