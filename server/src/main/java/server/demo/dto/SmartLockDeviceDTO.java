package server.demo.dto;

import server.demo.enums.SmartLockProvider;

import java.time.LocalDateTime;

public class SmartLockDeviceDTO {
    private Long id;
    private Long integrationId;
    private SmartLockProvider provider;
    private String providerLockId;
    private String lockName;
    private String deviceType;
    private String auxiliaryDeviceId;
    private String statusSource;
    private String statusSourceDeviceId;
    private Boolean supportsControl;
    private Boolean supportsPasscode;
    private String linkedControlProviderLockId;
    private Integer battery;
    private String lockStatus;
    private Boolean online;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastStatusAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(Long integrationId) {
        this.integrationId = integrationId;
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

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getAuxiliaryDeviceId() {
        return auxiliaryDeviceId;
    }

    public void setAuxiliaryDeviceId(String auxiliaryDeviceId) {
        this.auxiliaryDeviceId = auxiliaryDeviceId;
    }

    public String getStatusSource() {
        return statusSource;
    }

    public void setStatusSource(String statusSource) {
        this.statusSource = statusSource;
    }

    public String getStatusSourceDeviceId() {
        return statusSourceDeviceId;
    }

    public void setStatusSourceDeviceId(String statusSourceDeviceId) {
        this.statusSourceDeviceId = statusSourceDeviceId;
    }

    public Boolean getSupportsControl() {
        return supportsControl;
    }

    public void setSupportsControl(Boolean supportsControl) {
        this.supportsControl = supportsControl;
    }

    public Boolean getSupportsPasscode() {
        return supportsPasscode;
    }

    public void setSupportsPasscode(Boolean supportsPasscode) {
        this.supportsPasscode = supportsPasscode;
    }

    public String getLinkedControlProviderLockId() {
        return linkedControlProviderLockId;
    }

    public void setLinkedControlProviderLockId(String linkedControlProviderLockId) {
        this.linkedControlProviderLockId = linkedControlProviderLockId;
    }

    public Integer getBattery() {
        return battery;
    }

    public void setBattery(Integer battery) {
        this.battery = battery;
    }

    public String getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public LocalDateTime getLastStatusAt() {
        return lastStatusAt;
    }

    public void setLastStatusAt(LocalDateTime lastStatusAt) {
        this.lastStatusAt = lastStatusAt;
    }
}
