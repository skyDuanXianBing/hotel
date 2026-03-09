package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.Channel;
import server.demo.entity.Notification;
import server.demo.entity.Reservation;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.enums.ReservationStatus;
import server.demo.repository.NotificationRepository;
import server.demo.repository.StoreUserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderNotificationDispatchServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private StoreUserRepository storeUserRepository;

    @InjectMocks
    private OrderNotificationDispatchService dispatchService;

    @Test
    void notifyOrderCreated_shouldDispatchToAllActiveStoreMembers() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of(
                storeUser(101L),
                storeUser(102L),
                storeUser(101L)
        ));

        Reservation reservation = reservation("Booking.com", "Lin", "BK-123", ReservationStatus.CONFIRMED);
        dispatchService.notifyOrderCreated(7L, reservation, 999L);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        Set<Long> userIds = saved.stream().map(Notification::getUserId).collect(Collectors.toSet());
        assertEquals(Set.of(101L, 102L), userIds);
        assertEquals("订单创建", saved.get(0).getTitle());
        assertEquals("ORDER", saved.get(0).getNotificationType());
    }

    @Test
    void notifyOrderCancelled_shouldFallbackToOwnerWhenNoActiveMembers() {
        when(storeUserRepository.findActiveUsersByStoreId(7L)).thenReturn(List.of());

        Reservation reservation = reservation("Airbnb", "Aki", "AB-321", ReservationStatus.CANCELLED);
        dispatchService.notifyOrderCancelled(7L, reservation, 9L);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();

        assertEquals(1, saved.size());
        assertEquals(9L, saved.get(0).getUserId());
        assertEquals("订单取消", saved.get(0).getTitle());
    }

    @Test
    void resolveOtaEventType_shouldReturnExpectedEvent() {
        Reservation reservation = reservation("Booking.com", "Tom", "B-1", ReservationStatus.CONFIRMED);

        OrderNotificationDispatchService.OrderEventType created = dispatchService.resolveOtaEventType(
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                reservation
        );
        assertEquals(OrderNotificationDispatchService.OrderEventType.CREATED, created);

        reservation.setStatus(ReservationStatus.CANCELLED);
        OrderNotificationDispatchService.OrderEventType cancelled = dispatchService.resolveOtaEventType(
                false,
                "Tom",
                null,
                "B-1",
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2),
                ReservationStatus.CONFIRMED,
                reservation
        );
        assertEquals(OrderNotificationDispatchService.OrderEventType.CANCELLED, cancelled);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setGuestName("Jerry");
        OrderNotificationDispatchService.OrderEventType updated = dispatchService.resolveOtaEventType(
                false,
                "Tom",
                null,
                "B-1",
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2),
                ReservationStatus.CONFIRMED,
                reservation
        );
        assertEquals(OrderNotificationDispatchService.OrderEventType.UPDATED, updated);

        reservation.setGuestName("Tom");
        OrderNotificationDispatchService.OrderEventType unchanged = dispatchService.resolveOtaEventType(
                false,
                "Tom",
                null,
                "B-1",
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2),
                ReservationStatus.CONFIRMED,
                reservation
        );
        assertNull(unchanged);
    }

    private StoreUser storeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        StoreUser storeUser = new StoreUser();
        storeUser.setUser(user);
        storeUser.setIsActive(true);
        return storeUser;
    }

    private Reservation reservation(String channelName, String guestName, String channelOrderNumber, ReservationStatus status) {
        Channel channel = new Channel();
        channel.setName(channelName);

        Reservation reservation = new Reservation();
        reservation.setId(123L);
        reservation.setChannel(channel);
        reservation.setGuestName(guestName);
        reservation.setChannelOrderNumber(channelOrderNumber);
        reservation.setOrderNumber("RSV-123");
        reservation.setStatus(status);
        reservation.setCheckInDate(LocalDate.of(2026, 3, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 3, 2));
        return reservation;
    }
}
