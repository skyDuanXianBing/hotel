package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import server.demo.entity.MessageKnowledgeTopic;
import server.demo.repository.MessageKnowledgeTopicRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageKnowledgeTopicDictionaryServiceTest {

    @Test
    void findActiveTopics_shouldSeedDefaultTopicsWhenStoreDictionaryIsMissing() {
        MessageKnowledgeTopicRepository repository = Mockito.mock(MessageKnowledgeTopicRepository.class);
        MessageKnowledgeTopicDictionaryService service =
                new MessageKnowledgeTopicDictionaryService(repository, new ObjectMapper());
        MessageKnowledgeTopic wifi = activeTopic("wifi", "WiFi");

        when(repository.findByStoreIdAndTopicCode(eq(26L), anyString())).thenReturn(Optional.empty());
        when(repository.findByStoreIdAndStatus(
                eq(26L),
                eq(MessageKnowledgeTopic.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of(wifi));

        List<MessageKnowledgeTopic> topics = service.findActiveTopics(26L, 50);

        assertEquals(List.of(wifi), topics);
        ArgumentCaptor<Iterable<MessageKnowledgeTopic>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<MessageKnowledgeTopic> seededTopics = toList(captor.getValue());
        assertEquals(service.countDefaultTopics(), seededTopics.size());
        assertTrue(seededTopics.stream().anyMatch(topic -> "wifi".equals(topic.getTopicCode())));
        assertTrue(seededTopics.stream().anyMatch(topic -> "breakfast".equals(topic.getTopicCode())));
        assertTrue(seededTopics.stream().anyMatch(topic -> "location".equals(topic.getTopicCode())));
        for (MessageKnowledgeTopic topic : seededTopics) {
            assertEquals(26L, topic.getStoreId());
            assertEquals(MessageKnowledgeTopic.STATUS_ACTIVE, topic.getStatus());
            assertEquals(MessageKnowledgeTopic.SOURCE_DEFAULT, topic.getSource());
        }
    }

    @Test
    void createNeedsReviewCandidate_shouldNeverCreateActiveTopic() {
        MessageKnowledgeTopicRepository repository = Mockito.mock(MessageKnowledgeTopicRepository.class);
        MessageKnowledgeTopicDictionaryService service =
                new MessageKnowledgeTopicDictionaryService(repository, new ObjectMapper());
        when(repository.findByStoreIdAndTopicCode(26L, "pool_hours")).thenReturn(Optional.empty());
        when(repository.save(any(MessageKnowledgeTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageKnowledgeTopic topic = service.createNeedsReviewCandidate(
                26L,
                "pool-hours",
                "Pool Hours",
                "Swimming pool hours.",
                null
        );

        assertEquals("pool_hours", topic.getTopicCode());
        assertEquals(MessageKnowledgeTopic.STATUS_NEEDS_REVIEW, topic.getStatus());
        assertEquals(MessageKnowledgeTopic.SOURCE_AI_CANDIDATE, topic.getSource());
    }

    private static MessageKnowledgeTopic activeTopic(String topicCode, String displayName) {
        MessageKnowledgeTopic topic = new MessageKnowledgeTopic();
        topic.setStoreId(26L);
        topic.setTopicCode(topicCode);
        topic.setDisplayName(displayName);
        topic.setStatus(MessageKnowledgeTopic.STATUS_ACTIVE);
        return topic;
    }

    private static List<MessageKnowledgeTopic> toList(Iterable<MessageKnowledgeTopic> values) {
        List<MessageKnowledgeTopic> topics = new ArrayList<>();
        for (MessageKnowledgeTopic value : values) {
            topics.add(value);
        }
        return topics;
    }
}
