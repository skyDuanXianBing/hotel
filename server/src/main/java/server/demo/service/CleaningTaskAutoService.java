package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.CleaningTaskGenerateResult;
import server.demo.entity.CleaningTask;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.ReservationRepository;
import server.demo.util.StoreContextUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 预订驱动的保洁任务自动生成/删除
 */
@Service
public class CleaningTaskAutoService {

    private static final EnumSet<ReservationStatus> ACTIVE_RESERVATION_STATUSES =
            EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.REQUESTED, ReservationStatus.CHECKED_IN);

    private static final List<String> EXPIRE_STATUSES = List.of("pending", "assigned", "in_progress");

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Transactional
    public CleaningTaskGenerateResult generateTasksForRange(LocalDate startDate, LocalDate endDate) {
        Long storeId = StoreContextUtils.requireStoreId();
        List<Reservation> reservations = reservationRepository
                .findByStoreIdAndCheckOutDateBetweenAndStatusIn(storeId, startDate, endDate, ACTIVE_RESERVATION_STATUSES);

        int processed = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (Reservation reservation : reservations) {
            processed++;
            SyncAction action = syncTaskForReservation(reservation);
            switch (action) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                default -> skipped++;
            }
        }

        return new CleaningTaskGenerateResult(processed, created, updated, skipped);
    }

    @Transactional
    public SyncAction syncTaskForReservation(Reservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return SyncAction.SKIPPED;
        }

        if (!ACTIVE_RESERVATION_STATUSES.contains(reservation.getStatus()) || reservation.getRoom() == null) {
            return deleteTasksByReservationId(reservation.getId());
        }

        LocalDate taskDate = reservation.getCheckOutDate();
        if (taskDate == null) {
            return SyncAction.SKIPPED;
        }

        List<CleaningTask> tasks = cleaningTaskRepository.findByReservationId(reservation.getId());
        if (tasks.isEmpty()) {
            CleaningTask task = buildReservationTask(reservation);
            cleaningTaskRepository.save(task);
            return SyncAction.CREATED;
        }

        CleaningTask task = tasks.get(0);
        if (tasks.size() > 1) {
            List<CleaningTask> extras = new ArrayList<>(tasks.subList(1, tasks.size()));
            cleaningTaskRepository.deleteAll(extras);
        }

        boolean dateChanged = !taskDate.equals(task.getTaskDate());
        boolean roomChanged = task.getRoom() == null || !task.getRoom().getId().equals(reservation.getRoom().getId());

        task.setTaskDate(taskDate);
        task.setRoom(reservation.getRoom());
        task.setTaskType("checkout");
        task.setReservationId(reservation.getId());
        task.setSource("reservation");

        if (dateChanged || roomChanged) {
            resetTaskIfNeeded(task);
        }

        cleaningTaskRepository.save(task);
        return SyncAction.UPDATED;
    }

    @Transactional
    public int markExpiredTasks(Long storeId, LocalDate today) {
        if (storeId == null || today == null) {
            return 0;
        }
        return cleaningTaskRepository.markExpiredTasks(storeId, today, EXPIRE_STATUSES);
    }

    private CleaningTask buildReservationTask(Reservation reservation) {
        CleaningTask task = new CleaningTask();
        task.setTaskDate(reservation.getCheckOutDate());
        task.setRoom(reservation.getRoom());
        task.setTaskType("checkout");
        task.setStatus("pending");
        task.setReservationId(reservation.getId());
        task.setSource("reservation");
        return task;
    }

    private SyncAction deleteTasksByReservationId(Long reservationId) {
        List<CleaningTask> tasks = cleaningTaskRepository.findByReservationId(reservationId);
        if (tasks.isEmpty()) {
            return SyncAction.SKIPPED;
        }
        cleaningTaskRepository.deleteAll(tasks);
        return SyncAction.DELETED;
    }

    private void resetTaskIfNeeded(CleaningTask task) {
        if ("completed".equals(task.getStatus()) || "expired".equals(task.getStatus())) {
            task.setStatus("pending");
            task.setCleaner(null);
            task.setStartTime(null);
            task.setCompleteTime(null);
            task.setApprover(null);
        }
    }

    public enum SyncAction {
        CREATED,
        UPDATED,
        DELETED,
        SKIPPED
    }
}
