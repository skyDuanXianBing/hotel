package server.demo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * PriceLabs Webhook 请求 DTO
 * 用于接收 PriceLabs 推送的价格更新
 */
public class PriceLabsWebhookRequest {

    /**
     * 房源/listing ID
     */
    private String listingId;

    /**
     * 价格数据列表
     */
    private List<PriceData> prices;

    /**
     * 最小入住天数数据
     */
    private List<MinStayData> minStays;

    // Inner classes for data structures
    public static class PriceData {
        private String date;
        private BigDecimal price;
        private String currency;

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

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

    public static class MinStayData {
        private String date;
        private Integer minStay;
        private Integer maxStay;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
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
    }

    // Getters and Setters
    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public List<PriceData> getPrices() {
        return prices;
    }

    public void setPrices(List<PriceData> prices) {
        this.prices = prices;
    }

    public List<MinStayData> getMinStays() {
        return minStays;
    }

    public void setMinStays(List<MinStayData> minStays) {
        this.minStays = minStays;
    }
}
