package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;
import server.demo.dto.SuMessagingTranslationRequest;
import server.demo.dto.SuMessagingTranslationResponse;
import server.demo.entity.MessageKnowledgeEntry;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.entity.SuMessageTranslation;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuMessageTranslationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingTranslationServiceTest {
    private static final Long STORE_ID = 26L;
    private static final Long THREAD_ID = 77L;
    private static final Long MESSAGE_ID = 201L;

    @Test
    void getOrCreateTranslation_shouldReturnCachedSuccessWithoutCallingAi() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();
        SuMessage message = newMessage(thread, "Hello guest");
        String sourceHash = SuMessagingTranslationService.hashSourceContent("Hello guest");
        SuMessageTranslation existing = newTranslation(thread, message, "zh-CN", sourceHash, "你好");

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        STORE_ID,
                        MESSAGE_ID,
                        "zh-CN",
                        sourceHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )).thenReturn(Optional.of(existing));

        SuMessagingTranslationResponse response = fixture.service.getOrCreateTranslation(
                STORE_ID,
                THREAD_ID,
                MESSAGE_ID,
                newRequest("zh-CN")
        );

        assertTrue(response.isCached());
        assertEquals(MESSAGE_ID, response.getMessageId());
        assertEquals("zh-CN", response.getTargetLanguage());
        assertEquals("你好", response.getTranslatedContent());
        assertEquals(sourceHash, response.getSourceContentHash());
        assertEquals(SuMessageTranslation.STATUS_SUCCESS, response.getStatus());
        verify(fixture.aiTranslationService, never()).translate(any(), any());
        verify(fixture.knowledgeSearchService, never()).searchSimilar(any(), any(), any(), any(), anyInt());
        verify(fixture.translationRepository, never()).save(any(SuMessageTranslation.class));
    }

    @Test
    void getOrCreateTranslation_shouldTranslateSaveAndReturnWhenCacheMisses() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();
        SuMessage message = newMessage(thread, "Please upload passport");
        String sourceHash = SuMessagingTranslationService.hashSourceContent("Please upload passport");

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        STORE_ID,
                        MESSAGE_ID,
                        "ja",
                        sourceHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )).thenReturn(Optional.empty());
        when(fixture.aiTranslationService.translate(eq("Please upload passport"), any(RegistrationTargetLanguage.class)))
                .thenReturn(AiTranslationResult.translated(
                        "パスポートをアップロードしてください",
                        RegistrationTargetLanguage.resolved("ja", "Japanese", "test")
                ));
        when(fixture.translationRepository.save(any(SuMessageTranslation.class))).thenAnswer(invocation -> {
            SuMessageTranslation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        SuMessagingTranslationResponse response = fixture.service.getOrCreateTranslation(
                STORE_ID,
                THREAD_ID,
                MESSAGE_ID,
                newRequest("ja")
        );

        assertFalse(response.isCached());
        assertEquals("ja", response.getTargetLanguage());
        assertEquals("パスポートをアップロードしてください", response.getTranslatedContent());
        assertEquals(sourceHash, response.getSourceContentHash());

        ArgumentCaptor<RegistrationTargetLanguage> languageCaptor =
                ArgumentCaptor.forClass(RegistrationTargetLanguage.class);
        verify(fixture.aiTranslationService).translate(eq("Please upload passport"), languageCaptor.capture());
        assertEquals("ja", languageCaptor.getValue().getCode());
        assertEquals("Japanese", languageCaptor.getValue().getName());

        ArgumentCaptor<SuMessageTranslation> savedCaptor = ArgumentCaptor.forClass(SuMessageTranslation.class);
        verify(fixture.translationRepository).save(savedCaptor.capture());
        SuMessageTranslation saved = savedCaptor.getValue();
        assertEquals(STORE_ID, saved.getStoreId());
        assertEquals(thread, saved.getThread());
        assertEquals(message, saved.getMessage());
        assertEquals("ja", saved.getTargetLanguage());
        assertEquals(sourceHash, saved.getSourceContentHash());
        assertEquals(SuMessageTranslation.STATUS_SUCCESS, saved.getTranslationStatus());
    }

    @Test
    void getOrCreateTranslation_shouldUseKnowledgeGuidanceOnCacheMissWithoutChangingHash() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();
        SuMessage message = newMessage(thread, "Is late checkout possible?");
        String sourceHash = SuMessagingTranslationService.hashSourceContent("Is late checkout possible?");
        MessageKnowledgeEntry entry = new MessageKnowledgeEntry();
        entry.setAnswer("Late checkout is available until 12:00 when available.");
        MessageKnowledgeMatch match = new MessageKnowledgeMatch(
                entry,
                0.8,
                SuMessagingThreadContext.SCOPE_STORE,
                List.of("REFINED_FACT"),
                List.of(MessageKnowledgeTopicService.TOPIC_LATE_CHECKOUT),
                List.of("Late checkout is available until 12:00 when available.")
        );

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        STORE_ID,
                        MESSAGE_ID,
                        "ja",
                        sourceHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )).thenReturn(Optional.empty());
        when(fixture.contextResolver.resolveForIndex(STORE_ID, thread)).thenReturn(new SuMessagingThreadContext());
        when(fixture.knowledgeSearchService.searchSimilar(
                eq(STORE_ID),
                eq(THREAD_ID),
                any(SuMessagingThreadContext.class),
                eq("Is late checkout possible?"),
                eq(2)
        )).thenReturn(new MessageKnowledgeSearchResult(
                MessageKnowledgeSearchService.STATUS_MATCHED,
                List.of(match),
                List.of()
        ));
        when(fixture.aiTranslationService.translate(eq("Is late checkout possible?"), any(RegistrationTargetLanguage.class)))
                .thenReturn(AiTranslationResult.translated(
                        "レイトチェックアウトは可能ですか？",
                        RegistrationTargetLanguage.resolved("ja", "Japanese", "test")
                ));
        when(fixture.translationRepository.save(any(SuMessageTranslation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuMessagingTranslationResponse response = fixture.service.getOrCreateTranslation(
                STORE_ID,
                THREAD_ID,
                MESSAGE_ID,
                newRequest("ja")
        );

        assertFalse(response.isCached());
        assertEquals(sourceHash, response.getSourceContentHash());
        ArgumentCaptor<RegistrationTargetLanguage> languageCaptor =
                ArgumentCaptor.forClass(RegistrationTargetLanguage.class);
        verify(fixture.aiTranslationService).translate(eq("Is late checkout possible?"), languageCaptor.capture());
        assertEquals("ja", languageCaptor.getValue().getCode());
        assertTrue(languageCaptor.getValue().getName().contains("Late checkout is available until 12:00"));
    }

    @Test
    void getOrCreateTranslation_shouldNotReuseOldTranslationWhenSourceHashChanges() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();
        SuMessage message = newMessage(thread, "New text");
        String oldHash = SuMessagingTranslationService.hashSourceContent("Old text");
        String newHash = SuMessagingTranslationService.hashSourceContent("New text");
        SuMessageTranslation oldTranslation = newTranslation(thread, message, "ko", oldHash, "이전 번역");

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        STORE_ID,
                        MESSAGE_ID,
                        "ko",
                        oldHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )).thenReturn(Optional.of(oldTranslation));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        STORE_ID,
                        MESSAGE_ID,
                        "ko",
                        newHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )).thenReturn(Optional.empty());
        when(fixture.aiTranslationService.translate(eq("New text"), any(RegistrationTargetLanguage.class)))
                .thenReturn(AiTranslationResult.translated(
                        "새 텍스트",
                        RegistrationTargetLanguage.resolved("ko", "Korean", "test")
                ));
        when(fixture.translationRepository.save(any(SuMessageTranslation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuMessagingTranslationResponse response = fixture.service.getOrCreateTranslation(
                STORE_ID,
                THREAD_ID,
                MESSAGE_ID,
                newRequest("ko")
        );

        assertFalse(response.isCached());
        assertEquals(newHash, response.getSourceContentHash());
        assertEquals("새 텍스트", response.getTranslatedContent());
        verify(fixture.aiTranslationService).translate(eq("New text"), any(RegistrationTargetLanguage.class));
    }

    @Test
    void getOrCreateTranslation_shouldKeepTargetLanguagesSeparate() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();
        SuMessage message = newMessage(thread, "Late checkout is possible.");
        String sourceHash = SuMessagingTranslationService.hashSourceContent("Late checkout is possible.");

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        eq(STORE_ID),
                        eq(MESSAGE_ID),
                        any(String.class),
                        eq(sourceHash),
                        eq(SuMessageTranslation.STATUS_SUCCESS)
                )).thenReturn(Optional.empty());
        when(fixture.aiTranslationService.translate(eq("Late checkout is possible."), any(RegistrationTargetLanguage.class)))
                .thenAnswer(invocation -> {
                    RegistrationTargetLanguage language = invocation.getArgument(1);
                    if ("ja".equals(language.getCode())) {
                        return AiTranslationResult.translated("レイトチェックアウト可能です。", language);
                    }
                    return AiTranslationResult.translated("늦은 체크아웃이 가능합니다.", language);
                });
        when(fixture.translationRepository.save(any(SuMessageTranslation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuMessagingTranslationResponse japanese = fixture.service.getOrCreateTranslation(
                STORE_ID,
                THREAD_ID,
                MESSAGE_ID,
                newRequest("ja")
        );
        SuMessagingTranslationResponse korean = fixture.service.getOrCreateTranslation(
                STORE_ID,
                THREAD_ID,
                MESSAGE_ID,
                newRequest("ko")
        );

        assertEquals("ja", japanese.getTargetLanguage());
        assertEquals("ko", korean.getTargetLanguage());
        assertEquals("レイトチェックアウト可能です。", japanese.getTranslatedContent());
        assertEquals("늦은 체크아웃이 가능합니다.", korean.getTranslatedContent());

        ArgumentCaptor<SuMessageTranslation> savedCaptor = ArgumentCaptor.forClass(SuMessageTranslation.class);
        verify(fixture.translationRepository, Mockito.times(2)).save(savedCaptor.capture());
        List<SuMessageTranslation> savedTranslations = savedCaptor.getAllValues();
        assertEquals("ja", savedTranslations.get(0).getTargetLanguage());
        assertEquals("ko", savedTranslations.get(1).getTargetLanguage());
    }

    @Test
    void getOrCreateTranslation_shouldRejectBlankAiTranslationWithoutSaving() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();
        SuMessage message = newMessage(thread, "Please upload passport");
        String sourceHash = SuMessagingTranslationService.hashSourceContent("Please upload passport");

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        STORE_ID,
                        MESSAGE_ID,
                        "zh-CN",
                        sourceHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )).thenReturn(Optional.empty());
        when(fixture.aiTranslationService.translate(eq("Please upload passport"), any(RegistrationTargetLanguage.class)))
                .thenReturn(AiTranslationResult.translated(
                        "   ",
                        RegistrationTargetLanguage.resolved("zh-CN", "Simplified Chinese", "test")
                ));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> fixture.service.getOrCreateTranslation(
                        STORE_ID,
                        THREAD_ID,
                        MESSAGE_ID,
                        newRequest("zh-CN")
                )
        );

        assertEquals("TRANSLATION_BLANK_RESULT", ex.getMessage());
        verify(fixture.translationRepository, never()).save(any(SuMessageTranslation.class));
    }

    @Test
    void getOrCreateTranslation_shouldPropagateSaveFailureWhenNoExistingSuccessCanBeRecovered() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();
        SuMessage message = newMessage(thread, "Please upload passport");
        String sourceHash = SuMessagingTranslationService.hashSourceContent("Please upload passport");

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(fixture.translationRepository
                .findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
                        STORE_ID,
                        MESSAGE_ID,
                        "zh-CN",
                        sourceHash,
                        SuMessageTranslation.STATUS_SUCCESS
                )).thenReturn(Optional.empty());
        when(fixture.aiTranslationService.translate(eq("Please upload passport"), any(RegistrationTargetLanguage.class)))
                .thenReturn(AiTranslationResult.translated(
                        "请上传护照",
                        RegistrationTargetLanguage.resolved("zh-CN", "Simplified Chinese", "test")
                ));
        when(fixture.translationRepository.save(any(SuMessageTranslation.class)))
                .thenThrow(new DataIntegrityViolationException("schema unavailable"));

        DataIntegrityViolationException ex = assertThrows(
                DataIntegrityViolationException.class,
                () -> fixture.service.getOrCreateTranslation(
                        STORE_ID,
                        THREAD_ID,
                        MESSAGE_ID,
                        newRequest("zh-CN")
                )
        );

        assertEquals("schema unavailable", ex.getMessage());
        verify(fixture.translationRepository).save(any(SuMessageTranslation.class));
    }

    @Test
    void chatGptTranslationService_shouldAcceptPlainTextTranslationReply() {
        ChatService chatService = Mockito.mock(ChatService.class);
        ChatGptTranslationService service = new ChatGptTranslationService(chatService);
        RegistrationTargetLanguage targetLanguage =
                RegistrationTargetLanguage.resolved("ja", "Japanese", "test");

        when(chatService.processMessage(any(ChatMessageRequest.class)))
                .thenReturn(ChatMessageResponse.success("パスポートをアップロードしてください", null));

        AiTranslationResult result = service.translate("Please upload passport", targetLanguage);

        assertTrue(result.isTranslated());
        assertEquals("パスポートをアップロードしてください", result.getTranslatedText());
        assertEquals("ja", result.getTargetLanguage().getCode());
        verify(chatService).processMessage(any(ChatMessageRequest.class));
    }

    @Test
    void getOrCreateTranslation_shouldRejectMessageOutsideThreadWithoutCallingAi() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread();

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.of(thread));
        when(fixture.messageRepository.findByStoreIdAndThreadIdAndId(STORE_ID, THREAD_ID, MESSAGE_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.getOrCreateTranslation(
                        STORE_ID,
                        THREAD_ID,
                        MESSAGE_ID,
                        newRequest("zh-CN")
                )
        );

        assertEquals("Message not found or no permission", ex.getMessage());
        verify(fixture.aiTranslationService, never()).translate(any(), any());
        verify(fixture.translationRepository, never()).save(any(SuMessageTranslation.class));
    }

    @Test
    void getOrCreateTranslation_shouldRejectThreadOutsideStoreWithoutCallingAi() {
        TestFixture fixture = new TestFixture();

        when(fixture.threadRepository.findByStoreIdAndId(STORE_ID, THREAD_ID)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.getOrCreateTranslation(
                        STORE_ID,
                        THREAD_ID,
                        MESSAGE_ID,
                        newRequest("zh-CN")
                )
        );

        assertEquals("Thread not found or no permission", ex.getMessage());
        verify(fixture.messageRepository, never()).findByStoreIdAndThreadIdAndId(any(), any(), any());
        verify(fixture.aiTranslationService, never()).translate(any(), any());
    }

    private static SuMessagingTranslationRequest newRequest(String targetLanguage) {
        SuMessagingTranslationRequest request = new SuMessagingTranslationRequest();
        request.setTargetLanguage(targetLanguage);
        return request;
    }

    private static SuMessageThread newThread() {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(THREAD_ID);
        thread.setStoreId(STORE_ID);
        thread.setSuHotelId("HOTEL26");
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setThreadKey("T1");
        thread.setThreadId("T1");
        return thread;
    }

    private static SuMessage newMessage(SuMessageThread thread, String content) {
        SuMessage message = new SuMessage();
        message.setId(MESSAGE_ID);
        message.setStoreId(thread.getStoreId());
        message.setThread(thread);
        message.setSenderType(SuMessagingSenderType.GUEST);
        message.setContent(content);
        message.setSentAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        message.setDeliveryStatus("SENT");
        return message;
    }

    private static SuMessageTranslation newTranslation(
            SuMessageThread thread,
            SuMessage message,
            String targetLanguage,
            String sourceHash,
            String translatedContent
    ) {
        SuMessageTranslation translation = new SuMessageTranslation();
        translation.setId(1L);
        translation.setStoreId(STORE_ID);
        translation.setThread(thread);
        translation.setMessage(message);
        translation.setTargetLanguage(targetLanguage);
        translation.setSourceContentHash(sourceHash);
        translation.setTranslatedContent(translatedContent);
        translation.setTranslationStatus(SuMessageTranslation.STATUS_SUCCESS);
        translation.setTranslatedAt(LocalDateTime.of(2026, 6, 10, 12, 30));
        return translation;
    }

    private static class TestFixture {
        private final SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        private final SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        private final SuMessageTranslationRepository translationRepository =
                Mockito.mock(SuMessageTranslationRepository.class);
        private final AiTranslationService aiTranslationService = Mockito.mock(AiTranslationService.class);
        private final MessageKnowledgeSearchService knowledgeSearchService =
                Mockito.mock(MessageKnowledgeSearchService.class);
        private final SuMessagingThreadContextResolver contextResolver =
                Mockito.mock(SuMessagingThreadContextResolver.class);
        private final SuMessagingTranslationService service;

        private TestFixture() {
            service = new SuMessagingTranslationService(
                    threadRepository,
                    messageRepository,
                    translationRepository,
                    aiTranslationService
            );
            service.setKnowledgeSearchService(knowledgeSearchService);
            service.setContextResolver(contextResolver);
        }
    }
}
