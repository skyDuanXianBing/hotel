package server.demo.service;

public record MessageKnowledgeThreadExtractionSummary(
        Long storeId,
        Long threadId,
        Long coveredUntilMessageId,
        String runId,
        String status,
        int totalMessages,
        int promptMessages,
        int parsedItems,
        int validItems,
        int rejectedItems,
        int writtenItems,
        int skippedDuplicateEvidence
) {
    public static MessageKnowledgeThreadExtractionSummary disabled(
            Long storeId,
            Long threadId,
            Long coveredUntilMessageId,
            String runId
    ) {
        return new MessageKnowledgeThreadExtractionSummary(
                storeId,
                threadId,
                coveredUntilMessageId,
                runId,
                "DISABLED",
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }
}
