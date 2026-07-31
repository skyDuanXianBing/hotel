package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.net.RequestOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.config.StripeConfig;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.IndependentSite;
import server.demo.entity.Payment;
import server.demo.entity.PaymentAttempt;
import server.demo.entity.Reservation;
import server.demo.entity.ReservationDailyPrice;
import server.demo.entity.Room;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.enums.IndependentSitePaymentProvider;
import server.demo.enums.PaymentAttemptStatus;
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
import server.demo.util.StoreTimeZoneUtil;
import server.demo.util.StripeCurrencyAmounts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import server.demo.i18n.ApiMessages;
@Service
public class IndependentSiteBookingService {

    private static final int HOLD_TTL_MINUTES = 15;
    private static final String PAYMENT_TYPE = "payment";
    private static final String SIMULATED_PAYMENT_METHOD = "SIMULATED";
    private static final String STRIPE_PAYMENT_METHOD = "STRIPE";

    private final IndependentSiteRepository siteRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationDailyPriceRepository reservationDailyPriceRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final RoomBlockoutRepository roomBlockoutRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final IndependentSiteQuoteService quoteService;
    private final RoomTypeInventoryLockService inventoryLockService;
    private final IndependentSiteReservationLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final StripeConfig stripeConfig;
    private final IndependentSiteStripeSettingsService stripeSettingsService;

    public IndependentSiteBookingService(
            IndependentSiteRepository siteRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            ReservationRepository reservationRepository,
            ReservationDailyPriceRepository reservationDailyPriceRepository,
            PaymentRepository paymentRepository,
            RoomRepository roomRepository,
            RoomBlockoutRepository roomBlockoutRepository,
            StoreRepository storeRepository,
            UserRepository userRepository,
            IndependentSiteQuoteService quoteService,
            RoomTypeInventoryLockService inventoryLockService,
            IndependentSiteReservationLifecycleService lifecycleService,
            ObjectMapper objectMapper,
            Clock clock,
            StripeConfig stripeConfig,
            IndependentSiteStripeSettingsService stripeSettingsService
    ) {
        this.siteRepository = siteRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.reservationRepository = reservationRepository;
        this.reservationDailyPriceRepository = reservationDailyPriceRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.roomBlockoutRepository = roomBlockoutRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.quoteService = quoteService;
        this.inventoryLockService = inventoryLockService;
        this.lifecycleService = lifecycleService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.stripeConfig = stripeConfig;
        this.stripeSettingsService = stripeSettingsService;
    }

    @Transactional
    public IndependentSiteDtos.PaymentAttemptResponse createPreviewHold(
            Long storeId,
            String rawSlug,
            IndependentSiteDtos.HoldRequest request
    ) {
        if (storeId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2bfd332b0f72"));
        }
        validateHoldRequest(request);
        String slug = normalizeSlug(rawSlug);
        IndependentSite site = resolvePreviewSiteForUpdate(storeId, slug);
        requirePaymentAvailable(site);
        return createHold(site, request);
    }

    /**
     * 公开游客下单：不依赖登录态与门店上下文，按 slug 解析已启用站点。
     * SIMULATED 站点要求开启模拟支付；STRIPE 站点要求后端已配置 Stripe 密钥，
     * 未配置时直接 422，避免落出无法支付的脏 attempt。
     */
    @Transactional
    public IndependentSiteDtos.PaymentAttemptResponse createPublicHold(
            String rawSlug,
            IndependentSiteDtos.HoldRequest request
    ) {
        validateHoldRequest(request);
        IndependentSite site = resolveEnabledSiteForUpdate(normalizeSlug(rawSlug));
        requirePaymentAvailable(site);
        return createHold(site, request);
    }

