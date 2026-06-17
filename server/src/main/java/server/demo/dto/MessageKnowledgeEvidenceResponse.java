package server.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class MessageKnowledgeEvidenceResponse {
    private MessageKnowledgeItemDTO item;
    private List<MessageKnowledgeEvidenceDTO> evidence = new ArrayList<>();

    public MessageKnowledgeEvidenceResponse() {
    }

    public MessageKnowledgeEvidenceResponse(
            MessageKnowledgeItemDTO item,
            List<MessageKnowledgeEvidenceDTO> evidence
    ) {
        this.item = item;
        setEvidence(evidence);
    }

    public MessageKnowledgeItemDTO getItem() {
        return item;
    }

    public void setItem(MessageKnowledgeItemDTO item) {
        this.item = item;
    }

    public List<MessageKnowledgeEvidenceDTO> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<MessageKnowledgeEvidenceDTO> evidence) {
        this.evidence = evidence == null ? new ArrayList<>() : evidence;
    }

    public List<MessageKnowledgeEvidenceDTO> getContent() {
        return evidence;
    }

    public List<MessageKnowledgeEvidenceDTO> getItems() {
        return evidence;
    }
}
