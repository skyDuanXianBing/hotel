package server.demo.dto;

public class MessageKnowledgeRebuildResponse {
    private Long storeId;
    private Integer lookbackDays;
    private Integer limit;
    private Integer attemptedCount;

    public MessageKnowledgeRebuildResponse() {
    }

    public MessageKnowledgeRebuildResponse(
            Long storeId,
            Integer lookbackDays,
            Integer limit,
            Integer attemptedCount
    ) {
        this.storeId = storeId;
        this.lookbackDays = lookbackDays;
        this.limit = limit;
        this.attemptedCount = attemptedCount;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Integer getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(Integer lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getAttemptedCount() {
        return attemptedCount;
    }

    public void setAttemptedCount(Integer attemptedCount) {
        this.attemptedCount = attemptedCount;
    }
}
