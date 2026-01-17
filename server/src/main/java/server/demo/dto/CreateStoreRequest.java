package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建门店请求DTO
 */
public class CreateStoreRequest {

    @NotBlank(message = "门店名称不能为空")
    private String name;

    private String phone;

    @NotBlank(message = "门店类型不能为空")
    private String type; // 日式旅馆、酒店、民宿

    private String timezone;

    @NotBlank(message = "负责人不能为空")
    private String manager;

    @NotBlank(message = "国家和地址不能为空")
    private String country;

    private String city;

    private String state;

    private String address;

    /**
     * 门店货币（例如：CNY / JPY / USD）
     */
    private String currency;

    /**
     * Su Channel Manager 的 hotelid / HotelCode（可选，不填则后端随机生成不重复的编码）
     */
    private String suHotelId;

    /**
     * 创建门店后是否同步创建/覆盖 Su 物业（默认 true）
     */
    private Boolean createSuProperty;

    // Getters and Setters
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
}
