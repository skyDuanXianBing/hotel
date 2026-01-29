package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.PricePlan;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.util.SuContentPayloadBuilder;
import server.demo.util.SuRoomIdUtil;

import java.util.List;

/**
 * PMS -> Su：同步房型/费率计划清单（供 Su Widget 映射下拉使用）。
 */
@Service
public class SuContentSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SuContentSyncService.class);
    private static final Logger pmsPushLogger = LoggerFactory.getLogger("SU_PMS_PUSH");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PricePlanRepository pricePlanRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;

    public SuContentSyncService(
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            PricePlanRepository pricePlanRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.pricePlanRepository = pricePlanRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    /**
     * PMS -> Su：同步“房间号列表”（用于 Su Widget 映射到具体房间）。
     */
    public SuRoomSyncSummary syncRoomsForWidget(Long storeId, String hotelId) {
        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        if (rooms.isEmpty()) {
            logger.info("Skip Su rooms sync: no rooms. storeId={}, hotelId={}", storeId, hotelId);
            pmsPushLogger.info("[SuRoomsSync] skip (no rooms). storeId={}, hotelId={}", storeId, hotelId);
            return new SuRoomSyncSummary(0, true, null);
        }

        List<Room> eligibleRooms = rooms.stream()
                .filter(r -> r != null && r.getRoomNumber() != null && !r.getRoomNumber().isBlank())
                .toList();
        SuRoomIdUtil.assertRoomIdsWithinLimit(eligibleRooms);

        int successCount = 0;
        int failedCount = 0;
        StringBuilder errorBuilder = new StringBuilder();

        logger.info("[SuRoomsSync] start. storeId={}, hotelId={}, total={}", storeId, hotelId, rooms.size());
        pmsPushLogger.info("[SuRoomsSync] start. storeId={}, hotelId={}, total={}", storeId, hotelId, rooms.size());

        for (Room room : rooms) {
            if (room == null || room.getRoomNumber() == null || room.getRoomNumber().isBlank()) {
                continue;
            }

            String roomId = SuRoomIdUtil.buildRoomId(room);

            try {
                UpsertOutcome outcome = upsertSingleRoom(storeId, hotelId, room);
                if (outcome.success) {
                    successCount++;
                } else {
                    failedCount++;
                    appendError(errorBuilder, roomId, outcome.message);
                }
            } catch (RuntimeException e) {
                failedCount++;
                logger.error("[SuRoomsSync] room upsert failed. storeId={}, hotelId={}, roomId={}", storeId, hotelId, roomId, e);
                pmsPushLogger.error("[SuRoomsSync] room upsert failed. storeId={}, hotelId={}, roomId={}, err={}", storeId, hotelId, roomId, e.getMessage());
                appendError(errorBuilder, roomId, e.getMessage());
            }
        }

        boolean allOk = failedCount == 0;
        String err = errorBuilder.length() > 0 ? errorBuilder.toString() : null;
        logger.info("[SuRoomsSync] done. storeId={}, hotelId={}, success={}, failed={}", storeId, hotelId, successCount, failedCount);
        pmsPushLogger.info("[SuRoomsSync] done. storeId={}, hotelId={}, success={}, failed={}", storeId, hotelId, successCount, failedCount);
        if (!allOk) {
            logger.warn("[SuRoomsSync] failed summary. storeId={}, hotelId={}, err={}", storeId, hotelId, err);
            pmsPushLogger.warn("[SuRoomsSync] failed summary. storeId={}, hotelId={}, err={}", storeId, hotelId, err);
        }

        return new SuRoomSyncSummary(rooms.size(), allOk, err);
    }

    private record UpsertOutcome(boolean success, String message) {}

    private UpsertOutcome upsertSingleRoom(Long storeId, String hotelId, Room room) {
        String roomId = SuRoomIdUtil.buildRoomId(room);

        JsonNode createResp = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.postOtaHotelRoom(
                        token,
                        SuContentPayloadBuilder.buildRoomCreatePayloadForRooms(hotelId, List.of(room))
                ),
                "OTA_HotelRoom(room=" + roomId + ")"
        );

        if (suApiClient.isSuSuccess(createResp)) {
            return new UpsertOutcome(true, null);
        }

        String createErr = suApiClient.extractSuErrorMessage(createResp);
        if (!isRoomAlreadyExists(createErr)) {
            logger.warn("[SuRoomsSync] create failed. storeId={}, hotelId={}, roomId={}, err={}", storeId, hotelId, roomId, createErr);
            pmsPushLogger.warn("[SuRoomsSync] create failed. storeId={}, hotelId={}, roomId={}, err={}", storeId, hotelId, roomId, createErr);
            return new UpsertOutcome(false, createErr != null ? createErr : "Su 房间创建失败");
        }

        logger.info("[SuRoomsSync] room already exists, switching to modify. storeId={}, hotelId={}, roomId={}", storeId, hotelId, roomId);

        JsonNode modifyResp = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.postOtaHotelRoom(
                        token,
                        SuContentPayloadBuilder.buildRoomModifyPayloadForRooms(hotelId, List.of(room))
                ),
                "OTA_HotelRoom(modify room=" + roomId + ")"
        );

        if (suApiClient.isSuSuccess(modifyResp)) {
            return new UpsertOutcome(true, null);
        }

        String modifyErr = suApiClient.extractSuErrorMessage(modifyResp);
        if (isRoomDoesNotExist(modifyErr)) {
            logger.warn("[SuRoomsSync] modify says room does not exist, retry create once. storeId={}, hotelId={}, roomId={}", storeId, hotelId, roomId);
            pmsPushLogger.warn("[SuRoomsSync] modify says room does not exist, retry create once. storeId={}, hotelId={}, roomId={}", storeId, hotelId, roomId);
            JsonNode retryCreateResp = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.postOtaHotelRoom(
                            token,
                            SuContentPayloadBuilder.buildRoomCreatePayloadForRooms(hotelId, List.of(room))
                    ),
                    "OTA_HotelRoom(retry-create room=" + roomId + ")"
            );
            if (suApiClient.isSuSuccess(retryCreateResp)) {
                return new UpsertOutcome(true, null);
            }
            String retryErr = suApiClient.extractSuErrorMessage(retryCreateResp);
            return new UpsertOutcome(false, retryErr != null ? retryErr : "Su 房间创建失败(重试)");
        }

        logger.warn("[SuRoomsSync] modify failed. storeId={}, hotelId={}, roomId={}, err={}", storeId, hotelId, roomId, modifyErr);
        pmsPushLogger.warn("[SuRoomsSync] modify failed. storeId={}, hotelId={}, roomId={}, err={}", storeId, hotelId, roomId, modifyErr);
        return new UpsertOutcome(false, modifyErr != null ? modifyErr : "Su 房间修改失败");
    }

    private static boolean isRoomAlreadyExists(String err) {
        if (err == null || err.isBlank()) {
            return false;
        }
        String lower = err.toLowerCase();
        return lower.contains("already") && lower.contains("exist");
    }

    private static boolean isRoomDoesNotExist(String err) {
        if (err == null || err.isBlank()) {
            return false;
        }
        String lower = err.toLowerCase();
        return lower.contains("does not") && lower.contains("exist");
    }

    private static void appendError(StringBuilder builder, String roomId, String message) {
        if (builder == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("; ");
        }
        builder.append(roomId).append(":").append(message != null ? message : "unknown error");
    }

    /**
     * PMS -> Su：同步“价格计划列表”（用于 Su Widget 费率计划映射下拉）。
     */
    public SuRatePlanSyncSummary syncRatePlansForWidget(Long storeId, String hotelId) {
        List<PricePlan> pricePlans = pricePlanRepository.findByStoreIdOrderByName(storeId);
        if (pricePlans.isEmpty()) {
            logger.info("Skip Su rate plans sync: no price plans. storeId={}, hotelId={}", storeId, hotelId);
            pmsPushLogger.info("[SuRatePlansSync] skip (no price plans). storeId={}, hotelId={}", storeId, hotelId);
            return new SuRatePlanSyncSummary(0, true, null);
        }

        String planSummary = pricePlans.stream()
                .map(p -> p.getId() + ":" + (p.getName() != null ? p.getName() : ""))
                .limit(200)
                .toList()
                .toString();
        logger.info("[SuRatePlansSync] start. storeId={}, hotelId={}, count={}, plans={}", storeId, hotelId, pricePlans.size(), planSummary);
        pmsPushLogger.info("[SuRatePlansSync] start. storeId={}, hotelId={}, count={}, plans={}", storeId, hotelId, pricePlans.size(), planSummary);

        int successCount = 0;
        int failedCount = 0;
        StringBuilder errorBuilder = new StringBuilder();

        for (PricePlan plan : pricePlans) {
            if (plan == null || plan.getId() == null) {
                continue;
            }

            String planId = String.valueOf(plan.getId());
            try {
                UpsertOutcome outcome = upsertSingleRatePlan(storeId, hotelId, plan);
                if (outcome.success) {
                    successCount++;
                } else {
                    failedCount++;
                    appendError(errorBuilder, planId, outcome.message);
                }
            } catch (RuntimeException e) {
                failedCount++;
                logger.error("[SuRatePlansSync] rate plan upsert failed. storeId={}, hotelId={}, planId={}", storeId, hotelId, planId, e);
                pmsPushLogger.error("[SuRatePlansSync] rate plan upsert failed. storeId={}, hotelId={}, planId={}, err={}", storeId, hotelId, planId, e.getMessage());
                appendError(errorBuilder, planId, e.getMessage());
            }
        }

        boolean allOk = failedCount == 0;
        String err = errorBuilder.length() > 0 ? errorBuilder.toString() : null;
        logger.info("[SuRatePlansSync] done. storeId={}, hotelId={}, success={}, failed={}", storeId, hotelId, successCount, failedCount);
        pmsPushLogger.info("[SuRatePlansSync] done. storeId={}, hotelId={}, success={}, failed={}", storeId, hotelId, successCount, failedCount);
        if (!allOk) {
            logger.warn("[SuRatePlansSync] failed summary. storeId={}, hotelId={}, err={}", storeId, hotelId, err);
            pmsPushLogger.warn("[SuRatePlansSync] failed summary. storeId={}, hotelId={}, err={}", storeId, hotelId, err);
        }

        return new SuRatePlanSyncSummary(pricePlans.size(), allOk, err);
    }

    private UpsertOutcome upsertSingleRatePlan(Long storeId, String hotelId, PricePlan plan) {
        String planId = String.valueOf(plan.getId());

        Object createPayload = SuContentPayloadBuilder.buildRatePlanCreatePayload(hotelId, List.of(plan));
        if (logger.isDebugEnabled()) {
            try {
                logger.debug("[SuRatePlansSync] create payload(single). storeId={}, hotelId={}, planId={}, payload={}", storeId, hotelId, planId, objectMapper.writeValueAsString(createPayload));
            } catch (Exception e) {
                logger.debug("[SuRatePlansSync] create payload serialize failed(single). storeId={}, hotelId={}, planId={}, err={}", storeId, hotelId, planId, e.getMessage());
            }
        }

        JsonNode createResp = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.postOtaHotelRatePlan(token, createPayload),
                "OTA_HotelRatePlan(create plan=" + planId + ")"
        );

        if (suApiClient.isSuSuccess(createResp)) {
            return new UpsertOutcome(true, null);
        }

        String createErr = suApiClient.extractSuErrorMessage(createResp);
        if (!isRatePlanAlreadyExists(createErr)) {
            logger.warn("[SuRatePlansSync] create failed(single). storeId={}, hotelId={}, planId={}, err={}", storeId, hotelId, planId, createErr);
            pmsPushLogger.warn("[SuRatePlansSync] create failed(single). storeId={}, hotelId={}, planId={}, err={}", storeId, hotelId, planId, createErr);
            logger.warn("[SuRatePlansSync] create raw response(single). storeId={}, hotelId={}, planId={}, raw={}", storeId, hotelId, planId, createResp);
            return new UpsertOutcome(false, createErr != null ? createErr : "Su 价格计划创建失败");
        }

        logger.info("[SuRatePlansSync] rate plan already exists, switching to overlay. storeId={}, hotelId={}, planId={}", storeId, hotelId, planId);

        Object overlayPayload = SuContentPayloadBuilder.buildRatePlanOverlayPayload(hotelId, List.of(plan));
        if (logger.isDebugEnabled()) {
            try {
                logger.debug("[SuRatePlansSync] overlay payload(single). storeId={}, hotelId={}, planId={}, payload={}", storeId, hotelId, planId, objectMapper.writeValueAsString(overlayPayload));
            } catch (Exception e) {
                logger.debug("[SuRatePlansSync] overlay payload serialize failed(single). storeId={}, hotelId={}, planId={}, err={}", storeId, hotelId, planId, e.getMessage());
            }
        }

        JsonNode overlayResp = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.postOtaHotelRatePlan(token, overlayPayload),
                "OTA_HotelRatePlan(overlay plan=" + planId + ")"
        );

        if (suApiClient.isSuSuccess(overlayResp)) {
            return new UpsertOutcome(true, null);
        }

        String overlayErr = suApiClient.extractSuErrorMessage(overlayResp);
        if (isRatePlanNotExists(overlayErr)) {
            logger.warn("[SuRatePlansSync] overlay says rate does not exist, retry create once. storeId={}, hotelId={}, planId={}", storeId, hotelId, planId);
            pmsPushLogger.warn("[SuRatePlansSync] overlay says rate does not exist, retry create once. storeId={}, hotelId={}, planId={}", storeId, hotelId, planId);

            JsonNode retryCreateResp = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.postOtaHotelRatePlan(
                            token,
                            SuContentPayloadBuilder.buildRatePlanCreatePayload(hotelId, List.of(plan))
                    ),
                    "OTA_HotelRatePlan(retry-create plan=" + planId + ")"
            );
            if (suApiClient.isSuSuccess(retryCreateResp)) {
                return new UpsertOutcome(true, null);
            }
            String retryErr = suApiClient.extractSuErrorMessage(retryCreateResp);
            logger.warn("[SuRatePlansSync] retry-create raw response(single). storeId={}, hotelId={}, planId={}, raw={}", storeId, hotelId, planId, retryCreateResp);
            return new UpsertOutcome(false, retryErr != null ? retryErr : "Su 价格计划创建失败(重试)");
        }

        logger.warn("[SuRatePlansSync] overlay failed(single). storeId={}, hotelId={}, planId={}, err={}", storeId, hotelId, planId, overlayErr);
        pmsPushLogger.warn("[SuRatePlansSync] overlay failed(single). storeId={}, hotelId={}, planId={}, err={}", storeId, hotelId, planId, overlayErr);
        logger.warn("[SuRatePlansSync] overlay raw response(single). storeId={}, hotelId={}, planId={}, raw={}", storeId, hotelId, planId, overlayResp);
        return new UpsertOutcome(false, overlayErr != null ? overlayErr : "Su 价格计划覆盖失败");
    }

    private static boolean isRatePlanAlreadyExists(String err) {
        if (err == null || err.isBlank()) {
            return false;
        }
        String lower = err.toLowerCase();
        return lower.contains("already") && lower.contains("exist");
    }

    private static boolean isRatePlanNotExists(String err) {
        if (err == null || err.isBlank()) {
            return false;
        }
        String lower = err.toLowerCase();
        return (lower.contains("not") && lower.contains("exist")) || (lower.contains("does not") && lower.contains("exist"));
    }

    public SuContentSyncSummary syncRoomTypesAndRatePlans(Long storeId, String hotelId) {
        List<RoomType> roomTypes = roomTypeRepository.findByStoreIdOrderByName(storeId);
        List<PricePlan> pricePlans = pricePlanRepository.findByStoreIdOrderByName(storeId);

        if (roomTypes.isEmpty() && pricePlans.isEmpty()) {
            logger.info("Skip Su content sync: no room types and price plans. storeId={}, hotelId={}", storeId, hotelId);
            return new SuContentSyncSummary(0, 0, true, true, null, null);
        }

        boolean roomTypesOk = true;
        boolean ratePlansOk = true;
        String roomTypesError = null;
        String ratePlansError = null;

        if (!roomTypes.isEmpty()) {
            try {
                JsonNode createResp = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.postOtaHotelRoom(
                                token,
                                SuContentPayloadBuilder.buildRoomCreatePayload(hotelId, roomTypes)
                        ),
                        "OTA_HotelRoom"
                );

                if (!suApiClient.isSuSuccess(createResp)) {
                    String err = suApiClient.extractSuErrorMessage(createResp);
                    logger.warn("Su OTA_HotelRoom create returned fail, retry modify. storeId={}, hotelId={}, err={}", storeId, hotelId, err);

                    JsonNode modifyResp = suAccessTokenService.executeWithTokenRetry(
                            token -> suApiClient.postOtaHotelRoom(
                                    token,
                                    SuContentPayloadBuilder.buildRoomModifyPayload(hotelId, roomTypes)
                            ),
                            "OTA_HotelRoom"
                    );
                    if (!suApiClient.isSuSuccess(modifyResp)) {
                        String modifyErr = suApiClient.extractSuErrorMessage(modifyResp);
                        throw new RuntimeException(modifyErr != null ? modifyErr : String.valueOf(modifyResp));
                    }
                }
            } catch (RuntimeException e) {
                roomTypesOk = false;
                roomTypesError = e.getMessage();
                logger.error("Su room types sync failed. storeId={}, hotelId={}", storeId, hotelId, e);
            }
        }

        if (!pricePlans.isEmpty()) {
            try {
                SuRatePlanSyncSummary summary = syncRatePlansForWidget(storeId, hotelId);
                if (!summary.ratePlansSynced) {
                    throw new RuntimeException(summary.ratePlansError != null ? summary.ratePlansError : "Su 价格计划同步失败");
                }
            } catch (RuntimeException e) {
                ratePlansOk = false;
                ratePlansError = e.getMessage();
                logger.error("Su rate plans sync failed. storeId={}, hotelId={}", storeId, hotelId, e);
            }
        }

        if (!roomTypesOk || !ratePlansOk) {
            throw new RuntimeException("同步渠道房型/价格计划失败: "
                    + (roomTypesOk ? "" : "房型=" + roomTypesError + "; ")
                    + (ratePlansOk ? "" : "价格计划=" + ratePlansError));
        }

        return new SuContentSyncSummary(roomTypes.size(), pricePlans.size(), true, true, null, null);
    }

    public record SuContentSyncSummary(
            int roomTypeCount,
            int pricePlanCount,
            boolean roomTypesSynced,
            boolean ratePlansSynced,
            String roomTypesError,
            String ratePlansError
    ) {}

    public record SuRoomSyncSummary(
            int roomCount,
            boolean roomsSynced,
            String roomsError
    ) {}

    public record SuRatePlanSyncSummary(
            int pricePlanCount,
            boolean ratePlansSynced,
            String ratePlansError
    ) {}
}
