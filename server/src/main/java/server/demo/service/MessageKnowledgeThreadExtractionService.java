package server.demo.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeTopic;
import server.demo.enums.SuMessagingSenderType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageKnowledgeThreadExtractionService {
    private static final int DEFAULT_MAX_TOPICS = 50;

    private final MessageKnowledgeThreadConversationLoader conversationLoader;
    private final MessageKnowledgeThreadPromptBuilder promptBuilder;
    private final MessageKnowledgeThreadExtractionOutputParser outputParser;
    private final MessageKnowledgeThreadExtractionOutputValidator outputValidator;
    private final MessageKnowledgeThreadKnowledgeWriter knowledgeWriter;
    private final MessageKnowledgeTopicDictionaryService topicDictionaryService;
    private final ChatLanguageModel chatLanguageModel;

    @Value("${messaging.knowledge.enabled:false}")
    private boolean knowledgeEnabled;

    @Value("${messaging.knowledge.thread-extractor.enabled:false}")
    private boolean threadExtractorEnabled;

    @Value("${messaging.knowledge.thread-extractor.max-topics-per-store:50}")
    private int maxTopicsPerStore;

    public MessageKnowledgeThreadExtractionService(
            MessageKnowledgeThreadConversationLoader conversationLoader,
            MessageKnowledgeThreadPromptBuilder promptBuilder,
            MessageKnowledgeThreadExtractionOutputParser outputParser,
            MessageKnowledgeThreadExtractionOutputValidator outputValidator,
            MessageKnowledgeThreadKnowledgeWriter knowledgeWriter,
            MessageKnowledgeTopicDictionaryService topicDictionaryService,
            ChatLanguageModel chatLanguageModel
    ) {
        this.conversationLoader = conversationLoader;
        this.promptBuilder = promptBuilder;
        this.outputParser = outputParser;
        this.outputValidator = outputValidator;
        this.knowledgeWriter = knowledgeWriter;
        this.topicDictionaryService = topicDictionaryService;
        this.chatLanguageModel = chatLanguageModel;
    }

    public MessageKnowledgeThreadExtractionSummary extractThread(
            Long storeId,
            Long threadId,
            Long coveredUntilMessageId,
            String extractionOwner
    ) {
        return extractThread(storeId, threadId, coveredUntilMessageId, extractionOwner, extractionOwner);
    }

    public MessageKnowledgeThreadExtractionSummary extractThread(
            Long storeId,
            Long threadId,
            Long coveredUntilMessageId,
            String extractionOwner,
            String runId
    ) {
        if (!isEnabled()) {
            return MessageKnowledgeThreadExtractionSummary.disabled(
                    storeId,
                    threadId,
                    coveredUntilMessageId,
                    normalizeRunId(extractionOwner, runId)
            );
        }

        MessageKnowledgeThreadConversation conversation =
                conversationLoader.load(storeId, threadId, coveredUntilMessageId);
        if (conversation.messages().isEmpty() || !hasEligibleStaffEvidence(conversation.messages())) {
            return completedSummary(
                    conversation,
                    normalizeRunId(extractionOwner, runId),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        List<MessageKnowledgeTopic> activeTopics = topicDictionaryService.findActiveTopics(
                storeId,
                resolveMaxTopicsPerStore()
        );
        MessageKnowledgeThreadPrompt prompt = promptBuilder.buildPrompt(conversation, activeTopics);
        String output = chatLanguageModel.generate(prompt.prompt());
        MessageKnowledgeThreadParsedOutput parsedOutput = outputParser.parse(output);
        MessageKnowledgeThreadValidationContext validationContext = new MessageKnowledgeThreadValidationContext(
                storeId,
                threadId,
                conversation.context(),
                activeTopics,
                inputMessagesById(conversation.messages())
        );
        MessageKnowledgeThreadValidationResult validationResult =
                outputValidator.validate(parsedOutput, validationContext);
        MessageKnowledgeThreadWriterResult writerResult = knowledgeWriter.write(
                storeId,
                conversation.thread(),
                conversation.context(),
                validationResult.validItems()
        );

        return completedSummary(
                conversation,
                normalizeRunId(extractionOwner, runId),
                prompt.messageCount(),
                parsedOutput.items().size(),
                validationResult.validItems().size(),
                validationResult.rejectedItems().size(),
                writerResult.writtenItems(),
                writerResult.skippedDuplicateEvidence()
        );
    }

    public boolean isEnabled() {
        return knowledgeEnabled || threadExtractorEnabled;
    }

    private MessageKnowledgeThreadExtractionSummary completedSummary(
            MessageKnowledgeThreadConversation conversation,
            String runId,
            int promptMessages,
            int parsedItems,
            int validItems,
            int rejectedItems,
            int writtenItems,
            int skippedDuplicateEvidence
    ) {
        return new MessageKnowledgeThreadExtractionSummary(
                conversation.storeId(),
                conversation.threadId(),
                conversation.coveredUntilMessageId(),
                runId,
                "COMPLETED",
                conversation.messages().size(),
                promptMessages,
                parsedItems,
                validItems,
                rejectedItems,
                writtenItems,
                skippedDuplicateEvidence
        );
    }

    private Map<Long, MessageKnowledgeThreadConversationMessage> inputMessagesById(
            List<MessageKnowledgeThreadConversationMessage> messages
    ) {
        Map<Long, MessageKnowledgeThreadConversationMessage> byId = new LinkedHashMap<>();
        for (MessageKnowledgeThreadConversationMessage message : messages) {
            if (message != null && message.id() != null) {
                byId.put(message.id(), message);
            }
        }
        return byId;
    }

    private boolean hasEligibleStaffEvidence(List<MessageKnowledgeThreadConversationMessage> messages) {
        for (MessageKnowledgeThreadConversationMessage message : messages) {
            if (message == null) {
                continue;
            }
            if (message.senderType() == SuMessagingSenderType.STAFF && message.eligibleAsEvidence()) {
                return true;
            }
        }
        return false;
    }

    private int resolveMaxTopicsPerStore() {
        if (maxTopicsPerStore < 1) {
            return DEFAULT_MAX_TOPICS;
        }
        return Math.min(maxTopicsPerStore, DEFAULT_MAX_TOPICS);
    }

    private static String normalizeRunId(String extractionOwner, String runId) {
        if (runId != null && !runId.trim().isBlank()) {
            return runId.trim();
        }
        if (extractionOwner != null && !extractionOwner.trim().isBlank()) {
            return extractionOwner.trim();
        }
        return "manual-thread-extraction";
    }
}
