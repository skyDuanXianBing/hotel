package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.constants.SaasFeatureCodes;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.Channel;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.entity.IndependentSitePublication;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.Store;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePaymentProvider;
import server.demo.enums.IndependentSitePublicationType;
import server.demo.enums.PriceAdjustmentType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSitePublicationRepository;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.service.saas.EntitlementService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSiteQuoteServiceTest {

    private IndependentSiteRepository siteRepository;
    private IndependentSitePublicationRepository publicationRepository;
    private IndependentSitePageRepository pageRepository;
    private StoreRepository storeRepository;
    private RoomTypeRepository roomTypeRepository;
    private RoomRepository roomRepository;
    private RoomTypePricePlanRepository mappingRepository;
    private RoomPriceRepository roomPriceRepository;
    private ReservationRepository reservationRepository;
    private RoomBlockoutRepository roomBlockoutRepository;
    private ObjectMapper objectMapper;
    private IndependentSitePageSchemaValidator schemaValidator;
    private Clock clock;
    private EntitlementService entitlementService;
    private IndependentSiteQuoteService service;
    private Map<String, IndependentSite> sitesBySlug;
    private Map<Long, Store> storesById;
    private Map<String, List<IndependentSitePublication>> publicationsBySite;
    private Map<String, List<IndependentSitePage>> pagesBySite;
    private Map<String, RoomType> roomTypesByStoreAndId;
    private Map<String, RoomTypePricePlan> mappingsByStoreRoomTypeAndPlan;
    private List<RoomPrice> roomPrices;
    private List<Room> rooms;
    private List<Reservation> reservations;
    private List<ReservationRepository.ReservationOccupancyRow> occupancyRows;
    private List<Long> roomTypeListLookupStoreIds;

    @BeforeEach
    void setUp() {
        sitesBySlug = new LinkedHashMap<>();
        storesById = new LinkedHashMap<>();
        publicationsBySite = new LinkedHashMap<>();
        pagesBySite = new LinkedHashMap<>();
        roomTypesByStoreAndId = new LinkedHashMap<>();
        mappingsByStoreRoomTypeAndPlan = new LinkedHashMap<>();
        roomPrices = List.of();
        rooms = List.of();
        reservations = List.of();
        occupancyRows = List.of();
        roomTypeListLookupStoreIds = new ArrayList<>();

        siteRepository = repository(IndependentSiteRepository.class, this::handleSiteRepository);
        publicationRepository = repository(
                IndependentSitePublicationRepository.class,
                this::handlePublicationRepository
        );
        pageRepository = repository(
                IndependentSitePageRepository.class,
                this::handlePageRepository
        );
        storeRepository = repository(StoreRepository.class, this::handleStoreRepository);
        roomTypeRepository = repository(RoomTypeRepository.class, this::handleRoomTypeRepository);
        roomRepository = repository(RoomRepository.class, this::handleRoomRepository);
        mappingRepository = repository(
                RoomTypePricePlanRepository.class,
                this::handleMappingRepository
        );
        roomPriceRepository = repository(RoomPriceRepository.class, this::handleRoomPriceRepository);
        reservationRepository = repository(ReservationRepository.class, this::handleReservationRepository);
        roomBlockoutRepository = repository(RoomBlockoutRepository.class, this::handleBlockoutRepository);
        objectMapper = new ObjectMapper();
        schemaValidator = new IndependentSitePageSchemaValidator(objectMapper);
        clock = Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC);
        // 缺省按“权益具备”处理，既有用例不受影响；closed 语义由专门用例覆盖
        entitlementService = Mockito.mock(EntitlementService.class);
        Mockito.lenient().when(entitlementService.storeHasFeature(Mockito.any(), Mockito.any()))
                .thenReturn(true);
        service = new IndependentSiteQuoteService(
                siteRepository,
                publicationRepository,
                pageRepository,
                storeRepository,
                roomTypeRepository,
                roomRepository,
                mappingRepository,
                roomPriceRepository,
                reservationRepository,
                roomBlockoutRepository,
                schemaValidator,
                objectMapper,
                clock,
                new IndependentSiteCanvasValidator(objectMapper),
                entitlementService
        );
    }

    @Test
    void getPublicSite_shouldResolveEachSlugToItsOwnStoreWithoutClientStoreId() throws Exception {
        IndependentSite alpha = site(1L, 11L, "alpha");
        IndependentSite beta = site(2L, 22L, "beta");
        Store storeA = store(1L, "Alpha Hotel");
        Store storeB = store(2L, "Beta Hotel");
        RoomType roomTypeA = roomType(101L, 1L, "Alpha Room");
        RoomType roomTypeB = roomType(202L, 2L, "Beta Room");

        sitesBySlug.put("alpha", alpha);
        sitesBySlug.put("beta", beta);
        storesById.put(1L, storeA);
        storesById.put(2L, storeB);
        publicationsBySite.put(
                siteKey(1L, 11L),
                List.of(publication(alpha, IndependentSitePublicationType.ROOM_TYPE, 101L))
        );
        publicationsBySite.put(
                siteKey(2L, 22L),
                List.of(publication(beta, IndependentSitePublicationType.ROOM_TYPE, 202L))
        );
        roomTypesByStoreAndId.put(entityKey(1L, 101L), roomTypeA);
        roomTypesByStoreAndId.put(entityKey(2L, 202L), roomTypeB);

        IndependentSiteDtos.PublicSiteResponse alphaResponse = service.getPublicSite("alpha");
        IndependentSiteDtos.PublicSiteResponse betaResponse = service.getPublicSite("beta");

        assertEquals("Alpha Hotel", alphaResponse.name());
        assertEquals(101L, alphaResponse.roomTypes().get(0).id());
        assertEquals("Beta Hotel", betaResponse.name());
        assertEquals(202L, betaResponse.roomTypes().get(0).id());
        assertEquals(List.of(1L, 2L), roomTypeListLookupStoreIds);
    }

    @Test
    void getPublicSite_shouldRejectEnabledSiteWithoutExplicitPagePublication() {
        IndependentSite site = site(1L, 11L, "alpha");
        site.setPublishedAt(null);
        sitesBySlug.put("alpha", site);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.getPublicSite("alpha")
        );

        assertEquals("SITE_UNAVAILABLE", exception.getCode());
    }

    @Test
    void getPublicSite_shouldRejectWhenSharedChannelDisabled() {
        IndependentSite site = site(1L, 11L, "alpha");
        site.getChannel().setEnabled(false);
        sitesBySlug.put("alpha", site);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.getPublicSite("alpha")
        );

        assertEquals("SITE_UNAVAILABLE", exception.getCode());
    }

    @Test
    void getPublicSite_shouldExposeThemeKeyAndPublishedPageNavigation() {
        IndependentSite site = site(1L, 11L, "alpha");
        Store store = store(1L, "Alpha Hotel");
        RoomType roomType = roomType(101L, 1L, "Alpha Room");
        sitesBySlug.put("alpha", site);
        storesById.put(1L, store);
        publicationsBySite.put(
                siteKey(1L, 11L),
                List.of(publication(site, IndependentSitePublicationType.ROOM_TYPE, 101L))
        );
        roomTypesByStoreAndId.put(entityKey(1L, 101L), roomType);
        IndependentSitePage custom = customPage(site, "/about-us", "About Us", true);
        pagesBySite.get(siteKey(1L, 11L)).add(custom);

        IndependentSiteDtos.PublicSiteResponse response = service.getPublicSite("alpha");

        assertEquals("classic", response.themeKey());
        assertEquals(2, response.pages().size());
        assertEquals("/", response.pages().get(0).path());
        assertEquals("HOME", response.pages().get(0).type());
        assertEquals("/about-us", response.pages().get(1).path());
        assertEquals("About Us", response.pages().get(1).title());
    }

    @Test
    void getPublicPage_shouldReturnPublishedCustomPage() {
        IndependentSite site = site(1L, 11L, "alpha");
        sitesBySlug.put("alpha", site);
        IndependentSitePage custom = customPage(site, "/about-us", "About Us", true);
        pagesBySite.get(siteKey(1L, 11L)).add(custom);

        IndependentSiteDtos.PublicPageResponse response = service.getPublicPage("alpha", "/about-us");

        assertEquals("/about-us", response.path());
        assertEquals("About Us", response.title());
        assertEquals("CUSTOM", response.type());
        assertEquals(
                IndependentSitePageSchemaValidator.SCHEMA_VERSION,
                response.schema().path("schemaVersion").asText()
        );
    }

    @Test
    void getPublicSite_shouldExposeClosedFlagWhenEntitlementMissing() {
        IndependentSite site = site(1L, 11L, "alpha");
        sitesBySlug.put("alpha", site);
        storesById.put(1L, store(1L, "Alpha Hotel"));
        publicationsBySite.put(
                siteKey(1L, 11L),
                List.of(publication(site, IndependentSitePublicationType.ROOM_TYPE, 101L))
        );
        roomTypesByStoreAndId.put(entityKey(1L, 101L), roomType(101L, 1L, "Alpha Room"));

        // 门店缺失 independent_website 权益 → closed=true（前端据此展示维护页）
        Mockito.when(entitlementService.storeHasFeature(1L, SaasFeatureCodes.INDEPENDENT_WEBSITE))
                .thenReturn(false);
        assertTrue(service.getPublicSite("alpha").closed());

        // 权益具备 → closed=false，站点正常展示
        Mockito.when(entitlementService.storeHasFeature(1L, SaasFeatureCodes.INDEPENDENT_WEBSITE))
                .thenReturn(true);
        assertFalse(service.getPublicSite("alpha").closed());
    }

    @Test
    void getPublicPage_shouldExposeClosedFlagWhenEntitlementMissing() {
        IndependentSite site = site(1L, 11L, "alpha");
        sitesBySlug.put("alpha", site);
        IndependentSitePage custom = customPage(site, "/about-us", "About Us", true);
        pagesBySite.get(siteKey(1L, 11L)).add(custom);

        Mockito.when(entitlementService.storeHasFeature(1L, SaasFeatureCodes.INDEPENDENT_WEBSITE))
                .thenReturn(false);
        assertTrue(service.getPublicPage("alpha", "/about-us").closed());

        Mockito.when(entitlementService.storeHasFeature(1L, SaasFeatureCodes.INDEPENDENT_WEBSITE))
                .thenReturn(true);
        assertFalse(service.getPublicPage("alpha", "/about-us").closed());
    }

    @Test
    void getPublicPage_shouldRejectUnknownPathAndUnpublishedPage() {
        IndependentSite site = site(1L, 11L, "alpha");
        sitesBySlug.put("alpha", site);

        IndependentSiteServiceException unknown = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.getPublicPage("alpha", "/missing")
        );
        assertEquals("SITE_UNAVAILABLE", unknown.getCode());

        IndependentSitePage unpublished = customPage(site, "/draft-only", "Draft Only", false);
        pagesBySite.get(siteKey(1L, 11L)).add(unpublished);
        IndependentSiteServiceException hidden = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.getPublicPage("alpha", "/draft-only")
        );
        assertEquals("SITE_UNAVAILABLE", hidden.getCode());
    }

    private IndependentSitePage customPage(
            IndependentSite site,
            String path,
            String title,
            boolean published
    ) {
        IndependentSitePage page = new IndependentSitePage();
        page.setId(9100L + path.hashCode());
        page.setStoreId(site.getStoreId());
        page.setSite(site);
        page.setPath(path);
        page.setType(IndependentSitePageType.CUSTOM);
        page.setTitle(title);
        page.setSeoDescription("seo " + title);
        if (published) {
            page.setPublishedSchemaJson(writeDefaultSchema());
            page.setPublishedAt(java.time.LocalDateTime.of(2026, 7, 20, 0, 0));
        }
        page.setEnabled(true);
        page.setSortOrder(1);
        return page;
    }

    @Test
    void quote_shouldRejectClosedToArrivalBeforeReturningAnyAmount() {
        IndependentSite site = site(1L, 11L, "alpha");
        RoomType roomType = roomType(101L, 1L, "Alpha Room");
        RoomTypePricePlan mapping = mapping(site.getChannel().getDefaultPricePlan(), roomType);
        LocalDate checkIn = LocalDate.of(2026, 7, 21);
        RoomPrice arrival = new RoomPrice(
                roomType,
                site.getChannel().getDefaultPricePlan(),
                checkIn,
                new BigDecimal("100.00")
        );
        arrival.setCta(true);

        stubQuoteBase(site, roomType, mapping);
        roomPrices = List.of(arrival);

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.quote("alpha", new IndependentSiteDtos.QuoteRequest(
                        101L,
                        checkIn,
                        checkIn.plusDays(1),
                        1,
                        1,
                        0
                ))
        );

        assertEquals("CLOSED_TO_ARRIVAL", exception.getCode());
    }

    @Test
    void quote_shouldApplyPricePlanMinimumStayWhenDailyOverrideIsAbsent() {
        IndependentSite site = site(1L, 11L, "alpha");
        site.getChannel().getDefaultPricePlan().setMinNights(2);
        RoomType roomType = roomType(101L, 1L, "Alpha Room");
        RoomTypePricePlan mapping = mapping(site.getChannel().getDefaultPricePlan(), roomType);
        LocalDate checkIn = LocalDate.of(2026, 7, 21);

        stubQuoteBase(site, roomType, mapping);
        roomPrices = List.of();

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.quote("alpha", new IndependentSiteDtos.QuoteRequest(
                        101L,
                        checkIn,
                        checkIn.plusDays(1),
                        1,
                        1,
                        0
                ))
        );

        assertEquals("MIN_STAY_NOT_MET", exception.getCode());
    }

    @Test
    void quote_shouldSubtractExistingOccupancyFromDailyAvailableRoomsCap() {
        IndependentSite site = site(1L, 11L, "alpha");
        RoomType roomType = roomType(101L, 1L, "Alpha Room");
        roomType.setTotalRooms(3);
        RoomTypePricePlan mapping = mapping(site.getChannel().getDefaultPricePlan(), roomType);
        LocalDate checkIn = LocalDate.of(2026, 7, 21);
        RoomPrice dailyPrice = new RoomPrice(
                roomType,
                site.getChannel().getDefaultPricePlan(),
                checkIn,
                new BigDecimal("100.00")
        );
        dailyPrice.setAvailableRooms(2);

        Room bookedRoom = room(201L, roomType, "101");
        Room availableRoomA = room(202L, roomType, "102");
        Room availableRoomB = room(203L, roomType, "103");
        Reservation existing = new Reservation();
        existing.setStoreId(1L);
        existing.setRoom(bookedRoom);
        existing.setCheckInDate(checkIn);
        existing.setCheckOutDate(checkIn.plusDays(1));
        existing.setStatus(ReservationStatus.CONFIRMED);

        stubQuoteBase(site, roomType, mapping);
        roomPrices = List.of(dailyPrice);
        rooms = List.of(bookedRoom, availableRoomA, availableRoomB);
        reservations = List.of(existing);
        occupancyRows = List.of(occupancyRow(existing));

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.quote("alpha", new IndependentSiteDtos.QuoteRequest(
                        101L,
                        checkIn,
                        checkIn.plusDays(1),
                        2,
                        2,
                        0
                ))
        );

        assertEquals("NO_AVAILABILITY", exception.getCode());
    }

    @Test
    void quote_shouldCountUnassignedOtaReservationAgainstDailyInventoryCap() {
        IndependentSite site = site(1L, 11L, "alpha");
        RoomType roomType = roomType(101L, 1L, "Alpha Room");
        roomType.setTotalRooms(3);
        RoomTypePricePlan mapping = mapping(site.getChannel().getDefaultPricePlan(), roomType);
        LocalDate checkIn = LocalDate.of(2026, 7, 21);
        RoomPrice dailyPrice = new RoomPrice(
                roomType,
                site.getChannel().getDefaultPricePlan(),
                checkIn,
                new BigDecimal("100.00")
        );
        dailyPrice.setAvailableRooms(2);

        stubQuoteBase(site, roomType, mapping);
        roomPrices = List.of(dailyPrice);
        rooms = List.of(
                room(201L, roomType, "101"),
                room(202L, roomType, "102"),
                room(203L, roomType, "103")
        );
        occupancyRows = List.of(occupancyRow(
                unassignedOtaReservation(roomType.getId(), checkIn, ReservationStatus.CONFIRMED)
        ));

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.quote("alpha", new IndependentSiteDtos.QuoteRequest(
                        roomType.getId(),
                        checkIn,
                        checkIn.plusDays(1),
                        2,
                        2,
                        0
                ))
        );

        assertEquals("NO_AVAILABILITY", exception.getCode());
    }

    @Test
    void holdInventoryRecheck_shouldCountNewUnassignedOtaReservationAgainstDailyCap() {
        IndependentSite site = site(1L, 11L, "alpha");
        RoomType roomType = roomType(101L, 1L, "Alpha Room");
        roomType.setTotalRooms(3);
        RoomTypePricePlan mapping = mapping(site.getChannel().getDefaultPricePlan(), roomType);
        LocalDate checkIn = LocalDate.of(2026, 7, 21);
        RoomPrice dailyPrice = new RoomPrice(
                roomType,
                site.getChannel().getDefaultPricePlan(),
                checkIn,
                new BigDecimal("100.00")
        );
        dailyPrice.setAvailableRooms(2);
        IndependentSiteDtos.QuoteRequest request = new IndependentSiteDtos.QuoteRequest(
                roomType.getId(),
                checkIn,
                checkIn.plusDays(1),
                2,
                2,
                0
        );

        stubQuoteBase(site, roomType, mapping);
        roomPrices = List.of(dailyPrice);
        rooms = List.of(
                room(201L, roomType, "101"),
                room(202L, roomType, "102"),
                room(203L, roomType, "103")
        );
        IndependentSiteQuoteService.QuoteComputation quote = service.calculate(site, request);

        occupancyRows = List.of(occupancyRow(
                unassignedOtaReservation(roomType.getId(), checkIn, ReservationStatus.REQUESTED)
        ));
        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> service.assertHoldInventoryAvailable(quote, request)
        );

        assertEquals("NO_AVAILABILITY", exception.getCode());
    }

    private void stubQuoteBase(
            IndependentSite site,
            RoomType roomType,
            RoomTypePricePlan mapping
    ) {
        sitesBySlug.put(site.getSlug(), site);
        storesById.put(site.getStoreId(), store(site.getStoreId(), "Hotel"));
        publicationsBySite.put(
                siteKey(site.getStoreId(), site.getId()),
                List.of(publication(site, IndependentSitePublicationType.ROOM_TYPE, roomType.getId()))
        );
        roomTypesByStoreAndId.put(entityKey(site.getStoreId(), roomType.getId()), roomType);
        mappingsByStoreRoomTypeAndPlan.put(
                mappingKey(
                        site.getStoreId(),
                        roomType.getId(),
                        site.getChannel().getDefaultPricePlan().getId()
                ),
                mapping
        );
    }

    private Object handleSiteRepository(Object proxy, Method method, Object[] args) {
        if ("findEnabledBySlugWithChannel".equals(method.getName())) {
            return Optional.ofNullable(sitesBySlug.get(args[0]));
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handlePublicationRepository(Object proxy, Method method, Object[] args) {
        if ("findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc"
                .equals(method.getName())) {
            return publicationsBySite.getOrDefault(siteKey((Long) args[0], (Long) args[1]), List.of());
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handlePageRepository(Object proxy, Method method, Object[] args) {
        if ("findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue".equals(method.getName())) {
            return pagesBySite.values().stream()
                    .flatMap(List::stream)
                    .filter(page -> page.getSite() != null
                            && Objects.equals(page.getSite().getId(), args[0]))
                    .filter(page -> page.getType() == args[1])
                    .filter(page -> page.getPublishedAt() != null)
                    .filter(page -> Boolean.TRUE.equals(page.getEnabled()))
                    .findFirst();
        }
        if ("findBySiteIdAndPublishedAtIsNotNullAndEnabledTrueOrderBySortOrderAscIdAsc"
                .equals(method.getName())) {
            return pagesBySite.values().stream()
                    .flatMap(List::stream)
                    .filter(page -> page.getSite() != null
                            && Objects.equals(page.getSite().getId(), args[0]))
                    .filter(page -> page.getPublishedAt() != null)
                    .filter(page -> Boolean.TRUE.equals(page.getEnabled()))
                    .toList();
        }
        if ("findByStoreIdAndSiteIdAndPath".equals(method.getName())) {
            return Optional.ofNullable(pagesBySite.get(siteKey((Long) args[0], (Long) args[1])))
                    .stream()
                    .flatMap(List::stream)
                    .filter(page -> Objects.equals(page.getPath(), args[2]))
                    .findFirst();
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handleStoreRepository(Object proxy, Method method, Object[] args) {
        if ("findById".equals(method.getName())) {
            return Optional.ofNullable(storesById.get(args[0]));
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handleRoomTypeRepository(Object proxy, Method method, Object[] args) {
        if ("findByStoreIdAndId".equals(method.getName())) {
            return Optional.ofNullable(roomTypesByStoreAndId.get(
                    entityKey((Long) args[0], (Long) args[1])
            ));
        }
        if ("findByStoreIdAndIdIn".equals(method.getName())) {
            Long storeId = (Long) args[0];
            roomTypeListLookupStoreIds.add(storeId);
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) args[1];
            return ids.stream()
                    .map(id -> roomTypesByStoreAndId.get(entityKey(storeId, id)))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handleMappingRepository(Object proxy, Method method, Object[] args) {
        if ("findByStoreIdAndRoomTypeIdAndPricePlanId".equals(method.getName())) {
            return Optional.ofNullable(mappingsByStoreRoomTypeAndPlan.get(
                    mappingKey((Long) args[0], (Long) args[1], (Long) args[2])
            ));
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handleRoomRepository(Object proxy, Method method, Object[] args) {
        if ("findByStoreIdAndRoomTypeIdAndStatus".equals(method.getName())) {
            return rooms.stream()
                    .filter(room -> Objects.equals(room.getStoreId(), args[0]))
                    .filter(room -> room.getRoomType() != null
                            && Objects.equals(room.getRoomType().getId(), args[1]))
                    .filter(room -> room.getStatus() == args[2])
                    .toList();
        }
        if ("findByStoreIdAndIdIn".equals(method.getName())) {
            @SuppressWarnings("unchecked")
            java.util.Collection<Long> ids = (java.util.Collection<Long>) args[1];
            return rooms.stream()
                    .filter(room -> Objects.equals(room.getStoreId(), args[0]))
                    .filter(room -> ids.contains(room.getId()))
                    .toList();
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handleRoomPriceRepository(Object proxy, Method method, Object[] args) {
        if (Set.of(
                "findByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateBetween",
                "findByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateBetweenForUpdate"
        ).contains(method.getName())) {
            return roomPrices;
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handleReservationRepository(Object proxy, Method method, Object[] args) {
        if ("findByStoreIdAndRoomIdInAndDateRangeAndStatuses".equals(method.getName())) {
            return reservations;
        }
        if ("findOccupancyRowsByStoreIdAndDateRangeAndStatuses".equals(method.getName())) {
            return occupancyRows;
        }
        return objectMethodOrFail(proxy, method, args);
    }

    private Object handleBlockoutRepository(Object proxy, Method method, Object[] args) {
        if ("findByStoreIdAndRoom_IdInAndBlockDateBetween".equals(method.getName())
                || "findByStoreIdAndBlockDateBetween".equals(method.getName())) {
            return List.of();
        }
        return objectMethodOrFail(proxy, method, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object unexpected(Object proxy, Method method, Object[] args) {
        return objectMethodOrFail(proxy, method, args);
    }

    private static Object objectMethodOrFail(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError("Unexpected repository method: " + method);
        };
    }

    private static String siteKey(Long storeId, Long siteId) {
        return storeId + ":" + siteId;
    }

    private static String entityKey(Long storeId, Long entityId) {
        return storeId + ":" + entityId;
    }

    private static String mappingKey(Long storeId, Long roomTypeId, Long pricePlanId) {
        return storeId + ":" + roomTypeId + ":" + pricePlanId;
    }

    private IndependentSite site(Long storeId, Long siteId, String slug) {
        PricePlan plan = new PricePlan();
        plan.setId(501L + storeId - 1L);
        plan.setStoreId(storeId);
        plan.setName("Standard");
        plan.setMinNights(1);
        plan.setMaxNights(365);

        Channel channel = new Channel();
        channel.setId(301L + storeId);
        channel.setStoreId(storeId);
        channel.setCode(IndependentSiteManagementService.BOOKING_ENGINE_CHANNEL_CODE);
        channel.setEnabled(true);
        channel.setIsActive(true);
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(new BigDecimal("10.00"));
        channel.setDefaultPricePlan(plan);

        IndependentSite site = new IndependentSite();
        site.setId(siteId);
        site.setStoreId(storeId);
        site.setSlug(slug);
        site.setName(slug + " hotel");
        site.setEnabled(true);
        site.setChannel(channel);
        site.setPaymentProvider(IndependentSitePaymentProvider.SIMULATED);
        site.setSimulatedPaymentEnabled(true);
        site.setPublishedAt(java.time.LocalDateTime.of(2026, 7, 20, 0, 0));
        pagesBySite.put(siteKey(storeId, siteId), new ArrayList<>(List.of(homePage(site))));
        return site;
    }

    private IndependentSitePage homePage(IndependentSite site) {
        IndependentSitePage home = new IndependentSitePage();
        home.setId(9000L + site.getId());
        home.setStoreId(site.getStoreId());
        home.setSite(site);
        home.setPath("/");
        home.setType(IndependentSitePageType.HOME);
        home.setTitle(site.getName());
        home.setPublishedSchemaJson(writeDefaultSchema());
        home.setPublishedAt(java.time.LocalDateTime.of(2026, 7, 20, 0, 0));
        home.setEnabled(true);
        home.setSortOrder(0);
        return home;
    }

    private String writeDefaultSchema() {
        try {
            return objectMapper.writeValueAsString(schemaValidator.defaultSchema());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Store store(Long id, String name) {
        Store store = new Store();
        store.setId(id);
        store.setUserId(900L + id);
        store.setName(name);
        store.setTimezone("UTC");
        store.setCurrency("CNY");
        return store;
    }

    private static RoomType roomType(Long id, Long storeId, String name) {
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setStoreId(storeId);
        roomType.setName(name);
        roomType.setCode("RT-" + id);
        roomType.setTotalRooms(2);
        roomType.setMaxGuests(2);
        roomType.setMaxChildOccupancy(1);
        roomType.setDefaultPrice(new BigDecimal("100.00"));
        return roomType;
    }

    private static RoomTypePricePlan mapping(PricePlan plan, RoomType roomType) {
        RoomTypePricePlan mapping = new RoomTypePricePlan();
        mapping.setStoreId(roomType.getStoreId());
        mapping.setRoomType(roomType);
        mapping.setPricePlan(plan);
        mapping.setMaxGuests(2);
        mapping.setIncludedGuests(2);
        mapping.setMondayPrice(new BigDecimal("100.00"));
        mapping.setTuesdayPrice(new BigDecimal("100.00"));
        mapping.setWednesdayPrice(new BigDecimal("100.00"));
        mapping.setThursdayPrice(new BigDecimal("100.00"));
        mapping.setFridayPrice(new BigDecimal("100.00"));
        mapping.setSaturdayPrice(new BigDecimal("100.00"));
        mapping.setSundayPrice(new BigDecimal("100.00"));
        return mapping;
    }

    private static Room room(Long id, RoomType roomType, String number) {
        Room room = new Room();
        room.setId(id);
        room.setStoreId(roomType.getStoreId());
        room.setRoomType(roomType);
        room.setRoomNumber(number);
        room.setStatus(RoomStatus.AVAILABLE);
        return room;
    }

    private static ReservationRepository.ReservationOccupancyRow occupancyRow(
            Reservation reservation
    ) {
        return new ReservationRepository.ReservationOccupancyRow() {
            @Override
            public LocalDate getCheckInDate() {
                return reservation.getCheckInDate();
            }

            @Override
            public LocalDate getCheckOutDate() {
                return reservation.getCheckOutDate();
            }

            @Override
            public ReservationStatus getStatus() {
                return reservation.getStatus();
            }

            @Override
            public java.time.LocalDateTime getActualCheckOut() {
                return reservation.getActualCheckOut();
            }

            @Override
            public Long getOtaRoomTypeId() {
                return reservation.getOtaRoomTypeId();
            }

            @Override
            public Long getAssignedRoomTypeId() {
                return reservation.getRoom() != null && reservation.getRoom().getRoomType() != null
                        ? reservation.getRoom().getRoomType().getId()
                        : null;
            }
        };
    }

    private static Reservation unassignedOtaReservation(
            Long otaRoomTypeId,
            LocalDate checkIn,
            ReservationStatus status
    ) {
        Reservation reservation = new Reservation();
        reservation.setStoreId(1L);
        reservation.setOtaRoomTypeId(otaRoomTypeId);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkIn.plusDays(1));
        reservation.setStatus(status);
        return reservation;
    }

    private static IndependentSitePublication publication(
            IndependentSite site,
            IndependentSitePublicationType type,
            Long targetId
    ) {
        IndependentSitePublication publication = new IndependentSitePublication();
        publication.setSite(site);
        publication.setTargetType(type);
        publication.setTargetId(targetId);
        publication.setEnabled(true);
        return publication;
    }
}
