package server.demo.dto.auth;

import server.demo.entity.User;

import java.time.LocalDateTime;

/**
 * 用户DTO
 */
public class UserDTO {

    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private String gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isCleaner; // 是否是保洁员

    public UserDTO() {
    }

    public UserDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.avatar = user.getAvatar();
        this.gender = user.getGender();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.isCleaner = false; // 默认不是保洁员
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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

    public Boolean getIsCleaner() {
        return isCleaner;
    }

    public void setIsCleaner(Boolean isCleaner) {
        this.isCleaner = isCleaner;
    }
}
