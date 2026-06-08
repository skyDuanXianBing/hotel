package server.demo.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import server.demo.entity.Channel;
import server.demo.entity.RegistrationForm;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomGroupMember;
import server.demo.entity.RoomType;
import server.demo.entity.User;
import server.demo.enums.ChannelType;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnabledIfEnvironmentVariable(
        named = "RUN_DB_INTEGRATION_TESTS",
        matches = "true",
        disabledReason = "Requires a reachable MySQL database. "
                + "Set RUN_DB_INTEGRATION_TESTS=true with DB_URL/DB_USERNAME/DB_PASSWORD to run."
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
class RegistrationFormRepositoryTest {

    private static final Long STORE_ID = 31001L;
    private static final Long OTHER_STORE_ID = 31002L;
    private static final Long ROOM_GROUP_ID = 41001L;
    private static final Long OTHER_ROOM_GROUP_ID = 41002L;
    private static final Long ROOM_OWNER_USER_ID = 51001L;
    private static final LocalDate CHECK_IN_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate CHECK_OUT_DATE = LocalDate.of(2026, 6, 3);
    private static final BigDecimal TOTAL_AMOUNT = BigDecimal.valueOf(1200);
    private static final String RUN_ID = Long.toString(System.nanoTime(), 36);

    @Autowired
    private RegistrationFormRepository registrationFormRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void searchForAdminList_shouldFilterByAssignedRoomNumberAndStoreId() {
        TestFixture fixture = createFixture();
        RegistrationForm room101Form = fixture.createForm("ORD-ROOM-101", "Alice", fixture.room101, null);
        fixture.createForm("ORD-ROOM-102", "Bob", fixture.room102, null);
        fixture.createOtherStoreForm("ORD-OTHER-101", "Other Store Guest", "101");
        flushAndClear();

        List<RegistrationForm> result = registrationFormRepository.searchForAdminList(
                STORE_ID,
                null,
                null,
                null,
                true,
                List.of("101"),
                null,
                null,
                null,
                null,
                null
        );

        assertSingleOrderNumber(room101Form.getOrderNumber(), result);
    }

    @Test
    void searchForAdminList_shouldFilterByOtaRoomNumberFallback() {
        TestFixture fixture = createFixture();
        RegistrationForm otaFallbackForm = fixture.createForm("ORD-OTA-305", "Alice", null, "OTA-305");
        fixture.createForm("ORD-ROOM-101", "Bob", fixture.room101, null);
        flushAndClear();

        List<RegistrationForm> result = registrationFormRepository.searchForAdminList(
                STORE_ID,
                null,
                null,
                null,
                true,
                List.of("OTA-305"),
                null,
                null,
                null,
                null,
                null
        );

        assertSingleOrderNumber(otaFallbackForm.getOrderNumber(), result);
    }

    @Test
    void searchForAdminList_shouldUseIntersectionForRoomGroupIdAndRoomNumber() {
        TestFixture fixture = createFixture();
        RegistrationForm room101Form = fixture.createForm("ORD-GROUP-101", "Alice", fixture.room101, null);
        fixture.createForm("ORD-GROUP-102", "Bob", fixture.room102, null);
        fixture.createForm("ORD-GROUP-OTA", "Carol", null, "OTA-305");
        flushAndClear();

        List<RegistrationForm> result = registrationFormRepository.searchForAdminList(
                STORE_ID,
                null,
                null,
                null,
                true,
                List.of("101", "102", "OTA-305"),
                ROOM_GROUP_ID,
                null,
                null,
                null,
                null
        );

        assertSingleOrderNumber(room101Form.getOrderNumber(), result);
    }

    @Test
    void searchForAdminList_shouldFilterCheckInAndCheckOutRangesInclusively() {
        TestFixture fixture = createFixture();
        RegistrationForm startBoundaryForm = fixture.createForm(
                "ORD-RANGE-START",
                "Alice",
                fixture.room101,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5)
        );
        RegistrationForm endBoundaryForm = fixture.createForm(
                "ORD-RANGE-END",
                "Bob",
                fixture.room102,
                null,
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 2)
        );
        fixture.createForm(
                "ORD-RANGE-CHECKIN-BEFORE",
                "Carol",
                fixture.room101,
                null,
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 6, 5)
        );
        fixture.createForm(
                "ORD-RANGE-CHECKIN-AFTER",
                "Dave",
                fixture.room102,
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2)
        );
        fixture.createForm(
                "ORD-RANGE-CHECKOUT-BEFORE",
                "Erin",
                fixture.room101,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 4)
        );
        fixture.createForm(
                "ORD-RANGE-CHECKOUT-AFTER",
                "Frank",
                fixture.room102,
                null,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 7, 3)
        );
        flushAndClear();

        List<RegistrationForm> result = registrationFormRepository.searchForAdminList(
                STORE_ID,
                null,
                null,
                null,
                false,
                List.of("unused"),
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 7, 2)
        );

        assertOrderNumbers(
                List.of(startBoundaryForm.getOrderNumber(), endBoundaryForm.getOrderNumber()),
                result
        );
    }

    @Test
    void searchForAdminList_shouldSupportSingleSidedDateRanges() {
        TestFixture fixture = createFixture();
        RegistrationForm checkInAfterStartForm = fixture.createForm(
                "ORD-SINGLE-START-IN",
                "Alice",
                fixture.room101,
                null,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 15)
        );
        RegistrationForm checkInBeforeStartForm = fixture.createForm(
                "ORD-SINGLE-START-OUT",
                "Bob",
                fixture.room102,
                null,
                LocalDate.of(2026, 6, 9),
                LocalDate.of(2026, 6, 15)
        );
        RegistrationForm checkOutBeforeEndForm = fixture.createForm(
                "ORD-SINGLE-END-IN",
                "Carol",
                fixture.room101,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 20)
        );
        fixture.createForm(
                "ORD-SINGLE-END-OUT",
                "Dave",
                fixture.room102,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 21)
        );
        flushAndClear();

        List<RegistrationForm> checkInStartOnlyResult = registrationFormRepository.searchForAdminList(
                STORE_ID,
                null,
                null,
                null,
                false,
                List.of("unused"),
                null,
                LocalDate.of(2026, 6, 10),
                null,
                null,
                null
        );
        List<RegistrationForm> checkOutEndOnlyResult = registrationFormRepository.searchForAdminList(
                STORE_ID,
                null,
                null,
                null,
                false,
                List.of("unused"),
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 6, 20)
        );

        assertOrderNumbers(List.of(checkInAfterStartForm.getOrderNumber()), checkInStartOnlyResult);
        assertOrderNumbers(
                List.of(
                        checkInAfterStartForm.getOrderNumber(),
                        checkInBeforeStartForm.getOrderNumber(),
                        checkOutBeforeEndForm.getOrderNumber()
                ),
                checkOutEndOnlyResult
        );
    }

    @Test
    void approveSubmittedAndRejectSubmitted_shouldReturnConditionalUpdateRows() {
        TestFixture fixture = createFixture();
        RegistrationForm approveCandidate = fixture.createForm("ORD-APPROVE", "Alice", fixture.room101, null);
        RegistrationForm rejectCandidate = fixture.createForm("ORD-REJECT", "Bob", fixture.room102, null);
        RegistrationForm approvedCandidate = fixture.createForm(
                "ORD-ALREADY-APPROVED",
                "Carol",
                fixture.room101,
                null,
                RegistrationFormStatus.APPROVED,
                ReservationStatus.CONFIRMED
        );
        flushAndClear();

        LocalDateTime approvedAt = LocalDateTime.of(2026, 6, 3, 9, 30);
        int approveRows = registrationFormRepository.approveSubmitted(
                STORE_ID,
                approveCandidate.getId(),
                "approved",
                approvedAt
        );
        flushAndClear();

        RegistrationForm approvedForm = entityManager.find(RegistrationForm.class, approveCandidate.getId());
        assertEquals(1, approveRows);
        assertEquals(RegistrationFormStatus.APPROVED, approvedForm.getStatus());
        assertEquals("approved", approvedForm.getReviewNote());
        assertEquals(approvedAt, approvedForm.getApprovedAt());
        assertNull(approvedForm.getRejectedAt());

        int repeatApproveRows = registrationFormRepository.approveSubmitted(
                STORE_ID,
                approveCandidate.getId(),
                "approved again",
                approvedAt.plusHours(1)
        );
        int wrongStoreApproveRows = registrationFormRepository.approveSubmitted(
                OTHER_STORE_ID,
                rejectCandidate.getId(),
                "wrong store",
                approvedAt.plusHours(2)
        );

        LocalDateTime rejectedAt = LocalDateTime.of(2026, 6, 3, 10, 30);
        int rejectRows = registrationFormRepository.rejectSubmitted(
                STORE_ID,
                rejectCandidate.getId(),
                "rejected",
                rejectedAt
        );
        int alreadyApprovedRejectRows = registrationFormRepository.rejectSubmitted(
                STORE_ID,
                approvedCandidate.getId(),
                "reject approved",
                rejectedAt.plusHours(1)
        );
        flushAndClear();

        RegistrationForm rejectedForm = entityManager.find(RegistrationForm.class, rejectCandidate.getId());
        assertEquals(0, repeatApproveRows);
        assertEquals(0, wrongStoreApproveRows);
        assertEquals(1, rejectRows);
        assertEquals(0, alreadyApprovedRejectRows);
        assertEquals(RegistrationFormStatus.REJECTED, rejectedForm.getStatus());
        assertEquals("rejected", rejectedForm.getReviewNote());
        assertEquals(rejectedAt, rejectedForm.getRejectedAt());
        assertNull(rejectedForm.getApprovedAt());
    }

    private TestFixture createFixture() {
        User user = createUser();
        Channel channel = createChannel(STORE_ID, "CH");
        Channel otherStoreChannel = createChannel(OTHER_STORE_ID, "OC");
        RoomType roomType = createRoomType(user, STORE_ID, "RT");
        RoomType otherStoreRoomType = createRoomType(user, OTHER_STORE_ID, "OT");
        Room room101 = createRoom(roomType, STORE_ID, "101");
        Room room102 = createRoom(roomType, STORE_ID, "102");
        Room otherStoreRoom101 = createRoom(otherStoreRoomType, OTHER_STORE_ID, "101");

        entityManager.persist(new RoomGroupMember(ROOM_GROUP_ID, room101.getId(), STORE_ID));
        entityManager.persist(new RoomGroupMember(OTHER_ROOM_GROUP_ID, room102.getId(), STORE_ID));
        entityManager.persist(new RoomGroupMember(ROOM_GROUP_ID, otherStoreRoom101.getId(), OTHER_STORE_ID));

        TestFixture fixture = new TestFixture();
        fixture.user = user;
        fixture.channel = channel;
        fixture.otherStoreChannel = otherStoreChannel;
        fixture.room101 = room101;
        fixture.room102 = room102;
        fixture.otherStoreRoom101 = otherStoreRoom101;
        return fixture;
    }

    private User createUser() {
        User user = new User(unique("repo-user"), unique("repo") + "@example.com", "password");
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Channel createChannel(Long storeId, String codePrefix) {
        Channel channel = new Channel("Repository Channel " + codePrefix, uniqueCode(codePrefix), ChannelType.OTA);
        channel.setStoreId(storeId);
        entityManager.persist(channel);
        return channel;
    }

    private RoomType createRoomType(User user, Long storeId, String codePrefix) {
        RoomType roomType = new RoomType("Repository Room Type " + codePrefix, uniqueCode(codePrefix), 2, "test");
        roomType.setUser(user);
        roomType.setStoreId(storeId);
        entityManager.persist(roomType);
        return roomType;
    }

    private Room createRoom(RoomType roomType, Long storeId, String roomNumber) {
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setUserId(ROOM_OWNER_USER_ID);
        room.setStoreId(storeId);
        entityManager.persist(room);
        entityManager.flush();
        return room;
    }

    private RegistrationForm createForm(
            User user,
            Channel channel,
            Long storeId,
            String orderPrefix,
            String guestName,
            Room room,
            String otaRoomNumber,
            RegistrationFormStatus formStatus,
            ReservationStatus reservationStatus
    ) {
        return createForm(
                user,
                channel,
                storeId,
                orderPrefix,
                guestName,
                room,
                otaRoomNumber,
                formStatus,
                reservationStatus,
                CHECK_IN_DATE,
                CHECK_OUT_DATE
        );
    }

    private RegistrationForm createForm(
            User user,
            Channel channel,
            Long storeId,
            String orderPrefix,
            String guestName,
            Room room,
            String otaRoomNumber,
            RegistrationFormStatus formStatus,
            ReservationStatus reservationStatus,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setChannel(channel);
        reservation.setStoreId(storeId);
        reservation.setRoom(room);
        reservation.setGuestName(guestName);
        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkOutDate);
        reservation.setAdults(2);
        reservation.setTotalAmount(TOTAL_AMOUNT);
        reservation.setStatus(reservationStatus);
        reservation.setOrderNumber(unique(orderPrefix));
        reservation.setOtaRoomNumber(otaRoomNumber);
        entityManager.persist(reservation);

        RegistrationForm form = new RegistrationForm();
        form.setReservation(reservation);
        form.setStatus(formStatus);
        form.setSubmittedAt(LocalDateTime.of(2026, 6, 2, 12, 0));
        entityManager.persist(form);
        return form;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static String unique(String value) {
        return value + "-" + RUN_ID;
    }

    private static String uniqueCode(String prefix) {
        return prefix + RUN_ID;
    }

    private static void assertSingleOrderNumber(String expectedOrderNumber, List<RegistrationForm> result) {
        List<String> orderNumbers = orderNumbersOf(result);
        assertEquals(1, orderNumbers.size());
        assertEquals(expectedOrderNumber, orderNumbers.get(0));
    }

    private static void assertOrderNumbers(List<String> expectedOrderNumbers, List<RegistrationForm> result) {
        List<String> expected = new ArrayList<>(expectedOrderNumbers);
        List<String> actual = orderNumbersOf(result);
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    private static List<String> orderNumbersOf(List<RegistrationForm> forms) {
        List<String> orderNumbers = new ArrayList<>();
        for (RegistrationForm form : forms) {
            orderNumbers.add(form.getOrderNumber());
        }
        return orderNumbers;
    }

    private class TestFixture {
        private User user;
        private Channel channel;
        private Channel otherStoreChannel;
        private Room room101;
        private Room room102;
        private Room otherStoreRoom101;

        private RegistrationForm createForm(String orderPrefix, String guestName, Room room, String otaRoomNumber) {
            return RegistrationFormRepositoryTest.this.createForm(
                    user,
                    channel,
                    STORE_ID,
                    orderPrefix,
                    guestName,
                    room,
                    otaRoomNumber,
                    RegistrationFormStatus.SUBMITTED,
                    ReservationStatus.CONFIRMED
            );
        }

        private RegistrationForm createForm(
                String orderPrefix,
                String guestName,
                Room room,
                String otaRoomNumber,
                LocalDate checkInDate,
                LocalDate checkOutDate
        ) {
            return RegistrationFormRepositoryTest.this.createForm(
                    user,
                    channel,
                    STORE_ID,
                    orderPrefix,
                    guestName,
                    room,
                    otaRoomNumber,
                    RegistrationFormStatus.SUBMITTED,
                    ReservationStatus.CONFIRMED,
                    checkInDate,
                    checkOutDate
            );
        }

        private RegistrationForm createForm(
                String orderPrefix,
                String guestName,
                Room room,
                String otaRoomNumber,
                RegistrationFormStatus formStatus,
                ReservationStatus reservationStatus
        ) {
            return RegistrationFormRepositoryTest.this.createForm(
                    user,
                    channel,
                    STORE_ID,
                    orderPrefix,
                    guestName,
                    room,
                    otaRoomNumber,
                    formStatus,
                    reservationStatus
            );
        }

        private RegistrationForm createOtherStoreForm(String orderPrefix, String guestName, String roomNumber) {
            Room room = otherStoreRoom101;
            if (!"101".equals(roomNumber)) {
                room = null;
            }
            return RegistrationFormRepositoryTest.this.createForm(
                    user,
                    otherStoreChannel,
                    OTHER_STORE_ID,
                    orderPrefix,
                    guestName,
                    room,
                    null,
                    RegistrationFormStatus.SUBMITTED,
                    ReservationStatus.CONFIRMED
            );
        }
    }
}
