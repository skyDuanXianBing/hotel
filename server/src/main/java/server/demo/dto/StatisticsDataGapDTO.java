package server.demo.dto;

public class StatisticsDataGapDTO {
    private String metric;
    private String reason;
    private String requiredSource;
    private Boolean unsupported;

    public StatisticsDataGapDTO() {}

    public StatisticsDataGapDTO(String metric, String reason, String requiredSource, Boolean unsupported) {
        this.metric = metric;
        this.reason = reason;
        this.requiredSource = requiredSource;
        this.unsupported = unsupported;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequiredSource() {
        return requiredSource;
    }

    public void setRequiredSource(String requiredSource) {
        this.requiredSource = requiredSource;
    }

    public Boolean getUnsupported() {
        return unsupported;
    }

    public void setUnsupported(Boolean unsupported) {
        this.unsupported = unsupported;
    }
}
