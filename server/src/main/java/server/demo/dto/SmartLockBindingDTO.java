package server.demo.dto;

import server.demo.enums.SmartLockBindingStatus;
import server.demo.enums.SmartLockProvider;

import java.time.LocalDateTime;

public class SmartLockBindingDTO {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private Long integrationId;
    private Long deviceId;
    private Long controlDeviceId;
    private String controlProviderLockId;
    private String controlLockName;
    private String controlDeviceType;
    private Long passcodeDeviceId;
    private String passcodeProviderLockId;
    private String passcodeLockName;
    private String passcodeDeviceType;
    private SmartLockProvider provider;
    private String providerLockId;
    private String lockName;
    private SmartLockBindingStatus status;
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

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Long getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(Long integrationId) {
        this.integrationId = integrationId;
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

    public String getControlDeviceType() {
        return controlDeviceType;
    }

    public void setControlDeviceType(String controlDeviceType) {
        this.controlDeviceType = controlDeviceType;
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

    public String getPasscodeDeviceType() {
        return passcodeDeviceType;
    }

    public void setPasscodeDeviceType(String passcodeDeviceType) {
        this.passcodeDeviceType = passcodeDeviceType;
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

    public SmartLockBindingStatus getStatus() {
        return status;
    }

    public void setStatus(SmartLockBindingStatus status) {
        this.status = status;
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
