package server.demo.dto;

import java.util.List;

public class SuMessagingMessagePageResponse {
    private List<SuMessagingMessageDTO> items;
    private int limit;
    private boolean hasMoreBefore;
    private Long nextBeforeMessageId;
    private boolean hasMoreAfter;

    public SuMessagingMessagePageResponse() {
    }

    public SuMessagingMessagePageResponse(
            List<SuMessagingMessageDTO> items,
            int limit,
            boolean hasMoreBefore,
            Long nextBeforeMessageId,
            boolean hasMoreAfter
    ) {
        this.items = items;
        this.limit = limit;
        this.hasMoreBefore = hasMoreBefore;
        this.nextBeforeMessageId = nextBeforeMessageId;
        this.hasMoreAfter = hasMoreAfter;
    }

    public List<SuMessagingMessageDTO> getItems() {
        return items;
    }

    public void setItems(List<SuMessagingMessageDTO> items) {
        this.items = items;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isHasMoreBefore() {
        return hasMoreBefore;
    }

    public void setHasMoreBefore(boolean hasMoreBefore) {
        this.hasMoreBefore = hasMoreBefore;
    }

    public Long getNextBeforeMessageId() {
        return nextBeforeMessageId;
    }

    public void setNextBeforeMessageId(Long nextBeforeMessageId) {
        this.nextBeforeMessageId = nextBeforeMessageId;
    }

    public boolean isHasMoreAfter() {
        return hasMoreAfter;
    }

    public void setHasMoreAfter(boolean hasMoreAfter) {
        this.hasMoreAfter = hasMoreAfter;
    }
}
