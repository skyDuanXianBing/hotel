package server.demo.dto;

import server.demo.enums.SmartLockProvider;

import java.time.LocalDateTime;

public class SmartLockStatusDTO {
    private Long roomId;
    private Long bindingId;
    private Long deviceId;
    private Long controlDeviceId;
    private String controlProviderLockId;
    private String controlLockName;
    private Boolean controlAvailable;
    private String controlUnavailableReason;
    private Long passcodeDeviceId;
    private String passcodeProviderLockId;
    private String passcodeLockName;
    private Boolean passcodeAvailable;
    private String passcodeUnavailableReason;
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

    public Long getControlDeviceId() {
        return controlDeviceId;
    }

    public void setControlDeviceId(Long controlDeviceId) {
        this.controlDeviceId = controlDeviceId;
    }

    public String getControlProviderLockId() {
        return controlProviderLockId;
    }

    public void setControlProviderLockId(String controlProviderLockId) {
        this.controlProviderLockId = controlProviderLockId;
    }

    public String getControlLockName() {
        return controlLockName;
    }

    public void setControlLockName(String controlLockName) {
        this.controlLockName = controlLockName;
    }

    public Boolean getControlAvailable() {
        return controlAvailable;
    }

    public void setControlAvailable(Boolean controlAvailable) {
        this.controlAvailable = controlAvailable;
    }

    public String getControlUnavailableReason() {
        return controlUnavailableReason;
    }

    public void setControlUnavailableReason(String controlUnavailableReason) {
        this.controlUnavailableReason = controlUnavailableReason;
    }

    public Long getPasscodeDeviceId() {
        return passcodeDeviceId;
    }

    public void setPasscodeDeviceId(Long passcodeDeviceId) {
        this.passcodeDeviceId = passcodeDeviceId;
    }

    public String getPasscodeProviderLockId() {
        return passcodeProviderLockId;
    }

    public void setPasscodeProviderLockId(String passcodeProviderLockId) {
        this.passcodeProviderLockId = passcodeProviderLockId;
    }

    public String getPasscodeLockName() {
        return passcodeLockName;
    }

    public void setPasscodeLockName(String passcodeLockName) {
        this.passcodeLockName = passcodeLockName;
    }

    public Boolean getPasscodeAvailable() {
        return passcodeAvailable;
    }

    public void setPasscodeAvailable(Boolean passcodeAvailable) {
        this.passcodeAvailable = passcodeAvailable;
    }

    public String getPasscodeUnavailableReason() {
        return passcodeUnavailableReason;
    }

    public void setPasscodeUnavailableReason(String passcodeUnavailableReason) {
        this.passcodeUnavailableReason = passcodeUnavailableReason;
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
