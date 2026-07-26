package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.Channel;
import server.demo.entity.IndependentSite;
import server.demo.entity.Payment;
import server.demo.entity.PaymentAttempt;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.ReservationDailyPrice;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.enums.IndependentSitePaymentProvider;
import server.demo.enums.PaymentAttemptStatus;
import server.demo.enums.PriceAdjustmentType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.PaymentAttemptRepository;
import server.demo.repository.PaymentRepository;
import server.demo.repository.ReservationDailyPriceRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-Java unit tests. Repository proxies intentionally avoid Mockito so these
 * lifecycle checks can run on JDKs where dynamic agent attachment is disabled.
 */
class IndependentSiteBookingServiceTest {

    @Test
    void createPreviewHold_shouldPersistRequestedReservationAndReplaySameIdempotencyKey() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.HoldRequest request = fixture.holdRequest("idem-key-001", "Guest One");

        IndependentSiteDtos.PaymentAttemptResponse first =
                fixture.createPreviewHold(request);
        IndependentSiteDtos.PaymentAttemptResponse replay =
                fixture.createPreviewHold(request);

        assertEquals(PaymentAttemptStatus.PENDING, first.status());
        assertEquals(first.paymentAttemptId(), replay.paymentAttemptId());
        assertEquals(new BigDecimal("110.00"), first.amount());
        assertEquals(1, fixture.reservations.size());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), fixture.reservations.get(0).getPaidAmount());
        assertEquals(fixture.site.getId(), fixture.reservations.get(0).getIndependentSiteId());
        assertEquals(1, fixture.dailyPrices.size());
        assertEquals(new BigDecimal("110.00"), fixture.dailyPrices.get(0).getPriceAfterTax());
        assertEquals(1, fixture.attemptsByIdempotency.size());
        assertEquals(1, fixture.quoteService.calculateCalls);
        assertEquals(List.of(Set.of(101L)), fixture.inventoryLockService.lockedRoomTypeIds);
        assertEquals(
                List.of(IndependentSiteReservationLifecycleService.Event.HOLD_CREATED),
                fixture.lifecycleService.events
        );
    }

    @Test
    void createPreviewHold_shouldRejectMissingStoreContextBeforeWrites() {
        Fixture fixture = new Fixture();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.createPreviewHold(
                        null,
                        fixture.site.getSlug(),
                        fixture.holdRequest("idem-key-no-store", "Guest One")
                )
        );

        assertEquals("缺少门店上下文", exception.getMessage());
        assertEquals(0, fixture.quoteService.calculateCalls);
        assertTrue(fixture.reservations.isEmpty());
        assertTrue(fixture.attemptsByIdempotency.isEmpty());
    }

    @Test
    void createPreviewHold_shouldRejectCrossStoreSlugBeforeWrites() {
        Fixture fixture = new Fixture();

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.createPreviewHold(
                        2L,
                        fixture.site.getSlug(),
                        fixture.holdRequest("idem-key-other-store", "Guest One")
                )
        );

        assertEquals("SITE_UNAVAILABLE", exception.getCode());
        assertEquals(0, fixture.quoteService.calculateCalls);
        assertTrue(fixture.inventoryLockService.lockedRoomTypeIds.isEmpty());
        assertTrue(fixture.reservations.isEmpty());
        assertTrue(fixture.dailyPrices.isEmpty());
        assertTrue(fixture.attemptsByIdempotency.isEmpty());
        assertTrue(fixture.lifecycleService.events.isEmpty());
    }

    @Test
    void createPreviewHold_shouldRejectReusingIdempotencyKeyForDifferentRequest() {
        Fixture fixture = new Fixture();
        fixture.createPreviewHold(
                fixture.holdRequest("idem-key-002", "Guest One")
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.createPreviewHold(
                        fixture.holdRequest("idem-key-002", "Another Guest")
                )
        );

        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.getCode());
        assertEquals(1, fixture.reservations.size());
        assertEquals(1, fixture.attemptsByIdempotency.size());
    }

    @Test
    void managedConfirmation_shouldWriteServerAmountPaymentAndConfirmReservation() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPreviewHold(
                fixture.holdRequest("idem-key-003", "Guest One")
        );

        IndependentSiteDtos.PaymentAttemptResponse response =
                fixture.service.confirmSimulatedPayment(
                        fixture.site.getStoreId(),
                        hold.paymentAttemptId()
                );

        Reservation reservation = fixture.reservations.get(0);
        assertEquals(PaymentAttemptStatus.SUCCEEDED, response.status());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("110.00"), reservation.getPaidAmount());
        assertEquals(1, fixture.payments.size());
        assertEquals(reservation.getId(), fixture.payments.get(0).getReservationId());
        assertEquals(new BigDecimal("110.00"), fixture.payments.get(0).getAmount());
        assertEquals(LocalDate.of(2026, 7, 20), fixture.payments.get(0).getDate());
        assertNotNull(fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .getProviderTransactionId());
        assertEquals(
                List.of(
                        IndependentSiteReservationLifecycleService.Event.HOLD_CREATED,
                        IndependentSiteReservationLifecycleService.Event.PAYMENT_SUCCEEDED
                ),
                fixture.lifecycleService.events
        );
    }

    @Test
    void managedConfirmation_shouldNotReviveExpiredHold() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPreviewHold(
                fixture.holdRequest("idem-key-004", "Guest One")
        );
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setExpiresAt(LocalDateTime.of(2026, 7, 19, 23, 59));

        IndependentSiteDtos.PaymentAttemptResponse response =
                fixture.service.confirmSimulatedPayment(
                        fixture.site.getStoreId(),
                        hold.paymentAttemptId()
                );

        assertEquals(PaymentAttemptStatus.EXPIRED, response.status());
        assertEquals(ReservationStatus.CANCELLED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(
                IndependentSiteReservationLifecycleService.Event.PAYMENT_RELEASED,
                fixture.lifecycleService.events.get(1)
        );
    }

    @Test
    void createPreviewHold_shouldRollbackBeforeWritesWhenLockedQuotaRecheckFails() {
        Fixture fixture = new Fixture();
        fixture.quoteService.inventoryAvailable = false;

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.createPreviewHold(
                        fixture.holdRequest("idem-key-005", "Guest One")
                )
        );

        assertEquals("NO_AVAILABILITY", exception.getCode());
        assertEquals(1, fixture.quoteService.inventoryChecks);
        assertTrue(fixture.reservations.isEmpty());
        assertTrue(fixture.dailyPrices.isEmpty());
        assertTrue(fixture.attemptsByIdempotency.isEmpty());
        assertTrue(fixture.lifecycleService.events.isEmpty());
    }

    @Test
    void expirePaymentAttempt_shouldReleasePastDuePendingHold() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPreviewHold(
                fixture.holdRequest("idem-key-008", "Guest One")
        );
        PaymentAttempt attempt = fixture.attemptsByPublicReference.get(hold.paymentAttemptId());
        attempt.setExpiresAt(LocalDateTime.of(2026, 7, 19, 23, 59));

        fixture.service.expirePaymentAttempt(hold.paymentAttemptId());

        assertEquals(PaymentAttemptStatus.EXPIRED, attempt.getStatus());
        assertEquals(ReservationStatus.CANCELLED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
        assertEquals(
                IndependentSiteReservationLifecycleService.Event.PAYMENT_RELEASED,
                fixture.lifecycleService.events.get(1)
        );
    }

    @Test
    void createPreviewHoldAndSimulation_shouldHonorPersistentServerSideSwitch() {
        Fixture fixture = new Fixture();
        fixture.site.setSimulatedPaymentEnabled(false);

        IndependentSiteServiceException holdException = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.createPreviewHold(
                        fixture.holdRequest("idem-key-006", "Guest One")
                )
        );

        assertEquals("SIMULATED_PAYMENT_DISABLED", holdException.getCode());
        assertTrue(fixture.reservations.isEmpty());
        assertTrue(fixture.attemptsByIdempotency.isEmpty());

        fixture.site.setSimulatedPaymentEnabled(true);
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.createPreviewHold(
                fixture.holdRequest("idem-key-007", "Guest One")
        );
        fixture.site.setSimulatedPaymentEnabled(false);

        IndependentSiteServiceException paymentException = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.confirmSimulatedPayment(
                        fixture.site.getStoreId(),
                        hold.paymentAttemptId()
                )
        );

        assertEquals("SIMULATED_PAYMENT_DISABLED", paymentException.getCode());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
    }

    @Test
    void createPublicHold_shouldWorkWithoutStoreContext() {
        Fixture fixture = new Fixture();

        IndependentSiteDtos.PaymentAttemptResponse response = fixture.service.createPublicHold(
                fixture.site.getSlug(),
                fixture.holdRequest("idem-key-public-001", "Guest Public")
        );

        assertEquals(PaymentAttemptStatus.PENDING, response.status());
        assertEquals(new BigDecimal("110.00"), response.amount());
        assertTrue(response.simulated());
        assertEquals("SIMULATED", response.provider());
        assertEquals(1, fixture.reservations.size());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertEquals(1, fixture.quoteService.calculateCalls);
        assertEquals(
                List.of(IndependentSiteReservationLifecycleService.Event.HOLD_CREATED),
                fixture.lifecycleService.events
        );
    }

    @Test
    void createPublicHold_shouldRejectUnknownSlugBeforeWrites() {
        Fixture fixture = new Fixture();

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.createPublicHold(
                        "unknown-slug",
                        fixture.holdRequest("idem-key-public-002", "Guest Public")
                )
        );

        assertEquals("SITE_UNAVAILABLE", exception.getCode());
        assertEquals(0, fixture.quoteService.calculateCalls);
        assertTrue(fixture.reservations.isEmpty());
        assertTrue(fixture.attemptsByIdempotency.isEmpty());
    }

    @Test
    void confirmPublicPayment_shouldConfirmSimulatedAttemptWithoutStoreContext() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.service.createPublicHold(
                fixture.site.getSlug(),
                fixture.holdRequest("idem-key-public-003", "Guest Public")
        );

        IndependentSiteDtos.PaymentAttemptResponse response = fixture.service.confirmPublicPayment(
                fixture.site.getSlug(),
                hold.paymentAttemptId()
        );

        assertEquals(PaymentAttemptStatus.SUCCEEDED, response.status());
        assertEquals(ReservationStatus.CONFIRMED, fixture.reservations.get(0).getStatus());
        assertEquals(new BigDecimal("110.00"), fixture.reservations.get(0).getPaidAmount());
        assertEquals(1, fixture.payments.size());
        assertEquals(
                List.of(
                        IndependentSiteReservationLifecycleService.Event.HOLD_CREATED,
                        IndependentSiteReservationLifecycleService.Event.PAYMENT_SUCCEEDED
                ),
                fixture.lifecycleService.events
        );
    }

    @Test
    void confirmPublicPayment_shouldRejectCrossSiteAttempt() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.service.createPublicHold(
                fixture.site.getSlug(),
                fixture.holdRequest("idem-key-public-004", "Guest Public")
        );

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.confirmPublicPayment("other-slug", hold.paymentAttemptId())
        );

        assertEquals("SITE_UNAVAILABLE", exception.getCode());
        assertEquals(PaymentAttemptStatus.PENDING,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertTrue(fixture.payments.isEmpty());
    }

    @Test
    void confirmPublicPayment_shouldRejectStripeProviderUntilIntegrated() {
        Fixture fixture = new Fixture();
        IndependentSiteDtos.PaymentAttemptResponse hold = fixture.service.createPublicHold(
                fixture.site.getSlug(),
                fixture.holdRequest("idem-key-public-005", "Guest Public")
        );
        fixture.attemptsByPublicReference.get(hold.paymentAttemptId())
                .setProvider(server.demo.enums.IndependentSitePaymentProvider.STRIPE);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> fixture.service.confirmPublicPayment(
                        fixture.site.getSlug(),
                        hold.paymentAttemptId()
                )
        );

        assertEquals("PAYMENT_PROVIDER_NOT_AVAILABLE", exception.getCode());
        assertEquals(PaymentAttemptStatus.PENDING,
                fixture.attemptsByPublicReference.get(hold.paymentAttemptId()).getStatus());
        assertEquals(ReservationStatus.REQUESTED, fixture.reservations.get(0).getStatus());
        assertTrue(fixture.payments.isEmpty());
    }

    private static final class Fixture {

        private static final Clock CLOCK = Clock.fixed(
                Instant.parse("2026-07-20T00:00:00Z"),
                ZoneOffset.UTC
        );

        private final IndependentSite site = site();
        private final Store store = store();
        private final User owner = owner();
        private final Room room = room();
        private final List<Reservation> reservations = new ArrayList<>();
        private final List<ReservationDailyPrice> dailyPrices = new ArrayList<>();
        private final List<Payment> payments = new ArrayList<>();
        private final Map<String, PaymentAttempt> attemptsByIdempotency = new LinkedHashMap<>();
        private final Map<String, PaymentAttempt> attemptsByPublicReference = new LinkedHashMap<>();
        private final RecordingQuoteService quoteService = new RecordingQuoteService(site, quote());
        private final RecordingInventoryLockService inventoryLockService =
                new RecordingInventoryLockService();
        private final RecordingLifecycleService lifecycleService = new RecordingLifecycleService();
        private long reservationSequence = 1000L;
        private long attemptSequence = 2000L;
        private long paymentSequence = 3000L;

        private final IndependentSiteBookingService service = new IndependentSiteBookingService(
                repository(IndependentSiteRepository.class, this::handleSiteRepository),
                repository(PaymentAttemptRepository.class, this::handlePaymentAttemptRepository),
                repository(ReservationRepository.class, this::handleReservationRepository),
                repository(ReservationDailyPriceRepository.class, this::handleDailyPriceRepository),
                repository(PaymentRepository.class, this::handlePaymentRepository),
                repository(RoomRepository.class, this::handleRoomRepository),
                repository(RoomBlockoutRepository.class, this::handleBlockoutRepository),
                repository(StoreRepository.class, this::handleStoreRepository),
                repository(UserRepository.class, this::handleUserRepository),
                quoteService,
                inventoryLockService,
                lifecycleService,
                new ObjectMapper().findAndRegisterModules(),
                CLOCK,
                new server.demo.config.StripeConfig(null),
                new IndependentSiteStripeSettingsService(null, "") {
                    @Override
                    public boolean isFullyConfigured(Long storeId) {
                        return false;
                    }
                }
        );

        private IndependentSiteDtos.PaymentAttemptResponse createPreviewHold(
                IndependentSiteDtos.HoldRequest request
        ) {
            return service.createPreviewHold(site.getStoreId(), site.getSlug(), request);
        }

        private Object handleSiteRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findEnabledByStoreIdAndSlugForUpdate" -> Optional.ofNullable(
                        Objects.equals(site.getStoreId(), args[0])
                                && Objects.equals(site.getSlug(), args[1])
                                ? site
                                : null
                );
                case "findEnabledBySlugForUpdate" ->
                        Optional.ofNullable(Objects.equals(site.getSlug(), args[0]) ? site : null);
                case "findByStoreIdAndIdWithChannelForUpdate" ->
                        Optional.ofNullable(Objects.equals(site.getStoreId(), args[0])
                                && Objects.equals(site.getId(), args[1])
                                ? site
                                : null);
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handlePaymentAttemptRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByStoreIdAndIdempotencyKeyWithSite" ->
                        Optional.ofNullable(attemptsByIdempotency.get(args[1]));
                case "findByStoreIdAndPublicReferenceForUpdate", "findByPublicReferenceForUpdate" ->
                        Optional.ofNullable(attemptsByPublicReference.get(args[args.length - 1]));
                case "save" -> saveAttempt((PaymentAttempt) args[0]);
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handleReservationRepository(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> saveReservation((Reservation) args[0]);
                case "saveAll" -> args[0];
                case "findByStoreIdAndGroupOrderNoOrderByIdAsc",
                     "findByStoreIdAndGroupOrderNoForUpdate" ->
                        reservations.stream()
                                .filter(value -> Objects.equals(value.getStoreId(), args[0]))
                                .filter(value -> Objects.equals(value.getGroupOrderNo(), args[1]))
                                .toList();
                case "findByStoreIdAndRoomIdAndDateRange" -> reservations.stream()
                        .filter(value -> value.getStatus() == ReservationStatus.REQUESTED
                                || value.getStatus() == ReservationStatus.CONFIRMED
                                || value.getStatus() == ReservationStatus.CHECKED_IN)
                        .filter(value -> value.getRoom() != null
                                && Objects.equals(value.getRoom().getId(), args[1]))
                        .filter(value -> value.getCheckInDate().isBefore((LocalDate) args[3])
                                && value.getCheckOutDate().isAfter((LocalDate) args[2]))
                        .toList();
                default -> objectMethodOrFail(proxy, method, args);
            };
        }

        private Object handleDailyPriceRepository(Object proxy, Method method, Object[] args) {
            if ("saveAll".equals(method.getName())) {
                @SuppressWarnings("unchecked")
                Iterable<ReservationDailyPrice> rows = (Iterable<ReservationDailyPrice>) args[0];
                rows.forEach(dailyPrices::add);
                return args[0];
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handlePaymentRepository(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                Payment payment = (Payment) args[0];
                if (payment.getId() == null) {
                    payment.setId(++paymentSequence);
                }
                payments.add(payment);
                return payment;
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleRoomRepository(Object proxy, Method method, Object[] args) {
            if ("findByStoreIdAndIdForUpdate".equals(method.getName())) {
                return Optional.ofNullable(
                        Objects.equals(room.getStoreId(), args[0])
                                && Objects.equals(room.getId(), args[1])
                                ? room
                                : null
                );
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleBlockoutRepository(Object proxy, Method method, Object[] args) {
            if ("findByStoreIdAndRoom_IdInAndBlockDateBetween".equals(method.getName())) {
                return List.of();
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleStoreRepository(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(Objects.equals(store.getId(), args[0]) ? store : null);
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Object handleUserRepository(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(Objects.equals(owner.getId(), args[0]) ? owner : null);
            }
            return objectMethodOrFail(proxy, method, args);
        }

        private Reservation saveReservation(Reservation reservation) {
            if (reservation.getId() == null) {
                reservation.setId(++reservationSequence);
                reservation.setOrderNumber("RSV-" + reservation.getId());
                reservations.add(reservation);
            }
            return reservation;
        }

        private PaymentAttempt saveAttempt(PaymentAttempt attempt) {
            if (attempt.getId() == null) {
                attempt.setId(++attemptSequence);
            }
            attemptsByIdempotency.put(attempt.getIdempotencyKey(), attempt);
            attemptsByPublicReference.put(attempt.getPublicReference(), attempt);
            return attempt;
        }

        private IndependentSiteDtos.HoldRequest holdRequest(String idempotencyKey, String guestName) {
            return new IndependentSiteDtos.HoldRequest(
                    idempotencyKey,
                    room.getRoomType().getId(),
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 2),
                    1,
                    1,
                    0,
                    new IndependentSiteDtos.Guest(
                            guestName,
                            "13800000000",
                            "guest@example.com",
                            "Late arrival"
                    )
            );
        }

        private IndependentSiteQuoteService.QuoteComputation quote() {
            PricePlan pricePlan = site.getChannel().getDefaultPricePlan();
            RoomType roomType = room.getRoomType();
            RoomTypePricePlan mapping = new RoomTypePricePlan();
            LocalDate date = LocalDate.of(2026, 8, 1);
            BigDecimal amount = new BigDecimal("110.00");
            OffsetDateTime quotedAt = OffsetDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);
            IndependentSiteDtos.QuoteResponse response = new IndependentSiteDtos.QuoteResponse(
                    site.getSlug(),
                    roomType.getId(),
                    roomType.getName(),
                    date,
                    date.plusDays(1),
                    1,
                    1,
                    0,
                    1,
                    "CNY",
                    new BigDecimal("10.00"),
                    List.of(new IndependentSiteDtos.NightlyRate(
                            date,
                            new BigDecimal("100.00"),
                            amount,
                            BigDecimal.ZERO.setScale(2),
                            amount
                    )),
                    amount,
                    quotedAt,
                    quotedAt.plusMinutes(5)
            );
            return new IndependentSiteQuoteService.QuoteComputation(
                    site,
                    roomType,
                    pricePlan,
                    mapping,
                    List.of(room.getId()),
                    List.of(new IndependentSiteQuoteService.GuestAllocation(1, 0)),
                    List.of(List.of(new IndependentSiteQuoteService.DailyAmount(date, amount))),
                    List.of(amount),
                    response
            );
        }

        private static IndependentSite site() {
            PricePlan pricePlan = new PricePlan();
            pricePlan.setId(501L);
            pricePlan.setStoreId(1L);
            pricePlan.setName("Standard");

            Channel channel = new Channel();
            channel.setId(301L);
            channel.setStoreId(1L);
            channel.setCode(IndependentSiteManagementService.BOOKING_ENGINE_CHANNEL_CODE);
            channel.setEnabled(true);
            channel.setIsActive(true);
            channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
            channel.setPriceAdjustmentValue(new BigDecimal("10.00"));
            channel.setDefaultPricePlan(pricePlan);

            IndependentSite site = new IndependentSite();
            site.setId(11L);
            site.setStoreId(1L);
            site.setSlug("alpha-hotel");
            site.setEnabled(true);
            site.setChannel(channel);
            site.setPaymentProvider(IndependentSitePaymentProvider.SIMULATED);
            site.setSimulatedPaymentEnabled(true);
            site.setPublishedAt(LocalDateTime.of(2026, 7, 20, 0, 0));
            return site;
        }

        private static Store store() {
            Store store = new Store();
            store.setId(1L);
            store.setUserId(901L);
            store.setName("Alpha Hotel");
            store.setTimezone("UTC");
            store.setCurrency("CNY");
            return store;
        }

        private static User owner() {
            User owner = new User();
            owner.setId(901L);
            return owner;
        }

        private static Room room() {
            RoomType roomType = new RoomType();
            roomType.setId(101L);
            roomType.setStoreId(1L);
            roomType.setName("King Room");
            roomType.setCode("KING");
            roomType.setMaxGuests(2);

            Room room = new Room();
            room.setId(201L);
            room.setStoreId(1L);
            room.setUserId(901L);
            room.setRoomNumber("101");
            room.setRoomType(roomType);
            room.setStatus(RoomStatus.AVAILABLE);
            return room;
        }
    }

    private static final class RecordingQuoteService extends IndependentSiteQuoteService {

        private final IndependentSite site;
        private final QuoteComputation quote;
        private int calculateCalls;
        private int inventoryChecks;
        private boolean inventoryAvailable = true;

        private RecordingQuoteService(IndependentSite site, QuoteComputation quote) {
            super(null, null, publishedHomePageRepository(site), null, null, null, null, null, null, null,
                    null, null, null, null);
            this.site = site;
            this.quote = quote;
        }

        @Override
        QuoteComputation calculate(IndependentSite requestedSite, IndependentSiteDtos.QuoteRequest request) {
            assertSame(site, requestedSite);
            calculateCalls++;
            return quote;
        }

        @Override
        void assertHoldInventoryAvailable(
                QuoteComputation quote,
                IndependentSiteDtos.QuoteRequest request
        ) {
            inventoryChecks++;
            if (!inventoryAvailable) {
                throw new IndependentSiteServiceException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "NO_AVAILABILITY",
                        "每日库存配额刚刚发生变化，请重新报价"
                );
            }
        }

        @Override
        IndependentSite resolveEnabledSite(String slug) {
            if (!Objects.equals(site.getSlug(), slug)) {
                throw new AssertionError("Unexpected slug: " + slug);
            }
            return site;
        }
    }

    private static final class RecordingLifecycleService
            extends IndependentSiteReservationLifecycleService {

        private final List<Event> events = new ArrayList<>();

        private RecordingLifecycleService() {
            super(null, null, Optional.empty(), Optional.empty(), null, null);
        }

        @Override
        public void onChanged(List<Reservation> reservations, Event event, Long fallbackUserId) {
            assertFalse(reservations.isEmpty());
            events.add(event);
        }
    }

    private static final class RecordingInventoryLockService
            extends RoomTypeInventoryLockService {

        private final List<Set<Long>> lockedRoomTypeIds = new ArrayList<>();

        private RecordingInventoryLockService() {
            super(null);
        }

        @Override
        public Set<Long> lockRoomTypes(Long storeId, Collection<Long> roomTypeIds) {
            assertEquals(1L, storeId);
            Set<Long> locked = Set.copyOf(roomTypeIds);
            lockedRoomTypeIds.add(locked);
            return locked;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static server.demo.repository.IndependentSitePageRepository publishedHomePageRepository(
            IndependentSite site
    ) {
        server.demo.entity.IndependentSitePage home = new server.demo.entity.IndependentSitePage();
        home.setId(9000L + site.getId());
        home.setStoreId(site.getStoreId());
        home.setSite(site);
        home.setPath("/");
        home.setType(server.demo.enums.IndependentSitePageType.HOME);
        home.setTitle("Home");
        home.setPublishedSchemaJson("{}");
        home.setPublishedAt(LocalDateTime.of(2026, 7, 20, 0, 0));
        home.setEnabled(true);
        return repository(
                server.demo.repository.IndependentSitePageRepository.class,
                (proxy, method, args) -> {
                    if ("findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue"
                            .equals(method.getName())) {
                        return Optional.of(home);
                    }
                    return objectMethodOrFail(proxy, method, args);
                }
        );
    }

    private static Object objectMethodOrFail(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError("Unexpected repository method: " + method);
        };
    }
}
