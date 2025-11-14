package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import server.demo.entity.base.StoreScopedEntity;

@Entity
@Table(name = "price_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "name"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PricePlan implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "价格计划名称不能为空")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 500)
    private String description;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @NotNull(message = "最少入住天数不能为空")
    @Min(value = 1, message = "最少入住天数必须大于0")
    @Column(name = "min_nights", nullable = false)
    private Integer minNights = 1;

    @Column(name = "max_nights")
    private Integer maxNights = 365;

    @Column(name = "include_meal", nullable = false)
    private Boolean includeMeal = false;

    @Column(name = "derivation_type", length = 20)
    private String derivationType; // independent 或 derived

    @Column(name = "base_plan_id")
    private Long basePlanId; // 如果是衍生类型,指向基础计划ID

    @Column(name = "derivation_rule", length = 500)
    private String derivationRule; // 衍生规则描述

    @Column(name = "cancellation_policy", length = 500)
    private String cancellationPolicy;

    @Column(name = "cancellation_policy_en", length = 500)
    private String cancellationPolicyEn;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (minNights == null) {
            minNights = 1;
        }
        if (maxNights == null) {
            maxNights = 365;
        }
        if (includeMeal == null) {
            includeMeal = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public PricePlan() {}

    public PricePlan(String name, Integer minNights, Integer maxNights, Boolean includeMeal) {
        this.name = name;
        this.minNights = minNights;
        this.maxNights = maxNights;
        this.includeMeal = includeMeal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }

    public Integer getMinNights() {
        return minNights;
    }

    public void setMinNights(Integer minNights) {
        this.minNights = minNights;
    }

    public Integer getMaxNights() {
        return maxNights;
    }

    public void setMaxNights(Integer maxNights) {
        this.maxNights = maxNights;
    }

    public Boolean getIncludeMeal() {
        return includeMeal;
    }

    public void setIncludeMeal(Boolean includeMeal) {
        this.includeMeal = includeMeal;
    }

    public String getDerivationType() {
        return derivationType;
    }

    public void setDerivationType(String derivationType) {
        this.derivationType = derivationType;
    }

    public Long getBasePlanId() {
        return basePlanId;
    }

    public void setBasePlanId(Long basePlanId) {
        this.basePlanId = basePlanId;
    }

    public String getDerivationRule() {
        return derivationRule;
    }

    public void setDerivationRule(String derivationRule) {
        this.derivationRule = derivationRule;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public String getCancellationPolicyEn() {
        return cancellationPolicyEn;
    }

    public void setCancellationPolicyEn(String cancellationPolicyEn) {
        this.cancellationPolicyEn = cancellationPolicyEn;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
