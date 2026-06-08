package server.demo.dto;

import java.util.Map;

public class SuMessagingUnreadSummaryDTO {
    private long totalUnread;
    private long unreadThreadCount;
    private Map<String, Long> byChannel;

    public SuMessagingUnreadSummaryDTO() {
    }

    public SuMessagingUnreadSummaryDTO(
            long totalUnread,
            long unreadThreadCount,
            Map<String, Long> byChannel
    ) {
        this.totalUnread = totalUnread;
        this.unreadThreadCount = unreadThreadCount;
        this.byChannel = byChannel;
    }

    public long getTotalUnread() {
        return totalUnread;
    }

    public void setTotalUnread(long totalUnread) {
        this.totalUnread = totalUnread;
    }

    public long getUnreadThreadCount() {
        return unreadThreadCount;
    }

    public void setUnreadThreadCount(long unreadThreadCount) {
        this.unreadThreadCount = unreadThreadCount;
    }

    public Map<String, Long> getByChannel() {
        return byChannel;
    }

    public void setByChannel(Map<String, Long> byChannel) {
        this.byChannel = byChannel;
    }
}
