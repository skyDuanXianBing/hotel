package server.demo.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Su Webhook 幂等处理（基于 Redis）。
 *
 * 目标：同一个 reservation_notif_id 重复推送时，避免重复触发拉单/ack。
 * - processing：短 TTL，防止并发重复处理
 * - done：长 TTL，表示已完成处理
 */
@Service
public class SuWebhookIdempotencyService {

    private static final Duration PROCESSING_TTL = Duration.ofMinutes(10);
    private static final Duration DONE_TTL = Duration.ofDays(7);

    private static final String PREFIX = "su:webhook:reservation-notif:";

    private final StringRedisTemplate redisTemplate;

    public SuWebhookIdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Set<String> markProcessingAndReturnNew(String hotelId, List<String> notifIds) {
        if (hotelId == null || hotelId.isBlank() || notifIds == null || notifIds.isEmpty()) {
            return Set.of();
        }

        Set<String> toProcess = new HashSet<>();
        for (String raw : notifIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String notifId = raw.trim();
            String key = key(hotelId, notifId);

            String existing = redisTemplate.opsForValue().get(key);
            if ("done".equalsIgnoreCase(existing)) {
                continue;
            }

            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "processing", PROCESSING_TTL);
            if (Boolean.TRUE.equals(ok)) {
                toProcess.add(notifId);
            }
        }
        return toProcess;
    }

    public void markDone(String hotelId, Set<String> notifIds) {
        if (hotelId == null || hotelId.isBlank() || notifIds == null || notifIds.isEmpty()) {
            return;
        }
        for (String notifId : notifIds) {
            if (notifId == null || notifId.isBlank()) {
                continue;
            }
            redisTemplate.opsForValue().set(key(hotelId, notifId.trim()), "done", DONE_TTL);
        }
    }

    public void clearProcessing(String hotelId, Set<String> notifIds) {
        if (hotelId == null || hotelId.isBlank() || notifIds == null || notifIds.isEmpty()) {
            return;
        }
        for (String notifId : notifIds) {
            if (notifId == null || notifId.isBlank()) {
                continue;
            }
            redisTemplate.delete(key(hotelId, notifId.trim()));
        }
    }

    private static String key(String hotelId, String notifId) {
        return PREFIX + hotelId.trim() + ":" + notifId.trim();
    }
}

