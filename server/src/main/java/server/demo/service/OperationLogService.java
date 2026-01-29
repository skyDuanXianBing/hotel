package server.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.OperationLogDTO;
import server.demo.dto.OperationLogDetailDTO;
import server.demo.entity.OperationLog;
import server.demo.entity.Reservation;
import server.demo.entity.User;
import server.demo.enums.OperationType;
import server.demo.repository.OperationLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.UserRepository;
import server.demo.util.StoreContextUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class OperationLogService {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_OPERATOR = "\u7cfb\u7edf";

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void logOperation(
            Long reservationId,
            OperationType operationType,
            String action,
            String operator,
            String content,
            List<OperationLogDetailDTO> details
    ) {
        Long storeId = resolveStoreId(reservationId);
        String resolvedOperator = resolveOperator(operator);

        OperationLog log = new OperationLog();
        log.setReservationId(reservationId);
        log.setOperationType(operationType);
        log.setAction(action);
        log.setOperator(resolvedOperator);
        log.setTimestamp(LocalDateTime.now());
        log.setContent(content);
        log.setStoreId(storeId);
        log.setDetails(toDetailsJson(details));

        operationLogRepository.save(log);
    }

    public List<OperationLogDTO> getReservationLogs(Long reservationId) {
        Long storeId = StoreContextUtils.requireStoreId();
        List<OperationLog> logs = operationLogRepository.findByStoreIdAndReservationIdOrderByTimestampDesc(storeId, reservationId);
        return logs.stream().map(this::toDTO).toList();
    }

    private OperationLogDTO toDTO(OperationLog log) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(log.getId());
        dto.setAction(log.getAction());
        dto.setOperator(log.getOperator());
        dto.setTimestamp(log.getTimestamp() == null ? null : log.getTimestamp().format(TS_FORMATTER));
        dto.setType(toFrontendType(log.getOperationType()));
        dto.setContent(log.getContent());
        dto.setDetails(fromDetailsJson(log.getDetails()));
        return dto;
    }

    private Long resolveStoreId(Long reservationId) {
        try {
            return StoreContextUtils.requireStoreId();
        } catch (Exception ignored) {
            // fall through
        }

        if (reservationId == null) {
            throw new RuntimeException("\u7f3a\u5c11\u95e8\u5e97\u4fe1\u606f");
        }
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("\u8ba2\u5355\u4e0d\u5b58\u5728"));
        if (reservation.getStoreId() == null) {
            throw new RuntimeException("\u8ba2\u5355\u7f3a\u5c11\u95e8\u5e97\u4fe1\u606f");
        }
        return reservation.getStoreId();
    }

    private String resolveOperator(String operatorOverride) {
        if (operatorOverride != null && !operatorOverride.isBlank()) {
            return operatorOverride;
        }
        try {
            Long userId = StoreContextUtils.requireUserId();
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return DEFAULT_OPERATOR;
            }
            if (user.getNickname() != null && !user.getNickname().isBlank()) {
                return user.getNickname();
            }
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                return user.getUsername();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return DEFAULT_OPERATOR;
    }

    private String toDetailsJson(List<OperationLogDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            throw new RuntimeException("\u64cd\u4f5c\u65e5\u5fd7\u8be6\u60c5\u5e8f\u5217\u5316\u5931\u8d25: " + e.getMessage(), e);
        }
    }

    private List<OperationLogDetailDTO> fromDetailsJson(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(detailsJson, new TypeReference<List<OperationLogDetailDTO>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String toFrontendType(OperationType operationType) {
        if (operationType == null) {
            return "order";
        }
        return switch (operationType) {
            case ORDER -> "order";
            case BILLING -> "billing";
        };
    }
}

