package server.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class SuMessagingAiReplyDraftResponse {
    private String draftReply;
    private String retrievalStatus;
    private List<String> warnings = new ArrayList<>();
    private Integer matchedKnowledgeCount;
    private Long processingTimeMs;

    public SuMessagingAiReplyDraftResponse() {
    }

    public SuMessagingAiReplyDraftResponse(
            String draftReply,
            String retrievalStatus,
            List<String> warnings,
            Integer matchedKnowledgeCount,
            Long processingTimeMs
    ) {
        this.draftReply = draftReply;
        this.retrievalStatus = retrievalStatus;
        setWarnings(warnings);
        this.matchedKnowledgeCount = matchedKnowledgeCount;
        this.processingTimeMs = processingTimeMs;
    }

    public String getDraftReply() {
        return draftReply;
    }

    public void setDraftReply(String draftReply) {
        this.draftReply = draftReply;
    }

    public String getRetrievalStatus() {
        return retrievalStatus;
    }

    public void setRetrievalStatus(String retrievalStatus) {
        this.retrievalStatus = retrievalStatus;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public Integer getMatchedKnowledgeCount() {
        return matchedKnowledgeCount;
    }

    public void setMatchedKnowledgeCount(Integer matchedKnowledgeCount) {
        this.matchedKnowledgeCount = matchedKnowledgeCount;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}
