package server.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class MappingPriceSettingsSaveResponseDTO {

    private Long channelId;
    private String channelCode;
    private String suChannelId;
    private String suPropertyId;
    private String operationId;
    private String batchId;
    private String status;
    private String message;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private int staleCount;
    private List<MappingPriceSettingRowDTO> rows = new ArrayList<>();

    public void refreshCounts() {
        int success = 0;
        int failure = 0;
        int stale = 0;
        for (MappingPriceSettingRowDTO row : rows) {
            if (row == null || row.getSyncStatus() == null) {
                continue;
            }
            if ("SUCCESS".equalsIgnoreCase(row.getSyncStatus())) {
                success++;
            } else if ("STALE_MAPPING".equalsIgnoreCase(row.getSyncStatus())) {
                stale++;
                failure++;
            } else if ("FAILED".equalsIgnoreCase(row.getSyncStatus())) {
                failure++;
            }
        }
        totalCount = rows.size();
        successCount = success;
        failureCount = failure;
        staleCount = stale;
        if (totalCount == 0 || successCount == 0) {
            status = "FAILED";
        } else if (failureCount == 0) {
            status = "SUCCESS";
        } else {
            status = "PARTIAL";
        }
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

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public List<MappingPriceSettingRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<MappingPriceSettingRowDTO> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
}
