package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.FacilityDTO;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.Channel;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.entity.IndependentSitePublication;
import server.demo.entity.PricePlan;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.IndependentSitePageFormat;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePaymentProvider;
import server.demo.enums.IndependentSitePublicationType;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.ChannelRepository;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSitePublicationRepository;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.PaymentAttemptRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import server.demo.i18n.ApiMessages;
@Service
public class IndependentSiteManagementService {

    public static final String BOOKING_ENGINE_CHANNEL_CODE = "BOOKING_ENGINE";
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "api",
            "admin",
            "auth",
            "login",
            "logout",
            "public",
            "settings",
            "stay"
    );
    private static final Set<String> THEME_KEYS = Set.of("classic", "modern", "elegant");
    private static final String HOME_PAGE_PATH = "/";

    private final IndependentSiteRepository siteRepository;
    private final IndependentSitePublicationRepository publicationRepository;
    private final IndependentSitePageRepository pageRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ChannelRepository channelRepository;
    private final PricePlanRepository pricePlanRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomTypePricePlanRepository roomTypePricePlanRepository;
    private final ChannelBootstrapService channelBootstrapService;
    private final IndependentSitePageSchemaValidator pageSchemaValidator;
    private final ObjectMapper objectMapper;
    private final IndependentSiteManagementRateLimiter managementRateLimiter;
    private final IndependentSiteUrlFetchService urlFetchService;
    private final IndependentSitePageSchemaGenerationService pageSchemaGenerationService;
    private final IndependentSiteStripeSettingsService stripeSettingsService;
    private final IndependentSiteCanvasValidator canvasValidator;

    public IndependentSiteManagementService(
            IndependentSiteRepository siteRepository,
            IndependentSitePublicationRepository publicationRepository,
            IndependentSitePageRepository pageRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            ChannelRepository channelRepository,
            PricePlanRepository pricePlanRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            ChannelBootstrapService channelBootstrapService,
            IndependentSitePageSchemaValidator pageSchemaValidator,
            ObjectMapper objectMapper,
            IndependentSiteManagementRateLimiter managementRateLimiter,
            IndependentSiteUrlFetchService urlFetchService,
            IndependentSitePageSchemaGenerationService pageSchemaGenerationService,
            IndependentSiteStripeSettingsService stripeSettingsService,
            IndependentSiteCanvasValidator canvasValidator
    ) {
        this.siteRepository = siteRepository;
        this.publicationRepository = publicationRepository;
        this.pageRepository = pageRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.channelRepository = channelRepository;
        this.pricePlanRepository = pricePlanRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.roomTypePricePlanRepository = roomTypePricePlanRepository;
        this.channelBootstrapService = channelBootstrapService;
        this.pageSchemaValidator = pageSchemaValidator;
        this.objectMapper = objectMapper;
        this.managementRateLimiter = managementRateLimiter;
        this.urlFetchService = urlFetchService;
        this.pageSchemaGenerationService = pageSchemaGenerationService;
        this.stripeSettingsService = stripeSettingsService;
        this.canvasValidator = canvasValidator;
    }

    // ------------------------------------------------------------------
    // 站点 CRUD（一店多站）
    // ------------------------------------------------------------------

    @Transactional
    public List<IndependentSiteDtos.SiteSummaryResponse> listSites(Long storeId) {
        requireStoreId(storeId);
        channelBootstrapService.ensureDefaultChannelsForStore(storeId);
        List<IndependentSite> sites = siteRepository.findByStoreIdOrderByCreatedAtAscIdAsc(storeId);
        List<IndependentSiteDtos.SiteSummaryResponse> responses = new ArrayList<>();
        for (int index = 0; index < sites.size(); index++) {
            IndependentSite site = sites.get(index);
            responses.add(new IndependentSiteDtos.SiteSummaryResponse(
                    site.getId(),
                    site.getName(),
                    site.getSlug(),
                    Boolean.TRUE.equals(site.getEnabled()),
                    site.getThemeKey(),
                    site.getPaymentProvider() != null ? site.getPaymentProvider().name() : null,
                    "/stay/" + site.getSlug(),
                    pageRepository.countByStoreIdAndSiteId(storeId, site.getId()),
                    publicationRepository.countByStoreIdAndSiteId(storeId, site.getId()),
                    index == 0,
                    toOffset(site.getPublishedAt()),
                    toOffset(site.getUpdatedAt())
            ));
        }
        return responses;
    }

    @Transactional
    public IndependentSiteDtos.SiteDetailResponse createSite(
            Long storeId,
            IndependentSiteDtos.SiteCreateRequest request
    ) {
        requireStoreId(storeId);
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.331f0757ba60"));
        }
        channelBootstrapService.ensureDefaultChannelsForStore(storeId);
        String name = normalizeSiteName(request.name());
        String slug = normalizeSlug(request.slug());
        if (siteRepository.existsBySlug(slug)) {
            throw conflict("SLUG_ALREADY_EXISTS", ApiMessages.get("api.t.3f9cd96f6b33"));
        }
        Channel channel = channelRepository.findByStoreIdAndCode(storeId, BOOKING_ENGINE_CHANNEL_CODE)
                .orElseThrow(() -> new IllegalStateException(ApiMessages.get("api.t.4f605cc2c43c")));

        IndependentSite site = new IndependentSite();
        site.setStoreId(storeId);
        site.setName(name);
        site.setSlug(slug);
        site.setThemeKey(normalizeThemeKey(request.themeKey()));
        site.setEnabled(false);
        site.setChannel(channel);
        site.setPaymentProvider(IndependentSitePaymentProvider.SIMULATED);
        site.setSimulatedPaymentEnabled(false);
        IndependentSite saved = siteRepository.save(site);
        createHomePage(saved, name, IndependentSitePageFormat.CANVAS);
        return toDetailResponse(saved);
    }

    @Transactional
    public IndependentSiteDtos.SiteDetailResponse getSite(Long storeId, Long id) {
        requireStoreId(storeId);
        channelBootstrapService.ensureDefaultChannelsForStore(storeId);
        IndependentSite site = siteRepository.findByStoreIdAndIdWithChannel(storeId, id)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.87f7c7d02b26")));
        return toDetailResponse(site);
    }

    @Transactional
    public IndependentSiteDtos.SiteDetailResponse updateSite(
            Long storeId,
            Long id,
            IndependentSiteDtos.SiteUpdateRequest request
    ) {
        requireStoreId(storeId);
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.331f0757ba60"));
        }
        channelBootstrapService.ensureDefaultChannelsForStore(storeId);
        IndependentSite site = siteRepository.findByStoreIdAndIdWithChannelForUpdate(storeId, id)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.87f7c7d02b26")));
        applySiteConfig(
                storeId,
                site,
                request.slug(),
                request.name(),
                request.themeKey(),
                request.paymentProvider(),
                request.enabled(),
                request.defaultPricePlanId(),
                request.priceAdjustmentValue(),
                request.simulatedPaymentEnabled(),
                request.publishedRoomTypeIds(),
                request.publishedRoomIds()
        );
        return toDetailResponse(site);
    }

    @Transactional
    public void deleteSite(Long storeId, Long id) {
        requireStoreId(storeId);
        IndependentSite site = siteRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.87f7c7d02b26")));
        if (paymentAttemptRepository.existsBySite_Id(site.getId())) {
            throw conflict("SITE_HAS_PAYMENTS", ApiMessages.get("api.t.2ca1d2717095"));
        }
        // publications/pages 由数据库外键 ON DELETE CASCADE 级联删除
        siteRepository.delete(site);
    }

    // ------------------------------------------------------------------
    // 旧默认站（该店最早创建的站点）兼容委托
    // ------------------------------------------------------------------

    @Transactional
    public IndependentSiteDtos.ConfigResponse getCurrent(Long storeId) {
        requireStoreId(storeId);
        channelBootstrapService.ensureDefaultChannelsForStore(storeId);
        IndependentSite site = defaultSite(storeId);
        return site == null ? null : toConfigResponse(site);
    }

    @Transactional
    public IndependentSiteDtos.ConfigResponse updateCurrent(
            Long storeId,
            IndependentSiteDtos.ConfigUpdateRequest request
    ) {
        requireStoreId(storeId);
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.ca3ca06e42fa"));
        }
        channelBootstrapService.ensureDefaultChannelsForStore(storeId);

        IndependentSite site = defaultSiteForUpdate(storeId);
        boolean isNew = site == null;
        if (isNew) {
            site = new IndependentSite();
        }
        applySiteConfig(
                storeId,
                site,
                request.slug(),
                null,
                null,
                IndependentSitePaymentProvider.SIMULATED.name(),
                request.enabled(),
                request.defaultPricePlanId(),
                request.priceAdjustmentValue(),
                request.simulatedPaymentEnabled(),
                request.publishedRoomTypeIds(),
                request.publishedRoomIds()
        );
        if (isNew) {
            // 旧单站端点服务旧编辑器：自动创建的站点保持 BLOCKS，新端点 createSite 才是 CANVAS
            createHomePage(site, site.getName(), IndependentSitePageFormat.BLOCKS);
        }
        return toConfigResponse(site);
    }

    // ------------------------------------------------------------------
    // 页面 CRUD（一站多页面）
    // ------------------------------------------------------------------

    @Transactional
    public List<IndependentSiteDtos.PageSummaryResponse> listPages(Long storeId, Long siteId) {
        requireStoreId(storeId);
        requireSite(storeId, siteId);
        return pageRepository.findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc(storeId, siteId)
                .stream()
                .map(this::toPageSummary)
                .toList();
    }

    @Transactional
    public IndependentSiteDtos.PageDetailResponse createPage(
            Long storeId,
            Long siteId,
            IndependentSiteDtos.PageCreateRequest request
    ) {
        requireStoreId(storeId);
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.fccd94b94156"));
        }
        IndependentSite site = requireSite(storeId, siteId);
        IndependentSitePageType type = parseCreatablePageType(request.type());
        String path = normalizePagePath(request.path());
        if (pageRepository.findByStoreIdAndSiteIdAndPath(storeId, siteId, path).isPresent()) {
            throw conflict("PAGE_PATH_ALREADY_EXISTS", ApiMessages.get("api.t.996bd7175c1b"));
        }

        IndependentSitePage page = new IndependentSitePage();
        page.setStoreId(storeId);
        page.setSite(site);
        page.setPath(path);
        page.setType(type);
        page.setFormat(IndependentSitePageFormat.CANVAS);
        page.setTitle(normalizePageTitle(request.title()));
        page.setSeoDescription(normalizeSeoDescription(request.seoDescription()));
        page.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        page.setEnabled(true);
        page.setDraftSchemaJson(writeJson(canvasValidator.defaultCanvasSchema(page.getTitle())));
        page.setDraftVersion(1L);
        page.setDraftUpdatedAt(nowUtc());
        return toPageDetail(pageRepository.save(page));
    }

    @Transactional
    public IndependentSiteDtos.PageDetailResponse getPage(Long storeId, Long siteId, Long pageId) {
        requireStoreId(storeId);
        requireSite(storeId, siteId);
        IndependentSitePage page = pageRepository.findByStoreIdAndSiteIdAndId(storeId, siteId, pageId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.55c9e10608ff")));
        return toPageDetail(page);
    }

    @Transactional
    public IndependentSiteDtos.PageDetailResponse updatePage(
            Long storeId,
            Long siteId,
            Long pageId,
            IndependentSiteDtos.PageUpdateRequest request
    ) {
        requireStoreId(storeId);
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.fccd94b94156"));
        }
        requireSite(storeId, siteId);
        IndependentSitePage page = pageRepository
                .findByStoreIdAndSiteIdAndIdForUpdate(storeId, siteId, pageId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.55c9e10608ff")));

        if (request.title() != null) {
            page.setTitle(normalizePageTitle(request.title()));
        }
        if (request.seoDescription() != null) {
            page.setSeoDescription(normalizeSeoDescription(request.seoDescription()));
        }
        if (request.path() != null) {
            if (page.getType() == IndependentSitePageType.HOME) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.6ea72805103d"));
            }
            String path = normalizePagePath(request.path());
            Optional<IndependentSitePage> existing =
                    pageRepository.findByStoreIdAndSiteIdAndPath(storeId, siteId, path);
            if (existing.isPresent() && !Objects.equals(existing.get().getId(), page.getId())) {
                throw conflict("PAGE_PATH_ALREADY_EXISTS", ApiMessages.get("api.t.996bd7175c1b"));
            }
            page.setPath(path);
        }
        if (request.enabled() != null) {
            page.setEnabled(request.enabled());
        }
        if (request.sortOrder() != null) {
            page.setSortOrder(request.sortOrder());
        }
        if (request.draftSchema() != null) {
            savePageDraftInternal(page, request.draftSchema(), request.expectedDraftVersion());
        }
        return toPageDetail(pageRepository.save(page));
    }

    @Transactional
    public void deletePage(Long storeId, Long siteId, Long pageId) {
        requireStoreId(storeId);
        requireSite(storeId, siteId);
        IndependentSitePage page = pageRepository.findByStoreIdAndSiteIdAndId(storeId, siteId, pageId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.55c9e10608ff")));
        if (page.getType() == IndependentSitePageType.HOME) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.deeb24a7b4ce"));
        }
        pageRepository.delete(page);
    }

    @Transactional
    public IndependentSiteDtos.PageDetailResponse undoAiEdit(Long storeId, Long siteId, Long pageId) {
        requireStoreId(storeId);
        requireSite(storeId, siteId);
        IndependentSitePage page = pageRepository
                .findByStoreIdAndSiteIdAndIdForUpdate(storeId, siteId, pageId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.55c9e10608ff")));
        String backup = page.getDraftBackupSchemaJson();
        if (backup == null || backup.isBlank()) {
            throw new IndependentSiteServiceException(
                    HttpStatus.NOT_FOUND,
                    "AI_EDIT_BACKUP_NOT_FOUND",
                    ApiMessages.get("api.t.926ef26f3218")
            );
        }
        page.setDraftSchemaJson(backup);
        page.setDraftBackupSchemaJson(null);
        page.setDraftVersion(draftVersion(page) + 1);
        page.setDraftUpdatedAt(nowUtc());
        return toPageDetail(pageRepository.save(page));
    }

    @Transactional
    public IndependentSiteDtos.PageDetailResponse publishPage(
            Long storeId,
            Long siteId,
            Long pageId,
            IndependentSiteDtos.PublishPageDraftRequest request
    ) {
        requireStoreId(storeId);
        if (request == null || request.draftVersion() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.bc5bae79f364"));
        }
        IndependentSite site = requireSite(storeId, siteId);
        IndependentSitePage page = pageRepository
                .findByStoreIdAndSiteIdAndIdForUpdate(storeId, siteId, pageId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.55c9e10608ff")));
        publishPageInternal(site, page, request.draftVersion());
        return toPageDetail(page);
    }

    // ------------------------------------------------------------------
    // 房型详情页自动生成
    // ------------------------------------------------------------------

    /**
     * 按发布范围生成/刷新 ROOM_DETAIL 页草稿。已发布内容与页面启用状态不受影响，
     * 数据未通过白名单校验的房型记入 skipped 而不是让整个操作失败。
     */
    @Transactional
    public IndependentSiteDtos.GenerateRoomPagesResponse generateRoomTypePages(Long storeId, Long siteId) {
        requireStoreId(storeId);
        IndependentSite site = requireSite(storeId, siteId);
        // 房型页格式跟随站点首页：CANVAS 站生成节点树骨架，BLOCKS 站维持旧区块骨架
        IndependentSitePage home = homePage(storeId, siteId);
        IndependentSitePageFormat batchFormat = home != null
                ? home.getFormat()
                : IndependentSitePageFormat.BLOCKS;

        Set<Long> targetRoomTypeIds = publicationRoomTypeIds(storeId, siteId);
        Map<Long, RoomType> roomTypesById = new LinkedHashMap<>();
        if (!targetRoomTypeIds.isEmpty()) {
            for (RoomType roomType : roomTypeRepository.findByStoreIdAndIdIn(
                    storeId,
                    new ArrayList<>(targetRoomTypeIds)
            )) {
                roomTypesById.put(roomType.getId(), roomType);
            }
        }

        List<IndependentSitePage> existingPages =
                pageRepository.findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc(storeId, siteId);
        Map<Long, IndependentSitePage> roomPagesByRoomTypeId = new LinkedHashMap<>();
        Set<String> usedPaths = new HashSet<>();
        for (IndependentSitePage page : existingPages) {
            if (page.getPath() != null) {
                usedPaths.add(page.getPath());
            }
            if (page.getType() == IndependentSitePageType.ROOM_DETAIL && page.getRoomTypeId() != null) {
                roomPagesByRoomTypeId.putIfAbsent(page.getRoomTypeId(), page);
            }
        }

        int generated = 0;
        int refreshed = 0;
        List<IndependentSiteDtos.SkippedRoomPage> skipped = new ArrayList<>();
        int nextSortOrder = 10;
        for (Long roomTypeId : targetRoomTypeIds) {
            RoomType roomType = roomTypesById.get(roomTypeId);
            if (roomType == null) {
                skipped.add(new IndependentSiteDtos.SkippedRoomPage(
                        roomTypeId,
                        ApiMessages.get("api.t.318168534f9f")
                ));
                continue;
            }
            JsonNode schema;
            try {
                schema = batchFormat == IndependentSitePageFormat.CANVAS
                        ? buildCanvasRoomDetailSchema(roomType)
                        : buildRoomDetailSchema(roomType);
            } catch (IllegalArgumentException e) {
                skipped.add(new IndependentSiteDtos.SkippedRoomPage(
                        roomTypeId,
                        e.getMessage() == null ? ApiMessages.get("api.t.1c8aee147f92") : e.getMessage()
                ));
                continue;
            }

            IndependentSitePage page = roomPagesByRoomTypeId.get(roomTypeId);
            if (page == null) {
                String path = resolveRoomPagePath(roomType, usedPaths);
                usedPaths.add(path);
                IndependentSitePage created = new IndependentSitePage();
                created.setStoreId(storeId);
                created.setSite(site);
                created.setPath(path);
                created.setType(IndependentSitePageType.ROOM_DETAIL);
                created.setFormat(batchFormat);
                created.setTitle(roomType.getName().trim());
                created.setRoomTypeId(roomTypeId);
                created.setEnabled(true);
                created.setSortOrder(nextSortOrder++);
                created.setDraftSchemaJson(writeJson(schema));
                created.setDraftVersion(1L);
                created.setDraftUpdatedAt(nowUtc());
                pageRepository.save(created);
                generated++;
            } else {
                page.setFormat(batchFormat);
                page.setDraftSchemaJson(writeJson(schema));
                page.setDraftVersion(draftVersion(page) + 1);
                page.setDraftUpdatedAt(nowUtc());
                if (!Boolean.TRUE.equals(page.getEnabled())) {
                    page.setEnabled(true);
                }
                pageRepository.save(page);
                refreshed++;
            }
        }

        List<IndependentSiteDtos.PageSummaryResponse> pages =
                pageRepository.findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc(storeId, siteId)
                        .stream()
                        .map(this::toPageSummary)
                        .toList();
        return new IndependentSiteDtos.GenerateRoomPagesResponse(generated, refreshed, skipped, pages);
    }

    // ------------------------------------------------------------------
    // URL 导入生成页面草稿（抠页面）
    // ------------------------------------------------------------------

    /**
     * 从外部 URL 抓取内容并生成合法页面草稿。NEW_PAGE 创建 CUSTOM 页（草稿=生成结果、
     * enabled=true、published 为空）；OVERWRITE_DRAFT 把旧草稿原文写入备份列后覆盖草稿，
     * 已发布内容不动。抓取或 AI 失败时不建页、不改草稿与备份。
     */
    @Transactional
    public IndependentSiteDtos.PageDetailResponse importPageFromUrl(
            Long storeId,
            Long siteId,
            IndependentSiteDtos.ImportPageFromUrlRequest request
    ) {
        requireStoreId(storeId);
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.4c383bd7b301"));
        }
        IndependentSite site = requireSite(storeId, siteId);
        String mode = normalizeImportMode(request.mode());

        String path = null;
        String title = null;
        IndependentSitePage page = null;
        if ("NEW_PAGE".equals(mode)) {
            path = normalizePagePath(request.path());
            title = normalizePageTitle(request.title());
            if (pageRepository.findByStoreIdAndSiteIdAndPath(storeId, siteId, path).isPresent()) {
                throw conflict("PAGE_PATH_ALREADY_EXISTS", ApiMessages.get("api.t.996bd7175c1b"));
            }
        } else {
            if (request.pageId() == null) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.ba106a6ad900"));
            }
            page = pageRepository
                    .findByStoreIdAndSiteIdAndIdForUpdate(storeId, siteId, request.pageId())
                    .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.55c9e10608ff")));
            if (page.getFormat() != IndependentSitePageFormat.BLOCKS) {
                // URL 导入本轮保持 BLOCKS 管线；CANVAS 页面的导入迁移在后续轮次进行
                throw new IllegalArgumentException(ApiMessages.get("api.t.ddb30f84e8d1"));
            }
        }

        managementRateLimiter.checkUrlImport(storeId);

        IndependentSiteUrlFetchService.ExtractedContent content = urlFetchService.fetch(request.url());
        JsonNode schema = pageSchemaGenerationService.generateSchemaFromUrlImport(
                storeId,
                siteId,
                content,
                page == null ? title : page.getTitle(),
                page == null ? path : page.getPath()
        );

        if (page == null) {
            page = new IndependentSitePage();
            page.setStoreId(storeId);
            page.setSite(site);
            page.setPath(path);
            page.setType(IndependentSitePageType.CUSTOM);
            page.setFormat(IndependentSitePageFormat.BLOCKS);
            page.setTitle(title);
            page.setEnabled(true);
            page.setSortOrder(0);
            page.setDraftSchemaJson(writeJson(schema));
            page.setDraftVersion(1L);
            page.setDraftUpdatedAt(nowUtc());
        } else {
            page.setDraftBackupSchemaJson(currentDraftOrFallback(page));
            page.setDraftSchemaJson(writeJson(schema));
            page.setDraftVersion(draftVersion(page) + 1);
            page.setDraftUpdatedAt(nowUtc());
        }
        return toPageDetail(pageRepository.save(page));
    }

    /** 旧草稿原文；草稿为空时回退到已发布版本，再退到默认 schema（与 AI 撤销机制一致）。 */
    private String currentDraftOrFallback(IndependentSitePage page) {
        if (page.getDraftSchemaJson() != null && !page.getDraftSchemaJson().isBlank()) {
            return page.getDraftSchemaJson();
        }
        if (page.getPublishedSchemaJson() != null && !page.getPublishedSchemaJson().isBlank()) {
            return page.getPublishedSchemaJson();
        }
        return writeJson(pageSchemaValidator.defaultSchema());
    }

    private static String normalizeImportMode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.389400c1df4b"));
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("NEW_PAGE", "OVERWRITE_DRAFT").contains(normalized)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.450f5cef4d78"));
        }
        return normalized;
    }

    // ------------------------------------------------------------------
    // 旧 HOME 页草稿端点兼容委托
    // ------------------------------------------------------------------

    @Transactional
    public IndependentSiteDtos.PageDraftStateResponse savePageDraft(
            Long storeId,
            IndependentSiteDtos.PageDraftSaveRequest request
    ) {
        requireStoreId(storeId);
        if (request == null || request.pageSchema() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.cfcd24adf980"));
        }
        IndependentSite site = defaultSite(storeId);
        if (site == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.4ea3a531c064"));
        }
        IndependentSitePage home = homePageForUpdate(storeId, site.getId());
        JsonNode schema = savePageDraftInternal(home, request.pageSchema(), request.expectedDraftVersion());
        pageRepository.save(home);
        return new IndependentSiteDtos.PageDraftStateResponse(
                site.getId(),
                IndependentSitePageSchemaValidator.SCHEMA_VERSION,
                schema,
                toOffset(home.getDraftUpdatedAt()),
                draftVersion(home)
        );
    }

    @Transactional
    public IndependentSiteDtos.ConfigResponse publishPageDraft(
            Long storeId,
            IndependentSiteDtos.PublishPageDraftRequest request
    ) {
        requireStoreId(storeId);
        if (request == null || request.draftVersion() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.bc5bae79f364"));
        }
        IndependentSite site = defaultSite(storeId);
        if (site == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.87f7c7d02b26"));
        }
        IndependentSitePage home = homePageForUpdate(storeId, site.getId());
        publishPageInternal(site, home, request.draftVersion());
        return toConfigResponse(site);
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private void applySiteConfig(
            Long storeId,
            IndependentSite site,
            String rawSlug,
            String rawName,
            String rawThemeKey,
            String rawPaymentProvider,
            boolean enabled,
            Long defaultPricePlanId,
            BigDecimal priceAdjustmentValue,
            boolean simulatedPaymentEnabled,
            Set<Long> publishedRoomTypeIds,
            Set<Long> publishedRoomIds
    ) {
        Channel channel = channelRepository.findByStoreIdAndCode(storeId, BOOKING_ENGINE_CHANNEL_CODE)
                .orElseThrow(() -> new IllegalStateException(ApiMessages.get("api.t.4f605cc2c43c")));
        PricePlan pricePlan = pricePlanRepository.findByStoreIdAndId(storeId, defaultPricePlanId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.682f5b830e57")));

        String slug = normalizeSlug(rawSlug);
        if (site.getId() == null) {
            if (siteRepository.existsBySlug(slug)) {
                throw conflict("SLUG_ALREADY_EXISTS", ApiMessages.get("api.t.3f9cd96f6b33"));
            }
        } else if (siteRepository.existsBySlugAndIdNot(slug, site.getId())) {
            throw conflict("SLUG_ALREADY_EXISTS", ApiMessages.get("api.t.3f9cd96f6b33"));
        }

        PublicationSelection publications = resolvePublications(
                storeId,
                publishedRoomTypeIds,
                publishedRoomIds
        );
        if (enabled && publications.roomTypeIds().isEmpty()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d20b0c5718f6"));
        }
        if (enabled && !isHomePagePublished(storeId, site)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.26971d3f7e07"));
        }
        validatePricePlanMappings(storeId, pricePlan.getId(), publications.roomTypeIds());

        site.setStoreId(storeId);
        site.setSlug(slug);
        if (rawName != null) {
            site.setName(normalizeSiteName(rawName));
        }
        if (site.getName() == null) {
            site.setName(slug);
        }
        if (rawThemeKey != null) {
            site.setThemeKey(normalizeThemeKey(rawThemeKey));
        }
        site.setEnabled(enabled);
        site.setChannel(channel);
        site.setPaymentProvider(resolvePaymentProvider(rawPaymentProvider, site));
        site.setSimulatedPaymentEnabled(simulatedPaymentEnabled);

        IndependentSite saved = siteRepository.save(site);

        channel.setDefaultPricePlan(pricePlan);
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(normalizeMoney(priceAdjustmentValue));
        // BOOKING_ENGINE 渠道为全店共享：enabled/isActive 不跟随单个站点开关，
        // 按"该店是否存在任一启用站点"重算，避免关闭一个站点拖垮同店其它启用站点。
        boolean anySiteEnabled = siteRepository.findByStoreIdOrderByCreatedAtAscIdAsc(storeId).stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getEnabled()));
        channel.setEnabled(anySiteEnabled);
        channel.setIsActive(anySiteEnabled);
        channelRepository.save(channel);

        publicationRepository.deleteByStoreIdAndSiteIdInBulk(storeId, saved.getId());
        publicationRepository.saveAll(buildPublicationEntities(saved, publications));
        disableRoomDetailPagesOutsidePublicationScope(storeId, saved.getId(), publications.roomTypeIds());
    }

    private IndependentSitePaymentProvider resolvePaymentProvider(String raw, IndependentSite site) {
        if (raw == null || raw.isBlank()) {
            return site.getPaymentProvider() != null
                    ? site.getPaymentProvider()
                    : IndependentSitePaymentProvider.SIMULATED;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (IndependentSitePaymentProvider.STRIPE.name().equals(normalized)) {
            if (!stripeSettingsService.isFullyConfigured(site.getStoreId())) {
                throw new IndependentSiteServiceException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "PAYMENT_PROVIDER_NOT_AVAILABLE",
                        ApiMessages.get("api.t.5142661bd220")
                );
            }
            return IndependentSitePaymentProvider.STRIPE;
        }
        if (!IndependentSitePaymentProvider.SIMULATED.name().equals(normalized)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.999ffc4d461a"));
        }
        return IndependentSitePaymentProvider.SIMULATED;
    }

    private boolean isHomePagePublished(Long storeId, IndependentSite site) {
        if (site.getId() == null) {
            return false;
        }
        IndependentSitePage home = homePage(storeId, site.getId());
        return home != null && home.getPublishedAt() != null;
    }

    private JsonNode savePageDraftInternal(
            IndependentSitePage page,
            JsonNode draftSchema,
            Long expectedDraftVersion
    ) {
        long currentDraftVersion = draftVersion(page);
        if (expectedDraftVersion != null && expectedDraftVersion != currentDraftVersion) {
            throw conflict("DRAFT_VERSION_CONFLICT", ApiMessages.get("api.t.3cc0a609ba1d"));
        }
        JsonNode schema = validateByFormat(page.getFormat(), draftSchema);
        page.setDraftSchemaJson(writeJson(schema));
        page.setDraftUpdatedAt(nowUtc());
        page.setDraftVersion(currentDraftVersion + 1);
        return schema;
    }

    private void publishPageInternal(IndependentSite site, IndependentSitePage page, long expectedDraftVersion) {
        if (page.getDraftSchemaJson() == null || page.getDraftSchemaJson().isBlank()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.38dccde7ee5e"));
        }
        if (expectedDraftVersion != draftVersion(page)) {
            throw conflict("DRAFT_VERSION_CONFLICT", ApiMessages.get("api.t.a608474868bd"));
        }
        JsonNode validatedDraft = validateByFormat(page.getFormat(), readJson(page.getDraftSchemaJson()));
        LocalDateTime now = nowUtc();
        page.setPublishedSchemaJson(writeJson(validatedDraft));
        page.setPublishedAt(now);
        pageRepository.save(page);
        if (page.getType() == IndependentSitePageType.HOME) {
            site.setPublishedAt(now);
            siteRepository.save(site);
        }
    }

    private IndependentSitePage createHomePage(
            IndependentSite site,
            String title,
            IndependentSitePageFormat format
    ) {
        IndependentSitePage page = new IndependentSitePage();
        page.setStoreId(site.getStoreId());
        page.setSite(site);
        page.setPath(HOME_PAGE_PATH);
        page.setType(IndependentSitePageType.HOME);
        page.setFormat(format);
        page.setTitle(title);
        page.setDraftSchemaJson(writeJson(
                format == IndependentSitePageFormat.CANVAS
                        ? canvasValidator.defaultCanvasSchema(title)
                        : pageSchemaValidator.defaultSchema()
        ));
        page.setDraftVersion(1L);
        page.setDraftUpdatedAt(nowUtc());
        page.setSortOrder(0);
        page.setEnabled(true);
        return pageRepository.save(page);
    }

    /** 按页面格式分派白名单校验：BLOCKS 走旧区块校验器，CANVAS 走节点树校验器。 */
    private JsonNode validateByFormat(IndependentSitePageFormat format, JsonNode schema) {
        return format == IndependentSitePageFormat.CANVAS
                ? canvasValidator.validate(schema)
                : pageSchemaValidator.validate(schema);
    }

    private IndependentSite requireSite(Long storeId, Long siteId) {
        return siteRepository.findByStoreIdAndId(storeId, siteId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.87f7c7d02b26")));
    }

    private IndependentSite defaultSite(Long storeId) {
        List<IndependentSite> sites = siteRepository.findByStoreIdOrderByCreatedAtAscIdAsc(storeId);
        return sites.isEmpty() ? null : sites.get(0);
    }

    private IndependentSite defaultSiteForUpdate(Long storeId) {
        IndependentSite site = defaultSite(storeId);
        if (site == null) {
            return null;
        }
        return siteRepository.findByStoreIdAndIdWithChannelForUpdate(storeId, site.getId())
                .orElse(site);
    }

    private IndependentSitePage homePage(Long storeId, Long siteId) {
        return pageRepository
                .findByStoreIdAndSiteIdAndType(storeId, siteId, IndependentSitePageType.HOME)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private IndependentSitePage homePageForUpdate(Long storeId, Long siteId) {
        IndependentSitePage home = homePage(storeId, siteId);
        if (home == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.71503be5d39f"));
        }
        return pageRepository
                .findByStoreIdAndSiteIdAndIdForUpdate(storeId, siteId, home.getId())
                .orElse(home);
    }

    private PublicationSelection resolvePublications(
            Long storeId,
            Set<Long> requestedRoomTypeIds,
            Set<Long> requestedRoomIds
    ) {
        Set<Long> roomTypeIds = normalizeIds(requestedRoomTypeIds, "publishedRoomTypeIds");
        Set<Long> roomIds = normalizeIds(requestedRoomIds, "publishedRoomIds");

        if (!roomTypeIds.isEmpty()) {
            List<RoomType> roomTypes = roomTypeRepository.findByStoreIdAndIdIn(
                    storeId,
                    new ArrayList<>(roomTypeIds)
            );
            if (roomTypes.size() != roomTypeIds.size()) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.f5b48940817c"));
            }
        }

        if (!roomIds.isEmpty()) {
            List<Room> rooms = roomRepository.findByStoreIdAndIdIn(storeId, roomIds);
            if (rooms.size() != roomIds.size()) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.9981bedea6c6"));
            }
            for (Room room : rooms) {
                if (room.getRoomType() == null || room.getRoomType().getId() == null) {
                    throw new IllegalArgumentException(ApiMessages.get("api.t.3dd6aabd08e2"));
                }
                roomTypeIds.add(room.getRoomType().getId());
            }
        }
        return new PublicationSelection(roomTypeIds, roomIds);
    }

    private void validatePricePlanMappings(Long storeId, Long pricePlanId, Set<Long> roomTypeIds) {
        for (Long roomTypeId : roomTypeIds) {
            boolean mapped = roomTypePricePlanRepository
                    .existsByStoreIdAndRoomTypeIdAndPricePlanId(storeId, roomTypeId, pricePlanId);
            if (!mapped) {
                throw new IllegalArgumentException(
                        ApiMessages.get("api.t.bb5978278388") + roomTypeId
                );
            }
        }
    }

    private List<IndependentSitePublication> buildPublicationEntities(
            IndependentSite site,
            PublicationSelection selection
    ) {
        List<IndependentSitePublication> publications = new ArrayList<>();
        int order = 0;
        for (Long roomTypeId : selection.roomTypeIds()) {
            publications.add(publication(site, IndependentSitePublicationType.ROOM_TYPE, roomTypeId, order++));
        }
        for (Long roomId : selection.roomIds()) {
            publications.add(publication(site, IndependentSitePublicationType.ROOM, roomId, order++));
        }
        return publications;
    }

    private static IndependentSitePublication publication(
            IndependentSite site,
            IndependentSitePublicationType type,
            Long targetId,
            int order
    ) {
        IndependentSitePublication publication = new IndependentSitePublication();
        publication.setSite(site);
        publication.setTargetType(type);
        publication.setTargetId(targetId);
        publication.setDisplayOrder(order);
        publication.setEnabled(true);
        return publication;
    }

    /**
     * 发布范围联动：ROOM_DETAIL 页的 roomTypeId 不在新发布范围内时禁用该页
     * （不删除，draft/published 不动）；回到发布范围后由 generateRoomTypePages 重新启用。
     */
    private void disableRoomDetailPagesOutsidePublicationScope(
            Long storeId,
            Long siteId,
            Set<Long> publishedRoomTypeIds
    ) {
        List<IndependentSitePage> pages =
                pageRepository.findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc(storeId, siteId);
        for (IndependentSitePage page : pages) {
            if (page.getType() != IndependentSitePageType.ROOM_DETAIL) {
                continue;
            }
            boolean inScope = page.getRoomTypeId() != null
                    && publishedRoomTypeIds.contains(page.getRoomTypeId());
            if (!inScope && Boolean.TRUE.equals(page.getEnabled())) {
                page.setEnabled(false);
                pageRepository.save(page);
            }
        }
    }

    /**
     * 该站 enabled 发布物对应的房型集合：ROOM_TYPE 目标的 targetId，
     * 加上 ROOM 目标房间归属的房型（与 resolvePublications 的并入语义一致）。
     */
    private Set<Long> publicationRoomTypeIds(Long storeId, Long siteId) {
        List<IndependentSitePublication> rows =
                publicationRepository.findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc(
                        storeId,
                        siteId
                );
        Set<Long> roomTypeIds = new LinkedHashSet<>();
        Set<Long> roomIds = new LinkedHashSet<>();
        for (IndependentSitePublication row : rows) {
            if (row.getTargetType() == IndependentSitePublicationType.ROOM_TYPE) {
                roomTypeIds.add(row.getTargetId());
            } else if (row.getTargetType() == IndependentSitePublicationType.ROOM) {
                roomIds.add(row.getTargetId());
            }
        }
        if (!roomIds.isEmpty()) {
            for (Room room : roomRepository.findByStoreIdAndIdIn(storeId, roomIds)) {
                if (room.getRoomType() != null && room.getRoomType().getId() != null) {
                    roomTypeIds.add(room.getRoomType().getId());
                }
            }
        }
        return roomTypeIds;
    }

    private JsonNode buildRoomDetailSchema(RoomType roomType) {
        String name = roomType.getName() == null ? "" : roomType.getName().trim();
        List<String> photos = roomType.getDesktopPhotoUrls();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", IndependentSitePageSchemaValidator.SCHEMA_VERSION);
        root.set("theme", pageSchemaValidator.defaultSchema().get("theme").deepCopy());
        ArrayNode sections = root.putArray("sections");

        ObjectNode hero = sections.addObject();
        hero.put("type", "HERO");
        hero.put("title", name);
        String description = trimToNull(roomType.getDescription());
        if (description != null) {
            hero.put("body", truncate(description, 600));
        }
        if (!photos.isEmpty()) {
            hero.put("imageUrl", photos.get(0));
        }
        hero.put("alignment", "CENTER");

        if (!photos.isEmpty()) {
            ObjectNode gallery = sections.addObject();
            gallery.put("type", "GALLERY");
            gallery.put("title", name);
            ArrayNode images = gallery.putArray("images");
            for (String url : photos.stream().limit(12).toList()) {
                ObjectNode image = images.addObject();
                image.put("url", url);
                image.put("alt", truncate(name, 100));
            }
        }

        ObjectNode about = sections.addObject();
        about.put("type", "ABOUT");
        about.put("title", ApiMessages.get("api.t.1c20862281c8"));
        String aboutBody = buildRoomAboutBody(roomType);
        if (aboutBody != null) {
            about.put("body", aboutBody);
        }

        List<String> amenities = new ArrayList<>();
        for (FacilityDTO facility : roomType.getFacilities()) {
            if (amenities.size() >= 12) {
                break;
            }
            String item = facility == null ? null : trimToNull(facility.getName());
            if (item != null) {
                String truncated = truncate(item, 100);
                if (!amenities.contains(truncated)) {
                    amenities.add(truncated);
                }
            }
        }
        if (!amenities.isEmpty()) {
            ObjectNode amenitiesSection = sections.addObject();
            amenitiesSection.put("type", "AMENITIES");
            amenitiesSection.put("title", ApiMessages.get("api.t.3deed6506845"));
            ArrayNode items = amenitiesSection.putArray("items");
            amenities.forEach(items::add);
        }

        ObjectNode booking = sections.addObject();
        booking.put("type", "BOOKING");
        booking.put("title", ApiMessages.get("api.t.ea64d274ea8f"));

        return pageSchemaValidator.validate(root);
    }

    /**
     * CANVAS 版房型详情页骨架（纯代码模板，不走 AI）：
     * hero（房型名 + 首图 + 描述）→ 信息区（面积/入住/设施 ul，全部来自 PMS 数据）
     * → CTA（scroll-to-booking）。整树过 canvas 白名单校验，失败由调用方记入 skipped。
     */
    private JsonNode buildCanvasRoomDetailSchema(RoomType roomType) {
        String name = roomType.getName() == null ? "" : roomType.getName().trim();
        List<String> photos = roomType.getDesktopPhotoUrls();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", IndependentSiteCanvasValidator.SCHEMA_VERSION);
        ObjectNode main = root.putObject("root");
        main.put("id", "root");
        main.put("type", "element");
        main.put("tag", "main");
        main.put("class", "min-h-screen bg-white text-slate-800");
        ArrayNode mainChildren = main.putArray("children");

        ObjectNode hero = mainChildren.addObject();
        hero.put("id", "sec-hero");
        hero.put("type", "element");
        hero.put("tag", "section");
        hero.put(
                "class",
                "flex min-h-[50vh] flex-col items-center justify-center gap-6 px-6 py-16 text-center"
        );
        ArrayNode heroChildren = hero.putArray("children");

        ObjectNode title = heroChildren.addObject();
        title.put("id", "hero-title");
        title.put("type", "element");
        title.put("tag", "h1");
        title.put("class", "text-4xl font-bold tracking-wide md:text-6xl");
        canvasText(title.putArray("children"), "hero-title-t", truncate(name, 120));

        if (!photos.isEmpty()) {
            ObjectNode image = heroChildren.addObject();
            image.put("id", "hero-img");
            image.put("type", "element");
            image.put("tag", "img");
            image.put("class", "w-full max-w-3xl rounded-2xl object-cover shadow-lg");
            ObjectNode attrs = image.putObject("attrs");
            attrs.put("src", photos.get(0));
            attrs.put("alt", truncate(name, 100));
        }

        String description = trimToNull(roomType.getDescription());
        if (description != null) {
            ObjectNode desc = heroChildren.addObject();
            desc.put("id", "hero-desc");
            desc.put("type", "element");
            desc.put("tag", "p");
            desc.put("class", "max-w-2xl text-lg text-slate-500");
            canvasText(desc.putArray("children"), "hero-desc-t", truncate(description, 500));
        }

        List<String> facts = new ArrayList<>();
        if (roomType.getSizeMeasurement() != null) {
            String unit = trimToNull(roomType.getSizeMeasurementUnit());
            facts.add(
                    ApiMessages.get("api.t.4a9ac763e06a") + roomType.getSizeMeasurement().stripTrailingZeros().toPlainString()
                            + (unit == null ? "" : " " + unit)
            );
        }
        Integer maxGuests = roomType.getMaxGuests();
        Integer maxChildren = roomType.getMaxChildOccupancy();
        if (maxGuests != null && maxGuests > 0) {
            String occupancy = ApiMessages.get("api.t.e1af1a6ecb23") + maxGuests + ApiMessages.get("api.t.5f70876879a5");
            if (maxChildren != null && maxChildren > 0) {
                occupancy += "、" + maxChildren + ApiMessages.get("api.t.1f9092cd4f8b");
            }
            facts.add(occupancy);
        }
        for (FacilityDTO facility : roomType.getFacilities()) {
            if (facts.size() >= 14) {
                break;
            }
            String item = facility == null ? null : trimToNull(facility.getName());
            if (item != null) {
                String truncated = truncate(item, 100);
                if (!facts.contains(truncated)) {
                    facts.add(truncated);
                }
            }
        }

        ObjectNode info = mainChildren.addObject();
        info.put("id", "sec-info");
        info.put("type", "element");
        info.put("tag", "section");
        info.put("class", "mx-auto w-full max-w-3xl px-6 py-16");
        ArrayNode infoChildren = info.putArray("children");

        ObjectNode infoTitle = infoChildren.addObject();
        infoTitle.put("id", "info-title");
        infoTitle.put("type", "element");
        infoTitle.put("tag", "h2");
        infoTitle.put("class", "text-2xl font-semibold");
        canvasText(infoTitle.putArray("children"), "info-title-t", ApiMessages.get("api.t.4156c8fcae09"));

        ObjectNode list = infoChildren.addObject();
        list.put("id", "info-list");
        list.put("type", "element");
        list.put("tag", "ul");
        list.put("class", "mt-6 list-disc space-y-2 pl-6 text-slate-600");
        ArrayNode listChildren = list.putArray("children");
        for (int index = 0; index < facts.size(); index++) {
            ObjectNode item = listChildren.addObject();
            item.put("id", "info-li-" + (index + 1));
            item.put("type", "element");
            item.put("tag", "li");
            canvasText(item.putArray("children"), "info-li-" + (index + 1) + "-t", facts.get(index));
        }

        ObjectNode cta = mainChildren.addObject();
        cta.put("id", "sec-cta");
        cta.put("type", "element");
        cta.put("tag", "section");
        cta.put("class", "flex justify-center px-6 pb-24");
        ArrayNode ctaChildren = cta.putArray("children");
        ObjectNode button = ctaChildren.addObject();
        button.put("id", "cta-button");
        button.put("type", "element");
        button.put("tag", "button");
        button.put(
                "class",
                "rounded-full bg-slate-900 px-8 py-3 text-white transition hover:bg-slate-700"
        );
        button.put("action", "scroll-to-booking");
        canvasText(button.putArray("children"), "cta-button-t", ApiMessages.get("api.t.ea64d274ea8f"));

        return canvasValidator.validate(root);
    }

    private static void canvasText(ArrayNode parent, String id, String text) {
        ObjectNode node = parent.addObject();
        node.put("id", id);
        node.put("type", "text");
        node.put("text", text);
    }

    private static String buildRoomAboutBody(RoomType roomType) {
        List<String> parts = new ArrayList<>();
        if (roomType.getSizeMeasurement() != null) {
            String unit = trimToNull(roomType.getSizeMeasurementUnit());
            parts.add(
                    ApiMessages.get("api.t.4a9ac763e06a") + roomType.getSizeMeasurement().stripTrailingZeros().toPlainString()
                            + (unit == null ? "" : " " + unit)
            );
        }
        Integer maxGuests = roomType.getMaxGuests();
        Integer maxChildren = roomType.getMaxChildOccupancy();
        if (maxGuests != null && maxGuests > 0) {
            String occupancy = ApiMessages.get("api.t.e1af1a6ecb23") + maxGuests + ApiMessages.get("api.t.5f70876879a5");
            if (maxChildren != null && maxChildren > 0) {
                occupancy += "、" + maxChildren + ApiMessages.get("api.t.1f9092cd4f8b");
            }
            parts.add(occupancy);
        }
        if (parts.isEmpty()) {
            return null;
        }
        return truncate(String.join("，", parts), 600);
    }

    private static String resolveRoomPagePath(RoomType roomType, Set<String> usedPaths) {
        String slug = slugifyRoomTypeCode(roomType.getCode());
        if (slug.isEmpty()) {
            slug = "room-type-" + roomType.getId();
        }
        String base = "/rooms/" + slug;
        String path = base;
        if (usedPaths.contains(path)) {
            path = base + "-" + roomType.getId();
        }
        int suffix = 2;
        while (usedPaths.contains(path)) {
            path = base + "-" + roomType.getId() + "-" + suffix;
            suffix++;
        }
        return path;
    }

    private static String slugifyRoomTypeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private IndependentSiteDtos.ConfigResponse toConfigResponse(IndependentSite site) {
        Channel channel = site.getChannel();
        PricePlan pricePlan = channel != null ? channel.getDefaultPricePlan() : null;
        PublicationSelection publications = readPublicationSelection(site);
        IndependentSitePage home = site.getId() == null ? null : homePage(site.getStoreId(), site.getId());
        IndependentSitePageFormat homeFormat = home != null
                ? home.getFormat()
                : IndependentSitePageFormat.BLOCKS;
        JsonNode publishedPageSchema = home != null
                && home.getPublishedSchemaJson() != null
                && !home.getPublishedSchemaJson().isBlank()
                ? validateByFormat(homeFormat, readJson(home.getPublishedSchemaJson()))
                : defaultSchemaByFormat(homeFormat, site.getName());
        return new IndependentSiteDtos.ConfigResponse(
                site.getId(),
                site.getSlug(),
                Boolean.TRUE.equals(site.getEnabled()),
                "/stay/" + site.getSlug(),
                channel != null ? channel.getId() : null,
                channel != null ? channel.getCode() : null,
                pricePlan != null ? pricePlan.getId() : null,
                pricePlan != null ? pricePlan.getName() : null,
                channel != null && channel.getPriceAdjustmentType() != null
                        ? channel.getPriceAdjustmentType().name()
                        : null,
                channel != null ? normalizeMoney(channel.getPriceAdjustmentValue()) : BigDecimal.ZERO.setScale(2),
                site.getPaymentProvider() != null ? site.getPaymentProvider().name() : null,
                Boolean.TRUE.equals(site.getSimulatedPaymentEnabled()),
                publications.roomTypeIds(),
                publications.roomIds(),
                publishedPageSchema,
                home != null ? readDraftSchema(home) : null,
                home != null ? toOffset(home.getDraftUpdatedAt()) : null,
                home != null ? draftVersion(home) : 0L,
                toOffset(site.getPublishedAt()),
                site.getRowVersion()
        );
    }

    private IndependentSiteDtos.SiteDetailResponse toDetailResponse(IndependentSite site) {
        Channel channel = site.getChannel();
        PricePlan pricePlan = channel != null ? channel.getDefaultPricePlan() : null;
        PublicationSelection publications = readPublicationSelection(site);
        List<IndependentSiteDtos.PageSummaryResponse> pages =
                pageRepository.findByStoreIdAndSiteIdOrderBySortOrderAscIdAsc(
                                site.getStoreId(),
                                site.getId()
                        )
                        .stream()
                        .map(this::toPageSummary)
                        .toList();
        return new IndependentSiteDtos.SiteDetailResponse(
                site.getId(),
                site.getName(),
                site.getSlug(),
                Boolean.TRUE.equals(site.getEnabled()),
                site.getThemeKey(),
                "/stay/" + site.getSlug(),
                channel != null ? channel.getId() : null,
                channel != null ? channel.getCode() : null,
                pricePlan != null ? pricePlan.getId() : null,
                pricePlan != null ? pricePlan.getName() : null,
                channel != null && channel.getPriceAdjustmentType() != null
                        ? channel.getPriceAdjustmentType().name()
                        : null,
                channel != null ? normalizeMoney(channel.getPriceAdjustmentValue()) : BigDecimal.ZERO.setScale(2),
                site.getPaymentProvider() != null ? site.getPaymentProvider().name() : null,
                Boolean.TRUE.equals(site.getSimulatedPaymentEnabled()),
                publications.roomTypeIds(),
                publications.roomIds(),
                toOffset(site.getPublishedAt()),
                site.getRowVersion(),
                pages,
                stripeSettingsService.isFullyConfigured(site.getStoreId())
        );
    }

    private IndependentSiteDtos.PageSummaryResponse toPageSummary(IndependentSitePage page) {
        boolean hasUnpublishedChanges = page.getDraftSchemaJson() != null
                && !page.getDraftSchemaJson().isBlank()
                && !Objects.equals(page.getDraftSchemaJson(), page.getPublishedSchemaJson());
        return new IndependentSiteDtos.PageSummaryResponse(
                page.getId(),
                page.getPath(),
                page.getType() != null ? page.getType().name() : null,
                page.getTitle(),
                Boolean.TRUE.equals(page.getEnabled()),
                page.getSortOrder() == null ? 0 : page.getSortOrder(),
                page.getRoomTypeId(),
                toOffset(page.getDraftUpdatedAt()),
                toOffset(page.getPublishedAt()),
                hasUnpublishedChanges,
                page.getFormat().name()
        );
    }

    private IndependentSiteDtos.PageDetailResponse toPageDetail(IndependentSitePage page) {
        return new IndependentSiteDtos.PageDetailResponse(
                page.getId(),
                page.getSite() != null ? page.getSite().getId() : null,
                page.getPath(),
                page.getType() != null ? page.getType().name() : null,
                page.getTitle(),
                page.getSeoDescription(),
                page.getRoomTypeId(),
                Boolean.TRUE.equals(page.getEnabled()),
                page.getSortOrder() == null ? 0 : page.getSortOrder(),
                readPageSchema(page, page.getDraftSchemaJson()),
                readPageSchema(page, page.getPublishedSchemaJson()),
                draftVersion(page),
                toOffset(page.getDraftUpdatedAt()),
                toOffset(page.getPublishedAt()),
                page.getDraftBackupSchemaJson() != null && !page.getDraftBackupSchemaJson().isBlank(),
                page.getFormat().name()
        );
    }

    private JsonNode readPageSchema(IndependentSitePage page, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return validateByFormat(page.getFormat(), readJson(json));
    }

    private JsonNode readDraftSchema(IndependentSitePage page) {
        if (page.getDraftSchemaJson() == null || page.getDraftSchemaJson().isBlank()) {
            return null;
        }
        return validateByFormat(page.getFormat(), readJson(page.getDraftSchemaJson()));
    }

    private JsonNode defaultSchemaByFormat(IndependentSitePageFormat format, String name) {
        return format == IndependentSitePageFormat.CANVAS
                ? canvasValidator.defaultCanvasSchema(name)
                : pageSchemaValidator.defaultSchema();
    }

    private static long draftVersion(IndependentSitePage page) {
        return page.getDraftVersion() == null ? 0L : page.getDraftVersion();
    }

    private PublicationSelection readPublicationSelection(IndependentSite site) {
        Set<Long> roomTypeIds = new LinkedHashSet<>();
        Set<Long> roomIds = new LinkedHashSet<>();
        List<IndependentSitePublication> rows =
                publicationRepository.findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc(
                        site.getStoreId(),
                        site.getId()
                );
        for (IndependentSitePublication row : rows) {
            if (row.getTargetType() == IndependentSitePublicationType.ROOM_TYPE) {
                roomTypeIds.add(row.getTargetId());
            } else if (row.getTargetType() == IndependentSitePublicationType.ROOM) {
                roomIds.add(row.getTargetId());
            }
        }
        return new PublicationSelection(roomTypeIds, roomIds);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(ApiMessages.get("api.t.62681428ec01"), e);
        }
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(ApiMessages.get("api.t.59af26623ec1"), e);
        }
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static java.time.OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private static String normalizeSlug(String value) {
        if (value == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.fa49aa62fc93"));
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?")) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.6c1fb59ae508"));
        }
        if (RESERVED_SLUGS.contains(normalized)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d7bdb3244bd6"));
        }
        return normalized;
    }

    private static String normalizeSiteName(String value) {
        if (value == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.78e4a8063b1b"));
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 120) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.48b7c37dd3cc"));
        }
        return normalized;
    }

    private static String normalizePageTitle(String value) {
        if (value == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.7d0c83d99c36"));
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 120) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.cef8a481eab7"));
        }
        return normalized;
    }

    private static String normalizeSeoDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 300) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.6efc31a08f5b"));
        }
        return normalized;
    }

    private static String normalizeThemeKey(String value) {
        if (value == null || value.isBlank()) {
            return "classic";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!THEME_KEYS.contains(normalized)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.bdd80c433e35"));
        }
        return normalized;
    }

    private static IndependentSitePageType parseCreatablePageType(String value) {
        if (value == null || value.isBlank()) {
            return IndependentSitePageType.CUSTOM;
        }
        IndependentSitePageType type;
        try {
            type = IndependentSitePageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.1b9f921f2ba0"));
        }
        if (type != IndependentSitePageType.CUSTOM) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.0adc88498c26"));
        }
        return type;
    }

    private static String normalizePagePath(String value) {
        if (value == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.cb6a26734cae"));
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.37c40b0838f9"));
        }
        if (!normalized.matches("^/[a-z0-9][a-z0-9/-]{0,119}$")) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.9bf03633aaf1"));
        }
        return normalized;
    }

    private static Set<Long> normalizeIds(Set<Long> values, String fieldName) {
        Set<Long> normalized = new TreeSet<>();
        if (values == null) {
            return normalized;
        }
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException(fieldName + ApiMessages.get("api.t.4371e3426af5"));
            }
            normalized.add(value);
        }
        return normalized;
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void requireStoreId(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2bfd332b0f72"));
        }
    }

    private static IndependentSiteServiceException conflict(String code, String message) {
        return new IndependentSiteServiceException(HttpStatus.CONFLICT, code, message);
    }

    private record PublicationSelection(Set<Long> roomTypeIds, Set<Long> roomIds) {
        private PublicationSelection {
            roomTypeIds = Set.copyOf(Objects.requireNonNull(roomTypeIds));
            roomIds = Set.copyOf(Objects.requireNonNull(roomIds));
        }
    }
}
