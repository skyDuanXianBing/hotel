package server.demo.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Component
public class HomeWorkbenchCursorCodec {
    private static final int VERSION = 1;
    private static final String SEPARATOR = "|";
    private static final int MAX_CURSOR_LENGTH = 512;
    private static final int MAX_DECODED_LENGTH = 384;

    public String encode(String queryContext, SortKey key) {
        validateKey(key);
        String dueAt = key.dueAt() == null ? "" : key.dueAt().toString();
        String payload = VERSION + SEPARATOR + contextHash(queryContext) + SEPARATOR
                + key.priorityRank() + SEPARATOR + key.dueAtNullRank() + SEPARATOR + dueAt + SEPARATOR
                + key.typeRank() + SEPARATOR + key.sourceId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public SortKey decode(String cursor, String queryContext) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        if (cursor.length() > MAX_CURSOR_LENGTH) {
            throw invalidCursor();
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            if (decoded.length > MAX_DECODED_LENGTH) {
                throw invalidCursor();
            }
            String payload = new String(decoded, StandardCharsets.UTF_8);
            String[] values = payload.split("\\|", -1);
            if (values.length != 7 || Integer.parseInt(values[0]) != VERSION) {
                throw invalidCursor();
            }
            if (!MessageDigest.isEqual(
                    values[1].getBytes(StandardCharsets.UTF_8),
                    contextHash(queryContext).getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("游标与当前门店或筛选条件不匹配");
            }
            LocalDateTime dueAt = values[4].isBlank() ? null : LocalDateTime.parse(values[4]);
            SortKey key = new SortKey(
                    Integer.parseInt(values[2]),
                    Integer.parseInt(values[3]),
                    dueAt,
                    Integer.parseInt(values[5]),
                    Long.parseLong(values[6])
            );
            validateKey(key);
            return key;
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegalArgumentException
                    && exception.getMessage() != null
                    && exception.getMessage().startsWith("游标")) {
                throw illegalArgumentException;
            }
            throw invalidCursor();
        }
    }

    private void validateKey(SortKey key) {
        if (key == null
                || key.priorityRank() < 0 || key.priorityRank() > 3
                || key.dueAtNullRank() < 0 || key.dueAtNullRank() > 1
                || key.typeRank() < 1 || key.typeRank() > 5
                || key.sourceId() <= 0
                || (key.dueAtNullRank() == 0 && key.dueAt() == null)
                || (key.dueAtNullRank() == 1 && key.dueAt() != null)) {
            throw invalidCursor();
        }
    }

    private String contextHash(String context) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(context.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法初始化工作台游标", exception);
        }
    }

    private IllegalArgumentException invalidCursor() {
        return new IllegalArgumentException("无效的工作台游标");
    }

    public record SortKey(
            int priorityRank,
            int dueAtNullRank,
            LocalDateTime dueAt,
            int typeRank,
            long sourceId
    ) {
    }
}
