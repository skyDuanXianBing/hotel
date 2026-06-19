package server.demo.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import server.demo.dto.SuMessagingAiReplyDraftRequest;
import server.demo.dto.SuMessagingAiReplyDraftResponse;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingAiReplyDraftServiceTest {

    @Test
    void generateDraft_shouldNotDependOnLegacyIndexer() {
        boolean hasIndexerDependency = Arrays.stream(SuMessagingAiReplyDraftService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().getSimpleName().contains("KnowledgeIndex"));

        assertFalse(hasIndexerDependency);
    }

    @Test
    void generateDraft_shouldReturnNoMatchDraftAndNotCreateStaffMessage() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage latestGuest = newMessage(201L, thread, SuMessagingSenderType.GUEST, "Can I check in early?");

        when(fixture.threadRepository.findByStoreIdAndId(26L, 77L)).thenReturn(Optional.of(thread));
        when(fixture.contextResolver.resolve(eq(26L), eq(thread), any())).thenReturn(newContext());
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdAndSenderTypeDesc(
                eq(26L),
                eq(77L),
                eq(SuMessagingSenderType.GUEST),
                any(Pageable.class)
        )).thenReturn(List.of(latestGuest));
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdDesc(
                eq(26L),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(latestGuest));
        when(fixture.searchService.searchSimilar(eq(26L), eq(77L), any(), eq("Can I check in early?"), eq(3)))
                .thenReturn(new MessageKnowledgeSearchResult(
                        MessageKnowledgeSearchService.STATUS_NO_MATCH,
                        List.of(),
                        List.of(MessageKnowledgeSearchService.WARNING_NO_SIMILAR_HISTORY)
                ));
        when(fixture.chatLanguageModel.generate(any(String.class)))
                .thenReturn("We can confirm early check-in availability on the arrival day.");

        SuMessagingAiReplyDraftResponse response = fixture.service.generateDraft(
                26L,
                77L,
                new SuMessagingAiReplyDraftRequest()
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_NO_MATCH, response.getRetrievalStatus());
        assertEquals("We can confirm early check-in availability on the arrival day.", response.getDraftReply());
        assertTrue(response.getWarnings().contains(MessageKnowledgeSearchService.WARNING_NO_SIMILAR_HISTORY));
        verify(fixture.messageRepository, never()).save(any(SuMessage.class));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.chatLanguageModel).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Last guest turn to reply to:"));
        assertTrue(prompt.contains("Can I check in early?"));
        assertTrue(prompt.contains("- 客户：Can I check in early?"));
    }

    @Test
    void generateDraft_shouldUseLastGuestTurnWhenLatestGuestOnlyThanks() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage staffGreeting = newMessage(200L, thread, SuMessagingSenderType.STAFF, "Welcome to the hotel.");
        SuMessage toiletPaperQuestion = newMessage(
                201L,
                thread,
                SuMessagingSenderType.GUEST,
                "Could you bring toilet paper to room 301?"
        );
        SuMessage thanks = newMessage(202L, thread, SuMessagingSenderType.GUEST, "Thank you");
        String expectedGuestTurn = "Could you bring toilet paper to room 301?\nThank you";

        when(fixture.threadRepository.findByStoreIdAndId(26L, 77L)).thenReturn(Optional.of(thread));
        when(fixture.contextResolver.resolve(eq(26L), eq(thread), any())).thenReturn(newContext());
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdDesc(
                eq(26L),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(thanks, toiletPaperQuestion, staffGreeting));
        when(fixture.searchService.searchSimilar(eq(26L), eq(77L), any(), any(String.class), eq(3)))
                .thenReturn(new MessageKnowledgeSearchResult(
                        MessageKnowledgeSearchService.STATUS_NO_MATCH,
                        List.of(),
                        List.of()
                ));
        when(fixture.chatLanguageModel.generate(any(String.class)))
                .thenReturn("Sure, we will bring toilet paper to room 301.");

        SuMessagingAiReplyDraftResponse response = fixture.service.generateDraft(
                26L,
                77L,
                new SuMessagingAiReplyDraftRequest()
        );

        assertEquals("Sure, we will bring toilet paper to room 301.", response.getDraftReply());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.searchService).searchSimilar(eq(26L), eq(77L), any(), queryCaptor.capture(), eq(3));
        assertEquals(expectedGuestTurn, queryCaptor.getValue());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.chatLanguageModel).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        String targetSection = sectionBetween(prompt, "Last guest turn to reply to:", "Recent conversation:");
        assertTrue(targetSection.contains("Could you bring toilet paper to room 301?"));
        assertTrue(targetSection.contains("Thank you"));
        assertTrue(prompt.contains("Do not answer only the final courtesy message"));
        assertTrue(prompt.contains("- 员工：Welcome to the hotel."));
        assertTrue(prompt.contains("- 客户：Could you bring toilet paper to room 301?"));
        assertTrue(prompt.contains("- 客户：Thank you"));
    }

    @Test
    void generateDraft_shouldMergeContinuousGuestQuestions() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage staffGreeting = newMessage(200L, thread, SuMessagingSenderType.STAFF, "Welcome to the hotel.");
        SuMessage wifiQuestion = newMessage(201L, thread, SuMessagingSenderType.GUEST, "What is the Wi-Fi password?");
        SuMessage towelsQuestion = newMessage(
                202L,
                thread,
                SuMessagingSenderType.GUEST,
                "Also, can we get extra towels?"
        );
        String expectedGuestTurn = "What is the Wi-Fi password?\nAlso, can we get extra towels?";

        when(fixture.threadRepository.findByStoreIdAndId(26L, 77L)).thenReturn(Optional.of(thread));
        when(fixture.contextResolver.resolve(eq(26L), eq(thread), any())).thenReturn(newContext());
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdDesc(
                eq(26L),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(towelsQuestion, wifiQuestion, staffGreeting));
        when(fixture.searchService.searchSimilar(eq(26L), eq(77L), any(), any(String.class), eq(3)))
                .thenReturn(new MessageKnowledgeSearchResult(
                        MessageKnowledgeSearchService.STATUS_NO_MATCH,
                        List.of(),
                        List.of()
                ));
        when(fixture.chatLanguageModel.generate(any(String.class)))
                .thenReturn("The Wi-Fi password is at the front desk, and we will bring extra towels.");

        fixture.service.generateDraft(26L, 77L, new SuMessagingAiReplyDraftRequest());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.searchService).searchSimilar(eq(26L), eq(77L), any(), queryCaptor.capture(), eq(3));
        assertEquals(expectedGuestTurn, queryCaptor.getValue());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.chatLanguageModel).generate(promptCaptor.capture());
        String targetSection = sectionBetween(promptCaptor.getValue(), "Last guest turn to reply to:", "Recent conversation:");
        assertTrue(targetSection.contains("What is the Wi-Fi password?"));
        assertTrue(targetSection.contains("Also, can we get extra towels?"));
    }

    @Test
    void generateDraft_shouldOnlyUseGuestTurnAfterLatestStaffReply() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage oldQuestion = newMessage(200L, thread, SuMessagingSenderType.GUEST, "What time is checkout?");
        SuMessage staffReply = newMessage(201L, thread, SuMessagingSenderType.STAFF, "Checkout is at 11:00.");
        SuMessage newQuestion = newMessage(
                202L,
                thread,
                SuMessagingSenderType.GUEST,
                "Can we leave bags after checkout?"
        );

        when(fixture.threadRepository.findByStoreIdAndId(26L, 77L)).thenReturn(Optional.of(thread));
        when(fixture.contextResolver.resolve(eq(26L), eq(thread), any())).thenReturn(newContext());
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdDesc(
                eq(26L),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(newQuestion, staffReply, oldQuestion));
        when(fixture.searchService.searchSimilar(eq(26L), eq(77L), any(), any(String.class), eq(3)))
                .thenReturn(new MessageKnowledgeSearchResult(
                        MessageKnowledgeSearchService.STATUS_NO_MATCH,
                        List.of(),
                        List.of()
                ));
        when(fixture.chatLanguageModel.generate(any(String.class)))
                .thenReturn("You can leave your bags with us after checkout.");

        fixture.service.generateDraft(26L, 77L, new SuMessagingAiReplyDraftRequest());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.searchService).searchSimilar(eq(26L), eq(77L), any(), queryCaptor.capture(), eq(3));
        assertEquals("Can we leave bags after checkout?", queryCaptor.getValue());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.chatLanguageModel).generate(promptCaptor.capture());
        String targetSection = sectionBetween(promptCaptor.getValue(), "Last guest turn to reply to:", "Recent conversation:");
        assertTrue(targetSection.contains("Can we leave bags after checkout?"));
        assertFalse(targetSection.contains("What time is checkout?"));
    }

    @Test
    void generateDraft_shouldIncludeMatchedHistoryInPromptWithoutRawJson() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage latestGuest = newMessage(201L, thread, SuMessagingSenderType.GUEST, "Is late checkout possible?");
        latestGuest.setRawJson("{\"secret\":\"DO_NOT_LEAK\"}");

        MessageKnowledgeCandidate candidate = new MessageKnowledgeCandidate();
        candidate.setId(1L);
        candidate.setStoreId(26L);
        candidate.setThreadId(11L);
        candidate.setQuestion("Can I check out late?");
        candidate.setAnswer("Late checkout is available until 12:00 if there is no next guest.");
        candidate.setNormalizedText("can i check out late late checkout available");
        candidate.setNormalizedHash("hash");
        MessageKnowledgeMatch match = new MessageKnowledgeMatch(
                candidate,
                0.75,
                SuMessagingThreadContext.SCOPE_ROOM_TYPE,
                List.of("KEYWORD_OVERLAP")
        );

        when(fixture.threadRepository.findByStoreIdAndId(26L, 77L)).thenReturn(Optional.of(thread));
        when(fixture.contextResolver.resolve(eq(26L), eq(thread), any())).thenReturn(newContext());
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdAndSenderTypeDesc(
                eq(26L),
                eq(77L),
                eq(SuMessagingSenderType.GUEST),
                any(Pageable.class)
        )).thenReturn(List.of(latestGuest));
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdDesc(
                eq(26L),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(latestGuest));
        when(fixture.searchService.searchSimilar(eq(26L), eq(77L), any(), eq("Is late checkout possible?"), eq(3)))
                .thenReturn(new MessageKnowledgeSearchResult(
                        MessageKnowledgeSearchService.STATUS_MATCHED,
                        List.of(match),
                        List.of()
                ));
        when(fixture.chatLanguageModel.generate(any(String.class)))
                .thenReturn("Late checkout may be available until 12:00, subject to availability.");

        SuMessagingAiReplyDraftResponse response = fixture.service.generateDraft(
                26L,
                77L,
                new SuMessagingAiReplyDraftRequest()
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, response.getRetrievalStatus());
        assertEquals(1, response.getMatchedKnowledgeCount());
        assertEquals(
                "Late checkout may be available until 12:00, subject to availability.",
                response.getDraftReply()
        );

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.chatLanguageModel).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Can I check out late?"));
        assertTrue(prompt.contains("Reusable policy facts:"));
        assertTrue(prompt.contains("Late checkout is available until 12:00"));
        assertFalse(prompt.contains("DO_NOT_LEAK"));
        assertTrue(prompt.indexOf("Last guest turn to reply to:") < prompt.indexOf("Reusable policy facts:"));
        assertTrue(prompt.indexOf("Reusable policy facts:") < prompt.indexOf("Similar historical resolved Q&A:"));
        assertTrue(prompt.indexOf("Similar historical resolved Q&A:") < prompt.indexOf("Recent conversation:"));
        assertTrue(prompt.indexOf("Recent conversation:") < prompt.indexOf("Current thread context:"));
    }

    @Test
    void generateDraft_shouldNotCopyPartialHistoryWhenModelFails() {
        TestFixture fixture = new TestFixture();
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage latestGuest = newMessage(201L, thread, SuMessagingSenderType.GUEST, "Is late checkout possible?");

        MessageKnowledgeCandidate candidate = new MessageKnowledgeCandidate();
        candidate.setId(1L);
        candidate.setStoreId(26L);
        candidate.setThreadId(11L);
        candidate.setQuestion("Can I check out late?");
        candidate.setAnswer("Late checkout is available until 12:30 for 1500 JPY.");
        candidate.setNormalizedText("can i check out late late checkout available");
        candidate.setNormalizedHash("hash");
        MessageKnowledgeMatch match = new MessageKnowledgeMatch(
                candidate,
                0.21,
                SuMessagingThreadContext.SCOPE_STORE,
                List.of("KEYWORD_OVERLAP"),
                List.of(MessageKnowledgeTopicService.TOPIC_LATE_CHECKOUT),
                List.of("Late checkout is available until 12:30 for 1500 JPY.")
        );

        when(fixture.threadRepository.findByStoreIdAndId(26L, 77L)).thenReturn(Optional.of(thread));
        when(fixture.contextResolver.resolve(eq(26L), eq(thread), any())).thenReturn(newContext());
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdAndSenderTypeDesc(
                eq(26L),
                eq(77L),
                eq(SuMessagingSenderType.GUEST),
                any(Pageable.class)
        )).thenReturn(List.of(latestGuest));
        when(fixture.messageRepository.findRecentByStoreIdAndThreadIdDesc(
                eq(26L),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(latestGuest));
        when(fixture.searchService.searchSimilar(eq(26L), eq(77L), any(), eq("Is late checkout possible?"), eq(3)))
                .thenReturn(new MessageKnowledgeSearchResult(
                        MessageKnowledgeSearchService.STATUS_PARTIAL,
                        List.of(match),
                        List.of(MessageKnowledgeSearchService.WARNING_LOW_CONFIDENCE_MATCH)
                ));
        when(fixture.chatLanguageModel.generate(any(String.class)))
                .thenThrow(new RuntimeException("model unavailable"));

        SuMessagingAiReplyDraftResponse response = fixture.service.generateDraft(
                26L,
                77L,
                new SuMessagingAiReplyDraftRequest()
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_PARTIAL, response.getRetrievalStatus());
        assertEquals(
                "Thank you for your message. We will check this and get back to you shortly.",
                response.getDraftReply()
        );
        assertTrue(response.getWarnings().contains(MessageKnowledgeSearchService.WARNING_LOW_CONFIDENCE_MATCH));
        assertTrue(response.getWarnings().contains("MODEL_GENERATION_FAILED"));
        assertFalse(response.getDraftReply().contains("12:30"));
        assertFalse(response.getDraftReply().contains("1500 JPY"));
    }

    private static SuMessagingThreadContext newContext() {
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        context.setChannelName("Airbnb");
        context.setBookingKey("B1");
        context.setRoomTypeId(3L);
        context.setRoomTypeName("Deluxe");
        context.setMatchStatus("EXACT_ONE");
        return context;
    }

    private static SuMessageThread newThread(Long id, Long storeId) {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(id);
        thread.setStoreId(storeId);
        thread.setSuHotelId("STORE26");
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setThreadKey("T1");
        thread.setThreadId("T1");
        thread.setBookingId("B1");
        return thread;
    }

    private static SuMessage newMessage(
            Long id,
            SuMessageThread thread,
            SuMessagingSenderType senderType,
            String content
    ) {
        SuMessage message = new SuMessage();
        message.setId(id);
        message.setStoreId(thread.getStoreId());
        message.setThread(thread);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setSentAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        message.setDeliveryStatus("SENT");
        return message;
    }

    private static String sectionBetween(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        int end = text.indexOf(endMarker);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return text.substring(start, end);
    }

    private static class TestFixture {
        private final SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        private final SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        private final SuMessagingThreadContextResolver contextResolver = Mockito.mock(SuMessagingThreadContextResolver.class);
        private final MessageKnowledgeSearchService searchService = Mockito.mock(MessageKnowledgeSearchService.class);
        private final ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        private final SuMessagingAiReplyDraftService service = new SuMessagingAiReplyDraftService(
                threadRepository,
                messageRepository,
            contextResolver,
            searchService,
            new SuMessagingAiPromptBuilder(new SuMessagingAiRedactor(), new MessageKnowledgeTopicService()),
            chatLanguageModel
        );
    }
}
