package server.demo.dto;

import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskType;

import java.time.LocalDateTime;

public class SmartLockRequests {
    private SmartLockRequests() {
    }

    public static class UpsertIntegrationRequest {
        private SmartLockProvider provider;
        private String name;
        private Boolean enabled;
        private String switchBotToken;
        private String switchBotSecret;
        private String ttLockClientId;
        private String ttLockClientSecret;
        private String ttLockUsername;
        private String ttLockPassword;
        private String ttLockPasswordMd5;

        public SmartLockProvider getProvider() {
            return provider;
        }

        public void setProvider(SmartLockProvider provider) {
            this.provider = provider;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getSwitchBotToken() {
            return switchBotToken;
        }

        public void setSwitchBotToken(String switchBotToken) {
            this.switchBotToken = switchBotToken;
        }

        public String getSwitchBotSecret() {
            return switchBotSecret;
        }

        public void setSwitchBotSecret(String switchBotSecret) {
            this.switchBotSecret = switchBotSecret;
        }

        public String getTtLockClientId() {
            return ttLockClientId;
        }

        public void setTtLockClientId(String ttLockClientId) {
            this.ttLockClientId = ttLockClientId;
        }

        public String getTtLockClientSecret() {
            return ttLockClientSecret;
        }

        public void setTtLockClientSecret(String ttLockClientSecret) {
            this.ttLockClientSecret = ttLockClientSecret;
        }

        public String getTtLockUsername() {
            return ttLockUsername;
        }

        public void setTtLockUsername(String ttLockUsername) {
            this.ttLockUsername = ttLockUsername;
        }

        public String getTtLockPassword() {
            return ttLockPassword;
        }

        public void setTtLockPassword(String ttLockPassword) {
            this.ttLockPassword = ttLockPassword;
        }

        public String getTtLockPasswordMd5() {
            return ttLockPasswordMd5;
        }

        public void setTtLockPasswordMd5(String ttLockPasswordMd5) {
            this.ttLockPasswordMd5 = ttLockPasswordMd5;
        }
    }

    public static class CreateBindingRequest {
        private Long roomId;
        private Long integrationId;
        private Long deviceId;
        private SmartLockProvider provider;
        private String providerLockId;

        public Long getRoomId() {
            return roomId;
        }

        public void setRoomId(Long roomId) {
            this.roomId = roomId;
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
    }

    public static class ConfirmationRequest {
        private SmartLockTaskType action;
        private Long bindingId;
        private String reason;

        public SmartLockTaskType getAction() {
            return action;
        }

        public void setAction(SmartLockTaskType action) {
            this.action = action;
        }

        public Long getBindingId() {
            return bindingId;
        }

        public void setBindingId(Long bindingId) {
            this.bindingId = bindingId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class LockOperationRequest {
        private Boolean confirm;
        private String confirmToken;
        private Long bindingId;
        private String reason;
        private String idempotencyKey;

        public Boolean getConfirm() {
            return confirm;
        }

        public void setConfirm(Boolean confirm) {
            this.confirm = confirm;
        }

        public String getConfirmToken() {
            return confirmToken;
        }

        public void setConfirmToken(String confirmToken) {
            this.confirmToken = confirmToken;
        }

        public Long getBindingId() {
            return bindingId;
        }

        public void setBindingId(Long bindingId) {
            this.bindingId = bindingId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }
    }

    public static class CreatePasscodeRequest {
        private String passcodeName;
        private String passcode;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private String idempotencyKey;

        public String getPasscodeName() {
            return passcodeName;
        }

        public void setPasscodeName(String passcodeName) {
            this.passcodeName = passcodeName;
        }

        public String getPasscode() {
            return passcode;
        }

        public void setPasscode(String passcode) {
            this.passcode = passcode;
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

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }
    }
}
