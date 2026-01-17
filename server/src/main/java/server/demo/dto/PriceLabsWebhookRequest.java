package server.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * PriceLabs Webhook 请求 DTO
 * 用于接收 PriceLabs 推送的价格更新
 */
public class PriceLabsWebhookRequest {

    /**
     * 事件类型 (price_update, calendar_request, error)
     */
    private String type;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 数据列表
     */
    private List<ListingData> data;

    // Nested class for listing data
    public static class ListingData {
        @JsonProperty("listingId")
        @JsonAlias({"listing_id", "listingId"})
        private String listingId;

        @JsonProperty("ratePlanId")
        @JsonAlias({"rate_plan_id", "ratePlanId"})
        private String ratePlanId;
        private List<CalendarData> calendar;

        public String getListingId() {
            return listingId;
        }

        public void setListingId(String listingId) {
            this.listingId = listingId;
        }

        public String getRatePlanId() {
            return ratePlanId;
        }

        public void setRatePlanId(String ratePlanId) {
            this.ratePlanId = ratePlanId;
        }

        public List<CalendarData> getCalendar() {
            return calendar;
        }

        public void setCalendar(List<CalendarData> calendar) {
            this.calendar = calendar;
        }
    }

    public static class CalendarData {
        private String date;
        private BigDecimal price;

        @JsonProperty("minStay")
        @JsonAlias({"min_stay", "minStay"})
        private Integer minStay;

        @JsonProperty("maxStay")
        @JsonAlias({"max_stay", "maxStay"})
        private Integer maxStay;

        @JsonProperty("closedToArrival")
        @JsonAlias({"closed_to_arrival", "closedToArrival"})
        private Boolean closedToArrival;

        @JsonProperty("closedToDeparture")
        @JsonAlias({"closed_to_departure", "closedToDeparture"})
        private Boolean closedToDeparture;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getMinStay() {
            return minStay;
        }

        public void setMinStay(Integer minStay) {
            this.minStay = minStay;
        }

        public Integer getMaxStay() {
            return maxStay;
        }

        public void setMaxStay(Integer maxStay) {
            this.maxStay = maxStay;
        }

        public Boolean getClosedToArrival() {
            return closedToArrival;
        }

        public void setClosedToArrival(Boolean closedToArrival) {
            this.closedToArrival = closedToArrival;
        }

        public Boolean getClosedToDeparture() {
            return closedToDeparture;
        }

        public void setClosedToDeparture(Boolean closedToDeparture) {
            this.closedToDeparture = closedToDeparture;
        }
    }

    // Getters and Setters for main class
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<ListingData> getData() {
        return data;
    }

    public void setData(List<ListingData> data) {
        this.data = data;
    }
}
