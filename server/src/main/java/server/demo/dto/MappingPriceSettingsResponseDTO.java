package server.demo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MappingPriceSettingsResponseDTO {

    private Long channelId;
    private String channelCode;
    private String channelName;
    private Long otaIntegrationId;
    private String suChannelId;
    private String suPropertyId;
    private BigDecimal defaultMultiplier;
    private BigDecimal defaultSurcharge;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private int staleCount;
    private int unsyncedCount;
    private List<MappingPriceSettingRowDTO> rows = new ArrayList<>();

    public void refreshCounts() {
        int success = 0;
        int failure = 0;
        int stale = 0;
        int unsynced = 0;
        for (MappingPriceSettingRowDTO row : rows) {
            if (row == null || row.getSyncStatus() == null) {
                continue;
            }
            String status = row.getSyncStatus();
            if ("SUCCESS".equalsIgnoreCase(status)) {
                success++;
            } else if ("FAILED".equalsIgnoreCase(status)) {
                failure++;
            } else if ("STALE_MAPPING".equalsIgnoreCase(status)) {
                stale++;
            } else if ("UNSYNCED".equalsIgnoreCase(status)) {
                unsynced++;
            }
        }
        totalCount = rows.size();
        successCount = success;
        failureCount = failure;
        staleCount = stale;
        unsyncedCount = unsynced;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Long getOtaIntegrationId() {
        return otaIntegrationId;
    }

    public void setOtaIntegrationId(Long otaIntegrationId) {
        this.otaIntegrationId = otaIntegrationId;
    }

    public String getSuChannelId() {
        return suChannelId;
    }

    public void setSuChannelId(String suChannelId) {
        this.suChannelId = suChannelId;
    }

    public String getSuPropertyId() {
        return suPropertyId;
    }

    public void setSuPropertyId(String suPropertyId) {
        this.suPropertyId = suPropertyId;
    }

    public BigDecimal getDefaultMultiplier() {
        return defaultMultiplier;
    }

    public void setDefaultMultiplier(BigDecimal defaultMultiplier) {
        this.defaultMultiplier = defaultMultiplier;
    }

    public BigDecimal getDefaultSurcharge() {
        return defaultSurcharge;
    }

    public void setDefaultSurcharge(BigDecimal defaultSurcharge) {
        this.defaultSurcharge = defaultSurcharge;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getStaleCount() {
        return staleCount;
    }

    public void setStaleCount(int staleCount) {
        this.staleCount = staleCount;
    }

    public int getUnsyncedCount() {
        return unsyncedCount;
    }

    public void setUnsyncedCount(int unsyncedCount) {
        this.unsyncedCount = unsyncedCount;
    }

    public List<MappingPriceSettingRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<MappingPriceSettingRowDTO> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
}
