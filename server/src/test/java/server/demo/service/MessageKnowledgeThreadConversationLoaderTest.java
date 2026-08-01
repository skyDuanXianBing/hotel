package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageKnowledgeThreadConversationLoaderTest {

    @Mock
    private SuMessageRepository messageRepository;

    @Mock
    private SuMessageThreadRepository threadRepository;

    @Mock
    private SuMessagingThreadContextResolver contextResolver;

    @InjectMocks
    private MessageKnowledgeThreadConversationLoader loader;

    @Test
    void load_shouldKeepNewestMessagesInAscendingOrderWhenThreadExceedsLimit() {
        ReflectionTestUtils.setField(loader, "maxMessages", 2);

        SuMessageThread thread = new SuMessageThread();
        thread.setId(77L);
        when(threadRepository.findByStoreIdAndId(26L, 77L)).thenReturn(Optional.of(thread));
        when(messageRepository.findByStoreIdAndThreadIdUpToMessageIdOrderBySentAtDesc(
                eq(26L),
                eq(77L),
                eq(103L),
                any(Pageable.class)
        )).thenReturn(List.of(
                        message(103L, thread, "newest"),
                        message(102L, thread, "newer")
                )
        );

        MessageKnowledgeThreadConversation conversation = loader.load(26L, 77L, 103L);

        assertEquals(2, conversation.messages().size());
        assertEquals(102L, conversation.messages().get(0).id());
        assertEquals(103L, conversation.messages().get(1).id());
    }

    private static SuMessage message(Long id, SuMessageThread thread, String content) {
        SuMessage message = new SuMessage();
        message.setId(id);
        message.setStoreId(26L);
        message.setThread(thread);
        message.setSenderType(SuMessagingSenderType.GUEST);
        message.setDeliveryStatus("SENT");
        message.setContent(content);
        message.setSentAt(LocalDateTime.of(2026, 6, 18, 12, 0).plusMinutes(id));
        return message;
    }
}
