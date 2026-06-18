package server.demo.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.dto.SuMessagingAiReplyDraftRequest;
import server.demo.dto.SuMessagingAiReplyDraftResponse;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SuMessagingAiReplyDraftService {
    private static final Logger logger = LoggerFactory.getLogger(SuMessagingAiReplyDraftService.class);

    private static final int RECENT_MESSAGE_LIMIT = 12;
    private static final int SIMILAR_MATCH_LIMIT = 3;
    private static final String WARNING_KNOWLEDGE_RETRIEVAL_FAILED = "KNOWLEDGE_RETRIEVAL_FAILED";
    private static final String WARNING_LATEST_MESSAGE_ID_IGNORED = "LATEST_MESSAGE_ID_IGNORED";
    private static final String WARNING_MODEL_GENERATION_FAILED = "MODEL_GENERATION_FAILED";
    private static final String WARNING_MODEL_RETURNED_EMPTY = "MODEL_RETURNED_EMPTY";
    private static final double HIGH_CONFIDENCE_FALLBACK_SCORE = 0.32;

    private final SuMessageThreadRepository threadRepository;
    private final SuMessageRepository messageRepository;
    private final SuMessagingThreadContextResolver contextResolver;
    private final MessageKnowledgeSearchService searchService;
    private final SuMessagingAiPromptBuilder promptBuilder;
    private final ChatLanguageModel chatLanguageModel;

    public SuMessagingAiReplyDraftService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            SuMessagingThreadContextResolver contextResolver,
            MessageKnowledgeSearchService searchService,
            SuMessagingAiPromptBuilder promptBuilder,
            ChatLanguageModel chatLanguageModel
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.contextResolver = contextResolver;
        this.searchService = searchService;
        this.promptBuilder = promptBuilder;
        this.chatLanguageModel = chatLanguageModel;
    }

    public SuMessagingAiReplyDraftResponse generateDraft(
            Long storeId,
            Long threadId,
            SuMessagingAiReplyDraftRequest request
    ) {
        long startedAt = System.currentTimeMillis();
        SuMessagingAiReplyDraftRequest safeRequest =
                request == null ? new SuMessagingAiReplyDraftRequest() : request;
        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("消息会话不存在或不属于当前门店"));

        List<String> warnings = new ArrayList<>();
        SuMessagingThreadContext context = contextResolver.resolve(storeId, thread, safeRequest);
        addWarnings(warnings, context.getWarnings());

        LatestGuestMessage latestGuestMessage = resolveLatestGuestMessage(storeId, threadId, safeRequest, warnings);
        if (latestGuestMessage.content() == null || latestGuestMessage.content().isBlank()) {
            throw new IllegalArgumentException("缺少可用于生成草稿的客人消息");
        }

        MessageKnowledgeSearchResult searchResult = searchSimilarSafely(
                storeId,
                threadId,
                context,
                latestGuestMessage.content(),
                warnings
        );
        addWarnings(warnings, searchResult.getWarnings());
        logger.debug(
                "AI draft knowledge retrieval. storeId={}, threadId={}, status={}, matchedCount={}, warnings={}",
                storeId,
                threadId,
                searchResult.getRetrievalStatus(),
                searchResult.getMatches().size(),
                searchResult.getWarnings()
        );

        List<SuMessage> recentMessages = messageRepository.findRecentByStoreIdAndThreadIdDesc(
                storeId,
                threadId,
                PageRequest.of(0, RECENT_MESSAGE_LIMIT)
        );
        String prompt = promptBuilder.buildPrompt(
                context,
                recentMessages,
                safeRequest.getRecentMessages(),
                latestGuestMessage.content(),
                searchResult,
                safeRequest.getLanguage()
        );

        String draftReply = generateDraftReply(prompt, latestGuestMessage.content(), searchResult, warnings);
        long processingTimeMs = System.currentTimeMillis() - startedAt;
        return new SuMessagingAiReplyDraftResponse(
                draftReply,
                searchResult.getRetrievalStatus(),
                warnings,
                searchResult.getMatches().size(),
                processingTimeMs
        );
    }

    private MessageKnowledgeSearchResult searchSimilarSafely(
            Long storeId,
            Long threadId,
            SuMessagingThreadContext context,
            String latestGuestMessage,
            List<String> warnings
    ) {
        try {
            return searchService.searchSimilar(
                    storeId,
                    threadId,
                    context,
                    latestGuestMessage,
                    SIMILAR_MATCH_LIMIT
            );
        } catch (Exception e) {
            logger.warn("Failed to search message knowledge. storeId={}, threadId={}, error={}",
                    storeId, threadId, e.getMessage(), e);
            addWarning(warnings, WARNING_KNOWLEDGE_RETRIEVAL_FAILED);
            return new MessageKnowledgeSearchResult(
                    MessageKnowledgeSearchService.STATUS_FAILED,
                    List.of(),
                    List.of(WARNING_KNOWLEDGE_RETRIEVAL_FAILED)
            );
        }
    }

    private LatestGuestMessage resolveLatestGuestMessage(
            Long storeId,
            Long threadId,
            SuMessagingAiReplyDraftRequest request,
            List<String> warnings
    ) {
        if (request.getLatestGuestMessageId() != null) {
            Optional<SuMessage> requestedMessage =
                    messageRepository.findByStoreIdAndId(storeId, request.getLatestGuestMessageId());
            if (requestedMessage.isPresent() && isThreadGuestMessage(requestedMessage.get(), threadId)) {
                return new LatestGuestMessage(requestedMessage.get().getContent());
            }
            addWarning(warnings, WARNING_LATEST_MESSAGE_ID_IGNORED);
        }

        List<SuMessage> storedGuestMessages = messageRepository.findRecentByStoreIdAndThreadIdAndSenderTypeDesc(
                storeId,
                threadId,
                SuMessagingSenderType.GUEST,
                PageRequest.of(0, 1)
        );
        if (!storedGuestMessages.isEmpty()) {
            return new LatestGuestMessage(storedGuestMessages.get(0).getContent());
        }

        String requestMessage = findLatestRequestGuestMessage(request.getRecentMessages());
        return new LatestGuestMessage(requestMessage);
    }

    private String generateDraftReply(
            String prompt,
            String latestGuestMessage,
            MessageKnowledgeSearchResult searchResult,
            List<String> warnings
    ) {
        try {
            String generated = chatLanguageModel.generate(prompt);
            String cleaned = cleanupGeneratedReply(generated);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
            addWarning(warnings, WARNING_MODEL_RETURNED_EMPTY);
        } catch (Exception e) {
            logger.warn("Failed to generate AI reply draft: {}", e.getMessage(), e);
            addWarning(warnings, WARNING_MODEL_GENERATION_FAILED);
        }

        if (canUseHistoricalAnswerFallback(searchResult)) {
            return cleanupGeneratedReply(searchResult.getMatches().get(0).getCandidate().getAnswer());
        }
        return fallbackDraft(latestGuestMessage);
    }

    private static boolean isThreadGuestMessage(SuMessage message, Long threadId) {
        return message.getThread() != null
                && threadId.equals(message.getThread().getId())
                && message.getSenderType() == SuMessagingSenderType.GUEST;
    }

    private static String findLatestRequestGuestMessage(
            List<SuMessagingAiReplyDraftRequest.RecentMessage> recentMessages
    ) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return null;
        }

        for (int index = recentMessages.size() - 1; index >= 0; index--) {
            SuMessagingAiReplyDraftRequest.RecentMessage message = recentMessages.get(index);
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            if (isGuestDirection(message.getDirection())) {
                return message.getContent();
            }
        }

        for (int index = recentMessages.size() - 1; index >= 0; index--) {
            SuMessagingAiReplyDraftRequest.RecentMessage message = recentMessages.get(index);
            if (message != null && message.getContent() != null && !message.getContent().isBlank()) {
                return message.getContent();
            }
        }
        return null;
    }

    private static boolean isGuestDirection(String direction) {
        if (direction == null) {
            return false;
        }
        String normalized = direction.trim();
        return "GUEST".equalsIgnoreCase(normalized)
                || "CUSTOMER".equalsIgnoreCase(normalized)
                || "USER".equalsIgnoreCase(normalized);
    }

    private static boolean canUseHistoricalAnswerFallback(MessageKnowledgeSearchResult searchResult) {
        if (searchResult == null || searchResult.getMatches().isEmpty()) {
            return false;
        }
        if (!MessageKnowledgeSearchService.STATUS_MATCHED.equals(searchResult.getRetrievalStatus())) {
            return false;
        }

        MessageKnowledgeMatch bestMatch = searchResult.getMatches().get(0);
        return bestMatch != null
                && bestMatch.getCandidate() != null
                && bestMatch.getCandidate().getAnswer() != null
                && !bestMatch.getCandidate().getAnswer().isBlank()
                && bestMatch.getScore() >= HIGH_CONFIDENCE_FALLBACK_SCORE;
    }

    private static String cleanupGeneratedReply(String generated) {
        if (generated == null) {
            return "";
        }
        String cleaned = generated.trim();
        cleaned = cleaned.replace("[DRAFT]", "").replace("[/DRAFT]", "").trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static String fallbackDraft(String latestGuestMessage) {
        if (latestGuestMessage != null && latestGuestMessage.matches(".*[\\u3040-\\u30FF].*")) {
            return "お問い合わせありがとうございます。確認のうえ、スタッフより改めてご案内いたします。";
        }
        if (latestGuestMessage != null && latestGuestMessage.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "문의해 주셔서 감사합니다. 확인 후 직원이 다시 안내드리겠습니다.";
        }
        if (latestGuestMessage != null && latestGuestMessage.matches(".*[\\u4E00-\\u9FFF].*")) {
            return "您好，感谢您的消息。我们会尽快确认后回复您。";
        }
        return "Thank you for your message. We will check this and get back to you shortly.";
    }

    private static void addWarning(List<String> warnings, String warning) {
        if (warning != null && !warning.isBlank() && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    private static void addWarnings(List<String> warnings, List<String> newWarnings) {
        if (newWarnings == null) {
            return;
        }
        for (String warning : newWarnings) {
            addWarning(warnings, warning);
        }
    }

    private static class LatestGuestMessage {
        private final String content;

        LatestGuestMessage(String content) {
            this.content = content;
        }

        String content() {
            return content;
        }
    }
}
