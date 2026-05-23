package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.config.SuWebhookConfig;
import server.demo.entity.Channel;
import server.demo.entity.OtaIntegration;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.entity.User;
import server.demo.enums.PriceAdjustmentType;
import server.demo.enums.RoomStatus;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.enums.SuWebhookEventType;
import server.demo.repository.ChannelRepository;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuReservationWebhookEventRepository;
import server.demo.repository.UserRepository;
import server.demo.util.JwtUtil;
import server.demo.util.SuHotelIdUtil;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChannelE2ETestSupportService {

    private static final int MAX_RESERVATION_LOOKUP_RESULTS = 50;
    private static final String CHANNEL_CODE_AIRBNB = "AIRBNB";
    private static final String CHANNEL_CODE_BOOKING = "BOOKING";
    private static final String LOCAL_SETUP_EMAIL = "channel-e2e-local@thehosthub.test";
    private static final String LOCAL_SETUP_USERNAME = "local_channel_e2e";
    private static final String LOCAL_SETUP_PASSWORD = "local-channel-e2e-password";
    private static final String LOCAL_SETUP_STORE_NAME = "Local Channel E2E Hotel";
    private static final String LOCAL_SETUP_SU_HOTEL_ID = "LOCALE2EHOTEL";
    private static final String LOCAL_SETUP_PRICE_PLAN_NAME = "Local E2E Standard Rate";
    private static final String LOCAL_SETUP_ROOM_TYPE_NAME = "Local E2E Standard Room";
    private static final String LOCAL_SETUP_ROOM_TYPE_CODE = "E2ELOCAL";
    private static final String LOCAL_SETUP_ROOM_NUMBER = "E2E-101";
    private static final List<String> LOCAL_SETUP_ROOM_NUMBERS = List.of(
            LOCAL_SETUP_ROOM_NUMBER,
            "E2E-102",
            "E2E-103"
    );
    private static final BigDecimal LOCAL_SETUP_DEFAULT_PRICE = new BigDecimal("120.00");

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreUserRepository storeUserRepository;
    private final ChannelRepository channelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PricePlanRepository pricePlanRepository;
    private final OtaIntegrationRepository otaIntegrationRepository;
    private final ReservationRepository reservationRepository;
    private final SuReservationWebhookEventRepository webhookEventRepository;
    private final SuMessageThreadRepository suMessageThreadRepository;
    private final SuWebhookConfig suWebhookConfig;
    private final ChannelBootstrapService channelBootstrapService;
    private final ChannelE2ETestSupportResponseMapper responseMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${channel.e2e.local-enabled:false}")
    private boolean localE2EEnabled;

    @Value("${channel.e2e.test-support.key:}")
    private String testSupportKey;

    public ChannelE2ETestSupportService(
            UserRepository userRepository,
            StoreRepository storeRepository,
            StoreUserRepository storeUserRepository,
            ChannelRepository channelRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            PricePlanRepository pricePlanRepository,
            OtaIntegrationRepository otaIntegrationRepository,
            ReservationRepository reservationRepository,
            SuReservationWebhookEventRepository webhookEventRepository,
            SuMessageThreadRepository suMessageThreadRepository,
            SuWebhookConfig suWebhookConfig,
            ChannelBootstrapService channelBootstrapService,
            ChannelE2ETestSupportResponseMapper responseMapper,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.storeUserRepository = storeUserRepository;
        this.channelRepository = channelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.pricePlanRepository = pricePlanRepository;
        this.otaIntegrationRepository = otaIntegrationRepository;
        this.reservationRepository = reservationRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.suMessageThreadRepository = suMessageThreadRepository;
        this.suWebhookConfig = suWebhookConfig;
        this.channelBootstrapService = channelBootstrapService;
        this.responseMapper = responseMapper;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public SetupLocalResponse setupLocal(String testSupportKey) {
        validateTestSupportAccess(testSupportKey);

        List<String> created = new ArrayList<>();
        List<String> reused = new ArrayList<>();

        User user = ensureLocalSetupUser(created, reused);
        Store store = ensureLocalSetupStore(user, created, reused);
        ensureLocalSetupStoreUser(store, user, created, reused);
        ensureLocalSetupChannels(store.getId(), created, reused);
        PricePlan pricePlan = ensureLocalSetupPricePlan(store.getId(), user, created, reused);
        RoomType roomType = ensureLocalSetupRoomType(store.getId(), user, created, reused);
        List<Room> rooms = ensureLocalSetupRooms(store.getId(), user, roomType, created, reused);
        Room primaryRoom = rooms.get(0);
        ensureLocalSetupOtaIntegration(store.getId(), CHANNEL_CODE_BOOKING, "Booking.com", pricePlan, created, reused);
        ensureLocalSetupOtaIntegration(store.getId(), CHANNEL_CODE_AIRBNB, "Airbnb", pricePlan, created, reused);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        ChannelE2EReadinessResponse readiness = getReadiness(store.getId());

        SetupLocalSummary summary = new SetupLocalSummary(List.copyOf(created), List.copyOf(reused));
        return new SetupLocalResponse(
                token,
                store.getId(),
                store.getSuHotelId(),
                user.getEmail(),
                user.getId(),
                roomType.getId(),
                primaryRoom.getId(),
                responseMapper.toRoomSummaries(rooms),
                summary,
                readiness
        );
    }

    @Transactional(readOnly = true)
    public ChannelE2EReadinessResponse getReadiness(Long storeId) {
        Store store = requireStore(storeId);
        String suHotelId = resolveSuHotelId(store);

        List<Channel> channels = channelRepository.findByStoreId(storeId);
        List<RoomType> roomTypes = roomTypeRepository.findByStoreIdOrderByName(storeId);
        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        List<PricePlan> pricePlans = pricePlanRepository.findByStoreIdOrderByName(storeId);
        List<OtaIntegration> otaIntegrations = otaIntegrationRepository.findByStoreId(storeId);

        WebhookConfigSummary webhookConfig = buildWebhookConfig(suHotelId);
        List<String> missingRequirements = findMissingRequirements(
                suHotelId,
                channels,
                roomTypes,
                rooms,
                pricePlans,
                otaIntegrations,
                webhookConfig
        );

        boolean ready = missingRequirements.isEmpty();
        return new ChannelE2EReadinessResponse(
                store.getId(),
                store.getName(),
                suHotelId,
                List.of(CHANNEL_CODE_BOOKING, CHANNEL_CODE_AIRBNB),
                ready,
                missingRequirements,
                webhookConfig,
                responseMapper.toChannelSummaries(channels),
                responseMapper.toRoomTypeSummaries(roomTypes),
                responseMapper.toRoomSummaries(rooms),
                responseMapper.toPricePlanSummaries(pricePlans),
                responseMapper.toOtaIntegrationSummaries(otaIntegrations)
        );
    }

    @Transactional(readOnly = true)
    public ReservationLookupResponse lookupReservations(
            Long storeId,
            String reservationNotifId,
            String suReservationId,
            String channelOrderNumber,
            String externalBookingKey,
            String orderNumber
    ) {
        ReservationLookupQuery query = new ReservationLookupQuery(
                normalize(reservationNotifId),
                normalize(suReservationId),
                normalize(channelOrderNumber),
                normalize(externalBookingKey),
                normalize(orderNumber)
        );

        if (!query.hasAnyKey()) {
            return new ReservationLookupResponse(query, 0, List.of());
        }

        List<Reservation> reservations = reservationRepository.findByStoreIdAndChannelE2ELookupKeys(
                storeId,
                query.reservationNotifId(),
                query.suReservationId(),
                query.channelOrderNumber(),
                query.externalBookingKey(),
                query.orderNumber()
        );

        List<ReservationSummary> items = new ArrayList<>();
        int count = Math.min(reservations.size(), MAX_RESERVATION_LOOKUP_RESULTS);
        for (int index = 0; index < count; index++) {
            items.add(responseMapper.toReservationSummary(reservations.get(index)));
        }

        return new ReservationLookupResponse(query, reservations.size(), items);
    }

    @Transactional(readOnly = true)
    public WebhookEventLookupResponse lookupWebhookEvents(
            Long storeId,
            String hotelId,
            String reservationNotifId,
            SuWebhookEventStatus status,
            SuWebhookEventType eventType,
            Integer limit
    ) {
        int pageSize = responseMapper.clampEventLimit(limit);
        WebhookEventLookupQuery query = new WebhookEventLookupQuery(
                normalize(hotelId),
                normalize(reservationNotifId),
                status,
                eventType,
                pageSize
        );

        List<SuReservationWebhookEvent> events = webhookEventRepository.findRecentByStoreIdAndFilters(
                storeId,
                query.hotelId(),
                query.reservationNotifId(),
                query.status(),
                query.eventType(),
                PageRequest.of(0, pageSize)
        );

        List<WebhookEventSummary> items = new ArrayList<>();
        for (SuReservationWebhookEvent event : events) {
            items.add(responseMapper.toWebhookEventSummary(event));
        }

        return new WebhookEventLookupResponse(query, items.size(), items);
    }

    @Transactional(readOnly = true)
    public MessagingThreadLookupResponse lookupMessagingThreads(
            Long storeId,
            Integer channelId,
            String threadId,
            String bookingId,
            String externalMessageId,
            Long messageId,
            Integer limit,
            Integer messageLimit
    ) {
        int threadLimit = responseMapper.clampMessagingThreadLimit(limit);
        int messagesPerThread = responseMapper.clampMessagingMessageLimit(messageLimit);
        MessagingThreadLookupQuery query = new MessagingThreadLookupQuery(
                channelId,
                normalize(threadId),
                normalize(bookingId),
                normalize(externalMessageId),
                messageId,
                threadLimit,
                messagesPerThread
        );

        List<SuMessageThread> threads = suMessageThreadRepository.findRecentByStoreIdAndMessagingFilters(
                storeId,
                query.channelId(),
                query.threadId(),
                query.bookingId(),
                query.externalMessageId(),
                query.messageId(),
                PageRequest.of(0, threadLimit)
        );

        List<MessagingThreadSummary> items = new ArrayList<>();
        for (SuMessageThread thread : threads) {
            items.add(responseMapper.toMessagingThreadSummary(storeId, thread, messagesPerThread));
        }

        return new MessagingThreadLookupResponse(query, items.size(), items);
    }

    public void validateTestSupportAccess(String providedTestSupportKey) {
        if (!localE2EEnabled) {
            throw new TestSupportAccessException(403, "本地渠道E2E test-support 未启用");
        }

        String configuredKey = normalize(testSupportKey);
        if (configuredKey == null) {
            throw new TestSupportAccessException(403, "本地渠道E2E test-support key 未配置");
        }

        String providedKey = normalize(providedTestSupportKey);
        if (providedKey == null) {
            throw new TestSupportAccessException(400, "缺少 X-Test-Support-Key");
        }

        byte[] configuredBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = providedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(configuredBytes, providedBytes)) {
            throw new TestSupportAccessException(403, "X-Test-Support-Key 不匹配");
        }
    }

    private User ensureLocalSetupUser(List<String> created, List<String> reused) {
        User existingUser = userRepository.findByEmail(LOCAL_SETUP_EMAIL).orElse(null);
        if (existingUser != null) {
            if (!Boolean.TRUE.equals(existingUser.getIsActive())) {
                existingUser.setIsActive(true);
                existingUser = userRepository.save(existingUser);
            }
            reused.add("user:" + existingUser.getEmail());
            return existingUser;
        }

        User user = new User();
        user.setEmail(LOCAL_SETUP_EMAIL);
        user.setUsername(resolveLocalSetupUsername());
        user.setName("Local Channel E2E");
        user.setNickname("Local Channel E2E");
        user.setPassword(passwordEncoder.encode(LOCAL_SETUP_PASSWORD));
        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        created.add("user:" + savedUser.getEmail());
        return savedUser;
    }

    private String resolveLocalSetupUsername() {
        if (!userRepository.existsByUsername(LOCAL_SETUP_USERNAME)) {
            return LOCAL_SETUP_USERNAME;
        }

        for (int index = 1; index <= 999; index++) {
            String candidate = LOCAL_SETUP_USERNAME + "_" + index;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("无法生成本地 E2E 测试用户名");
    }

    private Store ensureLocalSetupStore(User user, List<String> created, List<String> reused) {
        Store existingStore = storeRepository.findBySuHotelId(LOCAL_SETUP_SU_HOTEL_ID).orElse(null);
        if (existingStore != null) {
            reused.add("store:" + existingStore.getId());
            return existingStore;
        }

        Store store = new Store();
        store.setUserId(user.getId());
        store.setName(LOCAL_SETUP_STORE_NAME);
        store.setPhone("0000000000");
        store.setPhoneTechType("mobile");
        store.setType("hotel");
        store.setTimezone("Asia/Shanghai");
        store.setManager("Local E2E");
        store.setOwnerEmail(user.getEmail());
        store.setCountry("CN");
        store.setCity("Local");
        store.setState("Local");
        store.setAddress("Local Channel E2E Test Address");
        store.setCurrency("CNY");
        store.setSuHotelId(LOCAL_SETUP_SU_HOTEL_ID);
        store.setEmail(user.getEmail());
        store.setLanguage("zh-CN");
        Store savedStore = storeRepository.save(store);
        created.add("store:" + savedStore.getId());
        return savedStore;
    }

    private void ensureLocalSetupStoreUser(
            Store store,
            User user,
            List<String> created,
            List<String> reused
    ) {
        StoreUser existingStoreUser = storeUserRepository
                .findByStoreIdAndUserId(store.getId(), user.getId())
                .orElse(null);
        if (existingStoreUser != null) {
            boolean changed = false;
            if (!"owner".equals(existingStoreUser.getRole())) {
                existingStoreUser.setRole("owner");
                changed = true;
            }
            if (!Boolean.TRUE.equals(existingStoreUser.getIsActive())) {
                existingStoreUser.setIsActive(true);
                changed = true;
            }
            if (changed) {
                storeUserRepository.save(existingStoreUser);
            }
            reused.add("storeUser:" + existingStoreUser.getId());
            return;
        }

        StoreUser storeUser = new StoreUser(store, user, "owner");
        storeUser.setIsActive(true);
        StoreUser savedStoreUser = storeUserRepository.save(storeUser);
        created.add("storeUser:" + savedStoreUser.getId());
    }

    private void ensureLocalSetupChannels(Long storeId, List<String> created, List<String> reused) {
        int createdCount = channelBootstrapService.ensureDefaultChannelsForStore(storeId);
        if (createdCount > 0) {
            created.add("channels:" + createdCount);
            return;
        }
        reused.add("channels");
    }

    private PricePlan ensureLocalSetupPricePlan(
            Long storeId,
            User user,
            List<String> created,
            List<String> reused
    ) {
        PricePlan existingPricePlan = pricePlanRepository
                .findByStoreIdAndName(storeId, LOCAL_SETUP_PRICE_PLAN_NAME)
                .orElse(null);
        if (existingPricePlan != null) {
            reused.add("pricePlan:" + existingPricePlan.getId());
            return existingPricePlan;
        }

        PricePlan pricePlan = new PricePlan();
        pricePlan.setStoreId(storeId);
        pricePlan.setUser(user);
        pricePlan.setName(LOCAL_SETUP_PRICE_PLAN_NAME);
        pricePlan.setNameEn("Local E2E Standard Rate");
        pricePlan.setDescription("Local channel E2E bootstrap rate plan");
        pricePlan.setMinNights(1);
        pricePlan.setMaxNights(365);
        pricePlan.setIncludeMeal(false);
        pricePlan.setDerivationType("independent");
        PricePlan savedPricePlan = pricePlanRepository.save(pricePlan);
        created.add("pricePlan:" + savedPricePlan.getId());
        return savedPricePlan;
    }

    private RoomType ensureLocalSetupRoomType(
            Long storeId,
            User user,
            List<String> created,
            List<String> reused
    ) {
        RoomType existingRoomType = roomTypeRepository
                .findByStoreIdAndCode(storeId, LOCAL_SETUP_ROOM_TYPE_CODE)
                .orElse(null);
        if (existingRoomType != null) {
            reused.add("roomType:" + existingRoomType.getId());
            return existingRoomType;
        }

        RoomType roomType = new RoomType();
        roomType.setStoreId(storeId);
        roomType.setUser(user);
        roomType.setName(LOCAL_SETUP_ROOM_TYPE_NAME);
        roomType.setCode(LOCAL_SETUP_ROOM_TYPE_CODE);
        roomType.setTotalRooms(LOCAL_SETUP_ROOM_NUMBERS.size());
        roomType.setMaxGuests(2);
        roomType.setMaxChildOccupancy(0);
        roomType.setDescription("Local channel E2E bootstrap room type");
        roomType.setSuRoomType(LOCAL_SETUP_ROOM_TYPE_CODE);
        roomType.setDefaultPrice(LOCAL_SETUP_DEFAULT_PRICE);
        RoomType savedRoomType = roomTypeRepository.save(roomType);
        created.add("roomType:" + savedRoomType.getId());
        return savedRoomType;
    }

    private List<Room> ensureLocalSetupRooms(
            Long storeId,
            User user,
            RoomType roomType,
            List<String> created,
            List<String> reused
    ) {
        List<Room> rooms = new ArrayList<>();
        for (String roomNumber : LOCAL_SETUP_ROOM_NUMBERS) {
            Room room = roomRepository.findByStoreIdAndRoomNumber(storeId, roomNumber).orElse(null);
            if (room == null) {
                room = createLocalSetupRoom(storeId, user, roomType, roomNumber, created);
            } else {
                room = normalizeLocalSetupRoom(storeId, user, roomType, room, reused);
            }
            rooms.add(room);
        }

        int actualRoomCount = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomType.getId()).size();
        if (roomType.getTotalRooms() == null || roomType.getTotalRooms() < actualRoomCount) {
            roomType.setTotalRooms(actualRoomCount);
            roomTypeRepository.save(roomType);
        }

        return rooms;
    }

    private Room createLocalSetupRoom(
            Long storeId,
            User user,
            RoomType roomType,
            String roomNumber,
            List<String> created
    ) {
        Room room = new Room(roomNumber, roomType, 1, user.getId(), storeId);
        room.setStatus(RoomStatus.AVAILABLE);
        Room savedRoom = roomRepository.save(room);
        created.add("room:" + savedRoom.getRoomNumber());
        return savedRoom;
    }

    private Room normalizeLocalSetupRoom(
            Long storeId,
            User user,
            RoomType roomType,
            Room room,
            List<String> reused
    ) {
        boolean changed = false;
        Long existingRoomTypeId = null;
        if (room.getRoomType() != null) {
            existingRoomTypeId = room.getRoomType().getId();
        }
        if (!roomType.getId().equals(existingRoomTypeId)) {
            room.setRoomType(roomType);
            changed = true;
        }
        if (!storeId.equals(room.getStoreId())) {
            room.setStoreId(storeId);
            changed = true;
        }
        if (!user.getId().equals(room.getUserId())) {
            room.setUserId(user.getId());
            changed = true;
        }
        if (room.getFloor() == null) {
            room.setFloor(1);
            changed = true;
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            room.setStatus(RoomStatus.AVAILABLE);
            changed = true;
        }

        Room savedRoom = room;
        if (changed) {
            savedRoom = roomRepository.save(room);
        }
        reused.add("room:" + savedRoom.getRoomNumber());
        return savedRoom;
    }

    private void ensureLocalSetupOtaIntegration(
            Long storeId,
            String code,
            String name,
            PricePlan pricePlan,
            List<String> created,
            List<String> reused
    ) {
        OtaIntegration existingIntegration = otaIntegrationRepository.findByStoreIdAndCode(storeId, code).orElse(null);
        if (existingIntegration != null) {
            boolean changed = false;
            if (!Boolean.TRUE.equals(existingIntegration.getEnabled())) {
                existingIntegration.setEnabled(true);
                changed = true;
            }
            if (existingIntegration.getDefaultPricePlan() == null) {
                existingIntegration.setDefaultPricePlan(pricePlan);
                changed = true;
            }
            if (normalize(existingIntegration.getPropertyId()) == null) {
                existingIntegration.setPropertyId(LOCAL_SETUP_SU_HOTEL_ID);
                changed = true;
            }
            if (normalize(existingIntegration.getSuPropertyId()) == null) {
                existingIntegration.setSuPropertyId(LOCAL_SETUP_SU_HOTEL_ID);
                changed = true;
            }
            if (changed) {
                otaIntegrationRepository.save(existingIntegration);
            }
            reused.add("otaIntegration:" + code);
            return;
        }

        OtaIntegration integration = new OtaIntegration(name, code);
        integration.setStoreId(storeId);
        integration.setEnabled(true);
        integration.setIsConnected(false);
        integration.setPropertyId(LOCAL_SETUP_SU_HOTEL_ID);
        integration.setSuPropertyId(LOCAL_SETUP_SU_HOTEL_ID);
        integration.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        integration.setPriceAdjustmentValue(BigDecimal.ZERO);
        integration.setAutoSyncPrice(true);
        integration.setDefaultPricePlan(pricePlan);
        otaIntegrationRepository.save(integration);
        created.add("otaIntegration:" + code);
    }

    private Store requireStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));
    }

    private String resolveSuHotelId(Store store) {
        String configuredHotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (configuredHotelId != null) {
            return configuredHotelId;
        }
        return SuHotelIdUtil.buildDefault(store.getId());
    }

    private WebhookConfigSummary buildWebhookConfig(String suHotelId) {
        String baseUrl = normalize(suWebhookConfig.getServerBaseUrl());
        String reservationNotifUrl = normalize(suWebhookConfig.getReservationNotifWebhookUrl(suHotelId));
        boolean configured = baseUrl != null && reservationNotifUrl != null;
        return new WebhookConfigSummary(configured, baseUrl, reservationNotifUrl);
    }

    private List<String> findMissingRequirements(
            String suHotelId,
            List<Channel> channels,
            List<RoomType> roomTypes,
            List<Room> rooms,
            List<PricePlan> pricePlans,
            List<OtaIntegration> otaIntegrations,
            WebhookConfigSummary webhookConfig
    ) {
        List<String> missing = new ArrayList<>();
        if (normalize(suHotelId) == null) {
            missing.add("suHotelId");
        }
        if (!hasSupportedChannel(channels)) {
            missing.add("BOOKING_OR_AIRBNB_channel");
        }
        if (roomTypes.isEmpty()) {
            missing.add("roomTypes");
        }
        if (rooms.isEmpty()) {
            missing.add("rooms");
        }
        if (pricePlans.isEmpty()) {
            missing.add("pricePlans");
        }
        if (!hasSupportedOtaIntegration(otaIntegrations)) {
            missing.add("BOOKING_OR_AIRBNB_otaIntegration");
        }
        if (!webhookConfig.configured()) {
            missing.add("suWebhookConfig");
        }
        return missing;
    }

    private boolean hasSupportedChannel(List<Channel> channels) {
        for (Channel channel : channels) {
            String code = normalize(channel.getCode());
            boolean supported = CHANNEL_CODE_BOOKING.equals(code) || CHANNEL_CODE_AIRBNB.equals(code);
            boolean enabled = Boolean.TRUE.equals(channel.getEnabled()) && Boolean.TRUE.equals(channel.getIsActive());
            if (supported && enabled) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSupportedOtaIntegration(List<OtaIntegration> otaIntegrations) {
        for (OtaIntegration integration : otaIntegrations) {
            String code = normalize(integration.getCode());
            boolean supported = CHANNEL_CODE_BOOKING.equals(code) || CHANNEL_CODE_AIRBNB.equals(code);
            boolean enabled = Boolean.TRUE.equals(integration.getEnabled());
            if (supported && enabled) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    public record SetupLocalResponse(
            String token,
            Long storeId,
            String suHotelId,
            String email,
            Long userId,
            Long roomTypeId,
            Long roomId,
            List<RoomSummary> rooms,
            SetupLocalSummary summary,
            ChannelE2EReadinessResponse readiness
    ) {
    }

    public record SetupLocalSummary(
            List<String> created,
            List<String> reused
    ) {
    }

    public static class TestSupportAccessException extends RuntimeException {
        private final int statusCode;

        public TestSupportAccessException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    public record ChannelE2EReadinessResponse(
            Long storeId,
            String storeName,
            String suHotelId,
            List<String> supportedChannelCodes,
            boolean ready,
            List<String> missingRequirements,
            WebhookConfigSummary webhookConfig,
            List<ChannelSummary> channels,
            List<RoomTypeSummary> roomTypes,
            List<RoomSummary> rooms,
            List<PricePlanSummary> pricePlans,
            List<OtaIntegrationSummary> otaIntegrations
    ) {
    }

    public record WebhookConfigSummary(
            boolean configured,
            String serverBaseUrl,
            String reservationNotifWebhookUrl
    ) {
    }

    public record ChannelSummary(
            Long id,
            String name,
            String code,
            String type,
            Boolean enabled,
            Boolean active,
            Boolean autoSyncPrice,
            String otaPropertyId,
            Long defaultPricePlanId
    ) {
    }

    public record RoomTypeSummary(
            Long id,
            String name,
            String code,
            Integer totalRooms,
            Integer maxGuests,
            Integer maxChildOccupancy,
            BigDecimal defaultPrice,
            String suRoomType
    ) {
    }

    public record RoomSummary(
            Long id,
            String roomNumber,
            Long roomTypeId,
            String roomTypeName,
            String roomTypeCode,
            Integer floor,
            String status
    ) {
    }

    public record PricePlanSummary(
            Long id,
            String name,
            String nameEn,
            Integer minNights,
            Integer maxNights,
            Boolean includeMeal,
            String derivationType,
            Long basePlanId
    ) {
    }

    public record OtaIntegrationSummary(
            Long id,
            String name,
            String code,
            Boolean enabled,
            Boolean connected,
            String propertyId,
            String suPropertyId,
            Boolean autoSyncPrice,
            Long defaultPricePlanId,
            boolean hasApiKey,
            boolean hasApiSecret,
            boolean hasWidgetToken
    ) {
    }

    public record ReservationLookupQuery(
            String reservationNotifId,
            String suReservationId,
            String channelOrderNumber,
            String externalBookingKey,
            String orderNumber
    ) {
        public boolean hasAnyKey() {
            return reservationNotifId != null
                    || suReservationId != null
                    || channelOrderNumber != null
                    || externalBookingKey != null
                    || orderNumber != null;
        }
    }

    public record ReservationLookupResponse(
            ReservationLookupQuery query,
            int totalMatches,
            List<ReservationSummary> reservations
    ) {
    }

    public record ReservationSummary(
            Long id,
            String orderNumber,
            String channelOrderNumber,
            String externalBookingKey,
            String suHotelId,
            String suReservationId,
            String reservationNotifId,
            String roomReservationId,
            String status,
            String guestName,
            String guestPhone,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            BigDecimal totalAmount,
            String currencyCode,
            String otaRoomId,
            Long otaRoomTypeId,
            String otaRoomNumber,
            ChannelSummary channel,
            RoomSummary room,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record WebhookEventLookupQuery(
            String hotelId,
            String reservationNotifId,
            SuWebhookEventStatus status,
            SuWebhookEventType eventType,
            int limit
    ) {
    }

    public record WebhookEventLookupResponse(
            WebhookEventLookupQuery query,
            int totalMatches,
            List<WebhookEventSummary> events
    ) {
    }

    public record WebhookEventSummary(
            Long id,
            Long storeId,
            String hotelId,
            String reservationNotifId,
            String eventType,
            String status,
            Integer retryCount,
            LocalDateTime nextRetryAt,
            String lastError,
            String payloadJson,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record MessagingThreadLookupQuery(
            Integer channelId,
            String threadId,
            String bookingId,
            String externalMessageId,
            Long messageId,
            int limit,
            int messageLimit
    ) {
    }

    public record MessagingThreadLookupResponse(
            MessagingThreadLookupQuery query,
            int totalMatches,
            List<MessagingThreadSummary> threads
    ) {
    }

    public record MessagingThreadSummary(
            Long id,
            Long storeId,
            String suHotelId,
            Integer channelId,
            String threadKey,
            String threadId,
            String bookingId,
            String guestId,
            String bookingFlag,
            String listingId,
            String guestName,
            String listingName,
            String lastMessage,
            LocalDateTime lastActivity,
            Boolean closed,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<MessagingMessageSummary> messages
    ) {
    }

    public record MessagingMessageSummary(
            Long id,
            Long threadDatabaseId,
            String externalMessageId,
            String senderType,
            String senderName,
            String content,
            LocalDateTime sentAt,
            Boolean read,
            String deliveryStatus,
            String rawJson
    ) {
    }
}
