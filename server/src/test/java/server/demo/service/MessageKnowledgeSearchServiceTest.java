package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import server.demo.entity.MessageKnowledgeEntry;
import server.demo.repository.MessageKnowledgeEntryRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageKnowledgeSearchServiceTest {

    @Test
    void searchSimilar_shouldUseStoreScopedRoomQueryAndExcludeCurrentThread() {
        MessageKnowledgeEntryRepository repository = Mockito.mock(MessageKnowledgeEntryRepository.class);
        MessageKnowledgeSearchService service = new MessageKnowledgeSearchService(
                repository,
                new SuMessagingAiTextService(),
                new MessageKnowledgeTopicService()
        );

        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomId(8L);

        MessageKnowledgeEntry entry = new MessageKnowledgeEntry();
        entry.setId(1L);
        entry.setStoreId(26L);
        entry.setThreadId(11L);
        entry.setRoomId(8L);
        entry.setQuestion("Can I check out late?");
        entry.setAnswer("Late checkout is available until noon.");
        entry.setNormalizedText("can i check out late late checkout is available until noon");
        entry.setNormalizedHash("hash");
        entry.setSourceTimestamp(LocalDateTime.now());

        when(repository.findActiveByStoreIdAndRoomId(
                eq(26L),
                eq(8L),
                eq(MessageKnowledgeEntry.STATUS_ACTIVE),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(entry));
        when(repository.findActiveRecentByStoreId(
                eq(26L),
                eq(MessageKnowledgeEntry.STATUS_ACTIVE),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of());

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                context,
                "Is late checkout possible?",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, result.getRetrievalStatus());
        assertEquals(1, result.getMatches().size());
        assertFalse(result.getMatches().get(0).getMatchReasons().isEmpty());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findActiveByStoreIdAndRoomId(
                eq(26L),
                eq(8L),
                eq(MessageKnowledgeEntry.STATUS_ACTIVE),
                eq(77L),
                pageableCaptor.capture()
        );
        assertEquals(60, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void searchSimilar_shouldPromoteStoreScopedPolicyFactsForBa03Topics() {
        MessageKnowledgeEntryRepository repository = Mockito.mock(MessageKnowledgeEntryRepository.class);
        MessageKnowledgeSearchService service = new MessageKnowledgeSearchService(
                repository,
                new SuMessagingAiTextService(),
                new MessageKnowledgeTopicService()
        );
        List<MessageKnowledgeEntry> entries = List.of(
                newKnowledgeEntry(
                        1L,
                        "Can I request late checkout?",
                        "Late checkout is available until 12:30 for 1500 JPY when there is no same-day arrival."
                ),
                newKnowledgeEntry(
                        2L,
                        "Do you have parking?",
                        "Parking costs 1000 JPY per night. Spaces are first come, first served."
                ),
                newKnowledgeEntry(
                        3L,
                        "Can I store my luggage?",
                        "We can keep luggage at the 1楼前台 from 09:00 to 18:00."
                ),
                newKnowledgeEntry(
                        4L,
                        "What time is breakfast?",
                        "Breakfast is served 07:00-10:00 at 2 楼 Sakura. It is 1800 JPY per adult."
                )
        );
        when(repository.findActiveRecentByStoreId(
                eq(26L),
                eq(MessageKnowledgeEntry.STATUS_ACTIVE),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(entries);

        assertPolicyMatchContainsFacts(service, "Is late checkout possible?", "12:30", "1500 JPY");
        assertPolicyMatchContainsFacts(service, "Do you have parking for my car?", "1000 JPY", "first come");
        assertPolicyMatchContainsFacts(service, "Can you keep our bags before check-in?", "09:00", "18:00", "1楼前台");
        assertPolicyMatchContainsFacts(service, "What time is breakfast and how much?", "07:00-10:00", "Sakura", "1800 JPY");
    }

    @Test
    void searchSimilar_shouldNotLeakPolicyFactsForPetQuestion() {
        MessageKnowledgeEntryRepository repository = Mockito.mock(MessageKnowledgeEntryRepository.class);
        MessageKnowledgeSearchService service = new MessageKnowledgeSearchService(
                repository,
                new SuMessagingAiTextService(),
                new MessageKnowledgeTopicService()
        );
        when(repository.findActiveRecentByStoreId(
                eq(26L),
                eq(MessageKnowledgeEntry.STATUS_ACTIVE),
                eq(77L),
                any(Pageable.class)
        )).thenReturn(List.of(
                newKnowledgeEntry(
                        1L,
                        "Can I request late checkout?",
                        "Late checkout is available until 12:30 for 1500 JPY."
                ),
                newKnowledgeEntry(
                        2L,
                        "What time is breakfast?",
                        "Breakfast is served 07:00-10:00 at 2 楼 Sakura."
                )
        ));

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                null,
                "Can I bring my pet?",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_NO_MATCH, result.getRetrievalStatus());
        assertTrue(result.getMatches().isEmpty());
    }

    private static void assertPolicyMatchContainsFacts(
            MessageKnowledgeSearchService service,
            String query,
            String... expectedFacts
    ) {
        MessageKnowledgeSearchResult result = service.searchSimilar(26L, 77L, null, query, 3);

        assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, result.getRetrievalStatus());
        assertFalse(result.getMatches().isEmpty());
        MessageKnowledgeMatch topMatch = result.getMatches().get(0);
        for (String expectedFact : expectedFacts) {
            assertTrue(
                    topMatch.getReusableFacts().stream().anyMatch(fact -> fact.contains(expectedFact)),
                    "Expected reusable facts to contain: " + expectedFact
            );
        }
    }

    private static MessageKnowledgeEntry newKnowledgeEntry(Long id, String question, String answer) {
        MessageKnowledgeEntry entry = new MessageKnowledgeEntry();
        entry.setId(id);
        entry.setStoreId(26L);
        entry.setThreadId(11L);
        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setNormalizedText(question + " " + answer);
        entry.setNormalizedHash("hash-" + id);
        entry.setSourceTimestamp(LocalDateTime.now());
        return entry;
    }
}
