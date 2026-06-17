package server.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.MessageKnowledgeEvidenceDTO;
import server.demo.dto.MessageKnowledgeEvidenceResponse;
import server.demo.dto.MessageKnowledgeItemDTO;
import server.demo.dto.MessageKnowledgeItemPageResponse;
import server.demo.dto.MessageKnowledgeRejectRequest;
import server.demo.entity.MessageKnowledgeEvidence;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeEvidenceRepository;
import server.demo.repository.MessageKnowledgeItemRepository;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MessageKnowledgeManagementService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String STATUS_CONFLICT = "CONFLICT";
    private static final String STATUS_CONFLICTED = "CONFLICTED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANDIDATE = "CANDIDATE";
    private static final Set<String> ALLOWED_STATUS_FILTERS = Set.of(
            MessageKnowledgeItem.STATUS_ACTIVE,
            STATUS_CONFLICT,
            STATUS_CONFLICTED,
            STATUS_ARCHIVED,
            STATUS_REJECTED,
            STATUS_CANDIDATE
    );

    private final MessageKnowledgeItemRepository itemRepository;
    private final MessageKnowledgeEvidenceRepository evidenceRepository;

    public MessageKnowledgeManagementService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEvidenceRepository evidenceRepository
    ) {
        this.itemRepository = itemRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional(readOnly = true)
    public MessageKnowledgeItemPageResponse listItems(
            Long storeId,
            Integer page,
            Integer size,
            String status,
            String keyword,
            String scopeType,
            String topicCode
    ) {
        requireStoreId(storeId);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        Page<MessageKnowledgeItem> itemPage = itemRepository.searchManagementItems(
                storeId,
                normalizeStatusFilter(status),
                normalizeKeyword(keyword),
                normalizeUppercase(scopeType),
                normalizeTopic(topicCode),
                PageRequest.of(normalizedPage, normalizedSize)
        );
        List<MessageKnowledgeItemDTO> items = itemPage.getContent().stream()
                .map(this::toItemDTO)
                .toList();
        return new MessageKnowledgeItemPageResponse(
                items,
                normalizedPage,
                normalizedSize,
                itemPage.getTotalElements(),
                itemPage.getTotalPages(),
                itemPage.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public MessageKnowledgeEvidenceResponse getEvidence(Long storeId, Long itemId) {
        MessageKnowledgeItem item = loadItem(storeId, itemId);
        List<MessageKnowledgeEvidenceDTO> evidence = evidenceRepository
                .findByStoreIdAndItemIdOrderBySourceTimestampDesc(storeId, item.getId())
                .stream()
                .map(MessageKnowledgeManagementService::toEvidenceDTO)
                .toList();
        return new MessageKnowledgeEvidenceResponse(toItemDTO(item, languagesFromEvidence(evidence)), evidence);
    }

    @Transactional
    public MessageKnowledgeItemDTO approve(Long storeId, Long itemId) {
        MessageKnowledgeItem item = loadItem(storeId, itemId);
        ensureNotArchived(item);
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        return toItemDTO(itemRepository.save(item));
    }

    @Transactional
    public MessageKnowledgeItemDTO reject(Long storeId, Long itemId, MessageKnowledgeRejectRequest request) {
        MessageKnowledgeItem item = loadItem(storeId, itemId);
        ensureNotArchived(item);
        if (request != null) {
            normalizeBlankToNull(request.getReason());
        }
        item.setStatus(STATUS_REJECTED);
        return toItemDTO(itemRepository.save(item));
    }

    @Transactional
    public MessageKnowledgeItemDTO archive(Long storeId, Long itemId) {
        MessageKnowledgeItem item = loadItem(storeId, itemId);
        item.setStatus(STATUS_ARCHIVED);
        return toItemDTO(itemRepository.save(item));
    }

    private MessageKnowledgeItem loadItem(Long storeId, Long itemId) {
        requireStoreId(storeId);
        if (itemId == null) {
            throw new IllegalArgumentException("知识项 ID 不能为空");
        }
        return itemRepository.findByStoreIdAndId(storeId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("知识项不存在或不属于当前门店"));
    }

    private MessageKnowledgeItemDTO toItemDTO(MessageKnowledgeItem item) {
        List<String> evidenceLanguages = List.of();
        if (item.getId() != null) {
            evidenceLanguages = evidenceRepository.findDistinctLanguagesByStoreIdAndItemId(
                    item.getStoreId(),
                    item.getId()
            );
        }
        return toItemDTO(item, evidenceLanguages);
    }

    private static MessageKnowledgeItemDTO toItemDTO(
            MessageKnowledgeItem item,
            List<String> evidenceLanguages
    ) {
        MessageKnowledgeItemDTO dto = new MessageKnowledgeItemDTO();
        dto.setId(item.getId());
        dto.setTopicCode(item.getTopic());
        dto.setTopicName(humanizeTopic(item.getTopic()));
        dto.setTopicLabel(dto.getTopicName());
        dto.setStatus(toApiStatus(item.getStatus()));
        dto.setScopeType(item.getScopeType());
        dto.setScopeName(buildScopeName(item.getScopeType(), item.getRoomNumber(), item.getRoomTypeName()));
        dto.setQuestion(item.getQuestion());
        dto.setSourceQuestion(item.getQuestion());
        dto.setNormalizedQuestion(item.getNormalizedQuestion());
        dto.setAnswer(item.getAnswer());
        dto.setContent(item.getAnswer());
        dto.setSummary(item.getAnswer());
        dto.setKnowledgeText(item.getAnswer());
        dto.setLanguage(normalizeBlankToNull(item.getLanguage()));
        List<String> normalizedLanguages = normalizeLanguages(evidenceLanguages);
        dto.setEvidenceLanguages(normalizedLanguages);
        dto.setLanguageSummary(buildLanguageSummary(dto.getLanguage(), normalizedLanguages));
        dto.setEmbeddingStatus(normalizeBlankToNull(item.getEmbeddingStatus()));
        dto.setEmbeddingProvider(normalizeBlankToNull(item.getEmbeddingProvider()));
        dto.setEmbeddingModel(normalizeBlankToNull(item.getEmbeddingModel()));
        dto.setEmbeddingDimensions(item.getEmbeddingDimensions());
        dto.setEmbeddingUpdatedAt(toUtcOffset(item.getEmbeddingUpdatedAt()));
        dto.setEmbeddingError(normalizeBlankToNull(item.getEmbeddingError()));
        dto.setEvidenceCount(item.getEvidenceCount());
        dto.setConfidence(item.getConfidence());
        dto.setCreatedAt(toUtcOffset(item.getCreatedAt()));
        dto.setUpdatedAt(toUtcOffset(item.getUpdatedAt()));
        dto.setLastSeenAt(toUtcOffset(item.getLastSeenAt()));
        dto.setTitle(buildTitle(dto.getTopicName(), dto.getScopeName(), item.getQuestion()));
        return dto;
    }

    private static MessageKnowledgeEvidenceDTO toEvidenceDTO(MessageKnowledgeEvidence evidence) {
        MessageKnowledgeEvidenceDTO dto = new MessageKnowledgeEvidenceDTO();
        dto.setId(evidence.getId());
        dto.setSourceType("MESSAGE_PAIR");
        dto.setSourceTitle(buildEvidenceSourceTitle(evidence));
        dto.setGuestMessage(evidence.getQuestion());
        dto.setStaffMessage(evidence.getAnswer());
        dto.setMessageContent(evidence.getAnswer());
        dto.setSourceText(buildSourceText(evidence.getQuestion(), evidence.getAnswer()));
        dto.setContent(dto.getSourceText());
        dto.setChannelName(resolveChannelName(evidence.getChannelId()));
        dto.setTopicCode(evidence.getItem() == null ? null : evidence.getItem().getTopic());
        dto.setLanguage(normalizeBlankToNull(evidence.getLanguage()));
        dto.setOccurredAt(toUtcOffset(evidence.getSourceTimestamp()));
        dto.setCreatedAt(toUtcOffset(evidence.getCreatedAt()));
        dto.setConfidence(evidence.getConfidence());
        return dto;
    }

    private static List<String> languagesFromEvidence(List<MessageKnowledgeEvidenceDTO> evidence) {
        List<String> languages = new ArrayList<>();
        if (evidence == null || evidence.isEmpty()) {
            return languages;
        }
        for (MessageKnowledgeEvidenceDTO item : evidence) {
            if (item == null) {
                continue;
            }
            String language = normalizeBlankToNull(item.getLanguage());
            if (language != null) {
                languages.add(language);
            }
        }
        return languages;
    }

    private static List<String> normalizeLanguages(List<String> languages) {
        LinkedHashSet<String> uniqueLanguages = new LinkedHashSet<>();
        if (languages != null) {
            for (String language : languages) {
                String normalized = normalizeBlankToNull(language);
                if (normalized != null) {
                    uniqueLanguages.add(normalized);
                }
            }
        }
        return new ArrayList<>(uniqueLanguages);
    }

    private static String buildLanguageSummary(String itemLanguage, List<String> evidenceLanguages) {
        LinkedHashSet<String> languages = new LinkedHashSet<>();
        String normalizedItemLanguage = normalizeBlankToNull(itemLanguage);
        if (normalizedItemLanguage != null) {
            languages.add(normalizedItemLanguage);
        }
        if (evidenceLanguages != null) {
            languages.addAll(evidenceLanguages);
        }
        if (languages.isEmpty()) {
            return null;
        }
        return String.join(", ", languages);
    }

    private static String normalizeStatusFilter(String status) {
        String normalized = normalizeUppercase(status);
        if (normalized == null) {
            return null;
        }
        if (!ALLOWED_STATUS_FILTERS.contains(normalized)) {
            throw new IllegalArgumentException("不支持的知识状态筛选: " + status);
        }
        if (STATUS_CONFLICTED.equals(normalized)) {
            return STATUS_CONFLICT;
        }
        return normalized;
    }

    private static String toApiStatus(String status) {
        if (STATUS_CONFLICT.equalsIgnoreCase(nullToEmpty(status))) {
            return STATUS_CONFLICTED;
        }
        return status;
    }

    private static void ensureNotArchived(MessageKnowledgeItem item) {
        if (item != null && STATUS_ARCHIVED.equalsIgnoreCase(nullToEmpty(item.getStatus()))) {
            throw new IllegalStateException("已归档知识项不能再次变更为通过或拒绝");
        }
    }

    private static String buildTitle(String topicName, String scopeName, String question) {
        String topic = normalizeBlankToNull(topicName);
        String scope = normalizeBlankToNull(scopeName);
        if (topic != null && scope != null) {
            return topic + " - " + scope;
        }
        if (topic != null) {
            return topic;
        }
        String normalizedQuestion = normalizeBlankToNull(question);
        if (normalizedQuestion != null) {
            return normalizedQuestion;
        }
        return "知识项";
    }

    private static String buildScopeName(String scopeType, String roomNumber, String roomTypeName) {
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeType)) {
            String room = normalizeBlankToNull(roomNumber);
            if (room != null) {
                return "房间 " + room;
            }
            return "房间";
        }
        if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeType)) {
            String roomType = normalizeBlankToNull(roomTypeName);
            if (roomType != null) {
                return roomType;
            }
            return "房型";
        }
        if (SuMessagingThreadContext.SCOPE_STORE.equals(scopeType)) {
            return "门店通用";
        }
        return normalizeBlankToNull(scopeType);
    }

    private static String buildEvidenceSourceTitle(MessageKnowledgeEvidence evidence) {
        String channelName = resolveChannelName(evidence.getChannelId());
        String scopeName = buildScopeName(evidence.getScopeType(), evidence.getRoomNumber(), evidence.getRoomTypeName());
        if (channelName != null && scopeName != null) {
            return channelName + " - " + scopeName;
        }
        if (channelName != null) {
            return channelName;
        }
        if (scopeName != null) {
            return scopeName;
        }
        return "消息证据";
    }

    private static String buildSourceText(String question, String answer) {
        StringBuilder builder = new StringBuilder();
        String guest = normalizeBlankToNull(question);
        if (guest != null) {
            builder.append("Guest: ").append(guest);
        }
        String staff = normalizeBlankToNull(answer);
        if (staff != null) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("Staff: ").append(staff);
        }
        return builder.toString();
    }

    private static String humanizeTopic(String topic) {
        String normalized = normalizeBlankToNull(topic);
        if (normalized == null) {
            return "未分类";
        }
        String[] parts = normalized.replace('-', '_').split("_|:");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                builder.append(lower.substring(1));
            }
        }
        if (builder.length() == 0) {
            return normalized;
        }
        return builder.toString();
    }

    private static String resolveChannelName(Integer channelId) {
        if (channelId == null) {
            return null;
        }
        if (channelId == SuMessagingService.CHANNEL_AIRBNB) {
            return "Airbnb";
        }
        if (channelId == SuMessagingService.CHANNEL_BOOKING) {
            return "Booking.com";
        }
        return "Channel " + channelId;
    }

    private static String normalizeKeyword(String keyword) {
        String normalized = normalizeBlankToNull(keyword);
        if (normalized == null) {
            return null;
        }
        String escaped = normalized.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static String normalizeTopic(String topicCode) {
        String normalized = normalizeBlankToNull(topicCode);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeUppercase(String value) {
        String normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private static void requireStoreId(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("Store context is required");
        }
    }

    private static String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static OffsetDateTime toUtcOffset(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return UtcTimeUtil.toUtcOffset(value);
    }
}
