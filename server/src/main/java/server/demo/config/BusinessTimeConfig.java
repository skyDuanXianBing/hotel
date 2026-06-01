package server.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import server.demo.util.StoreTimeZoneUtil;

import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(BusinessTimeProperties.class)
public class BusinessTimeConfig {

    @Bean
    @Primary
    public ZoneId businessDefaultZoneId(BusinessTimeProperties properties) {
        ZoneId defaultZoneId = properties.resolveDefaultZoneId();
        StoreTimeZoneUtil.setBusinessDefaultZoneId(defaultZoneId);
        return defaultZoneId;
    }

    @Bean
    public ZoneId reservationTimestampStorageZoneId(BusinessTimeProperties properties) {
        ZoneId storageZoneId = properties.resolveReservationTimestampStorageZoneId();
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(storageZoneId);
        return storageZoneId;
    }
}
