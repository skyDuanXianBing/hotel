package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.AssignRoomTypePricePlanRequest;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.User;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;
import server.demo.util.StoreContextUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PricePlanService {

    @Autowired
    private PricePlanRepository pricePlanRepository;

    @Autowired
    private RoomTypePricePlanRepository roomTypePricePlanRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
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
        return pricePlanRepository.save(pricePlan);
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

        return pricePlanRepository.save(existingPlan);
    }

    public void deletePricePlan(Long id) {
        PricePlan pricePlan = pricePlanRepository.findByStoreIdAndId(currentStoreId(), id)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));
        roomTypePricePlanRepository.deleteByPricePlanId(id);
        pricePlanRepository.delete(pricePlan);
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
        return savedPlan;
    }

    public RoomTypePricePlan updateRoomTypePricePlan(Long id, RoomTypePricePlan roomTypePricePlan) {
        RoomTypePricePlan existing = roomTypePricePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房型价格计划不存在"));
        if (!currentStoreId().equals(existing.getStoreId())) {
            throw new RuntimeException("无权限操作");
        }

        existing.setMondayPrice(roomTypePricePlan.getMondayPrice());
        existing.setTuesdayPrice(roomTypePricePlan.getTuesdayPrice());
        existing.setWednesdayPrice(roomTypePricePlan.getWednesdayPrice());
        existing.setThursdayPrice(roomTypePricePlan.getThursdayPrice());
        existing.setFridayPrice(roomTypePricePlan.getFridayPrice());
        existing.setSaturdayPrice(roomTypePricePlan.getSaturdayPrice());
        existing.setSundayPrice(roomTypePricePlan.getSundayPrice());
        existing.setMaxGuests(roomTypePricePlan.getMaxGuests());
        existing.setIncludedGuests(roomTypePricePlan.getIncludedGuests());
        existing.setPriceMode(roomTypePricePlan.getPriceMode());
        return roomTypePricePlanRepository.save(existing);
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
        entity.setPriceMode(request.getPriceMode());
    }
}
