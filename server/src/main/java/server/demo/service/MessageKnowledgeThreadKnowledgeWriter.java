package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.MessageKnowledgeEvidence;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.entity.SuMessageThread;
import server.demo.repository.MessageKnowledgeEvidenceRepository;
import server.demo.repository.MessageKnowledgeItemRepository;
import server.demo.util.UtcTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageKnowledgeThreadKnowledgeWriter {
    public static final String EXTRACTOR_VERSION = "thread-v1";

    private final MessageKnowledgeItemRepository itemRepository;
    private final MessageKnowledgeEvidenceRepository evidenceRepository;
    private final MessageKnowledgeEmbeddingTextService embeddingTextService;
    private final SuMessagingAiTextService textService;
    private final ObjectMapper objectMapper;

    public MessageKnowledgeThreadKnowledgeWriter(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEvidenceRepository evidenceRepository,
            MessageKnowledgeEmbeddingTextService embeddingTextService,
            SuMessagingAiTextService textService,
            ObjectMapper objectMapper
    ) {
        this.itemRepository = itemRepository;
        this.evidenceRepository = evidenceRepository;
        this.embeddingTextService = embeddingTextService;
        this.textService = textService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MessageKnowledgeThreadWriterResult write(
            Long storeId,
            SuMessageThread thread,
            SuMessagingThreadContext context,
            List<MessageKnowledgeThreadValidatedItem> validItems
    ) {
        if (validItems == null || validItems.isEmpty()) {
            return new MessageKnowledgeThreadWriterResult(0, 0);
        }
        int writtenItems = 0;
        int skippedDuplicateEvidence = 0;
        for (MessageKnowledgeThreadValidatedItem validItem : validItems) {
            WriteAttempt attempt = writeOne(storeId, thread, context, validItem);
            if (attempt.written()) {
                writtenItems++;
            }
            if (attempt.skippedDuplicateEvidence()) {
                skippedDuplicateEvidence++;
            }
        }
        return new MessageKnowledgeThreadWriterResult(writtenItems, skippedDuplicateEvidence);
    }

    private WriteAttempt writeOne(
            Long storeId,
            SuMessageThread thread,
            SuMessagingThreadContext context,
            MessageKnowledgeThreadValidatedItem validItem
    ) {
        String topicHash = textService.sha256(validItem.scopeKey() + "|" + validItem.topicCode());
        String factHash = textService.sha256(
                validItem.scopeKey() + "|" + validItem.topicCode() + "|" + validItem.normalizedAnswer()
        );
        String sourceMessageIdsJson = serializeSourceMessageIds(validItem.sourceMessageIds());
        String sourceFingerprint = buildSourceFingerprint(
                storeId,
                thread.getId(),
                factHash,
                validItem.sourceMessageIds()
        );
        if (evidenceRepository.existsByStoreIdAndSourceFingerprint(storeId, sourceFingerprint)) {
            return new WriteAttempt(false, true);
        }

        MessageKnowledgeItem item = itemRepository
                .findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                        storeId,
                        validItem.scopeKey(),
                        topicHash,
                        factHash
                )
                .orElseGet(MessageKnowledgeItem::new);
        boolean newItem = item.getId() == null;
        LocalDateTime sourceSeenAt = resolveSeenAt(validItem.sourceTimestamp());
        LocalDateTime refinedAt = UtcTimeUtil.nowLocalDateTime();
        boolean updateCanonical = shouldUpdateCanonical(item, newItem, sourceSeenAt, validItem.normalizedAnswer());

        applyItemFields(
                item,
                storeId,
                thread,
                context,
                validItem,
                topicHash,
                factHash,
                sourceSeenAt,
                refinedAt,
                updateCanonical
        );
        embeddingTextService.refreshSemanticFields(item, newItem);
        itemRepository.save(item);

        MessageKnowledgeEvidence evidence = buildEvidence(
                item,
                storeId,
                thread,
                context,
                validItem,
                sourceMessageIdsJson,
                sourceFingerprint
        );
        evidenceRepository.save(evidence);

        int evidenceCount = item.getEvidenceCount() == null ? 0 : item.getEvidenceCount();
        item.setEvidenceCount(evidenceCount + 1);
        item.setLastSeenAt(sourceSeenAt);
        item.setRefinedAt(refinedAt);
        itemRepository.save(item);
        return new WriteAttempt(true, false);
    }

    private void applyItemFields(
            MessageKnowledgeItem item,
            Long storeId,
            SuMessageThread thread,
            SuMessagingThreadContext context,
            MessageKnowledgeThreadValidatedItem validItem,
            String topicHash,
            String factHash,
            LocalDateTime sourceSeenAt,
            LocalDateTime refinedAt,
            boolean updateCanonical
    ) {
        item.setStoreId(storeId);
        item.setScopeType(validItem.scopeType());
        item.setScopeId(validItem.scopeId());
        item.setScopeKey(validItem.scopeKey());
        item.setThreadId(thread.getId());
        item.setRoomId(context == null ? null : context.getRoomId());
        item.setRoomNumber(context == null ? null : context.getRoomNumber());
        item.setRoomTypeId(context == null ? null : context.getRoomTypeId());
        item.setRoomTypeName(context == null ? null : context.getRoomTypeName());
        item.setChannelId(context == null ? null : context.getChannelId());
        item.setTopic(validItem.topicCode());
        item.setTopicHash(topicHash);
        item.setFactHash(factHash);
        item.setExtractorVersion(EXTRACTOR_VERSION);
        if (updateCanonical || item.getQuestion() == null) {
            item.setQuestion(validItem.canonicalQuestion());
            item.setNormalizedQuestion(validItem.normalizedQuestion());
        }
        if (updateCanonical || item.getAnswer() == null) {
            item.setAnswer(validItem.canonicalAnswer());
            item.setNormalizedAnswer(validItem.normalizedAnswer());
            item.setNormalizedAnswerHash(textService.sha256(validItem.normalizedAnswer()));
        }
        if (item.getNormalizedQuestion() == null) {
            item.setNormalizedQuestion(validItem.normalizedQuestion());
        }
        if (item.getNormalizedAnswer() == null) {
            item.setNormalizedAnswer(validItem.normalizedAnswer());
        }
        if (item.getNormalizedAnswerHash() == null) {
            item.setNormalizedAnswerHash(textService.sha256(validItem.normalizedAnswer()));
        }
        if (updateCanonical || item.getLanguage() == null) {
            item.setLanguage(validItem.language());
        }
        item.setConfidence(validItem.confidence());
        item.setPiiRedactionStatus(MessageKnowledgeItem.REDACTION_STATUS_REDACTED);
        item.setStatus(resolveItemStatus(item.getStatus(), validItem.status()));
        if (item.getFirstSeenAt() == null) {
            item.setFirstSeenAt(sourceSeenAt);
        }
        item.setLastSeenAt(sourceSeenAt);
        item.setRefinedAt(refinedAt);
    }

    private MessageKnowledgeEvidence buildEvidence(
            MessageKnowledgeItem item,
            Long storeId,
            SuMessageThread thread,
            SuMessagingThreadContext context,
            MessageKnowledgeThreadValidatedItem validItem,
            String sourceMessageIdsJson,
            String sourceFingerprint
    ) {
        MessageKnowledgeEvidence evidence = new MessageKnowledgeEvidence();
        evidence.setStoreId(storeId);
        evidence.setItem(item);
        evidence.setScopeType(validItem.scopeType());
        evidence.setScopeId(validItem.scopeId());
        evidence.setScopeKey(validItem.scopeKey());
        evidence.setThreadId(thread.getId());
        evidence.setReservationId(context == null ? null : context.getReservationId());
        evidence.setRoomId(context == null ? null : context.getRoomId());
        evidence.setRoomNumber(context == null ? null : context.getRoomNumber());
        evidence.setRoomTypeId(context == null ? null : context.getRoomTypeId());
        evidence.setRoomTypeName(context == null ? null : context.getRoomTypeName());
        evidence.setChannelId(context == null ? null : context.getChannelId());
        evidence.setBookingKey(null);
        evidence.setSourceKind(MessageKnowledgeEvidence.SOURCE_KIND_THREAD_CONVERSATION);
        evidence.setSourceMessageIdsJson(sourceMessageIdsJson);
        evidence.setSourceMessageStartId(minSourceId(validItem.sourceMessageIds()));
        evidence.setSourceMessageEndId(maxSourceId(validItem.sourceMessageIds()));
        evidence.setExtractorVersion(EXTRACTOR_VERSION);
        evidence.setSourceFingerprint(sourceFingerprint);
        evidence.setSourceTimestamp(resolveSeenAt(validItem.sourceTimestamp()));
        evidence.setQuestion(validItem.canonicalQuestion());
        evidence.setAnswer(validItem.canonicalAnswer());
        evidence.setNormalizedText(validItem.normalizedEvidence());
        evidence.setNormalizedHash(textService.sha256(validItem.normalizedEvidence()));
        evidence.setLanguage(validItem.language());
        evidence.setConfidence(validItem.confidence());
        evidence.setStatus(resolveEvidenceStatus(validItem.status()));
        evidence.setPiiRedactionStatus(MessageKnowledgeEvidence.REDACTION_STATUS_REDACTED);
        return evidence;
    }

    private boolean shouldUpdateCanonical(
            MessageKnowledgeItem item,
            boolean newItem,
            LocalDateTime sourceSeenAt,
            String normalizedAnswer
    ) {
        if (newItem) {
            return true;
        }
        if (item.getAnswer() == null || item.getNormalizedAnswer() == null) {
            return true;
        }
        if (normalizedAnswer != null && !normalizedAnswer.equals(item.getNormalizedAnswer())) {
            LocalDateTime lastSeenAt = item.getLastSeenAt();
            return lastSeenAt == null || !sourceSeenAt.isBefore(lastSeenAt);
        }
        return false;
    }

    private String resolveItemStatus(String currentStatus, String validStatus) {
        if (MessageKnowledgeItem.STATUS_ARCHIVED.equals(currentStatus)) {
            return currentStatus;
        }
        if (MessageKnowledgeItem.STATUS_ACTIVE.equals(currentStatus)
                && MessageKnowledgeItem.STATUS_CANDIDATE.equals(validStatus)) {
            return currentStatus;
        }
        if (validStatus == null || validStatus.isBlank()) {
            return MessageKnowledgeItem.STATUS_CANDIDATE;
        }
        return validStatus;
    }

    private String resolveEvidenceStatus(String validStatus) {
        if (MessageKnowledgeItem.STATUS_ACTIVE.equals(validStatus)) {
            return MessageKnowledgeEvidence.STATUS_ACTIVE;
        }
        return MessageKnowledgeEvidence.STATUS_CANDIDATE;
    }

    private Long minSourceId(List<Long> sourceMessageIds) {
        Long value = null;
        for (Long sourceMessageId : sourceMessageIds) {
            if (sourceMessageId == null) {
                continue;
            }
            if (value == null || sourceMessageId < value) {
                value = sourceMessageId;
            }
        }
        return value;
    }

    private Long maxSourceId(List<Long> sourceMessageIds) {
        Long value = null;
        for (Long sourceMessageId : sourceMessageIds) {
            if (sourceMessageId == null) {
                continue;
            }
            if (value == null || sourceMessageId > value) {
                value = sourceMessageId;
            }
        }
        return value;
    }

    private String serializeSourceMessageIds(List<Long> sourceMessageIds) {
        try {
            return objectMapper.writeValueAsString(sourceMessageIds == null ? List.of() : sourceMessageIds);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize thread source message ids", e);
        }
    }

    private String buildSourceFingerprint(
            Long storeId,
            Long threadId,
            String factHash,
            List<Long> sourceMessageIds
    ) {
        List<Long> sortedSourceIds = new ArrayList<>();
        if (sourceMessageIds != null) {
            for (Long sourceMessageId : sourceMessageIds) {
                if (sourceMessageId != null) {
                    sortedSourceIds.add(sourceMessageId);
                }
            }
        }
        sortedSourceIds.sort(Long::compareTo);
        List<String> ids = new ArrayList<>();
        for (Long sourceMessageId : sortedSourceIds) {
            ids.add(String.valueOf(sourceMessageId));
        }
        return textService.sha256(
                storeId + "|" + threadId + "|" + EXTRACTOR_VERSION + "|" + factHash + "|" + String.join(",", ids)
        );
    }

    private static LocalDateTime resolveSeenAt(LocalDateTime sourceTimestamp) {
        if (sourceTimestamp != null) {
            return sourceTimestamp;
        }
        return UtcTimeUtil.nowLocalDateTime();
    }

    private record WriteAttempt(
            boolean written,
            boolean skippedDuplicateEvidence
    ) {
    }
}
