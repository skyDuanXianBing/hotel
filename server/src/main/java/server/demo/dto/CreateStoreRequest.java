package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * 创建/更新门店请求。
 */
public class CreateStoreRequest {

    @NotBlank(message = "门店名称不能为空")
    private String name;

    private String phone;
    private String phoneTechType;

    @NotBlank(message = "门店类型不能为空")
    private String type;

    private String timezone;

    @NotBlank(message = "负责人不能为空")
    private String manager;

    @NotBlank(message = "国家不能为空")
    private String country;

    private String city;
    private String state;
    private String address;
    private String currency;
    private String suHotelId;
    private Boolean createSuProperty;
    private String logo;
    private String description;
    private String email;
    private String wechat;
    private String whatsapp;
    private String line;
    private String language;
    private String checkinTime;
    private String checkoutTime;
    private List<FacilityDTO> facilities;
    private List<String> desktopPhotoUrls;
    private List<String> mobilePhotoUrls;
    private Map<String, LocalizedContentDTO> localizedContent;

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

    public String getPhoneTechType() {
        return phoneTechType;
    }

    public void setPhoneTechType(String phoneTechType) {
        this.phoneTechType = phoneTechType;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Boolean getCreateSuProperty() {
        return createSuProperty;
    }

    public void setCreateSuProperty(Boolean createSuProperty) {
        this.createSuProperty = createSuProperty;
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

    public String getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(String checkinTime) {
        this.checkinTime = checkinTime;
    }

    public String getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(String checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public List<FacilityDTO> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<FacilityDTO> facilities) {
        this.facilities = facilities;
    }

    public List<String> getDesktopPhotoUrls() {
        return desktopPhotoUrls;
    }

    public void setDesktopPhotoUrls(List<String> desktopPhotoUrls) {
        this.desktopPhotoUrls = desktopPhotoUrls;
    }

    public List<String> getMobilePhotoUrls() {
        return mobilePhotoUrls;
    }

    public void setMobilePhotoUrls(List<String> mobilePhotoUrls) {
        this.mobilePhotoUrls = mobilePhotoUrls;
    }

    public Map<String, LocalizedContentDTO> getLocalizedContent() {
        return localizedContent;
    }

    public void setLocalizedContent(Map<String, LocalizedContentDTO> localizedContent) {
        this.localizedContent = localizedContent;
    }
}
