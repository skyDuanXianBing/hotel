package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.OrderBoxDTO;
import server.demo.entity.Channel;
import server.demo.entity.OrderBox;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.OrderBoxRepository;
import server.demo.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderBoxServiceByReservationTest {

    @Test
    void getOrderBoxItemByReservationId_shouldReturnDtoWhenPresent() {
        OrderBoxRepository orderBoxRepository = mock(OrderBoxRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);

        Channel channel = new Channel();
        channel.setId(1L);
        channel.setName("Direct");

        Reservation reservation = new Reservation();
        reservation.setId(88L);
        reservation.setOrderNumber("ORD-88");
        reservation.setGuestName("Guest");
        reservation.setChannel(channel);
        reservation.setCheckInDate(LocalDate.of(2026, 7, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 7, 2));
        reservation.setStatus(ReservationStatus.CONFIRMED);

        OrderBox orderBox = new OrderBox();
        orderBox.setId(5L);
        orderBox.setReservation(reservation);
        orderBox.setMovedInAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        orderBox.setNotes("parked");

        when(orderBoxRepository.findByReservationId(88L)).thenReturn(Optional.of(orderBox));

        OrderBoxService service = new OrderBoxService();
        ReflectionTestUtils.setField(service, "orderBoxRepository", orderBoxRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);

        OrderBoxDTO result = service.getOrderBoxItemByReservationId(88L);

        assertEquals(5L, result.getId());
        assertEquals(88L, result.getReservationId());
        assertEquals("ORD-88", result.getReservation().getOrderNumber());
        assertEquals("parked", result.getNotes());
    }

    @Test
    void getOrderBoxItemByReservationId_shouldReturnNullWhenMissing() {
        OrderBoxRepository orderBoxRepository = mock(OrderBoxRepository.class);
        when(orderBoxRepository.findByReservationId(99L)).thenReturn(Optional.empty());

        OrderBoxService service = new OrderBoxService();
        ReflectionTestUtils.setField(service, "orderBoxRepository", orderBoxRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", mock(ReservationRepository.class));

        assertNull(service.getOrderBoxItemByReservationId(99L));
    }
}
