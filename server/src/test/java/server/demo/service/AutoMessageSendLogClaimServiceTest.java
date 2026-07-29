package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.AutoMessageSendLog;
import server.demo.repository.AutoMessageSendLogRepository;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AutoMessageSendLogClaimServiceTest {

    private final AutoMessageSendLogRepository repository = mock(AutoMessageSendLogRepository.class);
    private final AutoMessageSendLogClaimService service = new AutoMessageSendLogClaimService(repository);

    @Test
    void updateResult_reloadsByIdAndWritesResult() {
        AutoMessageSendLog row = new AutoMessageSendLog();
        row.setId(4315L);
        row.setSuccess(null);
        row.setErrorMessage("stale");
        when(repository.findById(4315L)).thenReturn(Optional.of(row));

        service.updateResult(4315L, true, null);

        assertEquals(Boolean.TRUE, row.getSuccess());
        assertNull(row.getErrorMessage());
        verify(repository).save(row);
    }

    @Test
    void updateResult_writesFailureWithErrorMessage() {
        AutoMessageSendLog row = new AutoMessageSendLog();
        row.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(row));

        service.updateResult(1L, false, "WAITING_THREAD: thread not found");

        assertEquals(Boolean.FALSE, row.getSuccess());
        assertEquals("WAITING_THREAD: thread not found", row.getErrorMessage());
        verify(repository).save(row);
    }

    @Test
    void updateResult_nullId_isNoop() {
        service.updateResult(null, true, null);

        verifyNoInteractions(repository);
    }

    @Test
    void updateResult_rowMissing_isNoop() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.updateResult(9L, true, null));
        verify(repository, never()).save(any());
    }

    @Test
    void updateResult_runsInRequiresNewTransaction() throws Exception {
        // 修复核心约束：结果回写必须在独立事务中运行，
        // 否则又会受外层 tick 事务的 REPEATABLE_READ 快照影响而重演刷屏事故。
        Method method = AutoMessageSendLogClaimService.class.getMethod(
                "updateResult", Long.class, Boolean.class, String.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, "updateResult 必须声明 @Transactional(REQUIRES_NEW)");
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    @Test
    void insertClaim_runsInRequiresNewTransaction() throws Exception {
        Method method = AutoMessageSendLogClaimService.class.getMethod(
                "insertClaim", Long.class, String.class, String.class, Long.class, Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    @Test
    void resetForResend_createsRowWhenMissing() {
        when(repository.findByStoreIdAndActionAndTargetTypeAndTargetId(26L, "AM:13", "RESERVATION", 800L))
                .thenReturn(Optional.empty());

        service.resetForResend(26L, "AM:13", "RESERVATION", 800L, 13L, "WAITING_MANUAL_REPLAY: manual replay requested");

        org.mockito.ArgumentCaptor<AutoMessageSendLog> captor = org.mockito.ArgumentCaptor.forClass(AutoMessageSendLog.class);
        verify(repository).save(captor.capture());
        AutoMessageSendLog saved = captor.getValue();
        assertEquals(26L, saved.getStoreId());
        assertEquals("AM:13", saved.getAction());
        assertEquals("RESERVATION", saved.getTargetType());
        assertEquals(800L, saved.getTargetId());
        assertEquals(13L, saved.getAutoMessageId());
        assertEquals(Boolean.FALSE, saved.getSuccess());
        assertEquals("WAITING_MANUAL_REPLAY: manual replay requested", saved.getErrorMessage());
    }

    @Test
    void resetForResend_rewritesExistingRow() {
        AutoMessageSendLog existing = new AutoMessageSendLog();
        existing.setId(4315L);
        existing.setStoreId(26L);
        existing.setAction("AM:13");
        existing.setTargetType("RESERVATION");
        existing.setTargetId(800L);
        existing.setSuccess(true);
        when(repository.findByStoreIdAndActionAndTargetTypeAndTargetId(26L, "AM:13", "RESERVATION", 800L))
                .thenReturn(Optional.of(existing));

        service.resetForResend(26L, "AM:13", "RESERVATION", 800L, 13L, "WAITING_MANUAL_REPLAY: manual replay requested");

        assertEquals(Boolean.FALSE, existing.getSuccess());
        assertEquals("WAITING_MANUAL_REPLAY: manual replay requested", existing.getErrorMessage());
        assertEquals(13L, existing.getAutoMessageId());
        verify(repository).save(existing);
    }

    @Test
    void resetForResend_runsInRequiresNewTransaction() throws Exception {
        Method method = AutoMessageSendLogClaimService.class.getMethod(
                "resetForResend", Long.class, String.class, String.class, Long.class, Long.class, String.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, "resetForResend 必须声明 @Transactional(REQUIRES_NEW)");
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
