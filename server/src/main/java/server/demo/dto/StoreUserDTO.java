package server.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店成员DTO
 */
public class StoreUserDTO {
    private Long id;
    private UserSimpleDTO user;
    private String role; // 基础角色: owner, admin, member
    private List<RoleDTO> roles; // 权限角色列表
    private List<PermissionDTO> extraPermissions; // 成员额外权限（叠加）
    private Boolean isActive;
    private Long invitedBy;
    private LocalDateTime joinedAt;

    public StoreUserDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserSimpleDTO getUser() {
        return user;
    }

    public void setUser(UserSimpleDTO user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<RoleDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleDTO> roles) {
        this.roles = roles;
    }

    public List<PermissionDTO> getExtraPermissions() {
        return extraPermissions;
    }

    public void setExtraPermissions(List<PermissionDTO> extraPermissions) {
        this.extraPermissions = extraPermissions;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Long invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    /**
     * 用户简化信息DTO（内部类）
     */
    public static class UserSimpleDTO {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        private Boolean isActive;

        public UserSimpleDTO() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
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

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }
}

