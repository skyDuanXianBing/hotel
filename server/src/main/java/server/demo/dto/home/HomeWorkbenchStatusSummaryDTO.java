package server.demo.dto.home;

public class HomeWorkbenchStatusSummaryDTO {
    private String statusGroup;
    private long count;

    public HomeWorkbenchStatusSummaryDTO() {
    }

    public HomeWorkbenchStatusSummaryDTO(String statusGroup, long count) {
        this.statusGroup = statusGroup;
        this.count = count;
    }

    public String getStatusGroup() {
        return statusGroup;
    }

    public void setStatusGroup(String statusGroup) {
        this.statusGroup = statusGroup;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
