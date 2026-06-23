package server.demo.dto;

import server.demo.enums.SmartLockTaskType;

import java.time.LocalDateTime;

public class SmartLockConfirmationDTO {
    private Long roomId;
    private Long bindingId;
    private SmartLockTaskType action;
    private String confirmToken;
    private LocalDateTime expiresAt;

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

    public SmartLockTaskType getAction() {
        return action;
    }

    public void setAction(SmartLockTaskType action) {
        this.action = action;
    }

    public String getConfirmToken() {
        return confirmToken;
    }

    public void setConfirmToken(String confirmToken) {
        this.confirmToken = confirmToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
