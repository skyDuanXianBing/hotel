package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MessageKnowledgeThreadExtractionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(MessageKnowledgeThreadExtractionScheduler.class);

    private final MessageKnowledgeThreadExtractionWorker worker;

    public MessageKnowledgeThreadExtractionScheduler(MessageKnowledgeThreadExtractionWorker worker) {
        this.worker = worker;
    }

    @Scheduled(
            initialDelayString = "${messaging.knowledge.thread-scheduler.initial-delay-ms:60000}",
            fixedDelayString = "${messaging.knowledge.thread-scheduler.fixed-delay-ms:60000}"
    )
    public void runScheduledExtraction() {
        int launched = worker.dispatchDueThreads();
        if (launched > 0) {
            logger.info(
                    "Thread message knowledge scheduler dispatched extraction tasks. launched={}, running={}",
                    launched,
                    worker.runningCount()
            );
        }
    }
}
