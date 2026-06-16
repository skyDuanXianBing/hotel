package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.CleaningTaskGenerateResult;
import server.demo.entity.CleaningTask;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.service.helper.util.ReservationOccupancyProjection;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private StoreRepository storeRepository;

    @Transactional
    public CleaningTaskGenerateResult generateTasksForRange(LocalDate startDate, LocalDate endDate) {
        Long storeId = StoreContextUtils.requireStoreId();
        ZoneId storeZoneId = resolveStoreZoneId(storeId);
        List<Reservation> reservations = findCleaningTaskCandidates(storeId, startDate, endDate, storeZoneId);

        int processed = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (Reservation reservation : reservations) {
            LocalDate taskDate = resolveTaskDate(reservation, storeZoneId);
            if (taskDate == null || taskDate.isBefore(startDate) || taskDate.isAfter(endDate)) {
                skipped++;
                continue;
            }
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

        if (reservation.getRoom() == null) {
            return deleteTasksByReservationId(reservation.getId());
        }

        ZoneId storeZoneId = resolveStoreZoneId(reservation.getStoreId());
        LocalDate taskDate = resolveTaskDate(reservation, storeZoneId);
        if (taskDate == null) {
            return deleteTasksByReservationId(reservation.getId());
        }

        return upsertReservationTask(reservation, taskDate);
    }

    /**
     * 退房场景兜底：即使订单状态已变更为 CHECKED_OUT，也确保退房日有任务。
     */
    @Transactional
    public SyncAction ensureCheckoutTaskForReservation(Reservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return SyncAction.SKIPPED;
        }
        if (reservation.getRoom() == null) {
            return SyncAction.SKIPPED;
        }
        LocalDate taskDate = resolveTaskDate(reservation, resolveStoreZoneId(reservation.getStoreId()));
        if (taskDate == null) {
            return SyncAction.SKIPPED;
        }
        return upsertReservationTask(reservation, taskDate);
    }

    private List<Reservation> findCleaningTaskCandidates(
            Long storeId,
            LocalDate startDate,
            LocalDate endDate,
            ZoneId storeZoneId
    ) {
        Map<Long, Reservation> byId = new LinkedHashMap<>();
        List<Reservation> plannedReservations = reservationRepository
                .findByStoreIdAndCheckOutDateBetweenAndStatusIn(
                        storeId,
                        startDate,
                        endDate,
                        ACTIVE_RESERVATION_STATUSES
                );
        addById(byId, plannedReservations);

        List<Reservation> checkedOutFallbackReservations = reservationRepository
                .findByStoreIdAndCheckOutDateBetweenAndStatusIn(
                        storeId,
                        startDate,
                        endDate,
                        EnumSet.of(ReservationStatus.CHECKED_OUT)
                );
        addById(byId, checkedOutFallbackReservations);

        LocalDateTime actualStart = StoreTimeZoneUtil.toReservationTimestampStorageLocalDateTime(
                startDate,
                LocalTime.MIDNIGHT,
                storeZoneId
        );
        LocalDateTime actualEnd = StoreTimeZoneUtil.toReservationTimestampStorageLocalDateTime(
                endDate.plusDays(1),
                LocalTime.MIDNIGHT,
                storeZoneId
        );
        List<Reservation> actualCheckoutReservations = reservationRepository
                .findByStoreIdAndActualCheckOutBetween(storeId, actualStart, actualEnd);
        addById(byId, actualCheckoutReservations);

        return new ArrayList<>(byId.values());
    }

    private void addById(Map<Long, Reservation> byId, List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return;
        }
        for (Reservation reservation : reservations) {
            if (reservation == null || reservation.getId() == null) {
                continue;
            }
            byId.putIfAbsent(reservation.getId(), reservation);
        }
    }

    private LocalDate resolveTaskDate(Reservation reservation, ZoneId storeZoneId) {
        if (reservation == null) {
            return null;
        }
        ReservationStatus status = reservation.getStatus();
        if (ACTIVE_RESERVATION_STATUSES.contains(status)) {
            return reservation.getCheckOutDate();
        }
        if (status == ReservationStatus.CHECKED_OUT) {
            LocalDate actualDate = ReservationOccupancyProjection.toStoreLocalDate(
                    reservation.getActualCheckOut(),
                    storeZoneId
            );
            return actualDate != null ? actualDate : reservation.getCheckOutDate();
        }
        return null;
    }

    private ZoneId resolveStoreZoneId(Long storeId) {
        Store store = storeId == null || storeRepository == null ? null : storeRepository.findById(storeId).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
    }

    private SyncAction upsertReservationTask(Reservation reservation, LocalDate taskDate) {
        List<CleaningTask> tasks = cleaningTaskRepository.findByReservationId(reservation.getId());
        if (tasks.isEmpty()) {
            CleaningTask task = buildReservationTask(reservation, taskDate);
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

    private CleaningTask buildReservationTask(Reservation reservation, LocalDate taskDate) {
        CleaningTask task = new CleaningTask();
        task.setTaskDate(taskDate);
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
