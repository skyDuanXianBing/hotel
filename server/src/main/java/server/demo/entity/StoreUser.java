package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 门店-用户关联表
 * 实现多用户协作管理同一门店
 */
@Entity
@Table(name = "store_users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "user_id"})
})
public class StoreUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 用户在该门店的基础角色: owner(所有者), admin(管理员), member(普通成员)
     */
    @Column(nullable = false, length = 20)
    private String role = "member";

    /**
     * 用户在该门店的权限角色（细粒度权限控制）
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "store_user_roles",
        joinColumns = @JoinColumn(name = "store_user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @JsonIgnoreProperties({"users", "rolePermissions"})
    private Set<Role> roles = new HashSet<>();

    /**
     * 是否激活
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * 邀请人ID
     */
    @Column(name = "invited_by")
    private Long invitedBy;

    /**
     * 加入时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    // Constructors
    public StoreUser() {}

    public StoreUser(Store store, User user, String role) {
        this.store = store;
        this.user = user;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
