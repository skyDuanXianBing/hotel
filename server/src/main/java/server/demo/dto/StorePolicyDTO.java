package server.demo.dto;

/**
 * 门店政策DTO
 */
public class StorePolicyDTO {
    private Long id;
    private Long storeId;
    private String checkinTime;
    private String checkoutTime;
    private String childPolicy;
    private String smokingPolicy;
    private String petPolicy;
    private String additionalRules;
    private String hotelTerms;

    public StorePolicyDTO() {
    }

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

    public String getChildPolicy() {
        return childPolicy;
    }

    public void setChildPolicy(String childPolicy) {
        this.childPolicy = childPolicy;
    }

    public String getSmokingPolicy() {
        return smokingPolicy;
    }

    public void setSmokingPolicy(String smokingPolicy) {
        this.smokingPolicy = smokingPolicy;
    }

    public String getPetPolicy() {
        return petPolicy;
    }

    public void setPetPolicy(String petPolicy) {
        this.petPolicy = petPolicy;
    }

    public String getAdditionalRules() {
        return additionalRules;
    }

    public void setAdditionalRules(String additionalRules) {
        this.additionalRules = additionalRules;
    }

    public String getHotelTerms() {
        return hotelTerms;
    }

    public void setHotelTerms(String hotelTerms) {
        this.hotelTerms = hotelTerms;
    }
}
