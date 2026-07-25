package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.demo.entity.Reservation;
import server.demo.entity.Room;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class IndependentSiteReservationLifecycleService {

    private static final Logger logger =
            LoggerFactory.getLogger(IndependentSiteReservationLifecycleService.class);

    public enum Event {
        HOLD_CREATED,
        PAYMENT_SUCCEEDED,
        PAYMENT_RELEASED
    }

    private final CleaningTaskAutoService cleaningTaskAutoService;
    private final PriceLabsReservationSyncService priceLabsReservationSyncService;
    private final Optional<PriceLabsCalendarSyncDebouncer> priceLabsCalendarSyncDebouncer;
    private final Optional<SuAriAutoSyncService> suAriAutoSyncService;
    private final AutoMessageTriggerService autoMessageTriggerService;
    private final OrderNotificationDispatchService orderNotificationDispatchService;

    public IndependentSiteReservationLifecycleService(
            CleaningTaskAutoService cleaningTaskAutoService,
            PriceLabsReservationSyncService priceLabsReservationSyncService,
            Optional<PriceLabsCalendarSyncDebouncer> priceLabsCalendarSyncDebouncer,
            Optional<SuAriAutoSyncService> suAriAutoSyncService,
            AutoMessageTriggerService autoMessageTriggerService,
            OrderNotificationDispatchService orderNotificationDispatchService
    ) {
        this.cleaningTaskAutoService = cleaningTaskAutoService;
        this.priceLabsReservationSyncService = priceLabsReservationSyncService;
        this.priceLabsCalendarSyncDebouncer = priceLabsCalendarSyncDebouncer;
        this.suAriAutoSyncService = suAriAutoSyncService;
        this.autoMessageTriggerService = autoMessageTriggerService;
        this.orderNotificationDispatchService = orderNotificationDispatchService;
    }

    public void onChanged(List<Reservation> reservations, Event event, Long fallbackUserId) {
        if (reservations == null || reservations.isEmpty()) {
            return;
        }
        for (Reservation reservation : reservations) {
            cleaningTaskAutoService.syncTaskForReservation(reservation);
            requestPriceLabsCalendarSync(reservation);
        }

        Runnable afterCommit = () -> {
            pushPriceLabsReservations(reservations);
            enqueueSuAvailability(reservations, event);
            if (event == Event.PAYMENT_SUCCEEDED) {
                notifyCreated(reservations, fallbackUserId);
                Long storeId = reservations.get(0).getStoreId();
                autoMessageTriggerService.dispatchStoreOnce(storeId);
            }
        };
        executeAfterCommit(afterCommit);
    }

    private void requestPriceLabsCalendarSync(Reservation reservation) {
        if (reservation == null || reservation.getRoom() == null
                || reservation.getRoom().getRoomType() == null) {
            return;
        }
        LocalDate checkIn = reservation.getCheckInDate();
        LocalDate checkOut = reservation.getCheckOutDate();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return;
        }
        priceLabsCalendarSyncDebouncer.ifPresent(debouncer -> debouncer.requestSyncAfterCommit(
                reservation.getStoreId(),
                reservation.getRoom().getRoomType().getId(),
                checkIn,
                checkOut.minusDays(1)
        ));
    }

    private void pushPriceLabsReservations(List<Reservation> reservations) {
        for (Reservation reservation : reservations) {
            if (reservation == null || reservation.getId() == null) {
                continue;
            }
            try {
                priceLabsReservationSyncService.pushReservationById(
                        reservation.getStoreId(),
                        reservation.getId()
                );
            } catch (Exception e) {
                logger.warn(
                        "Independent-site PriceLabs reservation sync failed. storeId={}, reservationId={}, error={}",
                        reservation.getStoreId(),
                        reservation.getId(),
                        e.getMessage()
                );
            }
        }
    }

    private void enqueueSuAvailability(List<Reservation> reservations, Event event) {
        if (suAriAutoSyncService.isEmpty()) {
            return;
        }
        Long storeId = reservations.get(0).getStoreId();
        Set<Long> roomTypeIds = new LinkedHashSet<>();
        List<SuAriAutoSyncService.DateRange> ranges = new ArrayList<>();
        for (Reservation reservation : reservations) {
            if (reservation == null) {
                continue;
            }
            Room room = reservation.getRoom();
            if (room != null && room.getRoomType() != null && room.getRoomType().getId() != null) {
                roomTypeIds.add(room.getRoomType().getId());
            }
            if (reservation.getCheckInDate() != null
                    && reservation.getCheckOutDate() != null
                    && reservation.getCheckOutDate().isAfter(reservation.getCheckInDate())) {
                ranges.add(new SuAriAutoSyncService.DateRange(
                        reservation.getCheckInDate(),
                        reservation.getCheckOutDate().minusDays(1)
                ));
            }
        }
        if (ranges.isEmpty()) {
            return;
        }
        try {
            suAriAutoSyncService.get().enqueueForStoreDateRanges(
                    storeId,
                    "independent_site_" + event.name().toLowerCase(java.util.Locale.ROOT),
                    ranges,
                    roomTypeIds.isEmpty() ? null : roomTypeIds,
                    null,
                    true,
                    false,
                    false,
                    false
            );
        } catch (Exception e) {
            logger.warn(
                    "Independent-site Su availability sync enqueue failed. storeId={}, event={}, error={}",
                    storeId,
                    event,
                    e.getMessage()
            );
        }
    }

    private void notifyCreated(List<Reservation> reservations, Long fallbackUserId) {
        for (Reservation reservation : reservations) {
            orderNotificationDispatchService.notifyOrderCreated(
                    reservation.getStoreId(),
                    reservation,
                    fallbackUserId
            );
        }
    }

    private static void executeAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
