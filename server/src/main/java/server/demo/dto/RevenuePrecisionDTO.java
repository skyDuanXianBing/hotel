package server.demo.dto;

import java.math.BigDecimal;

public class RevenuePrecisionDTO {
    private String priceBasis;
    private String dateBasis;
    private String taxMode;
    private String currencyCode;
    private Integer exactRoomNights;
    private Integer averagedRoomNights;
    private Integer totalRoomNights;
    private BigDecimal coverageRate;
    private Integer residualConflictCount;
    private Boolean residualConflictDetected;

    public String getPriceBasis() {
        return priceBasis;
    }

    public void setPriceBasis(String priceBasis) {
        this.priceBasis = priceBasis;
    }

    public String getDateBasis() {
        return dateBasis;
    }

    public void setDateBasis(String dateBasis) {
        this.dateBasis = dateBasis;
    }

    public String getTaxMode() {
        return taxMode;
    }

    public void setTaxMode(String taxMode) {
        this.taxMode = taxMode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getExactRoomNights() {
        return exactRoomNights;
    }

    public void setExactRoomNights(Integer exactRoomNights) {
        this.exactRoomNights = exactRoomNights;
    }

    public Integer getAveragedRoomNights() {
        return averagedRoomNights;
    }

    public void setAveragedRoomNights(Integer averagedRoomNights) {
        this.averagedRoomNights = averagedRoomNights;
    }

    public Integer getTotalRoomNights() {
        return totalRoomNights;
    }

    public void setTotalRoomNights(Integer totalRoomNights) {
        this.totalRoomNights = totalRoomNights;
    }

    public BigDecimal getCoverageRate() {
        return coverageRate;
    }

    public void setCoverageRate(BigDecimal coverageRate) {
        this.coverageRate = coverageRate;
    }

    public Integer getResidualConflictCount() {
        return residualConflictCount;
    }

    public void setResidualConflictCount(Integer residualConflictCount) {
        this.residualConflictCount = residualConflictCount;
    }

    public Boolean getResidualConflictDetected() {
        return residualConflictDetected;
    }

    public void setResidualConflictDetected(Boolean residualConflictDetected) {
        this.residualConflictDetected = residualConflictDetected;
    }
}
