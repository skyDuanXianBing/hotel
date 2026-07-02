package server.demo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 渠道汇总统计DTO
 */
public class ChannelSummaryDTO {
    // 总消费金额
    private BigDecimal totalRevenue;

    // 总间夜数
    private Integer totalRoomNights;

    // 收入口径元数据
    private RevenuePrecisionDTO revenuePrecision;

    // 渠道消费分布(饼图)
    private List<ChannelDistribution> revenueDistribution;

    // 渠道间夜分布(饼图)
    private List<ChannelDistribution> nightsDistribution;

    // 渠道消费趋势(折线图)
    private List<ChannelTrend> revenueTrend;

    // 渠道间夜趋势(柱状图)
    private List<ChannelTrend> nightsTrend;

    // 渠道明细表格数据
    private List<ChannelDetail> channelDetails;

    // Constructors
    public ChannelSummaryDTO() {}

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Integer getTotalRoomNights() {
        return totalRoomNights;
    }

    public void setTotalRoomNights(Integer totalRoomNights) {
        this.totalRoomNights = totalRoomNights;
    }

    public RevenuePrecisionDTO getRevenuePrecision() {
        return revenuePrecision;
    }

    public void setRevenuePrecision(RevenuePrecisionDTO revenuePrecision) {
        this.revenuePrecision = revenuePrecision;
    }

    public List<ChannelDistribution> getRevenueDistribution() {
        return revenueDistribution;
    }

    public void setRevenueDistribution(List<ChannelDistribution> revenueDistribution) {
        this.revenueDistribution = revenueDistribution;
    }

    public List<ChannelDistribution> getNightsDistribution() {
        return nightsDistribution;
    }

    public void setNightsDistribution(List<ChannelDistribution> nightsDistribution) {
        this.nightsDistribution = nightsDistribution;
    }

    public List<ChannelTrend> getRevenueTrend() {
        return revenueTrend;
    }

    public void setRevenueTrend(List<ChannelTrend> revenueTrend) {
        this.revenueTrend = revenueTrend;
    }

    public List<ChannelTrend> getNightsTrend() {
        return nightsTrend;
    }

    public void setNightsTrend(List<ChannelTrend> nightsTrend) {
        this.nightsTrend = nightsTrend;
    }

    public List<ChannelDetail> getChannelDetails() {
        return channelDetails;
    }

    public void setChannelDetails(List<ChannelDetail> channelDetails) {
        this.channelDetails = channelDetails;
    }

    /**
     * 渠道分布数据(饼图)
     */
    public static class ChannelDistribution {
        private String channelName; // 渠道名称
        private BigDecimal value; // 金额或间夜数
        private BigDecimal percentage; // 百分比

        public ChannelDistribution() {}

        public ChannelDistribution(String channelName, BigDecimal value, BigDecimal percentage) {
            this.channelName = channelName;
            this.value = value;
            this.percentage = percentage;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
    }

    /**
     * 渠道趋势数据(折线图/柱状图)
     */
    public static class ChannelTrend {
        private String date; // 日期
        private List<ChannelValue> channels; // 各渠道的值

        public ChannelTrend() {}

        public ChannelTrend(String date, List<ChannelValue> channels) {
            this.date = date;
            this.channels = channels;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public List<ChannelValue> getChannels() {
            return channels;
        }

        public void setChannels(List<ChannelValue> channels) {
            this.channels = channels;
        }

        /**
         * 渠道值
         */
        public static class ChannelValue {
            private String channelName; // 渠道名称
            private BigDecimal value; // 金额或间夜数

            public ChannelValue() {}

            public ChannelValue(String channelName, BigDecimal value) {
                this.channelName = channelName;
                this.value = value;
            }

            public String getChannelName() {
                return channelName;
            }

            public void setChannelName(String channelName) {
                this.channelName = channelName;
            }

            public BigDecimal getValue() {
                return value;
            }

            public void setValue(BigDecimal value) {
                this.value = value;
            }
        }
    }

    /**
     * 渠道明细(表格数据)
     */
    public static class ChannelDetail {
        private String channelName; // 渠道名称
        private BigDecimal totalRevenue; // 总消费
        private Integer totalRoomNights; // 总间夜数
        private List<DailyValue> dailyValues; // 每日数据

        public ChannelDetail() {}

        public ChannelDetail(String channelName, BigDecimal totalRevenue, Integer totalRoomNights, List<DailyValue> dailyValues) {
            this.channelName = channelName;
            this.totalRevenue = totalRevenue;
            this.totalRoomNights = totalRoomNights;
            this.dailyValues = dailyValues;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public Integer getTotalRoomNights() {
            return totalRoomNights;
        }

        public void setTotalRoomNights(Integer totalRoomNights) {
            this.totalRoomNights = totalRoomNights;
        }

        public List<DailyValue> getDailyValues() {
            return dailyValues;
        }

        public void setDailyValues(List<DailyValue> dailyValues) {
            this.dailyValues = dailyValues;
        }

        /**
         * 每日数据
         */
        public static class DailyValue {
            private String date; // 日期
            private BigDecimal revenue; // 消费金额
            private Integer roomNights; // 间夜数

            public DailyValue() {}

            public DailyValue(String date, BigDecimal revenue, Integer roomNights) {
                this.date = date;
                this.revenue = revenue;
                this.roomNights = roomNights;
            }

            public String getDate() {
                return date;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public BigDecimal getRevenue() {
                return revenue;
            }

            public void setRevenue(BigDecimal revenue) {
                this.revenue = revenue;
            }

            public Integer getRoomNights() {
                return roomNights;
            }

            public void setRoomNights(Integer roomNights) {
                this.roomNights = roomNights;
            }
        }
    }
}
