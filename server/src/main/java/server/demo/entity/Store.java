package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import server.demo.dto.FacilityDTO;
import server.demo.dto.LocalizedContentDTO;
import server.demo.util.JsonFieldUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 门店实体。
 */
@Entity
@Table(name = "stores")
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String type;

    @Column(length = 50)
    private String timezone;

    @Column(length = 100)
    private String manager;

    @Column(length = 100)
    private String ownerEmail;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 10)
    private String currency;

    @Column(length = 15)
    private String suHotelId;

    @Column(length = 255)
    private String logo;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String wechat;

    @Column(length = 100)
    private String whatsapp;

    @Column(length = 100)
    private String line;

    @Column(length = 20)
    private String language;

    @JsonIgnore
    @Column(name = "desktop_photo_urls_json", columnDefinition = "LONGTEXT")
    private String desktopPhotoUrlsJson;

    @JsonIgnore
    @Column(name = "mobile_photo_urls_json", columnDefinition = "LONGTEXT")
    private String mobilePhotoUrlsJson;

    @JsonIgnore
    @Column(name = "localized_content_json", columnDefinition = "LONGTEXT")
    private String localizedContentJson;

    @JsonIgnore
    @Column(name = "facilities_json", columnDefinition = "LONGTEXT")
    private String facilitiesJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSuHotelId() {
        return suHotelId;
    }

    public void setSuHotelId(String suHotelId) {
        this.suHotelId = suHotelId;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Transient
    public List<String> getDesktopPhotoUrls() {
        return JsonFieldUtils.readStringList(desktopPhotoUrlsJson);
    }

    public void setDesktopPhotoUrls(List<String> desktopPhotoUrls) {
        this.desktopPhotoUrlsJson = JsonFieldUtils.writeStringList(desktopPhotoUrls);
    }

    @Transient
    public List<String> getMobilePhotoUrls() {
        return JsonFieldUtils.readStringList(mobilePhotoUrlsJson);
    }

    public void setMobilePhotoUrls(List<String> mobilePhotoUrls) {
        this.mobilePhotoUrlsJson = JsonFieldUtils.writeStringList(mobilePhotoUrls);
    }

    @Transient
    public Map<String, LocalizedContentDTO> getLocalizedContent() {
        return JsonFieldUtils.readLocalizedContentMap(localizedContentJson);
    }

    public void setLocalizedContent(Map<String, LocalizedContentDTO> localizedContent) {
        this.localizedContentJson = JsonFieldUtils.writeLocalizedContentMap(localizedContent);
    }

    @Transient
    public List<FacilityDTO> getFacilities() {
        return JsonFieldUtils.readFacilityList(facilitiesJson);
    }

    public void setFacilities(List<FacilityDTO> facilities) {
        this.facilitiesJson = JsonFieldUtils.writeFacilityList(facilities);
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
