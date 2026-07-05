package server.demo.dto;

public class StatisticsSourceMetadataDTO {
    private String metric;
    private String sourceType;
    private String dateBasis;
    private String amountBasis;
    private String note;

    public StatisticsSourceMetadataDTO() {}

    public StatisticsSourceMetadataDTO(
            String metric,
            String sourceType,
            String dateBasis,
            String amountBasis,
            String note
    ) {
        this.metric = metric;
        this.sourceType = sourceType;
        this.dateBasis = dateBasis;
        this.amountBasis = amountBasis;
        this.note = note;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDateBasis() {
        return dateBasis;
    }

    public void setDateBasis(String dateBasis) {
        this.dateBasis = dateBasis;
    }

    public String getAmountBasis() {
        return amountBasis;
    }

    public void setAmountBasis(String amountBasis) {
        this.amountBasis = amountBasis;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
