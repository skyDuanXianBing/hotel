package server.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import server.demo.util.StoreTimeZoneUtil;

import java.time.ZoneId;

/**
 * Business time settings that are separate from JDBC/database timezone settings.
 */
@ConfigurationProperties(prefix = "app.time")
public class BusinessTimeProperties {

    private String defaultZone = StoreTimeZoneUtil.DEFAULT_TIMEZONE;
    private String reservationTimestampStorageZone = StoreTimeZoneUtil.DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE;

    public String getDefaultZone() {
        return defaultZone;
    }

    public void setDefaultZone(String defaultZone) {
        this.defaultZone = defaultZone;
    }

    public ZoneId resolveDefaultZoneId() {
        return StoreTimeZoneUtil.resolveDefaultZoneId(defaultZone);
    }

    public String getReservationTimestampStorageZone() {
        return reservationTimestampStorageZone;
    }

    public void setReservationTimestampStorageZone(String reservationTimestampStorageZone) {
        this.reservationTimestampStorageZone = reservationTimestampStorageZone;
    }

    public ZoneId resolveReservationTimestampStorageZoneId() {
        return StoreTimeZoneUtil.resolveReservationTimestampStorageZoneId(reservationTimestampStorageZone);
    }
}
