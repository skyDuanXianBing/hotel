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

    /**
     * 获取用户所有价格计划
     */
    public List<PricePlan> getAllPricePlans(Long userId) {
        return pricePlanRepository.findByUserIdOrderByName(userId);
    }

    /**
     * 根据ID获取价格计划
     */
    public Optional<PricePlan> getPricePlanById(Long userId, Long id) {
        return pricePlanRepository.findByIdAndUserId(id, userId);
    }

    /**
     * 创建价格计划
     */
    public PricePlan createPricePlan(Long userId, PricePlan pricePlan) {
        // 检查名称是否已存在
        if (pricePlanRepository.existsByUserIdAndName(userId, pricePlan.getName())) {
            throw new RuntimeException("价格计划名称已存在");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        pricePlan.setUser(user);
        return pricePlanRepository.save(pricePlan);
    }

    /**
     * 更新价格计划
     */
    public PricePlan updatePricePlan(Long userId, Long id, PricePlan pricePlan) {
        PricePlan existingPlan = pricePlanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        // 检查名称是否被其他价格计划使用
        if (!existingPlan.getName().equals(pricePlan.getName()) &&
            pricePlanRepository.existsByUserIdAndNameAndIdNot(userId, pricePlan.getName(), id)) {
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

    /**
     * 删除价格计划
     */
    public void deletePricePlan(Long userId, Long id) {
        PricePlan pricePlan = pricePlanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        // 删除所有关联的房型价格计划
        roomTypePricePlanRepository.deleteByPricePlanId(id);

        // 删除价格计划
        pricePlanRepository.delete(pricePlan);
    }

    /**
     * 获取价格计划关联的所有房型
     */
    public List<RoomTypePricePlan> getRoomTypesByPricePlan(Long pricePlanId) {
        return roomTypePricePlanRepository.findByPricePlanId(pricePlanId);
    }

    /**
     * 获取房型的所有价格计划
     */
    public List<RoomTypePricePlan> getPricePlansByRoomType(Long roomTypeId) {
        return roomTypePricePlanRepository.findByRoomTypeId(roomTypeId);
    }

    /**
     * 为房型分配价格计划
     */
    public RoomTypePricePlan assignPricePlanToRoomType(Long userId, Long roomTypeId, Long pricePlanId, AssignRoomTypePricePlanRequest request) {
        // 验证房型和价格计划是否属于当前用户
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("房型不存在"));

        if (!roomType.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限操作此房型");
        }

        PricePlan pricePlan = pricePlanRepository.findByIdAndUserId(pricePlanId, userId)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        // 检查是否已关联
        Optional<RoomTypePricePlan> existing = roomTypePricePlanRepository
                .findByRoomTypeIdAndPricePlanId(roomTypeId, pricePlanId);

        RoomTypePricePlan savedPlan;
        if (existing.isPresent()) {
            // 更新现有关联
            RoomTypePricePlan existingPlan = existing.get();
            existingPlan.setMondayPrice(request.getMondayPrice());
            existingPlan.setTuesdayPrice(request.getTuesdayPrice());
            existingPlan.setWednesdayPrice(request.getWednesdayPrice());
            existingPlan.setThursdayPrice(request.getThursdayPrice());
            existingPlan.setFridayPrice(request.getFridayPrice());
            existingPlan.setSaturdayPrice(request.getSaturdayPrice());
            existingPlan.setSundayPrice(request.getSundayPrice());
            existingPlan.setMaxGuests(request.getMaxGuests());
            existingPlan.setIncludedGuests(request.getIncludedGuests());
            existingPlan.setPriceMode(request.getPriceMode());
            savedPlan = roomTypePricePlanRepository.save(existingPlan);
        } else {
            // 创建新关联
            RoomTypePricePlan newPlan = new RoomTypePricePlan();
            newPlan.setRoomType(roomType);
            newPlan.setPricePlan(pricePlan);
            newPlan.setMondayPrice(request.getMondayPrice());
            newPlan.setTuesdayPrice(request.getTuesdayPrice());
            newPlan.setWednesdayPrice(request.getWednesdayPrice());
            newPlan.setThursdayPrice(request.getThursdayPrice());
            newPlan.setFridayPrice(request.getFridayPrice());
            newPlan.setSaturdayPrice(request.getSaturdayPrice());
            newPlan.setSundayPrice(request.getSundayPrice());
            newPlan.setMaxGuests(request.getMaxGuests());
            newPlan.setIncludedGuests(request.getIncludedGuests());
            newPlan.setPriceMode(request.getPriceMode());
            savedPlan = roomTypePricePlanRepository.save(newPlan);
        }

        return savedPlan;
    }

    /**
     * 更新房型价格计划
     */
    public RoomTypePricePlan updateRoomTypePricePlan(Long userId, Long id, RoomTypePricePlan roomTypePricePlan) {
        RoomTypePricePlan existing = roomTypePricePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房型价格计划不存在"));

        // 验证权限
        if (!existing.getRoomType().getUser().getId().equals(userId)) {
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

    /**
     * 删除房型价格计划关联
     */
    public void deleteRoomTypePricePlan(Long userId, Long id) {
        RoomTypePricePlan roomTypePricePlan = roomTypePricePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房型价格计划不存在"));

        // 验证权限
        if (!roomTypePricePlan.getRoomType().getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限操作");
        }

        roomTypePricePlanRepository.delete(roomTypePricePlan);
    }

    /**
     * 统计价格计划关联的房型数量
     */
    public long countRoomTypesByPricePlan(Long pricePlanId) {
        return roomTypePricePlanRepository.countByPricePlanId(pricePlanId);
    }
}
