package server.demo.dto.home;

public class HomeWorkbenchTypeSummaryDTO {
    private String type;
    private long count;
    private boolean connected;

    public HomeWorkbenchTypeSummaryDTO() {
    }

    public HomeWorkbenchTypeSummaryDTO(String type, long count, boolean connected) {
        this.type = type;
        this.count = count;
        this.connected = connected;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
