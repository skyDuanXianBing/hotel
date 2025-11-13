package server.demo.dto.auth;

import server.demo.entity.Cleaner;

import java.time.LocalDateTime;

/**
 * 保洁员数据传输对象
 */
public class CleanerDTO {

    private Long id;

    private Long storeId;

    private String name;

    private String email;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public CleanerDTO() {}

    public CleanerDTO(Cleaner cleaner) {
        this.id = cleaner.getId();
        this.storeId = cleaner.getStoreId();
        this.name = cleaner.getName();
        this.email = cleaner.getEmail();
        this.isActive = cleaner.getIsActive();
        this.createdAt = cleaner.getCreatedAt();
        this.updatedAt = cleaner.getUpdatedAt();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
