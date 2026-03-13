package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.util.SuHotelIdUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Su 图片同步服务。
 */
@Service
public class SuImageSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SuImageSyncService.class);

    private final StoreRepository storeRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;
    private final String serverBaseUrl;

    public SuImageSyncService(
            StoreRepository storeRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService,
            @Value("${server.base-url}") String serverBaseUrl
    ) {
        this.storeRepository = storeRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
        this.serverBaseUrl = serverBaseUrl;
    }

    public void syncStoreImagesStrict(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));
        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null || hotelId.isBlank()) {
            throw new RuntimeException("Su hotelId 未配置，无法同步门店图片");
        }
        List<ImageItem> images = buildStoreImages(store);
        if (images.isEmpty()) {
            logger.info("[SuStoreImageSync] skip: no images. storeId={}, hotelId={}", storeId, hotelId);
            return;
        }
        postCreateAndAssociate(hotelId, String.valueOf(storeId), null, images, "storeId=" + storeId);
    }

    public void syncRoomTypeImagesStrict(Long storeId, RoomType roomType) {
        if (roomType == null || roomType.getId() == null) {
            return;
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));
        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null || hotelId.isBlank()) {
            throw new RuntimeException("Su hotelId 未配置，无法同步房型图片");
        }
        List<ImageItem> images = buildRoomTypeImages(roomType);
        if (images.isEmpty()) {
            logger.info("[SuRoomTypeImageSync] skip: no images. storeId={}, hotelId={}, roomTypeId={}",
                    storeId, hotelId, roomType.getId());
            return;
        }
        postCreateAndAssociate(
                hotelId,
                String.valueOf(storeId),
                String.valueOf(roomType.getId()),
                images,
                "storeId=" + storeId + ", roomTypeId=" + roomType.getId()
        );
    }

    public JsonNode retrieveImages(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));
        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null || hotelId.isBlank()) {
            throw new RuntimeException("Su hotelId 未配置，无法查询图片");
        }
        return suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.retrieveImages(token, Map.of("hotelid", hotelId)),
                "imageRetrieve"
        );
    }

    private void postCreateAndAssociate(
            String hotelId,
            String propertyPmsId,
            String roomId,
            List<ImageItem> images,
            String logContext
    ) {
        List<Map<String, Object>> createImages = images.stream()
                .map(image -> Map.<String, Object>of(
                        "url", image.url(),
                        "name", image.name(),
                        "pms_imageid", image.pmsImageId()
                ))
                .toList();
        Map<String, Object> createPayload = Map.of(
                "hotelid", hotelId,
                "proppmsid", propertyPmsId,
                "images", createImages
        );
        JsonNode createResp = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.createImages(token, createPayload),
                roomId == null ? "imageCreate(store)" : "imageCreate(roomType)"
        );
        if (!suApiClient.isSuSuccess(createResp)) {
            String err = suApiClient.extractSuErrorMessage(createResp);
            throw new RuntimeException("Su 图片创建失败: " + (err != null ? err : createResp.toString()));
        }

        List<Map<String, Object>> associationImages = images.stream()
                .map(image -> Map.<String, Object>of("pms_imageid", image.pmsImageId()))
                .toList();
        Map<String, Object> associationPayload = new java.util.LinkedHashMap<>();
        associationPayload.put("hotelid", hotelId);
        if (roomId != null) {
            associationPayload.put("roomid", roomId);
        }
        associationPayload.put("images", associationImages);
        JsonNode associationResp = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.associateImages(token, associationPayload),
                roomId == null ? "imageAssociation(store)" : "imageAssociation(roomType)"
        );
        if (!suApiClient.isSuSuccess(associationResp)) {
            String err = suApiClient.extractSuErrorMessage(associationResp);
            throw new RuntimeException("Su 图片关联失败: " + (err != null ? err : associationResp.toString()));
        }
        logger.info("[SuImageSync] success. hotelId={}, roomId={}, {}", hotelId, roomId, logContext);
    }

    private List<ImageItem> buildStoreImages(Store store) {
        Set<String> urls = new LinkedHashSet<>();
        if (store.getLogo() != null && !store.getLogo().isBlank()) {
            urls.add(store.getLogo().trim());
        }
        urls.addAll(store.getDesktopPhotoUrls());
        urls.addAll(store.getMobilePhotoUrls());

        List<ImageItem> items = new ArrayList<>();
        int index = 1;
        for (String url : urls) {
            String suffix = index == 1 ? "cover" : "gallery-" + index;
            items.add(new ImageItem(
                    toSuImageUrl(url),
                    buildName(store.getName(), "store", index),
                    "store-" + store.getId() + "-" + suffix
            ));
            index++;
        }
        return items;
    }

    private List<ImageItem> buildRoomTypeImages(RoomType roomType) {
        Set<String> urls = new LinkedHashSet<>();
        urls.addAll(roomType.getDesktopPhotoUrls());
        urls.addAll(roomType.getMobilePhotoUrls());

        List<ImageItem> items = new ArrayList<>();
        int index = 1;
        for (String url : urls) {
            items.add(new ImageItem(
                    toSuImageUrl(url),
                    buildName(roomType.getName(), "room-type", index),
                    "room-type-" + roomType.getId() + "-" + index
            ));
            index++;
        }
        return items;
    }

    private static String buildName(String baseName, String fallbackPrefix, int index) {
        String normalized = baseName != null && !baseName.isBlank() ? baseName.trim() : fallbackPrefix;
        return normalized + " " + index;
    }

    /**
     * Su 只能抓取公网可访问的完整图片地址。
     * 本地保存的相对路径会在这里补全成基于 `server.base-url` 的完整地址。
     */
    String toSuImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new RuntimeException("图片地址不能为空");
        }

        String normalizedUrl = rawUrl.trim();
        URI resolvedUri;
        try {
            if (normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
                resolvedUri = URI.create(normalizedUrl);
            } else if (normalizedUrl.startsWith("/")) {
                if (serverBaseUrl == null || serverBaseUrl.isBlank()) {
                    throw new RuntimeException("SERVER_BASE_URL 未配置，无法生成图片公网地址");
                }
                resolvedUri = URI.create(normalizeBaseUrl(serverBaseUrl)).resolve(normalizedUrl);
            } else {
                throw new RuntimeException("图片地址格式不正确: " + normalizedUrl);
            }
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("图片地址格式不正确: " + normalizedUrl, ex);
        }

        validateResolvableImageUrl(resolvedUri);
        return resolvedUri.toString();
    }

    private static String normalizeBaseUrl(String rawBaseUrl) {
        String trimmedBaseUrl = rawBaseUrl.trim();
        return trimmedBaseUrl.endsWith("/") ? trimmedBaseUrl : trimmedBaseUrl + "/";
    }

    private static void validateResolvableImageUrl(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new RuntimeException("图片地址必须为 http 或 https: " + uri);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new RuntimeException("图片地址缺少可访问域名: " + uri);
        }

        String normalizedHost = host.trim().toLowerCase();
        if ("localhost".equals(normalizedHost) || "127.0.0.1".equals(normalizedHost) || "::1".equals(normalizedHost)) {
            throw new RuntimeException("图片地址不能使用本地回环地址，请使用可公网访问的后端域名: " + uri);
        }
    }

    private record ImageItem(String url, String name, String pmsImageId) {
    }
}
