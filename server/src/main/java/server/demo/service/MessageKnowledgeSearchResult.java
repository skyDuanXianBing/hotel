package server.demo.service;

import java.util.ArrayList;
import java.util.List;

public class MessageKnowledgeSearchResult {
    private String retrievalStatus;
    private List<MessageKnowledgeMatch> matches = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public MessageKnowledgeSearchResult() {
    }

    public MessageKnowledgeSearchResult(
            String retrievalStatus,
            List<MessageKnowledgeMatch> matches,
            List<String> warnings
    ) {
        this.retrievalStatus = retrievalStatus;
        setMatches(matches);
        setWarnings(warnings);
    }

    public String getRetrievalStatus() {
        return retrievalStatus;
    }

    public void setRetrievalStatus(String retrievalStatus) {
        this.retrievalStatus = retrievalStatus;
    }

    public List<MessageKnowledgeMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<MessageKnowledgeMatch> matches) {
        this.matches = matches == null ? new ArrayList<>() : matches;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }
}
