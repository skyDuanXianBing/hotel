package server.demo.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.User;
import server.demo.enums.ChannelType;
import server.demo.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfEnvironmentVariable(
        named = "RUN_DB_INTEGRATION_TESTS",
        matches = "true",
        disabledReason = "Requires a disposable local MySQL database. "
                + "Set RUN_DB_INTEGRATION_TESTS=true with a local DB_URL to run."
)
@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.config.import=optional:file:./.env[.properties],optional:file:./server/.env[.properties]",
        "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/booking_system_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:123456}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.sql.init.mode=never",
        "ota.sync.enabled=false",
        "ota.reservation-sync.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationRepositoryPendingDataTest {

    private static final Long STORE_ID = 47001L;
    private static final Long OTHER_STORE_ID = 47002L;
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 6, 12);
    private static final String RUN_ID = Long.toString(System.nanoTime(), 36);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EntityManager entityManager;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Test
    void pendingQueries_shouldApplyBusinessDateExclusionsAndStoreScopeToRealRows() {
        assumeTrue(isLocalDatasource(datasourceUrl), "Pending repository data test only runs against local DB URLs");

        User user = createUser();
        Channel directChannel = createChannel(STORE_ID, "DIRECT", ChannelType.DIRECT);
        Channel otaChannel = createChannel(STORE_ID, "OTA", ChannelType.OTA);
        Channel otherStoreChannel = createChannel(OTHER_STORE_ID, "OTHER", ChannelType.DIRECT);

        Reservation manualPending = createReservation(
                user,
                directChannel,
                STORE_ID,
                "MANUAL-PENDING",
                BUSINESS_DATE.minusDays(1),
                BUSINESS_DATE,
                null,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(20),
                false
        );
        createReservation(
                user,
                otaChannel,
                STORE_ID,
                "SU-OTA",
                BUSINESS_DATE.minusDays(1),
                BUSINESS_DATE.plusDays(1),
                "SU-RES-1",
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                false
        );
        createReservation(
                user,
                otaChannel,
                STORE_ID,
                "GENERIC-OTA-NO-SU",
                BUSINESS_DATE.minusDays(1),
                BUSINESS_DATE.plusDays(1),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                false
        );
        createReservation(
                user,
                directChannel,
                STORE_ID,
                "OLD-CHECKOUT",
                BUSINESS_DATE.minusDays(3),
                BUSINESS_DATE.minusDays(1),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                false
        );
        createReservation(
                user,
                directChannel,
                STORE_ID,
                "FULLY-PAID",
                BUSINESS_DATE,
                BUSINESS_DATE.plusDays(1),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                false
        );
        createReservation(
                user,
                directChannel,
                STORE_ID,
                "MANUAL-SETTLED",
                BUSINESS_DATE,
                BUSINESS_DATE.plusDays(1),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                true
        );
        createReservation(
                user,
                otherStoreChannel,
                OTHER_STORE_ID,
                "OTHER-STORE-PENDING",
                BUSINESS_DATE,
                BUSINESS_DATE.plusDays(1),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                false
        );
        flushAndClear();

        assertEquals(1L, reservationRepository.countPendingOrdersByStoreId(STORE_ID, BUSINESS_DATE));
        assertEquals(
                List.of(manualPending.getOrderNumber()),
                orderNumbers(reservationRepository.findPendingOrdersByStoreId(STORE_ID, BUSINESS_DATE))
        );
        assertEquals(
                List.of(manualPending.getOrderNumber()),
                orderNumbers(reservationRepository.findPendingOrdersWithDetailsByStoreId(STORE_ID, BUSINESS_DATE))
        );
        assertEquals(1L, reservationRepository.countPendingOrdersByStoreId(OTHER_STORE_ID, BUSINESS_DATE));
    }

    private User createUser() {
        User user = new User(unique("pending-user"), unique("pending") + "@example.com", "password");
        entityManager.persist(user);
        return user;
    }

    private Channel createChannel(Long storeId, String codePrefix, ChannelType type) {
        Channel channel = new Channel("Pending " + codePrefix, unique(codePrefix), type);
        channel.setStoreId(storeId);
        entityManager.persist(channel);
        return channel;
    }

    private Reservation createReservation(
            User user,
            Channel channel,
            Long storeId,
            String orderPrefix,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String suReservationId,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            boolean settled
    ) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setChannel(channel);
        reservation.setStoreId(storeId);
        reservation.setGuestName("Guest " + orderPrefix);
        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkOutDate);
        reservation.setAdults(2);
        reservation.setTotalAmount(totalAmount);
        reservation.setPaidAmount(paidAmount);
        reservation.setSettled(settled);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setOrderNumber(unique(orderPrefix));
        reservation.setSuReservationId(suReservationId);
        entityManager.persist(reservation);
        return reservation;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static List<String> orderNumbers(List<Reservation> reservations) {
        return reservations.stream()
                .map(Reservation::getOrderNumber)
                .toList();
    }

    private static boolean isLocalDatasource(String value) {
        if (value == null) {
            return false;
        }
        return value.contains("//localhost:") || value.contains("//127.0.0.1:");
    }

    private static String unique(String value) {
        return value + "-" + RUN_ID;
    }
}
