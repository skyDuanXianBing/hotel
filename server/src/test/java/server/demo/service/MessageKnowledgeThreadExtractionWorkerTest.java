package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import server.demo.repository.SuMessageThreadRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageKnowledgeThreadExtractionWorkerTest {
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void dispatchDueThreads_shouldDoNothingWhenDisabled() {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        MessageKnowledgeThreadExtractionService extractionService =
                mock(MessageKnowledgeThreadExtractionService.class);
        MessageKnowledgeThreadExtractionWorker worker = newWorker(threadRepository, extractionService);
        ReflectionTestUtils.setField(worker, "enabled", false);

        assertEquals(0, worker.dispatchDueThreads());

        verifyNoInteractions(threadRepository);
        verifyNoInteractions(extractionService);
    }

    @Test
    void dispatchDueThreads_shouldCapQueryByMaxConcurrencyThree() {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        MessageKnowledgeThreadExtractionService extractionService =
                mock(MessageKnowledgeThreadExtractionService.class);
        MessageKnowledgeThreadExtractionWorker worker = newWorker(threadRepository, extractionService);
        ReflectionTestUtils.setField(worker, "enabled", true);
        ReflectionTestUtils.setField(worker, "claimSize", 10);
        ReflectionTestUtils.setField(worker, "maxConcurrency", 9);
        when(extractionService.isEnabled()).thenReturn(true);
        when(threadRepository.findDueKnowledgeThreads(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        assertEquals(0, worker.dispatchDueThreads());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(threadRepository).findDueKnowledgeThreads(any(LocalDateTime.class), pageableCaptor.capture());
        assertEquals(3, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void processClaimedThread_shouldReleaseLeaseWhenNewerDirtyArrivedDuringExtraction() {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        MessageKnowledgeThreadExtractionService extractionService =
                mock(MessageKnowledgeThreadExtractionService.class);
        MessageKnowledgeThreadExtractionWorker worker = newWorker(threadRepository, extractionService);
        when(extractionService.isEnabled()).thenReturn(true);
        when(extractionService.extractThread(26L, 77L, 203L, "owner-1", "owner-1"))
                .thenReturn(completedSummary(203L));
        when(threadRepository.completeKnowledgeExtractionIfCurrent(
                eq(26L),
                eq(77L),
                eq("owner-1"),
                eq(203L),
                any(LocalDateTime.class),
                eq(MessageKnowledgeThreadKnowledgeWriter.EXTRACTOR_VERSION)
        )).thenReturn(0);
        when(threadRepository.releaseKnowledgeExtractionForStaleDirty(
                eq(26L),
                eq(77L),
                eq("owner-1"),
                eq(203L),
                any(LocalDateTime.class),
                eq(MessageKnowledgeThreadKnowledgeWriter.EXTRACTOR_VERSION)
        )).thenReturn(1);

        worker.processClaimedThread(26L, 77L, 203L, "owner-1");

        verify(threadRepository).completeKnowledgeExtractionIfCurrent(
                eq(26L),
                eq(77L),
                eq("owner-1"),
                eq(203L),
                any(LocalDateTime.class),
                eq(MessageKnowledgeThreadKnowledgeWriter.EXTRACTOR_VERSION)
        );
        verify(threadRepository).releaseKnowledgeExtractionForStaleDirty(
                eq(26L),
                eq(77L),
                eq("owner-1"),
                eq(203L),
                any(LocalDateTime.class),
                eq(MessageKnowledgeThreadKnowledgeWriter.EXTRACTOR_VERSION)
        );
        verify(threadRepository, never()).failKnowledgeExtractionForRetry(
                any(),
                any(),
                anyString(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any()
        );
    }

    @Test
    void processClaimedThread_shouldRecordBackoffOnExtractionFailure() {
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        MessageKnowledgeThreadExtractionService extractionService =
                mock(MessageKnowledgeThreadExtractionService.class);
        MessageKnowledgeThreadExtractionWorker worker = newWorker(threadRepository, extractionService);
        when(extractionService.isEnabled()).thenReturn(true);
        when(extractionService.extractThread(26L, 77L, 203L, "owner-1", "owner-1"))
                .thenThrow(new RuntimeException("model timeout"));
        when(threadRepository.failKnowledgeExtractionForRetry(
                eq(26L),
                eq(77L),
                eq("owner-1"),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq("model timeout")
        )).thenReturn(1);

        worker.processClaimedThread(26L, 77L, 203L, "owner-1");

        verify(threadRepository).failKnowledgeExtractionForRetry(
                eq(26L),
                eq(77L),
                eq("owner-1"),
                eq(LocalDateTime.of(2026, 6, 18, 10, 0)),
                eq(LocalDateTime.of(2026, 6, 18, 10, 15)),
                eq("model timeout")
        );
    }

    private static MessageKnowledgeThreadExtractionWorker newWorker(
            SuMessageThreadRepository threadRepository,
            MessageKnowledgeThreadExtractionService extractionService
    ) {
        return new MessageKnowledgeThreadExtractionWorker(
                threadRepository,
                extractionService,
                new NoopTransactionManager(),
                FIXED_CLOCK
        );
    }

    private static MessageKnowledgeThreadExtractionSummary completedSummary(Long coveredUntilMessageId) {
        return new MessageKnowledgeThreadExtractionSummary(
                26L,
                77L,
                coveredUntilMessageId,
                "owner-1",
                "COMPLETED",
                3,
                3,
                1,
                1,
                0,
                1,
                0
        );
    }

    private static final class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }
    }
}
