package server.demo.dto;

/**
 * 批量补齐保洁任务结果
 */
public class CleaningTaskGenerateResult {
    private int processedReservations;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;

    public CleaningTaskGenerateResult() {}

    public CleaningTaskGenerateResult(int processedReservations, int createdCount, int updatedCount, int skippedCount) {
        this.processedReservations = processedReservations;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.skippedCount = skippedCount;
    }

    public int getProcessedReservations() {
        return processedReservations;
    }

    public void setProcessedReservations(int processedReservations) {
        this.processedReservations = processedReservations;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
}
