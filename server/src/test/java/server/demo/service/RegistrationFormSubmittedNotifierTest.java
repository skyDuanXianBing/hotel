package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.Notification;
import server.demo.entity.RegistrationForm;
import server.demo.entity.Reservation;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.enums.NotificationType;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.NotificationRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.service.push.PushDispatchService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationFormSubmittedNotifierTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private StoreUserRepository storeUserRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private PushDispatchService pushDispatchService;

    private RegistrationFormSubmittedNotifier notifier;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-10T05:00:00Z"), ZoneOffset.UTC);
        notifier = new RegistrationFormSubmittedNotifier(
                notificationRepository, storeUserRepository, permissionService, pushDispatchService, clock);
        lenient().when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
    }

    @Test
    void notifySubmitted_shouldCreateTaskNotificationsForReviewersAndDispatchPush() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(
                storeUser(101L),
                storeUser(102L)
        ));
        when(permissionService.hasPermission(eq(7L), eq(101L), eq(PermissionModule.STATISTICS), eq(PermissionAction.VIEW_STATS)))
                .thenReturn(true);

        notifier.notifySubmitted(7L, form(55L), reservation("Lin", "RSV-1"));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(101L, saved.get(0).getUserId());
        assertEquals(NotificationType.TASK.name(), saved.get(0).getNotificationType());
        assertEquals(55L, saved.get(0).getRelatedId());
        assertEquals(LocalDateTime.of(2026, 4, 10, 5, 0), saved.get(0).getCreatedAt());

        verify(pushDispatchService).dispatchToUsers(
                org.mockito.ArgumentMatchers.argThat(ids -> Set.copyOf(ids).equals(Set.of(101L))),
                eq(PushDispatchService.PushCategory.TASK),
                eq(saved.get(0).getTitle()),
                eq(saved.get(0).getContent()),
                org.mockito.ArgumentMatchers.argThat(data ->
                        "task".equals(data.get("type")) && "55".equals(data.get("formId")) && "RSV-1".equals(data.get("orderNumber")))
        );
    }

    @Test
    void notifySubmitted_shouldFallbackToAllMembersWhenNoReviewer() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(
                storeUser(101L),
                storeUser(102L)
        ));

        notifier.notifySubmitted(7L, form(55L), reservation("Lin", "RSV-1"));

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        Set<Long> userIds = captor.getValue().stream().map(Notification::getUserId).collect(Collectors.toSet());
        assertEquals(Set.of(101L, 102L), userIds);
    }

    @Test
    void notifySubmitted_shouldDoNothingWhenNoMembers() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of());

        notifier.notifySubmitted(7L, form(55L), reservation("Lin", "RSV-1"));

        verify(notificationRepository, never()).saveAll(any());
        verify(pushDispatchService, never()).dispatchToUsers(anyCollection(), any(), anyString(), anyString(), anyMap());
    }

    private StoreUser storeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        StoreUser storeUser = new StoreUser();
        storeUser.setUser(user);
        storeUser.setIsActive(true);
        return storeUser;
    }

    private RegistrationForm form(Long id) {
        RegistrationForm form = new RegistrationForm();
        form.setId(id);
        return form;
    }

    private Reservation reservation(String guestName, String orderNumber) {
        Reservation reservation = new Reservation();
        reservation.setGuestName(guestName);
        reservation.setOrderNumber(orderNumber);
        return reservation;
    }
}
