package server.demo.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PriceLabs webhook 异步任务状态追踪（仅内存）。
 *
 * 用途：排查 webhook “已 ACK 但异步失败”的具体原因，辅助支持与联调。
 */
@Service
public class PriceLabsWebhookTaskTracker {

    public enum TaskStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED
    }

    public static final class TaskState {
        private final String traceId;
        private final String endpoint;
        private final TaskStatus status;
        private final LocalDateTime queuedAt;
        private final LocalDateTime startedAt;
        private final LocalDateTime finishedAt;
        private final String message;

        private TaskState(
                String traceId,
                String endpoint,
                TaskStatus status,
                LocalDateTime queuedAt,
                LocalDateTime startedAt,
                LocalDateTime finishedAt,
                String message
        ) {
            this.traceId = traceId;
            this.endpoint = endpoint;
            this.status = status;
            this.queuedAt = queuedAt;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.message = message;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public LocalDateTime getQueuedAt() {
            return queuedAt;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public LocalDateTime getFinishedAt() {
            return finishedAt;
        }

        public String getMessage() {
            return message;
        }
    }

    private final Map<String, TaskState> states = new ConcurrentHashMap<>();

    public void markPending(String traceId, String endpoint) {
        LocalDateTime now = LocalDateTime.now();
        states.put(traceId, new TaskState(traceId, endpoint, TaskStatus.PENDING, now, null, null, null));
    }

    public void markRunning(String traceId) {
        TaskState prev = states.get(traceId);
        LocalDateTime now = LocalDateTime.now();
        if (prev == null) {
            states.put(traceId, new TaskState(traceId, "sync", TaskStatus.RUNNING, now, now, null, null));
            return;
        }
        states.put(traceId, new TaskState(prev.traceId, prev.endpoint, TaskStatus.RUNNING, prev.queuedAt, now, null, null));
    }

    public void markSuccess(String traceId, String message) {
        TaskState prev = states.get(traceId);
        LocalDateTime now = LocalDateTime.now();
        if (prev == null) {
            states.put(traceId, new TaskState(traceId, "sync", TaskStatus.SUCCESS, now, now, now, message));
            return;
        }
        states.put(traceId, new TaskState(prev.traceId, prev.endpoint, TaskStatus.SUCCESS, prev.queuedAt, prev.startedAt, now, message));
    }

    public void markFailed(String traceId, String message) {
        TaskState prev = states.get(traceId);
        LocalDateTime now = LocalDateTime.now();
        if (prev == null) {
            states.put(traceId, new TaskState(traceId, "sync", TaskStatus.FAILED, now, now, now, message));
            return;
        }
        states.put(traceId, new TaskState(prev.traceId, prev.endpoint, TaskStatus.FAILED, prev.queuedAt, prev.startedAt, now, message));
    }

    public Optional<TaskState> get(String traceId) {
        return Optional.ofNullable(states.get(traceId));
    }
}

