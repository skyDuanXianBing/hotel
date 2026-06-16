package server.demo.service.helper.util;

import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.util.StoreTimeZoneUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

public final class ReservationOccupancyProjection {

    private ReservationOccupancyProjection() {
    }

    public static boolean occupiesDate(
            Reservation reservation,
            LocalDate date,
            Set<ReservationStatus> occupancyStatuses,
            ZoneId storeZoneId
    ) {
        if (reservation == null) {
            return false;
        }
        return occupiesDate(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getStatus(),
                reservation.getActualCheckOut(),
                date,
                occupancyStatuses,
                storeZoneId
        );
    }

    public static boolean occupiesDate(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            ReservationStatus status,
            LocalDateTime actualCheckOut,
            LocalDate date,
            Set<ReservationStatus> occupancyStatuses,
            ZoneId storeZoneId
    ) {
        if (date == null || checkInDate == null || checkOutDate == null || status == null) {
            return false;
        }
        if (occupancyStatuses == null || !occupancyStatuses.contains(status)) {
            return false;
        }

        LocalDate effectiveEndExclusive = resolveEffectiveCheckOutDate(
                checkInDate,
                checkOutDate,
                status,
                actualCheckOut,
                storeZoneId
        );
        if (effectiveEndExclusive == null) {
            return false;
        }

        return !date.isBefore(checkInDate) && date.isBefore(effectiveEndExclusive);
    }

    public static LocalDate resolveEffectiveCheckOutDate(Reservation reservation, ZoneId storeZoneId) {
        if (reservation == null) {
            return null;
        }
        return resolveEffectiveCheckOutDate(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getStatus(),
                reservation.getActualCheckOut(),
                storeZoneId
        );
    }

    public static LocalDate resolveEffectiveCheckOutDate(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            ReservationStatus status,
            LocalDateTime actualCheckOut,
            ZoneId storeZoneId
    ) {
        if (checkInDate == null || checkOutDate == null) {
            return null;
        }
        if (status != ReservationStatus.CHECKED_OUT || actualCheckOut == null) {
            return checkOutDate;
        }

        LocalDate actualCheckOutDate = toStoreLocalDate(actualCheckOut, storeZoneId);
        if (actualCheckOutDate == null) {
            return checkOutDate;
        }
        if (actualCheckOutDate.isAfter(checkOutDate)) {
            return checkOutDate;
        }
        return actualCheckOutDate;
    }

    public static LocalDate toStoreLocalDate(LocalDateTime storageLocalDateTime, ZoneId storeZoneId) {
        if (storageLocalDateTime == null) {
            return null;
        }
        ZoneId resolvedStoreZoneId = storeZoneId != null
                ? storeZoneId
                : StoreTimeZoneUtil.getBusinessDefaultZoneId();
        return storageLocalDateTime
                .atZone(StoreTimeZoneUtil.getReservationTimestampStorageZoneId())
                .withZoneSameInstant(resolvedStoreZoneId)
                .toLocalDate();
    }
}
