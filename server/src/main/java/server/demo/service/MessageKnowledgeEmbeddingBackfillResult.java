package server.demo.service;

public record MessageKnowledgeEmbeddingBackfillResult(
        int attempted,
        int succeeded,
        int failed,
        String skippedReason
) {
    public static MessageKnowledgeEmbeddingBackfillResult skipped(String reason) {
        return new MessageKnowledgeEmbeddingBackfillResult(0, 0, 0, reason);
    }

    public MessageKnowledgeEmbeddingBackfillResult plus(MessageKnowledgeEmbeddingBackfillResult other) {
        if (other == null) {
            return this;
        }
        String reason = skippedReason;
        if (reason == null) {
            reason = other.skippedReason();
        }
        return new MessageKnowledgeEmbeddingBackfillResult(
                attempted + other.attempted(),
                succeeded + other.succeeded(),
                failed + other.failed(),
                reason
        );
    }
}
