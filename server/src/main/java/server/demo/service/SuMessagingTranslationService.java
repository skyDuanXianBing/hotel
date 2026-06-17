package server.demo.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.dto.SuMessagingTranslationRequest;
import server.demo.dto.SuMessagingTranslationResponse;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.entity.SuMessageTranslation;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuMessageTranslationRepository;
import server.demo.util.UtcTimeUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SuMessagingTranslationService {
    private static final Logger logger = LoggerFactory.getLogger(SuMessagingTranslationService.class);

    private static final String LANGUAGE_SOURCE = "suMessagingTranslationRequest";
    private static final int TRANSLATION_KNOWLEDGE_LIMIT = 2;
    private static final int MAX_TRANSLATION_FACTS = 3;
    private static final int MAX_TRANSLATION_FACT_LENGTH = 180;
    private static final int MAX_TRANSLATION_GUIDANCE_LENGTH = 600;
    private static final Map<String, String> CANONICAL_LANGUAGE_CODES = Map.ofEntries(
            Map.entry("en", "en"),
            Map.entry("ja", "ja"),
            Map.entry("ko", "ko"),
            Map.entry("zh", "zh-CN"),
            Map.entry("zh-cn", "zh-CN"),
            Map.entry("zh-hans", "zh-CN"),
            Map.entry("zh-tw", "zh-TW"),
            Map.entry("zh-hant", "zh-TW"),
            Map.entry("th", "th"),
            Map.entry("vi", "vi"),
            Map.entry("id", "id"),
            Map.entry("fr", "fr"),
            Map.entry("es", "es"),
            Map.entry("de", "de"),
            Map.entry("it", "it"),
            Map.entry("ru", "ru"),
            Map.entry("ar", "ar")
    );
    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("ja", "Japanese"),
            Map.entry("ko", "Korean"),
            Map.entry("zh-CN", "Simplified Chinese"),
            Map.entry("zh-TW", "Traditional Chinese"),
            Map.entry("th", "Thai"),
            Map.entry("vi", "Vietnamese"),
            Map.entry("id", "Indonesian"),
            Map.entry("fr", "French"),
            Map.entry("es", "Spanish"),
            Map.entry("de", "German"),
            Map.entry("it", "Italian"),
            Map.entry("ru", "Russian"),
            Map.entry("ar", "Arabic")
    );

    private final SuMessageThreadRepository threadRepository;
    private final SuMessageRepository messageRepository;
    private final SuMessageTranslationRepository translationRepository;
    private final AiTranslationService aiTranslationService;
    private MessageKnowledgeSearchService knowledgeSearchService;
    private SuMessagingThreadContextResolver contextResolver;

    public SuMessagingTranslationService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            SuMessageTranslationRepository translationRepository,
            AiTranslationService aiTranslationService
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.translationRepository = translationRepository;
        this.aiTranslationService = aiTranslationService;
    }

    @Autowired(required = false)
    public void setKnowledgeSearchService(MessageKnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @Autowired(required = false)
    public void setContextResolver(SuMessagingThreadContextResolver contextResolver) {
        this.contextResolver = contextResolver;
    }

    public SuMessagingTranslationResponse getOrCreateTranslation(
            Long storeId,
            Long threadId,
            Long messageId,
            SuMessagingTranslationRequest request
    ) {
        if (storeId == null) {
            throw new IllegalArgumentException("Store context is required");
        }
        RegistrationTargetLanguage targetLanguage = resolveTargetLanguage(request);

        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found or no permission"));
        SuMessage message = messageRepository.findByStoreIdAndThreadIdAndId(storeId, thread.getId(), messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found or no permission"));

        String sourceContent = message.getContent();
        if (sourceContent == null || sourceContent.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        String sourceContentHash = hashSourceContent(sourceContent);
        SuMessageTranslation existing = findSuccessTranslation(
                storeId,
                messageId,
                targetLanguage.getCode(),
                sourceContentHash
        );
        if (existing != null) {
            return toResponse(existing, messageId, true);
        }

        RegistrationTargetLanguage translationLanguage = withKnowledgeGuidance(
                storeId,
                thread,
                sourceContent,
                targetLanguage
        );
        AiTranslationResult result = aiTranslationService.translate(sourceContent, translationLanguage);
        if (result == null || !result.isTranslated()) {
            String errorMessage = result == null ? "TRANSLATION_FAILED" : result.getErrorMessage();
            throw new IllegalStateException(normalizeTranslationFailure(errorMessage));
        }

        String translatedText = result.getTranslatedText();
        if (translatedText == null || translatedText.isBlank()) {
            throw new IllegalStateException("TRANSLATION_BLANK_RESULT");
        }

        SuMessageTranslation saved = saveSuccessTranslation(
                storeId,
                thread,
                message,
                targetLanguage.getCode(),
                sourceContentHash,
                translatedText
        );
        return toResponse(saved, messageId, false);
    }

    private RegistrationTargetLanguage withKnowledgeGuidance(
            Long storeId,
            SuMessageThread thread,
            String sourceContent,
            RegistrationTargetLanguage targetLanguage
    ) {
        if (knowledgeSearchService == null || contextResolver == null || sourceContent == null || sourceContent.isBlank()) {
            return targetLanguage;
        }

        try {
            SuMessagingThreadContext context = contextResolver.resolveForIndex(storeId, thread);
            MessageKnowledgeSearchResult searchResult = knowledgeSearchService.searchSimilar(
                    storeId,
                    thread.getId(),
                    context,
                    sourceContent,
                    TRANSLATION_KNOWLEDGE_LIMIT
            );
            List<String> facts = collectTranslationFacts(searchResult);
            if (facts.isEmpty()) {
                return targetLanguage;
            }
            String guidance = buildTranslationGuidance(facts);
            String augmentedName = targetLanguage.getName() + ". " + guidance;
            return RegistrationTargetLanguage.resolved(
                    targetLanguage.getCode(),
                    augmentedName,
                    targetLanguage.getSource()
            );
        } catch (Exception e) {
            logger.warn(
                    "Failed to load translation knowledge guidance. storeId={}, threadId={}, error={}",
                    storeId,
                    thread == null ? null : thread.getId(),
                    e.getMessage(),
                    e
            );
            return targetLanguage;
        }
    }

    private static List<String> collectTranslationFacts(MessageKnowledgeSearchResult searchResult) {
        List<String> facts = new ArrayList<>();
        if (searchResult == null || searchResult.getMatches() == null || searchResult.getMatches().isEmpty()) {
            return facts;
        }

        Set<String> seenFacts = new LinkedHashSet<>();
        for (MessageKnowledgeMatch match : searchResult.getMatches()) {
            if (facts.size() >= MAX_TRANSLATION_FACTS) {
                return facts;
            }
            if (match == null) {
                continue;
            }
            List<String> reusableFacts = match.getReusableFacts();
            if (reusableFacts != null && !reusableFacts.isEmpty()) {
                addTranslationFacts(facts, seenFacts, reusableFacts);
                continue;
            }
            if (match.getEntry() != null) {
                addTranslationFacts(facts, seenFacts, List.of(match.getEntry().getAnswer()));
            }
        }
        return facts;
    }

    private static void addTranslationFacts(
            List<String> facts,
            Set<String> seenFacts,
            List<String> candidates
    ) {
        for (String candidate : candidates) {
            if (facts.size() >= MAX_TRANSLATION_FACTS) {
                return;
            }
            String fact = normalizeTranslationFact(candidate);
            if (fact.isBlank()) {
                continue;
            }
            String key = fact.toLowerCase(Locale.ROOT);
            if (seenFacts.add(key)) {
                facts.add(fact);
            }
        }
    }

    private static String buildTranslationGuidance(List<String> facts) {
        StringBuilder builder = new StringBuilder();
        builder.append("Use these hotel facts only as terminology and local context while translating. ");
        builder.append("Do not answer the guest and do not add facts that are not in the source text: ");
        for (int index = 0; index < facts.size(); index++) {
            if (index > 0) {
                builder.append(" | ");
            }
            builder.append(facts.get(index));
        }
        String guidance = builder.toString();
        if (guidance.length() > MAX_TRANSLATION_GUIDANCE_LENGTH) {
            return guidance.substring(0, MAX_TRANSLATION_GUIDANCE_LENGTH).trim();
        }
        return guidance;
    }

    private static String normalizeTranslationFact(String value) {
        if (value == null) {
            return "";
        }
        String fact = value.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (fact.length() > MAX_TRANSLATION_FACT_LENGTH) {
            return fact.substring(0, MAX_TRANSLATION_FACT_LENGTH).trim();
        }
        return fact;
    }

    private SuMessageTranslation findSuccessTranslation(
            Long storeId,
            Long messageId,
            String targetLanguage,
            String sourceContentHash
    ) {
        return translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        storeId,
                        messageId,
                        targetLanguage,
                        sourceContentHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )
                .orElse(null);
    }

    private SuMessageTranslation saveSuccessTranslation(
            Long storeId,
            SuMessageThread thread,
            SuMessage message,
            String targetLanguage,
            String sourceContentHash,
            String translatedText
    ) {
        SuMessageTranslation translation = new SuMessageTranslation();
        translation.setStoreId(storeId);
        translation.setThread(thread);
        translation.setMessage(message);
        translation.setTargetLanguage(targetLanguage);
        translation.setSourceContentHash(sourceContentHash);
        translation.setTranslatedContent(translatedText);
        translation.setTranslationStatus(SuMessageTranslation.STATUS_SUCCESS);
        translation.setTranslatedAt(UtcTimeUtil.nowLocalDateTime());

        try {
            return translationRepository.save(translation);
        } catch (DataIntegrityViolationException ex) {
            SuMessageTranslation existing = findSuccessTranslation(
                    storeId,
                    message.getId(),
                    targetLanguage,
                    sourceContentHash
            );
            if (existing != null) {
                return existing;
            }
            throw ex;
        }
    }

    private static SuMessagingTranslationResponse toResponse(
            SuMessageTranslation translation,
            Long messageId,
            boolean cached
    ) {
        return new SuMessagingTranslationResponse(
                messageId,
                translation.getTargetLanguage(),
                translation.getTranslatedContent(),
                translation.getSourceContentHash(),
                translation.getTranslationStatus(),
                cached,
                UtcTimeUtil.toUtcOffset(translation.getTranslatedAt())
        );
    }

    private static RegistrationTargetLanguage resolveTargetLanguage(SuMessagingTranslationRequest request) {
        if (request == null || request.getTargetLanguage() == null || request.getTargetLanguage().isBlank()) {
            throw new IllegalArgumentException("目标语言不能为空");
        }
        String targetLanguage = request.getTargetLanguage().trim();
        String normalized = targetLanguage.toLowerCase(Locale.ROOT).replace('_', '-');
        String canonicalCode = CANONICAL_LANGUAGE_CODES.get(normalized);
        if (canonicalCode == null) {
            throw new IllegalArgumentException("Unsupported targetLanguage: " + targetLanguage);
        }
        String languageName = LANGUAGE_NAMES.get(canonicalCode);
        return RegistrationTargetLanguage.resolved(canonicalCode, languageName, LANGUAGE_SOURCE);
    }

    static String hashSourceContent(String sourceContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(sourceContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String normalizeTranslationFailure(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "TRANSLATION_FAILED";
        }
        return errorMessage;
    }
}
