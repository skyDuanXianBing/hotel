package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.OperationLogDTO;
import server.demo.dto.OperationLogDetailDTO;
import server.demo.entity.OperationLog;
import server.demo.entity.User;
import server.demo.enums.OperationType;
import server.demo.repository.OperationLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock
    private OperationLogRepository operationLogRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OperationLogService operationLogService;

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void logOperation_shouldResolveOperatorFromNickname() {
        StoreContextHolder.setContext(new StoreContext(10L, 20L, "ADMIN"));

        User user = new User();
        user.setId(10L);
        user.setUsername("user_10");
        user.setNickname("张三");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(operationLogRepository.save(any(OperationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        operationLogService.logOperation(
                100L,
                OperationType.ORDER,
                "修改订单",
                null,
                null,
                List.of(new OperationLogDetailDTO("房间", "A -> B"))
        );

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogRepository).save(captor.capture());
        OperationLog saved = captor.getValue();
        assertEquals(100L, saved.getReservationId());
        assertEquals(OperationType.ORDER, saved.getOperationType());
        assertEquals("修改订单", saved.getAction());
        assertEquals("张三", saved.getOperator());
        assertEquals(20L, saved.getStoreId());
        assertNotNull(saved.getTimestamp());
        assertNotNull(saved.getDetails());
    }

    @Test
    void getReservationLogs_shouldParseDetailsJson() {
        StoreContextHolder.setContext(new StoreContext(10L, 20L, "ADMIN"));

        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setReservationId(100L);
        log.setOperationType(OperationType.BILLING);
        log.setAction("收款");
        log.setOperator("系统");
        log.setTimestamp(LocalDateTime.of(2026, 2, 1, 12, 0, 0));
        log.setDetails("[{\"label\":\"类型\",\"value\":\"payment\"}]");

        when(operationLogRepository.findByStoreIdAndReservationIdOrderByTimestampDesc(20L, 100L))
                .thenReturn(List.of(log));

        List<OperationLogDTO> result = operationLogService.getReservationLogs(100L);
        assertEquals(1, result.size());
        assertEquals("billing", result.get(0).getType());
        assertEquals("收款", result.get(0).getAction());
        assertEquals("系统", result.get(0).getOperator());
        assertEquals("2026-02-01 12:00:00", result.get(0).getTimestamp());
        assertEquals(1, result.get(0).getDetails().size());
        assertEquals("类型", result.get(0).getDetails().get(0).getLabel());
        assertEquals("payment", result.get(0).getDetails().get(0).getValue());
    }
}

