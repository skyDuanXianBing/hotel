package server.demo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageKnowledgeThreadDirtyMarkerTest {

    @Test
    void markDirty_shouldSetDefaultDelayAndCasOnNewerDirtyMessage() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-18T10:00:00Z"));
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        MessageKnowledgeThreadDirtyMarker marker = new MessageKnowledgeThreadDirtyMarker(clock);
        ReflectionTestUtils.setField(marker, "entityManager", entityManager);

        assertTrue(marker.markDirty(26L, 77L, 100L));
        clock.advance(Duration.ofMinutes(5));
        assertTrue(marker.markDirty(26L, 77L, 101L));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, org.mockito.Mockito.times(2)).createNativeQuery(sqlCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("knowledge_dirty_message_id <= :messageId"));

        ArgumentCaptor<LocalDateTime> extractAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(query, org.mockito.Mockito.times(2)).setParameter(eq("extractAfter"), extractAfterCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 6, 18, 10, 30), extractAfterCaptor.getAllValues().get(0));
        assertEquals(LocalDateTime.of(2026, 6, 18, 10, 35), extractAfterCaptor.getAllValues().get(1));

        verify(query, org.mockito.Mockito.times(2)).setParameter(eq("storeId"), eq(26L));
        verify(query, org.mockito.Mockito.times(2)).setParameter(eq("threadId"), eq(77L));
        verify(query).setParameter("messageId", 100L);
        verify(query).setParameter("messageId", 101L);
    }

    @Test
    void markDirty_shouldIgnoreMessagesWithoutThreadIdentity() {
        EntityManager entityManager = mock(EntityManager.class);
        MessageKnowledgeThreadDirtyMarker marker =
                new MessageKnowledgeThreadDirtyMarker(Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(marker, "entityManager", entityManager);

        assertFalse(marker.markDirty(new SuMessage()));
        assertFalse(marker.markDirty(26L, null, 100L));

        verify(entityManager, never()).createNativeQuery(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void markDirty_shouldUseMessageStoreThreadAndId() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        MessageKnowledgeThreadDirtyMarker marker =
                new MessageKnowledgeThreadDirtyMarker(Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(marker, "entityManager", entityManager);

        SuMessageThread thread = new SuMessageThread();
        thread.setId(77L);
        SuMessage message = new SuMessage();
        message.setId(100L);
        message.setStoreId(26L);
        message.setThread(thread);

        assertTrue(marker.markDirty(message));

        verify(query).setParameter("storeId", 26L);
        verify(query).setParameter("threadId", 77L);
        verify(query).setParameter("messageId", 100L);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
