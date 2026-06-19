package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageKnowledgeDuplicateCandidateServiceTest {

    @Test
    void findCandidates_shouldKeepOnlySameBoundaryActiveOrCandidateItems() {
        MessageKnowledgeItemRepository repository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeDuplicateCandidateService service = new MessageKnowledgeDuplicateCandidateService(repository);
        ReflectionTestUtils.setField(service, "candidateCap", 5);

        MessageKnowledgeItem active = item(101L, 26L, "ROOM_TYPE:3", "checkout", "topic-hash", "fact-a");
        active.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        MessageKnowledgeItem candidate = item(102L, 26L, "ROOM_TYPE:3", "checkout", "topic-hash", "fact-b");
        candidate.setStatus(MessageKnowledgeItem.STATUS_CANDIDATE);
        MessageKnowledgeItem wrongStore = item(103L, 99L, "ROOM_TYPE:3", "checkout", "topic-hash", "fact-c");
        wrongStore.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        MessageKnowledgeItem wrongScope = item(104L, 26L, "ROOM_TYPE:9", "checkout", "topic-hash", "fact-d");
        wrongScope.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        MessageKnowledgeItem wrongTopic = item(105L, 26L, "ROOM_TYPE:3", "wifi", "topic-hash", "fact-e");
        wrongTopic.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        MessageKnowledgeItem archived = item(106L, 26L, "ROOM_TYPE:3", "checkout", "topic-hash", "fact-f");
        archived.setStatus(MessageKnowledgeItem.STATUS_ARCHIVED);
        MessageKnowledgeItem exactFact = item(107L, 26L, "ROOM_TYPE:3", "checkout", "topic-hash", "incoming-fact");
        exactFact.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);

        when(repository.findIngestDuplicateCandidates(
                eq(26L),
                eq("ROOM_TYPE:3"),
                eq("checkout"),
                eq("topic-hash"),
                eq(List.of(MessageKnowledgeItem.STATUS_ACTIVE, MessageKnowledgeItem.STATUS_CANDIDATE)),
                any(Pageable.class)
        )).thenReturn(List.of(active, candidate, wrongStore, wrongScope, wrongTopic, archived, exactFact));

        List<MessageKnowledgeItem> candidates = service.findCandidates(
                26L,
                "ROOM_TYPE:3",
                "checkout",
                "topic-hash",
                "incoming-fact"
        );

        assertEquals(2, candidates.size());
        assertEquals(101L, candidates.get(0).getId());
        assertEquals(102L, candidates.get(1).getId());
        verify(repository).findIngestDuplicateCandidates(
                eq(26L),
                eq("ROOM_TYPE:3"),
                eq("checkout"),
                eq("topic-hash"),
                eq(List.of(MessageKnowledgeItem.STATUS_ACTIVE, MessageKnowledgeItem.STATUS_CANDIDATE)),
                any(Pageable.class)
        );
    }

    private static MessageKnowledgeItem item(
            Long id,
            Long storeId,
            String scopeKey,
            String topic,
            String topicHash,
            String factHash
    ) {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(id);
        item.setStoreId(storeId);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(3L);
        item.setScopeKey(scopeKey);
        item.setTopic(topic);
        item.setTopicHash(topicHash);
        item.setFactHash(factHash);
        item.setQuestion("Question " + id);
        item.setAnswer("Answer " + id);
        item.setNormalizedQuestion("question " + id);
        item.setNormalizedAnswer("answer " + id);
        item.setNormalizedAnswerHash("answer-hash-" + id);
        return item;
    }
}
