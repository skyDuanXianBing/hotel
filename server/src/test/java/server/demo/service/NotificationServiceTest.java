package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.Notification;
import server.demo.repository.NotificationRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        Clock utcClock = Clock.fixed(Instant.parse("2026-04-08T05:00:00Z"), ZoneOffset.UTC);
        notificationService = new NotificationService(notificationRepository, utcClock);
    }

    @Test
    void createNotification_shouldSetUtcCreatedAtWhenMissing() {
        Notification notification = new Notification(1L, "SYSTEM", "title", "content");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = notificationService.createNotification(notification);

        assertEquals(LocalDateTime.of(2026, 4, 8, 5, 0), saved.getCreatedAt());
    }

    @Test
    void markAsRead_shouldSetUtcReadAt() {
        Notification notification = new Notification(1L, "ORDER", "title", "content");
        notification.setId(9L);

        when(notificationRepository.findById(9L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = notificationService.markAsRead(9L);

        assertTrue(Boolean.TRUE.equals(saved.getIsRead()));
        assertEquals(LocalDateTime.of(2026, 4, 8, 5, 0), saved.getReadAt());
    }

    @Test
    void markAllAsRead_shouldUseUtcReadAt() {
        notificationService.markAllAsRead(7L);

        verify(notificationRepository).markAllAsRead(eq(7L), eq(LocalDateTime.of(2026, 4, 8, 5, 0)));
    }

    @Test
    void markAllAsReadByType_shouldUseUtcReadAt() {
        notificationService.markAllAsReadByType(7L, "ORDER");

        verify(notificationRepository).markAllAsReadByType(
                eq(7L),
                eq("ORDER"),
                eq(LocalDateTime.of(2026, 4, 8, 5, 0))
        );
    }

    @Test
    void deleteOldReadNotifications_shouldUseUtcThreshold() {
        notificationService.deleteOldReadNotifications(7L, 30);

        verify(notificationRepository).deleteOldReadNotifications(
                eq(7L),
                eq(LocalDateTime.of(2026, 3, 9, 5, 0))
        );
    }
}
