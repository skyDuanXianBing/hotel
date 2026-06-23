package server.demo.dto;

import server.demo.enums.SmartLockIntegrationStatus;
import server.demo.enums.SmartLockProvider;

import java.time.LocalDateTime;
import java.util.Map;

public class SmartLockIntegrationDTO {
    private Long id;
    private SmartLockProvider provider;
    private String name;
    private Boolean enabled;
    private SmartLockIntegrationStatus connectionStatus;
    private Boolean credentialsConfigured;
    private Map<String, String> maskedCredentials;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime lastTestAt;
    private LocalDateTime lastSyncAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public SmartLockIntegrationStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(SmartLockIntegrationStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public Boolean getCredentialsConfigured() {
        return credentialsConfigured;
    }

    public void setCredentialsConfigured(Boolean credentialsConfigured) {
        this.credentialsConfigured = credentialsConfigured;
    }

    public Map<String, String> getMaskedCredentials() {
        return maskedCredentials;
    }

    public void setMaskedCredentials(Map<String, String> maskedCredentials) {
        this.maskedCredentials = maskedCredentials;
    }

    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public LocalDateTime getLastTestAt() {
        return lastTestAt;
    }

    public void setLastTestAt(LocalDateTime lastTestAt) {
        this.lastTestAt = lastTestAt;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
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
