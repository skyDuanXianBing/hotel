package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.Cleaner;
import server.demo.entity.CleaningTask;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.User;
import server.demo.enums.ReservationStatus;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.ReservationRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleaningTaskAutoServiceTest {

    @Test
    void syncTaskForReservation_shouldCreateTaskForActiveReservation() {
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        CleaningTaskAutoService service = new CleaningTaskAutoService();
        inject(service, "cleaningTaskRepository", cleaningTaskRepository);
        inject(service, "reservationRepository", reservationRepository);

        Reservation reservation = new Reservation();
        reservation.setId(11L);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(LocalDate.of(2025, 2, 1));
        reservation.setCheckOutDate(LocalDate.of(2025, 2, 2));

        Room room = new Room();
        room.setId(101L);
        reservation.setRoom(room);

        when(cleaningTaskRepository.findByReservationId(11L)).thenReturn(Collections.emptyList());

        CleaningTaskAutoService.SyncAction action = service.syncTaskForReservation(reservation);

        assertEquals(CleaningTaskAutoService.SyncAction.CREATED, action);

        ArgumentCaptor<CleaningTask> captor = ArgumentCaptor.forClass(CleaningTask.class);
        verify(cleaningTaskRepository).save(captor.capture());

        CleaningTask saved = captor.getValue();
        assertEquals(LocalDate.of(2025, 2, 2), saved.getTaskDate());
        assertEquals(room, saved.getRoom());
        assertEquals("checkout", saved.getTaskType());
        assertEquals("pending", saved.getStatus());
        assertEquals(11L, saved.getReservationId());
        assertEquals("reservation", saved.getSource());
    }

    @Test
    void syncTaskForReservation_shouldResetCompletedTaskWhenDateChanged() {
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        CleaningTaskAutoService service = new CleaningTaskAutoService();
        inject(service, "cleaningTaskRepository", cleaningTaskRepository);
        inject(service, "reservationRepository", reservationRepository);

        Reservation reservation = new Reservation();
        reservation.setId(22L);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(LocalDate.of(2025, 3, 10));
        reservation.setCheckOutDate(LocalDate.of(2025, 3, 11));

        Room room = new Room();
        room.setId(202L);
        reservation.setRoom(room);

        CleaningTask existing = new CleaningTask();
        existing.setId(99L);
        existing.setTaskDate(LocalDate.of(2025, 3, 8));
        existing.setStatus("completed");
        existing.setCleaner(new Cleaner());
        existing.setStartTime(LocalDateTime.now().minusHours(2));
        existing.setCompleteTime(LocalDateTime.now().minusHours(1));
        existing.setApprover(new User());

        when(cleaningTaskRepository.findByReservationId(22L)).thenReturn(List.of(existing));

        CleaningTaskAutoService.SyncAction action = service.syncTaskForReservation(reservation);

        assertEquals(CleaningTaskAutoService.SyncAction.UPDATED, action);

        ArgumentCaptor<CleaningTask> captor = ArgumentCaptor.forClass(CleaningTask.class);
        verify(cleaningTaskRepository).save(captor.capture());

        CleaningTask saved = captor.getValue();
        assertEquals(LocalDate.of(2025, 3, 11), saved.getTaskDate());
        assertEquals(room, saved.getRoom());
        assertEquals("checkout", saved.getTaskType());
        assertEquals("pending", saved.getStatus());
        assertEquals(22L, saved.getReservationId());
        assertEquals("reservation", saved.getSource());
        assertNull(saved.getCleaner());
        assertNull(saved.getStartTime());
        assertNull(saved.getCompleteTime());
        assertNull(saved.getApprover());
    }

    @Test
    void syncTaskForReservation_shouldDeleteTasksWhenInactive() {
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        CleaningTaskAutoService service = new CleaningTaskAutoService();
        inject(service, "cleaningTaskRepository", cleaningTaskRepository);
        inject(service, "reservationRepository", reservationRepository);

        Reservation reservation = new Reservation();
        reservation.setId(33L);
        reservation.setStatus(ReservationStatus.CANCELLED);

        CleaningTask existing = new CleaningTask();
        existing.setId(100L);

        when(cleaningTaskRepository.findByReservationId(33L)).thenReturn(List.of(existing));

        CleaningTaskAutoService.SyncAction action = service.syncTaskForReservation(reservation);

        assertEquals(CleaningTaskAutoService.SyncAction.DELETED, action);
        verify(cleaningTaskRepository).deleteAll(any(List.class));
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
