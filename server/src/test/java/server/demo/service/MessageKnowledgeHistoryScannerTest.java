package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import server.demo.entity.MessageKnowledgeScanState;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.MessageKnowledgeScanStateRepository;
import server.demo.repository.SuMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageKnowledgeHistoryScannerTest {

    @Test
    void normalizeBatchAndStoresPerRun_shouldClampToProductionSafeLimits() {
        assertEquals(1, MessageKnowledgeHistoryScanner.normalizeBatchSize(null));
        assertEquals(1, MessageKnowledgeHistoryScanner.normalizeBatchSize(-5));
        assertEquals(2, MessageKnowledgeHistoryScanner.normalizeBatchSize(2));
        assertEquals(3, MessageKnowledgeHistoryScanner.normalizeBatchSize(99));

        assertEquals(1, MessageKnowledgeHistoryScanner.normalizeStoresPerRun(null));
        assertEquals(1, MessageKnowledgeHistoryScanner.normalizeStoresPerRun(-5));
        assertEquals(1, MessageKnowledgeHistoryScanner.normalizeStoresPerRun(99));
    }

    @Test
    void runScheduledScan_shouldNotExecuteWhenDisabledByDefault() {
        MessageKnowledgeHistoryScanner historyScanner = Mockito.mock(MessageKnowledgeHistoryScanner.class);
        MessageKnowledgeScannerScheduler scheduler = new MessageKnowledgeScannerScheduler(historyScanner);

        scheduler.runScheduledScan();

        verifyNoInteractions(historyScanner);
    }

    @Test
    void scanOnce_shouldClaimLeaseUseStoreCursorAndSkipInvalidStaffReplies() {
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        MessageKnowledgeScanStateRepository scanStateRepository =
                Mockito.mock(MessageKnowledgeScanStateRepository.class);
        MessageKnowledgeRefinementService refinementService =
                Mockito.mock(MessageKnowledgeRefinementService.class);
        MessageKnowledgeHistoryScanner scanner = newScanner(
                messageRepository,
                scanStateRepository,
                refinementService
        );
        ReflectionTestUtils.setField(scanner, "batchSize", 99);
        ReflectionTestUtils.setField(scanner, "storesPerRun", 99);

        MessageKnowledgeScanState state = newScanState(1L, 26L, 100L);
        SuMessageThread thread = newThread(77L, 26L);
        SuMessage staffReply = newMessage(101L, thread, SuMessagingSenderType.STAFF, "Wifi is sakura2026.", "SENT");
        SuMessage failedReply = newMessage(102L, thread, SuMessagingSenderType.STAFF, "Old failed answer.", "FAILED");
        SuMessage sendingReply = newMessage(103L, thread, SuMessagingSenderType.STAFF, "Still sending.", "SENDING");
        SuMessage guestQuestion = newMessage(90L, thread, SuMessagingSenderType.GUEST, "What is wifi?", "SENT");

        stubSuccessfulClaim(scanStateRepository, state);
        when(scanStateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(scanStateRepository.save(any(MessageKnowledgeScanState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findHistoryBatchForKnowledgeScanner(
                eq(26L),
                eq(100L),
                any(Pageable.class)
        )).thenReturn(List.of(staffReply, failedReply, sendingReply));
        when(messageRepository.findPreviousMessageBySenderForKnowledgeScanner(
                eq(26L),
                eq(77L),
                eq(SuMessagingSenderType.GUEST),
                eq(101L),
                eq(PageRequest.of(0, 1))
        )).thenReturn(List.of(guestQuestion));
        when(messageRepository.existsSuccessfulStaffReplyBetween(
                26L,
                77L,
                SuMessagingSenderType.STAFF,
                90L,
                101L
        )).thenReturn(false);
        when(refinementService.refineSourcePair(26L, thread, guestQuestion, staffReply)).thenReturn(true);

        int extracted = scanner.scanOnce();

        assertEquals(1, extracted);
        assertEquals(MessageKnowledgeScanState.STATUS_IDLE, state.getStatus());
        assertEquals(103L, state.getLastScannedMessageId());
        assertEquals(3L, state.getProcessedMessageCount());
        assertEquals(1L, state.getExtractedPairCount());
        assertEquals(0, state.getFailureCount());
        assertNull(state.getLeaseOwner());
        assertNull(state.getLeaseUntil());
        assertNotNull(state.getLastStartedAt());
        assertNotNull(state.getLastFinishedAt());
        assertNotNull(state.getNextScanAfter());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findHistoryBatchForKnowledgeScanner(
                eq(26L),
                eq(100L),
                pageableCaptor.capture()
        );
        assertEquals(3, pageableCaptor.getValue().getPageSize());
        verify(scanStateRepository, times(1)).findNextDueStateId(any(LocalDateTime.class));
        verify(messageRepository, times(1)).findPreviousMessageBySenderForKnowledgeScanner(
                eq(26L),
                eq(77L),
                eq(SuMessagingSenderType.GUEST),
                any(),
                eq(PageRequest.of(0, 1))
        );
        verify(refinementService).refineSourcePair(26L, thread, guestQuestion, staffReply);
    }

    @Test
    void scanOnce_shouldAdvanceCursorWithoutExtractionWhenNoGuestQuestionExists() {
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        MessageKnowledgeScanStateRepository scanStateRepository =
                Mockito.mock(MessageKnowledgeScanStateRepository.class);
        MessageKnowledgeRefinementService refinementService =
                Mockito.mock(MessageKnowledgeRefinementService.class);
        MessageKnowledgeHistoryScanner scanner = newScanner(
                messageRepository,
                scanStateRepository,
                refinementService
        );

        MessageKnowledgeScanState state = newScanState(2L, 26L, 200L);
        SuMessageThread thread = newThread(88L, 26L);
        SuMessage staffReply = newMessage(201L, thread, SuMessagingSenderType.STAFF, "We have luggage storage.", "SENT");

        stubSuccessfulClaim(scanStateRepository, state);
        when(scanStateRepository.findById(2L)).thenReturn(Optional.of(state));
        when(scanStateRepository.save(any(MessageKnowledgeScanState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findHistoryBatchForKnowledgeScanner(
                eq(26L),
                eq(200L),
                eq(PageRequest.of(0, 1))
        )).thenReturn(List.of(staffReply));
        when(messageRepository.findPreviousMessageBySenderForKnowledgeScanner(
                eq(26L),
                eq(88L),
                eq(SuMessagingSenderType.GUEST),
                eq(201L),
                eq(PageRequest.of(0, 1))
        )).thenReturn(List.of());

        int extracted = scanner.scanOnce();

        assertEquals(0, extracted);
        assertEquals(201L, state.getLastScannedMessageId());
        assertEquals(1L, state.getProcessedMessageCount());
        assertEquals(0L, state.getExtractedPairCount());
        verify(refinementService, never()).refineSourcePair(any(), any(), any(), any());
    }

    @Test
    void scanOnce_shouldRecordFailureBackoffAndReleaseLease() {
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        MessageKnowledgeScanStateRepository scanStateRepository =
                Mockito.mock(MessageKnowledgeScanStateRepository.class);
        MessageKnowledgeRefinementService refinementService =
                Mockito.mock(MessageKnowledgeRefinementService.class);
        MessageKnowledgeHistoryScanner scanner = newScanner(
                messageRepository,
                scanStateRepository,
                refinementService
        );
        ReflectionTestUtils.setField(scanner, "failureBackoffMs", 250L);

        MessageKnowledgeScanState state = newScanState(3L, 26L, 300L);
        state.setFailureCount(2);

        stubSuccessfulClaim(scanStateRepository, state);
        when(scanStateRepository.findById(3L)).thenReturn(Optional.of(state));
        when(scanStateRepository.save(any(MessageKnowledgeScanState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findHistoryBatchForKnowledgeScanner(
                eq(26L),
                eq(300L),
                eq(PageRequest.of(0, 1))
        )).thenThrow(new IllegalStateException("repository unavailable"));

        int extracted = scanner.scanOnce();

        assertEquals(0, extracted);
        assertEquals(MessageKnowledgeScanState.STATUS_FAILED, state.getStatus());
        assertEquals(3, state.getFailureCount());
        assertEquals("repository unavailable", state.getLastError());
        assertNull(state.getLeaseOwner());
        assertNull(state.getLeaseUntil());
        assertNotNull(state.getBackoffUntil());
        assertNotNull(state.getNextScanAfter());
        assertNotNull(state.getLastFinishedAt());
        verify(refinementService, never()).refineSourcePair(any(), any(), any(), any());
        verify(scanStateRepository, times(1)).save(state);
    }

    @Test
    void scanOnce_shouldNotProcessStateWhenAtomicClaimLosesRace() {
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        MessageKnowledgeScanStateRepository scanStateRepository =
                Mockito.mock(MessageKnowledgeScanStateRepository.class);
        MessageKnowledgeRefinementService refinementService =
                Mockito.mock(MessageKnowledgeRefinementService.class);
        MessageKnowledgeHistoryScanner scanner = newScanner(
                messageRepository,
                scanStateRepository,
                refinementService
        );

        when(scanStateRepository.findNextDueStateId(any(LocalDateTime.class))).thenReturn(Optional.of(4L));
        when(scanStateRepository.claimDueState(
                eq(4L),
                any(LocalDateTime.class),
                any(),
                any(LocalDateTime.class),
                eq(MessageKnowledgeScanState.STATUS_RUNNING)
        )).thenReturn(0);

        int extracted = scanner.scanOnce();

        assertEquals(0, extracted);
        verify(scanStateRepository, times(1)).claimDueState(
                eq(4L),
                any(LocalDateTime.class),
                any(),
                any(LocalDateTime.class),
                eq(MessageKnowledgeScanState.STATUS_RUNNING)
        );
        verify(scanStateRepository, never()).findById(4L);
        verify(messageRepository, never()).findHistoryBatchForKnowledgeScanner(any(), any(), any());
        verifyNoInteractions(refinementService);
    }

    private static MessageKnowledgeHistoryScanner newScanner(
            SuMessageRepository messageRepository,
            MessageKnowledgeScanStateRepository scanStateRepository,
            MessageKnowledgeRefinementService refinementService
    ) {
        return new MessageKnowledgeHistoryScanner(
                messageRepository,
                scanStateRepository,
                refinementService,
                new NoopTransactionManager()
        );
    }

    private static MessageKnowledgeScanState newScanState(Long id, Long storeId, Long cursor) {
        MessageKnowledgeScanState state = new MessageKnowledgeScanState();
        state.setId(id);
        state.setStoreId(storeId);
        state.setStatus(MessageKnowledgeScanState.STATUS_IDLE);
        state.setLastScannedMessageId(cursor);
        state.setFailureCount(0);
        state.setProcessedMessageCount(0L);
        state.setExtractedPairCount(0L);
        return state;
    }

    private static void stubSuccessfulClaim(
            MessageKnowledgeScanStateRepository scanStateRepository,
            MessageKnowledgeScanState state
    ) {
        when(scanStateRepository.findNextDueStateId(any(LocalDateTime.class)))
                .thenReturn(Optional.of(state.getId()));
        when(scanStateRepository.claimDueState(
                eq(state.getId()),
                any(LocalDateTime.class),
                any(),
                any(LocalDateTime.class),
                eq(MessageKnowledgeScanState.STATUS_RUNNING)
        )).thenAnswer(invocation -> {
            state.setStatus(invocation.getArgument(4, String.class));
            state.setLeaseOwner(invocation.getArgument(2, String.class));
            state.setLeaseUntil(invocation.getArgument(3, LocalDateTime.class));
            state.setLastStartedAt(invocation.getArgument(1, LocalDateTime.class));
            state.setLastError(null);
            return 1;
        });
    }

    private static SuMessageThread newThread(Long id, Long storeId) {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(id);
        thread.setStoreId(storeId);
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setBookingId("B" + id);
        return thread;
    }

    private static SuMessage newMessage(
            Long id,
            SuMessageThread thread,
            SuMessagingSenderType senderType,
            String content,
            String deliveryStatus
    ) {
        SuMessage message = new SuMessage();
        message.setId(id);
        message.setStoreId(thread.getStoreId());
        message.setThread(thread);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setDeliveryStatus(deliveryStatus);
        message.setSentAt(LocalDateTime.of(2026, 6, 15, 10, 0).plusMinutes(id));
        return message;
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