    private IndependentSiteDtos.PaymentAttemptResponse createHold(
            IndependentSite site,
            IndependentSiteDtos.HoldRequest request
    ) {

        String idempotencyKey = request.idempotencyKey().trim();
        String fingerprint = fingerprint(site.getId(), request);
        PaymentAttempt existing = paymentAttemptRepository
                .findByStoreIdAndIdempotencyKeyWithSite(site.getStoreId(), idempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getSite().getId(), site.getId())
                    || !Objects.equals(existing.getRequestFingerprint(), fingerprint)) {
                throw conflict(
                        "IDEMPOTENCY_KEY_REUSED",
                        ApiMessages.get("api.t.369d536bed6f")
                );
            }
            expireIfDue(existing);
            return toResponse(existing, loadReservations(existing));
        }

        IndependentSiteQuoteService.QuoteComputation quote =
                quoteService.calculate(site, request.toQuoteRequest());
        inventoryLockService.lockRoomTypes(
                site.getStoreId(),
                List.of(quote.roomType().getId())
        );
        List<Room> selectedRooms = lockAvailableRooms(quote, request);
        quoteService.assertHoldInventoryAvailable(quote, request.toQuoteRequest());
        Store store = storeRepository.findById(site.getStoreId())
                .orElseThrow(IndependentSiteBookingService::siteUnavailable);
        User owner = userRepository.findById(store.getUserId())
                .orElseThrow(() -> new IllegalStateException(ApiMessages.get("api.t.6d3fb1113443")));

        String publicReference = UUID.randomUUID().toString();
        String groupOrderNo = "WEB" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = nowUtc();
        String currency = quote.response().currency();

        List<Reservation> reservations = new ArrayList<>();
        for (int index = 0; index < selectedRooms.size(); index++) {
            Room room = selectedRooms.get(index);
            IndependentSiteQuoteService.GuestAllocation allocation = quote.guestAllocations().get(index);
            Reservation reservation = new Reservation();
            reservation.setStoreId(site.getStoreId());
            reservation.setUser(owner);
            reservation.setRoom(room);
            reservation.setChannel(site.getChannel());
            reservation.setGuestName(normalizeRequiredText(request.guest().name()));
            reservation.setGuestPhone(normalizeOptionalText(request.guest().phone()));
            reservation.setGuestEmail(normalizeOptionalText(request.guest().email()));
            reservation.setCheckInDate(request.checkInDate());
            reservation.setCheckOutDate(request.checkOutDate());
            reservation.setAdults(allocation.adults());
            reservation.setChildren(allocation.children());
            reservation.setTotalAmount(quote.reservationTotals().get(index));
            reservation.setPaidAmount(BigDecimal.ZERO.setScale(2));
            reservation.setGroupOrderNo(groupOrderNo);
            reservation.setChannelOrderNumber(groupOrderNo);
            reservation.setExternalBookingKey("SITE:" + publicReference);
            reservation.setPaymentMethod(
                    providerOf(site) == IndependentSitePaymentProvider.STRIPE
                            ? STRIPE_PAYMENT_METHOD
                            : SIMULATED_PAYMENT_METHOD
            );
            reservation.setPricePlan(quote.pricePlan().getName());
            reservation.setSpecialRequests(normalizeOptionalText(request.guest().specialRequests()));
            reservation.setBookingDate(StoreTimeZoneUtil.nowReservationTimestampLocalDateTime());
            reservation.setCurrencyCode(currency);
            reservation.setStatus(ReservationStatus.REQUESTED);
            reservation.setIndependentSiteId(site.getId());
            reservations.add(reservationRepository.save(reservation));
        }

        List<ReservationDailyPrice> dailyPrices = new ArrayList<>();
        for (int reservationIndex = 0; reservationIndex < reservations.size(); reservationIndex++) {
            Reservation reservation = reservations.get(reservationIndex);
            for (IndependentSiteQuoteService.DailyAmount daily :
                    quote.dailyAmountsByReservation().get(reservationIndex)) {
                ReservationDailyPrice row = new ReservationDailyPrice();
                row.setStoreId(site.getStoreId());
                row.setReservation(reservation);
                row.setPriceDate(daily.date());
                row.setCurrencyCode(currency);
                row.setRateId(String.valueOf(quote.pricePlan().getId()));
                row.setTaxAmount(BigDecimal.ZERO.setScale(2));
                row.setPriceBeforeTax(daily.amount());
                row.setPriceAfterTax(daily.amount());
                dailyPrices.add(row);
            }
        }
        reservationDailyPriceRepository.saveAll(dailyPrices);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setStoreId(site.getStoreId());
        attempt.setSite(site);
        attempt.setPublicReference(publicReference);
        attempt.setGroupOrderNo(groupOrderNo);
        attempt.setIdempotencyKey(idempotencyKey);
        attempt.setRequestFingerprint(fingerprint);
        attempt.setProvider(providerOf(site));
        attempt.setStatus(PaymentAttemptStatus.PENDING);
        attempt.setAmount(quote.response().totalAmount());
        attempt.setCurrencyCode(currency);
        attempt.setQuoteSnapshotJson(writeJson(quote.response()));
        attempt.setExpiresAt(now.plusMinutes(HOLD_TTL_MINUTES));
        attempt = paymentAttemptRepository.save(attempt);

        lifecycleService.onChanged(
                reservations,
                IndependentSiteReservationLifecycleService.Event.HOLD_CREATED,
                owner.getId()
        );
        return toResponse(attempt, reservations);
    }

    @Transactional
    public IndependentSiteDtos.PaymentAttemptResponse confirmSimulatedPayment(
            Long storeId,
            String publicReference
    ) {
        if (storeId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2bfd332b0f72"));
        }
        // 一店多站：不再假设唯一站点，先定位支付尝试，再锁定其所属站点校验
        PaymentAttempt attempt = paymentAttemptRepository
                .findByStoreIdAndPublicReferenceForUpdate(storeId, publicReference)
                .orElseThrow(IndependentSiteBookingService::paymentNotFound);
        IndependentSite site = siteRepository
                .findByStoreIdAndIdWithChannelForUpdate(storeId, attempt.getSite().getId())
                .orElseThrow(IndependentSiteBookingService::siteUnavailable);
        quoteService.validateSiteChannel(site);
        requireSimulatedPaymentEnabled(site);
        return dispatchConfirm(site, attempt);
    }

    /**
     * 公开游客确认支付：按支付渠道分发。
     * SIMULATED 即时确认；STRIPE 由 Stripe webhook 驱动确认，本端点对 STRIPE 保持 422（红线）。
     */
    @Transactional
    public IndependentSiteDtos.PaymentAttemptResponse confirmPublicPayment(
            String rawSlug,
            String publicReference
    ) {
        IndependentSite site = resolveEnabledSiteForUpdate(normalizeSlug(rawSlug));
        PaymentAttempt attempt = paymentAttemptRepository
                .findByStoreIdAndPublicReferenceForUpdate(site.getStoreId(), publicReference)
                .filter(value -> Objects.equals(value.getSite().getId(), site.getId()))
                .orElseThrow(IndependentSiteBookingService::paymentNotFound);
        return dispatchConfirm(site, attempt);
    }

    /**
     * 创建或复用 Stripe PaymentIntent，供公开页 Stripe.js 收卡。
     * 密钥按门店解析（站点 → storeId → 门店设置解密），门店未配齐直接 422；
     * 非 PENDING（含过期降级）直接回当前状态且不带 clientSecret；
     * 已有 PaymentIntent 可复用则复用，已 succeeded 时借查询时机幂等补齐确认，已 canceled 则新建。
     */
    @Transactional
    public IndependentSiteDtos.StripeIntentResponse createStripeIntent(
            String rawSlug,
            String publicReference
    ) {
        IndependentSite site = resolveEnabledSiteForUpdate(normalizeSlug(rawSlug));
        PaymentAttempt attempt = paymentAttemptRepository
                .findByPublicReferenceForUpdate(publicReference)
                .filter(value -> Objects.equals(value.getSite().getId(), site.getId()))
                .orElseThrow(IndependentSiteBookingService::paymentNotFound);
        IndependentSiteStripeSettingsService.ResolvedStripeKeys stripeKeys = attempt.getProvider()
                        == IndependentSitePaymentProvider.STRIPE
                ? stripeSettingsService.resolveForStore(site.getStoreId()).orElse(null)
                : null;
        if (stripeKeys == null || !stripeKeys.hasSecretKey()) {
            throw new IndependentSiteServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PAYMENT_PROVIDER_NOT_AVAILABLE",
                    ApiMessages.get("api.t.addcac05f5c0")
            );
        }
        expireIfDue(attempt);
        if (attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            return new IndependentSiteDtos.StripeIntentResponse(
                    null,
                    stripeKeys.publishableKey(),
                    attempt.getStatus().name()
            );
        }

        StripeClient client = stripeConfig.clientFor(stripeKeys.secretKey());
        PaymentIntent intent = retrieveExistingIntent(client, attempt);
        if (intent != null && "succeeded".equals(intent.getStatus())) {
            // 极端时序：Stripe 已成功但 webhook 未到达，借查询时机幂等补齐确认
            transition(attempt, PaymentAttemptStatus.SUCCEEDED, null, intent.getId());
            return new IndependentSiteDtos.StripeIntentResponse(
                    null,
                    stripeKeys.publishableKey(),
                    attempt.getStatus().name()
            );
        }
        if (intent == null || "canceled".equals(intent.getStatus())) {
            intent = createIntent(client, attempt);
            attempt.setProviderTransactionId(intent.getId());
            paymentAttemptRepository.save(attempt);
        }
        return new IndependentSiteDtos.StripeIntentResponse(
                intent.getClientSecret(),
                stripeKeys.publishableKey(),
                attempt.getStatus().name()
        );
    }

    private PaymentIntent retrieveExistingIntent(StripeClient client, PaymentAttempt attempt) {
        if (attempt.getProviderTransactionId() == null
                || attempt.getProviderTransactionId().isBlank()) {
            return null;
        }
        try {
            return client.v1().paymentIntents()
                    .retrieve(attempt.getProviderTransactionId());
        } catch (StripeException e) {
            throw stripeApiFailed(e);
        }
    }

    private PaymentIntent createIntent(StripeClient client, PaymentAttempt attempt) {
        long amountMinor = StripeCurrencyAmounts.toMinorUnits(
                attempt.getAmount(),
                attempt.getCurrencyCode()
        );
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountMinor)
                .setCurrency(attempt.getCurrencyCode().toLowerCase(Locale.ROOT))
                .putMetadata("publicReference", attempt.getPublicReference())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();
        // 幂等键 = publicReference：同一支付尝试重复建 intent 在 Stripe 侧也去重
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(attempt.getPublicReference())
                .build();
        try {
            return client.v1().paymentIntents().create(params, options);
        } catch (StripeException e) {
            throw stripeApiFailed(e);
        }
    }

    /**
     * Stripe webhook 驱动确认：与模拟支付共用 transition 落库管线。
     * 非 PENDING（含重复投递、已过期）直接返回，天然幂等；找不到说明非本站 intent，忽略。
     */
    @Transactional
    public void confirmStripePayment(String publicReference, String paymentIntentId) {
        PaymentAttempt attempt = paymentAttemptRepository
                .findByPublicReferenceForUpdate(publicReference)
                .orElse(null);
        if (attempt == null
                || attempt.getProvider() != IndependentSitePaymentProvider.STRIPE
                || attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            return;
        }
        transition(attempt, PaymentAttemptStatus.SUCCEEDED, null, paymentIntentId);
    }

    /**
     * Stripe webhook 终态支付失败（PaymentIntent 已 canceled）：PENDING 转 FAILED，释放预订逻辑与过期一致。
     * 可重试失败（卡被拒等）由 webhook 分发层拦在前面，不会走到这里。
     */
    @Transactional
    public void failStripePayment(String publicReference, String failureReason) {
        PaymentAttempt attempt = paymentAttemptRepository
                .findByPublicReferenceForUpdate(publicReference)
                .orElse(null);
        if (attempt == null
                || attempt.getProvider() != IndependentSitePaymentProvider.STRIPE
                || attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            return;
        }
        transition(attempt, PaymentAttemptStatus.FAILED, failureReason, null);
    }

    private IndependentSiteDtos.PaymentAttemptResponse dispatchConfirm(
            IndependentSite site,
            PaymentAttempt attempt
    ) {
        return switch (attempt.getProvider()) {
            case SIMULATED -> {
                requireSimulatedPaymentEnabled(site);
                yield confirmSimulatedAttempt(attempt);
            }
            case STRIPE -> throw new IndependentSiteServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PAYMENT_PROVIDER_NOT_AVAILABLE",
                    ApiMessages.get("api.t.4ad8b052c802")
            );
        };
    }

    private IndependentSiteDtos.PaymentAttemptResponse confirmSimulatedAttempt(PaymentAttempt attempt) {
        expireIfDue(attempt);
        if (attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            return toResponse(attempt, loadReservations(attempt));
        }

        transition(attempt, PaymentAttemptStatus.SUCCEEDED, null);
        return toResponse(attempt, loadReservations(attempt));
    }

    @Transactional
    public IndependentSiteDtos.PaymentAttemptResponse getPaymentStatus(
            String rawSlug,
            String publicReference
    ) {
        IndependentSite site = resolveEnabledSiteForUpdate(normalizeSlug(rawSlug));
        PaymentAttempt attempt = paymentAttemptRepository
                .findByStoreIdAndPublicReferenceForUpdate(site.getStoreId(), publicReference)
                .filter(value -> Objects.equals(value.getSite().getId(), site.getId()))
                .orElseThrow(IndependentSiteBookingService::paymentNotFound);
        expireIfDue(attempt);
        return toResponse(attempt, loadReservations(attempt));
    }

    @Transactional
    public void expirePaymentAttempt(String publicReference) {
        PaymentAttempt attempt = paymentAttemptRepository.findByPublicReferenceForUpdate(publicReference)
                .orElse(null);
        if (attempt == null
                || attempt.getStatus() != PaymentAttemptStatus.PENDING
                || attempt.getExpiresAt() == null
                || attempt.getExpiresAt().isAfter(nowUtc())) {
            return;
        }
        transition(attempt, PaymentAttemptStatus.EXPIRED, ApiMessages.get("api.t.fa6f0165aa19"));
    }

    private List<Room> lockAvailableRooms(
            IndependentSiteQuoteService.QuoteComputation quote,
            IndependentSiteDtos.HoldRequest request
    ) {
        List<Room> selected = new ArrayList<>();
        LocalDate lastNight = request.checkOutDate().minusDays(1);
        for (Long candidateRoomId : quote.candidateRoomIds()) {
            Room room = roomRepository.findByStoreIdAndIdForUpdate(
                            quote.site().getStoreId(),
                            candidateRoomId
                    )
                    .orElse(null);
            if (room == null
                    || room.getStatus() != RoomStatus.AVAILABLE
                    || room.getRoomType() == null
                    || !Objects.equals(room.getRoomType().getId(), quote.roomType().getId())) {
                continue;
            }
            boolean occupied = !reservationRepository.findByStoreIdAndRoomIdAndDateRange(
                    quote.site().getStoreId(),
                    room.getId(),
                    request.checkInDate(),
                    request.checkOutDate()
            ).isEmpty();
            if (occupied) {
                continue;
            }
            boolean blocked = !roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                    quote.site().getStoreId(),
                    List.of(room.getId()),
                    request.checkInDate(),
                    lastNight
            ).isEmpty();
            if (blocked) {
                continue;
            }
            selected.add(room);
            if (selected.size() == request.rooms()) {
                return selected;
            }
        }
        throw conflict("NO_AVAILABILITY", ApiMessages.get("api.t.d30f9afc5448"));
    }

    private void expireIfDue(PaymentAttempt attempt) {
        if (attempt.getStatus() == PaymentAttemptStatus.PENDING
                && attempt.getExpiresAt() != null
                && !attempt.getExpiresAt().isAfter(nowUtc())) {
            transition(attempt, PaymentAttemptStatus.EXPIRED, ApiMessages.get("api.t.fa6f0165aa19"));
        }
    }

    private void transition(
            PaymentAttempt attempt,
            PaymentAttemptStatus target,
            String failureReason
    ) {
        transition(attempt, target, failureReason, null);
    }

    /**
     * 支付尝试状态机（仅允许从 PENDING 出发，天然幂等）。
     * SUCCEEDED 分支按 attempt.provider 派生落库要素：SIMULATED 保持模拟支付语义与
     * "SIM-" 交易号；STRIPE 由调用方（webhook / intent 同步）传入 PaymentIntent.id。
     * 目标 SUCCEEDED 但保留已过期时降级 EXPIRED，webhook 迟到不会复活过期尝试。
     */
    private void transition(
            PaymentAttempt attempt,
            PaymentAttemptStatus target,
            String failureReason,
            String providerTransactionId
    ) {
        if (attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            return;
        }
        if (target == PaymentAttemptStatus.SUCCEEDED
                && attempt.getExpiresAt() != null
                && !attempt.getExpiresAt().isAfter(nowUtc())) {
            target = PaymentAttemptStatus.EXPIRED;
            failureReason = ApiMessages.get("api.t.fa6f0165aa19");
        }

        List<Reservation> reservations = reservationRepository.findByStoreIdAndGroupOrderNoForUpdate(
                attempt.getStoreId(),
                attempt.getGroupOrderNo()
        );
        if (reservations.isEmpty()) {
            throw new IllegalStateException(ApiMessages.get("api.t.803729502c1e"));
        }
        verifyReservationAmount(attempt, reservations);
        Store store = storeRepository.findById(attempt.getStoreId())
                .orElseThrow(() -> new IllegalStateException(ApiMessages.get("api.t.a4ad2d6034ff")));

        if (target == PaymentAttemptStatus.SUCCEEDED) {
            boolean stripe = attempt.getProvider() == IndependentSitePaymentProvider.STRIPE;
            String paymentMethod = stripe ? STRIPE_PAYMENT_METHOD : SIMULATED_PAYMENT_METHOD;
            String paymentActor = stripe ? ApiMessages.get("api.t.895df10e79c7") : ApiMessages.get("api.t.573aacdb4ff2");
            for (Reservation reservation : reservations) {
                if (reservation.getStatus() != ReservationStatus.REQUESTED) {
                    throw conflict("HOLD_STATE_CONFLICT", ApiMessages.get("api.t.71d4e6d207ab"));
                }
                Payment payment = new Payment();
                payment.setReservationId(reservation.getId());
                payment.setType(PAYMENT_TYPE);
                payment.setPaymentMethod(paymentMethod);
                payment.setAmount(reservation.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
                payment.setDate(storeToday(store));
                payment.setRemark(paymentActor + " " + attempt.getPublicReference());
                payment.setCreatedBy(paymentActor);
                paymentRepository.save(payment);

                reservation.setPaidAmount(reservation.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
                reservation.setStatus(ReservationStatus.CONFIRMED);
            }
            attempt.setStatus(PaymentAttemptStatus.SUCCEEDED);
            attempt.setProviderTransactionId(stripe
                    ? (providerTransactionId != null && !providerTransactionId.isBlank()
                            ? providerTransactionId
                            : attempt.getProviderTransactionId())
                    : "SIM-" + UUID.randomUUID());
            attempt.setFailureReason(null);
        } else {
            for (Reservation reservation : reservations) {
                if (reservation.getStatus() == ReservationStatus.REQUESTED) {
                    reservation.setStatus(ReservationStatus.CANCELLED);
                }
            }
            attempt.setStatus(target);
            attempt.setFailureReason(normalizeFailureReason(failureReason, target.name()));
        }
        attempt.setCompletedAt(nowUtc());
        reservationRepository.saveAll(reservations);
        paymentAttemptRepository.save(attempt);

        Long fallbackUserId = store.getUserId();
        lifecycleService.onChanged(
                reservations,
                target == PaymentAttemptStatus.SUCCEEDED
                        ? IndependentSiteReservationLifecycleService.Event.PAYMENT_SUCCEEDED
                        : IndependentSiteReservationLifecycleService.Event.PAYMENT_RELEASED,
                fallbackUserId
        );
    }

    private static void verifyReservationAmount(PaymentAttempt attempt, List<Reservation> reservations) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : reservations) {
            if (!Objects.equals(attempt.getStoreId(), reservation.getStoreId())
                    || !Objects.equals(attempt.getSite().getId(), reservation.getIndependentSiteId())) {
                throw new IllegalStateException(ApiMessages.get("api.t.b3f4b64b13ff"));
            }
            if (reservation.getTotalAmount() == null) {
                throw new IllegalStateException(ApiMessages.get("api.t.7350cc32f430"));
            }
            total = total.add(reservation.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
        }
        if (attempt.getAmount() == null
                || total.compareTo(attempt.getAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalStateException(ApiMessages.get("api.t.a66fe121c357"));
        }
    }

    private List<Reservation> loadReservations(PaymentAttempt attempt) {
        return reservationRepository.findByStoreIdAndGroupOrderNoOrderByIdAsc(
                attempt.getStoreId(),
                attempt.getGroupOrderNo()
        );
    }

    private IndependentSiteDtos.PaymentAttemptResponse toResponse(
            PaymentAttempt attempt,
            List<Reservation> reservations
    ) {
        List<String> orderNumbers = reservations == null
                ? List.of()
                : reservations.stream()
                        .map(Reservation::getOrderNumber)
                        .filter(Objects::nonNull)
                        .toList();
        return new IndependentSiteDtos.PaymentAttemptResponse(
                attempt.getPublicReference(),
                attempt.getStatus(),
                attempt.getAmount(),
                attempt.getCurrencyCode(),
                toOffset(attempt.getExpiresAt()),
                toOffset(attempt.getCompletedAt()),
                attempt.getGroupOrderNo(),
                orderNumbers,
                attempt.getFailureReason(),
                attempt.getProvider() == IndependentSitePaymentProvider.SIMULATED,
                attempt.getProvider() != null ? attempt.getProvider().name() : null
        );
    }

    /**
     * 建 hold 前的支付渠道门槛：SIMULATED 要求站点开启模拟支付；
     * STRIPE 要求门店三密钥齐全（站点 → storeId → 门店设置），未配齐直接 422，
     * 避免落出无法支付的脏 attempt。
     */
    private void requirePaymentAvailable(IndependentSite site) {
        if (providerOf(site) == IndependentSitePaymentProvider.STRIPE) {
            if (!stripeSettingsService.isFullyConfigured(site.getStoreId())) {
                throw new IndependentSiteServiceException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "PAYMENT_PROVIDER_NOT_AVAILABLE",
                        ApiMessages.get("api.t.b0f2b87adc4e")
                );
            }
            return;
        }
        requireSimulatedPaymentEnabled(site);
    }

    private static IndependentSitePaymentProvider providerOf(IndependentSite site) {
        return site != null && site.getPaymentProvider() != null
                ? site.getPaymentProvider()
                : IndependentSitePaymentProvider.SIMULATED;
    }

    private void requireSimulatedPaymentEnabled(IndependentSite site) {
        if (site == null
                || site.getPaymentProvider() != IndependentSitePaymentProvider.SIMULATED
                || !Boolean.TRUE.equals(site.getSimulatedPaymentEnabled())) {
            throw new IndependentSiteServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "SIMULATED_PAYMENT_DISABLED",
                    ApiMessages.get("api.t.0f901bacfa3c")
            );
        }
    }

    private static IndependentSiteServiceException stripeApiFailed(StripeException exception) {
        return new IndependentSiteServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "STRIPE_API_FAILED",
                ApiMessages.get("api.t.391e810ce96b")
        );
    }

    private IndependentSite resolvePreviewSiteForUpdate(Long storeId, String slug) {
        IndependentSite site = siteRepository.findEnabledByStoreIdAndSlugForUpdate(storeId, slug)
                .orElseThrow(IndependentSiteBookingService::siteUnavailable);
        quoteService.validateSiteChannel(site);
        return site;
    }

    private IndependentSite resolveEnabledSiteForUpdate(String slug) {
        IndependentSite site = siteRepository.findEnabledBySlugForUpdate(slug)
                .orElseThrow(IndependentSiteBookingService::siteUnavailable);
        quoteService.validateSiteChannel(site);
        return site;
    }

    private static void validateHoldRequest(IndependentSiteDtos.HoldRequest request) {
        if (request == null || request.guest() == null) {
            throw badRequest("INVALID_HOLD", ApiMessages.get("api.t.534ddacf8ebb"));
        }
        if (request.idempotencyKey() == null
                || !request.idempotencyKey().trim().matches("[A-Za-z0-9._:-]{8,100}")) {
            throw badRequest("INVALID_IDEMPOTENCY_KEY", ApiMessages.get("api.t.33c60502dcf1"));
        }
        if (request.guest().name() == null || request.guest().name().trim().isEmpty()
                || request.guest().name().trim().length() > 100) {
            throw badRequest("INVALID_GUEST", ApiMessages.get("api.t.efa674010269"));
        }
        if (request.guest().phone() != null && request.guest().phone().length() > 255
                || request.guest().email() != null && request.guest().email().length() > 254
                || request.guest().specialRequests() != null
                && request.guest().specialRequests().length() > 1000) {
            throw badRequest("INVALID_GUEST", ApiMessages.get("api.t.6106402de88b"));
        }
    }

    private String fingerprint(Long siteId, IndependentSiteDtos.HoldRequest request) {
        String canonical = String.join(
                "\u001F",
                String.valueOf(siteId),
                String.valueOf(request.roomTypeId()),
                String.valueOf(request.checkInDate()),
                String.valueOf(request.checkOutDate()),
                String.valueOf(request.rooms()),
                String.valueOf(request.adults()),
                String.valueOf(request.children()),
                normalizeRequiredText(request.guest().name()),
                nullToEmpty(normalizeOptionalText(request.guest().phone())),
                nullToEmpty(normalizeOptionalText(request.guest().email())),
                nullToEmpty(normalizeOptionalText(request.guest().specialRequests()))
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(ApiMessages.get("api.t.e1ca57af325d"), e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(ApiMessages.get("api.t.2d1446c1401a"), e);
        }
    }

    private LocalDate storeToday(Store store) {
        ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId(store);
        return LocalDate.now(clock.withZone(zoneId));
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static java.time.OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private static String normalizeSlug(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?")) {
            throw siteUnavailable();
        }
        return normalized;
    }

    private static String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeFailureReason(String value, String fallback) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            normalized = fallback;
        }
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static IndependentSiteServiceException siteUnavailable() {
        return new IndependentSiteServiceException(
                HttpStatus.NOT_FOUND,
                "SITE_UNAVAILABLE",
                ApiMessages.get("api.t.f8d8d0982ec5")
        );
    }

    private static IndependentSiteServiceException paymentNotFound() {
        return new IndependentSiteServiceException(
                HttpStatus.NOT_FOUND,
                "PAYMENT_ATTEMPT_NOT_FOUND",
                ApiMessages.get("api.t.5fb0add5c5a0")
        );
    }

    private static IndependentSiteServiceException badRequest(String code, String message) {
        return new IndependentSiteServiceException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static IndependentSiteServiceException conflict(String code, String message) {
        return new IndependentSiteServiceException(HttpStatus.CONFLICT, code, message);
    }
}
