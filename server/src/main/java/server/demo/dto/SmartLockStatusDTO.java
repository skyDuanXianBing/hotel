package server.demo.dto;

import server.demo.enums.SmartLockProvider;

import java.time.LocalDateTime;

public class SmartLockStatusDTO {
    private Long roomId;
    private Long bindingId;
    private Long deviceId;
    private SmartLockProvider provider;
    private String providerLockId;
    private String lockName;
    private String lockStatus;
    private Integer battery;
    private Boolean online;
    private LocalDateTime lastStatusAt;

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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
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

    public String getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }

    public Integer getBattery() {
        return battery;
    }

    public void setBattery(Integer battery) {
        this.battery = battery;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public LocalDateTime getLastStatusAt() {
        return lastStatusAt;
    }

    public void setLastStatusAt(LocalDateTime lastStatusAt) {
        this.lastStatusAt = lastStatusAt;
    }
}
