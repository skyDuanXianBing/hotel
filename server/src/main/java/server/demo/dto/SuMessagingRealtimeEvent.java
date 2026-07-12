package server.demo.dto;

public class SuMessagingRealtimeEvent {

    private String eventType;
    private Long threadId;
    private SuMessagingMessageDTO message;
    private String resourceType;

    public SuMessagingRealtimeEvent() {
    }

    public SuMessagingRealtimeEvent(String eventType, Long threadId, SuMessagingMessageDTO message) {
        this.eventType = eventType;
        this.threadId = threadId;
        this.message = message;
    }

    public SuMessagingRealtimeEvent(String eventType, String resourceType) {
        this.eventType = eventType;
        this.resourceType = resourceType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public SuMessagingMessageDTO getMessage() {
        return message;
    }

    public void setMessage(SuMessagingMessageDTO message) {
        this.message = message;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
