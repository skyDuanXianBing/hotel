package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.repository.SuMessageRepository;

import static org.mockito.Mockito.verifyNoInteractions;

class MessageKnowledgeIndexSchedulerTest {

    @Test
    void runScheduledIndex_shouldNotExecuteWhenDisabledByDefault() {
        MessageKnowledgeIndexService indexService = Mockito.mock(MessageKnowledgeIndexService.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        MessageKnowledgeIndexScheduler scheduler = new MessageKnowledgeIndexScheduler(
                indexService,
                messageRepository
        );

        scheduler.runScheduledIndex();

        verifyNoInteractions(indexService, messageRepository);
    }
}
