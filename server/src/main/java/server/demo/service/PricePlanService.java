package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.AssignRoomTypePricePlanRequest;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.PriceChangeHistoryRepository;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import server.demo.util.StoreContextUtils;
import server.demo.util.SuHotelIdUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class PricePlanService {

    private static final Logger logger = LoggerFactory.getLogger(PricePlanService.class);

    @Autowired
    private PricePlanRepository pricePlanRepository;

    @Autowired
    private RoomTypePricePlanRepository roomTypePricePlanRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomPriceRepository roomPriceRepository;

    @Autowired
    private ChannelPriceRepository channelPriceRepository;

    @Autowired
    private PriceChangeHistoryRepository priceChangeHistoryRepository;

    @Autowired
    private PriceLabsConnectionRepository priceLabsConnectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private SuContentSyncService suContentSyncService;

    @Autowired
    private SuAriAutoSyncService suAriAutoSyncService;

    @Value("${su.content.autosync.rateplan.enabled:true}")
    private boolean suRatePlanAutoSyncEnabled;

    @Value("${su.content.autosync.rateplan.strict:true}")
    private boolean suRatePlanAutoSyncStrict;

    @Value("${su.ari.autosync.days:365}")
    private int suAriWarmupDays;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
    }

    private String resolveOrInitSuHotelId(Long storeId) {
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return null;
        }
        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId != null) {
            return hotelId;
        }
        String generated = SuHotelIdUtil.buildDefault(storeId);
        store.setSuHotelId(generated);
        storeRepository.save(store);
        return generated;
    }

    private void syncRatePlanToSuIfEnabled(PricePlan plan) {
        if (!suRatePlanAutoSyncEnabled) {
            return;
        }
        Long storeId = currentStoreId();
        String hotelId = resolveOrInitSuHotelId(storeId);
        if (hotelId == null || hotelId.isBlank()) {
            String msg = "Su hotelId 未配置，无法同步价格计划到 SU（stores.su_hotel_id）。";
            if (suRatePlanAutoSyncStrict) {
                throw new RuntimeException(msg);
            }
            logger.warn("[SuRatePlanUpsert] skip: {} storeId={}", msg, storeId);
            return;
        }

        try {
            suContentSyncService.upsertSingleRatePlanStrict(storeId, hotelId, plan);
        } catch (RuntimeException e) {
            if (suRatePlanAutoSyncStrict) {
                throw e;
            }
            logger.warn("[SuRatePlanUpsert] best-effort failed. storeId={}, hotelId={}, planId={}, err={}",
                    storeId,
                    hotelId,
                    plan != null ? plan.getId() : null,
                    e.getMessage());
        }
    }

    private void warmupSuAriForMappingIfEnabled(Long storeId, Long roomTypeId, Long pricePlanId, String source) {
        if (storeId == null || roomTypeId == null || pricePlanId == null) {
            return;
        }
        int days = Math.max(1, suAriWarmupDays);
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days - 1L);
        suAriAutoSyncService.enqueueForStoreScope(
                storeId,
                source != null ? source : "roomtype-rateplan-mapping",
                start,
                end,
                Set.of(roomTypeId),
                Set.of(pricePlanId),
                true,
                true,
                true,
                false
        );
    }

    public List<PricePlan> getAllPricePlans() {
        return pricePlanRepository.findByStoreIdOrderByName(currentStoreId());
    }

    public Optional<PricePlan> getPricePlanById(Long id) {
        return pricePlanRepository.findByStoreIdAndId(currentStoreId(), id);
    }

    public PricePlan createPricePlan(PricePlan pricePlan) {
        Long storeId = currentStoreId();
        if (pricePlanRepository.existsByStoreIdAndName(storeId, pricePlan.getName())) {
            throw new RuntimeException("价格计划名称已存在");
        }

        User user = userRepository.findById(currentUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        pricePlan.setStoreId(storeId);
        pricePlan.setUser(user);
        PricePlan saved = pricePlanRepository.save(pricePlan);
        syncRatePlanToSuIfEnabled(saved);
        return saved;
    }

    public PricePlan updatePricePlan(Long id, PricePlan pricePlan) {
        Long storeId = currentStoreId();
        PricePlan existingPlan = pricePlanRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        if (!existingPlan.getName().equals(pricePlan.getName()) &&
                pricePlanRepository.existsByStoreIdAndNameAndIdNot(storeId, pricePlan.getName(), id)) {
            throw new RuntimeException("价格计划名称已存在");
        }

        existingPlan.setName(pricePlan.getName());
        existingPlan.setNameEn(pricePlan.getNameEn());
        existingPlan.setDescription(pricePlan.getDescription());
        existingPlan.setDescriptionEn(pricePlan.getDescriptionEn());
        existingPlan.setMinNights(pricePlan.getMinNights());
        existingPlan.setMaxNights(pricePlan.getMaxNights());
        existingPlan.setIncludeMeal(pricePlan.getIncludeMeal());
        existingPlan.setDerivationType(pricePlan.getDerivationType());
        existingPlan.setBasePlanId(pricePlan.getBasePlanId());
        existingPlan.setDerivationRule(pricePlan.getDerivationRule());
        existingPlan.setCancellationPolicy(pricePlan.getCancellationPolicy());
        existingPlan.setCancellationPolicyEn(pricePlan.getCancellationPolicyEn());

        PricePlan saved = pricePlanRepository.save(existingPlan);
        syncRatePlanToSuIfEnabled(saved);
        return saved;
    }

    public void deletePricePlan(Long id) {
        Long storeId = currentStoreId();
        PricePlan pricePlan = pricePlanRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        // If this rate plan has been used to generate any derived/overridden data, do not allow deleting it,
        // otherwise foreign keys or downstream integrations (PriceLabs etc.) may break.
        boolean hasRoomPrices = roomPriceRepository.existsByStoreIdAndPricePlanId(storeId, id);
        boolean hasChannelPrices = channelPriceRepository.existsByStoreIdAndPricePlanId(storeId, id);
        boolean hasPriceChangeHistory = priceChangeHistoryRepository.existsByStoreIdAndPricePlanId(storeId, id);
        boolean hasPriceLabsConnections = priceLabsConnectionRepository.existsByStoreIdAndPricePlanId(storeId, id);
        if (hasRoomPrices || hasChannelPrices || hasPriceChangeHistory || hasPriceLabsConnections) {
            StringBuilder msg = new StringBuilder("该价格计划已被使用，无法删除：");
            boolean first = true;
            if (hasRoomPrices) {
                msg.append(first ? "" : "、").append("房价覆盖记录");
                first = false;
            }
            if (hasChannelPrices) {
                msg.append(first ? "" : "、").append("渠道价格记录");
                first = false;
            }
            if (hasPriceChangeHistory) {
                msg.append(first ? "" : "、").append("改价历史");
                first = false;
            }
            if (hasPriceLabsConnections) {
                msg.append(first ? "" : "、").append("PriceLabs 连接");
            }
            throw new RuntimeException(msg.toString());
        }

        roomTypePricePlanRepository.deleteByStoreIdAndPricePlanId(storeId, id);
        pricePlanRepository.delete(pricePlan);
    }

    /**
     * 最小改动的“强制删除”：仅清理会直接阻塞删除的 channel_prices（渠道价格记录），然后复用常规删除流程。
     * 若仍被其它引用（room_prices / 改价历史 / PriceLabs 连接）阻塞，则继续提示具体原因。
     */
    public void forceDeletePricePlan(Long id, boolean confirm) {
        if (!confirm) {
            throw new RuntimeException("请二次确认后再执行彻底删除");
        }

        Long storeId = currentStoreId();
        pricePlanRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        channelPriceRepository.deleteByStoreIdAndPricePlanId(storeId, id);
        deletePricePlan(id);
    }

    public List<RoomTypePricePlan> getRoomTypesByPricePlan(Long pricePlanId) {
        // 结果在调用处过滤 storeId，这里沿用旧逻辑
        return roomTypePricePlanRepository.findByPricePlanId(pricePlanId);
    }

    public List<RoomTypePricePlan> getPricePlansByRoomType(Long roomTypeId) {
        return roomTypePricePlanRepository.findByRoomTypeId(roomTypeId);
    }

    public RoomTypePricePlan assignPricePlanToRoomType(Long roomTypeId, Long pricePlanId, AssignRoomTypePricePlanRequest request) {
        Long storeId = currentStoreId();
        RoomType roomType = roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId)
                .orElseThrow(() -> new RuntimeException("房型不存在或无权限"));
        PricePlan pricePlan = pricePlanRepository.findByStoreIdAndId(storeId, pricePlanId)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        Optional<RoomTypePricePlan> existing = roomTypePricePlanRepository.findByRoomTypeIdAndPricePlanId(roomTypeId, pricePlanId);
        RoomTypePricePlan savedPlan;
        if (existing.isPresent()) {
            RoomTypePricePlan entity = existing.get();
            applyAssignRequest(entity, request);
            savedPlan = roomTypePricePlanRepository.save(entity);
        } else {
            RoomTypePricePlan entity = new RoomTypePricePlan();
            entity.setRoomType(roomType);
            entity.setPricePlan(pricePlan);
            entity.setStoreId(storeId);
            applyAssignRequest(entity, request);
            savedPlan = roomTypePricePlanRepository.save(entity);
        }

        // 认证严格模式：关联成功后自动预热该房型+该价格计划的未来 N 天 ARI（严格最小范围）。
        warmupSuAriForMappingIfEnabled(storeId, roomTypeId, pricePlanId, "mapping-assign");
        return savedPlan;
    }

    public RoomTypePricePlan updateRoomTypePricePlan(Long id, AssignRoomTypePricePlanRequest request) {
        RoomTypePricePlan existing = roomTypePricePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房型价格计划不存在"));
        if (!currentStoreId().equals(existing.getStoreId())) {
            throw new RuntimeException("无权限操作");
        }

        applyAssignRequest(existing, request);
        RoomTypePricePlan saved = roomTypePricePlanRepository.save(existing);

        Long roomTypeId = existing.getRoomType() != null ? existing.getRoomType().getId() : null;
        Long pricePlanId = existing.getPricePlan() != null ? existing.getPricePlan().getId() : null;
        warmupSuAriForMappingIfEnabled(existing.getStoreId(), roomTypeId, pricePlanId, "mapping-update");
        return saved;
    }

    public void deleteRoomTypePricePlan(Long id) {
        RoomTypePricePlan roomTypePricePlan = roomTypePricePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房型价格计划不存在"));
        if (!currentStoreId().equals(roomTypePricePlan.getStoreId())) {
            throw new RuntimeException("无权限操作");
        }
        roomTypePricePlanRepository.delete(roomTypePricePlan);
    }

    public long countRoomTypesByPricePlan(Long pricePlanId) {
        return roomTypePricePlanRepository.countByPricePlanId(pricePlanId);
    }

    private void applyAssignRequest(RoomTypePricePlan entity, AssignRoomTypePricePlanRequest request) {
        entity.setMondayPrice(request.getMondayPrice());
        entity.setTuesdayPrice(request.getTuesdayPrice());
        entity.setWednesdayPrice(request.getWednesdayPrice());
        entity.setThursdayPrice(request.getThursdayPrice());
        entity.setFridayPrice(request.getFridayPrice());
        entity.setSaturdayPrice(request.getSaturdayPrice());
        entity.setSundayPrice(request.getSundayPrice());
        entity.setMaxGuests(request.getMaxGuests());
        entity.setIncludedGuests(request.getIncludedGuests());
        entity.setExtraAdultRate(request.getExtraAdultRate());
        entity.setExtraChildRate(request.getExtraChildRate());
        entity.setPriceMode(request.getPriceMode());
    }
}
