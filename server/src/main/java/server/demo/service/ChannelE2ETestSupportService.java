package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.config.SuWebhookConfig;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
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
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.AutoMessageSendLogRepository;
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
    private static final int DEFAULT_SEND_LOG_LIMIT = 20;
    private static final int MAX_SEND_LOG_LIMIT = 100;
    private static final String CHANNEL_CODE_DIRECT = "DIRECT";
    private static final String CHANNEL_CODE_AIRBNB = "AIRBNB";
    private static final String CHANNEL_CODE_BOOKING = "BOOKING";
    private static final String AUTO_MESSAGE_ACTION_BOOKING_CONFIRM = "BOOKING_CONFIRM";
    private static final String AUTO_MESSAGE_TIMING_IMMEDIATELY = "IMMEDIATELY";
    private static final String AUTO_MESSAGE_TARGET_TYPE_RESERVATION = "RESERVATION";
    private static final String LOCAL_SETUP_AUTO_MESSAGE_TITLE = "Local E2E Booking Confirm";
    private static final String LOCAL_SETUP_AUTO_MESSAGE_MARKER = "LOCAL_E2E_AUTO_BOOKING_CONFIRM";
    private static final String LOCAL_SETUP_AUTO_MESSAGE_BODY =
            "LOCAL_E2E_AUTO_BOOKING_CONFIRM Booking confirmed for {{guest_name}} / {{reservation_number}}.";
    private static final List<String> LOCAL_SETUP_CHANNEL_CODES = List.of(
            CHANNEL_CODE_DIRECT,
            CHANNEL_CODE_AIRBNB,
            CHANNEL_CODE_BOOKING
    );
    private static final List<String> LOCAL_SETUP_OTA_CODES = List.of(
            CHANNEL_CODE_BOOKING,
            CHANNEL_CODE_AIRBNB
    );
    private static final String LOCAL_SETUP_EMAIL = "channel-e2e-local@thehosthub.test";
    private static final String LOCAL_SETUP_USERNAME = "local_channel_e2e";
    private static final String LOCAL_SETUP_PASSWORD = "local-channel-e2e-password";
    private static final String LOCAL_SETUP_STORE_NAME = "Local Channel E2E Hotel";
    private static final String LOCAL_SETUP_SU_HOTEL_ID = "LOCALE2EHOTEL";
    private static final String LOCAL_SETUP_TIME_ZONE = "Asia/Tokyo";
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
    private final AutoMessageRepository autoMessageRepository;
    private final AutoMessageSendLogRepository autoMessageSendLogRepository;
    private final ReservationRepository reservationRepository;
    private final SuReservationWebhookEventRepository webhookEventRepository;
    private final SuMessageThreadRepository suMessageThreadRepository;
    private final SuWebhookConfig suWebhookConfig;
    private final ChannelBootstrapService channelBootstrapService;
    private final AutoMessageTriggerService autoMessageTriggerService;
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
            AutoMessageRepository autoMessageRepository,
            AutoMessageSendLogRepository autoMessageSendLogRepository,
            ReservationRepository reservationRepository,
            SuReservationWebhookEventRepository webhookEventRepository,
            SuMessageThreadRepository suMessageThreadRepository,
            SuWebhookConfig suWebhookConfig,
            ChannelBootstrapService channelBootstrapService,
            AutoMessageTriggerService autoMessageTriggerService,
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
        this.autoMessageRepository = autoMessageRepository;
        this.autoMessageSendLogRepository = autoMessageSendLogRepository;
        this.reservationRepository = reservationRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.suMessageThreadRepository = suMessageThreadRepository;
        this.suWebhookConfig = suWebhookConfig;
        this.channelBootstrapService = channelBootstrapService;
        this.autoMessageTriggerService = autoMessageTriggerService;
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
        Channel bookingChannel = requireLocalSetupBookingChannel(store.getId());
        PricePlan pricePlan = ensureLocalSetupPricePlan(store.getId(), user, created, reused);
        RoomType roomType = ensureLocalSetupRoomType(store.getId(), user, created, reused);
        List<Room> rooms = ensureLocalSetupRooms(store.getId(), user, roomType, created, reused);
        Room primaryRoom = rooms.get(0);
        ensureLocalSetupOtaIntegration(store.getId(), CHANNEL_CODE_BOOKING, "Booking.com", pricePlan, created, reused);
        ensureLocalSetupOtaIntegration(store.getId(), CHANNEL_CODE_AIRBNB, "Airbnb", pricePlan, created, reused);
        ensureLocalSetupAutoMessageTemplate(store.getId(), bookingChannel, created, reused);

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
        List<AutoMessage> autoMessages = autoMessageRepository.findByStoreId(storeId);
        boolean localSetupStore = isLocalSetupStore(store);
        if (localSetupStore) {
            roomTypes = selectLocalSetupRoomTypes(roomTypes);
            rooms = selectLocalSetupRooms(rooms, firstRoomType(roomTypes));
            pricePlans = selectLocalSetupPricePlans(pricePlans);
            channels = selectLocalSetupChannels(channels);
            otaIntegrations = selectLocalSetupOtaIntegrations(otaIntegrations);
            autoMessages = selectLocalSetupAutoMessages(autoMessages);
        }

        WebhookConfigSummary webhookConfig = buildWebhookConfig(suHotelId);
        List<String> missingRequirements = findMissingRequirements(
                suHotelId,
                channels,
                roomTypes,
                rooms,
                pricePlans,
                otaIntegrations,
                autoMessages,
                localSetupStore,
                webhookConfig
        );

        boolean ready = missingRequirements.isEmpty();
        return new ChannelE2EReadinessResponse(
                store.getId(),
                store.getName(),
                store.getTimezone(),
                suHotelId,
                List.of(CHANNEL_CODE_BOOKING, CHANNEL_CODE_AIRBNB),
                ready,
                missingRequirements,
                webhookConfig,
                responseMapper.toChannelSummaries(channels),
                responseMapper.toRoomTypeSummaries(roomTypes),
                responseMapper.toRoomSummaries(rooms),
                responseMapper.toPricePlanSummaries(pricePlans),
                responseMapper.toOtaIntegrationSummaries(otaIntegrations),
                toAutoMessageSummaries(autoMessages, channels)
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

    @Transactional(readOnly = true)
    public AutoMessageSendLogLookupResponse lookupAutoMessageSendLogs(
            Long storeId,
            Long targetId,
            Long autoMessageId,
            Boolean success,
            Integer limit
    ) {
        int pageSize = clampSendLogLimit(limit);
        AutoMessageSendLogLookupQuery query = new AutoMessageSendLogLookupQuery(
                AUTO_MESSAGE_TARGET_TYPE_RESERVATION,
                targetId,
                autoMessageId,
                success,
                pageSize
        );
        List<AutoMessageSendLog> logs = autoMessageSendLogRepository.findRecentByStoreIdAndFilters(
                storeId,
                query.targetType(),
                query.targetId(),
                query.autoMessageId(),
                query.success(),
                PageRequest.of(0, pageSize)
        );

        List<AutoMessageSendLogSummary> items = new ArrayList<>();
        for (AutoMessageSendLog log : logs) {
            items.add(toAutoMessageSendLogSummary(log));
        }

        return new AutoMessageSendLogLookupResponse(query, items.size(), items);
    }

    @Transactional
    public AutoMessageDispatchResponse dispatchAutoMessages(Long storeId) {
        autoMessageTriggerService.dispatchStoreOnce(storeId);
        return new AutoMessageDispatchResponse(storeId, true);
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
        Store existingStore = firstStore(storeRepository.findAllBySuHotelIdOrderByIdAsc(LOCAL_SETUP_SU_HOTEL_ID));
        if (existingStore != null) {
            if (!LOCAL_SETUP_TIME_ZONE.equals(existingStore.getTimezone())) {
                existingStore.setTimezone(LOCAL_SETUP_TIME_ZONE);
                existingStore = storeRepository.save(existingStore);
                reused.add("storeTimezone:" + LOCAL_SETUP_TIME_ZONE);
            }
            reused.add("store:" + existingStore.getId());
            return existingStore;
        }

        Store store = new Store();
        store.setUserId(user.getId());
        store.setName(LOCAL_SETUP_STORE_NAME);
        store.setPhone("0000000000");
        store.setPhoneTechType("mobile");
        store.setType("hotel");
        store.setTimezone(LOCAL_SETUP_TIME_ZONE);
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

    private Channel requireLocalSetupBookingChannel(Long storeId) {
        return channelRepository.findByStoreIdAndCode(storeId, CHANNEL_CODE_BOOKING)
                .orElseThrow(() -> new IllegalStateException("本地 E2E Booking 渠道不存在"));
    }

    private PricePlan ensureLocalSetupPricePlan(
            Long storeId,
            User user,
            List<String> created,
            List<String> reused
    ) {
        PricePlan existingPricePlan = firstPricePlan(
                pricePlanRepository.findAllByStoreIdAndNameOrderByIdAsc(storeId, LOCAL_SETUP_PRICE_PLAN_NAME)
        );
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
        RoomType existingRoomType = firstRoomType(
                roomTypeRepository.findAllByStoreIdAndCodeOrderByIdAsc(storeId, LOCAL_SETUP_ROOM_TYPE_CODE)
        );
        if (existingRoomType == null) {
            existingRoomType = firstRoomType(
                    roomTypeRepository.findAllByStoreIdAndNameOrderByIdAsc(storeId, LOCAL_SETUP_ROOM_TYPE_NAME)
            );
        }
        if (existingRoomType != null) {
            existingRoomType = normalizeLocalSetupRoomType(existingRoomType, user);
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
            Room room = firstRoom(roomRepository.findAllByStoreIdAndRoomNumberOrderByIdAsc(storeId, roomNumber));
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
        OtaIntegration existingIntegration = firstOtaIntegration(
                otaIntegrationRepository.findAllByStoreIdAndCodeOrderByIdAsc(storeId, code)
        );
        if (existingIntegration == null) {
            existingIntegration = firstOtaIntegration(
                    otaIntegrationRepository.findAllByStoreIdAndNameOrderByIdAsc(storeId, name)
            );
        }
        if (existingIntegration != null) {
            boolean changed = false;
            if (!code.equals(existingIntegration.getCode())) {
                existingIntegration.setCode(code);
                changed = true;
            }
            if (!name.equals(existingIntegration.getName())) {
                existingIntegration.setName(name);
                changed = true;
            }
            if (!Boolean.TRUE.equals(existingIntegration.getEnabled())) {
                existingIntegration.setEnabled(true);
                changed = true;
            }
            if (!Boolean.TRUE.equals(existingIntegration.getIsConnected())) {
                existingIntegration.setIsConnected(true);
                changed = true;
            }
            if (existingIntegration.getConnectedAt() == null) {
                existingIntegration.setConnectedAt(LocalDateTime.now());
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
        integration.setIsConnected(true);
        integration.setConnectedAt(LocalDateTime.now());
        integration.setPropertyId(LOCAL_SETUP_SU_HOTEL_ID);
        integration.setSuPropertyId(LOCAL_SETUP_SU_HOTEL_ID);
        integration.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        integration.setPriceAdjustmentValue(BigDecimal.ZERO);
        integration.setAutoSyncPrice(true);
        integration.setDefaultPricePlan(pricePlan);
        otaIntegrationRepository.save(integration);
        created.add("otaIntegration:" + code);
    }

    private AutoMessage ensureLocalSetupAutoMessageTemplate(
            Long storeId,
            Channel bookingChannel,
            List<String> created,
            List<String> reused
    ) {
        List<AutoMessage> candidates = autoMessageRepository.findByStoreIdAndActionAndSendTimingOrderByIdAsc(
                storeId,
                AUTO_MESSAGE_ACTION_BOOKING_CONFIRM,
                AUTO_MESSAGE_TIMING_IMMEDIATELY
        );
        AutoMessage selected = firstLocalSetupAutoMessage(candidates, bookingChannel);
        if (selected == null) {
            selected = new AutoMessage();
            selected.setStoreId(storeId);
            selected.setTitle(LOCAL_SETUP_AUTO_MESSAGE_TITLE);
            selected.setAction(AUTO_MESSAGE_ACTION_BOOKING_CONFIRM);
            selected.setSendTiming(AUTO_MESSAGE_TIMING_IMMEDIATELY);
            selected.setAutomationRule(AUTO_MESSAGE_ACTION_BOOKING_CONFIRM);
            selected.setRoomSelectionType("ALL_LOCAL");
            selected.setRoomSelection(null);
            selected.setResendOnExpire(false);
            selected.setEnabled(true);
            selected.setMessage(LOCAL_SETUP_AUTO_MESSAGE_BODY);
            selected.setChannels(buildSingleChannelJson(bookingChannel.getId()));
            AutoMessage saved = autoMessageRepository.save(selected);
            created.add("autoMessage:" + saved.getId());
            return saved;
        }

        boolean changed = normalizeLocalSetupAutoMessageTemplate(selected, storeId, bookingChannel);
        AutoMessage saved = selected;
        if (changed) {
            saved = autoMessageRepository.save(selected);
        }
        reused.add("autoMessage:" + saved.getId());
        return saved;
    }

    private AutoMessage firstLocalSetupAutoMessage(List<AutoMessage> candidates, Channel bookingChannel) {
        AutoMessage fallback = null;
        for (AutoMessage candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (hasLocalSetupAutoMessageTemplate(candidate, bookingChannel)) {
                return candidate;
            }
            if (fallback == null || hasLowerId(candidate.getId(), fallback.getId())) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private boolean normalizeLocalSetupAutoMessageTemplate(
            AutoMessage template,
            Long storeId,
            Channel bookingChannel
    ) {
        boolean changed = false;
        if (!storeId.equals(template.getStoreId())) {
            template.setStoreId(storeId);
            changed = true;
        }
        if (!LOCAL_SETUP_AUTO_MESSAGE_TITLE.equals(template.getTitle())) {
            template.setTitle(LOCAL_SETUP_AUTO_MESSAGE_TITLE);
            changed = true;
        }
        if (!LOCAL_SETUP_AUTO_MESSAGE_BODY.equals(template.getMessage())) {
            template.setMessage(LOCAL_SETUP_AUTO_MESSAGE_BODY);
            changed = true;
        }
        if (!AUTO_MESSAGE_ACTION_BOOKING_CONFIRM.equals(template.getAction())) {
            template.setAction(AUTO_MESSAGE_ACTION_BOOKING_CONFIRM);
            changed = true;
        }
        if (!AUTO_MESSAGE_TIMING_IMMEDIATELY.equals(template.getSendTiming())) {
            template.setSendTiming(AUTO_MESSAGE_TIMING_IMMEDIATELY);
            changed = true;
        }
        if (!AUTO_MESSAGE_ACTION_BOOKING_CONFIRM.equals(template.getAutomationRule())) {
            template.setAutomationRule(AUTO_MESSAGE_ACTION_BOOKING_CONFIRM);
            changed = true;
        }
        String expectedChannels = buildSingleChannelJson(bookingChannel.getId());
        if (!expectedChannels.equals(template.getChannels())) {
            template.setChannels(expectedChannels);
            changed = true;
        }
        if (!"ALL_LOCAL".equals(template.getRoomSelectionType())) {
            template.setRoomSelectionType("ALL_LOCAL");
            changed = true;
        }
        if (template.getRoomSelection() != null) {
            template.setRoomSelection(null);
            changed = true;
        }
        if (!Boolean.FALSE.equals(template.getResendOnExpire())) {
            template.setResendOnExpire(false);
            changed = true;
        }
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            template.setEnabled(true);
            changed = true;
        }
        return changed;
    }

    private List<Channel> selectLocalSetupChannels(List<Channel> channels) {
        List<Channel> selected = new ArrayList<>();
        for (String code : LOCAL_SETUP_CHANNEL_CODES) {
            Channel channel = selectChannelByCode(channels, code);
            if (channel != null) {
                selected.add(channel);
            }
        }
        return selected;
    }

    private List<RoomType> selectLocalSetupRoomTypes(List<RoomType> roomTypes) {
        RoomType roomType = selectRoomTypeByCode(roomTypes, LOCAL_SETUP_ROOM_TYPE_CODE);
        if (roomType == null) {
            roomType = selectRoomTypeByName(roomTypes, LOCAL_SETUP_ROOM_TYPE_NAME);
        }
        if (roomType == null) {
            return List.of();
        }
        return List.of(roomType);
    }

    private List<Room> selectLocalSetupRooms(List<Room> rooms, RoomType roomType) {
        List<Room> selected = new ArrayList<>();
        for (String roomNumber : LOCAL_SETUP_ROOM_NUMBERS) {
            Room room = selectRoomByNumber(rooms, roomNumber, roomType);
            if (room != null) {
                selected.add(room);
            }
        }
        return selected;
    }

    private List<PricePlan> selectLocalSetupPricePlans(List<PricePlan> pricePlans) {
        PricePlan pricePlan = selectPricePlanByName(pricePlans, LOCAL_SETUP_PRICE_PLAN_NAME);
        if (pricePlan == null) {
            return List.of();
        }
        return List.of(pricePlan);
    }

    private List<OtaIntegration> selectLocalSetupOtaIntegrations(List<OtaIntegration> otaIntegrations) {
        List<OtaIntegration> selected = new ArrayList<>();
        for (String code : LOCAL_SETUP_OTA_CODES) {
            OtaIntegration integration = selectOtaIntegrationByCode(otaIntegrations, code);
            if (integration != null) {
                selected.add(integration);
            }
        }
        return selected;
    }

    private List<AutoMessage> selectLocalSetupAutoMessages(List<AutoMessage> autoMessages) {
        List<AutoMessage> selected = new ArrayList<>();
        for (AutoMessage autoMessage : autoMessages) {
            if (autoMessage == null) {
                continue;
            }
            if (!matchesText(autoMessage.getAction(), AUTO_MESSAGE_ACTION_BOOKING_CONFIRM)) {
                continue;
            }
            if (!matchesText(autoMessage.getSendTiming(), AUTO_MESSAGE_TIMING_IMMEDIATELY)) {
                continue;
            }
            if (!containsLocalSetupMarker(autoMessage.getMessage())) {
                continue;
            }
            selected.add(autoMessage);
        }
        return selected;
    }

    private Channel selectChannelByCode(List<Channel> channels, String code) {
        Channel selected = null;
        for (Channel channel : channels) {
            if (!matchesText(channel.getCode(), code)) {
                continue;
            }
            if (selected == null || isBetterLocalSetupChannel(channel, selected)) {
                selected = channel;
            }
        }
        return selected;
    }

    private RoomType selectRoomTypeByCode(List<RoomType> roomTypes, String code) {
        RoomType selected = null;
        for (RoomType roomType : roomTypes) {
            if (!matchesText(roomType.getCode(), code)) {
                continue;
            }
            if (selected == null || hasLowerId(roomType.getId(), selected.getId())) {
                selected = roomType;
            }
        }
        return selected;
    }

    private RoomType selectRoomTypeByName(List<RoomType> roomTypes, String name) {
        RoomType selected = null;
        for (RoomType roomType : roomTypes) {
            if (!matchesText(roomType.getName(), name)) {
                continue;
            }
            if (selected == null || hasLowerId(roomType.getId(), selected.getId())) {
                selected = roomType;
            }
        }
        return selected;
    }

    private Room selectRoomByNumber(List<Room> rooms, String roomNumber, RoomType roomType) {
        Room selected = null;
        for (Room room : rooms) {
            if (!matchesText(room.getRoomNumber(), roomNumber)) {
                continue;
            }
            if (selected == null || isBetterLocalSetupRoom(room, selected, roomType)) {
                selected = room;
            }
        }
        return selected;
    }

    private PricePlan selectPricePlanByName(List<PricePlan> pricePlans, String name) {
        PricePlan selected = null;
        for (PricePlan pricePlan : pricePlans) {
            if (!matchesText(pricePlan.getName(), name)) {
                continue;
            }
            if (selected == null || hasLowerId(pricePlan.getId(), selected.getId())) {
                selected = pricePlan;
            }
        }
        return selected;
    }

    private OtaIntegration selectOtaIntegrationByCode(List<OtaIntegration> otaIntegrations, String code) {
        OtaIntegration selected = null;
        for (OtaIntegration integration : otaIntegrations) {
            if (!matchesText(integration.getCode(), code)) {
                continue;
            }
            if (selected == null || isBetterLocalSetupOtaIntegration(integration, selected)) {
                selected = integration;
            }
        }
        return selected;
    }

    private boolean isBetterLocalSetupChannel(Channel candidate, Channel selected) {
        boolean candidateReady = Boolean.TRUE.equals(candidate.getEnabled())
                && Boolean.TRUE.equals(candidate.getIsActive());
        boolean selectedReady = Boolean.TRUE.equals(selected.getEnabled())
                && Boolean.TRUE.equals(selected.getIsActive());
        if (candidateReady != selectedReady) {
            return candidateReady;
        }
        return hasLowerId(candidate.getId(), selected.getId());
    }

    private boolean isBetterLocalSetupRoom(Room candidate, Room selected, RoomType roomType) {
        Long roomTypeId = roomType != null ? roomType.getId() : null;
        boolean candidateMatchesRoomType = roomTypeId != null
                && candidate.getRoomType() != null
                && roomTypeId.equals(candidate.getRoomType().getId());
        boolean selectedMatchesRoomType = roomTypeId != null
                && selected.getRoomType() != null
                && roomTypeId.equals(selected.getRoomType().getId());
        if (candidateMatchesRoomType != selectedMatchesRoomType) {
            return candidateMatchesRoomType;
        }

        boolean candidateAvailable = candidate.getStatus() == RoomStatus.AVAILABLE;
        boolean selectedAvailable = selected.getStatus() == RoomStatus.AVAILABLE;
        if (candidateAvailable != selectedAvailable) {
            return candidateAvailable;
        }

        return hasLowerId(candidate.getId(), selected.getId());
    }

    private boolean isBetterLocalSetupOtaIntegration(OtaIntegration candidate, OtaIntegration selected) {
        boolean candidateReady = Boolean.TRUE.equals(candidate.getEnabled())
                && Boolean.TRUE.equals(candidate.getIsConnected());
        boolean selectedReady = Boolean.TRUE.equals(selected.getEnabled())
                && Boolean.TRUE.equals(selected.getIsConnected());
        if (candidateReady != selectedReady) {
            return candidateReady;
        }
        return hasLowerId(candidate.getId(), selected.getId());
    }

    private RoomType normalizeLocalSetupRoomType(RoomType roomType, User user) {
        boolean changed = false;
        if (!LOCAL_SETUP_ROOM_TYPE_CODE.equals(roomType.getCode())) {
            roomType.setCode(LOCAL_SETUP_ROOM_TYPE_CODE);
            changed = true;
        }
        if (!LOCAL_SETUP_ROOM_TYPE_NAME.equals(roomType.getName())) {
            roomType.setName(LOCAL_SETUP_ROOM_TYPE_NAME);
            changed = true;
        }
        if (!LOCAL_SETUP_ROOM_TYPE_CODE.equals(roomType.getSuRoomType())) {
            roomType.setSuRoomType(LOCAL_SETUP_ROOM_TYPE_CODE);
            changed = true;
        }
        if (!LOCAL_SETUP_DEFAULT_PRICE.equals(roomType.getDefaultPrice())) {
            roomType.setDefaultPrice(LOCAL_SETUP_DEFAULT_PRICE);
            changed = true;
        }
        if (roomType.getMaxGuests() == null || roomType.getMaxGuests() < 1) {
            roomType.setMaxGuests(2);
            changed = true;
        }
        if (roomType.getMaxChildOccupancy() == null) {
            roomType.setMaxChildOccupancy(0);
            changed = true;
        }
        if (roomType.getUser() == null) {
            roomType.setUser(user);
            changed = true;
        }
        if (changed) {
            return roomTypeRepository.save(roomType);
        }
        return roomType;
    }

    private boolean isLocalSetupStore(Store store) {
        return matchesText(store.getSuHotelId(), LOCAL_SETUP_SU_HOTEL_ID);
    }

    private Store firstStore(List<Store> stores) {
        if (stores.isEmpty()) {
            return null;
        }
        return stores.get(0);
    }

    private PricePlan firstPricePlan(List<PricePlan> pricePlans) {
        if (pricePlans.isEmpty()) {
            return null;
        }
        return pricePlans.get(0);
    }

    private RoomType firstRoomType(List<RoomType> roomTypes) {
        if (roomTypes.isEmpty()) {
            return null;
        }
        return roomTypes.get(0);
    }

    private Room firstRoom(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return null;
        }
        return rooms.get(0);
    }

    private OtaIntegration firstOtaIntegration(List<OtaIntegration> integrations) {
        if (integrations.isEmpty()) {
            return null;
        }
        return integrations.get(0);
    }

    private boolean matchesText(String actual, String expected) {
        String normalizedActual = normalize(actual);
        String normalizedExpected = normalize(expected);
        if (normalizedActual == null || normalizedExpected == null) {
            return false;
        }
        return normalizedActual.equalsIgnoreCase(normalizedExpected);
    }

    private boolean hasLowerId(Long candidateId, Long selectedId) {
        if (candidateId == null) {
            return false;
        }
        if (selectedId == null) {
            return true;
        }
        return candidateId < selectedId;
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
            List<AutoMessage> autoMessages,
            boolean requireAutoMessage,
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
        if (requireAutoMessage && !hasLocalSetupReadyAutoMessage(autoMessages, channels)) {
            missing.add("BOOKING_CONFIRM_autoMessageTemplate");
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

    private boolean hasLocalSetupReadyAutoMessage(List<AutoMessage> autoMessages, List<Channel> channels) {
        Channel bookingChannel = selectChannelByCode(channels, CHANNEL_CODE_BOOKING);
        for (AutoMessage autoMessage : autoMessages) {
            if (hasLocalSetupAutoMessageTemplate(autoMessage, bookingChannel)) {
                return true;
            }
        }
        return false;
    }

    private List<AutoMessageSummary> toAutoMessageSummaries(
            List<AutoMessage> autoMessages,
            List<Channel> selectedChannels
    ) {
        List<AutoMessageSummary> summaries = new ArrayList<>();
        Channel bookingChannel = selectChannelByCode(selectedChannels, CHANNEL_CODE_BOOKING);
        for (AutoMessage autoMessage : autoMessages) {
            boolean bookingOnly = bookingChannel != null
                    && channelsJsonMatchesSingleChannelId(autoMessage.getChannels(), bookingChannel.getId());
            summaries.add(new AutoMessageSummary(
                    autoMessage.getId(),
                    autoMessage.getTitle(),
                    autoMessage.getAction(),
                    autoMessage.getSendTiming(),
                    autoMessage.getEnabled(),
                    autoMessage.getChannels(),
                    containsLocalSetupMarker(autoMessage.getMessage()),
                    bookingOnly
            ));
        }
        return summaries;
    }

    private boolean hasLocalSetupAutoMessageTemplate(AutoMessage template, Channel bookingChannel) {
        if (template == null || bookingChannel == null) {
            return false;
        }
        boolean enabled = Boolean.TRUE.equals(template.getEnabled());
        boolean actionMatched = matchesText(template.getAction(), AUTO_MESSAGE_ACTION_BOOKING_CONFIRM);
        boolean timingMatched = matchesText(template.getSendTiming(), AUTO_MESSAGE_TIMING_IMMEDIATELY);
        boolean markerPresent = containsLocalSetupMarker(template.getMessage());
        boolean bookingChannelMatched = channelsJsonMatchesSingleChannelId(template.getChannels(), bookingChannel.getId());
        return enabled && actionMatched && timingMatched && markerPresent && bookingChannelMatched;
    }

    private boolean channelsJsonMatchesSingleChannelId(String channelsJson, Long channelId) {
        if (channelsJson == null || channelId == null) {
            return false;
        }
        String normalized = channelsJson.trim();
        if (!normalized.startsWith("[") || !normalized.endsWith("]")) {
            return false;
        }
        String body = normalized.substring(1, normalized.length() - 1);
        String[] parts = body.split(",");
        int parsedCount = 0;
        boolean matched = false;
        for (String part : parts) {
            String value = part.trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            try {
                Long parsed = Long.valueOf(value);
                parsedCount++;
                if (channelId.equals(parsed)) {
                    matched = true;
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return parsedCount == 1 && matched;
    }

    private String buildSingleChannelJson(Long channelId) {
        if (channelId == null) {
            return "[]";
        }
        return "[" + channelId + "]";
    }

    private boolean containsLocalSetupMarker(String value) {
        return value != null && value.contains(LOCAL_SETUP_AUTO_MESSAGE_MARKER);
    }

    private int clampSendLogLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SEND_LOG_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_SEND_LOG_LIMIT);
    }

    private AutoMessageSendLogSummary toAutoMessageSendLogSummary(AutoMessageSendLog log) {
        return new AutoMessageSendLogSummary(
                log.getId(),
                log.getStoreId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getAutoMessageId(),
                log.getSuccess(),
                log.getErrorMessage(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }

    private boolean hasSupportedOtaIntegration(List<OtaIntegration> otaIntegrations) {
        for (OtaIntegration integration : otaIntegrations) {
            String code = normalize(integration.getCode());
            boolean supported = CHANNEL_CODE_BOOKING.equals(code) || CHANNEL_CODE_AIRBNB.equals(code);
            boolean enabled = Boolean.TRUE.equals(integration.getEnabled());
            boolean connected = Boolean.TRUE.equals(integration.getIsConnected());
            if (supported && enabled && connected) {
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
            String storeTimezone,
            String suHotelId,
            List<String> supportedChannelCodes,
            boolean ready,
            List<String> missingRequirements,
            WebhookConfigSummary webhookConfig,
            List<ChannelSummary> channels,
            List<RoomTypeSummary> roomTypes,
            List<RoomSummary> rooms,
            List<PricePlanSummary> pricePlans,
            List<OtaIntegrationSummary> otaIntegrations,
            List<AutoMessageSummary> autoMessages
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

    public record AutoMessageSummary(
            Long id,
            String title,
            String action,
            String sendTiming,
            Boolean enabled,
            String channels,
            boolean markerPresent,
            boolean bookingOnly
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

    public record AutoMessageSendLogLookupQuery(
            String targetType,
            Long targetId,
            Long autoMessageId,
            Boolean success,
            int limit
    ) {
    }

    public record AutoMessageSendLogLookupResponse(
            AutoMessageSendLogLookupQuery query,
            int totalMatches,
            List<AutoMessageSendLogSummary> logs
    ) {
    }

    public record AutoMessageSendLogSummary(
            Long id,
            Long storeId,
            String action,
            String targetType,
            Long targetId,
            Long autoMessageId,
            Boolean success,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AutoMessageDispatchResponse(
            Long storeId,
            boolean dispatched
    ) {
    }
}
